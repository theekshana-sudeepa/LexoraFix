package com.lexoratech.lexorafix.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.lexoratech.lexorafix.R;

/**
 * Shows order tracking progress with step indicators.
 * Listens to Firestore for real-time status updates.
 */
public class OrderTrackingActivity extends AppCompatActivity {

    private ListenerRegistration listener;
    private View lineConfirmedProcessing, lineProcessingShipped, lineShippedDelivered;
    private ImageView iconConfirmed, iconProcessing, iconShipped, iconDelivered;
    private TextView tvTrackingNumber, tvEstimatedDelivery, tvStatus, tvOrderId;
    private String previousStatus = null;
    private String currentOrderId = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_order_tracking);

        String documentId = getIntent().getStringExtra("documentId");
        String userId = getIntent().getStringExtra("userId");
        String orderId = getIntent().getStringExtra("orderId");
        currentOrderId = orderId;

        // Toolbar
        ImageView btnBack = findViewById(R.id.btn_back);
        TextView tvToolbarTitle = findViewById(R.id.tv_toolbar_title);
        if (tvToolbarTitle != null)
            tvToolbarTitle.setText("Track Order");
        btnBack.setOnClickListener(v -> finish());

        // Status text
        tvStatus = findViewById(R.id.tv_status);
        tvOrderId = findViewById(R.id.tv_order_id);
        tvTrackingNumber = findViewById(R.id.tv_tracking_number);
        tvEstimatedDelivery = findViewById(R.id.tv_estimated_delivery);

        // Step indicators
        iconConfirmed = findViewById(R.id.icon_confirmed);
        iconProcessing = findViewById(R.id.icon_processing);
        iconShipped = findViewById(R.id.icon_shipped);
        iconDelivered = findViewById(R.id.icon_delivered);

        // Progress lines
        lineConfirmedProcessing = findViewById(R.id.line_confirmed_processing);
        lineProcessingShipped = findViewById(R.id.line_processing_shipped);
        lineShippedDelivered = findViewById(R.id.line_shipped_delivered);

        if (orderId != null) {
            tvOrderId.setText("Order #" + orderId);
        }

        // Listen for real-time updates
        if (documentId != null) {
            listener = FirebaseFirestore.getInstance()
                    .collection("orders").document(documentId)
                    .addSnapshotListener((doc, error) -> {
                        if (error != null) {
                            if (error.getMessage() != null && !error.getMessage().contains("PERMISSION_DENIED")) {
                                // Ignore permission denied on logout
                            }
                            return;
                        }
                        if (doc == null || !doc.exists())
                            return;
                        updateUI(doc);
                    });
        } else if (userId != null && orderId != null) {
            listener = FirebaseFirestore.getInstance()
                    .collection("users").document(userId)
                    .collection("orders").document(orderId)
                    .addSnapshotListener((doc, error) -> {
                        if (error != null) {
                            if (error.getMessage() != null && !error.getMessage().contains("PERMISSION_DENIED")) {
                                // Ignore permission denied on logout
                            }
                            return;
                        }
                        if (doc == null || !doc.exists())
                            return;
                        updateUI(doc);
                    });
        }
    }

    private void updateUI(DocumentSnapshot doc) {
        String status = doc.getString("status");
        String trackingNumber = doc.getString("trackingNumber");
        String estimatedDelivery = doc.getString("estimatedDelivery");

        if (status != null) {
            tvStatus.setText(status);
            updateStatusBadgeColor(status);
            updateProgress(status);

            // Show notification if status changed while viewing
            if (previousStatus != null && !status.equals(previousStatus)) {
                com.lexoratech.lexorafix.notifications.NotificationHelper helper = new com.lexoratech.lexorafix.notifications.NotificationHelper(
                        this);
                helper.showOrderStatusUpdate(
                        currentOrderId != null ? currentOrderId : doc.getId(), status);
            }
            previousStatus = status;
        }

        View cardTracking = findViewById(R.id.card_tracking_details);

        if (trackingNumber != null && !trackingNumber.isEmpty()) {
            tvTrackingNumber.setText(trackingNumber);
            tvTrackingNumber.setVisibility(View.VISIBLE);
            findViewById(R.id.label_tracking).setVisibility(View.VISIBLE);
            if (cardTracking != null)
                cardTracking.setVisibility(View.VISIBLE);
        }
        if (estimatedDelivery != null && !estimatedDelivery.isEmpty()) {
            tvEstimatedDelivery.setText(estimatedDelivery);
            tvEstimatedDelivery.setVisibility(View.VISIBLE);
            findViewById(R.id.label_delivery).setVisibility(View.VISIBLE);
            if (cardTracking != null)
                cardTracking.setVisibility(View.VISIBLE);
        }

        // Extract order items
        Object itemsObj = doc.get("items");
        View cardOrderItems = findViewById(R.id.card_order_items);
        androidx.recyclerview.widget.RecyclerView rvOrderItems = findViewById(R.id.rv_order_items);
        
        // Extract pricing
        Object totalObj = doc.get("totalPrice");
        double total = totalObj instanceof Number ? ((Number) totalObj).doubleValue() : 0;
        
        Object subObj = doc.get("subtotal");
        double subtotal = subObj instanceof Number ? ((Number) subObj).doubleValue() : total;
        
        Object feeObj = doc.get("deliveryFee");
        double deliveryFee = feeObj instanceof Number ? ((Number) feeObj).doubleValue() : 0;
        
        TextView tvSubtotal = findViewById(R.id.tv_subtotal);
        TextView tvDeliveryFee = findViewById(R.id.tv_delivery_fee);
        TextView tvTotal = findViewById(R.id.tv_total);
        
        if (tvSubtotal != null) tvSubtotal.setText(String.format(java.util.Locale.US, "LKR %.2f", subtotal));
        if (tvDeliveryFee != null) {
            if (deliveryFee > 0) {
                tvDeliveryFee.setText(String.format(java.util.Locale.US, "LKR %.2f", deliveryFee));
            } else {
                tvDeliveryFee.setText("FREE");
                tvDeliveryFee.setTextColor(ContextCompat.getColor(this, R.color.colorSuccess));
            }
        }
        if (tvTotal != null) tvTotal.setText(String.format(java.util.Locale.US, "LKR %.2f", total));

        if (itemsObj instanceof java.util.List) {
            java.util.List<java.util.Map<String, Object>> items = (java.util.List<java.util.Map<String, Object>>) itemsObj;
            if (!items.isEmpty()) {
                if (cardOrderItems != null)
                    cardOrderItems.setVisibility(View.VISIBLE);
                rvOrderItems.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this));

                com.lexoratech.lexorafix.adapters.OrderProductAdapter adapter = new com.lexoratech.lexorafix.adapters.OrderProductAdapter(
                        this, items, status, (productId, productName, productImage) -> {
                            // Show review bottom sheet
                            com.lexoratech.lexorafix.fragments.ReviewBottomSheetFragment bottomSheet = com.lexoratech.lexorafix.fragments.ReviewBottomSheetFragment
                                    .newInstance(productId, productName);
                            bottomSheet.show(getSupportFragmentManager(), "ReviewBottomSheet");
                        });
                rvOrderItems.setAdapter(adapter);
            } else {
                if (cardOrderItems != null)
                    cardOrderItems.setVisibility(View.GONE);
            }
        } else {
            if (cardOrderItems != null)
                cardOrderItems.setVisibility(View.GONE);
        }
    }

    private void updateStatusBadgeColor(String status) {
        int colorRes;
        switch (status.toLowerCase(java.util.Locale.ROOT)) {
            case "delivered":
                colorRes = R.color.colorSuccess;
                break;
            case "shipped":
                colorRes = R.color.colorPrimary;
                break;
            case "processing":
                colorRes = R.color.colorSecondary;
                break;
            case "cancelled":
                colorRes = R.color.colorError;
                break;
            case "confirmed":
            default:
                colorRes = R.color.colorPrimary;
                break;
        }
        android.graphics.drawable.GradientDrawable bg = (android.graphics.drawable.GradientDrawable) tvStatus
                .getBackground();
        if (bg != null) {
            bg.setColor(ContextCompat.getColor(this, colorRes));
        }
    }

    private void updateProgress(String status) {
        int active = ContextCompat.getColor(this, R.color.colorPrimary);
        int inactive = ContextCompat.getColor(this, R.color.textHint);
        int delivered = ContextCompat.getColor(this, R.color.colorSuccess);

        // Reset
        setStepColor(iconConfirmed, inactive);
        setStepColor(iconProcessing, inactive);
        setStepColor(iconShipped, inactive);
        setStepColor(iconDelivered, inactive);
        lineConfirmedProcessing.setBackgroundColor(inactive);
        lineProcessingShipped.setBackgroundColor(inactive);
        lineShippedDelivered.setBackgroundColor(inactive);

        switch (status.toLowerCase(java.util.Locale.ROOT)) {
            case "delivered":
                setStepColor(iconDelivered, delivered);
                lineShippedDelivered.setBackgroundColor(delivered);
                // fall through
            case "shipped":
                setStepColor(iconShipped, active);
                lineProcessingShipped.setBackgroundColor(active);
                // fall through
            case "processing":
                setStepColor(iconProcessing, active);
                lineConfirmedProcessing.setBackgroundColor(active);
                // fall through
            case "confirmed":
            default:
                setStepColor(iconConfirmed, active);
                break;
        }
    }

    private void setStepColor(ImageView icon, int color) {
        icon.setColorFilter(color);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (listener != null)
            listener.remove();
    }
}
