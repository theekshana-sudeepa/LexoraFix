package com.lexoratech.lexorafix.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.TextView;
import android.widget.Toast;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Window;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

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
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.lexoratech.lexorafix.utils.BiometricHelper;

public class RegisterActivity extends AppCompatActivity {

    private static final String TAG = "RegisterActivity";
    private static final int RC_GOOGLE_SIGN_IN = 9002;

    private TextInputEditText etName, etEmail, etPhone, etPassword, etConfirmPassword;
    private TextInputLayout tilName, tilEmail, tilPhone, tilPassword, tilConfirmPassword;
    private MaterialButton btnRegister, btnGoogleSignUp;
    private ProgressBar progressBar;
    private GoogleSignInClient googleSignInClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        tilName = findViewById(R.id.til_name);
        tilEmail = findViewById(R.id.til_email);
        tilPhone = findViewById(R.id.til_phone);
        tilPassword = findViewById(R.id.til_password);
        tilConfirmPassword = findViewById(R.id.til_confirm_password);

        etName = findViewById(R.id.et_name);
        etEmail = findViewById(R.id.et_email);
        etPhone = findViewById(R.id.et_phone);
        etPassword = findViewById(R.id.et_password);
        etConfirmPassword = findViewById(R.id.et_confirm_password);

        btnRegister = findViewById(R.id.btn_register);
        btnGoogleSignUp = findViewById(R.id.btn_google_sign_up);
        progressBar = findViewById(R.id.progress_bar);
        TextView tvLoginLink = findViewById(R.id.tv_login_link);

        // Configure Google Sign-In
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        googleSignInClient = GoogleSignIn.getClient(this, gso);

        btnRegister.setOnClickListener(v -> attemptRegister());
        btnGoogleSignUp.setOnClickListener(v -> signUpWithGoogle());
        tvLoginLink.setOnClickListener(v -> {
            finish();
            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
        });
    }

    // ========== Google Sign-Up ==========

    private void signUpWithGoogle() {
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
                Log.w(TAG, "Google sign-up failed", e);
                Toast.makeText(this, "Google Sign-Up failed. Please try again.",
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
                    Toast.makeText(RegisterActivity.this, "Registration failed. Unable to verify user context.",
                            Toast.LENGTH_SHORT).show();
                    return;
                }

                // Check if user already exists
                FirestoreHelper.getInstance().getUserProfile(uid, new FirestoreHelper.FirestoreCallback<User>() {
                    @Override
                    public void onSuccess(User user) {
                        // User already exists — just log them in
                        finishRegistration(uid, user.getName(), user.getEmail());
                    }

                    @Override
                    public void onError(String message) {
                        // New user — create profile from Google account
                        User newUser = new User();
                        newUser.setName(account.getDisplayName() != null ? account.getDisplayName() : "");
                        newUser.setEmail(account.getEmail() != null ? account.getEmail() : "");
                        newUser.setRole("customer");

                        FirestoreHelper.getInstance().saveUserProfile(uid, newUser,
                                new FirestoreHelper.FirestoreCallback<Void>() {
                                    @Override
                                    public void onSuccess(Void result) {
                                        // Trigger welcome email for new Google signups
                                        com.lexoratech.lexorafix.network.EmailHelper.getInstance()
                                                .sendWelcomeEmail(newUser.getEmail(), newUser.getName());
                                        finishRegistration(uid, newUser.getName(), newUser.getEmail());
                                    }

                                    @Override
                                    public void onError(String msg) {
                                        // Still let them in
                                        finishRegistration(uid, account.getDisplayName(),
                                                account.getEmail());
                                    }
                                });
                    }
                });
            }

            @Override
            public void onError(String message) {
                setLoading(false);
                Toast.makeText(RegisterActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    // ========== Email/Password Registration ==========

    private void attemptRegister() {
        // Clear errors
        tilName.setError(null);
        tilEmail.setError(null);
        tilPhone.setError(null);
        tilPassword.setError(null);
        tilConfirmPassword.setError(null);

        String name = getText(etName);
        String email = getText(etEmail);
        String phone = getText(etPhone);
        String password = getText(etPassword);
        String confirmPassword = getText(etConfirmPassword);

        // Validation
        if (TextUtils.isEmpty(name)) {
            tilName.setError(getString(R.string.field_required));
            return;
        }
        if (TextUtils.isEmpty(email)) {
            tilEmail.setError(getString(R.string.field_required));
            return;
        }
        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            tilEmail.setError(getString(R.string.invalid_email));
            return;
        }
        if (TextUtils.isEmpty(phone)) {
            tilPhone.setError(getString(R.string.field_required));
            return;
        }
        if (TextUtils.isEmpty(password)) {
            tilPassword.setError(getString(R.string.field_required));
            return;
        }
        if (password.length() < 6) {
            tilPassword.setError(getString(R.string.password_too_short));
            return;
        }
        if (!password.equals(confirmPassword)) {
            tilConfirmPassword.setError(getString(R.string.passwords_dont_match));
            return;
        }

        setLoading(true);

        // Register with Firebase Auth
        FirebaseAuthHelper.getInstance().register(email, password, new FirebaseAuthHelper.AuthCallback() {
            @Override
            public void onSuccess() {
                String uid = FirebaseAuthHelper.getInstance().getCurrentUid();
                if (uid == null) {
                    setLoading(false);
                    Toast.makeText(RegisterActivity.this, "Registration failed. Please try again.", Toast.LENGTH_SHORT)
                            .show();
                    return;
                }

                // Create user profile in Firestore
                User user = new User(0, name, email, "", phone, "");
                user.setFirebaseUid(uid);
                user.setRole("customer");

                FirestoreHelper.getInstance().saveUserProfile(uid, user, new FirestoreHelper.FirestoreCallback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        com.lexoratech.lexorafix.network.EmailHelper.getInstance()
                                .sendWelcomeEmail(email, name);
                        showVerificationDialog();
                    }

                    @Override
                    public void onError(String message) {
                        Toast.makeText(RegisterActivity.this,
                                "Account created, but profile save failed. Please contact support.", Toast.LENGTH_LONG)
                                .show();
                        showVerificationDialog();
                    }
                });
            }

            @Override
            public void onError(String message) {
                setLoading(false);
                Toast.makeText(RegisterActivity.this, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    private void showVerificationDialog() {
        setLoading(false);
        com.google.firebase.auth.FirebaseUser user = FirebaseAuthHelper.getInstance().getCurrentUser();
        if (user != null) {
            user.sendEmailVerification();
        }
        FirebaseAuthHelper.getInstance().signOut();
        
        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_signup_success);
        
        Window window = dialog.getWindow();
        if (window != null) {
            window.setLayout(android.view.ViewGroup.LayoutParams.MATCH_PARENT, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT)); // Important for rounded corners
        }
        
        dialog.setCancelable(false);
        
        android.widget.ImageView ivIcon = dialog.findViewById(R.id.iv_success_icon);
        if (ivIcon != null) {
            // Optional: add a pop-in animation to the checkmark for a premium feel
            ivIcon.setScaleX(0f);
            ivIcon.setScaleY(0f);
            ivIcon.animate().scaleX(1.3f).scaleY(1.3f).setDuration(300).withEndAction(() -> {
                ivIcon.animate().scaleX(1f).scaleY(1f).setDuration(150).start();
            }).start();
        }
        
        SwitchMaterial switchBiometric = dialog.findViewById(R.id.switch_biometric);
        boolean isBiometricAvailable = BiometricHelper.isBiometricAvailable(this);
        if (isBiometricAvailable && switchBiometric != null) {
            switchBiometric.setVisibility(View.VISIBLE);
        }

        MaterialButton btnOk = dialog.findViewById(R.id.btn_dialog_ok);
        if (btnOk != null) {
            btnOk.setOnClickListener(v -> {
                if (switchBiometric != null && switchBiometric.isChecked()) {
                    // Try to authenticate right now to "register" it
                    BiometricHelper.showBiometricPrompt(this, "Confirm Fingerprint", "Verify your biometric to enable quick login.", new BiometricHelper.BiometricCallback() {
                        @Override
                        public void onSuccess() {
                            // Save preference
                            SharedPreferencesManager.getInstance(RegisterActivity.this).setBiometricEnabled(true);
                            
                            // Securely cache credentials for future LoginActivity auto-login
                            String currentEmail = getText(etEmail);
                            String currentPassword = getText(etPassword);
                            com.lexoratech.lexorafix.data.SecurePreferencesManager.getInstance(RegisterActivity.this)
                                    .saveCredentials(currentEmail, currentPassword);

                            dialog.dismiss();
                            finish();
                            overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
                        }

                        @Override
                        public void onError(String error) {
                            Toast.makeText(RegisterActivity.this, "Authentication failed. Leaving biometric disabled.", Toast.LENGTH_SHORT).show();
                            switchBiometric.setChecked(false);
                        }

                        @Override
                        public void onCancelled() {
                            switchBiometric.setChecked(false);
                        }
                    });
                } else {
                    // Biometric not opted in, proceed normally
                    dialog.dismiss();
                    finish();
                    overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
                }
            });
        }
        
        dialog.show();
    }

    // ========== Common ==========

    private void finishRegistration(String uid, String name, String email) {
        setLoading(false);
        SharedPreferencesManager prefs = SharedPreferencesManager.getInstance(RegisterActivity.this);
        prefs.saveLoginSession(uid,
                name != null ? name : "",
                email != null ? email : "");

        Toast.makeText(RegisterActivity.this, "Registration successful!", Toast.LENGTH_SHORT).show();

        Intent intent = new Intent(RegisterActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnRegister.setEnabled(!loading);
        btnGoogleSignUp.setEnabled(!loading);
    }

    private String getText(TextInputEditText et) {
        return et.getText() != null ? et.getText().toString().trim() : "";
    }
}
