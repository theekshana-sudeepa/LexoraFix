package com.lexoratech.lexorafix.admin;

import android.app.Application;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.os.Build;
import com.lexoratech.lexorafix.admin.utils.ThemeUtils;

public class AdminApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        
        // Ensure Theme is evaluated immediately on app process creation
        ThemeUtils.applyTheme(this);

        // Create notification channels early so background FCM notifications work
        createNotificationChannels();
    }

    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager =
                    (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

            NotificationChannel workerChannel = new NotificationChannel(
                    "lexorafix_admin_notifications",
                    "LexoraFix Worker Notifications",
                    NotificationManager.IMPORTANCE_HIGH);
            workerChannel.setDescription("Job alerts and chat messages");
            workerChannel.enableVibration(true);
            workerChannel.setVibrationPattern(new long[]{0, 300, 200, 300});
            notificationManager.createNotificationChannel(workerChannel);
        }
    }
}
