package com.lexoratech.lexorafix.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.GoogleAuthProvider;
import com.lexoratech.lexorafix.R;
import com.lexoratech.lexorafix.data.FirebaseAuthHelper;
import com.lexoratech.lexorafix.data.FirestoreHelper;
import com.lexoratech.lexorafix.data.SharedPreferencesManager;
import com.lexoratech.lexorafix.models.User;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";
    private static final int RC_GOOGLE_SIGN_IN = 9001;

    private TextInputEditText etEmail, etPassword;
    private TextInputLayout tilEmail, tilPassword;
    private MaterialButton btnLogin, btnGoogleSignIn;
    private ProgressBar progressBar;
    private GoogleSignInClient googleSignInClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        tilEmail = findViewById(R.id.til_email);
        tilPassword = findViewById(R.id.til_password);
        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        btnLogin = findViewById(R.id.btn_login);
        btnGoogleSignIn = findViewById(R.id.btn_google_sign_in);
        progressBar = findViewById(R.id.progress_bar);
        TextView tvRegister = findViewById(R.id.tv_register_link);
        TextView tvForgotPassword = findViewById(R.id.tv_forgot_password);

        // Configure Google Sign-In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);

        btnLogin.setOnClickListener(v -> attemptLogin());
        btnGoogleSignIn.setOnClickListener(v -> signInWithGoogle());

        tvRegister.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
        });

        tvForgotPassword.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, ForgotPasswordActivity.class));
            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
        });
        
        setupBiometricLogin();
    }
    
    // ========== Biometric Sign-In ==========
    
    private void setupBiometricLogin() {
        android.widget.ImageButton btnBiometric = findViewById(R.id.btn_biometric_login);
        
        com.lexoratech.lexorafix.data.SecurePreferencesManager securePrefs = com.lexoratech.lexorafix.data.SecurePreferencesManager.getInstance(this);
        String savedEmail = securePrefs.getSavedEmail();
        String savedPassword = securePrefs.getSavedPassword();
        boolean isBiometricsEnabled = SharedPreferencesManager.getInstance(this).isBiometricEnabled();
        boolean isHardwareAvailable = com.lexoratech.lexorafix.utils.BiometricHelper.isBiometricAvailable(this);
        
        // Only show button if hardware works, user opted in previously, and we possess secure cached credentials
        if (isHardwareAvailable && isBiometricsEnabled && !TextUtils.isEmpty(savedEmail) && !TextUtils.isEmpty(savedPassword)) {
            btnBiometric.setVisibility(View.VISIBLE);
            btnBiometric.setOnClickListener(v -> attemptBiometricLogin(savedEmail, savedPassword));
        } else {
            btnBiometric.setVisibility(View.GONE);
        }
    }
    
    private void attemptBiometricLogin(String email, String password) {
        com.lexoratech.lexorafix.utils.BiometricHelper.showBiometricPrompt(this, "Sign In", "Use your fingerprint to log into LexoraFix.", new com.lexoratech.lexorafix.utils.BiometricHelper.BiometricCallback() {
            @Override
            public void onSuccess() {
                // Set the UI fields just for visual completeness
                etEmail.setText(email);
                etPassword.setText(password);
                
                // Trigger the normal login flow programmatically
                attemptLogin();
            }

            @Override
            public void onError(String error) {
                Toast.makeText(LoginActivity.this, "Biometric sign-in failed. Please use your password.", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onCancelled() {
                // User backed out, do nothing
            }
        });
    }

    // ========== Google Sign-In ==========

    private void signInWithGoogle() {
        setLoading(true);
        Intent signInIntent = googleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_GOOGLE_SIGN_IN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_GOOGLE_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account);
            } catch (ApiException e) {
                setLoading(false);
                Log.w(TAG, "Google sign-in failed", e);
                Toast.makeText(this, "Google Sign-In failed. Please try again.",
                        Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount account) {
        AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
        FirebaseAuthHelper.getInstance().signInWithGoogleCredential(credential, new FirebaseAuthHelper.AuthCallback() {
            @Override
            public void onSuccess() {
                String uid = FirebaseAuthHelper.getInstance().getCurrentUid();
                if (uid == null) {
                    setLoading(false);
                    Toast.makeText(LoginActivity.this, "Authentication failed. Unable to verify user context.",
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                // Check if user profile already exists, if not create one
                FirestoreHelper.getInstance().getUserProfile(uid, new FirestoreHelper.FirestoreCallback<User>() {
                    @Override
                    public void onSuccess(User user) {
                        if ("Banned".equals(user.getStatus())) {
                            handleBannedUser();
                            return;
                        }
                        // Profile exists, proceed to main
                        finishLogin(uid, user.getName(), user.getEmail());
                    }

                    @Override
                    public void onError(String message) {
                        // New user — create profile from Google account info
                        User newUser = new User();
                        newUser.setName(account.getDisplayName() != null ? account.getDisplayName() : "");
                        newUser.setEmail(account.getEmail() != null ? account.getEmail() : "");
                        newUser.setRole("customer");

                        FirestoreHelper.getInstance().saveUserProfile(uid, newUser,
                                new FirestoreHelper.FirestoreCallback<Void>() {
                                    @Override
                                    public void onSuccess(Void result) {
                                        finishLogin(uid, newUser.getName(), newUser.getEmail());
                                    }

                                    @Override
                                    public void onError(String msg) {
                                        // Still let them in even if profile save fails
                                        finishLogin(uid, account.getDisplayName(),
                                                account.getEmail());
                                    }
                                });
                    }
                });
            }

            @Override
            public void onError(String message) {
                setLoading(false);
                Toast.makeText(LoginActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    // ========== Email/Password Login ==========

    private void attemptLogin() {
        tilEmail.setError(null);
        tilPassword.setError(null);

        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        String password = etPassword.getText() != null ? etPassword.getText().toString().trim() : "";

        if (TextUtils.isEmpty(email)) {
            tilEmail.setError(getString(R.string.field_required));
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError(getString(R.string.invalid_email));
            return;
        }
        if (TextUtils.isEmpty(password)) {
            tilPassword.setError(getString(R.string.field_required));
            return;
        }

        setLoading(true);

        FirebaseAuthHelper.getInstance().login(email, password, new FirebaseAuthHelper.AuthCallback() {
            @Override
            public void onSuccess() {
                String uid = FirebaseAuthHelper.getInstance().getCurrentUid();
                if (uid == null) {
                    setLoading(false);
                    Toast.makeText(LoginActivity.this, "Authentication failed. Please verify your credentials.",
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                FirestoreHelper.getInstance().getUserProfile(uid, new FirestoreHelper.FirestoreCallback<User>() {
                    @Override
                    public void onSuccess(User user) {
                        if ("Banned".equals(user.getStatus())) {
                            handleBannedUser();
                            return;
                        }

                        com.google.firebase.auth.FirebaseUser fbUser = FirebaseAuthHelper.getInstance()
                                .getCurrentUser();
                        if (fbUser != null && !fbUser.isEmailVerified()) {
                            handleUnverifiedEmail(fbUser);
                            return;
                        }

                        finishLogin(uid,
                                user.getName() != null ? user.getName() : "",
                                user.getEmail() != null ? user.getEmail() : email);
                    }

                    @Override
                    public void onError(String message) {
                        finishLogin(uid, "", email);
                    }
                });
            }

            @Override
            public void onError(String message) {
                setLoading(false);
                Toast.makeText(LoginActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void handleUnverifiedEmail(com.google.firebase.auth.FirebaseUser user) {
        setLoading(false);
        FirebaseAuthHelper.getInstance().signOut();
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Email Not Verified")
                .setMessage(
                        "Please verify your email address to log in. Check your inbox or click below to resend the verification link.")
                .setPositiveButton("Resend Email", (dialog, which) -> {
                    user.sendEmailVerification().addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(LoginActivity.this, "Verification email sent successfully.",
                                    Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(LoginActivity.this,
                                    "Failed to send verification email. Please try again later.", Toast.LENGTH_SHORT)
                                    .show();
                        }
                    });
                })
                .setNegativeButton("Cancel", null)
                .setCancelable(false)
                .show();
    }

    // ========== Common ==========

    private void finishLogin(String uid, String name, String email) {
        setLoading(false);
        SharedPreferencesManager prefs = SharedPreferencesManager.getInstance(LoginActivity.this);
        prefs.saveLoginSession(uid,
                name != null ? name : "",
                email != null ? email : "");

        Toast.makeText(LoginActivity.this,
                getString(R.string.welcome_back, name != null && !name.isEmpty() ? name : "User"),
                Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnLogin.setEnabled(!loading);
        btnGoogleSignIn.setEnabled(!loading);
    }

    private void handleBannedUser() {
        setLoading(false);
        FirebaseAuthHelper.getInstance().signOut();
        googleSignInClient.signOut();
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Account Suspended")
                .setMessage("Your account has been restricted. Please contact support.")
                .setPositiveButton("OK", null)
                .show();
    }
}
