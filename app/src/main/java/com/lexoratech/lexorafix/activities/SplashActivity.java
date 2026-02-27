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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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

        // Navigate after splash duration
        new Handler(Looper.getMainLooper()).postDelayed(this::navigateToNextScreen, SPLASH_DURATION);
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
                    goToMain();
                }

                @Override
                public void onError(String message) {
                    if (!prefs.isLoggedIn()) {
                        prefs.saveLoginSession(uid, "", email);
                    }
                    goToMain();
                }
            });
        } else {
            // No Firebase user — go to login
            goToLogin();
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
