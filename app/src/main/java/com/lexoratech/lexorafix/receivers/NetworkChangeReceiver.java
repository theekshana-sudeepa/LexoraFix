package com.lexoratech.lexorafix.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.widget.Toast;

import com.lexoratech.lexorafix.network.ConnectivityHelper;

/**
 * Broadcast receiver to detect network connectivity changes.
 * Syncs offline data when connectivity is restored.
 */
public class NetworkChangeReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (ConnectivityManager.CONNECTIVITY_ACTION.equals(intent.getAction())) {
            boolean isConnected = ConnectivityHelper.isConnected(context);
            if (isConnected) {
                // Network restored — could trigger data sync here
                Toast.makeText(context, "Network connected", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(context, "Network disconnected — using offline data", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
