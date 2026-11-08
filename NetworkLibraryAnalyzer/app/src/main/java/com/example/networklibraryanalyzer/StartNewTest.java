package com.example.networklibraryanalyzer;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;


import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.android.volley.VolleyError;
import com.android.volley.DefaultRetryPolicy;
import com.google.gson.Gson;

import okhttp3.*;
import java.io.IOException;
import java.util.Locale;

public class StartNewTest extends AppCompatActivity {

    Button btnStartTest;
    TextView txtRes;
    Spinner libraryType,methodType;
    ProgressBar progressBar;



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

        btnStartTest = findViewById(R.id.btnStartTest);
        txtRes = findViewById(R.id.txtResponse);
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

                    startLoading();

                    String api = "http://192.168.59.40:8080/info";
                    String api2 = "http://192.168.59.40:8080/student";

                    switch (selectedLibrary) {
                        case "HttpURLConnection":
                            callUsingHttpURLConnection(api2);
                            break;

                        case "OkHttp":
                            callUsingOkHttp(api2);
                            break;

                        case "Retrofit":
                            callUsingRetrofit(api);
                            break;

                        case "Volley":
                            callUsingVolley(api);
                            break;
                    }
                }


                private void callUsingVolley(String api) {

                    RequestQueue queue = Volley.newRequestQueue(StartNewTest.this);

                    long startTime = System.nanoTime();

                    StringRequest request = new StringRequest(
                            Request.Method.GET,
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


                private void callUsingRetrofit(String api) {
                }

                private void callUsingOkHttp(String api) {

                    OkHttpClient client = new OkHttpClient();

                    okhttp3.Request request = new okhttp3.Request.Builder()
                            .url(api)
                            .get()
                            .build();

                    long startTime = System.nanoTime();

                    client.newCall(request).enqueue(new Callback() {

                        @Override
                        public void onFailure(Call call, IOException e) {
                            e.printStackTrace();
                            finishTest();
                        }

                        @Override
                        public void onResponse(Call call, Response response)
                                throws IOException {

                            long start = startTime;
                            int statusCode = response.code();
                            String responseString = "";

                            try (Response res = response) {

                                if (res.body() != null) {
                                    responseString = res.body().string();
                                }

                                final String finalResponseString = responseString;
                                long endTime = System.nanoTime();

                                double responseTime =
                                        (endTime - start) / 1_000_000.0;

                                if (res.isSuccessful()) {

                                    Student student = new Gson().fromJson(
                                            responseString, Student.class
                                    );

                                    String result =
                                            "Network Library: OkHttp\n\n" +
                                                    "HTTP Method: GET\n\n" +
                                                    "API URL: /info\n\n" +
                                                    "Status Code: " + statusCode + "\n\n" +
                                                    String.format(
                                                            java.util.Locale.US,
                                                            "Response Time: %.2f ms\n\n",
                                                            responseTime
                                                    ) +
                                                    "Student Information\n" +
                                                    "Student ID: "  + "\n" +
                                                    "Name: " + student.getName() + "\n" +
                                                    "Age: " + student.getAge() + "\n" +
                                                    "Course: " ;

                                    runOnUiThread(() -> txtRes.setText(result));

                                } else {
                                    runOnUiThread(() ->
                                            txtRes.setText(
                                                    "HTTP Error: " + statusCode +
                                                            "\n" + finalResponseString
                                            )
                                    );
                                }
                            } catch (Exception e) {
                                runOnUiThread(() ->
                                        txtRes.setText("Error: " + e.getMessage())
                                );
                            } finally {
                                finishTest();
                            }
                        }
                    });
                }

                private void callUsingHttpURLConnection(String api) {

                    // Show loading and disable the button on the UI thread


                    String selectedLibrary =
                            libraryType.getSelectedItem().toString();

                    new Thread(() -> {

                        HttpURLConnection connection = null;

                        try {
                            Log.d("TAG", "Starting HttpURLConnection request");

                            long startTime = System.nanoTime();

                            URL url = new URL(api);

                            connection = (HttpURLConnection) url.openConnection();
                            connection.setRequestMethod("GET");
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
                                             new BufferedReader(
                                                     new InputStreamReader(stream))) {

                                    String line;

                                    while ((line = reader.readLine()) != null) {
                                        response.append(line).append("\n");
                                    }
                                }
                            }

                            long endTime = System.nanoTime();

                            double responseTime =
                                    (endTime - startTime) / 1_000_000.0;

                            String responseString = response.toString();


                            Gson gson = new Gson();

                            Student student = gson.fromJson(responseString, Student.class);

                            String result =
                                    "Network Library: " + selectedLibrary + "\n\n" +
                                            "HTTP Method: GET\n\n" +
                                            "API URL: /info\n\n" +
                                            "Status Code: " + statusCode + "\n\n" +
                                            String.format(
                                                    java.util.Locale.US,
                                                    "Response Time: %.2f ms\n\n",
                                                    responseTime
                                            ) +
                                            "Student Information\n" +
                                            "------------------------\n" +
                                            "Student ID: "  + "\n" +
                                            "Name: " + student.getName() + "\n" +
                                            "Age: " + student.getAge() + "\n" +
                                            "Course: ";


                            runOnUiThread(() -> txtRes.setText(result));

                        } catch (Exception e) {

                            Log.e("HTTP_ERROR", "API request failed", e);

                            runOnUiThread(() ->
                                    txtRes.setText(
                                            "Error: " +
                                                    e.getClass().getSimpleName() +
                                                    "\n" + e.getMessage()
                                    )
                            );

                        } finally {

                            if (connection != null) {
                                connection.disconnect();
                            }

                            runOnUiThread(() -> {
                                finishTest();
                            });
                        }

                    }).start();
                }

            });
        }
    }
