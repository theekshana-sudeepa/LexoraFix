package com.lexoratech.lexorafix.services;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;
import com.lexoratech.lexorafix.R;
import com.lexoratech.lexorafix.activities.SplashActivity;

/**
 * Handles incoming Firebase Cloud Messages (both data and notification payloads).
 * This ensures push notifications are displayed even when the app is in the
 * background or killed.
 */
public class MyFirebaseMessagingService extends FirebaseMessagingService {

    private static final String CHANNEL_ID = "lexorafix_notifications";
    private static final String CHANNEL_NAME = "LexoraFix Notifications";

    @Override
    public void onMessageReceived(@NonNull RemoteMessage remoteMessage) {
        super.onMessageReceived(remoteMessage);

        String title = "";
        String body = "";

        // 1) Check notification payload (sent by Firebase console or Cloud Functions)
        if (remoteMessage.getNotification() != null) {
            title = remoteMessage.getNotification().getTitle() != null
                    ? remoteMessage.getNotification().getTitle() : "";
            body = remoteMessage.getNotification().getBody() != null
                    ? remoteMessage.getNotification().getBody() : "";
        }

        // 2) Check data payload — used by Cloud Functions that send data-only messages
        if (remoteMessage.getData().size() > 0) {
            if (remoteMessage.getData().containsKey("title")) {
                title = remoteMessage.getData().get("title");
            }
            if (remoteMessage.getData().containsKey("body")) {
                body = remoteMessage.getData().get("body");
            }
        }

        // Only show notification if we have content
        if (!title.isEmpty() || !body.isEmpty()) {
            showNotification(title, body);
        }
    }

    @Override
    public void onNewToken(@NonNull String token) {
        super.onNewToken(token);
        // Save the new FCM token to Firestore for the current user
        updateTokenInFirestore(token);
    }

    private void showNotification(String title, String body) {
        // Tap action — opens the app at SplashActivity which routes appropriately
        Intent intent = new Intent(this, SplashActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, 0, intent,
                PendingIntent.FLAG_ONE_SHOT | PendingIntent.FLAG_IMMUTABLE);

        // Build notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_stat_logo)
                .setContentTitle(title)
                .setContentText(body)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                .setVibrate(new long[]{0, 300, 200, 300})
                .setContentIntent(pendingIntent)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(body));

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        // Create channel for Android 8+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Order updates, booking alerts, and promotions");
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{0, 300, 200, 300});
            notificationManager.createNotificationChannel(channel);
        }

        // Use current time as unique notification ID so multiple notifications stack
        int notificationId = (int) System.currentTimeMillis();
        notificationManager.notify(notificationId, builder.build());
    }

    /**
     * Persist the refreshed FCM token so Cloud Functions can target this device.
     */
    private void updateTokenInFirestore(String token) {
        try {
            com.google.firebase.auth.FirebaseUser user =
                    com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) {
                com.google.firebase.firestore.FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(user.getUid())
                        .update("fcmToken", token);
            }
        } catch (Exception ignored) {
            // Non-critical — token will be refreshed again on next launch
        }
    }
}
