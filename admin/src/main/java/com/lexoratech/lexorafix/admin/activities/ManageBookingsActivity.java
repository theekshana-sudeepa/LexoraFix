package com.lexoratech.lexorafix.admin.activities;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.lexoratech.lexorafix.admin.R;
import com.lexoratech.lexorafix.admin.adapters.AdminBookingAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ManageBookingsActivity extends AppCompatActivity {

    private static final String TAG = "ManageBookings";
    private AdminBookingAdapter adapter;
    private List<Map<String, Object>> bookingList = new ArrayList<>();
    private ListenerRegistration bookingListener;
    private SwipeRefreshLayout swipeRefresh;
    private LinearLayout emptyState;
    private TextView tvCount;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_list);

        ImageView btnBack = findViewById(R.id.btn_back);
        TextView tvTitle = findViewById(R.id.tv_title);
        RecyclerView rvList = findViewById(R.id.rv_list);
        swipeRefresh = findViewById(R.id.swipe_refresh);
        emptyState = findViewById(R.id.empty_state);
        tvCount = findViewById(R.id.tv_count);

        tvTitle.setText("Manage Bookings");
        btnBack.setOnClickListener(v -> finish());

        rvList.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AdminBookingAdapter(this, bookingList);
        rvList.setAdapter(adapter);

        // SwipeRefresh
        swipeRefresh.setColorSchemeColors(0xFF4CAF50, 0xFF6C63FF, 0xFFFF9800);
        swipeRefresh.setOnRefreshListener(() -> {
            if (bookingListener != null) {
                bookingListener.remove();
                bookingListener = null;
            }
            startListening();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        startListening();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (bookingListener != null) {
            bookingListener.remove();
            bookingListener = null;
        }
    }

    private void startListening() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Log.d(TAG, "Starting Firestore listener on collection: bookings");

        bookingListener = db.collection("bookings")
                .addSnapshotListener((querySnapshot, error) -> {
                    swipeRefresh.setRefreshing(false);
                    if (error != null) {
                        Log.e(TAG, "Bookings listener error: " + error.getMessage());
                        Log.e(TAG, "Error code: " + error.getCode());
                        loadBookingsFallback();
                        return;
                    }
                    if (querySnapshot != null) {
                        Log.d(TAG, "Snapshot size: " + querySnapshot.size() + ", fromCache: " + querySnapshot.getMetadata().isFromCache());
                        populateBookings(querySnapshot.getDocuments());
                    } else {
                        Log.w(TAG, "querySnapshot is null");
                    }
                });
    }

    private void loadBookingsFallback() {
        if (bookingListener != null) {
            bookingListener.remove();
            bookingListener = null;
        }
        FirebaseFirestore.getInstance()
                .collection("bookings")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    swipeRefresh.setRefreshing(false);
                    populateBookings(querySnapshot.getDocuments());
                })
                .addOnFailureListener(e -> {
                    swipeRefresh.setRefreshing(false);
                    Log.e(TAG, "Failed to load bookings: " + e.getMessage());
                    Toast.makeText(this, "Failed to load bookings: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    private void populateBookings(List<DocumentSnapshot> documents) {
        bookingList.clear();
        for (DocumentSnapshot doc : documents) {
            Map<String, Object> data = doc.getData();
            if (data != null) {
                data.put("_documentId", doc.getId());
                String uid = doc.getString("firebaseUid");
                if (uid != null) {
                    data.put("_firebaseUid", uid);
                }
                bookingList.add(data);
            }
        }
        
        // Ensure robust local sorting (descending)
        Collections.sort(bookingList, (a, b) -> {
            Object tsA = a.get("timestamp");
            Object tsB = b.get("timestamp");
            long timeA = 0, timeB = 0;

            if (tsA instanceof com.google.firebase.Timestamp) timeA = ((com.google.firebase.Timestamp) tsA).getSeconds();
            else if (tsA instanceof Long) timeA = (Long) tsA;

            if (tsB instanceof com.google.firebase.Timestamp) timeB = ((com.google.firebase.Timestamp) tsB).getSeconds();
            else if (tsB instanceof Long) timeB = (Long) tsB;

            int cmp = Long.compare(timeB, timeA);
            if (cmp == 0) {
                String idA = (String) a.get("_documentId");
                String idB = (String) b.get("_documentId");
                if (idA != null && idB != null) return idB.compareTo(idA);
            }
            return cmp;
        });
        
        Log.d(TAG, "Loaded " + bookingList.size() + " bookings");
        adapter.notifyDataSetChanged();

        // Update count badge
        if (tvCount != null) {
            tvCount.setText(String.valueOf(bookingList.size()));
            tvCount.setVisibility(View.VISIBLE);
        }

        // Show/hide empty state
        emptyState.setVisibility(bookingList.isEmpty() ? View.VISIBLE : View.GONE);
    }

    /**
     * Update booking status in BOTH top-level bookings AND user subcollection.
     */
    public void updateBookingStatus(int position, String newStatus) {
        if (position < 0 || position >= bookingList.size()) return;

        Map<String, Object> booking = bookingList.get(position);
        String docId = (String) booking.get("_documentId");
        String uid = (String) booking.get("_firebaseUid");

        if (docId == null) {
            Toast.makeText(this, "Error: missing document ID", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("status", newStatus);
        updates.put("statusUpdatedAt", com.google.firebase.Timestamp.now());

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("bookings").document(docId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    if (uid != null && !uid.isEmpty()) {
                        db.collection("users").document(uid)
                                .collection("bookings").document(docId)
                                .update(updates)
                                .addOnSuccessListener(aVoid2 ->
                                        Toast.makeText(this, "Status → " + newStatus + " ✅",
                                                Toast.LENGTH_SHORT).show())
                                .addOnFailureListener(e ->
                                        Toast.makeText(this, "Updated, but user sync pending",
                                                Toast.LENGTH_SHORT).show());
                    } else {
                        Toast.makeText(this, "Status → " + newStatus + " ✅",
                                Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Update failed: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show());
    }

    /**
     * Legacy method — used if adapter passes docPath string.
     */
    public void updateBookingStatus(String docPath, String newStatus) {
        for (int i = 0; i < bookingList.size(); i++) {
            String id = (String) bookingList.get(i).get("_documentId");
            if (id != null && docPath.contains(id)) {
                updateBookingStatus(i, newStatus);
                return;
            }
        }
        FirebaseFirestore.getInstance().document(docPath)
                .update("status", newStatus, "statusUpdatedAt", com.google.firebase.Timestamp.now())
                .addOnSuccessListener(aVoid ->
                        Toast.makeText(this, "Status → " + newStatus + " ✅",
                                Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Update failed: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show());
    }
}
