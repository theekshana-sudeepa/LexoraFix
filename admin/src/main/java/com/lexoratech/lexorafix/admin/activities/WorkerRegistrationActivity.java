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
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;
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
    private GoogleSignInClient mGoogleSignInClient;
    private static final int RC_SIGN_IN = 9001;

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
        tvBackToLogin.setOnClickListener(v -> {
            startActivity(new Intent(WorkerRegistrationActivity.this, AdminLoginActivity.class));
            finish();
        });
        
        // Google Sign In Setup
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        MaterialButton btnGoogleSignUp = findViewById(R.id.btn_google_sign_up);
        if (btnGoogleSignUp != null) {
            btnGoogleSignUp.setOnClickListener(v -> signInWithGoogle());
        }
        
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

    private void signInWithGoogle() {
        setLoading(true);
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account);
            } catch (ApiException e) {
                setLoading(false);
                Toast.makeText(this, "Google sign up failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount account) {
        AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
        auth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Check if user already exists
                        String uid = auth.getCurrentUser().getUid();
                        FirebaseFirestore.getInstance().collection("users").document(uid).get()
                                .addOnSuccessListener(documentSnapshot -> {
                                    if (documentSnapshot.exists()) {
                                        // Already registered, go to pending or dashboard
                                        Toast.makeText(this, "Account already exists. Please login.", Toast.LENGTH_LONG).show();
                                        setLoading(false);
                                        auth.signOut();
                                    } else {
                                        String name = account.getDisplayName() != null ? account.getDisplayName() : "";
                                        String email = account.getEmail() != null ? account.getEmail() : "";
                                        
                                        // Prompt for phone number
                                        android.widget.EditText input = new android.widget.EditText(this);
                                        input.setHint("Enter your phone number");
                                        input.setInputType(android.text.InputType.TYPE_CLASS_PHONE);
                                        
                                        android.widget.LinearLayout.LayoutParams lp = new android.widget.LinearLayout.LayoutParams(
                                                android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
                                                android.widget.LinearLayout.LayoutParams.WRAP_CONTENT);
                                        int margin = (int) (20 * getResources().getDisplayMetrics().density);
                                        lp.setMargins(margin, margin, margin, margin);
                                        input.setLayoutParams(lp);
                                
                                        android.widget.LinearLayout container = new android.widget.LinearLayout(this);
                                        container.setPadding(margin, margin, margin, margin);
                                        container.addView(input);
                                        
                                        setLoading(false); // Stop loading while waiting for user input
                                        
                                        new androidx.appcompat.app.AlertDialog.Builder(this)
                                                .setTitle("Phone Number Required")
                                                .setMessage("Please enter your phone number to complete registration.")
                                                .setView(container)
                                                .setCancelable(false) // Force them to enter it
                                                .setPositiveButton("Complete Setup", (dialog, which) -> {
                                                    String phone = input.getText().toString().trim();
                                                    if (android.text.TextUtils.isEmpty(phone)) {
                                                        Toast.makeText(this, "Phone number is required.", Toast.LENGTH_SHORT).show();
                                                        auth.signOut();
                                                    } else {
                                                        setLoading(true);
                                                        saveWorkerToFirestore(name, email, phone);
                                                    }
                                                })
                                                .setNegativeButton("Cancel", (dialog, which) -> {
                                                    auth.signOut();
                                                })
                                                .show();
                                    }
                                }).addOnFailureListener(e -> {
                                    setLoading(false);
                                    Toast.makeText(this, "Error checking account: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                    } else {
                        setLoading(false);
                        Toast.makeText(this, "Firebase Auth with Google failed.", Toast.LENGTH_SHORT).show();
                    }
                });
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
