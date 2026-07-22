package com.example.reverseportraitguard;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class MainActivity extends Activity {
    private TextView permissionTitle;
    private LinearLayout permissionContent;
    private TextView permissionState;
    private TextView accessibilityState;
    private TextView serviceState;
    private boolean lastAllGranted;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(createContentView());
    }

    private android.view.View createContentView() {
        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        scrollView.setBackgroundColor(Color.rgb(244, 247, 251));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER_HORIZONTAL);
        root.setPadding(dp(22), dp(38), dp(22), dp(32));

        ImageView appIcon = new ImageView(this);
        appIcon.setImageResource(R.mipmap.ic_launcher);
        appIcon.setContentDescription("Reverse Portrait Guard 아이콘");
        appIcon.setElevation(dp(8));
        LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dp(112), dp(112));
        iconParams.bottomMargin = dp(18);
        root.addView(appIcon, iconParams);

        TextView title = text("Reverse Portrait Guard", 27, Color.rgb(10, 25, 55));
        title.setGravity(Gravity.CENTER);
        title.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        root.addView(title, matchWrap());

        TextView description = text(
                "다른 앱 사용 중에도 휴대폰을 거꾸로 세우면 화면 터치를 차단합니다. " +
                        "자동 회전 설정과 무관하게 작동합니다.",
                16, Color.rgb(71, 85, 105));
        description.setGravity(Gravity.CENTER);
        description.setPadding(dp(8), dp(12), dp(8), dp(26));
        root.addView(description, matchWrap());

        LinearLayout permissionCard = createCard();
        permissionTitle = text("필수 권한", 19, Color.rgb(15, 40, 85));
        permissionTitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        permissionTitle.setPadding(0, dp(4), 0, dp(4));
        permissionTitle.setOnClickListener(v -> setPermissionExpanded(
                permissionContent.getVisibility() != android.view.View.VISIBLE));
        permissionCard.addView(permissionTitle, matchWrap());

        permissionContent = new LinearLayout(this);
        permissionContent.setOrientation(LinearLayout.VERTICAL);

        permissionState = text("", 17, Color.rgb(71, 85, 105));
        permissionState.setGravity(Gravity.CENTER);
        permissionState.setPadding(0, dp(14), 0, 0);
        permissionContent.addView(permissionState, matchWrap());

        Button permissionButton = new Button(this);
        permissionButton.setText("다른 앱 위에 표시 권한 설정");
        permissionButton.setOnClickListener(v -> openOverlaySettings());
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(-1, dp(56));
        buttonParams.topMargin = dp(18);
        permissionContent.addView(permissionButton, buttonParams);

        accessibilityState = text("", 17, Color.rgb(71, 85, 105));
        accessibilityState.setGravity(Gravity.CENTER);
        accessibilityState.setPadding(0, dp(24), 0, 0);
        permissionContent.addView(accessibilityState, matchWrap());

        Button accessibilityButton = new Button(this);
        accessibilityButton.setText("잠금화면 접근성 서비스 설정");
        accessibilityButton.setOnClickListener(v ->
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        LinearLayout.LayoutParams accessibilityParams = new LinearLayout.LayoutParams(-1, dp(56));
        accessibilityParams.topMargin = dp(12);
        permissionContent.addView(accessibilityButton, accessibilityParams);

        permissionCard.addView(permissionContent, matchWrap());

        root.addView(permissionCard, cardParams());

        LinearLayout controlCard = createCard();
        TextView controlTitle = text("보호 서비스", 19, Color.rgb(15, 40, 85));
        controlTitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        controlCard.addView(controlTitle, matchWrap());

        serviceState = text("", 18, Color.rgb(71, 85, 105));
        serviceState.setGravity(Gravity.CENTER);
        serviceState.setPadding(0, dp(28), 0, dp(12));
        controlCard.addView(serviceState, matchWrap());

        Button startButton = new Button(this);
        startButton.setText("터치 방지 시작");
        startButton.setOnClickListener(v -> startGuard());
        controlCard.addView(startButton, new LinearLayout.LayoutParams(-1, dp(56)));

        Button stopButton = new Button(this);
        stopButton.setText("터치 방지 중지");
        stopButton.setOnClickListener(v -> {
            stopService(new Intent(this, GuardService.class));
            GuardService.running = false;
            updateStatus();
        });
        LinearLayout.LayoutParams stopParams = new LinearLayout.LayoutParams(-1, dp(56));
        stopParams.topMargin = dp(10);
        controlCard.addView(stopButton, stopParams);

        Button notificationButton = new Button(this);
        notificationButton.setText("상시 알림 켜기/끄기");
        notificationButton.setOnClickListener(v -> openNotificationSettings());
        LinearLayout.LayoutParams notificationParams = new LinearLayout.LayoutParams(-1, dp(56));
        notificationParams.topMargin = dp(10);
        controlCard.addView(notificationButton, notificationParams);

        root.addView(controlCard, cardParams());

        TextView note = text(
                "차단 중에는 화면 가장자리를 포함한 일반 앱 영역의 터치가 막힙니다. " +
                        "전원·볼륨 버튼과 시스템 보안 화면은 차단되지 않습니다.",
                14, Color.rgb(100, 116, 139));
        note.setPadding(0, dp(28), 0, 0);
        note.setGravity(Gravity.CENTER);
        root.addView(note, matchWrap());

        scrollView.addView(root, new ScrollView.LayoutParams(-1, -2));
        return scrollView;
    }

    private LinearLayout createCard() {
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(18), dp(18), dp(18), dp(18));
        card.setBackground(roundedBackground(Color.WHITE, 20));
        card.setElevation(dp(3));
        return card;
    }

    private LinearLayout.LayoutParams cardParams() {
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-1, -2);
        params.bottomMargin = dp(16);
        return params;
    }

    private GradientDrawable roundedBackground(int color, int radiusDp) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setColor(color);
        drawable.setCornerRadius(dp(radiusDp));
        return drawable;
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

    private void openNotificationSettings() {
        Intent intent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager.getNotificationChannel(GuardService.CHANNEL_ID) == null) {
                NotificationChannel channel = new NotificationChannel(
                        GuardService.CHANNEL_ID,
                        "Reverse Portrait Guard",
                        NotificationManager.IMPORTANCE_LOW);
                channel.setDescription("방향 감지 보호 서비스의 실행 상태를 표시합니다.");
                manager.createNotificationChannel(channel);
            }
            intent = new Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName())
                    .putExtra(Settings.EXTRA_CHANNEL_ID, GuardService.CHANNEL_ID);
        } else {
            intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, getPackageName());
        }
        startActivity(intent);
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
        boolean allGranted = permitted && accessibilityEnabled;
        permissionTitle.setText(allGranted ? "필수 권한 (전부 허용됨)" : "필수 권한");
        if (allGranted && !lastAllGranted) {
            setPermissionExpanded(false);
        } else if (!allGranted) {
            setPermissionExpanded(true);
        }
        lastAllGranted = allGranted;
        serviceState.setText(GuardService.running ? "보호 서비스 실행 중" : "보호 서비스 중지됨");
        serviceState.setTextColor(GuardService.running ? Color.rgb(22, 163, 74) : Color.rgb(100, 116, 139));
    }

    private void setPermissionExpanded(boolean expanded) {
        permissionContent.setVisibility(expanded
                ? android.view.View.VISIBLE : android.view.View.GONE);
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
