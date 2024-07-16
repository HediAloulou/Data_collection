package com.example.datacollection;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class SensorService extends Service implements SensorEventListener {

    private static final String CHANNEL_ID = "SensorServiceChannel";
    private static final String TAG = "SensorService";

    private SensorManager sensorManager;
    private Sensor linearAccelerationSensor;
    private Sensor rotationVectorSensor;
    private Sensor gyroscopeVectorSensor;

    private List<Map<String, Object>> sensorDataList = new ArrayList<>();
    private String username;
    private String userType;

    private long lastTimestamp = -1;

    @SuppressLint("ForegroundServiceType")
    @Override
    public void onCreate() {
        super.onCreate();

        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        linearAccelerationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        rotationVectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
        gyroscopeVectorSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE);
        createNotificationChannel();

        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Sensor Recording")
                .setContentText("Recording sensor data...")
                .setSmallIcon(com.google.firebase.database.collection.R.drawable.common_google_signin_btn_icon_light_normal)
                .setContentIntent(pendingIntent);

        startForeground(1, notificationBuilder.build());

        sensorManager.registerListener(this, linearAccelerationSensor, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, rotationVectorSensor, SensorManager.SENSOR_DELAY_NORMAL);
        sensorManager.registerListener(this, gyroscopeVectorSensor, SensorManager.SENSOR_DELAY_NORMAL);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        username = intent.getStringExtra("username");
        userType = intent.getStringExtra("userType");
        Log.d(TAG, "Service started with username: " + username + " and userType: " + userType);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        sensorManager.unregisterListener(this);
        Log.d(TAG, "Service destroyed, sensor listeners unregistered");
        sendSensorDataToFirestore();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float[] values = event.values.clone();
        long timestamp = event.timestamp;
        if (lastTimestamp == -1 || timestamp - lastTimestamp >= 1000000000) { // 1 second
            lastTimestamp = timestamp;
            if (!sensorDataList.isEmpty()) {
                sendSensorDataToFirestore();
                sensorDataList.clear();
            }
        }

        Map<String, Object> sensorData = new HashMap<>();
        sensorData.put("timestamp", timestamp);
        sensorData.put("username", username);
        sensorData.put("userType", userType);
        switch (event.sensor.getType()) {
            case Sensor.TYPE_LINEAR_ACCELERATION:
                sensorData.put("sensor", "Linear Acceleration");
                sensorData.put("x", values[0]);
                sensorData.put("y", values[1]);
                sensorData.put("z", values[2]);
                break;
            case Sensor.TYPE_ROTATION_VECTOR:
                sensorData.put("sensor", "Rotation Vector");
                sensorData.put("x", values[0]);
                sensorData.put("y", values[1]);
                sensorData.put("z", values[2]);
                break;
            case Sensor.TYPE_GYROSCOPE:
                sensorData.put("sensor", "Gyroscope");
                sensorData.put("x", values[0]);
                sensorData.put("y", values[1]);
                sensorData.put("z", values[2]);
                break;
        }
        sensorDataList.add(sensorData);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Not implemented but can be added if needed
    }

    private void sendSensorDataToFirestore() {
        if (sensorDataList.isEmpty()) return;

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        ExecutorService executor = Executors.newSingleThreadExecutor();

        executor.submit(() -> {
            Map<String, Object> batchData = new HashMap<>();
            batchData.put("timestamp", sensorDataList.get(0).get("timestamp"));
            batchData.put("username", sensorDataList.get(0).get("username"));
            batchData.put("userType", sensorDataList.get(0).get("userType"));

            for (Map<String, Object> sensorData : sensorDataList) {
                String sensorType = (String) sensorData.get("sensor");
                batchData.put(sensorType + "_x", sensorData.get("x"));
                batchData.put(sensorType + "_y", sensorData.get("y"));
                batchData.put(sensorType + "_z", sensorData.get("z"));
            }

            db.collection("sensor_data")
                    .add(batchData)
                    .addOnSuccessListener(documentReference -> {
                        Log.d(TAG, "Data sent to Firestore successfully");
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to send data to Firestore", e);
                    });

        });

        executor.shutdown();
        try {
            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Sensor Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }
}
