package com.lexoratech.lexorafix.fragments;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.lexoratech.lexorafix.R;
import com.lexoratech.lexorafix.activities.BookServiceActivity;
import com.lexoratech.lexorafix.activities.MapActivity;
import com.lexoratech.lexorafix.adapters.BookingAdapter;
import com.lexoratech.lexorafix.data.SharedPreferencesManager;
import com.lexoratech.lexorafix.models.ServiceBooking;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ServicesFragment extends Fragment {

    private static final String TAG = "ServicesFragment";
    private static final String CHANNEL_ID = "booking_status_channel";
    private final List<ServiceBooking> bookings = new ArrayList<>();
    private BookingAdapter adapter;
    private ListenerRegistration bookingListener;

    // Track previous statuses to detect changes
    private final Map<String, String> previousStatuses = new HashMap<>();
    private boolean isFirstLoad = true;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_services, container, false);

        createNotificationChannel();

        CardView cardInstallation = view.findViewById(R.id.card_installation);
        CardView cardRepair = view.findViewById(R.id.card_repair);
        CardView cardMaintenance = view.findViewById(R.id.card_maintenance);
        MaterialButton btnFindCenters = view.findViewById(R.id.btn_find_centers);

        cardInstallation.setOnClickListener(v -> openBooking("Installation"));
        cardRepair.setOnClickListener(v -> openBooking("Repair"));
        cardMaintenance.setOnClickListener(v -> openBooking("Maintenance"));

        btnFindCenters.setOnClickListener(v -> {
            startActivity(new Intent(requireContext(), MapActivity.class));
        });

        // Bookings list with real-time Firestore listener
        RecyclerView rvBookings = view.findViewById(R.id.rv_bookings);
        rvBookings.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new BookingAdapter(requireContext(), bookings);
        rvBookings.setAdapter(adapter);

        // Start real-time listener for bookings from Firestore
        String uid = SharedPreferencesManager.getInstance(requireContext()).getFirebaseUid();
        if (uid != null && !uid.isEmpty()) {
            startBookingListener(uid);
            saveFcmToken(uid);
        }

        return view;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && getContext() != null) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Booking Status Updates",
                    NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Notifications when your booking status changes");
            channel.enableVibration(true);
            NotificationManager manager = requireContext().getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    /**
     * Real-time Firestore listener on user's bookings subcollection.
     * Picks up status changes made by workers immediately.
     */
    private void startBookingListener(String uid) {
        bookingListener = FirebaseFirestore.getInstance()
                .collection("users").document(uid)
                .collection("bookings")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((querySnapshot, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Bookings listener error: " + error.getMessage());
                        if (error.getMessage() != null && error.getMessage().contains("PERMISSION_DENIED")) {
                            return;
                        }
                        loadBookingsFallback(uid);
                        return;
                    }
                    if (querySnapshot != null && isAdded()) {
                        bookings.clear();
                        for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                            ServiceBooking booking = new ServiceBooking();
                            booking.setServiceType(doc.getString("serviceType") != null
                                    ? doc.getString("serviceType")
                                    : "");
                            booking.setDate(doc.getString("date") != null
                                    ? doc.getString("date")
                                    : "");
                            booking.setTime(doc.getString("time") != null
                                    ? doc.getString("time")
                                    : "");
                            booking.setNotes(doc.getString("notes") != null
                                    ? doc.getString("notes")
                                    : "");
                            booking.setStatus(doc.getString("status") != null
                                    ? doc.getString("status")
                                    : "Pending");
                            // Worker details
                            booking.setAssignedWorkerName(doc.getString("assignedWorkerName"));
                            booking.setAssignedWorkerPhone(doc.getString("assignedWorkerPhone"));
                            booking.setAssignedWorkerEmail(doc.getString("assignedWorkerEmail"));

                            bookings.add(booking);

                            // Check for status changes and notify
                            String docId = doc.getId();
                            String currentStatus = booking.getStatus();
                            String prevStatus = previousStatuses.get(docId);

                            if (!isFirstLoad && prevStatus != null && !prevStatus.equals(currentStatus)) {
                                showStatusNotification(booking.getServiceType(), currentStatus, booking.getAssignedWorkerName());
                            }
                            previousStatuses.put(docId, currentStatus);
                        }

                        isFirstLoad = false;
                        Log.d(TAG, "Loaded " + bookings.size() + " bookings from Firestore");
                        adapter.notifyDataSetChanged();
                    }
                });
    }

    private void showStatusNotification(String serviceType, String newStatus, String workerName) {
        if (getContext() == null) return;

        String title;
        String body;
        switch (newStatus) {
            case "Accepted":
                title = "🤝 Worker Assigned!";
                body = (workerName != null ? workerName : "A worker") + " accepted your " + serviceType + " booking. Tap to see details!";
                break;
            case "In Progress":
                title = "🔧 Work Started!";
                body = "Your " + serviceType + " booking is now in progress!";
                break;
            case "Completed":
                title = "✅ Job Completed!";
                body = "Your " + serviceType + " booking has been completed!";
                break;
            default:
                title = "📋 Booking Updated";
                body = "Your " + serviceType + " booking status changed to: " + newStatus;
                break;
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(requireContext(), CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_notifications)
                .setContentTitle(title)
                .setContentText(body)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setVibrate(new long[]{0, 300, 200, 300});

        try {
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(requireContext());
            notificationManager.notify((int) System.currentTimeMillis(), builder.build());
        } catch (SecurityException e) {
            Log.w(TAG, "Notification permission not granted");
        }
    }

    private void loadBookingsFallback(String uid) {
        FirebaseFirestore.getInstance()
                .collection("users").document(uid)
                .collection("bookings")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!isAdded())
                        return;
                    bookings.clear();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        ServiceBooking booking = new ServiceBooking();
                        booking.setServiceType(doc.getString("serviceType") != null
                                ? doc.getString("serviceType")
                                : "");
                        booking.setDate(doc.getString("date") != null
                                ? doc.getString("date")
                                : "");
                        booking.setTime(doc.getString("time") != null
                                ? doc.getString("time")
                                : "");
                        booking.setStatus(doc.getString("status") != null
                                ? doc.getString("status")
                                : "Pending");
                        booking.setAssignedWorkerName(doc.getString("assignedWorkerName"));
                        booking.setAssignedWorkerPhone(doc.getString("assignedWorkerPhone"));
                        booking.setAssignedWorkerEmail(doc.getString("assignedWorkerEmail"));
                        bookings.add(booking);
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> Log.e(TAG, "Fallback load failed: " + e.getMessage()));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (bookingListener != null) {
            bookingListener.remove();
            bookingListener = null;
        }
    }

    private void openBooking(String serviceType) {
        Intent intent = new Intent(requireContext(), BookServiceActivity.class);
        intent.putExtra("service_type", serviceType);
        startActivity(intent);
    }

    private void saveFcmToken(String uid) {
        com.google.firebase.messaging.FirebaseMessaging.getInstance().getToken()
                .addOnSuccessListener(token -> {
                    FirebaseFirestore.getInstance()
                            .collection("users").document(uid)
                            .update("fcmToken", token)
                            .addOnSuccessListener(v -> Log.d(TAG, "FCM token saved"))
                            .addOnFailureListener(e -> Log.e(TAG, "FCM token save failed: " + e.getMessage()));
                });
    }
}
