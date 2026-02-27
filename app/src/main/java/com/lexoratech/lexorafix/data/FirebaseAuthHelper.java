package com.lexoratech.lexorafix.data;

import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * Singleton wrapper around FirebaseAuth for authentication operations.
 */
public class FirebaseAuthHelper {

    private static FirebaseAuthHelper instance;
    private final FirebaseAuth auth;

    public interface AuthCallback {
        void onSuccess();

        void onError(String message);
    }

    private FirebaseAuthHelper() {
        auth = FirebaseAuth.getInstance();
    }

    public static synchronized FirebaseAuthHelper getInstance() {
        if (instance == null) {
            instance = new FirebaseAuthHelper();
        }
        return instance;
    }

    /**
     * Get the underlying FirebaseAuth instance.
     */
    public FirebaseAuth getFirebaseAuth() {
        return auth;
    }

    /**
     * Register a new user with email and password.
     */
    public void register(String email, String password, AuthCallback callback) {
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        callback.onSuccess();
                    } else {
                        String msg = task.getException() != null
                                ? task.getException().getMessage()
                                : "Registration failed";
                        callback.onError(msg);
                    }
                });
    }

    /**
     * Sign in an existing user with email and password.
     */
    public void login(String email, String password, AuthCallback callback) {
        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        callback.onSuccess();
                    } else {
                        String msg = task.getException() != null
                                ? task.getException().getMessage()
                                : "Login failed";
                        callback.onError(msg);
                    }
                });
    }

    /**
     * Sign in with a Google AuthCredential (from Google Sign-In flow).
     */
    public void signInWithGoogleCredential(AuthCredential credential, AuthCallback callback) {
        auth.signInWithCredential(credential)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        callback.onSuccess();
                    } else {
                        String msg = task.getException() != null
                                ? task.getException().getMessage()
                                : "Google sign-in failed";
                        callback.onError(msg);
                    }
                });
    }

    /**
     * Send a password reset email.
     */
    public void sendPasswordReset(String email, AuthCallback callback) {
        auth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        callback.onSuccess();
                    } else {
                        String msg = task.getException() != null
                                ? task.getException().getMessage()
                                : "Failed to send reset email";
                        callback.onError(msg);
                    }
                });
    }

    /**
     * Sign out the current user.
     */
    public void signOut() {
        auth.signOut();
    }

    /**
     * Get the currently signed-in FirebaseUser, or null if not signed in.
     */
    public FirebaseUser getCurrentUser() {
        return auth.getCurrentUser();
    }

    /**
     * Get the UID of the currently signed-in user, or null.
     */
    public String getCurrentUid() {
        FirebaseUser user = auth.getCurrentUser();
        return user != null ? user.getUid() : null;
    }
}
