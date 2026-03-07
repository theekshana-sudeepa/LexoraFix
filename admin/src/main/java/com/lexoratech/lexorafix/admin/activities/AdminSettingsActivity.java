package com.lexoratech.lexorafix.admin.activities;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.Toolbar;
import androidx.appcompat.widget.SwitchCompat;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.messaging.FirebaseMessaging;
import com.lexoratech.lexorafix.admin.R;

public class AdminSettingsActivity extends AppCompatActivity {

    private SwitchCompat switchTheme, switchNotifications, switchMaintenance;
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

        switchTheme = findViewById(R.id.switch_theme);
        switchNotifications = findViewById(R.id.switch_notifications);
        switchMaintenance = findViewById(R.id.switch_maintenance);

        SharedPreferences prefs = getSharedPreferences("admin_prefs", Context.MODE_PRIVATE);
        boolean isDark = prefs.getBoolean("dark_mode", true);
        boolean isNotifications = prefs.getBoolean("notifications", true);

        // Actual state loads
        switchTheme.setChecked(isDark);
        switchNotifications.setChecked(isNotifications);

        loadMaintenanceState();

        switchTheme.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("dark_mode", isChecked).apply();
            AppCompatDelegate.setDefaultNightMode(
                    isChecked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
            Toast.makeText(this, isChecked ? "Dark Theme Enabled" : "Light Theme Enabled", Toast.LENGTH_SHORT).show();
        });

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
}
