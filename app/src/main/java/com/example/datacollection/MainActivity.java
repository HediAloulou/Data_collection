package com.example.datacollection;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private Button startButton;
    private Button stopButton;  // Add this line
    private EditText ageEditText;
    private boolean isRecording = false;
    private String userId;
    private Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Setting up UI insets
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            v.setPadding(insets.getInsets(WindowInsetsCompat.Type.systemBars()).left,
                    insets.getInsets(WindowInsetsCompat.Type.systemBars()).top,
                    insets.getInsets(WindowInsetsCompat.Type.systemBars()).right,
                    insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom);
            return insets;
        });

        // Initialize UI components
        startButton = findViewById(R.id.startButton);
        stopButton = findViewById(R.id.stopButton);  // Add this line
        ageEditText = findViewById(R.id.ageEditText);

        startButton.setOnClickListener(v -> startSensorRecording());
        stopButton.setOnClickListener(v -> stopSensorRecording());  // Add this line
    }

    private void startSensorRecording() {
        if (isRecording) return;

        String ageText = ageEditText.getText().toString();
        if (ageText.isEmpty()) {
            Toast.makeText(this, "Please enter your age", Toast.LENGTH_SHORT).show();
            return;
        }

        int age = Integer.parseInt(ageText);
        if (!isValidAge(age)) {
            Toast.makeText(this, "Please enter a valid age (0-13 or 18-60)", Toast.LENGTH_SHORT).show();
            return;
        }

        userId = UUID.randomUUID().toString(); // Generate new ID
        Toast.makeText(this, "Generated user ID: " + userId, Toast.LENGTH_SHORT).show();

        isRecording = true;
        startButton.setEnabled(false);
        stopButton.setEnabled(false);

        Intent serviceIntent = new Intent(this, SensorService.class);
        serviceIntent.putExtra("userId", userId);
        serviceIntent.putExtra("age", age);
        startService(serviceIntent);

        // Enable the stop button after 6 minutes
        handler.postDelayed(() -> stopButton.setEnabled(true), 6 * 60 * 1000);
    }

    private boolean isValidAge(int age) {
        return (age >= 0 && age <= 13) || (age >= 18 && age <= 60);
    }

    private void stopSensorRecording() {
        if (!isRecording) return;

        isRecording = false;
        startButton.setEnabled(true);
        stopButton.setEnabled(false);

        Intent serviceIntent = new Intent(this, SensorService.class);
        stopService(serviceIntent);
    }
}
