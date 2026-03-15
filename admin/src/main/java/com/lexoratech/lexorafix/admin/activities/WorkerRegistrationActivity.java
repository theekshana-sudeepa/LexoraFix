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
import com.lexoratech.lexorafix.admin.utils.ThemeUtils;

import java.util.HashMap;
import java.util.Map;

public class WorkerRegistrationActivity extends AppCompatActivity {

    private TextInputEditText etName, etEmail, etPhone, etPassword;
    private TextInputLayout tilName, tilEmail, tilPhone, tilPassword;
    private MaterialButton btnRegister;
    private ProgressBar progressBar;
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeUtils.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_worker_registration);

        auth = FirebaseAuth.getInstance();

        tilName = findViewById(R.id.til_name);
        etName = findViewById(R.id.et_name);
        tilEmail = findViewById(R.id.til_email);
        etEmail = findViewById(R.id.et_email);
        tilPhone = findViewById(R.id.til_phone);
        etPhone = findViewById(R.id.et_phone);
        tilPassword = findViewById(R.id.til_password);
        etPassword = findViewById(R.id.et_password);
        btnRegister = findViewById(R.id.btn_register);
        progressBar = findViewById(R.id.progress_bar);

        btnRegister.setOnClickListener(v -> attemptRegistration());
        
        TextView tvBackToLogin = findViewById(R.id.tv_back_to_login);
        tvBackToLogin.setOnClickListener(v -> finish());
        
        applyEntranceAnimations();
    }

    private void applyEntranceAnimations() {
        View logo = findViewById(R.id.iv_logo);
        View title = findViewById(R.id.tv_title);
        View subtitle = findViewById(R.id.tv_subtitle);
        View registerBox = findViewById(R.id.cv_register_box);

        View[] views = new View[] { logo, title, subtitle, registerBox };

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

    private void attemptRegistration() {
        tilName.setError(null);
        tilEmail.setError(null);
        tilPhone.setError(null);
        tilPassword.setError(null);

        String name = etName.getText() != null ? etName.getText().toString().trim() : "";
        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        String phone = etPhone.getText() != null ? etPhone.getText().toString().trim() : "";
        String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";

        if (TextUtils.isEmpty(name)) {
            tilName.setError("Name is required");
            return;
        }
        if (TextUtils.isEmpty(email)) {
            tilEmail.setError("Email is required");
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError("Invalid email");
            return;
        }
        if (TextUtils.isEmpty(password) || password.length() < 6) {
            tilPassword.setError("Password must be at least 6 characters");
            return;
        }

        setLoading(true);

        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        sendEmailVerificationAndSaveUser(name, email, phone);
                    } else {
                        setLoading(false);
                        String msg = task.getException() != null ? task.getException().getMessage() : "Registration failed";
                        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void sendEmailVerificationAndSaveUser(String name, String email, String phone) {
        if (auth.getCurrentUser() != null) {
            auth.getCurrentUser().sendEmailVerification()
                    .addOnCompleteListener(task -> {
                        saveWorkerToFirestore(name, email, phone);
                    });
        }
    }

    private void saveWorkerToFirestore(String name, String email, String phone) {
        String uid = auth.getCurrentUser().getUid();

        Map<String, Object> workerData = new HashMap<>();
        workerData.put("name", name);
        workerData.put("email", email);
        workerData.put("phone", phone);
        workerData.put("role", "worker");
        workerData.put("status", "Pending Verification");
        workerData.put("emailVerified", false);
        workerData.put("adminApproved", false);

        FirebaseFirestore.getInstance().collection("users").document(uid)
                .set(workerData)
                .addOnSuccessListener(aVoid -> {
                    setLoading(false);
                    Toast.makeText(this, "Registration Successful. Please verify your email.", Toast.LENGTH_LONG).show();
                    startActivity(new Intent(this, WorkerPendingActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    setLoading(false);
                    Toast.makeText(this, "Failed to save profile: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnRegister.setEnabled(!loading);
        etName.setEnabled(!loading);
        etEmail.setEnabled(!loading);
        etPhone.setEnabled(!loading);
        etPassword.setEnabled(!loading);
    }
}
