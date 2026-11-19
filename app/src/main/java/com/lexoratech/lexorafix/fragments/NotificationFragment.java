package com.lexoratech.lexorafix.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.lexoratech.lexorafix.R;
import com.lexoratech.lexorafix.activities.ChatActivity;
import com.lexoratech.lexorafix.adapters.NotificationAdapter;
import com.lexoratech.lexorafix.models.AppNotification;

import java.util.ArrayList;
import java.util.List;

public class NotificationFragment extends Fragment implements NotificationAdapter.OnNotificationClickListener {

    private RecyclerView rvNotifications;
    private LinearLayout layoutEmpty;
    private NotificationAdapter adapter;
    private List<AppNotification> notificationList;
    private ListenerRegistration listenerRegistration;
    private FirebaseFirestore db;
    private String currentUserId;
    private View cardAction;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_notifications, container, false);

        // Setup Action Bar 
        ImageView btnBack = view.findViewById(R.id.btn_back);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> requireActivity().getSupportFragmentManager().popBackStack());
        }

        TextView tvTitle = view.findViewById(R.id.tv_toolbar_title);
        if (tvTitle != null) {
            tvTitle.setText("Notifications");
        }

        rvNotifications = view.findViewById(R.id.rvNotifications);
        layoutEmpty = view.findViewById(R.id.ll_empty_state);
        cardAction = view.findViewById(R.id.card_action);

        if (cardAction != null) {
            cardAction.setOnClickListener(v -> clearAllNotifications());
        }

        notificationList = new ArrayList<>();
        adapter = new NotificationAdapter(requireContext(), notificationList, this);
        rvNotifications.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvNotifications.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            currentUserId = user.getUid();
            loadNotifications();
        } else {
            updateEmptyState();
        }

        return view;
    }

    private void loadNotifications() {
        android.util.Log.d("NotificationFragment", "Loading notifications for user: " + currentUserId);
        
        listenerRegistration = db.collection("users").document(currentUserId)
                .collection("notifications")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        android.util.Log.e("NotificationFragment", "Error loading notifications: " + error.getMessage(), error);
                        
                        // If index error, try fallback query without ordering
                        loadNotificationsFallback();
                        return;
                    }
                    if (value != null) {
                        android.util.Log.d("NotificationFragment", "Received " + value.size() + " notifications");
                        notificationList.clear();
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            AppNotification notification = doc.toObject(AppNotification.class);
                            if (notification != null) {
                                notification.setId(doc.getId());
                                notificationList.add(notification);
                            }
                        }

                        adapter.notifyDataSetChanged();
                        updateEmptyState();
                    }
                });
    }

    /**
     * Fallback: load notifications without orderBy (in case Firestore index is missing).
     * This ensures notifications still appear even without descending timestamp index.
     */
    private void loadNotificationsFallback() {
        android.util.Log.w("NotificationFragment", "Using fallback query without orderBy");
        
        // Remove the failed listener first
        if (listenerRegistration != null) {
            listenerRegistration.remove();
        }
        
        listenerRegistration = db.collection("users").document(currentUserId)
                .collection("notifications")
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        android.util.Log.e("NotificationFragment", "Fallback also failed: " + error.getMessage(), error);
                        updateEmptyState();
                        return;
                    }
                    if (value != null) {
                        android.util.Log.d("NotificationFragment", "Fallback received " + value.size() + " notifications");
                        notificationList.clear();
                        for (DocumentSnapshot doc : value.getDocuments()) {
                            AppNotification notification = doc.toObject(AppNotification.class);
                            if (notification != null) {
                                notification.setId(doc.getId());
                                notificationList.add(notification);
                            }
                        }

                        // Sort manually since we couldn't use orderBy
                        notificationList.sort((a, b) -> {
                            if (a.getTimestamp() == null && b.getTimestamp() == null) return 0;
                            if (a.getTimestamp() == null) return 1;
                            if (b.getTimestamp() == null) return -1;
                            return b.getTimestamp().compareTo(a.getTimestamp());
                        });

                        adapter.notifyDataSetChanged();
                        updateEmptyState();
                    }
                });
    }

    private void updateEmptyState() {
        if (notificationList.isEmpty()) {
            layoutEmpty.setVisibility(View.VISIBLE);
            rvNotifications.setVisibility(View.GONE);
            if (cardAction != null) cardAction.setVisibility(View.GONE);
        } else {
            layoutEmpty.setVisibility(View.GONE);
            rvNotifications.setVisibility(View.VISIBLE);
            if (cardAction != null) cardAction.setVisibility(View.VISIBLE);
        }
    }

    private void clearAllNotifications() {
        if (currentUserId == null || notificationList.isEmpty()) return;
        
        android.app.Dialog dialog = new android.app.Dialog(requireContext());
        dialog.setContentView(R.layout.dialog_clear_notifications);
        
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }
        
        com.google.android.material.button.MaterialButton btnCancel = dialog.findViewById(R.id.btn_dialog_cancel);
        com.google.android.material.button.MaterialButton btnClear = dialog.findViewById(R.id.btn_dialog_clear);
        
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        
        btnClear.setOnClickListener(v -> {
            dialog.dismiss();
            com.google.firebase.firestore.WriteBatch batch = db.batch();
            for (AppNotification notif : notificationList) {
                batch.delete(db.collection("users").document(currentUserId)
                        .collection("notifications").document(notif.getId()));
            }
            batch.commit().addOnSuccessListener(aVoid -> {
                Toast.makeText(getContext(), "Notifications cleared", Toast.LENGTH_SHORT).show();
            }).addOnFailureListener(e -> {
                Toast.makeText(getContext(), "Failed to clear notifications", Toast.LENGTH_SHORT).show();
            });
        });
        
        dialog.show();
    }

    @Override
    public void onNotificationClick(AppNotification notification) {
        // Mark as read
        if (!notification.isRead()) {
            db.collection("users").document(currentUserId)
                    .collection("notifications").document(notification.getId())
                    .update("read", true);
        }

        // Navigate based on type
        if ("chat_message".equals(notification.getType()) && notification.getBookingId() != null) {
            Intent intent = new Intent(requireContext(), ChatActivity.class);
            intent.putExtra(ChatActivity.EXTRA_BOOKING_ID, notification.getBookingId());
            intent.putExtra(ChatActivity.EXTRA_WORKER_NAME, notification.getTitle());
            startActivity(intent);
        } else if (("booking_update".equals(notification.getType()) || "status_update".equals(notification.getType())) && notification.getBookingId() != null) {
            Intent intent = new Intent(requireContext(), com.lexoratech.lexorafix.activities.MainActivity.class);
            intent.putExtra("navigateTo", "bookings");
            intent.putExtra("bookingId", notification.getBookingId());
            startActivity(intent);
        } else if (("order_update".equals(notification.getType()) || "order_status_update".equals(notification.getType())) && notification.getOrderId() != null) {
            Intent intent = new Intent(requireContext(), com.lexoratech.lexorafix.activities.OrderTrackingActivity.class);
            intent.putExtra("userId", currentUserId);
            intent.putExtra("orderId", notification.getOrderId());
            startActivity(intent);
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (listenerRegistration != null) {
            listenerRegistration.remove();
        }
    }
}
