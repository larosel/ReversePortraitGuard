package com.example.reverseportraitguard;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

public class MainActivity extends Activity {
    private SharedPreferences preferences;
    private Switch overlaySwitch;
    private Switch lockScreenSwitch;
    private LinearLayout overlayPermissionContent;
    private LinearLayout lockScreenPermissionContent;
    private LinearLayout unusedAppRestrictionsContent;
    private TextView permissionState;
    private TextView accessibilityState;
    private boolean updatingSwitches;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        preferences = getSharedPreferences(OverlaySettings.PREFERENCES_NAME, MODE_PRIVATE);
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
        TextView protectionTitle = text("보호 기능", 19, Color.rgb(15, 40, 85));
        protectionTitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        protectionTitle.setPadding(0, dp(4), 0, dp(8));
        permissionCard.addView(protectionTitle, matchWrap());

        overlaySwitch = new Switch(this);
        overlaySwitch.setText("일반 화면 오버레이");
        overlaySwitch.setTextSize(18);
        overlaySwitch.setChecked(preferences.getBoolean(
                OverlaySettings.KEY_SCREEN_OVERLAY_ENABLED, false));
        overlaySwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (updatingSwitches) return;
            preferences.edit().putBoolean(
                    OverlaySettings.KEY_SCREEN_OVERLAY_ENABLED, isChecked).apply();
            if (isChecked) {
                if (Settings.canDrawOverlays(this)) {
                    startGuard();
                } else {
                    openOverlaySettings();
                }
            } else {
                stopGuard();
            }
            updateStatus();
        });
        permissionCard.addView(overlaySwitch, new LinearLayout.LayoutParams(-1, dp(52)));

        overlayPermissionContent = new LinearLayout(this);
        overlayPermissionContent.setOrientation(LinearLayout.VERTICAL);

        permissionState = text("오버레이 권한이 필요합니다.", 17, Color.rgb(220, 38, 38));
        permissionState.setGravity(Gravity.CENTER);
        permissionState.setPadding(0, dp(8), 0, 0);
        overlayPermissionContent.addView(permissionState, matchWrap());

        Button permissionButton = new Button(this);
        permissionButton.setText("다른 앱 위에 표시 권한 설정");
        permissionButton.setOnClickListener(v -> openOverlaySettings());
        LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(-1, dp(56));
        buttonParams.topMargin = dp(10);
        overlayPermissionContent.addView(permissionButton, buttonParams);
        permissionCard.addView(overlayPermissionContent, matchWrap());

        lockScreenSwitch = new Switch(this);
        lockScreenSwitch.setText("잠금화면 보호");
        lockScreenSwitch.setTextSize(18);
        lockScreenSwitch.setChecked(preferences.getBoolean(
                OverlaySettings.KEY_LOCK_SCREEN_ENABLED, false));
        lockScreenSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (updatingSwitches) return;
            preferences.edit().putBoolean(
                    OverlaySettings.KEY_LOCK_SCREEN_ENABLED, isChecked).apply();
            updateStatus();
        });
        LinearLayout.LayoutParams lockSwitchParams = new LinearLayout.LayoutParams(-1, dp(52));
        lockSwitchParams.topMargin = dp(12);
        permissionCard.addView(lockScreenSwitch, lockSwitchParams);

        lockScreenPermissionContent = new LinearLayout(this);
        lockScreenPermissionContent.setOrientation(LinearLayout.VERTICAL);

        accessibilityState = text("잠금화면 보호를 사용하려면 접근성 권한이 필요합니다. " +
                        "제한된 설정으로 차단된 경우 앱 권한 화면에서 먼저 허용해주세요.",
                17, Color.rgb(220, 38, 38));
        accessibilityState.setGravity(Gravity.CENTER);
        accessibilityState.setPadding(0, dp(8), 0, 0);
        lockScreenPermissionContent.addView(accessibilityState, matchWrap());

        Button accessibilityButton = new Button(this);
        accessibilityButton.setText("잠금화면 접근성 서비스 설정");
        accessibilityButton.setOnClickListener(v ->
                startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)));
        LinearLayout.LayoutParams accessibilityParams = new LinearLayout.LayoutParams(-1, dp(56));
        accessibilityParams.topMargin = dp(10);
        lockScreenPermissionContent.addView(accessibilityButton, accessibilityParams);

        Button appPermissionButton = new Button(this);
        appPermissionButton.setText("앱 권한 설정 열기");
        appPermissionButton.setOnClickListener(v -> openAppDetailsSettings());
        LinearLayout.LayoutParams appPermissionParams = new LinearLayout.LayoutParams(-1, dp(56));
        appPermissionParams.topMargin = dp(10);
        lockScreenPermissionContent.addView(appPermissionButton, appPermissionParams);
        permissionCard.addView(lockScreenPermissionContent, matchWrap());

        unusedAppRestrictionsContent = new LinearLayout(this);
        unusedAppRestrictionsContent.setOrientation(LinearLayout.VERTICAL);

        TextView unusedAppWarning = text(
                "사용하지 않는 앱 관리가 켜져 있습니다. 장기간 앱을 열지 않아도 보호 기능이 " +
                        "중단되지 않도록 이 설정을 꺼주세요.",
                17, Color.rgb(220, 38, 38));
        unusedAppWarning.setGravity(Gravity.CENTER);
        unusedAppWarning.setPadding(0, dp(18), 0, 0);
        unusedAppRestrictionsContent.addView(unusedAppWarning, matchWrap());

        Button unusedAppSettingsButton = new Button(this);
        unusedAppSettingsButton.setText("사용하지 않는 앱 관리 끄기");
        unusedAppSettingsButton.setOnClickListener(v -> openUnusedAppRestrictionsSettings());
        LinearLayout.LayoutParams unusedAppParams = new LinearLayout.LayoutParams(-1, dp(56));
        unusedAppParams.topMargin = dp(10);
        unusedAppRestrictionsContent.addView(unusedAppSettingsButton, unusedAppParams);
        permissionCard.addView(unusedAppRestrictionsContent, matchWrap());

        root.addView(permissionCard, cardParams());

        LinearLayout appearanceCard = createCard();
        TextView appearanceTitle = text("오버레이 및 알림", 19, Color.rgb(15, 40, 85));
        appearanceTitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        appearanceCard.addView(appearanceTitle, matchWrap());

        TextView opacityTitle = text("오버레이 불투명도", 16, Color.rgb(71, 85, 105));
        opacityTitle.setPadding(0, dp(12), 0, 0);
        appearanceCard.addView(opacityTitle, matchWrap());

        int initialOpacity = preferences.getInt(
                OverlaySettings.KEY_OPACITY_PERCENT, OverlaySettings.DEFAULT_OPACITY_PERCENT);
        TextView opacityValue = text(initialOpacity + "%", 17, Color.rgb(71, 85, 105));
        opacityValue.setGravity(Gravity.CENTER);
        opacityValue.setPadding(0, dp(12), 0, 0);
        appearanceCard.addView(opacityValue, matchWrap());

        SeekBar opacitySeekBar = new SeekBar(this);
        opacitySeekBar.setMax(100);
        opacitySeekBar.setProgress(initialOpacity);
        opacitySeekBar.setContentDescription("오버레이 불투명도");
        opacitySeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                opacityValue.setText(progress + "%");
                if (fromUser) {
                    preferences.edit().putInt(OverlaySettings.KEY_OPACITY_PERCENT, progress).apply();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) { }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) { }
        });
        LinearLayout.LayoutParams seekBarParams = new LinearLayout.LayoutParams(-1, dp(48));
        seekBarParams.topMargin = dp(4);
        appearanceCard.addView(opacitySeekBar, seekBarParams);

        TextView opacityRange = text("0% (완전 투명)  —  100% (완전 불투명)",
                14, Color.rgb(100, 116, 139));
        opacityRange.setGravity(Gravity.CENTER);
        appearanceCard.addView(opacityRange, matchWrap());

        Button previewButton = new Button(this);
        previewButton.setText("오버레이 미리보기");
        previewButton.setOnClickListener(v -> showOverlayPreview(
                preferences.getInt(OverlaySettings.KEY_OPACITY_PERCENT,
                        OverlaySettings.DEFAULT_OPACITY_PERCENT)));
        LinearLayout.LayoutParams previewParams = new LinearLayout.LayoutParams(-1, dp(56));
        previewParams.topMargin = dp(14);
        appearanceCard.addView(previewButton, previewParams);

        Button notificationButton = new Button(this);
        notificationButton.setText("상시 알림 켜기/끄기");
        notificationButton.setOnClickListener(v -> openNotificationSettings());
        LinearLayout.LayoutParams notificationParams = new LinearLayout.LayoutParams(-1, dp(56));
        notificationParams.topMargin = dp(10);
        appearanceCard.addView(notificationButton, notificationParams);
        root.addView(appearanceCard, cardParams());

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

    private void showOverlayPreview(int opacityPercent) {
        Dialog dialog = new Dialog(this, android.R.style.Theme_Material_Light_NoActionBar);

        LinearLayout preview = new LinearLayout(this);
        preview.setGravity(Gravity.CENTER);
        preview.setOrientation(LinearLayout.VERTICAL);
        preview.setRotation(180f);
        preview.setBackgroundColor(Color.rgb(127, 29, 29));
        preview.setAlpha(opacityPercent / 100f);

        TextView title = text("터치 차단 중", 28, Color.WHITE);
        title.setGravity(Gravity.CENTER);
        preview.addView(title);

        TextView help = text("휴대폰을 정상 방향으로 돌려주세요.\n화면을 누르면 미리보기가 닫힙니다.",
                17, Color.WHITE);
        help.setGravity(Gravity.CENTER);
        help.setPadding(dp(24), dp(24), dp(24), dp(24));
        preview.addView(help);
        preview.setOnClickListener(v -> dialog.dismiss());

        dialog.setContentView(preview);
        Window window = dialog.getWindow();
        if (window != null) {
            window.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));
            window.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT,
                    WindowManager.LayoutParams.MATCH_PARENT);
        }
        dialog.setOnShowListener(ignored -> {
            Window shownWindow = dialog.getWindow();
            if (shownWindow != null) {
                shownWindow.setLayout(WindowManager.LayoutParams.MATCH_PARENT,
                        WindowManager.LayoutParams.MATCH_PARENT);
            }
        });
        dialog.show();
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

    private void openAppDetailsSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.parse("package:" + getPackageName()));
        startActivity(intent);
    }

    private void openUnusedAppRestrictionsSettings() {
        Intent intent;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                    Uri.parse("package:" + getPackageName()));
        } else {
            intent = new Intent("android.intent.action.AUTO_REVOKE_PERMISSIONS",
                    Uri.parse("package:" + getPackageName()));
        }
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivityForResult(intent, 11);
        } else {
            openAppDetailsSettings();
        }
    }

    private boolean areUnusedAppRestrictionsEnabled() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            return false;
        }
        try {
            return !getPackageManager().isAutoRevokeWhitelisted();
        } catch (RuntimeException ignored) {
            return false;
        }
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
        if (preferences.getBoolean(OverlaySettings.KEY_SCREEN_OVERLAY_ENABLED, false)
                && Settings.canDrawOverlays(this) && !GuardService.running) {
            startGuard();
        }
        updateStatus();
    }

    private void updateStatus() {
        boolean overlayEnabled = preferences.getBoolean(
                OverlaySettings.KEY_SCREEN_OVERLAY_ENABLED, false);
        boolean lockScreenEnabled = preferences.getBoolean(
                OverlaySettings.KEY_LOCK_SCREEN_ENABLED, false);
        updatingSwitches = true;
        overlaySwitch.setChecked(overlayEnabled);
        lockScreenSwitch.setChecked(lockScreenEnabled);
        updatingSwitches = false;

        boolean permitted = Settings.canDrawOverlays(this);
        overlayPermissionContent.setVisibility(overlayEnabled && !permitted
                ? android.view.View.VISIBLE : android.view.View.GONE);

        boolean accessibilityEnabled = isAccessibilityServiceEnabled();
        lockScreenPermissionContent.setVisibility(lockScreenEnabled && !accessibilityEnabled
                ? android.view.View.VISIBLE : android.view.View.GONE);
        unusedAppRestrictionsContent.setVisibility(
                areUnusedAppRestrictionsEnabled()
                        ? android.view.View.VISIBLE : android.view.View.GONE);
    }

    private void stopGuard() {
        stopService(new Intent(this, GuardService.class));
        GuardService.running = false;
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
