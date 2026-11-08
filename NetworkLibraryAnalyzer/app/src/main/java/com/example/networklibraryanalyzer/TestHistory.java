package com.example.networklibraryanalyzer;

import android.os.Bundle;
import android.widget.ArrayAdapter;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.widget.Spinner;

public class TestHistory extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_test_history);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });


        Spinner librarySpinner =
                findViewById(R.id.spinnerHistoryLibrary);

        Spinner methodSpinner =
                findViewById(R.id.spinnerHistoryMethod);

        String[] libraries = {
                "All Libraries",
                "HttpURLConnection",
                "OkHttp",
                "Retrofit",
                "Volley"
        };

        String[] methods = {
                "All Methods",
                "GET",
                "POST",
                "PUT",
                "DELETE"
        };

        ArrayAdapter<String> libraryAdapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_item,
                        libraries
                );

        libraryAdapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        librarySpinner.setAdapter(libraryAdapter);

        ArrayAdapter<String> methodAdapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_item,
                        methods
                );

        methodAdapter.setDropDownViewResource(
                android.R.layout.simple_spinner_dropdown_item
        );

        methodSpinner.setAdapter(methodAdapter);

    }
}