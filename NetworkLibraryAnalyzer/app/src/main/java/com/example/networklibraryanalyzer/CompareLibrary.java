
package com.example.networklibraryanalyzer;

import android.os.Bundle;
import android.os.SystemClock;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ProgressBar;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;



public class CompareLibrary extends AppCompatActivity {

    private Spinner spinnerMethod, spinnerRuns;
    private EditText editEndpoint;
    private Button btnRunComparison;

    private android.widget.TableLayout tableResults;
    private TextView txtFastestLibrary;
    private TextView txtFastestTime;
    private TextView txtOverallAverage;

    private final String[] libraries = {
            "HttpURLConnection",
            "OkHttp",
            "Volley",
            "Retrofit",
            "Ktor"
    };

    private final Map<String, TextView> timeViews =
            new HashMap<>();

    private final Map<String, TextView> responseViews =
            new HashMap<>();

    private final Map<String, ProgressBar> progressBars =
            new HashMap<>();

    private final Map<String, LibraryStats> statsMap =
            new ConcurrentHashMap<>();

    private AtomicInteger completed;
    private int totalRequests;

    // Stores results for each library
    private static class LibraryStats {

        long totalTime = 0;
        int count = 0;
        int successCount = 0;

        String lastResponse = "Waiting for response...";


        private int getStatusCode(String response) {

            try {

                java.util.regex.Matcher matcher =
                        java.util.regex.Pattern.compile(
                                "Status Code:\\s*(\\d{3})"
                        ).matcher(response);

                if (matcher.find()) {
                    return Integer.parseInt(matcher.group(1));
                }

            } catch (Exception e) {
                Log.e("COMPARE", "Status parsing error", e);
            }

            return -1;
        }

        synchronized void addResult(String response, long time) {

            totalTime += time;
            count++;

            lastResponse = response;

            int statusCode = getStatusCode(response);

            if (statusCode >= 200 && statusCode < 300) {
                successCount++;
            }
        }

        synchronized long getAverage() {

            if (count == 0) return 0;

            return totalTime / count;
        }

        synchronized int getCount() {
            return count;
        }

        synchronized String getResponse() {
            return lastResponse;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);

        setContentView(R.layout.activity_compare_library);

        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.main), (v, insets) -> {

                    Insets systemBars = insets.getInsets(
                            WindowInsetsCompat.Type.systemBars()
                    );

                    v.setPadding(
                            systemBars.left,
                            systemBars.top,
                            systemBars.right,
                            systemBars.bottom
                    );

                    return insets;
                }
        );

        // Initialize views

        tableResults = findViewById(
                R.id.tableComparisonResults
        );

        spinnerMethod = findViewById(
                R.id.spinnerCompareMethod
        );

        spinnerRuns = findViewById(
                R.id.spinnerCompareRuns
        );

        editEndpoint = findViewById(
                R.id.editCompareEndpoint
        );

        btnRunComparison = findViewById(
                R.id.btnRunComparison
        );

        txtFastestLibrary = findViewById(
                R.id.txtFastestLibrary
        );

        txtFastestTime = findViewById(
                R.id.txtFastestTime
        );

        txtOverallAverage = findViewById(
                R.id.txtOverallAverage
        );

        // Initialize library UI

        initializeLibraryViews();

        // HTTP method spinner

        String[] methods = {
                "GET", "POST", "PUT", "DELETE"
        };

        ArrayAdapter<String> methodAdapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_item,
                        methods
                );

        methodAdapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        spinnerMethod.setAdapter(methodAdapter);

        // Request count spinner

        String[] runOptions = {
                "5", "10", "30"
        };

        ArrayAdapter<String> runsAdapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_item,
                        runOptions
                );

        runsAdapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        spinnerRuns.setAdapter(runsAdapter);

        // Run comparison button

        btnRunComparison.setOnClickListener(v -> {

            String method =
                    spinnerMethod.getSelectedItem().toString();

            String endpoint =
                    editEndpoint.getText().toString().trim();

            int runs = Integer.parseInt(
                    spinnerRuns.getSelectedItem().toString()
            );

            if (endpoint.isEmpty()) {

                editEndpoint.setError(
                        "Enter an API endpoint"
                );

                return;
            }

            if (!endpoint.startsWith("/")) {
                endpoint = "/" + endpoint;
            }

            startCompareTest(endpoint, method, runs);

        });
    }

    // Connect XML views with Java



    private void updateComparisonTable() {

        // Keep the first row as the header
        tableResults.removeViews(
                1,
                Math.max(0, tableResults.getChildCount() - 1)
        );

        for (String library : libraries) {

            LibraryStats stats = statsMap.get(library);

            if (stats == null) continue;

            android.widget.TableRow row =
                    new android.widget.TableRow(this);

            long average = stats.getAverage();

            double successRate =
                    stats.getCount() == 0
                            ? 0
                            : stats.successCount * 100.0
                              / stats.getCount();

            addTableCell(
                    row,
                    library
            );

            addTableCell(
                    row,
                    average + " ms"
            );

            addTableCell(
                    row,
                    String.format(
                            java.util.Locale.US,
                            "%.1f%%",
                            successRate
                    )
            );

            tableResults.addView(row);
        }
    }

    private void addTableCell(
            android.widget.TableRow row,
            String value) {

        android.widget.TextView text =
                new android.widget.TextView(this);

        text.setText(value);

        text.setTextColor(
                android.graphics.Color.rgb(70, 80, 100)
        );

        text.setTextSize(12);

        text.setPadding(12, 12, 12, 12);

        row.addView(text);
    }


    private void initializeLibraryViews() {

        timeViews.put(
                "HttpURLConnection",
                findViewById(R.id.txtHttpTime)
        );

        timeViews.put(
                "OkHttp",
                findViewById(R.id.txtOkHttpTime)
        );

        timeViews.put(
                "Volley",
                findViewById(R.id.txtVolleyTime)
        );

        timeViews.put(
                "Retrofit",
                findViewById(R.id.txtRetrofitTime)
        );

        timeViews.put(
                "Ktor",
                findViewById(R.id.txtKtorTime)
        );

        responseViews.put(
                "HttpURLConnection",
                findViewById(R.id.txtHttpResponse)
        );

        responseViews.put(
                "OkHttp",
                findViewById(R.id.txtOkHttpResponse)
        );

        responseViews.put(
                "Volley",
                findViewById(R.id.txtVolleyResponse)
        );

        responseViews.put(
                "Retrofit",
                findViewById(R.id.txtRetrofitResponse)
        );

        responseViews.put(
                "Ktor",
                findViewById(R.id.txtKtorResponse)
        );

        progressBars.put(
                "HttpURLConnection",
                findViewById(R.id.progressHttp)
        );

        progressBars.put(
                "OkHttp",
                findViewById(R.id.progressOkHttp)
        );

        progressBars.put(
                "Volley",
                findViewById(R.id.progressVolley)
        );

        progressBars.put(
                "Retrofit",
                findViewById(R.id.progressRetrofit)
        );

        progressBars.put(
                "Ktor",
                findViewById(R.id.progressKtor)
        );
    }

    // Start comparison

    private void startCompareTest(
            String endpoint,
            String method,
            int runs) {

        String baseURL = "http://192.168.1.25:8080";

        String api = baseURL + endpoint;

        // Reset previous results

        statsMap.clear();

        completed = new AtomicInteger(0);

        totalRequests = 5 * runs;

        for (String library : libraries) {

            statsMap.put(
                    library,
                    new LibraryStats()
            );

            timeViews.get(library).setText("-- ms");

            responseViews.get(library).setText(
                    "Testing..."
            );

            progressBars.get(library).setProgress(0);
        }

        txtFastestLibrary.setText("Testing...");
        txtFastestTime.setText("Calculating...");
        txtOverallAverage.setText("-- ms");

        btnRunComparison.setEnabled(false);

        btnRunComparison.setText("Comparing...");

        LibrariesMethods methods = new LibrariesMethods();

        // Start all five libraries

        for (int i = 0; i < runs; i++) {

            callHttp(methods, api, method);

            callOkHttp(methods, api, method);

            callVolley(methods, api, method);

            callRetrofit(methods, api, method);

            callKtor(methods, api, method);
        }
    }

    // HttpURLConnection

    private void callHttp(
            LibrariesMethods methods,
            String api,
            String method) {

        long startTime = SystemClock.elapsedRealtime();

        methods.callUsingHttpURLConnection(
                api,
                method,
                result -> onCompareResult(
                        "HttpURLConnection",
                        result,
                        startTime
                )
        );
    }

    // OkHttp

    private void callOkHttp(
            LibrariesMethods methods,
            String api,
            String method) {

        long startTime = SystemClock.elapsedRealtime();

        methods.callUsingOkHttp(
                api,
                method,
                result -> onCompareResult(
                        "OkHttp",
                        result,
                        startTime
                )
        );
    }

    // Volley

    private void callVolley(
            LibrariesMethods methods,
            String api,
            String method) {

        long startTime = SystemClock.elapsedRealtime();

        methods.callUsingVolley(
                api,
                method,
                CompareLibrary.this,
                result -> onCompareResult(
                        "Volley",
                        result,
                        startTime
                )
        );
    }

    // Retrofit

    private void callRetrofit(
            LibrariesMethods methods,
            String api,
            String method) {

        long startTime = SystemClock.elapsedRealtime();

        methods.callUsingRetrofit(
                api,
                method,
                result -> onCompareResult(
                        "Retrofit",
                        result,
                        startTime
                )
        );
    }

    // Ktor

    private void callKtor(
            LibrariesMethods methods,
            String api,
            String method) {

        long startTime = SystemClock.elapsedRealtime();

        methods.callUsingKtor(
                api,
                method,
                result -> onCompareResult(
                        "Ktor",
                        result,
                        startTime
                )
        );
    }

    // Receive individual library response

    private void onCompareResult(
            String library,
            String result,
            long startTime) {

        long responseTime =
                SystemClock.elapsedRealtime() - startTime;

        LibraryStats stats = statsMap.get(library);

        if (stats == null) return;

        stats.addResult(result, responseTime);

        runOnUiThread(() -> {

            // Update individual library UI

            updateLibraryUI(library, stats);

            int count = completed.incrementAndGet();

            // All requests completed

            if (count == totalRequests) {

                showComparisonResults();

                btnRunComparison.setEnabled(true);

                btnRunComparison.setText(
                        "Run Comparison"
                );
            }

        });
    }

    // Update individual library result

    private void updateLibraryUI(
            String library,
            LibraryStats stats) {

        long average = stats.getAverage();

        // Update average time

        timeViews.get(library).setText(
                average + " ms"
        );

        // Update API response

        String response = stats.getResponse();

        responseViews.get(library).setText(
                response == null ? "No response" : response
        );

        // Update progress bar

        ProgressBar progress = progressBars.get(library);

        int max = 200;

        progress.setMax(max);

        int progressValue = (int) Math.min(
                average,
                max
        );

        progress.setProgress(progressValue);
    }

    // Calculate final comparison

    private void showComparisonResults() {

        String fastestLibrary = "";

        long fastestTime = Long.MAX_VALUE;

        long totalTime = 0;

        int totalCount = 0;

        for (String library : libraries) {

            LibraryStats stats = statsMap.get(library);

            long average = stats.getAverage();

            if (stats.getCount() == 0) continue;

            // Find fastest library

            if (average < fastestTime) {

                fastestTime = average;

                fastestLibrary = library;
            }

            totalTime += stats.totalTime;

            totalCount += stats.getCount();
        }

        // Fastest library

        if (!fastestLibrary.isEmpty()) {

            txtFastestLibrary.setText(
                    fastestLibrary
            );

            txtFastestTime.setText(
                    fastestTime + " ms average"
            );
        }

        // Overall average

        if (totalCount > 0) {

            long overallAverage =
                    totalTime / totalCount;

            txtOverallAverage.setText(
                    overallAverage + " ms"
            );
        }

        Toast.makeText(
                this,
                "Comparison completed successfully",
                Toast.LENGTH_SHORT
        ).show();

        updateComparisonTable();
    }
}
