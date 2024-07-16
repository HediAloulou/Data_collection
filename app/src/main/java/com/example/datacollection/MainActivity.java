package com.example.datacollection;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    private Button startButton;
    private Button stopButton;
    private EditText usernameEditText;
    private RadioGroup userTypeGroup;
    private boolean isRecording = false;

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

        // Set up the start button
        startButton = findViewById(R.id.startButton);
        startButton.setOnClickListener(v -> startSensorRecording());

        // Set up the stop button
        stopButton = findViewById(R.id.stopButton);
        stopButton.setOnClickListener(v -> stopSensorRecording());

        // Username input
        usernameEditText = findViewById(R.id.usernameEditText);

        // User type radio group
        userTypeGroup = findViewById(R.id.userTypeGroup);
    }

    private void startSensorRecording() {
        if (isRecording) return;

        String username = usernameEditText.getText().toString().trim();
        if (username.isEmpty()) {
            Toast.makeText(this, "Please enter a username", Toast.LENGTH_SHORT).show();
            return;
        }

        int selectedUserTypeId = userTypeGroup.getCheckedRadioButtonId();
        if (selectedUserTypeId == -1) {
            Toast.makeText(this, "Please select a user type", Toast.LENGTH_SHORT).show();
            return;
        }

        RadioButton selectedUserTypeButton = findViewById(selectedUserTypeId);
        String userType = selectedUserTypeButton.getText().toString();

        isRecording = true;
        startButton.setEnabled(false);
        stopButton.setEnabled(true);

        Intent serviceIntent = new Intent(this, SensorService.class);
        serviceIntent.putExtra("username", username);
        serviceIntent.putExtra("userType", userType);
        startService(serviceIntent);
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