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

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.lexoratech.lexorafix.admin.R;
import com.lexoratech.lexorafix.admin.adapters.AdminOrderAdapter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ManageOrdersActivity extends AppCompatActivity {

    private static final String TAG = "ManageOrders";
    private AdminOrderAdapter adapter;
    private final List<Map<String, Object>> orderList = new ArrayList<>();
    private ListenerRegistration orderListener;
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

        tvTitle.setText("Manage Orders");
        btnBack.setOnClickListener(v -> finish());

        rvList.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AdminOrderAdapter(this, orderList);
        rvList.setAdapter(adapter);

        // SwipeRefresh
        swipeRefresh.setColorSchemeColors(0xFFFF9800, 0xFF6C63FF, 0xFF4CAF50);
        swipeRefresh.setOnRefreshListener(() -> {
            if (orderListener != null) {
                orderListener.remove();
                orderListener = null;
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
        if (orderListener != null) {
            orderListener.remove();
            orderListener = null;
        }
    }

    private void startListening() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        orderListener = db.collection("orders")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((querySnapshot, error) -> {
                    swipeRefresh.setRefreshing(false);
                    if (error != null) {
                        Log.w(TAG, "orderBy query failed, trying without sort: " + error.getMessage());
                        loadOrdersWithoutSort();
                        return;
                    }
                    if (querySnapshot != null) {
                        populateOrders(querySnapshot.getDocuments());
                    }
                });
    }

    private void loadOrdersWithoutSort() {
        if (orderListener != null) {
            orderListener.remove();
            orderListener = null;
        }

        FirebaseFirestore.getInstance()
                .collection("orders")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    swipeRefresh.setRefreshing(false);
                    populateOrders(querySnapshot.getDocuments());
                })
                .addOnFailureListener(e -> {
                    swipeRefresh.setRefreshing(false);
                    Log.e(TAG, "Failed to load orders: " + e.getMessage());
                    Toast.makeText(this, "Failed to load orders: " + e.getMessage(),
                            Toast.LENGTH_LONG).show();
                });
    }

    private void populateOrders(List<DocumentSnapshot> documents) {
        orderList.clear();
        for (DocumentSnapshot doc : documents) {
            Map<String, Object> data = doc.getData();
            if (data != null) {
                data.put("_documentId", doc.getId());
                orderList.add(data);
            }
        }
        
        // Ensure robust local sorting (descending) regardless of Firestore indices
        Collections.sort(orderList, (a, b) -> {
            Object tsA = a.get("timestamp");
            Object tsB = b.get("timestamp");
            long timeA = 0, timeB = 0;

            if (tsA instanceof Timestamp) timeA = ((Timestamp) tsA).getSeconds();
            else if (tsA instanceof Long) timeA = (Long) tsA;

            if (tsB instanceof Timestamp) timeB = ((Timestamp) tsB).getSeconds();
            else if (tsB instanceof Long) timeB = (Long) tsB;

            int cmp = Long.compare(timeB, timeA);
            if (cmp == 0) {
                String idA = (String) a.get("_documentId");
                String idB = (String) b.get("_documentId");
                if (idA != null && idB != null) return idB.compareTo(idA);
            }
            return cmp;
        });
        
        Log.d(TAG, "Loaded " + orderList.size() + " orders");
        adapter.notifyDataSetChanged();

        // Update count badge
        if (tvCount != null) {
            tvCount.setText(String.valueOf(orderList.size()));
            tvCount.setVisibility(View.VISIBLE);
        }

        // Show/hide empty state
        emptyState.setVisibility(orderList.isEmpty() ? View.VISIBLE : View.GONE);
    }

    /**
     * Update order status — called by AdminOrderAdapter on long-press.
     * Syncs to both top-level orders and user subcollection.
     */
    public void updateOrderStatus(int position, String newStatus) {
        if (position < 0 || position >= orderList.size()) return;

        Map<String, Object> order = orderList.get(position);
        String docId = (String) order.get("_documentId");
        String uid = order.get("firebaseUid") != null ? order.get("firebaseUid").toString() : null;
        String orderId = order.get("orderId") != null ? order.get("orderId").toString() : null;

        if (docId == null) {
            Toast.makeText(this, "Error: missing document ID", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("status", newStatus);
        updates.put("statusUpdatedAt", Timestamp.now());

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Update top-level orders collection
        db.collection("orders").document(docId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    // Also update user subcollection so customer listener picks it up
                    if (uid != null && !uid.isEmpty() && orderId != null) {
                        db.collection("users").document(uid)
                                .collection("orders").document(orderId)
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
}
