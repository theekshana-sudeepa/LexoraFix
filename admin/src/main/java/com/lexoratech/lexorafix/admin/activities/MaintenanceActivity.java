package com.lexoratech.lexorafix.admin.activities;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.lexoratech.lexorafix.admin.R;

public class MaintenanceActivity extends AppCompatActivity {
    private ListenerRegistration listener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maintenance);
    }

    @Override
    protected void onStart() {
        super.onStart();
        listener = FirebaseFirestore.getInstance().collection("system").document("settings")
            .addSnapshotListener((snapshot, e) -> {
                if (snapshot != null && snapshot.exists()) {
                    Boolean isMaintenance = snapshot.getBoolean("maintenance_mode");
                    if (isMaintenance == null || !isMaintenance) {
                        // Maintenance over, go back to login
                        startActivity(new Intent(MaintenanceActivity.this, AdminLoginActivity.class));
                        finish();
                    }
                }
            });
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (listener != null) {
            listener.remove();
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        moveTaskToBack(true);
    }
}
