package com.lexoratech.lexorafix.admin.activities;

import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.appcompat.widget.SwitchCompat;
import com.google.firebase.firestore.FirebaseFirestore;
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

        // Mock state loads
        switchTheme.setChecked(true); // Dark theme default
        switchNotifications.setChecked(true);

        loadMaintenanceState();

        switchTheme.setOnCheckedChangeListener((buttonView, isChecked) -> {
            Toast.makeText(this, isChecked ? "Dark Theme Enabled" : "Light Theme Enabled", Toast.LENGTH_SHORT).show();
            // In a real scenario, we'd save to SharedPreferences and recreate()
        });

        switchNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
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
