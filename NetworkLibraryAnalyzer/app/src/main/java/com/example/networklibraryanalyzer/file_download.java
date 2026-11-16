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
import java.util.Collections;
import java.util.Comparator;
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
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
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
    // 1B. RESULT OF ONE LIBRARY + BEST LIBRARY COMPARISON
    // =====================================================

    // Result of one library after all runs.
    private static class LibraryStats {

        final String name;
        final long avgMs;           // average download time of all files (ms)
        final double throughput;    // MB/s
        final long bestMs;          // fastest single run (response time, ms)
        final int successCount;
        final int successPercent;   // 0 - 100
        int score;                  // sum of ranks, lower is better
        double energyMwh = -1;      // energy per run in mWh (-1 = not measured)

        LibraryStats(String name, long avgMs, double throughput, long bestMs,
                     int successCount, int successPercent) {
            this.name = name;
            this.avgMs = avgMs;
            this.throughput = throughput;
            this.bestMs = bestMs;
            this.successCount = successCount;
            this.successPercent = successPercent;
        }

        boolean ok() {
            return successCount > 0;
        }
    }


    // Texts shown in the "Best Library" card.
    private static class BestResult {
        boolean any = false;
        String overall = "";
        String downloadTime = "";
        String throughput = "";
        String responseTime = "";
        String successRate = "";
        String energy = "";
        String comparison = "";
    }


    // Adds the rank (1 = best) of every library for one result to "score".
    // Libraries with the same value get the same rank.
    private static void addRanks(List<LibraryStats> list, Comparator<LibraryStats> cmp) {

        List<LibraryStats> sorted = new ArrayList<>(list);
        Collections.sort(sorted, cmp);

        int rank = 1;

        for (int i = 0; i < sorted.size(); i++) {

            if (i > 0 && cmp.compare(sorted.get(i - 1), sorted.get(i)) != 0) {
                rank = i + 1;
            }

            sorted.get(i).score += rank;
        }
    }


    // Best library for one result (if equal, the one with the lower download time).
    private static LibraryStats pickBest(List<LibraryStats> list, Comparator<LibraryStats> cmp) {

        LibraryStats best = list.get(0);

        for (LibraryStats s : list) {

            int c = cmp.compare(s, best);

            if (c < 0 || (c == 0 && s.avgMs < best.avgMs)) {
                best = s;
            }
        }

        return best;
    }


    // Compares all libraries: download time, throughput, response time, success rate, energy.
    private static BestResult compareLibraries(List<LibraryStats> all) {

        BestResult result = new BestResult();

        // Only libraries that downloaded at least once can be "best".
        List<LibraryStats> ok = new ArrayList<>();

        for (LibraryStats s : all) {
            if (s.ok()) {
                ok.add(s);
            }
        }

        if (ok.isEmpty()) {
            return result;
        }

        result.any = true;

        Comparator<LibraryStats> byTime = (a, b) -> Long.compare(a.avgMs, b.avgMs);
        Comparator<LibraryStats> byThroughput = (a, b) -> Double.compare(b.throughput, a.throughput);
        Comparator<LibraryStats> byResponse = (a, b) -> Long.compare(a.bestMs, b.bestMs);
        Comparator<LibraryStats> bySuccess = (a, b) -> Integer.compare(b.successPercent, a.successPercent);
        Comparator<LibraryStats> byEnergy = (a, b) -> Double.compare(a.energyMwh, b.energyMwh);

        // Energy is compared only when it was measured for every successful library.
        boolean useEnergy = true;

        for (LibraryStats s : ok) {
            if (s.energyMwh < 0) {
                useEnergy = false;
            }
        }

        // Best library for each result.
        LibraryStats bestTime = pickBest(ok, byTime);
        LibraryStats bestThroughput = pickBest(ok, byThroughput);
        LibraryStats bestResponse = pickBest(ok, byResponse);
        LibraryStats bestSuccess = pickBest(ok, bySuccess);
        LibraryStats bestEnergy = useEnergy ? pickBest(ok, byEnergy) : null;

        // Best overall: the lowest sum of ranks over all results.
        for (LibraryStats s : ok) {
            s.score = 0;
        }

        addRanks(ok, byTime);
        addRanks(ok, byThroughput);
        addRanks(ok, byResponse);
        addRanks(ok, bySuccess);

        if (useEnergy) {
            addRanks(ok, byEnergy);
        }

        List<LibraryStats> ranked = new ArrayList<>(ok);

        Collections.sort(ranked, (a, b) -> {

            int c = Integer.compare(a.score, b.score);

            if (c != 0) {
                return c;
            }

            c = Integer.compare(b.successPercent, a.successPercent);

            if (c != 0) {
                return c;
            }

            return Long.compare(a.avgMs, b.avgMs);
        });

        LibraryStats overall = ranked.get(0);

        String dot = "  \u2022  ";

        result.overall = "\uD83C\uDFC6 " + overall.name;

        result.downloadTime = bestTime.name + dot + bestTime.avgMs + " ms";

        result.throughput = bestThroughput.name + dot
                + String.format(Locale.US, "%.2f MB/s", bestThroughput.throughput);

        result.responseTime = bestResponse.name + dot + bestResponse.bestMs + " ms";

        result.successRate = bestSuccess.name + dot + bestSuccess.successPercent + "%";

        result.energy = useEnergy
                ? bestEnergy.name + dot
                + String.format(Locale.US, "%.3f mWh per run", bestEnergy.energyMwh)
                : "Not measured (unplug the charger)";

        // Comparison of all libraries (best first, failed ones at the end).
        String star = "  \u2605";

        StringBuilder text = new StringBuilder();
        int place = 1;

        for (LibraryStats s : ranked) {

            text.append(place++).append(". ").append(s.name).append("\n")
                    .append("Download time: ").append(s.avgMs).append(" ms")
                    .append(s == bestTime ? star : "").append("\n")
                    .append("Throughput: ")
                    .append(String.format(Locale.US, "%.2f MB/s", s.throughput))
                    .append(s == bestThroughput ? star : "").append("\n")
                    .append("Response time: ").append(s.bestMs).append(" ms")
                    .append(s == bestResponse ? star : "").append("\n")
                    .append("Energy used: ")
                    .append(s.energyMwh >= 0
                            ? String.format(Locale.US, "%.3f mWh per run", s.energyMwh)
                            : "not measured")
                    .append(s == bestEnergy ? star : "").append("\n")
                    .append("Success rate: ").append(s.successPercent).append("% (")
                    .append(s.successCount).append("/").append(DOWNLOAD_RUNS).append(")")
                    .append(s == bestSuccess ? star : "").append("\n\n");
        }

        for (LibraryStats s : all) {

            if (!s.ok()) {
                text.append("-. ").append(s.name).append("\n")
                        .append("Failed (success rate 0%)\n\n");
            }
        }

        text.append("\u2605 = best in that result");

        result.comparison = text.toString();

        return result;
    }


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
        binding.performanceEnergy.setText("Not tested");

        // Hide the old "Best Library" card.
        binding.bestLibrarySection.setVisibility(View.GONE);
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

            // Result of every library (used for the "Best Library" card).
            List<LibraryStats> allStats = new ArrayList<>();

            // Energy meter (estimates the battery energy used by each library).
            EnergyMeter meter = new EnergyMeter(getApplicationContext());
            StringBuilder energyText = new StringBuilder();
            boolean canMeasure = !meter.isCharging();

            if (canMeasure) {
                try {
                    meter.measureBaseline();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    canMeasure = false;
                }
            }

            for (String library : selectedLibraries) {

                long totalMs = 0;
                long totalBytesAllRuns = 0;
                long bestMs = Long.MAX_VALUE;
                int successCount = 0;

                if (canMeasure) {
                    meter.start();
                }

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

                // Stop the energy meter for this library.
                EnergyMeter.Result energy = canMeasure ? meter.stop() : null;

                // Energy of ONE run (all files) and the size of one run.
                double perRunEnergy = (energy != null && energy.valid)
                        ? energy.mWh / DOWNLOAD_RUNS
                        : -1;
                double mbPerRun = (successCount > 0 ? totalBytesAllRuns / successCount : 0)
                        / (1024.0 * 1024.0);

                if (successCount > 0) {

                    anySuccess = true;

                    long avgMs = Math.max(totalMs / successCount, 1);
                    long avgBytes = totalBytesAllRuns / successCount;
                    lastTotalBytes = avgBytes;

                    double megabytes = avgBytes / (1024.0 * 1024.0);
                    double throughput = megabytes / (avgMs / 1000.0);

                    // Save the result for the "Best Library" card.
                    LibraryStats stats = new LibraryStats(library, avgMs, throughput, bestMs,
                            successCount, successCount * 100 / DOWNLOAD_RUNS);
                    stats.energyMwh = perRunEnergy;
                    allStats.add(stats);

                    timeText.append(library).append(": ").append(avgMs).append(" ms\n");

                    throughputText.append(library).append(": ")
                            .append(String.format(Locale.US, "%.2f MB/s", throughput))
                            .append("\n");

                    responseText.append(library).append(": ")
                            .append(bestMs).append(" ms (best)\n");

                } else {

                    allStats.add(new LibraryStats(library, 0, 0, 0, 0, 0));

                    timeText.append(library).append(": Failed\n");
                    throughputText.append(library).append(": Failed\n");
                    responseText.append(library).append(": Failed\n");
                }

                if (energy == null) {
                    energyText.append(library).append(": Unplug the charger\n");
                } else if (!energy.valid) {
                    energyText.append(library).append(": ").append(energy.reason).append("\n");
                } else if (successCount == 0) {
                    energyText.append(library).append(": Failed\n");
                } else {
                    energyText.append(library).append(": ")
                            .append(String.format(Locale.US, "%.3f mWh per run", perRunEnergy))
                            .append(mbPerRun > 0
                                    ? String.format(Locale.US, " (%.3f mWh/MB)", perRunEnergy / mbPerRun)
                                    : "")
                            .append("\n");
                }

                rateText.append(library).append(": ")
                        .append(successCount * 100 / DOWNLOAD_RUNS).append("% (")
                        .append(successCount).append("/")
                        .append(DOWNLOAD_RUNS).append(")\n");

                // Short pause so the radio calms down before the next library.
                if (canMeasure) {
                    try {
                        Thread.sleep(1500);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }

            final boolean success = anySuccess;
            final String errorText = lastError;
            final long downloadedBytes = lastTotalBytes;
            final boolean anySaved = !savedNames.isEmpty();

            final String timeResult = timeText.toString().trim();
            final String throughputResult = throughputText.toString().trim();
            final String responseResult = responseText.toString().trim();
            final String rateResult = rateText.toString().trim();
            final String energyResult = energyText.toString().trim();

            // Compare all libraries and find the best one.
            final BestResult best = compareLibraries(allStats);

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
                binding.performanceEnergy.setText(energyResult);

                // Best library card.
                if (best.any) {

                    binding.bestOverall.setText(best.overall);
                    binding.bestDownloadTime.setText(best.downloadTime);
                    binding.bestThroughput.setText(best.throughput);
                    binding.bestResponseTime.setText(best.responseTime);
                    binding.bestSuccessRate.setText(best.successRate);
                    binding.bestEnergy.setText(best.energy);
                    binding.comparisonText.setText(best.comparison);

                    binding.bestLibrarySection.setVisibility(View.VISIBLE);

                } else {

                    binding.bestLibrarySection.setVisibility(View.GONE);
                }

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
    // ENERGY METER
    // Estimates the battery energy used while a test runs.
    // It reads the battery current about every 100 ms and adds up
    // current x voltage x time. The idle power measured before the test
    // is subtracted, so only the extra energy of the test is left.
    // The phone must NOT be charging. The result is an ESTIMATE.
    // =====================================================

    static class EnergyMeter {

        static class Result {
            final boolean valid;     // false when the energy could not be measured
            final String reason;     // why it could not be measured
            final double mWh;        // net energy in milliwatt-hours

            Result(boolean valid, String reason, double mWh) {
                this.valid = valid;
                this.reason = reason;
                this.mWh = mWh;
            }
        }

        private final Context context;
        private final BatteryManager batteryManager;

        private volatile boolean running = false;
        private Thread thread;
        private double totalMwh = 0;
        private long samples = 0;
        private long startNanos = 0;
        private double baselineMw = 0;      // idle power in mW

        EnergyMeter(Context context) {
            this.context = context.getApplicationContext();
            this.batteryManager = (BatteryManager)
                    this.context.getSystemService(Context.BATTERY_SERVICE);
        }

        private Intent batteryIntent() {
            return context.registerReceiver(null,
                    new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        }

        // True when a charger or USB cable is connected (values would be wrong).
        boolean isCharging() {
            Intent intent = batteryIntent();
            return intent != null && intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0) != 0;
        }

        // Power used right now in mW (0 if the phone does not report it).
        private double powerMw() {

            if (batteryManager == null) {
                return 0;
            }

            long raw = batteryManager.getLongProperty(
                    BatteryManager.BATTERY_PROPERTY_CURRENT_NOW);

            if (raw == Long.MIN_VALUE || raw == 0) {
                return 0;
            }

            // The sign depends on the phone, so use the absolute value.
            double microAmps = Math.abs((double) raw);

            // Some phones report milliamps instead of microamps.
            if (microAmps < 10000) {
                microAmps *= 1000;
            }

            Intent intent = batteryIntent();
            double volts = intent != null
                    ? intent.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) / 1000.0
                    : 0;

            if (volts <= 0) {
                return 0;
            }

            return (microAmps / 1000.0) * volts;      // mA x V = mW
        }

        // Measures the idle power for about 2 seconds. Call it before the test.
        void measureBaseline() throws InterruptedException {

            double sum = 0;
            int count = 0;

            for (int i = 0; i < 20; i++) {

                double p = powerMw();

                if (p > 0) {
                    sum += p;
                    count++;
                }

                Thread.sleep(100);
            }

            baselineMw = count > 0 ? sum / count : 0;
        }

        void start() {

            totalMwh = 0;
            samples = 0;
            running = true;
            startNanos = System.nanoTime();

            thread = new Thread(() -> {

                long last = System.nanoTime();

                while (running) {

                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException e) {
                        return;
                    }

                    long now = System.nanoTime();
                    double p = powerMw();

                    if (p > 0) {
                        // mW x hours  (1 hour = 3.6e12 nanoseconds)
                        totalMwh += p * ((now - last) / 3.6e12);
                        samples++;
                    }

                    last = now;
                }
            });

            thread.start();
        }

        Result stop() {

            running = false;

            try {
                if (thread != null) {
                    thread.join();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            if (samples == 0) {
                return new Result(false, "Not supported on this phone", 0);
            }

            double hours = (System.nanoTime() - startNanos) / 3.6e12;

            // Remove the energy the phone would have used anyway (screen, idle).
            double net = totalMwh - baselineMw * hours;

            return new Result(true, "", Math.max(net, 0));
        }
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