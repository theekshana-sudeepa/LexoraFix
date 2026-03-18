package com.lexoratech.lexorafix;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;

/**
 * Application class that creates notification channels at app startup.
 * This ensures channels exist BEFORE any FCM notification arrives,
 * which is critical for background/killed-state notification delivery on Android 8+.
 */
public class LexoraFixApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannels();
    }

    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager =
                    (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

            // Main notifications channel (matches manifest default_notification_channel_id)
            NotificationChannel mainChannel = new NotificationChannel(
                    "lexorafix_notifications",
                    "LexoraFix Notifications",
                    NotificationManager.IMPORTANCE_HIGH);
            mainChannel.setDescription("Order updates, booking alerts, and promotions");
            mainChannel.enableVibration(true);
            mainChannel.setVibrationPattern(new long[]{0, 300, 200, 300});
            notificationManager.createNotificationChannel(mainChannel);
        }
    }
}
