
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
import java.util.ArrayList;
import java.util.List;

public class file_upload extends AppCompatActivity {

    // =====================================================
    // 1. VIEW BINDING AND VARIABLES
    // =====================================================

    private ActivityFileUploadBinding binding;

    // Stores the selected file URI.
    private Uri selectedUri;

    // File picker launcher.
    private ActivityResultLauncher<String> filePicker;


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


        // Register Android file picker.
        filePicker = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {

                    if (uri != null) {

                        // Store selected file.
                        selectedUri = uri;

                        // Show selected file card.
                        binding.fileCard.setVisibility(View.VISIBLE);

                        // Display selected file name.
                        binding.fileName.setText(
                                getFileName(uri)
                        );

                        // Update status.
                        binding.status.setText(
                                "File selected successfully!"
                        );

                        binding.infoUploadStatus.setText(
                                "File ready for analysis"
                        );

                        // Hide previous success message.
                        binding.successCard.setVisibility(View.GONE);

                        // Update information and performance.
                        updateUploadingInformation();
                        resetUploadingPerformance();

                        // Update button.
                        updateStartButton();
                    }
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
                binding.volleyCheck

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

        // Check file selection.
        boolean fileSelected = selectedUri != null;

        // Check library selection.
        boolean librarySelected =
                binding.httpURLConnectionCheck.isChecked()
                        || binding.okHttpCheck.isChecked()
                        || binding.retrofitCheck.isChecked()
                        || binding.volleyCheck.isChecked();

        // Enable button only if both conditions are true.
        binding.uploadButton.setEnabled(
                fileSelected && librarySelected
        );

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

        return selectedLibraries;

    }


    // =====================================================
    // 9. UPDATE UPLOADING INFORMATION
    // =====================================================

    private void updateUploadingInformation() {

        // If no file is selected.
        if (selectedUri == null) {

            binding.infoFileName.setText("No file selected");
            binding.infoFileSize.setText("--");
            binding.infoFileType.setText("--");

            binding.infoLibraries.setText(
                    getSelectedLibraries().isEmpty()
                            ? "No library selected"
                            : android.text.TextUtils.join(
                            ", ",
                            getSelectedLibraries()
                    )
            );

            binding.infoUploadStatus.setText(
                    "Waiting for file"
            );

            return;
        }


        // Get file name.
        String fileName = getFileName(selectedUri);

        // Get file size.
        long fileSize = getFileSize(selectedUri);

        // Get file type.
        String fileType = getContentResolver().getType(
                selectedUri
        );


        // Display file name.
        binding.infoFileName.setText(fileName);

        // Display file size.
        if (fileSize >= 0) {

            binding.infoFileSize.setText(
                    android.text.format.Formatter.formatFileSize(
                            this,
                            fileSize
                    )
            );

        } else {

            binding.infoFileSize.setText(
                    "Size unavailable"
            );

        }


        // Display file type.
        binding.infoFileType.setText(
                fileType != null ? fileType : "Unknown"
        );


        // Display selected libraries.
        List<String> libraries = getSelectedLibraries();

        if (libraries.isEmpty()) {

            binding.infoLibraries.setText(
                    "No library selected"
            );

        } else {

            binding.infoLibraries.setText(
                    android.text.TextUtils.join(
                            ", ",
                            libraries
                    )
            );

        }


        // Update upload status.
        if (libraries.isEmpty()) {

            binding.infoUploadStatus.setText(
                    "Select a network library"
            );

        } else {

            binding.infoUploadStatus.setText(
                    "Ready for analysis"
            );

        }

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
    // 12. START NETWORK ANALYSIS
    // =====================================================

    private void startNetworkAnalysis() {

        // Validate file.
        if (selectedUri == null) {

            Toast.makeText(
                    this,
                    "Please select a file first!",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }


        // Get selected libraries.
        List<String> selectedLibraries = getSelectedLibraries();


        // Validate libraries.
        if (selectedLibraries.isEmpty()) {

            Toast.makeText(
                    this,
                    "Please select at least one network library!",
                    Toast.LENGTH_SHORT
            ).show();

            return;
        }


        // Disable button during preparation.
        binding.uploadButton.setEnabled(false);


        // Get file and library information.
        String fileName = getFileName(selectedUri);

        String libraries = android.text.TextUtils.join(
                ", ",
                selectedLibraries
        );


        // Update upload status.
        binding.status.setText(
                "Analysis preparation completed (Demo Mode)"
        );

        binding.infoUploadStatus.setText(
                "Prepared - No actual upload performed"
        );


        // Performance remains untested.
        resetUploadingPerformance();


        // Display success information.
        binding.successMessage.setText(
                "File and library selection completed! "
                        + "No network upload was performed."
        );

        binding.successCard.setVisibility(View.VISIBLE);


        // Log information for Android Studio Logcat.
        String result =
                "File: " + fileName
                        + "\nSelected Libraries: " + libraries
                        + "\nMode: Demo Mode"
                        + "\nBackend: Not connected"
                        + "\nActual Upload: Not performed";


        android.util.Log.d(
                "NETWORK_ANALYSIS",
                result
        );


        // Show confirmation.
        Toast.makeText(
                this,
                "Analysis preparation completed!",
                Toast.LENGTH_LONG
        ).show();


        // Re-enable button if selections are valid.
        updateStartButton();

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

        binding = null;

        super.onDestroy();

    }

}
