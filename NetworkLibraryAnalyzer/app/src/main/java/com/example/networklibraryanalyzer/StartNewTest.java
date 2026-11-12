package com.example.networklibraryanalyzer;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
<<<<<<< HEAD

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Locale;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.material.chip.Chip;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Response;
import retrofit2.Retrofit;
=======
import com.google.android.material.chip.Chip;
import okhttp3.*;

>>>>>>> 183bfca168f599f2f2cbd6742469bc2b0443a59c

public class StartNewTest extends AppCompatActivity {

    // EC2 server address (no trailing slash). Change only here.
    private static final String BASE_URL = "http://3.107.213.99:8080";

    private static final String JSON_BODY =
            "{\"name\":\"Rahul\",\"age\":22,\"course\":\"Android\"}";

    Button btnStartTest;
<<<<<<< HEAD
    TextView txtRes;
    Spinner libraryType, methodType;
=======
    TextView txtRes,txtStatus;
    Spinner libraryType,methodType;
>>>>>>> 183bfca168f599f2f2cbd6742469bc2b0443a59c
    ProgressBar progressBar;
    EditText editEndPoint;

<<<<<<< HEAD
=======

>>>>>>> 183bfca168f599f2f2cbd6742469bc2b0443a59c
    private void startLoading() {
        progressBar.setVisibility(View.VISIBLE);
        btnStartTest.setEnabled(false);
        txtRes.setVisibility(View.VISIBLE);
        txtRes.setText("Loading...");
    }

    private void finishTest() {
        runOnUiThread(() -> {
            progressBar.setVisibility(View.GONE);
            btnStartTest.setEnabled(true);
        });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_start_new_test);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        int[] chipIds = {
                R.id.chipInfo,
                R.id.chipStudent,
                R.id.chipStudents,
                R.id.chipStudentId,
                R.id.chipUsers,
        };

        editEndPoint = findViewById(R.id.editEndpoint);
        editEndPoint.setText("/info");
        for (int id : chipIds) {
            Chip currentChip = findViewById(id);
            currentChip.setOnClickListener(v -> {
                String endpoint = ((Chip) v).getText().toString();
                editEndPoint.setText(endpoint);
                editEndPoint.setSelection(editEndPoint.getText().length());
            });
        }

        btnStartTest = findViewById(R.id.btnStartTest);
        txtRes = findViewById(R.id.txtResponse);
        txtStatus = findViewById(R.id.txtStatus);
        txtRes.setVisibility(View.GONE);

        libraryType = findViewById(R.id.spinnerLibrary);
        methodType = findViewById(R.id.spinnerMethod);
        View jsonBody = findViewById(R.id.jsonBody);
        progressBar = findViewById(R.id.progressBar);

        // Hide the JSON body when method type is GET.
        methodType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String methodT = parent.getItemAtPosition(position).toString();
                if (methodT.equalsIgnoreCase("GET")) {
                    jsonBody.setVisibility(View.GONE);
                } else {
                    jsonBody.setVisibility(View.VISIBLE);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // No action needed
            }
        });

        btnStartTest.setOnClickListener(v -> {
            String selectedLibrary = libraryType.getSelectedItem().toString();
            String selectedMethod = methodType.getSelectedItem().toString();

            String endpoint = editEndPoint.getText().toString().trim();

            if (endpoint.isEmpty()) {
                editEndPoint.setError("Enter API endpoint");
                return;
            }

            if (!endpoint.startsWith("/")) {
                endpoint = "/" + endpoint;
            }

            // Start loading only after validation passes.
            startLoading();

            String api = BASE_URL + endpoint;

            switch (selectedLibrary) {
                case "HttpURLConnection":
                    callUsingHttpURLConnection(api, selectedMethod);
                    break;
                case "OkHttp":
                    callUsingOkHttp(api, selectedMethod);
                    break;
                case "Retrofit":
                    callUsingRetrofit(api, selectedMethod);
                    break;
                case "Volley":
                    callUsingVolley(api, selectedMethod);
                    break;
                case "Ktor":
                    callUsingKtor(api, selectedMethod);
                    break;
                default:
                    txtRes.setText("Unknown library: " + selectedLibrary);
                    finishTest();
                    break;
            }
        });
    }

    // ------------------------------------------------------------------ Volley
    private void callUsingVolley(String api, String method) {

        RequestQueue queue = Volley.newRequestQueue(StartNewTest.this);

        long startTime = System.nanoTime();

        int volleyMethod;

        switch (method.toUpperCase(Locale.ROOT)) {
            case "GET": volleyMethod = Request.Method.GET; break;
            case "POST": volleyMethod = Request.Method.POST; break;
            case "PUT": volleyMethod = Request.Method.PUT; break;
            case "DELETE": volleyMethod = Request.Method.DELETE; break;
            case "PATCH": volleyMethod = Request.Method.PATCH; break;
            case "HEAD": volleyMethod = Request.Method.HEAD; break;
            case "OPTIONS": volleyMethod = Request.Method.OPTIONS; break;
            default:
                txtRes.setText("Unsupported HTTP method");
                finishTest();
                return;
        }

        StringRequest request = new StringRequest(
                volleyMethod,
                api,

                response -> {
                    long endTime = System.nanoTime();
                    double responseTime = (endTime - startTime) / 1_000_000.0;

                    String result =
                            "Network Library: Volley\n\n" +
                                    "HTTP Method: " + method + "\n\n" +
                                    "API URL: " + api + "\n\n" +
                                    String.format(Locale.US,
                                            "Response Time: %.2f ms\n\n", responseTime) +
                                    "API Response:\n" + response;

                    txtRes.setText(result);
                    finishTest();
                },

                error -> {
                    String message = "Volley request failed.";

                    if (error.networkResponse != null) {
                        message += "\nStatus Code: " + error.networkResponse.statusCode;
                    } else if (error.getMessage() != null) {
                        message += "\n" + error.getMessage();
                    } else {
                        message += "\n" + error.getClass().getSimpleName();
                    }

                    txtRes.setText(message);
                    finishTest();
                }
        );

        // Avoid using a cached response for this performance test.
        request.setShouldCache(false);

        request.setRetryPolicy(new DefaultRetryPolicy(
                5000,
                0,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT
        ));

        queue.add(request);
    }

    // ---------------------------------------------------------------- Retrofit
    private void callUsingRetrofit(String api, String method) {

        // Retrofit needs a base URL that ends with "/".
        String retrofitBase = BASE_URL + "/";

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(retrofitBase)
                .build();

        RetrofitApi retrofitApi = retrofit.create(RetrofitApi.class);

        // Relative endpoint without leading slash, e.g. "info"
        String endpoint = api.replace(retrofitBase, "");

        RequestBody body = RequestBody.create(
                MediaType.parse("application/json; charset=utf-8"),
                JSON_BODY
        );

        retrofit2.Call<ResponseBody> call;

        switch (method.toUpperCase(Locale.ROOT)) {
            case "GET":
                call = retrofitApi.get(endpoint);
                break;
            case "POST":
                call = retrofitApi.post(endpoint, body);
                break;
            case "PUT":
                call = retrofitApi.put(endpoint, body);
                break;
            case "PATCH":
                call = retrofitApi.patch(endpoint, body);
                break;
            case "DELETE":
                call = retrofitApi.delete(endpoint);
                break;
            case "HEAD":
                call = retrofitApi.head(endpoint);
                break;
            case "OPTIONS":
                call = retrofitApi.options(endpoint);
                break;
            default:
                runOnUiThread(() -> txtRes.setText("Unsupported HTTP method"));
                finishTest();
                return;
        }

        long startTime = System.nanoTime();

        call.enqueue(new retrofit2.Callback<ResponseBody>() {

            @Override
            public void onResponse(retrofit2.Call<ResponseBody> call,
                                   Response<ResponseBody> response) {

                long endTime = System.nanoTime();
                double responseTime = (endTime - startTime) / 1_000_000.0;

                String responseString = "";

                try {
                    if (response.body() != null) {
                        responseString = response.body().string();
                    } else if (response.errorBody() != null) {
                        responseString = response.errorBody().string();
                    }
                } catch (IOException e) {
                    responseString = "Error reading response: " + e.getMessage();
                }

                String result =
                        "Network Library: Retrofit\n\n" +
                                "HTTP Method: " + method + "\n\n" +
                                "API URL: " + api + "\n\n" +
                                "Status Code: " + response.code() + "\n\n" +
                                String.format(Locale.US,
                                        "Response Time: %.2f ms\n\n", responseTime) +
                                "API Response:\n" + responseString;

                runOnUiThread(() -> txtRes.setText(result));
                finishTest();
            }

            @Override
            public void onFailure(retrofit2.Call<ResponseBody> call, Throwable t) {
                runOnUiThread(() -> txtRes.setText("Retrofit Error: " + t.getMessage()));
                finishTest();
            }
        });
    }

    // ------------------------------------------------------------------ OkHttp
    private void callUsingOkHttp(String api, String method) {

        OkHttpClient client = new OkHttpClient();

        String upperMethod = method.toUpperCase(Locale.ROOT);

        RequestBody requestBody = null;

        if (upperMethod.equals("POST") || upperMethod.equals("PUT") || upperMethod.equals("PATCH")) {
            MediaType mediaType = MediaType.parse("application/json; charset=utf-8");
            requestBody = RequestBody.create(JSON_BODY, mediaType);
        }

        okhttp3.Request request;
        try {
            request = new okhttp3.Request.Builder()
                    .url(api)
                    .method(upperMethod, requestBody)
                    .build();
        } catch (Exception e) {
            txtRes.setText("OkHttp Error: " + e.getMessage());
            finishTest();
            return;
        }

        long startTime = System.nanoTime();

        client.newCall(request).enqueue(new okhttp3.Callback() {

            @Override
            public void onFailure(okhttp3.Call call, IOException e) {
                e.printStackTrace();
                runOnUiThread(() -> txtRes.setText("OkHttp Error: " + e.getMessage()));
                finishTest();
            }

            @Override
            public void onResponse(okhttp3.Call call, okhttp3.Response response) {

                int statusCode = response.code();
                String responseString = "";

                try (okhttp3.Response res = response) {

                    if (res.body() != null) {
                        responseString = res.body().string();
                    }

                    final String finalResponseString = responseString;
                    long endTime = System.nanoTime();
                    double responseTime = (endTime - startTime) / 1_000_000.0;

<<<<<<< HEAD
                    if (res.isSuccessful()) {
                        String result =
                                "Network Library: OkHttp\n\n" +
                                        "HTTP Method: " + method + "\n\n" +
                                        "Status Code: " + statusCode + "\n\n" +
                                        String.format(Locale.US,
                                                "Response Time: %.2f ms\n\n", responseTime) +
                                        "API Response:\n" + finalResponseString;

                        runOnUiThread(() -> txtRes.setText(result));
                    } else {
                        runOnUiThread(() ->
                                txtRes.setText("HTTP Error: " + statusCode +
                                        "\n" + finalResponseString));
                    }
                } catch (Exception e) {
                    runOnUiThread(() -> txtRes.setText("Error: " + e.getMessage()));
                } finally {
                    finishTest();
                }
            }
        });
    }

    // -------------------------------------------------------- HttpURLConnection
    private void callUsingHttpURLConnection(String api, String method) {
        new Thread(() -> {

            HttpURLConnection connection = null;

            try {
                Log.d("TAG", "Starting HttpURLConnection request");

                long startTime = System.nanoTime();

                URL url = new URL(api);

                connection = (HttpURLConnection) url.openConnection();
                connection.setRequestMethod(method.toUpperCase(Locale.ROOT));
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);

                int statusCode = connection.getResponseCode();

                InputStream stream =
                        (statusCode >= 200 && statusCode < 400)
                                ? connection.getInputStream()
                                : connection.getErrorStream();

                StringBuilder response = new StringBuilder();

                if (stream != null) {
                    try (BufferedReader reader =
                                 new BufferedReader(new InputStreamReader(stream))) {

                        String line;
                        while ((line = reader.readLine()) != null) {
                            response.append(line).append("\n");
                        }
=======
                    LibrariesMethods methods = new LibrariesMethods();

                    switch (selectedLibrary) {
                        case "HttpURLConnection":
                            methods.callUsingHttpURLConnection(api, selectedMethod, this::displayResponse);
                            break;

                        case "OkHttp":
                            methods.callUsingOkHttp(api, selectedMethod, this::displayResponse);
                            break;

                        case "Retrofit":
                            methods.callUsingRetrofit(api, selectedMethod, this::displayResponse);
                            break;

                        case "Volley":
                            methods.callUsingVolley(api, selectedMethod, StartNewTest.this, this::displayResponse);
                            break;

                        case "Ktor":
                            methods.callUsingKtor(api, selectedMethod,this::displayResponse);
                            break;
>>>>>>> 183bfca168f599f2f2cbd6742469bc2b0443a59c
                    }
                }
                private void displayResponse(String response) {

<<<<<<< HEAD
                long endTime = System.nanoTime();
                double responseTime = (endTime - startTime) / 1_000_000.0;

                String result =
                        "Network Library: HttpURLConnection\n\n" +
                                "HTTP Method: " + method + "\n\n" +
                                "Status Code: " + statusCode + "\n\n" +
                                String.format(Locale.US,
                                        "Response Time: %.2f ms\n\n", responseTime) +
                                "API Response:\n" + response;

                runOnUiThread(() -> txtRes.setText(result));

            } catch (Exception e) {

                Log.e("HTTP_ERROR", "API request failed", e);

                runOnUiThread(() ->
                        txtRes.setText("Error: " + e.getClass().getSimpleName() +
                                "\n" + e.getMessage()));
=======
                    runOnUiThread(() -> {

                        txtRes.setText(response);

                        if (response.contains("Error") ||
                                response.contains("Timeout")) {

                            txtStatus.setText("FAILED");
                            txtStatus.setTextColor(Color.parseColor("#DC2626"));

                        } else {

<<<<<<< HEAD
                            txtStatus.setText("COMPLETED");
                            txtStatus.setTextColor(Color.parseColor("#15803D"));
=======
                    switch (method.toUpperCase()) {
                        case "GET": volleyMethod = Request.Method.GET; break;
                        case "POST": volleyMethod = Request.Method.POST; break;
                        case "PUT": volleyMethod = Request.Method.PUT; break;
                        case "DELETE": volleyMethod = Request.Method.DELETE; break;
                        case "PATCH": volleyMethod = Request.Method.PATCH; break;
                        case "HEAD": volleyMethod = Request.Method.HEAD; break;
                        case "OPTIONS": volleyMethod = Request.Method.OPTIONS; break;
                        default: throw new IllegalArgumentException("Unsupported method");
                    }
>>>>>>> 183bfca168f599f2f2cbd6742469bc2b0443a59c

            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
                finishTest();
            }

        }).start();
    }

    // -------------------------------------------------------------------- Ktor
    private void callUsingKtor(String api, String method) {

        KtorHelper.execute(
                api,
                method,
                JSON_BODY,
                new KtorHelper.ResultCallback() {

                    @Override
                    public void onResult(int statusCode,
                                         String response,
                                         double responseTime,
                                         String error) {

                        runOnUiThread(() -> {

                            if (error != null) {
                                txtRes.setText("Ktor Error: " + error);
                            } else {
                                String result =
                                        "Network Library: Ktor\n\n" +
                                                "HTTP Method: " + method + "\n\n" +
                                                "Status Code: " + statusCode + "\n\n" +
                                                String.format(Locale.US,
                                                        "Response Time: %.2f ms\n\n", responseTime) +
                                                "API Response:\n" + response;

                                txtRes.setText(result);
                            }

                            finishTest();
                        });
                    }
<<<<<<< HEAD
                }
        );
=======

                    long startTime = System.nanoTime();

                    call.enqueue(new retrofit2.Callback<ResponseBody>() {

                        @Override
                        public void onResponse(
                                retrofit2.Call<ResponseBody> call,
                                Response<ResponseBody> response) {

                            long endTime = System.nanoTime();

                            double responseTime =
                                    (endTime - startTime) / 1_000_000.0;

                            String responseString = "";

                            try {
                                if (response.body() != null) {
                                    responseString = response.body().string();
                                } else if (response.errorBody() != null) {
                                    responseString = response.errorBody().string();
                                }

                            } catch (IOException e) {
                                responseString = "Error reading response: " + e.getMessage();
                            }

                            String result =
                                    "Network Library: Retrofit\n\n" +
                                            "HTTP Method: " + method + "\n\n" +
                                            "API URL: " + api + "\n\n" +
                                            "Status Code: " + response.code() + "\n\n" +
                                            String.format(Locale.US,
                                                    "Response Time: %.2f ms\n\n",
                                                    responseTime) +
                                            "API Response:\n" + responseString;

                            runOnUiThread(() -> txtRes.setText(result));

                            finishTest();
>>>>>>> 8639bc917f4fba07aa7dd404a336f172cf23e210
                        }

                        finishTest();
                    });
                }
            });
        }
>>>>>>> 183bfca168f599f2f2cbd6742469bc2b0443a59c
    }
}