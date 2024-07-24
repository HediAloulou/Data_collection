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
import android.os.Handler;
import android.os.HandlerThread;
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

public class SensorService extends Service implements SensorEventListener {

    private static final String CHANNEL_ID = "SensorServiceChannel";
    private static final String TAG = "SensorService";

    private SensorManager sensorManager;
    private Sensor linearAccelerationSensor;
    private Sensor rotationVectorSensor;
    private Sensor gyroscopeVectorSensor;

    private List<Map<String, Object>> sensorDataList = new ArrayList<>();
    private String userId;
    private int age;

    private HandlerThread sensorThread;
    private Handler sensorHandler;

    @SuppressLint("ForegroundServiceType")
    @Override
    public void onCreate() {
        super.onCreate();

        sensorThread = new HandlerThread("SensorThread");
        sensorThread.start();
        sensorHandler = new Handler(sensorThread.getLooper());

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
                .setSmallIcon(android.R.drawable.ic_menu_compass)
                .setContentIntent(pendingIntent);

        startForeground(1, notificationBuilder.build());

        sensorManager.registerListener(this, linearAccelerationSensor, SensorManager.SENSOR_DELAY_NORMAL, sensorHandler);
        sensorManager.registerListener(this, rotationVectorSensor, SensorManager.SENSOR_DELAY_NORMAL, sensorHandler);
        sensorManager.registerListener(this, gyroscopeVectorSensor, SensorManager.SENSOR_DELAY_NORMAL, sensorHandler);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        userId = intent.getStringExtra("userId");
        age = intent.getIntExtra("age", -1);
        Log.d(TAG, "Service started with userId: " + userId + " and age: " + age);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        sensorManager.unregisterListener(this);
        Log.d(TAG, "Service destroyed, sensor listeners unregistered");
        sendSensorDataToFirestore();

        sensorThread.quitSafely();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        long timestamp = new Date().getTime();
        float[] values = event.values.clone();
        Map<String, Object> sensorData = new HashMap<>();
        sensorData.put("timestamp", timestamp);
        sensorData.put("userId", userId);
        sensorData.put("age", age);

        switch (event.sensor.getType()) {
            case Sensor.TYPE_LINEAR_ACCELERATION:
                sensorData.put("sensor", "Linear Acceleration");
                sensorData.put("x", values[0]);
                sensorData.put("y", values[1]);
                sensorData.put("z", values[2]);
                Log.d(TAG, "Linear Acceleration Sensor Data: " + values[0] + ", " + values[1] + ", " + values[2]);
                break;
            case Sensor.TYPE_ROTATION_VECTOR:
                sensorData.put("sensor", "Rotation Vector");
                sensorData.put("x", values[0]);
                sensorData.put("y", values[1]);
                sensorData.put("z", values[2]);
                Log.d(TAG, "Rotation Vector Sensor Data: " + values[0] + ", " + values[1] + ", " + values[2]);
                break;
            case Sensor.TYPE_GYROSCOPE:
                sensorData.put("sensor", "Gyroscope");
                sensorData.put("x", values[0]);
                sensorData.put("y", values[1]);
                sensorData.put("z", values[2]);
                Log.d(TAG, "Gyroscope Sensor Data: " + values[0] + ", " + values[1] + ", " + values[2]);
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
        Log.d(TAG, "Sending data to Firestore collection: " + userId);
        for (Map<String, Object> sensorData : sensorDataList) {
            db.collection(userId)
                    .add(sensorData)
                    .addOnSuccessListener(documentReference -> {
                        Log.d(TAG, "Data sent to Firestore successfully");
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to send data to Firestore", e);
                    });
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
