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
import com.google.android.material.chip.Chip;
import okhttp3.*;


public class StartNewTest extends AppCompatActivity {

    Button btnStartTest;
    TextView txtRes,txtStatus;
    Spinner libraryType,methodType;
    ProgressBar progressBar;
    Chip chip;
    EditText editEndPoint;


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

        // Hiding the Json body when Method Type is GET.
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


            btnStartTest.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    String selectedLibrary = libraryType.getSelectedItem().toString();
                    String selectedMethod = methodType.getSelectedItem().toString();

                    startLoading();

                    String baseURL = "http://192.168.1.25:8080";

                    String endpoint = editEndPoint.getText().toString().trim();

                    if (endpoint.isEmpty()) {
                        editEndPoint.setError("Enter API endpoint");
                        return;
                    }

                    if (!endpoint.startsWith("/")) {
                        endpoint = "/" + endpoint;
                    }

                    String api = baseURL + endpoint;

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
                    }
                }
                private void displayResponse(String response) {

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

                    StringRequest request = new StringRequest(
                            volleyMethod,
                            api,

                            response -> {
                                long endTime = System.nanoTime();

                                double responseTime =
                                        (endTime - startTime) / 1_000_000.0;

                                String result =
                                        "Network Library: Volley\n\n" +
                                                "HTTP Method: GET\n\n" +
                                                "API URL: /info\n\n" +
                                                "Status Code: 200 (successful response)\n\n" +
                                                String.format(
                                                        java.util.Locale.US,
                                                        "Response Time: %.2f ms\n\n",
                                                        responseTime
                                                ) +
                                                "API Response:\n" + response;

                                txtRes.setText(result);
                                finishTest();
                            },

                            error -> {
                                String message = "Volley request failed.";

                                if (error.networkResponse != null) {
                                    message += "\nStatus Code: " +
                                            error.networkResponse.statusCode;
                                } else if (error.getMessage() != null) {
                                    message += "\n" + error.getMessage();
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

                private void callUsingRetrofit(String api, String method) {

                    String baseUrl = "http://192.168.1.11:8080/";

                    Retrofit retrofit = new Retrofit.Builder()
                            .baseUrl(baseUrl)
                            .build();

                    RetrofitApi retrofitApi =
                            retrofit.create(RetrofitApi.class);

                    // Extract relative endpoint
                    String endpoint = api.replace(baseUrl, "");

                    String json = "{\"name\":\"Rahul\",\"age\":22,\"course\":\"Android\"}";

                    RequestBody body = RequestBody.create(
                            MediaType.parse("application/json; charset=utf-8"),
                            json
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
                            finishTest();
                            runOnUiThread(() ->
                                    txtRes.setText("Unsupported HTTP method")
                            );
                            return;
                    }

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
    }
