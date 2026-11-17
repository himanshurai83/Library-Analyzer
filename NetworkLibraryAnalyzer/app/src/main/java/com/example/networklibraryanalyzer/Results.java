
package com.example.networklibraryanalyzer;

import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class Results extends AppCompatActivity {

    private static final String BASE_URL =
            "http://3.107.213.99:8080";

    private static final String RESULTS_URL =
            BASE_URL + "/results";

    private LinearLayout container;
    private TextView fastestName;
    private TextView fastestTime;
    private TextView testedFile;

    private final Handler mainHandler =
            new Handler(Looper.getMainLooper());

    private static class ResultItem {
        String name;
        double timeMs;
        double speedMBps;
        double energyJ;
        double responseTimeMs;
        double successRate;

        ResultItem(String name, double timeMs,
                   double speedMBps, double energyJ,
                   double responseTimeMs, double successRate) {

            this.name = name;
            this.timeMs = timeMs;
            this.speedMBps = speedMBps;
            this.energyJ = energyJ;
            this.responseTimeMs = responseTimeMs;
            this.successRate = successRate;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_results);

        fastestName = findViewById(R.id.fastestName);
        fastestTime = findViewById(R.id.fastestTime);
        testedFile = findViewById(R.id.testedFile);
        container = findViewById(R.id.resultContainer);

        Button backButton = findViewById(R.id.backButton);

        backButton.setOnClickListener(v -> finish());

        String fileName =
                getIntent().getStringExtra("file_name");

        String jsonResponse =
                getIntent().getStringExtra("results_json");

        if (fileName != null) {
            testedFile.setText(fileName);
        }

        // First use results passed from the uploading page.
        // Otherwise request saved results from Spring Boot.
        if (jsonResponse != null && !jsonResponse.trim().isEmpty()) {

            displayResults(jsonResponse);

        } else {

            testedFile.setText(
                    fileName != null ? fileName : "Loading server results..."
            );

            fetchResultsFromBackend();
        }
    }

    // Connect to Spring Boot backend.
    private void fetchResultsFromBackend() {

        fastestName.setText("Loading...");
        fastestTime.setText("Connecting to backend server");

        new Thread(() -> {

            HttpURLConnection connection = null;

            try {

                URL url = new URL(RESULTS_URL);

                connection =
                        (HttpURLConnection) url.openConnection();

                connection.setRequestMethod("GET");
                connection.setConnectTimeout(15000);
                connection.setReadTimeout(15000);

                connection.setRequestProperty(
                        "Accept", "application/json"
                );

                int responseCode =
                        connection.getResponseCode();

                InputStream inputStream;

                if (responseCode >= 200 && responseCode < 300) {
                    inputStream = connection.getInputStream();
                } else {
                    inputStream = connection.getErrorStream();
                }

                if (inputStream == null) {
                    throw new Exception(
                            "Server returned HTTP " + responseCode
                    );
                }

                BufferedReader reader =
                        new BufferedReader(
                                new InputStreamReader(inputStream)
                        );

                StringBuilder response =
                        new StringBuilder();

                String line;

                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }

                reader.close();

                if (responseCode < 200 || responseCode >= 300) {
                    throw new Exception(
                            "Server error: HTTP " + responseCode
                    );
                }

                String jsonResponse = response.toString();

                mainHandler.post(() ->
                        displayResults(jsonResponse)
                );

            } catch (Exception e) {

                e.printStackTrace();

                mainHandler.post(() -> {
                    fastestName.setText("Connection Failed");
                    fastestTime.setText(e.getMessage());

                    showMessage(
                            "Could not connect to the backend. " +
                                    "Check your server and internet connection."
                    );
                });

            } finally {

                if (connection != null) {
                    connection.disconnect();
                }
            }

        }).start();
    }

    // Read results from JSON.
    private void displayResults(String jsonResponse) {

        try {

            List<ResultItem> results =
                    new ArrayList<>();

            JSONArray array;

            JSONObject root = null;

            if (jsonResponse.trim().startsWith("[")) {

                array = new JSONArray(jsonResponse);

            } else {

                root = new JSONObject(jsonResponse);

                if (!root.optBoolean("success", true)) {
                    showMessage("Backend returned unsuccessful results.");
                    fastestName.setText("Not available");
                    fastestTime.setText("--");
                    return;
                }

                array = root.optJSONArray("results");

                if (array == null) {
                    // Supports a direct Spring Boot list response
                    // wrapped under another common property.
                    array = root.optJSONArray("content");
                }

                if (array == null) {
                    throw new JSONException(
                            "Results array not found"
                    );
                }

                String responseFile =
                        root.optString("file_name", "");

                if (!responseFile.isEmpty()) {
                    testedFile.setText(responseFile);
                }
            }

            for (int i = 0; i < array.length(); i++) {

                JSONObject item =
                        array.getJSONObject(i);

                String libraryName =
                        item.optString(
                                "library",
                                item.optString(
                                        "libraryName",
                                        "Unknown Library"
                                )
                        );

                double time =
                        item.optDouble(
                                "time_ms",
                                item.optDouble("uploadTimeMs", -1)
                        );

                double speed =
                        item.optDouble("speed_mbps", -1);

                // Support the existing backend throughputKbps field.
                if (speed < 0) {
                    double kbps =
                            item.optDouble("throughputKbps", -1);

                    if (kbps >= 0) {
                        speed = kbps / 1024.0;
                    }
                }

                double energy =
                        item.optDouble("energy_j", -1);

                if (energy < 0) {
                    energy = item.optDouble(
                            "energy_consumption_j", -1
                    );
                }

                double responseTime =
                        item.optDouble("response_time_ms", -1);

                double successRate =
                        item.optDouble("success_rate", -1);

                if (successRate < 0) {
                    successRate =
                            item.optDouble("successRate", -1);
                }

                if (time < 0 ||
                        Double.isNaN(time) ||
                        Double.isInfinite(time)) {
                    continue;
                }

                results.add(new ResultItem(
                        libraryName,
                        time,
                        speed,
                        energy,
                        responseTime,
                        successRate
                ));
            }

            if (results.isEmpty()) {
                fastestName.setText("No Results");
                fastestTime.setText("--");
                showMessage("No valid library results found.");
                return;
            }

            showComparison(results);

        } catch (JSONException e) {

            e.printStackTrace();

            fastestName.setText("Invalid Data");
            fastestTime.setText("--");

            showMessage(
                    "Unable to read the server response. " +
                            "Please check your JSON format."
            );
        }
    }

    // Compare all network libraries.
    private void showComparison(List<ResultItem> results) {

        Collections.sort(
                results,
                (a, b) -> Double.compare(a.timeMs, b.timeMs)
        );

        ResultItem fastest = results.get(0);

        fastestName.setText(fastest.name);

        StringBuilder summary = new StringBuilder();

        summary.append(String.format(
                Locale.US,
                "Upload Time: %.1f ms",
                fastest.timeMs
        ));

        if (fastest.speedMBps >= 0) {
            summary.append(String.format(
                    Locale.US,
                    "\nThroughput: %.2f MB/s",
                    fastest.speedMBps
            ));
        }

        if (fastest.responseTimeMs >= 0) {
            summary.append(String.format(
                    Locale.US,
                    "\nResponse Time: %.1f ms",
                    fastest.responseTimeMs
            ));
        }

        if (fastest.successRate >= 0) {
            summary.append(String.format(
                    Locale.US,
                    "\nSuccess Rate: %.1f%%",
                    fastest.successRate
            ));
        }

        if (fastest.energyJ >= 0) {
            summary.append(String.format(
                    Locale.US,
                    "\nEnergy: %.3f J",
                    fastest.energyJ
            ));
        }

        fastestTime.setText(summary.toString());

        double lowestEnergy = Double.MAX_VALUE;
        String lowestEnergyName = null;

        for (ResultItem item : results) {

            if (item.energyJ >= 0 &&
                    item.energyJ < lowestEnergy) {

                lowestEnergy = item.energyJ;
                lowestEnergyName = item.name;
            }
        }

        container.removeAllViews();

        TextView heading = new TextView(this);

        heading.setText(
                "LIBRARY COMPARISON\n\n" +
                        "Fastest Upload: " + fastest.name +
                        (lowestEnergyName != null
                                ? "\nLowest Energy: " + lowestEnergyName
                                : "\nEnergy measurements not available")
        );

        heading.setTextSize(14);
        heading.setTypeface(null, Typeface.BOLD);
        heading.setTextColor(Color.rgb(31, 42, 68));
        heading.setPadding(4, 8, 4, 16);

        container.addView(heading);

        for (int i = 0; i < results.size(); i++) {

            container.addView(
                    buildRow(
                            i + 1,
                            results.get(i),
                            fastest.timeMs,
                            lowestEnergyName
                    )
            );
        }
    }

    // Create a comparison card.
    private LinearLayout buildRow(
            int rank,
            ResultItem item,
            double fastestTime,
            String lowestEnergyName) {

        LinearLayout card = new LinearLayout(this);

        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(14), dp(14), dp(14), dp(14));

        GradientDrawable background =
                new GradientDrawable();

        background.setColor(Color.WHITE);
        background.setCornerRadius(dp(14));
        background.setStroke(
                dp(1),
                Color.rgb(229, 234, 242)
        );

        card.setBackground(background);

        LinearLayout.LayoutParams cardParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );

        cardParams.bottomMargin = dp(12);

        card.setLayoutParams(cardParams);

        TextView name = new TextView(this);

        name.setText("#" + rank + "   " + item.name);
        name.setTextSize(16);
        name.setTypeface(null, Typeface.BOLD);
        name.setTextColor(Color.rgb(31, 42, 68));

        card.addView(name);

        if (rank == 1) {
            addText(card, "FASTEST UPLOAD", true);
        }

        if (lowestEnergyName != null &&
                lowestEnergyName.equals(item.name)) {
            addText(card, "LOWEST ENERGY CONSUMPTION", true);
        }

        ProgressBar bar = new ProgressBar(
                this,
                null,
                android.R.attr.progressBarStyleHorizontal
        );

        bar.setMax(100);

        int progress = (int) Math.round(
                fastestTime / item.timeMs * 100
        );

        bar.setProgress(
                Math.max(0, Math.min(100, progress))
        );

        LinearLayout.LayoutParams barParams =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        dp(7)
                );

        barParams.topMargin = dp(12);

        bar.setLayoutParams(barParams);

        card.addView(bar);

        addMetric(card, "Upload Time", formatMs(item.timeMs));
        addMetric(card, "Throughput", formatSpeed(item.speedMBps));
        addMetric(card, "Response Time", formatMs(item.responseTimeMs));
        addMetric(card, "Success Rate", formatPercent(item.successRate));
        addMetric(card, "Energy Consumption", formatEnergy(item.energyJ));

        return card;
    }

    private void addText(
            LinearLayout parent,
            String value,
            boolean bold) {

        TextView text = new TextView(this);

        text.setText(value);
        text.setTextColor(Color.rgb(21, 128, 61));
        text.setTextSize(11);

        if (bold) {
            text.setTypeface(null, Typeface.BOLD);
        }

        text.setPadding(0, dp(6), 0, dp(3));

        parent.addView(text);
    }

    private void addMetric(
            LinearLayout parent,
            String label,
            String value) {

        LinearLayout row = new LinearLayout(this);

        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout.LayoutParams params =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );

        params.topMargin = dp(8);

        row.setLayoutParams(params);

        TextView left = new TextView(this);

        left.setText(label);
        left.setTextColor(Color.rgb(100, 116, 139));
        left.setTextSize(12);

        left.setLayoutParams(
                new LinearLayout.LayoutParams(
                        0,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        1
                )
        );

        TextView right = new TextView(this);

        right.setText(value);
        right.setTextColor(Color.rgb(31, 42, 68));
        right.setTextSize(12);
        right.setTypeface(null, Typeface.BOLD);

        row.addView(left);
        row.addView(right);

        parent.addView(row);
    }

    private void showMessage(String message) {

        container.removeAllViews();

        TextView text = new TextView(this);

        text.setText(message);
        text.setTextSize(14);
        text.setTextColor(Color.rgb(100, 116, 139));
        text.setGravity(Gravity.CENTER);
        text.setPadding(dp(12), dp(24), dp(12), dp(24));

        container.addView(text);
    }

    private String formatMs(double value) {
        return value >= 0
                ? String.format(Locale.US, "%.1f ms", value)
                : "--";
    }

    private String formatSpeed(double value) {
        return value >= 0
                ? String.format(Locale.US, "%.2f MB/s", value)
                : "--";
    }

    private String formatEnergy(double value) {
        return value >= 0
                ? String.format(Locale.US, "%.3f J", value)
                : "--";
    }

    private String formatPercent(double value) {
        return value >= 0
                ? String.format(Locale.US, "%.1f%%", value)
                : "--";
    }

    private int dp(int value) {
        return (int) (
                value * getResources()
                        .getDisplayMetrics().density
        );
    }
}
