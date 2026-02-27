package com.lexoratech.lexorafix.services;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;
import com.lexoratech.lexorafix.data.FirebaseAuthHelper;
import com.lexoratech.lexorafix.data.FirestoreHelper;
import com.lexoratech.lexorafix.notifications.NotificationHelper;

/**
 * Background service that listens for real-time user status changes.
 * Uses a Firestore snapshot listener on the user's document to detect when
 * an admin updates the user's status (e.g., Active → Banned).
 */
public class UserStatusService extends Service {

    private static final String TAG = "UserStatusService";
    private ListenerRegistration userStatusListener;
    private NotificationHelper notificationHelper;
    private String currentUserStatus = null;
    private boolean isFirstSnapshot = true;

    @Override
    public void onCreate() {
        super.onCreate();
        notificationHelper = new NotificationHelper(this);
        startUserStatusListening();
    }

    private void startUserStatusListening() {
        String uid = FirebaseAuthHelper.getInstance().getCurrentUid();
        if (uid == null) {
            Log.w(TAG, "No authenticated user, cannot listen for status updates");
            stopSelf();
            return;
        }

        userStatusListener = FirestoreHelper.getInstance().listenForUserStatus(uid,
                new FirestoreHelper.FirestoreCallback<DocumentSnapshot>() {
                    @Override
                    public void onSuccess(DocumentSnapshot documentSnapshot) {
                        String status = documentSnapshot.getString("status");
                        if (status == null)
                            return;

                        if (isFirstSnapshot) {
                            // On first snapshot, record the current status without notifying
                            currentUserStatus = status;
                            isFirstSnapshot = false;
                            return;
                        }

                        // On subsequent snapshots, detect status changes
                        if (currentUserStatus == null || !currentUserStatus.equals(status)) {
                            // Status changed! Show notification
                            Log.d(TAG, "User status changed: " + currentUserStatus + " → " + status);
                            notificationHelper.showUserStatusUpdate(status);
                            currentUserStatus = status;
                        }
                    }

                    @Override
                    public void onError(String message) {
                        Log.e(TAG, "Error listening for user status updates: " + message);
                        if (message != null && message.contains("PERMISSION_DENIED")) {
                            Log.w(TAG, "Lost permission to user status, stopping service.");
                            stopSelf();
                        }
                    }
                });
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (userStatusListener != null) {
            userStatusListener.remove();
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
