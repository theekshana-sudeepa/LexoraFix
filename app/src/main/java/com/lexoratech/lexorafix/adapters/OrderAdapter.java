package com.lexoratech.lexorafix.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.lexoratech.lexorafix.R;
import com.lexoratech.lexorafix.activities.OrderTrackingActivity;
import com.lexoratech.lexorafix.models.Order;

import java.util.List;
import java.util.Locale;

public class OrderAdapter extends RecyclerView.Adapter<OrderAdapter.ViewHolder> {

    private final List<Order> orders;
    private final Context context;

    public OrderAdapter(Context context, List<Order> orders) {
        this.context = context;
        this.orders = orders;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_order, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Order order = orders.get(position);

        holder.tvOrderId.setText("Order #" + order.getOrderId());
        holder.tvDate.setText(order.getTimestamp() != null ? order.getTimestamp() : "");
        holder.tvItems.setText(order.getItemCount() + " items");
        holder.tvTotal.setText(String.format(Locale.getDefault(), "LKR %.2f", order.getTotalPrice()));
        holder.tvStatus.setText(order.getStatus());

        if ("Confirmed".equalsIgnoreCase(order.getStatus()) || "Delivered".equalsIgnoreCase(order.getStatus())) {
            holder.tvStatus.setTextColor(context.getResources().getColor(R.color.colorSuccess, null));
        } else if ("Cancelled".equalsIgnoreCase(order.getStatus())) {
            holder.tvStatus.setTextColor(context.getResources().getColor(R.color.colorError, null));
        } else {
            holder.tvStatus.setTextColor(context.getResources().getColor(R.color.colorWarning, null));
        }

        // Track Order button — launches OrderTrackingActivity with this order's data
        holder.btnTrack.setOnClickListener(v -> {
            Intent intent = new Intent(context, OrderTrackingActivity.class);
            intent.putExtra("documentId", order.getDocumentId());
            intent.putExtra("orderId", order.getOrderId());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return orders.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvOrderId, tvDate, tvItems, tvTotal, tvStatus;
        MaterialButton btnTrack;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvOrderId = itemView.findViewById(R.id.tv_order_id);
            tvDate = itemView.findViewById(R.id.tv_date);
            tvItems = itemView.findViewById(R.id.tv_items);
            tvTotal = itemView.findViewById(R.id.tv_total);
            tvStatus = itemView.findViewById(R.id.tv_status);
            btnTrack = itemView.findViewById(R.id.btn_track);
        }
    }
}
