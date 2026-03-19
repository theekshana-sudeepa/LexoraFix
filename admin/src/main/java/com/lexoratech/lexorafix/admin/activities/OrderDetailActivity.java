package com.lexoratech.lexorafix.admin.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.lexoratech.lexorafix.admin.R;
import com.lexoratech.lexorafix.admin.adapters.OrderItemAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Shows full details of a single order: customer info, items list, status control.
 * Admin can change order status here, which syncs to both top-level and user subcollection.
 */
public class OrderDetailActivity extends AppCompatActivity {

    private String orderId;
    private String documentId;
    private String userId; // Firebase UID of the customer
    private String customerEmail; // Stored to send emails
    private Spinner spinnerStatus;
    private ProgressBar progressBar;

    private static final String[] STATUSES = {
            "Confirmed", "Processing", "Shipped", "Delivered", "Cancelled"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_detail);

        documentId = getIntent().getStringExtra("documentId");
        if (documentId == null || documentId.isEmpty()) {
            Toast.makeText(this, "Order not found", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        ImageView btnBack = findViewById(R.id.btn_back);
        TextView tvOrderId = findViewById(R.id.tv_order_id);
        TextView tvCustomerName = findViewById(R.id.tv_customer_name);
        TextView tvCustomerEmail = findViewById(R.id.tv_customer_email);
        TextView tvAddress = findViewById(R.id.tv_address);
        TextView tvTotal = findViewById(R.id.tv_total);
        TextView tvTimestamp = findViewById(R.id.tv_timestamp);
        RecyclerView rvItems = findViewById(R.id.rv_items);
        spinnerStatus = findViewById(R.id.spinner_status);
        MaterialButton btnUpdateStatus = findViewById(R.id.btn_update_status);
        progressBar = findViewById(R.id.progress_bar);

        btnBack.setOnClickListener(v -> finish());

        // Setup status spinner
        ArrayAdapter<String> statusAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, STATUSES);
        statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerStatus.setAdapter(statusAdapter);

        rvItems.setLayoutManager(new LinearLayoutManager(this));

        // Load order from Firestore
        progressBar.setVisibility(View.VISIBLE);
        FirebaseFirestore.getInstance().collection("orders").document(documentId)
                .get()
                .addOnSuccessListener(doc -> {
                    progressBar.setVisibility(View.GONE);
                    if (!doc.exists()) {
                        Toast.makeText(this, "Order not found", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }

                    orderId = doc.getString("orderId");
                    userId = doc.getString("firebaseUid");
                    String customerName = doc.getString("userName");
                    customerEmail = doc.getString("userEmail");
                    String address = doc.getString("address");
                    String status = doc.getString("status");
                    Object totalObj = doc.get("totalPrice");
                    double total = totalObj instanceof Number ? ((Number) totalObj).doubleValue() : 0;

                    // Handle timestamp
                    Object tsObj = doc.get("timestamp");
                    String timeStr = "";
                    if (tsObj instanceof com.google.firebase.Timestamp) {
                        timeStr = ((com.google.firebase.Timestamp) tsObj).toDate().toString();
                    }

                    tvOrderId.setText("Order #" + (orderId != null ? orderId : documentId));
                    tvCustomerName.setText(customerName != null ? customerName : "N/A");
                    tvCustomerEmail.setText(customerEmail != null ? customerEmail : "N/A");
                    tvAddress.setText(address != null ? address : "N/A");
                    tvTotal.setText(String.format(java.util.Locale.US, "LKR %.2f", total));
                    tvTimestamp.setText(timeStr);

                    // Set current status in spinner
                    if (status != null) {
                        for (int i = 0; i < STATUSES.length; i++) {
                            if (STATUSES[i].equalsIgnoreCase(status)) {
                                spinnerStatus.setSelection(i);
                                break;
                            }
                        }
                    }

                    // Load items
                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> items = (List<Map<String, Object>>) doc.get("items");
                    if (items != null && !items.isEmpty()) {
                        rvItems.setAdapter(new OrderItemAdapter(items));
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    finish();
                });

        // Update status button
        btnUpdateStatus.setOnClickListener(v -> updateOrderStatus());
    }

    private void updateOrderStatus() {
        String newStatus = spinnerStatus.getSelectedItem().toString();
        
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
                .setTitle("Confirm Status Update")
                .setMessage("Are you sure you want to update the order status to " + newStatus + "?")
                .setPositiveButton("Update", (dialog, which) -> {
                    progressBar.setVisibility(View.VISIBLE);

                    // Update in BOTH top-level orders and user subcollection
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("status", newStatus);
                    updates.put("statusUpdatedAt", com.google.firebase.Timestamp.now());

                    FirebaseFirestore db = FirebaseFirestore.getInstance();

                    // Update top-level
                    db.collection("orders").document(documentId)
                            .update(updates)
                            .addOnSuccessListener(aVoid -> {
                                // Also update user's subcollection so their listener picks it up
                                if (userId != null && !userId.isEmpty() && orderId != null) {
                                    
                                    // Send Order Status Update Email
                                    if (customerEmail != null && !customerEmail.isEmpty()) {
                                        com.lexoratech.lexorafix.admin.network.EmailHelper.getInstance()
                                                .sendOrderStatusUpdateEmail(customerEmail, orderId, newStatus);
                                    }

                                    db.collection("users").document(userId)
                                            .collection("orders").document(orderId)
                                            .update(updates)
                                            .addOnSuccessListener(aVoid2 -> {
                                                progressBar.setVisibility(View.GONE);
                                                Toast.makeText(this,
                                                        "Status updated to " + newStatus + "",
                                                        Toast.LENGTH_SHORT).show();
                                            })
                                            .addOnFailureListener(e -> {
                                                progressBar.setVisibility(View.GONE);
                                                Toast.makeText(this,
                                                        "Updated top-level but user sync failed",
                                                        Toast.LENGTH_SHORT).show();
                                            });
                                } else {
                                    progressBar.setVisibility(View.GONE);
                                    Toast.makeText(this,
                                            "Status updated to " + newStatus + "",
                                            Toast.LENGTH_SHORT).show();
                                }
                            })
                            .addOnFailureListener(e -> {
                                progressBar.setVisibility(View.GONE);
                                Toast.makeText(this, "Failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
