package com.lexoratech.lexorafix.services;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;
import com.lexoratech.lexorafix.data.FirebaseAuthHelper;
import com.lexoratech.lexorafix.data.FirestoreHelper;
import com.lexoratech.lexorafix.notifications.NotificationHelper;

import java.util.HashMap;
import java.util.Map;

/**
 * Background service that listens for real-time order AND booking status
 * changes.
 * Uses Firestore snapshot listeners on user's subcollections to detect when
 * admin updates status (e.g. Confirmed → Shipped → Delivered).
 */
public class OrderStatusService extends Service {

    private static final String TAG = "OrderStatusService";
    private ListenerRegistration orderListener;
    private ListenerRegistration bookingListener;
    private NotificationHelper notificationHelper;
    private final Map<String, String> knownOrderStatuses = new HashMap<>();
    private final Map<String, String> knownBookingStatuses = new HashMap<>();
    private boolean isFirstOrderSnapshot = true;
    private boolean isFirstBookingSnapshot = true;

    @Override
    public void onCreate() {
        super.onCreate();
        notificationHelper = new NotificationHelper(this);
        startOrderListening();
        startBookingListening();
    }

    private void startOrderListening() {
        String uid = FirebaseAuthHelper.getInstance().getCurrentUid();
        if (uid == null) {
            Log.w(TAG, "No authenticated user, cannot listen for order updates");
            stopSelf();
            return;
        }

        orderListener = FirestoreHelper.getInstance().listenForUserOrders(uid,
                new FirestoreHelper.FirestoreCallback<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot result) {
                        if (isFirstOrderSnapshot) {
                            // On first snapshot, record all known statuses without notifying
                            for (DocumentChange dc : result.getDocumentChanges()) {
                                String orderId = dc.getDocument().getString("orderId");
                                String status = dc.getDocument().getString("status");
                                if (orderId != null && status != null) {
                                    knownOrderStatuses.put(orderId, status);
                                }
                            }
                            isFirstOrderSnapshot = false;
                            return;
                        }

                        // On subsequent snapshots, detect status changes
                        for (DocumentChange dc : result.getDocumentChanges()) {
                            if (dc.getType() == DocumentChange.Type.MODIFIED) {
                                String orderId = dc.getDocument().getString("orderId");
                                String newStatus = dc.getDocument().getString("status");
                                if (orderId == null || newStatus == null)
                                    continue;

                                String oldStatus = knownOrderStatuses.get(orderId);
                                if (oldStatus == null || !oldStatus.equals(newStatus)) {
                                    // Status changed! Show notification
                                    Log.d(TAG, "Order " + orderId + " status: "
                                            + oldStatus + " → " + newStatus);
                                    notificationHelper.showOrderStatusUpdate(orderId, newStatus);
                                    
                                    // Also save to Firestore so it shows up in the Notifications screen
                                    String title = "Order Update";
                                    String text = "Your order #" + orderId + " is now " + newStatus;
                                    saveNotificationToFirestore(uid, title, text, "order_update", orderId);
                                    
                                    knownOrderStatuses.put(orderId, newStatus);
                                }
                            } else if (dc.getType() == DocumentChange.Type.ADDED) {
                                // New order added after first snapshot
                                String orderId = dc.getDocument().getString("orderId");
                                String status = dc.getDocument().getString("status");
                                if (orderId != null && status != null) {
                                    knownOrderStatuses.put(orderId, status);
                                }
                            }
                        }
                    }

                    @Override
                    public void onError(String message) {
                        Log.e(TAG, "Error listening for order updates: " + message);
                        if (message != null && message.contains("PERMISSION_DENIED")) {
                            Log.w(TAG, "Lost permission to orders, stopping service.");
                            stopSelf();
                        }
                    }
                });
    }

    private void startBookingListening() {
        String uid = FirebaseAuthHelper.getInstance().getCurrentUid();
        if (uid == null)
            return;

        bookingListener = FirestoreHelper.getInstance().listenForUserBookings(uid,
                new FirestoreHelper.FirestoreCallback<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot result) {
                        if (isFirstBookingSnapshot) {
                            for (DocumentChange dc : result.getDocumentChanges()) {
                                String docId = dc.getDocument().getId();
                                String status = dc.getDocument().getString("status");
                                if (status != null) {
                                    knownBookingStatuses.put(docId, status);
                                }
                            }
                            isFirstBookingSnapshot = false;
                            return;
                        }

                        for (DocumentChange dc : result.getDocumentChanges()) {
                            if (dc.getType() == DocumentChange.Type.MODIFIED) {
                                String docId = dc.getDocument().getId();
                                String newStatus = dc.getDocument().getString("status");
                                if (newStatus == null)
                                    continue;

                                String oldStatus = knownBookingStatuses.get(docId);
                                if (oldStatus == null || !oldStatus.equals(newStatus)) {
                                    String serviceType = dc.getDocument().getString("serviceType");
                                    Log.d(TAG, "Booking " + docId + " status: "
                                            + oldStatus + " → " + newStatus);
                                    
                                    String sType = serviceType != null ? serviceType : "Service";
                                    notificationHelper.showBookingStatusUpdate(docId, sType, newStatus);
                                    
                                    // Also save to Firestore so it shows up in the Notifications screen
                                    String title = "Booking Update";
                                    String text = "Your " + sType + " booking is now " + newStatus;
                                    saveNotificationToFirestore(uid, title, text, "booking_update", docId);
                                    
                                    knownBookingStatuses.put(docId, newStatus);
                                }
                            } else if (dc.getType() == DocumentChange.Type.ADDED) {
                                String docId = dc.getDocument().getId();
                                String status = dc.getDocument().getString("status");
                                if (status != null) {
                                    knownBookingStatuses.put(docId, status);
                                }
                            }
                        }
                    }

                    @Override
                    public void onError(String message) {
                        Log.e(TAG, "Error listening for booking updates: " + message);
                        if (message != null && message.contains("PERMISSION_DENIED")) {
                            Log.w(TAG, "Lost permission to bookings, stopping service.");
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
        if (orderListener != null)
            orderListener.remove();
        if (bookingListener != null)
            bookingListener.remove();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void saveNotificationToFirestore(String uid, String title, String body, String type, String referenceId) {
        if (uid == null || uid.isEmpty()) return;
        
        // Generate a deterministic ID based on type and refId so we don't duplicate
        String notificationId = type + "_" + referenceId + "_" + (System.currentTimeMillis() / 1000);

        Map<String, Object> notificationData = new HashMap<>();
        notificationData.put("title", title);
        notificationData.put("message", body);
        notificationData.put("timestamp", com.google.firebase.firestore.FieldValue.serverTimestamp());
        notificationData.put("read", false);
        notificationData.put("type", type);
        notificationData.put("referenceId", referenceId);

        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("users")
                .document(uid)
                .collection("notifications")
                .document(notificationId)
                .set(notificationData, com.google.firebase.firestore.SetOptions.merge())
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Locally saved notification to Firestore: " + title))
                .addOnFailureListener(e -> Log.e(TAG, "Failed to save local notification to Firestore: " + e.getMessage()));
    }
}
