package com.example.reverseportraitguard;

import android.accessibilityservice.AccessibilityService;
import android.app.KeyguardManager;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.SystemClock;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityEvent;
import android.widget.LinearLayout;
import android.widget.TextView;

public class LockScreenGuardAccessibilityService extends AccessibilityService
        implements SensorEventListener {
    private static final long STABLE_TIME_MS = 300L;
    private static final float ENTER_THRESHOLD = -7.0f;
    private static final float EXIT_THRESHOLD = -5.5f;

    private SensorManager sensorManager;
    private Sensor orientationSensor;
    private WindowManager windowManager;
    private KeyguardManager keyguardManager;
    private View overlay;
    private boolean usingAccelerometer;
    private boolean reversePortrait;
    private boolean candidateState;
    private long candidateSince;
    private final float[] filteredGravity = new float[3];

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
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

        boolean nextCandidate = reversePortrait
                ? y < EXIT_THRESHOLD && Math.abs(x) < 6.0f && Math.abs(z) < 7.0f
                : y < ENTER_THRESHOLD && Math.abs(x) < 5.0f && Math.abs(z) < 6.0f;
        updateOrientation(nextCandidate);
        updateOverlay();
    }

    private void updateOrientation(boolean newState) {
        long now = SystemClock.elapsedRealtime();
        if (newState != candidateState) {
            candidateState = newState;
            candidateSince = now;
            return;
        }
        if (reversePortrait != newState && now - candidateSince >= STABLE_TIME_MS) {
            reversePortrait = newState;
        }
    }

    private void updateOverlay() {
        if (reversePortrait && keyguardManager.isKeyguardLocked()) {
            showOverlay();
        } else {
            removeOverlay();
        }
    }

    private void showOverlay() {
        if (overlay != null) {
            return;
        }
        LinearLayout blocker = new LinearLayout(this);
        blocker.setGravity(Gravity.CENTER);
        blocker.setOrientation(LinearLayout.VERTICAL);
        blocker.setRotation(180f);
        blocker.setBackgroundColor(Color.argb(215, 100, 20, 20));
        blocker.setOnTouchListener((view, event) -> true);

        TextView title = new TextView(this);
        title.setText("잠금화면 터치 차단 중");
        title.setTextColor(Color.WHITE);
        title.setTextSize(27);
        title.setGravity(Gravity.CENTER);
        blocker.addView(title);

        TextView help = new TextView(this);
        help.setText("휴대폰을 정상 방향으로 돌리면 즉시 해제됩니다");
        help.setTextColor(Color.WHITE);
        help.setTextSize(16);
        help.setGravity(Gravity.CENTER);
        help.setPadding(32, 24, 32, 24);
        blocker.addView(help);

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY,
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
                        WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT);
        params.gravity = Gravity.TOP | Gravity.START;
        try {
            windowManager.addView(blocker, params);
            overlay = blocker;
        } catch (RuntimeException ignored) {
            overlay = null;
        }
    }

    private void removeOverlay() {
        if (overlay != null) {
            try {
                windowManager.removeView(overlay);
            } catch (RuntimeException ignored) {
                // System UI may have already detached the accessibility overlay.
            }
            overlay = null;
        }
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        updateOverlay();
    }

    @Override
    public void onInterrupt() {
        removeOverlay();
    }

    @Override
    public void onDestroy() {
        removeOverlay();
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
        super.onDestroy();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) { }
}
