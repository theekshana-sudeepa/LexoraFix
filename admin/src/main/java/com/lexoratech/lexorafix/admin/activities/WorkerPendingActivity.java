package com.lexoratech.lexorafix.admin.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.lexoratech.lexorafix.admin.R;
import com.lexoratech.lexorafix.admin.utils.ThemeUtils;

import java.util.HashMap;
import java.util.Map;

public class WorkerPendingActivity extends AppCompatActivity {

    private ImageView ivStatusIcon;
    private TextView tvStatusTitle, tvStatusMessage;
    private MaterialButton btnAction, btnResend;
    private ProgressBar progressBar;

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private boolean isEmailVerified = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeUtils.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_worker_pending);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        if (auth.getCurrentUser() == null) {
            startActivity(new Intent(this, AdminLoginActivity.class));
            finish();
            return;
        }

        initViews();
        checkCurrentStatus();

        findViewById(R.id.tv_logout).setOnClickListener(v -> {
            auth.signOut();
            startActivity(new Intent(this, AdminLoginActivity.class));
            finish();
        });
    }

    private void initViews() {
        ivStatusIcon = findViewById(R.id.iv_status_icon);
        tvStatusTitle = findViewById(R.id.tv_status_title);
        tvStatusMessage = findViewById(R.id.tv_status_message);
        btnAction = findViewById(R.id.btn_action);
        btnResend = findViewById(R.id.btn_resend);
        progressBar = findViewById(R.id.progress_bar);
    }

    private void checkCurrentStatus() {
        setLoading(true);
        // Force refresh the auth session to get latest emailVerified flag
        auth.getCurrentUser().reload().addOnCompleteListener(task -> {
            isEmailVerified = auth.getCurrentUser().isEmailVerified();
            
            String uid = auth.getCurrentUser().getUid();
            db.collection("users").document(uid).get().addOnSuccessListener(doc -> {
                boolean adminApproved = false;
                if (doc.exists() && doc.contains("adminApproved")) {
                    adminApproved = doc.getBoolean("adminApproved");
                }

                if (!isEmailVerified) {
                    showEmailVerificationUI();
                } else if (!adminApproved) {
                    // Update firestore to reflect email is verified
                    db.collection("users").document(uid).update("emailVerified", true, "status", "Pending Admin Approval");
                    showAdminApprovalUI();
                } else {
                    // Fully verified and approved
                    startActivity(new Intent(this, WorkerDashboardActivity.class));
                    finish();
                }
                setLoading(false);
            }).addOnFailureListener(e -> {
                setLoading(false);
                Toast.makeText(this, "Failed to load status.", Toast.LENGTH_SHORT).show();
            });
        });
    }

    private void showEmailVerificationUI() {
        ivStatusIcon.setImageResource(R.drawable.ic_email);
        ivStatusIcon.setColorFilter(androidx.core.content.ContextCompat.getColor(this, R.color.adminWarning));
        tvStatusTitle.setText("Verify Your Email");
        tvStatusMessage.setText("We sent a verification link to your email address. Please click the link to continue.");
        
        btnAction.setText("I've Verified My Email");
        btnAction.setOnClickListener(v -> checkCurrentStatus());
        
        btnResend.setVisibility(View.VISIBLE);
        btnResend.setOnClickListener(v -> {
            btnResend.setEnabled(false);
            auth.getCurrentUser().sendEmailVerification().addOnCompleteListener(task -> {
                Toast.makeText(this, "Verification email sent!", Toast.LENGTH_SHORT).show();
            });
        });
    }

    private void showAdminApprovalUI() {
        ivStatusIcon.setImageResource(R.drawable.ic_profile); // Replace with appropriate icon if you have one
        ivStatusIcon.setColorFilter(androidx.core.content.ContextCompat.getColor(this, R.color.adminInfo));
        tvStatusTitle.setText("Awaiting Approval");
        tvStatusMessage.setText("Your email is verified! Your account is currently under review by an Administrator. You will be granted access once approved.");

        btnAction.setText("Check Status Again");
        btnAction.setOnClickListener(v -> checkCurrentStatus());

        btnResend.setVisibility(View.GONE);
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnAction.setEnabled(!loading);
    }
}
