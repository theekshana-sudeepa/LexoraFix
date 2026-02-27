package com.lexoratech.lexorafix.admin.adapters;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.lexoratech.lexorafix.admin.R;
import com.lexoratech.lexorafix.admin.activities.ManageOrdersActivity;
import com.lexoratech.lexorafix.admin.activities.OrderDetailActivity;

import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AdminOrderAdapter extends RecyclerView.Adapter<AdminOrderAdapter.ViewHolder> {

    private final Context context;
    private final List<Map<String, Object>> orders;
    private static final String[] STATUSES = {
            "Confirmed", "Processing", "Shipped", "Delivered", "Cancelled"
    };

    public AdminOrderAdapter(Context context, List<Map<String, Object>> orders) {
        this.context = context;
        this.orders = orders;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_admin_order, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Map<String, Object> order = orders.get(position);
        String docId = getStr(order, "_documentId", "--");

        // Order ID
        holder.tvOrderId.setText("Order #" + getStr(order, "orderId", docId));

        // Status with color-coded badge
        String status = getStr(order, "status", "Unknown");
        holder.tvStatus.setText(status);
        setStatusBadgeColor(holder.tvStatus, status);

        // Customer info
        String customerName = getStr(order, "userName", "");
        holder.tvCustomerName.setText(customerName.isEmpty() ? "Unknown Customer" : customerName);

        String customerEmail = getStr(order, "userEmail", "");
        holder.tvCustomerEmail.setText(customerEmail.isEmpty() ? "No email" : customerEmail);
        holder.tvCustomerEmail.setVisibility(customerEmail.isEmpty() ? View.GONE : View.VISIBLE);

        // Payment status
        String paymentStatus = getStr(order, "paymentStatus", "");
        String paymentMethod = getStr(order, "paymentMethod", "");
        if (!paymentStatus.isEmpty()) {
            holder.tvPaymentStatus.setText("💳 " + paymentStatus);
            holder.tvPaymentStatus.setTextColor(
                    "Paid".equalsIgnoreCase(paymentStatus) ? 0xFF4CAF50 : 0xFFFF9800);
        } else {
            holder.tvPaymentStatus.setText("💳 " + (paymentMethod.isEmpty() ? "Pending" : paymentMethod));
        }

        // Total
        Object totalObj = order.get("totalPrice");
        double total = 0;
        if (totalObj instanceof Number) {
            total = ((Number) totalObj).doubleValue();
        }
        holder.tvTotal.setText(String.format(Locale.getDefault(), "LKR %.2f", total));

        // Item count
        Object itemCountObj = order.get("itemCount");
        int itemCount = 0;
        if (itemCountObj instanceof Number) {
            itemCount = ((Number) itemCountObj).intValue();
        }
        holder.tvItemCount.setText(itemCount + " item" + (itemCount != 1 ? "s" : ""));

        // Address
        String address = getStr(order, "address", "");
        holder.tvAddress.setText(address.isEmpty() ? "📍 No address" : "📍 " + address);

        // Navigate to OrderDetailActivity on click
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, OrderDetailActivity.class);
            intent.putExtra("documentId", docId);
            context.startActivity(intent);
        });

        // Long press for quick status update
        holder.itemView.setOnLongClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos != RecyclerView.NO_POSITION && context instanceof ManageOrdersActivity) {
                new AlertDialog.Builder(context)
                        .setTitle("Update Order Status")
                        .setItems(STATUSES, (dialog, which) ->
                                ((ManageOrdersActivity) context).updateOrderStatus(pos, STATUSES[which]))
                        .show();
            }
            return true;
        });
    }

    private void setStatusBadgeColor(TextView tv, String status) {
        int color;
        switch (status) {
            case "Delivered":
                color = 0xFF4CAF50;
                break;
            case "Cancelled":
                color = 0xFFF44336;
                break;
            case "Processing":
            case "Shipped":
                color = 0xFFFF9800;
                break;
            case "Confirmed":
                color = 0xFF2196F3;
                break;
            default:
                color = 0xFF6C63FF;
                break;
        }
        tv.setTextColor(0xFFFFFFFF);
        if (tv.getBackground() instanceof GradientDrawable) {
            ((GradientDrawable) tv.getBackground().mutate()).setColor(color);
        }
    }

    @Override
    public int getItemCount() {
        return orders.size();
    }

    private String getStr(Map<String, Object> map, String key, String def) {
        Object val = map.get(key);
        return val != null ? val.toString() : def;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvOrderId, tvStatus, tvCustomerName, tvCustomerEmail,
                tvPaymentStatus, tvTotal, tvItemCount, tvAddress;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvOrderId = itemView.findViewById(R.id.tv_order_id);
            tvStatus = itemView.findViewById(R.id.tv_status);
            tvCustomerName = itemView.findViewById(R.id.tv_customer_name);
            tvCustomerEmail = itemView.findViewById(R.id.tv_customer_email);
            tvPaymentStatus = itemView.findViewById(R.id.tv_payment_status);
            tvTotal = itemView.findViewById(R.id.tv_total);
            tvItemCount = itemView.findViewById(R.id.tv_item_count);
            tvAddress = itemView.findViewById(R.id.tv_address);
        }
    }
}
