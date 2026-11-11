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

                            txtStatus.setText("COMPLETED");
                            txtStatus.setTextColor(Color.parseColor("#15803D"));
                        }

                        finishTest();
                    });
                }
            });
        }
    }
