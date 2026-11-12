package com.example.networklibraryanalyzer;

// Android imports
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Toast;

// AndroidX imports
import androidx.appcompat.app.AppCompatActivity;

// Material Components
import com.google.android.material.checkbox.MaterialCheckBox;

// View Binding
import com.example.networklibraryanalyzer.databinding.ActivityFileDownloadBinding;

// Java utilities
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

// JSON (file list from the server)
import org.json.JSONArray;
import org.json.JSONObject;

// Download libraries (used by the DownloadClient class at the bottom)
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
import okhttp3.OkHttpClient;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Retrofit;
import retrofit2.http.GET;
import retrofit2.http.Streaming;
import retrofit2.http.Url;

// Ktor (Kotlin library, called from Java)
import io.ktor.client.HttpClient;
import io.ktor.client.HttpClientJvmKt;
import io.ktor.client.statement.HttpResponse;
import kotlin.Unit;
import kotlin.coroutines.EmptyCoroutineContext;
import kotlinx.coroutines.BuildersKt;

public class file_download extends AppCompatActivity {

    // =====================================================
    // 1. VIEW BINDING AND VARIABLES
    // =====================================================

    private ActivityFileDownloadBinding binding;

    // Backend server URL.
    private static final String BASE_URL = "http://3.107.213.99:8080";

    // Only 1 to MAX_FILES files can be selected.
    private static final int MAX_FILES = 10;

    // How many times each library downloads the files (results are averaged).
    private static final int DOWNLOAD_RUNS = 3;

    // Runs network work in the background.
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    // A file that exists on the server.
    private static class ServerFile {
        final String name;
        final long size;      // -1 when the server did not send the size

        ServerFile(String name, long size) {
            this.name = name;
            this.size = size;
        }
    }

    // Selected files (in the order the user selected them).
    private final List<ServerFile> selectedFiles = new ArrayList<>();

    // Used to avoid a loop when we un-check a box from code.
    private boolean ignoreCheckChange = false;


    // =====================================================
    // 2. ON CREATE
    // =====================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityFileDownloadBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        resetPerformance();
        updateInformation();
        binding.btnDownloadFile.setEnabled(false);


        // Load the list of files from the server.
        binding.refreshFilesButton.setOnClickListener(v -> loadServerFiles());


        // Scroll to the library card.
        binding.chooseLibraryButton.setOnClickListener(v ->
                binding.main.post(() ->
                        binding.main.smoothScrollTo(0, binding.libraryCard.getTop())
                )
        );


        // Library check boxes.
        MaterialCheckBox[] libraryChecks = {
                binding.httpURLConnectionCheck,
                binding.okHttpCheck,
                binding.retrofitCheck,
                binding.volleyCheck,
                binding.ktorCheck
        };

        for (MaterialCheckBox checkBox : libraryChecks) {

            checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
                updateInformation();
                updateStartButton();
                binding.successCard.setVisibility(View.GONE);
            });
        }


        // Start the download test.
        binding.btnDownloadFile.setOnClickListener(v -> startDownloadAnalysis());

        // Load the list when the screen opens.
        loadServerFiles();
    }


    // =====================================================
    // 3. LOAD THE FILE LIST FROM THE SERVER  (GET /files)
    // =====================================================

    private void loadServerFiles() {

        binding.refreshFilesButton.setEnabled(false);
        binding.filesStatus.setText("Loading files from server...");

        executor.execute(() -> {

            try {

                String json = DownloadClient.httpGetText(BASE_URL + "/files");

                JSONArray array = new JSONArray(json);
                List<ServerFile> files = new ArrayList<>();

                for (int i = 0; i < array.length(); i++) {

                    JSONObject item = array.getJSONObject(i);

                    files.add(new ServerFile(
                            item.getString("name"),
                            item.optLong("size", -1)
                    ));
                }

                runOnUiThread(() -> {

                    if (binding == null) {
                        return;
                    }

                    showServerFiles(files);
                    binding.refreshFilesButton.setEnabled(true);
                });

            } catch (Exception e) {

                runOnUiThread(() -> {

                    if (binding == null) {
                        return;
                    }

                    binding.filesStatus.setText(
                            "Could not load files: " + e.getMessage()
                    );

                    binding.refreshFilesButton.setEnabled(true);
                });
            }
        });
    }


    // =====================================================
    // 4. SHOW THE FILES AS CHECK BOXES (1 to 10 can be selected)
    // =====================================================

    private void showServerFiles(List<ServerFile> files) {

        binding.serverFilesContainer.removeAllViews();
        selectedFiles.clear();

        if (files.isEmpty()) {

            binding.filesStatus.setText("No files on the server. Upload a file first.");

        } else {

            binding.filesStatus.setText(
                    files.size() + " file(s) on server. Select 1 to " + MAX_FILES + "."
            );
        }

        int padding = (int) (8 * getResources().getDisplayMetrics().density);

        for (ServerFile file : files) {

            MaterialCheckBox checkBox = new MaterialCheckBox(this);

            checkBox.setText(
                    file.name + (file.size >= 0
                            ? "  (" + android.text.format.Formatter.formatFileSize(this, file.size) + ")"
                            : "")
            );
            checkBox.setTextColor(Color.parseColor("#172033"));
            checkBox.setTextSize(15);
            checkBox.setPadding(padding, padding, padding, padding);

            checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {

                if (ignoreCheckChange) {
                    return;
                }

                if (isChecked) {

                    // Do not allow more than MAX_FILES.
                    if (selectedFiles.size() >= MAX_FILES) {

                        ignoreCheckChange = true;
                        buttonView.setChecked(false);
                        ignoreCheckChange = false;

                        Toast.makeText(
                                this,
                                "You can select only 1 to " + MAX_FILES + " files",
                                Toast.LENGTH_SHORT
                        ).show();

                        return;
                    }

                    selectedFiles.add(file);

                } else {

                    selectedFiles.remove(file);
                }

                onSelectionChanged();
            });

            binding.serverFilesContainer.addView(checkBox);
        }

        onSelectionChanged();
    }


    private void onSelectionChanged() {

        binding.selectedCountBadge.setText(selectedFiles.size() + " selected");

        binding.successCard.setVisibility(View.GONE);

        updateInformation();
        resetPerformance();
        updateStartButton();
    }


    // =====================================================
    // 5. SELECTED LIBRARIES / START BUTTON / INFORMATION
    // =====================================================

    private List<String> getSelectedLibraries() {

        List<String> libraries = new ArrayList<>();

        if (binding.httpURLConnectionCheck.isChecked()) libraries.add("HttpURLConnection");
        if (binding.okHttpCheck.isChecked()) libraries.add("OkHttp");
        if (binding.retrofitCheck.isChecked()) libraries.add("Retrofit");
        if (binding.volleyCheck.isChecked()) libraries.add("Volley");
        if (binding.ktorCheck.isChecked()) libraries.add("Ktor");

        return libraries;
    }


    private void updateStartButton() {

        boolean fileSelected = !selectedFiles.isEmpty();
        boolean librarySelected = !getSelectedLibraries().isEmpty();

        binding.btnDownloadFile.setEnabled(fileSelected && librarySelected);
    }


    private long getTotalExpectedSize() {

        long total = 0;

        for (ServerFile file : selectedFiles) {
            if (file.size > 0) {
                total += file.size;
            }
        }

        return total;
    }


    private void updateInformation() {

        List<String> libraries = getSelectedLibraries();

        binding.infoLibraries.setText(
                libraries.isEmpty()
                        ? "No library selected"
                        : android.text.TextUtils.join(", ", libraries)
        );

        if (selectedFiles.isEmpty()) {

            binding.infoFileCount.setText("0");
            binding.infoFileName.setText("No file selected");
            binding.infoFileSize.setText("--");
            binding.infoDownloadStatus.setText("Select a file from the list");

            return;
        }

        List<String> names = new ArrayList<>();

        for (ServerFile file : selectedFiles) {
            names.add(file.name);
        }

        binding.infoFileCount.setText(String.valueOf(selectedFiles.size()));
        binding.infoFileName.setText(android.text.TextUtils.join(", ", names));

        long total = getTotalExpectedSize();

        binding.infoFileSize.setText(
                total > 0
                        ? android.text.format.Formatter.formatFileSize(this, total)
                        : "Size unavailable"
        );

        binding.infoDownloadStatus.setText(
                libraries.isEmpty() ? "Select a network library" : "Ready for analysis"
        );
    }


    private void resetPerformance() {

        binding.performanceDownloadTime.setText("Not tested");
        binding.performanceThroughput.setText("Not tested");
        binding.performanceResponseTime.setText("Not tested");
        binding.performanceSuccessRate.setText("Not tested");
    }


    // =====================================================
    // 6. START DOWNLOAD ANALYSIS (REAL DOWNLOAD)
    // =====================================================

    private void startDownloadAnalysis() {

        if (selectedFiles.isEmpty()) {

            Toast.makeText(this, "Please select at least one file!", Toast.LENGTH_SHORT).show();
            return;
        }

        final List<String> selectedLibraries = getSelectedLibraries();

        if (selectedLibraries.isEmpty()) {

            Toast.makeText(this, "Please select at least one network library!",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        // Lock the screen while downloading.
        binding.btnDownloadFile.setEnabled(false);
        binding.refreshFilesButton.setEnabled(false);
        binding.successCard.setVisibility(View.GONE);
        binding.progressBar.setVisibility(View.VISIBLE);

        binding.infoDownloadStatus.setText(
                "Downloading " + selectedFiles.size() + " file(s) from " + BASE_URL
        );

        // Copy (the user could change the selection while downloading).
        final List<ServerFile> files = new ArrayList<>(selectedFiles);

        // Folder where the downloaded files are saved (no permission needed).
        File dir = getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS);

        if (dir == null) {
            dir = getFilesDir();
        }

        final File saveDir = dir;


        executor.execute(() -> {

            DownloadClient client = new DownloadClient(getApplicationContext());

            StringBuilder timeText = new StringBuilder();
            StringBuilder throughputText = new StringBuilder();
            StringBuilder responseText = new StringBuilder();
            StringBuilder rateText = new StringBuilder();

            // Each file is saved only once (from the first successful download).
            Set<String> savedNames = new HashSet<>();

            boolean anySuccess = false;
            String lastError = "";
            long lastTotalBytes = 0;

            for (String library : selectedLibraries) {

                long totalMs = 0;
                long totalBytesAllRuns = 0;
                long bestMs = Long.MAX_VALUE;
                int successCount = 0;

                // One run = download ALL selected files one after another.
                for (int run = 0; run < DOWNLOAD_RUNS; run++) {

                    long runMs = 0;
                    long runBytes = 0;
                    boolean runOk = true;

                    for (ServerFile file : files) {

                        String url = BASE_URL + "/download/" + Uri.encode(file.name);

                        DownloadClient.Result result = client.download(library, url);

                        // Check that the whole file arrived.
                        if (result.success && file.size >= 0 && result.bytes >= 0
                                && result.bytes != file.size) {

                            result = new DownloadClient.Result(false, result.timeMs, result.code,
                                    "size mismatch: got " + result.bytes + " of " + file.size,
                                    null);
                        }

                        if (result.success) {

                            runMs += result.timeMs;
                            runBytes += result.bytes >= 0 ? result.bytes : Math.max(file.size, 0);

                            // Save the file once so the user really has it.
                            if (result.data != null && savedNames.add(file.name)) {
                                saveToFile(saveDir, file.name, result.data);
                            }

                        } else {

                            runOk = false;

                            lastError = library + " (" + file.name + "): HTTP "
                                    + result.code + " " + result.message;

                            break;
                        }
                    }

                    if (runOk) {

                        successCount++;
                        totalMs += runMs;
                        totalBytesAllRuns += runBytes;
                        bestMs = Math.min(bestMs, runMs);
                    }
                }

                if (successCount > 0) {

                    anySuccess = true;

                    long avgMs = Math.max(totalMs / successCount, 1);
                    long avgBytes = totalBytesAllRuns / successCount;
                    lastTotalBytes = avgBytes;

                    double megabytes = avgBytes / (1024.0 * 1024.0);
                    double throughput = megabytes / (avgMs / 1000.0);

                    timeText.append(library).append(": ").append(avgMs).append(" ms\n");

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
                        .append(successCount * 100 / DOWNLOAD_RUNS).append("% (")
                        .append(successCount).append("/")
                        .append(DOWNLOAD_RUNS).append(")\n");
            }

            final boolean success = anySuccess;
            final String errorText = lastError;
            final long downloadedBytes = lastTotalBytes;
            final boolean anySaved = !savedNames.isEmpty();

            final String timeResult = timeText.toString().trim();
            final String throughputResult = throughputText.toString().trim();
            final String responseResult = responseText.toString().trim();
            final String rateResult = rateText.toString().trim();

            android.util.Log.d(
                    "NETWORK_ANALYSIS",
                    "Download of " + files.size() + " file(s)"
                            + "\nBytes per run: " + downloadedBytes
                            + "\nBackend URL: " + BASE_URL
                            + "\nDownload time:\n" + timeResult
                            + "\nSuccess rate:\n" + rateResult
                            + (errorText.isEmpty() ? "" : "\nLast error: " + errorText)
            );

            runOnUiThread(() -> {

                // Screen may be closed while downloading.
                if (binding == null) {
                    return;
                }

                binding.progressBar.setVisibility(View.GONE);
                binding.refreshFilesButton.setEnabled(true);

                binding.performanceDownloadTime.setText(timeResult);
                binding.performanceThroughput.setText(throughputResult);
                binding.performanceResponseTime.setText(responseResult);
                binding.performanceSuccessRate.setText(rateResult);

                if (success) {

                    binding.infoDownloadStatus.setText("Downloaded from server");

                    binding.successMessage.setText(
                            files.size() + (files.size() == 1 ? " file" : " files")
                                    + " downloaded."
                                    + (anySaved ? "\nSaved in: " + saveDir.getAbsolutePath() : "")
                    );

                    binding.successCard.setVisibility(View.VISIBLE);

                    Toast.makeText(this, "Download completed!", Toast.LENGTH_LONG).show();

                } else {

                    binding.infoDownloadStatus.setText("Failed - check server and network");

                    Toast.makeText(this, "Download failed: " + errorText,
                            Toast.LENGTH_LONG).show();
                }

                updateStartButton();
            });
        });
    }


    // =====================================================
    // 7. SAVE A DOWNLOADED FILE
    // =====================================================

    private void saveToFile(File dir, String name, byte[] data) {

        // getName() removes any folder part, so the file stays inside "dir".
        File target = new File(dir, new File(name).getName());

        try (FileOutputStream out = new FileOutputStream(target)) {

            out.write(data);

        } catch (IOException e) {

            android.util.Log.e("DOWNLOAD", "Could not save " + name, e);
        }
    }


    // =====================================================
    // 8. ACTIVITY CLEANUP
    // =====================================================

    @Override
    protected void onDestroy() {

        executor.shutdownNow();

        binding = null;

        super.onDestroy();
    }


    // =====================================================
    // 9. DOWNLOAD CLIENT (HttpURLConnection, OkHttp, Retrofit, Volley, Ktor)
    // =====================================================

    static class DownloadClient {

        // =====================================================
        // RESULT OF ONE DOWNLOAD
        // =====================================================

        public static class Result {
            public final boolean success;
            public final long timeMs;
            public final int code;
            public final String message;
            public final byte[] data;
            public final long bytes;

            public Result(boolean success, long timeMs, int code, String message, byte[] data) {
                this(success, timeMs, code, message, data, data != null ? data.length : 0);
            }

            // bytes = -1 means "number of bytes unknown".
            public Result(boolean success, long timeMs, int code, String message,
                          byte[] data, long bytes) {
                this.success = success;
                this.timeMs = timeMs;
                this.code = code;
                this.message = message;
                this.data = data;
                this.bytes = bytes;
            }
        }


        // =====================================================
        // RETROFIT API
        // =====================================================

        interface DownloadApi {
            @Streaming
            @GET
            Call<ResponseBody> download(@Url String url);
        }


        // =====================================================
        // VOLLEY REQUEST THAT RETURNS THE RAW BYTES
        // =====================================================

        private static class BytesRequest extends Request<NetworkResponse> {

            private final Response.Listener<NetworkResponse> listener;

            BytesRequest(String url,
                         Response.Listener<NetworkResponse> listener,
                         Response.ErrorListener errorListener) {
                super(Method.GET, url, errorListener);
                this.listener = listener;
                setShouldCache(false);
                // 60 s timeout, no automatic retry.
                setRetryPolicy(new DefaultRetryPolicy(60000, 0, 1f));
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

        private final OkHttpClient okHttp;
        private final Retrofit retrofit;
        private final RequestQueue volleyQueue;

        public DownloadClient(Context context) {

            this.okHttp = new OkHttpClient.Builder()
                    .connectTimeout(15, TimeUnit.SECONDS)
                    .readTimeout(60, TimeUnit.SECONDS)
                    .build();

            // Retrofit needs a base URL that ends with "/".
            // The real address is given with @Url for every download.
            this.retrofit = new Retrofit.Builder()
                    .baseUrl(BASE_URL + "/")
                    .client(okHttp)
                    .build();

            this.volleyQueue = Volley.newRequestQueue(context.getApplicationContext());
        }


        // =====================================================
        // SMALL HELPER: GET a text answer (used for the file list)
        // =====================================================

        public static String httpGetText(String address) throws IOException {

            HttpURLConnection connection = (HttpURLConnection) new URL(address).openConnection();

            try {
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(10000);
                connection.setReadTimeout(15000);

                int code = connection.getResponseCode();

                if (code < 200 || code >= 300) {
                    throw new IOException("HTTP " + code);
                }

                return new String(readAll(connection.getInputStream()), "UTF-8");

            } finally {
                connection.disconnect();
            }
        }


        // =====================================================
        // DOWNLOAD WITH THE CHOSEN LIBRARY
        // =====================================================

        public Result download(String library, String url) {

            try {
                switch (library) {

                    case "HttpURLConnection":
                        return viaHttpUrlConnection(url);

                    case "OkHttp":
                        return viaOkHttp(url);

                    case "Retrofit":
                        return viaRetrofit(url);

                    case "Volley":
                        return viaVolley(url);

                    case "Ktor":
                        return viaKtor(url);

                    default:
                        return new Result(false, 0, -1, "Unknown library: " + library, null);
                }

            } catch (Exception e) {
                return new Result(false, 0, -1,
                        e.getClass().getSimpleName() + ": " + e.getMessage(), null);
            }
        }


        // -----------------------------------------------------
        // HttpURLConnection
        // -----------------------------------------------------

        private Result viaHttpUrlConnection(String url) throws IOException {

            long start = System.nanoTime();

            HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();

            try {
                connection.setRequestMethod("GET");
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(60000);

                int code = connection.getResponseCode();

                InputStream in = code >= 400
                        ? connection.getErrorStream()
                        : connection.getInputStream();

                byte[] data = in != null ? readAll(in) : new byte[0];

                return finish(start, code, data);

            } finally {
                connection.disconnect();
            }
        }


        // -----------------------------------------------------
        // OkHttp
        // -----------------------------------------------------

        private Result viaOkHttp(String url) throws IOException {

            okhttp3.Request request = new okhttp3.Request.Builder()
                    .url(url)
                    .get()
                    .build();

            long start = System.nanoTime();

            try (okhttp3.Response response = okHttp.newCall(request).execute()) {

                byte[] data = response.body() != null
                        ? response.body().bytes()
                        : new byte[0];

                return finish(start, response.code(), data);
            }
        }


        // -----------------------------------------------------
        // Retrofit
        // -----------------------------------------------------

        private Result viaRetrofit(String url) throws IOException {

            DownloadApi api = retrofit.create(DownloadApi.class);

            long start = System.nanoTime();

            retrofit2.Response<ResponseBody> response = api.download(url).execute();

            ResponseBody body = response.isSuccessful() ? response.body() : response.errorBody();

            byte[] data = body != null ? body.bytes() : new byte[0];

            return finish(start, response.code(), data);
        }


        // -----------------------------------------------------
        // Volley
        // -----------------------------------------------------

        private Result viaVolley(String url) {

            RequestFuture<NetworkResponse> future = RequestFuture.newFuture();

            BytesRequest request = new BytesRequest(url, future, future);

            long start = System.nanoTime();
            volleyQueue.add(request);

            try {
                NetworkResponse response = future.get(60, TimeUnit.SECONDS);

                return finish(start, response.statusCode, response.data);

            } catch (ExecutionException e) {

                // The server answered with an error code (404, 500 ...).
                if (e.getCause() instanceof VolleyError) {

                    NetworkResponse nr = ((VolleyError) e.getCause()).networkResponse;

                    if (nr != null) {
                        return finish(start, nr.statusCode, nr.data);
                    }
                }

                return new Result(false, elapsedMs(start), -1,
                        String.valueOf(e.getCause()), null);

            } catch (Exception e) {

                return new Result(false, elapsedMs(start), -1,
                        e.getClass().getSimpleName() + ": " + e.getMessage(), null);
            }
        }


        // -----------------------------------------------------
        // Ktor (Kotlin library, called from Java with runBlocking)
        // -----------------------------------------------------

        // One shared client (creating a client per download would leak threads).
        private static HttpClient ktorClient;

        private static synchronized HttpClient getKtorClient() {

            if (ktorClient == null) {
                // Uses whichever Ktor engine you added (CIO or OkHttp).
                ktorClient = HttpClientJvmKt.HttpClient(config -> Unit.INSTANCE);
            }

            return ktorClient;
        }

        private Result viaKtor(final String url) throws InterruptedException {

            final HttpClient client = getKtorClient();

            long start = System.nanoTime();

            // Ktor calls are "suspend" functions, so we run them with runBlocking.
            // (We are already on a background thread, so blocking is fine.)
            final HttpResponse response = BuildersKt.<HttpResponse>runBlocking(
                    EmptyCoroutineContext.INSTANCE,
                    (scope, continuation) -> io.ktor.client.request.BuildersKt.get(
                            client, url, request -> Unit.INSTANCE, continuation)
            );

            // Ktor has already received the whole body when get() returns,
            // so the time measured here is the full download time.
            int code = response.getStatus().getValue();
            boolean ok = code >= 200 && code < 300;

            // Number of bytes = Content-Length header sent by the server.
            long bytes = -1;

            try {
                String length = response.getHeaders().get("Content-Length");

                if (length != null) {
                    bytes = Long.parseLong(length.trim());
                }

            } catch (NumberFormatException ignored) {
                // keep -1 (unknown)
            }

            return new Result(ok, elapsedMs(start), code,
                    ok ? "OK" : "HTTP " + code, null, ok ? bytes : 0);
        }


        // =====================================================
        // HELPERS
        // =====================================================

        private static byte[] readAll(InputStream in) throws IOException {

            try (InputStream stream = in) {

                ByteArrayOutputStream out = new ByteArrayOutputStream();
                byte[] buffer = new byte[8192];
                int read;

                while ((read = stream.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }

                return out.toByteArray();
            }
        }

        private static long elapsedMs(long startNanos) {
            return (System.nanoTime() - startNanos) / 1_000_000;
        }

        // Success means an HTTP 2xx answer from the server.
        private static Result finish(long startNanos, int code, byte[] data) {

            boolean ok = code >= 200 && code < 300 && data != null;

            String message = "OK";

            if (!ok && data != null) {
                // Keep error text short (it is shown in a toast).
                message = new String(data, java.nio.charset.StandardCharsets.UTF_8);
                if (message.length() > 120) {
                    message = message.substring(0, 120);
                }
            }

            return new Result(ok, elapsedMs(startNanos), code, message, ok ? data : null);
        }
    }

}