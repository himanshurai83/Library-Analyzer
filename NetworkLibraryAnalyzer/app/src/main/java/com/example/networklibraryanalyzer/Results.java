
package com.example.networklibraryanalyzer;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class Results extends AppCompatActivity {

    // =====================================================
    // 1. RESULT ITEM CLASS
    // =====================================================
    // This class stores the result of each network library.
    //
    // BACKEND CONNECTION:
    // The backend will send these values in JSON format.
    // We will read those values and store them in this class.

    private static class ResultItem {

        final String name;
        final double timeMs;
        final double speedMBps;

        ResultItem(String name, double timeMs, double speedMBps) {
            this.name = name;
            this.timeMs = timeMs;
            this.speedMBps = speedMBps;
        }
    }


    // =====================================================
    // 2. ACTIVITY START
    // =====================================================

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // IMPORTANT:
        // This is the XML layout file name:
        // activity_view_results.xml
        setContentView(R.layout.activity_results);


        // Find XML views
        TextView fastestName = findViewById(R.id.fastestName);
        TextView fastestTime = findViewById(R.id.fastestTime);
        TextView testedFile = findViewById(R.id.testedFile);

        LinearLayout container = findViewById(R.id.resultContainer);
        Button backButton = findViewById(R.id.backButton);


        // Check whether all required XML IDs exist
        if (fastestName == null ||
                fastestTime == null ||
                testedFile == null ||
                container == null ||
                backButton == null) {

            throw new IllegalStateException(
                    "Please check the IDs in activity_view_results.xml"
            );
        }


        // =================================================
        // 3. RECEIVE DATA FROM PREVIOUS ACTIVITY
        // =================================================
        //
        // BACKEND CONNECTION:
        // The upload screen will send the backend response
        // to this Results activity.
        //
        // The JSON response will be received using:
        // getStringExtra("results_json")
        //
        // The uploaded file name will be received using:
        // getStringExtra("file_name")

        String jsonResponse =
                getIntent().getStringExtra("results_json");

        String fileName =
                getIntent().getStringExtra("file_name");


        // Display uploaded file name
        if (fileName != null && !fileName.trim().isEmpty()) {
            testedFile.setText(fileName);
        } else {
            testedFile.setText("No file information received");
        }


        // =================================================
        // 4. READ BACKEND JSON RESPONSE
        // =================================================

        List<ResultItem> results = new ArrayList<>();

        if (jsonResponse != null && !jsonResponse.trim().isEmpty()) {

            try {

                // Convert backend JSON response into JSONObject
                JSONObject json = new JSONObject(jsonResponse);

                // Check whether backend reports success
                boolean success = json.optBoolean("success", false);

                if (!success) {
                    showEmptyMessage(
                            container,
                            "Backend did not return successful results."
                    );

                    fastestName.setText("Not available");
                    fastestTime.setText("--");

                    backButton.setOnClickListener(v -> finish());
                    return;
                }

                // Get file name from JSON if Intent did not provide it
                String responseFileName =
                        json.optString("file_name", "");

                if ((fileName == null || fileName.trim().isEmpty())
                        && !responseFileName.isEmpty()) {

                    testedFile.setText(responseFileName);
                }


                // Get the results array from backend
                JSONArray array = json.getJSONArray("results");


                // Read every library result
                for (int i = 0; i < array.length(); i++) {

                    JSONObject item = array.getJSONObject(i);

                    String libraryName =
                            item.getString("library");

                    double executionTime =
                            item.getDouble("time_ms");

                    // Speed is optional.
                    // If backend does not send it, display it as unavailable.
                    double speed =
                            item.optDouble("speed_mbps", -1);

                    // Store result
                    results.add(
                            new ResultItem(
                                    libraryName,
                                    executionTime,
                                    speed
                            )
                    );
                }

            } catch (JSONException e) {

                // JSON parsing failed
                e.printStackTrace();

                showEmptyMessage(
                        container,
                        "Unable to read results from the backend."
                );

                fastestName.setText("Invalid response");
                fastestTime.setText("--");

                backButton.setOnClickListener(v -> finish());
                return;
            }

        } else {

            // BACKEND CONNECTION TODO:
            //
            // No backend response has been received.
            //
            // Once the upload activity connects to the backend,
            // it must pass the actual JSON response using:
            //
            // intent.putExtra("results_json", responseBody);
            //
            // Do not insert fake results here.
            // This message will disappear when actual data arrives.

            showEmptyMessage(
                    container,
                    "No test results received yet. Upload a file and run the analysis."
            );

            fastestName.setText("Waiting for results");
            fastestTime.setText("--");

            backButton.setOnClickListener(v -> finish());
            return;
        }


        // =================================================
        // 5. SORT RESULTS
        // =================================================
        // Lower execution time means faster performance.

        Collections.sort(
                results,
                (a, b) -> Double.compare(a.timeMs, b.timeMs)
        );


        // Check that the backend returned at least one result
        if (results.isEmpty()) {

            showEmptyMessage(
                    container,
                    "No library results were found."
            );

            fastestName.setText("Not available");
            fastestTime.setText("--");

            backButton.setOnClickListener(v -> finish());
            return;
        }


        // =================================================
        // 6. DISPLAY FASTEST LIBRARY
        // =================================================

        ResultItem best = results.get(0);

        fastestName.setText(best.name);

        String fastestInfo = String.format(
                Locale.US,
                "%.0f ms",
                best.timeMs
        );

        if (best.speedMBps >= 0) {
            fastestInfo += String.format(
                    Locale.US,
                    " | %.1f MB/s",
                    best.speedMBps
            );
        }

        fastestTime.setText(fastestInfo);


        // =================================================
        // 7. DISPLAY DYNAMIC COMPARISON ROWS
        // =================================================
        //
        // BACKEND CONNECTION:
        // Each object inside the JSON results array
        // creates one row on the Results page.
        //
        // If the backend returns 4 libraries,
        // this code creates 4 rows automatically.

        container.removeAllViews();

        for (int i = 0; i < results.size(); i++) {

            View row = buildRow(
                    i + 1,
                    results.get(i),
                    best.timeMs
            );

            container.addView(row);
        }


        // =================================================
        // 8. BACK BUTTON
        // =================================================

        backButton.setOnClickListener(v -> finish());

    }


    // =====================================================
    // 9. BUILD INDIVIDUAL RESULT ROW
    // =====================================================
    // This method creates one comparison row dynamically.
    // It displays rank, library name, speed and execution time.

    private View buildRow(
            int rank,
            ResultItem item,
            double bestTime
    ) {

        boolean isBest = rank == 1;


        // Main row layout
        LinearLayout row = new LinearLayout(this);

        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setBackgroundResource(R.drawable.bg_file_item);

        row.setPadding(
                dp(12),
                dp(12),
                dp(12),
                dp(12)
        );


        LinearLayout.LayoutParams rowLp =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );

        rowLp.bottomMargin = dp(10);
        row.setLayoutParams(rowLp);


        // Rank badge
        TextView rankLabel = new TextView(this);

        rankLabel.setLayoutParams(
                new LinearLayout.LayoutParams(dp(44), dp(44))
        );

        rankLabel.setBackgroundResource(R.drawable.bg_file_icon);
        rankLabel.setGravity(Gravity.CENTER);
        rankLabel.setTextSize(14);
        rankLabel.setTypeface(null, Typeface.BOLD);
        rankLabel.setText("#" + rank);

        rankLabel.setTextColor(
                Color.parseColor(
                        isBest ? "#16A34A" : "#3D7BF7"
                )
        );

        row.addView(rankLabel);


        // Middle column
        LinearLayout middle = new LinearLayout(this);

        middle.setOrientation(LinearLayout.VERTICAL);

        LinearLayout.LayoutParams midLp =
                new LinearLayout.LayoutParams(
                        0,
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        1f
                );

        midLp.setMarginStart(dp(12));
        middle.setLayoutParams(midLp);


        // Library name
        TextView name = new TextView(this);

        name.setText(item.name);
        name.setTextColor(Color.parseColor("#1F2A44"));
        name.setTextSize(14);
        name.setTypeface(null, Typeface.BOLD);
        name.setSingleLine(true);

        middle.addView(name);


        // Speed information
        TextView info = new TextView(this);

        if (item.speedMBps >= 0) {

            info.setText(
                    String.format(
                            Locale.US,
                            "%.1f MB/s",
                            item.speedMBps
                    )
            );

        } else {

            info.setText("Speed not provided by backend");
        }

        info.setTextColor(Color.parseColor("#8A94A8"));
        info.setTextSize(11);

        middle.addView(info);


        // Performance progress bar
        ProgressBar bar = new ProgressBar(
                this,
                null,
                android.R.attr.progressBarStyleHorizontal
        );

        LinearLayout.LayoutParams barLp =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        dp(6)
                );

        barLp.topMargin = dp(6);

        bar.setLayoutParams(barLp);
        bar.setMax(100);


        // Fastest library gets a full progress bar.
        int progress = 0;

        if (item.timeMs > 0 && bestTime > 0) {

            progress = (int) Math.round(
                    bestTime / item.timeMs * 100
            );
        }

        bar.setProgress(
                Math.max(0, Math.min(100, progress))
        );


        bar.setProgressTintList(
                ColorStateList.valueOf(
                        Color.parseColor("#3D7BF7")
                )
        );

        bar.setProgressBackgroundTintList(
                ColorStateList.valueOf(
                        Color.parseColor("#E6ECFA")
                )
        );

        middle.addView(bar);

        row.addView(middle);


        // Execution time
        TextView time = new TextView(this);

        LinearLayout.LayoutParams timeLp =
                new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.WRAP_CONTENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT
                );

        timeLp.setMarginStart(dp(10));
        time.setLayoutParams(timeLp);

        time.setText(
                String.format(
                        Locale.US,
                        "%.0f ms",
                        item.timeMs
                )
        );

        time.setTextSize(13);
        time.setTypeface(null, Typeface.BOLD);

        time.setTextColor(
                Color.parseColor(
                        isBest ? "#16A34A" : "#1F2A44"
                )
        );

        row.addView(time);

        return row;
    }


    // =====================================================
    // 10. SHOW MESSAGE WHEN RESULTS ARE NOT AVAILABLE
    // =====================================================

    private void showEmptyMessage(
            LinearLayout container,
            String message
    ) {

        container.removeAllViews();

        TextView emptyMessage = new TextView(this);

        emptyMessage.setText(message);
        emptyMessage.setTextSize(14);
        emptyMessage.setTextColor(
                Color.parseColor("#64748B")
        );

        emptyMessage.setGravity(Gravity.CENTER);

        emptyMessage.setPadding(
                dp(12),
                dp(24),
                dp(12),
                dp(24)
        );

        container.addView(emptyMessage);
    }


    // =====================================================
    // 11. DP CONVERSION
    // =====================================================
    // Converts dp values into pixels for Android layouts.

    private int dp(int value) {

        return (int) (
                value * getResources()
                        .getDisplayMetrics().density
        );
    }

}
