package com.example.reverseportraitguard;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.app.KeyguardManager;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.IBinder;
import android.os.SystemClock;
import android.provider.Settings;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.TextView;

public class GuardService extends Service implements SensorEventListener {
    public static volatile boolean running;

    private static final int NOTIFICATION_ID = 1001;
    public static final String CHANNEL_ID = "reverse_portrait_guard";
    private static final long STABLE_TIME_MS = 300L;
    private static final float ENTER_THRESHOLD = -7.0f;
    private static final float EXIT_THRESHOLD = -5.5f;

    private SensorManager sensorManager;
    private Sensor orientationSensor;
    private WindowManager windowManager;
    private KeyguardManager keyguardManager;
    private View blockingOverlay;
    private boolean usingAccelerometer;
    private boolean blocked;
    private boolean candidateState;
    private long candidateSince;
    private final float[] filteredGravity = new float[3];

    @Override
    public void onCreate() {
        super.onCreate();
        running = true;
        createNotificationChannel();
        startForeground(NOTIFICATION_ID, createNotification());

        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        keyguardManager = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        orientationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
        if (orientationSensor == null) {
            orientationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            usingAccelerometer = true;
        }
        if (orientationSensor != null) {
            sensorManager.registerListener(this, orientationSensor, SensorManager.SENSOR_DELAY_UI);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float x;
        float y;
        float z;
        if (usingAccelerometer) {
            final float alpha = 0.8f;
            for (int i = 0; i < 3; i++) {
                filteredGravity[i] = alpha * filteredGravity[i] + (1f - alpha) * event.values[i];
            }
            x = filteredGravity[0];
            y = filteredGravity[1];
            z = filteredGravity[2];
        } else {
            x = event.values[0];
            y = event.values[1];
            z = event.values[2];
        }

        boolean nextCandidate = blocked
                ? y < EXIT_THRESHOLD && Math.abs(x) < 6.0f && Math.abs(z) < 7.0f
                : y < ENTER_THRESHOLD && Math.abs(x) < 5.0f && Math.abs(z) < 6.0f;
        updateBlockedState(nextCandidate);
        if (keyguardManager.isKeyguardLocked()) {
            removeBlockingOverlay();
        } else if (blocked) {
            showBlockingOverlay();
        }
    }

    private void updateBlockedState(boolean newState) {
        long now = SystemClock.elapsedRealtime();
        if (newState != candidateState) {
            candidateState = newState;
            candidateSince = now;
            return;
        }
        if (blocked != newState && now - candidateSince >= STABLE_TIME_MS) {
            blocked = newState;
            if (blocked) {
                showBlockingOverlay();
            } else {
                removeBlockingOverlay();
            }
        }
    }

    private void showBlockingOverlay() {
        if (blockingOverlay != null || keyguardManager.isKeyguardLocked()
                || !Settings.canDrawOverlays(this)) {
            return;
        }

        LinearLayout overlay = new LinearLayout(this);
        overlay.setGravity(Gravity.CENTER);
        overlay.setOrientation(LinearLayout.VERTICAL);
        overlay.setRotation(180f);
        overlay.setBackgroundColor(Color.argb(210, 127, 29, 29));
        overlay.setOnTouchListener((view, event) -> true);

        TextView title = new TextView(this);
        title.setText("터치 차단 중");
        title.setTextColor(Color.WHITE);
        title.setTextSize(28);
        title.setGravity(Gravity.CENTER);
        overlay.addView(title);

        TextView help = new TextView(this);
        help.setText("휴대폰을 정상 방향으로 돌려주세요");
        help.setTextColor(Color.WHITE);
        help.setTextSize(17);
        help.setGravity(Gravity.CENTER);
        help.setPadding(24, 24, 24, 24);
        overlay.addView(help);

        int type = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                : WindowManager.LayoutParams.TYPE_PHONE;
        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                type,
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.START;

        try {
            windowManager.addView(overlay, params);
            blockingOverlay = overlay;
        } catch (RuntimeException ignored) {
            blockingOverlay = null;
        }
    }

    private void removeBlockingOverlay() {
        if (blockingOverlay != null) {
            try {
                windowManager.removeView(blockingOverlay);
            } catch (RuntimeException ignored) {
                // The system may already have removed the overlay.
            }
            blockingOverlay = null;
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Reverse Portrait Guard", NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("거꾸로 든 상태를 감지해 실수로 발생하는 터치를 차단합니다.");
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
    }

    private Notification createNotification() {
        Intent openIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, openIntent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new Notification.Builder(this, CHANNEL_ID)
                : new Notification.Builder(this);
        return builder
                .setSmallIcon(android.R.drawable.ic_lock_lock)
                .setContentTitle("터치 방지 실행 중")
                .setContentText("Reverse Portrait를 감지하고 있습니다.")
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build();
    }

    @Override
    public void onDestroy() {
        removeBlockingOverlay();
        sensorManager.unregisterListener(this);
        running = false;
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) { }
}
