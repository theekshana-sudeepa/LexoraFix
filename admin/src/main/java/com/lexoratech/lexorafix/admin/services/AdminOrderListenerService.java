package com.lexoratech.lexorafix.admin.services;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.Nullable;

import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.lexoratech.lexorafix.admin.notifications.AdminNotificationHelper;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Background service that listens for new orders/bookings AND status modifications.
 * Uses Firestore snapshot listeners on top-level collections.
 */
public class AdminOrderListenerService extends Service {

    private static final String TAG = "AdminOrderListener";
    private ListenerRegistration orderListener;
    private ListenerRegistration bookingListener;
    private AdminNotificationHelper notificationHelper;
    private final Set<String> knownOrderIds = new HashSet<>();
    private final Set<String> knownBookingIds = new HashSet<>();
    private final Map<String, String> orderStatuses = new HashMap<>();
    private final Map<String, String> bookingStatuses = new HashMap<>();
    private boolean isFirstOrderSnapshot = true;
    private boolean isFirstBookingSnapshot = true;

    @Override
    public void onCreate() {
        super.onCreate();
        notificationHelper = new AdminNotificationHelper(this);
        startOrderListener();
        startBookingListener();
        Log.d(TAG, "Admin notification listeners started");
    }

    private void startOrderListener() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        orderListener = db.collection("orders")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((querySnapshot, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error listening for orders: " + error.getMessage());
                        return;
                    }
                    if (querySnapshot == null) return;

                    if (isFirstOrderSnapshot) {
                        // Seed known orders on first snapshot
                        for (DocumentChange dc : querySnapshot.getDocumentChanges()) {
                            String docId = dc.getDocument().getId();
                            knownOrderIds.add(docId);
                            String status = dc.getDocument().getString("status");
                            if (status != null) {
                                orderStatuses.put(docId, status);
                            }
                        }
                        isFirstOrderSnapshot = false;
                        return;
                    }

                    for (DocumentChange dc : querySnapshot.getDocumentChanges()) {
                        String docId = dc.getDocument().getId();

                        if (dc.getType() == DocumentChange.Type.ADDED) {
                            if (!knownOrderIds.contains(docId)) {
                                knownOrderIds.add(docId);
                                String orderId = dc.getDocument().getString("orderId");
                                String customerName = dc.getDocument().getString("userName");
                                Object totalObj = dc.getDocument().get("totalPrice");
                                double total = totalObj instanceof Number
                                        ? ((Number) totalObj).doubleValue() : 0;
                                String status = dc.getDocument().getString("status");
                                if (status != null) orderStatuses.put(docId, status);

                                Log.d(TAG, "New order: " + orderId + " from " + customerName);
                                notificationHelper.showNewOrderNotification(
                                        orderId != null ? orderId : docId,
                                        customerName != null ? customerName : "Customer",
                                        total);
                            }
                        } else if (dc.getType() == DocumentChange.Type.MODIFIED) {
                            String newStatus = dc.getDocument().getString("status");
                            String oldStatus = orderStatuses.get(docId);

                            if (newStatus != null && !newStatus.equals(oldStatus)) {
                                orderStatuses.put(docId, newStatus);
                                String orderId = dc.getDocument().getString("orderId");
                                String customerName = dc.getDocument().getString("userName");

                                Log.d(TAG, "Order " + orderId + " status: "
                                        + oldStatus + " → " + newStatus);
                                notificationHelper.showOrderStatusChangedNotification(
                                        orderId != null ? orderId : docId,
                                        customerName != null ? customerName : "Customer",
                                        newStatus);
                            }
                        }
                    }
                });
    }

    private void startBookingListener() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        bookingListener = db.collection("bookings")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((querySnapshot, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Error listening for bookings: " + error.getMessage());
                        return;
                    }
                    if (querySnapshot == null) return;

                    if (isFirstBookingSnapshot) {
                        for (DocumentChange dc : querySnapshot.getDocumentChanges()) {
                            String docId = dc.getDocument().getId();
                            knownBookingIds.add(docId);
                            String status = dc.getDocument().getString("status");
                            if (status != null) bookingStatuses.put(docId, status);
                        }
                        isFirstBookingSnapshot = false;
                        return;
                    }

                    for (DocumentChange dc : querySnapshot.getDocumentChanges()) {
                        String docId = dc.getDocument().getId();

                        if (dc.getType() == DocumentChange.Type.ADDED) {
                            if (!knownBookingIds.contains(docId)) {
                                knownBookingIds.add(docId);
                                String serviceType = dc.getDocument().getString("serviceType");
                                String customerName = dc.getDocument().getString("userName");
                                String date = dc.getDocument().getString("date");
                                String status = dc.getDocument().getString("status");
                                if (status != null) bookingStatuses.put(docId, status);

                                Log.d(TAG, "New booking: " + serviceType + " from " + customerName);
                                notificationHelper.showNewBookingNotification(
                                        docId,
                                        serviceType != null ? serviceType : "Service",
                                        customerName != null ? customerName : "Customer",
                                        date != null ? date : "");
                            }
                        } else if (dc.getType() == DocumentChange.Type.MODIFIED) {
                            String newStatus = dc.getDocument().getString("status");
                            String oldStatus = bookingStatuses.get(docId);

                            if (newStatus != null && !newStatus.equals(oldStatus)) {
                                bookingStatuses.put(docId, newStatus);
                                String serviceType = dc.getDocument().getString("serviceType");
                                String customerName = dc.getDocument().getString("userName");

                                Log.d(TAG, "Booking " + docId + " status: "
                                        + oldStatus + " → " + newStatus);
                                notificationHelper.showBookingStatusChangedNotification(
                                        docId,
                                        serviceType != null ? serviceType : "Service",
                                        customerName != null ? customerName : "Customer",
                                        newStatus);
                            }
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
        if (orderListener != null) orderListener.remove();
        if (bookingListener != null) bookingListener.remove();
        Log.d(TAG, "Admin notification listeners stopped");
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
