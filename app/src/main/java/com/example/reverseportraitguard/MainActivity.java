package com.example.reverseportraitguard;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

public class MainActivity extends Activity {
    private TextView permissionState;
    private TextView accessibilityState;
    private TextView serviceState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(createContentView());
    }

    private LinearLayout createContentView() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(dp(24), dp(56), dp(24), dp(24));
        root.setBackgroundColor(Color.rgb(248, 250, 252));

        root.addView(text("Reverse Portrait Guard", 26, Color.rgb(15, 23, 42)), matchWrap());

        TextView description = text(
                "다른 앱 사용 중에도 휴대폰을 거꾸로 세우면 화면 터치를 차단합니다. " +
                        "자동 회전 설정과 무관하게 작동합니다.",
                16, Color.rgb(71, 85, 105));
        description.setPadding(0, dp(16), 0, dp(28));
        root.addView(description, matchWrap());

        permissionState = text("", 17, Color.rgb(71, 85, 105));
        permissionState.setGravity(Gravity.CENTER);
        root.addView(permissionState, matchWrap());

        Button permissionButton = new Button(this);
        permissionButton.setText("다른 앱 위에 표시 권한 설정");
        permissionButton.setOnClickListener(v -> openOverlaySettings());
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(-1, dp(56));
        buttonParams.topMargin = dp(18);
        root.addView(permissionButton, buttonParams);

        accessibilityState = text("", 17, Color.rgb(71, 85, 105));
        accessibilityState.setGravity(Gravity.CENTER);
        accessibilityState.setPadding(0, dp(24), 0, 0);
        root.addView(accessibilityState, matchWrap());

        Button accessibilityButton = new Button(this);
        accessibilityButton.setText("잠금화면 접근성 서비스 설정");
        accessibilityButton.setOnClickListener(v ->
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        LinearLayout.LayoutParams accessibilityParams = new LinearLayout.LayoutParams(-1, dp(56));
        accessibilityParams.topMargin = dp(12);
        root.addView(accessibilityButton, accessibilityParams);

        serviceState = text("", 18, Color.rgb(71, 85, 105));
        serviceState.setGravity(Gravity.CENTER);
        serviceState.setPadding(0, dp(28), 0, dp(12));
        root.addView(serviceState, matchWrap());

        Button startButton = new Button(this);
        startButton.setText("터치 방지 시작");
        startButton.setOnClickListener(v -> startGuard());
        root.addView(startButton, new LinearLayout.LayoutParams(-1, dp(56)));

        Button stopButton = new Button(this);
        stopButton.setText("터치 방지 중지");
        stopButton.setOnClickListener(v -> {
            stopService(new Intent(this, GuardService.class));
            GuardService.running = false;
            updateStatus();
        });
        LinearLayout.LayoutParams stopParams = new LinearLayout.LayoutParams(-1, dp(56));
        stopParams.topMargin = dp(10);
        root.addView(stopButton, stopParams);

        TextView note = text(
                "차단 중에는 화면 가장자리를 포함한 일반 앱 영역의 터치가 막힙니다. " +
                        "전원·볼륨 버튼과 시스템 보안 화면은 차단되지 않습니다.",
                14, Color.rgb(100, 116, 139));
        note.setPadding(0, dp(28), 0, 0);
        root.addView(note, matchWrap());
        return root;
    }

    private void openOverlaySettings() {
        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:" + getPackageName()));
        startActivity(intent);
    }

    private void startGuard() {
        if (!Settings.canDrawOverlays(this)) {
            openOverlaySettings();
            return;
        }
        if (Build.VERSION.SDK_INT >= 33 &&
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 10);
        }
        Intent intent = new Intent(this, GuardService.class);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent);
        } else {
            startService(intent);
        }
        GuardService.running = true;
        updateStatus();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateStatus();
    }

    private void updateStatus() {
        boolean permitted = Settings.canDrawOverlays(this);
        permissionState.setText(permitted ? "오버레이 권한: 허용됨" : "오버레이 권한: 필요함");
        permissionState.setTextColor(permitted ? Color.rgb(22, 163, 74) : Color.rgb(220, 38, 38));
        boolean accessibilityEnabled = isAccessibilityServiceEnabled();
        accessibilityState.setText(accessibilityEnabled
                ? "잠금화면 접근성 서비스: 켜짐"
                : "잠금화면 접근성 서비스: 꺼짐");
        accessibilityState.setTextColor(accessibilityEnabled
                ? Color.rgb(22, 163, 74) : Color.rgb(220, 38, 38));
        serviceState.setText(GuardService.running ? "보호 서비스 실행 중" : "보호 서비스 중지됨");
        serviceState.setTextColor(GuardService.running ? Color.rgb(22, 163, 74) : Color.rgb(100, 116, 139));
    }

    private boolean isAccessibilityServiceEnabled() {
        ComponentName service = new ComponentName(this, LockScreenGuardAccessibilityService.class);
        String enabled = Settings.Secure.getString(
                getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        return enabled != null && enabled.contains(service.flattenToString());
    }

    private TextView text(String value, float size, int color) {
        TextView view = new TextView(this);
        view.setText(value);
        view.setTextSize(size);
        view.setTextColor(color);
        return view;
    }

    private LinearLayout.LayoutParams matchWrap() {
        return new LinearLayout.LayoutParams(-1, -2);
    }

    private int dp(int value) {
        return Math.round(value * getResources().getDisplayMetrics().density);
    }
}
