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
        listenerRegistration = db.collection("users").document(currentUserId)
                .collection("notifications")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        return;
                    }
                    if (value != null) {
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

    private void updateEmptyState() {
        if (notificationList.isEmpty()) {
            layoutEmpty.setVisibility(View.VISIBLE);
            rvNotifications.setVisibility(View.GONE);
        } else {
            layoutEmpty.setVisibility(View.GONE);
            rvNotifications.setVisibility(View.VISIBLE);
        }
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
