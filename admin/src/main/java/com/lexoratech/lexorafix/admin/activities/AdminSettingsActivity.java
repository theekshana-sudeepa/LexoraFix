package com.lexoratech.lexorafix.admin.activities;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.appcompat.widget.SwitchCompat;
import android.widget.TextView;
import android.view.View;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;
import com.lexoratech.lexorafix.admin.R;
import com.lexoratech.lexorafix.admin.utils.ThemeUtils;

public class AdminSettingsActivity extends AppCompatActivity {

    private SwitchCompat switchNotifications, switchMaintenance;
    private View llThemeSelector;
    private TextView tvThemeStatus;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_settings);

        db = FirebaseFirestore.getInstance();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        llThemeSelector = findViewById(R.id.ll_theme_selector);
        tvThemeStatus = findViewById(R.id.tv_theme_status);
        switchNotifications = findViewById(R.id.switch_notifications);
        switchMaintenance = findViewById(R.id.switch_maintenance);

        SharedPreferences prefs = getSharedPreferences("admin_prefs", Context.MODE_PRIVATE);
        boolean isNotifications = prefs.getBoolean("notifications", true);
        
        int currentThemeMode = ThemeUtils.getThemeMode(this);
        updateThemeStatusText(currentThemeMode);

        // Actual state loads
        switchNotifications.setChecked(isNotifications);

        loadMaintenanceState();

        llThemeSelector.setOnClickListener(v -> showThemePickerDialog());

        switchNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("notifications", isChecked).apply();
            if (isChecked) {
                FirebaseMessaging.getInstance().subscribeToTopic("admin_alerts");
            } else {
                FirebaseMessaging.getInstance().unsubscribeFromTopic("admin_alerts");
            }
            String msg = isChecked ? "Push Notifications Enabled" : "Push Notifications Disabled";
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        });

        switchMaintenance.setOnCheckedChangeListener((buttonView, isChecked) -> {
            updateMaintenanceState(isChecked);
        });
    }

    private void loadMaintenanceState() {
        db.collection("system").document("settings")
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        Boolean isMaintenance = documentSnapshot.getBoolean("maintenance_mode");
                        if (isMaintenance != null) {
                            switchMaintenance.setChecked(isMaintenance);
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load settings.", Toast.LENGTH_SHORT).show();
                });
    }

    private void updateMaintenanceState(boolean isMaintenance) {
        db.collection("system").document("settings")
                .update("maintenance_mode", isMaintenance)
                .addOnSuccessListener(aVoid -> {
                    String msg = isMaintenance ? "Maintenance Mode Enabled" : "Maintenance Mode Disabled";
                    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                })
                .addOnFailureListener(e -> {
                    // Try setting it if it doesn't exist yet
                    java.util.Map<String, Object> data = new java.util.HashMap<>();
                    data.put("maintenance_mode", isMaintenance);
                    db.collection("system").document("settings")
                            .set(data)
                            .addOnSuccessListener(a -> {
                                String msg = isMaintenance ? "Maintenance Mode Enabled" : "Maintenance Mode Disabled";
                                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                            });
                });
    }

    private void updateThemeStatusText(int mode) {
        if (tvThemeStatus == null) return;
        switch (mode) {
            case ThemeUtils.THEME_LIGHT:
                tvThemeStatus.setText("Light Theme");
                break;
            case ThemeUtils.THEME_DARK:
                tvThemeStatus.setText("Dark Theme");
                break;
            case ThemeUtils.THEME_SYSTEM:
            default:
                tvThemeStatus.setText("System Default");
                break;
        }
    }

    private void showThemePickerDialog() {
        String[] options = {"System Default", "Light Theme", "Dark Theme"};
        int currentSelection = ThemeUtils.getThemeMode(this);

        new AlertDialog.Builder(this)
                .setTitle("Select App Theme")
                .setSingleChoiceItems(options, currentSelection, (dialog, which) -> {
                    ThemeUtils.setThemeMode(AdminSettingsActivity.this, which);
                    updateThemeStatusText(which);
                    dialog.dismiss();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
