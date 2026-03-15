package com.lexoratech.lexorafix.admin.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Patterns;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.lexoratech.lexorafix.admin.R;

import android.content.Context;
import com.lexoratech.lexorafix.admin.utils.ThemeUtils;

public class AdminLoginActivity extends AppCompatActivity {

    private TextInputEditText etEmail, etPassword;
    private TextInputLayout tilEmail, tilPassword;
    private MaterialButton btnLogin;
    private ProgressBar progressBar;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeUtils.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_login);

        auth = FirebaseAuth.getInstance();

        // Check if already logged in as admin
        if (auth.getCurrentUser() != null) {
            checkAdminAndProceed();
            return;
        }

        tilEmail = findViewById(R.id.til_email);
        tilPassword = findViewById(R.id.til_password);
        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        btnLogin = findViewById(R.id.btn_login);
        progressBar = findViewById(R.id.progress_bar);

        // Apply Entrance Animations
        applyEntranceAnimations();

        btnLogin.setOnClickListener(v -> attemptLogin());
        
        TextView tvRegister = findViewById(R.id.tv_register_worker);
        tvRegister.setOnClickListener(v -> {
            startActivity(new Intent(AdminLoginActivity.this, WorkerRegistrationActivity.class));
        });
    }

    private void applyEntranceAnimations() {
        View logo = findViewById(R.id.iv_admin_logo);
        View title = findViewById(R.id.tv_title);
        View subtitle = findViewById(R.id.tv_subtitle);
        View loginBox = findViewById(R.id.cv_login_box);

        View[] views = new View[] { logo, title, subtitle, loginBox };

        for (int i = 0; i < views.length; i++) {
            if (views[i] != null) {
                views[i].setAlpha(0f);
                views[i].setTranslationY(80f);
                views[i].animate()
                        .alpha(1f)
                        .translationY(0f)
                        .setDuration(600)
                        .setStartDelay(i * 100L) // Staggered delay
                        .setInterpolator(new android.view.animation.DecelerateInterpolator(2f))
                        .start();
            }
        }
    }

    private void attemptLogin() {
        tilEmail.setError(null);
        tilPassword.setError(null);

        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";

        if (TextUtils.isEmpty(email)) {
            tilEmail.setError("Email is required");
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError("Invalid email");
            return;
        }
        if (TextUtils.isEmpty(password)) {
            tilPassword.setError("Password is required");
            return;
        }

        setLoading(true);

        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        checkAdminAndProceed();
                    } else {
                        setLoading(false);
                        String msg = task.getException() != null
                                ? task.getException().getMessage()
                                : "Login failed";
                        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void checkAdminAndProceed() {
        String uid = auth.getCurrentUser().getUid();
        FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    String role = doc.getString("role");
                    if ("admin".equals(role)) {
                        startActivity(new Intent(this, AdminDashboardActivity.class));
                        finish();
                    } else if ("worker".equals(role)) {
                        boolean emailVerified = doc.contains("emailVerified") && Boolean.TRUE.equals(doc.getBoolean("emailVerified"));
                        boolean adminApproved = doc.contains("adminApproved") && Boolean.TRUE.equals(doc.getBoolean("adminApproved"));

                        if (emailVerified && adminApproved) {
                            startActivity(new Intent(this, WorkerDashboardActivity.class));
                        } else {
                            startActivity(new Intent(this, WorkerPendingActivity.class));
                        }
                        finish();
                    } else {
                        setLoading(false);
                        auth.signOut();
                        Toast.makeText(this,
                                "Access denied. Admin or Worker privileges required.",
                                Toast.LENGTH_LONG).show();
                    }
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    auth.signOut();
                    Toast.makeText(this,
                            "Failed to verify role: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    private void setLoading(boolean loading) {
        if (progressBar != null)
            progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        if (btnLogin != null)
            btnLogin.setEnabled(!loading);
    }
}
