package com.lexoratech.lexorafix.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.ScaleAnimation;
import android.widget.ImageView;
import android.widget.TextView;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.lexoratech.lexorafix.R;
import com.lexoratech.lexorafix.data.FirestoreHelper;
import com.lexoratech.lexorafix.data.SharedPreferencesManager;
import com.lexoratech.lexorafix.models.User;
import com.lexoratech.lexorafix.network.NetworkHelper;

public class SplashActivity extends AppCompatActivity {

    private static final int SPLASH_DURATION = 2500;
    private static final int PERMISSION_REQUEST_CODE = 101;
    private boolean isNavigating = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Override any process-wide overrides created by the Admin app,
        // ensuring the main customer app always follows system Dark Mode.
        androidx.appcompat.app.AppCompatDelegate.setDefaultNightMode(
                androidx.appcompat.app.AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                
        setContentView(R.layout.activity_splash);

        ImageView logo = findViewById(R.id.splash_logo);
        TextView title = findViewById(R.id.splash_title);
        TextView tagline = findViewById(R.id.splash_tagline);

        // Animate logo - scale + fade in
        AnimationSet logoAnim = new AnimationSet(true);
        ScaleAnimation scale = new ScaleAnimation(0.5f, 1f, 0.5f, 1f,
                Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        scale.setDuration(800);
        AlphaAnimation fadeIn = new AlphaAnimation(0f, 1f);
        fadeIn.setDuration(800);
        logoAnim.addAnimation(scale);
        logoAnim.addAnimation(fadeIn);
        logo.startAnimation(logoAnim);

        // Animate title - fade in with delay
        AlphaAnimation titleFade = new AlphaAnimation(0f, 1f);
        titleFade.setDuration(600);
        titleFade.setStartOffset(400);
        title.startAnimation(titleFade);

        // Animate tagline - fade in with delay
        AlphaAnimation taglineFade = new AlphaAnimation(0f, 1f);
        taglineFade.setDuration(600);
        taglineFade.setStartOffset(700);
        tagline.startAnimation(taglineFade);

        // Load data in background while splash shows
        NetworkHelper networkHelper = new NetworkHelper(this);
        networkHelper.loadAllData(new NetworkHelper.DataLoadCallback() {
            @Override
            public void onSuccess() {
                /* Data loaded */ }

            @Override
            public void onError(String message) {
                /* Will use cached data */ }
        });

        // Ask for permissions on first run
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, PERMISSION_REQUEST_CODE);
                // Do not navigate automatically, wait for onRequestPermissionsResult
                return;
            }
        }

        // Fast-path routing: If we already have a saved session, we don't need to force the user
        // to stare at the splash branding for 2.5 seconds every time they open the app.
        SharedPreferencesManager prefs = SharedPreferencesManager.getInstance(this);
        if (prefs.isLoggedIn() && FirebaseAuth.getInstance().getCurrentUser() != null) {
            // Already logged in - route instantly after data load starts
            new Handler(Looper.getMainLooper()).postDelayed(this::navigateSafely, 200);
        } else {
            // New user or unauthenticated, show the full branding pause
            new Handler(Looper.getMainLooper()).postDelayed(this::navigateSafely, SPLASH_DURATION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            // Permission answered (granted or denied), now move to next screen after a short delay
            new Handler(Looper.getMainLooper()).postDelayed(this::navigateSafely, 1000);
        }
    }

    private void navigateSafely() {
        if (isNavigating) return;
        isNavigating = true;
        navigateToNextScreen();
    }

    private void navigateToNextScreen() {
        SharedPreferencesManager prefs = SharedPreferencesManager.getInstance(this);
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();

        if (firebaseUser != null) {
            String uid = firebaseUser.getUid();
            String email = firebaseUser.getEmail() != null ? firebaseUser.getEmail() : "";

            // Always check status on app launch to intercept mid-session bans
            FirestoreHelper.getInstance().getUserProfile(uid, new FirestoreHelper.FirestoreCallback<User>() {
                @Override
                public void onSuccess(User user) {
                    if ("Banned".equals(user.getStatus())) {
                        FirebaseAuth.getInstance().signOut();
                        prefs.logout();
                        goToLogin();
                        return;
                    }

                    if (!prefs.isLoggedIn()) {
                        prefs.saveLoginSession(uid,
                                user.getName() != null ? user.getName() : "",
                                user.getEmail() != null ? user.getEmail() : email);
                    }
                    gateKeepWithBiometric(prefs);
                }

                @Override
                public void onError(String message) {
                    if (!prefs.isLoggedIn()) {
                        prefs.saveLoginSession(uid, "", email);
                    }
                    gateKeepWithBiometric(prefs);
                }
            });
        } else {
            // No Firebase user — go to login
            goToLogin();
        }
    }

    private void gateKeepWithBiometric(SharedPreferencesManager prefs) {
        if (prefs.isBiometricEnabled() && com.lexoratech.lexorafix.utils.BiometricHelper.isBiometricAvailable(this)) {
            com.lexoratech.lexorafix.utils.BiometricHelper.showBiometricPrompt(this, "Unlock LexoraFix", "Verify your identity or use your PIN to continue.", new com.lexoratech.lexorafix.utils.BiometricHelper.BiometricCallback() {
                @Override
                public void onSuccess() {
                    goToMain();
                }

                @Override
                public void onError(String error) {
                    android.widget.Toast.makeText(SplashActivity.this, "Authentication failed. Try again.", android.widget.Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onCancelled() {
                    finishAffinity(); // Exit app if cancelled
                }
            });
        } else {
            goToMain();
        }
    }

    private void goToMain() {
        Intent intent = new Intent(SplashActivity.this, MainActivity.class);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }

    private void goToLogin() {
        Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        finish();
    }
}
