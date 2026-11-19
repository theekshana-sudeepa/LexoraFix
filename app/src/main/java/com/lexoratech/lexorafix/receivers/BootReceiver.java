package com.lexoratech.lexorafix.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.lexoratech.lexorafix.services.ReminderSchedulerService;

/**
 * Boot receiver that restarts reminder services after device reboot.
 * Ensures scheduled reminders persist across reboots.
 */
public class BootReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            // Restart reminder service after boot
            Intent serviceIntent = new Intent(context, ReminderSchedulerService.class);
            context.startService(serviceIntent);
        }
    }
}
