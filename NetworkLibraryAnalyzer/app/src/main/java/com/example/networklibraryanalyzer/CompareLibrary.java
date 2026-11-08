
package com.example.networklibraryanalyzer;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import android.widget.ArrayAdapter;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class CompareLibrary extends AppCompatActivity {

    private Spinner spinnerMethod, spinnerRuns;
    private EditText editEndpoint;
    private Button btnRunComparison;

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

        spinnerMethod = findViewById(R.id.spinnerCompareMethod);
        spinnerRuns = findViewById(R.id.spinnerCompareRuns);
        editEndpoint = findViewById(R.id.editCompareEndpoint);
        btnRunComparison = findViewById(R.id.btnRunComparison);

        // HTTP method options
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

        // Number of requests per library
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

        // Button click
        btnRunComparison.setOnClickListener(v -> {

            String method = spinnerMethod.getSelectedItem().toString();
            String endpoint = editEndpoint.getText().toString().trim();
            String runs = spinnerRuns.getSelectedItem().toString();

            if (endpoint.isEmpty()) {
                editEndpoint.setError("Enter an API endpoint");
                return;
            }

            Toast.makeText(
                    CompareLibrary.this,
                    "Method: " + method
                            + "\nEndpoint: " + endpoint
                            + "\nRequests per library: " + runs,
                    Toast.LENGTH_LONG
            ).show();

            // Next step:
            // Execute the selected request using all four libraries.
        });
    }
}
