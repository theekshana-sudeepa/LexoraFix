package com.lexoratech.lexorafix.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.ChipGroup;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;
import com.lexoratech.lexorafix.R;
import com.lexoratech.lexorafix.adapters.OrderAdapter;
import com.lexoratech.lexorafix.data.FirestoreHelper;
import com.lexoratech.lexorafix.data.SharedPreferencesManager;
import com.lexoratech.lexorafix.models.Order;
import com.lexoratech.lexorafix.notifications.NotificationHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OrderHistoryFragment extends Fragment {

    private static final String TAG = "OrderHistoryFragment";
    private ListenerRegistration orderListener;
    private OrderAdapter adapter;
    private final List<Order> allOrders = new ArrayList<>();
    private final List<Order> filteredOrders = new ArrayList<>();
    private final Map<String, String> previousStatuses = new HashMap<>();
    private boolean isFirstLoad = true;
    private String currentFilter = "All";

    private RecyclerView rvOrders;
    private LinearLayout layoutEmpty;
    private ProgressBar progressBar;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_order_history, container, false);

        // Action bar
        ImageView btnBack = view.findViewById(R.id.btn_back);
        TextView tvToolbarTitle = view.findViewById(R.id.tv_toolbar_title);
        if (tvToolbarTitle != null) tvToolbarTitle.setText("My Orders");
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                    getParentFragmentManager().popBackStack();
                }
            });
        }

        rvOrders = view.findViewById(R.id.rv_orders);
        layoutEmpty = view.findViewById(R.id.layout_empty);
        progressBar = view.findViewById(R.id.progress_bar);

        rvOrders.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new OrderAdapter(requireContext(), filteredOrders);
        rvOrders.setAdapter(adapter);

        // Filter chips
        ChipGroup chipGroup = view.findViewById(R.id.chip_group_filter);
        chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int id = checkedIds.get(0);
            if (id == R.id.chip_all) currentFilter = "All";
            else if (id == R.id.chip_confirmed) currentFilter = "Confirmed";
            else if (id == R.id.chip_processing) currentFilter = "Processing";
            else if (id == R.id.chip_shipped) currentFilter = "Shipped";
            else if (id == R.id.chip_delivered) currentFilter = "Delivered";
            else if (id == R.id.chip_cancelled) currentFilter = "Cancelled";
            applyFilter();
        });

        // Show loading
        progressBar.setVisibility(View.VISIBLE);

        // Start listening
        String uid = SharedPreferencesManager.getInstance(requireContext()).getFirebaseUid();
        if (uid != null && !uid.isEmpty()) {
            startOrderListener(uid);
        } else {
            progressBar.setVisibility(View.GONE);
            showEmptyState();
        }

        return view;
    }

    private void startOrderListener(String uid) {
        orderListener = FirestoreHelper.getInstance().listenForUserOrders(uid,
                new FirestoreHelper.FirestoreCallback<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot result) {
                        if (!isAdded()) return;
                        progressBar.setVisibility(View.GONE);

                        // Detect status changes and show notifications
                        if (!isFirstLoad) {
                            for (DocumentChange change : result.getDocumentChanges()) {
                                if (change.getType() == DocumentChange.Type.MODIFIED) {
                                    DocumentSnapshot doc = change.getDocument();
                                    String docId = doc.getId();
                                    String newStatus = doc.getString("status");
                                    String orderId = doc.getString("orderId");
                                    String prevStatus = previousStatuses.get(docId);

                                    if (newStatus != null && prevStatus != null
                                            && !newStatus.equals(prevStatus)) {
                                        NotificationHelper helper = new NotificationHelper(requireContext());
                                        helper.showOrderStatusUpdate(
                                                orderId != null ? orderId : docId, newStatus);
                                    }
                                }
                            }
                        }

                        // Update order list and cache statuses
                        allOrders.clear();
                        previousStatuses.clear();
                        for (DocumentSnapshot doc : result.getDocuments()) {
                            Map<String, Object> data = doc.getData();
                            if (data != null) {
                                Order order = Order.fromMap(data);
                                if (order != null) {
                                    order.setDocumentId(doc.getId());
                                    allOrders.add(order);
                                    String status = doc.getString("status");
                                    if (status != null) {
                                        previousStatuses.put(doc.getId(), status);
                                    }
                                }
                            }
                        }
                        isFirstLoad = false;
                        applyFilter();
                    }

                    @Override
                    public void onError(String message) {
                        if (!isAdded()) return;
                        progressBar.setVisibility(View.GONE);
                        if (message != null && message.contains("PERMISSION_DENIED")) return;
                        Toast.makeText(requireContext(),
                                "Failed to load orders: " + message, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void applyFilter() {
        filteredOrders.clear();
        if ("All".equals(currentFilter)) {
            filteredOrders.addAll(allOrders);
        } else {
            for (Order order : allOrders) {
                if (currentFilter.equalsIgnoreCase(order.getStatus())) {
                    filteredOrders.add(order);
                }
            }
        }
        adapter.notifyDataSetChanged();

        if (filteredOrders.isEmpty()) {
            showEmptyState();
        } else {
            layoutEmpty.setVisibility(View.GONE);
            rvOrders.setVisibility(View.VISIBLE);
        }
    }

    private void showEmptyState() {
        rvOrders.setVisibility(View.GONE);
        layoutEmpty.setVisibility(View.VISIBLE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (orderListener != null) {
            orderListener.remove();
            orderListener = null;
        }
    }
}
