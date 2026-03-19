package com.lexoratech.lexorafix.admin.notifications;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.lexoratech.lexorafix.admin.R;
import com.lexoratech.lexorafix.admin.activities.ManageOrdersActivity;
import com.lexoratech.lexorafix.admin.activities.ManageBookingsActivity;

/**
 * Manages local notifications for the admin app.
 * Shows alerts when new orders/bookings arrive AND when statuses change.
 */
public class AdminNotificationHelper {

    public static final String CHANNEL_NEW_ORDERS = "admin_new_orders";
    public static final String CHANNEL_NEW_BOOKINGS = "admin_new_bookings";

    private final Context context;
    private final NotificationManagerCompat notificationManager;

    public AdminNotificationHelper(Context context) {
        this.context = context;
        this.notificationManager = NotificationManagerCompat.from(context);
        createNotificationChannels();
    }

    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager == null) return;

            NotificationChannel ordersChannel = new NotificationChannel(
                    CHANNEL_NEW_ORDERS,
                    "New Orders",
                    NotificationManager.IMPORTANCE_HIGH);
            ordersChannel.setDescription("Alerts when new orders are placed or statuses change");
            ordersChannel.enableVibration(true);

            NotificationChannel bookingsChannel = new NotificationChannel(
                    CHANNEL_NEW_BOOKINGS,
                    "New Bookings",
                    NotificationManager.IMPORTANCE_HIGH);
            bookingsChannel.setDescription("Alerts when new service bookings are made or statuses change");
            bookingsChannel.enableVibration(true);

            manager.createNotificationChannel(ordersChannel);
            manager.createNotificationChannel(bookingsChannel);
        }
    }

    /**
     * Show notification for a new order.
     */
    public void showNewOrderNotification(String orderId, String customerName, double total) {
        Intent intent = new Intent(context, ManageOrdersActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(context,
                orderId.hashCode(), intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String title = "New Order! \uD83D\uDCE6";
        String text = customerName + " placed an order — LKR " + String.format(java.util.Locale.US, "%.2f", total);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_NEW_ORDERS)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText("Order #" + orderId + "\n" + text))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        safeNotify(("order_" + orderId).hashCode(), builder);
    }

    /**
     * Show notification when an order status is changed.
     */
    public void showOrderStatusChangedNotification(String orderId, String customerName, String newStatus) {
        Intent intent = new Intent(context, ManageOrdersActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(context,
                ("status_" + orderId).hashCode(), intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String emoji = getStatusEmoji(newStatus);
        String title = "Order Status Updated " + emoji;
        String text = "Order #" + orderId + " (" + customerName + ") → " + newStatus;

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_NEW_ORDERS)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(text))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        safeNotify(("order_status_" + orderId).hashCode(), builder);
    }

    /**
     * Show notification for a new service booking.
     */
    public void showNewBookingNotification(String bookingId, String serviceType,
            String customerName, String date) {
        Intent intent = new Intent(context, ManageBookingsActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(context,
                bookingId.hashCode(), intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String title = "New Service Request! \uD83D\uDD27";
        String text = customerName + " booked " + serviceType + " for " + date;

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_NEW_BOOKINGS)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(text))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        safeNotify(("booking_" + bookingId).hashCode(), builder);
    }

    /**
     * Show notification when a booking status is changed.
     */
    public void showBookingStatusChangedNotification(String bookingId, String serviceType,
            String customerName, String newStatus) {
        Intent intent = new Intent(context, ManageBookingsActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(context,
                ("bstatus_" + bookingId).hashCode(), intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        String emoji = getBookingStatusEmoji(newStatus);
        String title = "Booking Status Updated " + emoji;
        String text = serviceType + " booking (" + customerName + ") → " + newStatus;

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_NEW_BOOKINGS)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(text))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true);

        safeNotify(("booking_status_" + bookingId).hashCode(), builder);
    }

    private void safeNotify(int id, NotificationCompat.Builder builder) {
        try {
            if (hasNotificationPermission()) {
                notificationManager.notify(id, builder.build());
            }
        } catch (SecurityException e) {
            // ignore
        }
    }

    private String getStatusEmoji(String status) {
        if (status == null) return "";
        switch (status.toLowerCase(java.util.Locale.ROOT)) {
            case "confirmed": return "\uD83D\uDCE6";
            case "processing": return "⚙️";
            case "shipped": return "\uD83D\uDE9A";
            case "delivered": return "";
            case "cancelled": return "";
            default: return "\uD83D\uDCCB";
        }
    }

    private String getBookingStatusEmoji(String status) {
        if (status == null) return "";
        switch (status.toLowerCase(java.util.Locale.ROOT)) {
            case "confirmed": return "";
            case "in progress": return "🔧";
            case "completed": return "🎉";
            case "cancelled": return "";
            default: return "\uD83D\uDCCB";
        }
    }

    private boolean hasNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(context,
                    Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }
}
