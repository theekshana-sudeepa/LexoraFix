package com.lexoratech.lexorafix.notifications;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;

import com.lexoratech.lexorafix.R;

/**
 * Manages local notifications for the customer app.
 */
public class NotificationHelper {

    public static final String CHANNEL_ORDERS = "channel_orders";
    public static final String CHANNEL_BOOKINGS = "channel_bookings";
    public static final String CHANNEL_REMINDERS = "channel_reminders";
    public static final String CHANNEL_PROMOTIONS = "channel_promotions";

    private final Context context;
    private final NotificationManagerCompat notificationManager;

    public NotificationHelper(Context context) {
        this.context = context;
        this.notificationManager = NotificationManagerCompat.from(context);
        createNotificationChannels();
    }

    private void createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager == null)
                return;

            NotificationChannel ordersChannel = new NotificationChannel(
                    CHANNEL_ORDERS,
                    context.getString(R.string.channel_orders),
                    NotificationManager.IMPORTANCE_HIGH);
            ordersChannel.setDescription("Receive updates about your orders");
            ordersChannel.enableVibration(true);

            NotificationChannel bookingsChannel = new NotificationChannel(
                    CHANNEL_BOOKINGS,
                    context.getString(R.string.channel_bookings),
                    NotificationManager.IMPORTANCE_HIGH);
            bookingsChannel.setDescription("Receive updates about your service bookings");

            NotificationChannel remindersChannel = new NotificationChannel(
                    CHANNEL_REMINDERS,
                    context.getString(R.string.channel_reminders),
                    NotificationManager.IMPORTANCE_DEFAULT);
            remindersChannel.setDescription("Service and maintenance reminders");

            NotificationChannel promoChannel = new NotificationChannel(
                    CHANNEL_PROMOTIONS,
                    context.getString(R.string.channel_promotions),
                    NotificationManager.IMPORTANCE_LOW);
            promoChannel.setDescription("Promotional offers and deals");

            manager.createNotificationChannel(ordersChannel);
            manager.createNotificationChannel(bookingsChannel);
            manager.createNotificationChannel(remindersChannel);
            manager.createNotificationChannel(promoChannel);
        }
    }

    /**
     * Show notification confirming the user's order was placed.
     */
    public void showOrderConfirmation(String orderId) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ORDERS)
                .setSmallIcon(R.drawable.ic_notifications)
                .setContentTitle("Order Confirmed! \uD83C\uDF89")
                .setContentText("Your order #" + orderId + " has been placed successfully.")
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText("Your order #" + orderId
                                + " has been placed successfully. You'll receive updates as it progresses."))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        try {
            if (hasNotificationPermission()) {
                notificationManager.notify(orderId.hashCode(), builder.build());
            }
        } catch (SecurityException e) {
            // ignore
        }
    }

    /**
     * Show notification confirming a service booking.
     */
    public void showBookingConfirmation(String bookingId, String serviceType) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_BOOKINGS)
                .setSmallIcon(R.drawable.ic_notifications)
                .setContentTitle("Booking Confirmed! ✅")
                .setContentText(serviceType + " service has been booked successfully.")
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText("Your " + serviceType + " service booking #" + bookingId
                                + " has been confirmed. We'll notify you before the scheduled date."))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        try {
            if (hasNotificationPermission()) {
                notificationManager.notify(bookingId.hashCode(), builder.build());
            }
        } catch (SecurityException e) {
            // ignore
        }
    }

    /**
     * Show notification when order status is updated by admin.
     */
    public void showOrderStatusUpdate(String orderId, String newStatus) {
        String emoji = getStatusEmoji(newStatus);
        String title = "Order Update " + emoji;
        String text = "Your order #" + orderId + " is now " + newStatus;

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ORDERS)
                .setSmallIcon(R.drawable.ic_notifications)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(text))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        try {
            if (hasNotificationPermission()) {
                notificationManager.notify(("status_" + orderId).hashCode(), builder.build());
            }
        } catch (SecurityException e) {
            // ignore
        }
    }

    /**
     * Show a service reminder notification.
     */
    public void showServiceReminder(String title, String message) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_REMINDERS)
                .setSmallIcon(R.drawable.ic_notifications)
                .setContentTitle(title)
                .setContentText(message)
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                .setAutoCancel(true);

        try {
            if (hasNotificationPermission()) {
                notificationManager.notify(message.hashCode(), builder.build());
            }
        } catch (SecurityException e) {
            // ignore
        }
    }

    /**
     * Show notification when user account status changes.
     */
    public void showUserStatusUpdate(String newStatus) {
        String title = "Account Status Update";
        String text;
        
        if ("Banned".equalsIgnoreCase(newStatus)) {
            title = "Account Suspended 🚫";
            text = "Your LexoraFix account has been suspended by an administrator. Please contact support.";
        } else if ("Active".equalsIgnoreCase(newStatus)) {
            title = "Account Activated ✅";
            text = "Your LexoraFix account has been reactivated. Welcome back!";
        } else {
            text = "Your account status has been updated to: " + newStatus;
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_REMINDERS)
                .setSmallIcon(R.drawable.ic_notifications)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(text))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        try {
            if (hasNotificationPermission()) {
                notificationManager.notify("user_status_notice".hashCode(), builder.build());
            }
        } catch (SecurityException e) {
            // ignore
        }
    }

    /**
     * Show notification when account is banned.
     */
    public void showAccountBannedNotification() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_REMINDERS)
                .setSmallIcon(R.drawable.ic_notifications)
                .setContentTitle("Account Suspended 🚫")
                .setContentText("Your LexoraFix account has been suspended by an administrator.")
                .setStyle(new NotificationCompat.BigTextStyle().bigText(
                        "Your LexoraFix account has been suspended by an administrator. Please contact support for assistance."))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        try {
            if (hasNotificationPermission()) {
                notificationManager.notify("banned_notice".hashCode(), builder.build());
            }
        } catch (SecurityException e) {
            // ignore
        }
    }

    /**
     * Show notification when booking status is updated by admin.
     */
    public void showBookingStatusUpdate(String bookingId, String serviceType, String newStatus) {
        String emoji = getBookingStatusEmoji(newStatus);
        String title = "Booking Update " + emoji;
        String text = "Your " + serviceType + " booking is now " + newStatus;

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_BOOKINGS)
                .setSmallIcon(R.drawable.ic_notifications)
                .setContentTitle(title)
                .setContentText(text)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(text))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        try {
            if (hasNotificationPermission()) {
                notificationManager.notify(("booking_status_" + bookingId).hashCode(), builder.build());
            }
        } catch (SecurityException e) {
            // ignore
        }
    }

    private String getStatusEmoji(String status) {
        if (status == null)
            return "";
        switch (status.toLowerCase()) {
            case "confirmed":
                return "\uD83D\uDCE6";
            case "processing":
                return "⚙️";
            case "shipped":
                return "\uD83D\uDE9A";
            case "delivered":
                return "✅";
            case "cancelled":
                return "❌";
            default:
                return "\uD83D\uDCCB";
        }
    }

    private String getBookingStatusEmoji(String status) {
        if (status == null)
            return "";
        switch (status.toLowerCase()) {
            case "confirmed":
                return "✅";
            case "in progress":
                return "🔧";
            case "completed":
                return "🎉";
            case "cancelled":
                return "❌";
            default:
                return "\uD83D\uDCCB";
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
