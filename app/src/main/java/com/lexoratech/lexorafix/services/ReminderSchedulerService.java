package com.lexoratech.lexorafix.services;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import androidx.annotation.Nullable;

import com.lexoratech.lexorafix.notifications.NotificationHelper;

/**
 * Background service that manages service booking reminders.
 * Demonstrates background service pattern with scheduled notifications.
 */
public class ReminderSchedulerService extends Service {

    private Handler handler;
    private Runnable reminderChecker;
    private static final long CHECK_INTERVAL = 300000; // 5 minutes

    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler(Looper.getMainLooper());
        reminderChecker = new Runnable() {
            @Override
            public void run() {
                checkUpcomingReminders();
                handler.postDelayed(this, CHECK_INTERVAL);
            }
        };
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        handler.post(reminderChecker);
        return START_STICKY;
    }

    private void checkUpcomingReminders() {
        // In production, check database for upcoming bookings and send reminders
        // For demonstration purposes, this shows the background service architecture
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (handler != null && reminderChecker != null) {
            handler.removeCallbacks(reminderChecker);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
