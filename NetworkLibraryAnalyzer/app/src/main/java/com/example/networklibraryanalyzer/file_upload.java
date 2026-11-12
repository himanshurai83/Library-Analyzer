package com.example.networklibraryanalyzer;

// Android imports
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.View;
import android.widget.Toast;

// AndroidX imports
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

// Material Components
import com.google.android.material.checkbox.MaterialCheckBox;

// View Binding
import com.example.networklibraryanalyzer.databinding.ActivityFileUploadBinding;

// Java utilities
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// Upload libraries (used by the UploadClient class at the bottom)
import android.content.ContentResolver;
import android.content.Context;
import com.android.volley.DefaultRetryPolicy;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.RequestFuture;
import com.android.volley.toolbox.Volley;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.http.Multipart;
import retrofit2.http.POST;
import retrofit2.http.Part;

// Ktor (Kotlin library, called from Java)
import io.ktor.client.HttpClient;
import io.ktor.client.HttpClientJvmKt;
import io.ktor.client.request.forms.FormBuildersKt;
import io.ktor.client.request.forms.FormDslKt;
import io.ktor.client.request.forms.FormPart;
import io.ktor.client.statement.HttpResponse;
import io.ktor.http.HeadersBuilder;
import io.ktor.http.HttpHeaders;
import io.ktor.http.content.PartData;
import kotlin.Unit;
import kotlin.coroutines.EmptyCoroutineContext;
import kotlinx.coroutines.BuildersKt;

public class file_upload extends AppCompatActivity {

    // =====================================================
    // 1. VIEW BINDING AND VARIABLES
    // =====================================================

    private ActivityFileUploadBinding binding;

    // Backend server URL.
    private static final String BASE_URL = "http://3.107.213.99:8080";

    // Stores ALL selected file URIs (the user can add more files later).
    private final List<Uri> selectedUris = new ArrayList<>();

    // Maximum number of files that can be selected (files are read into memory).
    private static final int MAX_FILES = 10;

    // File picker launcher.
    private ActivityResultLauncher<String> filePicker;

    // Runs uploads in the background (network is not allowed on the main thread).
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    // How many times each library uploads the file (results are averaged).
    private static final int UPLOAD_RUNS = 3;


    // =====================================================
    // 2. ON CREATE
    // =====================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Connect Java with XML.
        binding = ActivityFileUploadBinding.inflate(
                getLayoutInflater()
        );

        setContentView(binding.getRoot());


        // Register Android file picker (multiple files).
        filePicker = registerForActivityResult(
                new ActivityResultContracts.GetMultipleContents(),
                uris -> {

                    if (uris == null || uris.isEmpty()) {
                        return;
                    }

                    // Only 1 to MAX_FILES files can be selected at one time.
                    if (uris.size() > MAX_FILES) {

                        Toast.makeText(
                                this,
                                "Select only 1 to " + MAX_FILES + " files at one time. "
                                        + "You selected " + uris.size() + ".",
                                Toast.LENGTH_LONG
                        ).show();

                        return;
                    }

                    int added = 0;
                    boolean limitReached = false;

                    for (Uri uri : uris) {

                        // Skip files that are already in the list.
                        if (selectedUris.contains(uri)) {
                            continue;
                        }

                        if (selectedUris.size() >= MAX_FILES) {
                            limitReached = true;
                            break;
                        }

                        selectedUris.add(uri);
                        added++;
                    }

                    if (limitReached) {
                        Toast.makeText(
                                this,
                                "Maximum " + MAX_FILES + " files allowed",
                                Toast.LENGTH_SHORT
                        ).show();
                    }

                    refreshSelectedFiles();

                    binding.status.setText(
                            added + " file(s) added"
                    );
                }
        );


        // Initially hide file and success cards.
        binding.fileCard.setVisibility(View.GONE);
        binding.successCard.setVisibility(View.GONE);

        // Initialize performance information.
        resetUploadingPerformance();

        // Initialize upload information.
        updateUploadingInformation();

        // Disable start button initially.
        binding.uploadButton.setEnabled(false);


        // =================================================
        // 3. FILE SELECTION
        // =================================================

        binding.pickCard.setOnClickListener(v -> {

            filePicker.launch("*/*");

        });


        // Remove all selected files.
        binding.clearFilesButton.setOnClickListener(v -> {

            selectedUris.clear();
            refreshSelectedFiles();

        });


        // =================================================
        // 4. CHOOSE NETWORK LIBRARY BUTTON
        // =================================================

        binding.chooseLibraryButton.setOnClickListener(v -> {

            binding.main.post(() -> {

                binding.main.smoothScrollTo(
                        0,
                        binding.libraryCard.getTop()
                );

            });

        });


        // =================================================
        // 5. LIBRARY CHECKBOX LISTENERS
        // =================================================

        MaterialCheckBox[] libraryChecks = {

                binding.httpURLConnectionCheck,
                binding.okHttpCheck,
                binding.retrofitCheck,
                binding.volleyCheck,
                binding.ktorCheck

        };

        for (MaterialCheckBox checkBox : libraryChecks) {

            checkBox.setOnCheckedChangeListener(
                    (buttonView, isChecked) -> {

                        // Update selected library information.
                        updateUploadingInformation();

                        // Update start button.
                        updateStartButton();

                        // Hide old success message.
                        binding.successCard.setVisibility(View.GONE);

                    }
            );
        }


        // =================================================
        // 6. START ANALYSIS BUTTON
        // =================================================

        binding.uploadButton.setOnClickListener(v -> {

            startNetworkAnalysis();

        });

    }


    // =====================================================
    // 7. UPDATE START BUTTON
    // =====================================================

    private void updateStartButton() {

        // Check file selection (at least one file).
        boolean fileSelected = !selectedUris.isEmpty();

        // Check library selection.
        boolean librarySelected =
                binding.httpURLConnectionCheck.isChecked()
                        || binding.okHttpCheck.isChecked()
                        || binding.retrofitCheck.isChecked()
                        || binding.volleyCheck.isChecked()
                        || binding.ktorCheck.isChecked();

        // Enable button only if both conditions are true.
        binding.uploadButton.setEnabled(
                fileSelected && librarySelected
        );

    }


    // =====================================================
    // 7B. REFRESH THE SELECTED FILES (count, names, size)
    // =====================================================

    private void refreshSelectedFiles() {

        int count = selectedUris.size();
        long totalSize = getTotalSize();

        String sizeText = android.text.format.Formatter.formatFileSize(this, totalSize);

        // "3 files - 12 MB" badge next to the heading.
        binding.fileCountBadge.setText(
                count + (count == 1 ? " file" : " files")
                        + (count > 0 ? "  \u2022  " + sizeText : "")
        );

        if (count == 0) {

            binding.fileCard.setVisibility(View.GONE);

        } else {

            binding.fileCard.setVisibility(View.VISIBLE);

            binding.fileName.setText(
                    count + (count == 1 ? " file selected" : " files selected")
            );

            // Numbered list of file names.
            StringBuilder names = new StringBuilder();

            for (int i = 0; i < count; i++) {
                names.append(i + 1)
                        .append(". ")
                        .append(getFileName(selectedUris.get(i)))
                        .append("\n");
            }

            binding.fileList.setText(names.toString().trim());

            binding.status.setText("Ready to upload");
        }

        // Hide previous success message.
        binding.successCard.setVisibility(View.GONE);

        updateUploadingInformation();
        resetUploadingPerformance();
        updateStartButton();

    }


    // Sum of the sizes of all selected files (files with unknown size are skipped).
    private long getTotalSize() {

        long total = 0;

        for (Uri uri : selectedUris) {

            long size = getFileSize(uri);

            if (size > 0) {
                total += size;
            }
        }

        return total;

    }


    // =====================================================
    // 8. GET SELECTED LIBRARIES
    // =====================================================

    private List<String> getSelectedLibraries() {

        List<String> selectedLibraries = new ArrayList<>();

        if (binding.httpURLConnectionCheck.isChecked()) {

            selectedLibraries.add("HttpURLConnection");

        }

        if (binding.okHttpCheck.isChecked()) {

            selectedLibraries.add("OkHttp");

        }

        if (binding.retrofitCheck.isChecked()) {

            selectedLibraries.add("Retrofit");

        }

        if (binding.volleyCheck.isChecked()) {

            selectedLibraries.add("Volley");

        }

        if (binding.ktorCheck.isChecked()) {

            selectedLibraries.add("Ktor");

        }

        return selectedLibraries;

    }


    // =====================================================
    // 9. UPDATE UPLOADING INFORMATION
    // =====================================================

    private void updateUploadingInformation() {

        List<String> libraries = getSelectedLibraries();

        // Selected libraries.
        binding.infoLibraries.setText(
                libraries.isEmpty()
                        ? "No library selected"
                        : android.text.TextUtils.join(", ", libraries)
        );

        // If no file is selected.
        if (selectedUris.isEmpty()) {

            binding.infoFileCount.setText("0");
            binding.infoFileName.setText("No file selected");
            binding.infoFileSize.setText("--");
            binding.infoFileType.setText("--");
            binding.infoUploadStatus.setText("Waiting for file");

            return;
        }


        List<String> names = new ArrayList<>();
        java.util.Set<String> types = new java.util.LinkedHashSet<>();

        for (Uri uri : selectedUris) {

            names.add(getFileName(uri));

            String type = getContentResolver().getType(uri);
            types.add(type != null ? type : "Unknown");
        }

        // Number of files.
        binding.infoFileCount.setText(String.valueOf(selectedUris.size()));

        // File names.
        binding.infoFileName.setText(
                android.text.TextUtils.join(", ", names)
        );

        // Total size.
        long totalSize = getTotalSize();

        binding.infoFileSize.setText(
                totalSize > 0
                        ? android.text.format.Formatter.formatFileSize(this, totalSize)
                        : "Size unavailable"
        );

        // File types.
        binding.infoFileType.setText(
                android.text.TextUtils.join(", ", types)
        );

        // Upload status.
        binding.infoUploadStatus.setText(
                libraries.isEmpty()
                        ? "Select a network library"
                        : "Ready for analysis"
        );

    }


    // =====================================================
    // 10. GET FILE SIZE
    // =====================================================

    private long getFileSize(Uri uri) {

        long size = -1;

        try (Cursor cursor = getContentResolver().query(
                uri,
                null,
                null,
                null,
                null
        )) {

            if (cursor != null && cursor.moveToFirst()) {

                int sizeIndex = cursor.getColumnIndex(
                        OpenableColumns.SIZE
                );

                if (sizeIndex >= 0 && !cursor.isNull(sizeIndex)) {

                    size = cursor.getLong(sizeIndex);

                }

            }

        } catch (Exception e) {

            e.printStackTrace();

        }

        return size;

    }


    // =====================================================
    // 11. RESET UPLOADING PERFORMANCE
    // =====================================================

    private void resetUploadingPerformance() {

        binding.performanceUploadTime.setText(
                "Not tested"
        );

        binding.performanceThroughput.setText(
                "Not tested"
        );

        binding.performanceResponseTime.setText(
                "Not tested"
        );

        binding.performanceSuccessRate.setText(
                "Not tested"
        );

    }


    // =====================================================
    // 12. START NETWORK ANALYSIS (REAL UPLOAD OF ALL FILES)
    // =====================================================

    private void startNetworkAnalysis() {

        // Validate files.
        if (selectedUris.isEmpty()) {

            Toast.makeText(
                    this,
                    "Please select at least one file first!",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }


        // Get selected libraries.
        final List<String> selectedLibraries = getSelectedLibraries();


        // Validate libraries.
        if (selectedLibraries.isEmpty()) {

            Toast.makeText(
                    this,
                    "Please select at least one network library!",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }


        // Lock the button while uploading.
        binding.uploadButton.setEnabled(false);
        binding.successCard.setVisibility(View.GONE);

        binding.status.setText("Uploading...");
        binding.infoUploadStatus.setText(
                "Uploading " + selectedUris.size() + " file(s) to " + BASE_URL
        );


        // Copy of the selected files (the list may change while uploading).
        final List<Uri> uris = new ArrayList<>(selectedUris);
        final List<String> fileNames = new ArrayList<>();
        final List<String> mimeTypes = new ArrayList<>();

        for (Uri uri : uris) {

            fileNames.add(getFileName(uri));

            String type = getContentResolver().getType(uri);
            mimeTypes.add(type != null ? type : "application/octet-stream");
        }


        // Upload in the background.
        executor.execute(() -> {

            try {

                // Read every file once. All libraries send the same bytes.
                List<byte[]> allData = new ArrayList<>();
                long totalBytes = 0;

                for (Uri uri : uris) {

                    byte[] bytes = UploadClient.readBytes(getContentResolver(), uri);

                    allData.add(bytes);
                    totalBytes += bytes.length;
                }

                final int fileCount = allData.size();
                final long totalSizeBytes = totalBytes;

                UploadClient client = new UploadClient(getApplicationContext(), BASE_URL);

                StringBuilder timeText = new StringBuilder();
                StringBuilder throughputText = new StringBuilder();
                StringBuilder responseText = new StringBuilder();
                StringBuilder rateText = new StringBuilder();

                boolean anySuccess = false;
                String lastError = "";

                for (String library : selectedLibraries) {

                    long totalMs = 0;
                    long bestMs = Long.MAX_VALUE;
                    int successCount = 0;

                    // One run = upload ALL files one after another.
                    for (int i = 0; i < UPLOAD_RUNS; i++) {

                        long runMs = 0;
                        boolean runOk = true;

                        for (int f = 0; f < fileCount; f++) {

                            UploadClient.Result result = client.upload(
                                    library,
                                    allData.get(f),
                                    fileNames.get(f),
                                    mimeTypes.get(f)
                            );

                            if (result.success) {

                                runMs += result.timeMs;

                            } else {

                                runOk = false;

                                lastError = library + " (" + fileNames.get(f) + "): HTTP "
                                        + result.code + " " + result.message;

                                break;
                            }
                        }

                        if (runOk) {

                            successCount++;
                            totalMs += runMs;
                            bestMs = Math.min(bestMs, runMs);
                        }
                    }

                    if (successCount > 0) {

                        anySuccess = true;

                        long avgMs = Math.max(totalMs / successCount, 1);
                        double megabytes = totalSizeBytes / (1024.0 * 1024.0);
                        double throughput = megabytes / (avgMs / 1000.0);

                        timeText.append(library).append(": ")
                                .append(avgMs).append(" ms\n");

                        throughputText.append(library).append(": ")
                                .append(String.format(Locale.US, "%.2f MB/s", throughput))
                                .append("\n");

                        responseText.append(library).append(": ")
                                .append(bestMs).append(" ms (best)\n");

                    } else {

                        timeText.append(library).append(": Failed\n");
                        throughputText.append(library).append(": Failed\n");
                        responseText.append(library).append(": Failed\n");
                    }

                    rateText.append(library).append(": ")
                            .append(successCount * 100 / UPLOAD_RUNS).append("% (")
                            .append(successCount).append("/")
                            .append(UPLOAD_RUNS).append(")\n");
                }

                final boolean success = anySuccess;
                final String errorText = lastError;

                final String timeResult = timeText.toString().trim();
                final String throughputResult = throughputText.toString().trim();
                final String responseResult = responseText.toString().trim();
                final String rateResult = rateText.toString().trim();

                android.util.Log.d(
                        "NETWORK_ANALYSIS",
                        "Files: " + fileCount + " " + fileNames
                                + "\nTotal size: " + totalSizeBytes + " bytes"
                                + "\nBackend URL: " + BASE_URL
                                + "\nUpload time:\n" + timeResult
                                + "\nSuccess rate:\n" + rateResult
                                + (errorText.isEmpty() ? "" : "\nLast error: " + errorText)
                );

                runOnUiThread(() -> {

                    // Screen may be closed while uploading.
                    if (binding == null) {
                        return;
                    }

                    binding.performanceUploadTime.setText(timeResult);
                    binding.performanceThroughput.setText(throughputResult);
                    binding.performanceResponseTime.setText(responseResult);
                    binding.performanceSuccessRate.setText(rateResult);

                    if (success) {

                        binding.status.setText("Upload completed!");
                        binding.infoUploadStatus.setText("Uploaded to server");

                        binding.successMessage.setText(
                                fileCount + (fileCount == 1 ? " file" : " files")
                                        + " uploaded to " + BASE_URL
                        );

                        binding.successCard.setVisibility(View.VISIBLE);

                        Toast.makeText(
                                this,
                                "Upload completed!",
                                Toast.LENGTH_LONG
                        ).show();

                    } else {

                        binding.status.setText("Upload failed");
                        binding.infoUploadStatus.setText("Failed - check server and network");

                        Toast.makeText(
                                this,
                                "Upload failed: " + errorText,
                                Toast.LENGTH_LONG
                        ).show();
                    }

                    updateStartButton();
                });

            } catch (IOException e) {

                runOnUiThread(() -> {

                    if (binding == null) {
                        return;
                    }

                    binding.status.setText("Could not read the selected files");
                    binding.infoUploadStatus.setText("Failed");

                    Toast.makeText(
                            this,
                            "Could not read file: " + e.getMessage(),
                            Toast.LENGTH_LONG
                    ).show();

                    updateStartButton();
                });
            }
        });

    }


    // =====================================================
    // 13. GET FILE NAME
    // =====================================================

    private String getFileName(Uri uri) {

        String name = "Unknown file";

        try (Cursor cursor = getContentResolver().query(
                uri,
                null,
                null,
                null,
                null
        )) {

            if (cursor != null && cursor.moveToFirst()) {

                int index = cursor.getColumnIndex(
                        OpenableColumns.DISPLAY_NAME
                );

                if (index >= 0) {

                    String fileName = cursor.getString(index);

                    if (fileName != null) {

                        name = fileName;

                    }

                }

            }

        } catch (Exception e) {

            e.printStackTrace();

        }

        return name;

    }


    // =====================================================
    // 14. ACTIVITY CLEANUP
    // =====================================================

    @Override
    protected void onDestroy() {

        executor.shutdownNow();

        binding = null;

        super.onDestroy();

    }


    // =====================================================
    // 15. UPLOAD CLIENT (HttpURLConnection, OkHttp, Retrofit, Volley, Ktor)
    // =====================================================

    static class UploadClient {

        // =====================================================
        // RESULT OF ONE UPLOAD
        // =====================================================

        public static class Result {
            public final boolean success;
            public final long timeMs;
            public final int code;
            public final String message;

            public Result(boolean success, long timeMs, int code, String message) {
                this.success = success;
                this.timeMs = timeMs;
                this.code = code;
                this.message = message;
            }
        }


        // =====================================================
        // RETROFIT API
        // =====================================================

        interface UploadApi {
            @Multipart
            @POST("upload")
            Call<ResponseBody> upload(
                    @Part MultipartBody.Part file,
                    @Part("library") RequestBody library
            );
        }


        // =====================================================
        // VOLLEY MULTIPART REQUEST (Volley has no built-in multipart)
        // =====================================================

        private static class MultipartRequest extends Request<NetworkResponse> {

            private final byte[] body;
            private final String contentType;
            private final Response.Listener<NetworkResponse> listener;

            MultipartRequest(String url,
                             byte[] body,
                             String boundary,
                             Response.Listener<NetworkResponse> listener,
                             Response.ErrorListener errorListener) {
                super(Method.POST, url, errorListener);
                this.body = body;
                this.contentType = "multipart/form-data; boundary=" + boundary;
                this.listener = listener;
                setShouldCache(false);
                // 60 s timeout, no automatic retry (a retry would upload the file twice).
                setRetryPolicy(new DefaultRetryPolicy(60000, 0, 1f));
            }

            @Override
            public String getBodyContentType() {
                return contentType;
            }

            @Override
            public byte[] getBody() {
                return body;
            }

            @Override
            protected Response<NetworkResponse> parseNetworkResponse(NetworkResponse response) {
                return Response.success(response, HttpHeaderParser.parseCacheHeaders(response));
            }

            @Override
            protected void deliverResponse(NetworkResponse response) {
                listener.onResponse(response);
            }
        }


        // =====================================================
        // FIELDS
        // =====================================================

        private final String baseUrl;
        private final OkHttpClient okHttp;
        private final Retrofit retrofit;
        private final RequestQueue volleyQueue;

        public UploadClient(Context context, String baseUrl) {
            this.baseUrl = baseUrl;

            this.okHttp = new OkHttpClient.Builder()
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .writeTimeout(60, TimeUnit.SECONDS)
                    .readTimeout(60, TimeUnit.SECONDS)
                    .build();

            // Retrofit needs a base URL that ends with "/".
            this.retrofit = new Retrofit.Builder()
                    .baseUrl(baseUrl + "/")
                    .client(okHttp)
                    .build();

            this.volleyQueue = Volley.newRequestQueue(context.getApplicationContext());
        }


        // =====================================================
        // READ THE SELECTED FILE INTO MEMORY
        // (all libraries send the same bytes, so the test is fair)
        // =====================================================

        public static byte[] readBytes(ContentResolver resolver, Uri uri) throws IOException {

            try (InputStream in = resolver.openInputStream(uri)) {

                if (in == null) {
                    throw new IOException("Cannot open the selected file");
                }

                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] buffer = new byte[8192];
                int read;

                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }

                return out.toByteArray();
            }
        }


        // =====================================================
        // UPLOAD WITH THE CHOSEN LIBRARY
        // =====================================================

        public Result upload(String library, byte[] data, String fileName, String mimeType) {

            try {
                switch (library) {

                    case "HttpURLConnection":
                        return viaHttpUrlConnection(data, fileName, mimeType, library);

                    case "OkHttp":
                        return viaOkHttp(data, fileName, mimeType, library);

                    case "Retrofit":
                        return viaRetrofit(data, fileName, mimeType, library);

                    case "Volley":
                        return viaVolley(data, fileName, mimeType, library);

                    case "Ktor":
                        return viaKtor(data, fileName, mimeType, library);

                    default:
                        return new Result(false, 0, -1, "Unknown library: " + library);
                }

            } catch (Exception e) {
                return new Result(false, 0, -1,
                        e.getClass().getSimpleName() + ": " + e.getMessage());
            }
        }


        // -----------------------------------------------------
        // HttpURLConnection
        // -----------------------------------------------------

        private Result viaHttpUrlConnection(byte[] data, String fileName,
                                            String mimeType, String library) throws IOException {

            String boundary = "----NLA" + System.currentTimeMillis();
            byte[] body = buildMultipartBody(boundary, library, fileName, mimeType, data);

            long start = System.nanoTime();

            HttpURLConnection connection =
                    (HttpURLConnection) new URL(baseUrl + "/upload").openConnection();

            try {
                connection.setRequestMethod("POST");
                connection.setDoOutput(true);
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(60000);
                connection.setFixedLengthStreamingMode(body.length);
                connection.setRequestProperty(
                        "Content-Type", "multipart/form-data; boundary=" + boundary);

                try (OutputStream out = connection.getOutputStream()) {
                    out.write(body);
                }

                int code = connection.getResponseCode();

                InputStream in = code >= 400
                        ? connection.getErrorStream()
                        : connection.getInputStream();

                return finish(start, code, readText(in));

            } finally {
                connection.disconnect();
            }
        }


        // -----------------------------------------------------
        // OkHttp
        // -----------------------------------------------------

        private Result viaOkHttp(byte[] data, String fileName,
                                 String mimeType, String library) throws IOException {

            RequestBody fileBody = RequestBody.create(data, MediaType.parse(mimeType));

            RequestBody body = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("library", library)
                    .addFormDataPart("file", fileName, fileBody)
                    .build();

            okhttp3.Request request = new okhttp3.Request.Builder()
                    .url(baseUrl + "/upload")
                    .post(body)
                    .build();

            long start = System.nanoTime();

            try (okhttp3.Response response = okHttp.newCall(request).execute()) {

                String text = response.body() != null ? response.body().string() : "";

                return finish(start, response.code(), text);
            }
        }


        // -----------------------------------------------------
        // Retrofit
        // -----------------------------------------------------

        private Result viaRetrofit(byte[] data, String fileName,
                                   String mimeType, String library) throws IOException {

            UploadApi api = retrofit.create(UploadApi.class);

            MultipartBody.Part filePart = MultipartBody.Part.createFormData(
                    "file",
                    fileName,
                    RequestBody.create(data, MediaType.parse(mimeType))
            );

            RequestBody libraryPart =
                    RequestBody.create(library, MediaType.parse("text/plain"));

            long start = System.nanoTime();

            retrofit2.Response<ResponseBody> response =
                    api.upload(filePart, libraryPart).execute();

            ResponseBody responseBody =
                    response.isSuccessful() ? response.body() : response.errorBody();

            String text = responseBody != null ? responseBody.string() : "";

            return finish(start, response.code(), text);
        }


        // -----------------------------------------------------
        // Volley
        // -----------------------------------------------------

        private Result viaVolley(byte[] data, String fileName,
                                 String mimeType, String library) {

            String boundary = "----NLA" + System.currentTimeMillis();
            byte[] body = buildMultipartBody(boundary, library, fileName, mimeType, data);

            RequestFuture<NetworkResponse> future = RequestFuture.newFuture();

            MultipartRequest request = new MultipartRequest(
                    baseUrl + "/upload", body, boundary, future, future);

            long start = System.nanoTime();
            volleyQueue.add(request);

            try {
                NetworkResponse response = future.get(60, TimeUnit.SECONDS);

                return finish(start, response.statusCode,
                        new String(response.data, StandardCharsets.UTF_8));

            } catch (ExecutionException e) {

                // The server answered with an error code (400, 413, 500 ...).
                if (e.getCause() instanceof VolleyError) {
                    NetworkResponse nr = ((VolleyError) e.getCause()).networkResponse;

                    if (nr != null) {
                        return finish(start, nr.statusCode,
                                new String(nr.data, StandardCharsets.UTF_8));
                    }
                }

                return new Result(false, elapsedMs(start), -1,
                        String.valueOf(e.getCause()));

            } catch (Exception e) {
                return new Result(false, elapsedMs(start), -1,
                        e.getClass().getSimpleName() + ": " + e.getMessage());
            }
        }


        // -----------------------------------------------------
        // Ktor (Kotlin library, called from Java with runBlocking)
        // -----------------------------------------------------

        // One shared client (creating a client per upload would leak threads).
        private static HttpClient ktorClient;

        private static synchronized HttpClient getKtorClient() {
            if (ktorClient == null) {
                // Uses whichever Ktor engine you added (CIO or OkHttp).
                ktorClient = HttpClientJvmKt.HttpClient(config -> Unit.INSTANCE);
            }
            return ktorClient;
        }

        private Result viaKtor(byte[] data, String fileName,
                               String mimeType, String library)
                throws InterruptedException {

            final String url = baseUrl + "/upload";
            final HttpClient client = getKtorClient();

            // Headers of the "file" part.
            HeadersBuilder fileHeaders = new HeadersBuilder(8);
            fileHeaders.append(HttpHeaders.INSTANCE.getContentType(), mimeType);
            fileHeaders.append(HttpHeaders.INSTANCE.getContentDisposition(),
                    "filename=\"" + fileName.replace("\"", "_") + "\"");

            // Multipart body: fields "library" and "file".
            final List<PartData> parts = FormDslKt.formData(
                    new FormPart<String>("library", library,
                            new HeadersBuilder(8).build()),
                    new FormPart<byte[]>("file", data, fileHeaders.build())
            );

            long start = System.nanoTime();

            // Ktor calls are "suspend" functions, so we run them with runBlocking.
            // (We are already on a background thread, so blocking is fine.)
            HttpResponse response = BuildersKt.<HttpResponse>runBlocking(
                    EmptyCoroutineContext.INSTANCE,
                    (scope, continuation) -> FormBuildersKt.submitFormWithBinaryData(
                            client, url, parts, request -> Unit.INSTANCE, continuation)
            );

            return finish(start, response.getStatus().getValue(), "");
        }


        // =====================================================
        // HELPERS
        // =====================================================

        // Builds a multipart/form-data body with fields "library" and "file".
        private static byte[] buildMultipartBody(String boundary, String library,
                                                 String fileName, String mimeType,
                                                 byte[] data) {

            String nl = "\r\n";
            String safeName = fileName.replace("\"", "_");

            ByteArrayOutputStream out = new ByteArrayOutputStream(data.length + 1024);

            write(out, "--" + boundary + nl
                    + "Content-Disposition: form-data; name=\"library\"" + nl + nl
                    + library + nl);

            write(out, "--" + boundary + nl
                    + "Content-Disposition: form-data; name=\"file\"; filename=\""
                    + safeName + "\"" + nl
                    + "Content-Type: " + mimeType + nl + nl);

            out.write(data, 0, data.length);

            write(out, nl + "--" + boundary + "--" + nl);

            return out.toByteArray();
        }

        private static void write(ByteArrayOutputStream out, String text) {
            byte[] bytes = text.getBytes(StandardCharsets.UTF_8);
            out.write(bytes, 0, bytes.length);
        }

        private static String readText(InputStream in) throws IOException {

            if (in == null) {
                return "";
            }

            try (InputStream stream = in) {
                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] buffer = new byte[4096];
                int read;

                while ((read = stream.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }

                return out.toString("UTF-8");
            }
        }

        private static long elapsedMs(long startNanos) {
            return (System.nanoTime() - startNanos) / 1_000_000;
        }

        // Success means an HTTP 2xx answer from the server.
        private static Result finish(long startNanos, int code, String body) {
            boolean ok = code >= 200 && code < 300;
            return new Result(ok, elapsedMs(startNanos), code, body);
        }
    }

}