package com.lexoratech.lexorafix.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.lexoratech.lexorafix.R;

import java.util.List;
import java.util.Locale;
import java.util.Map;

public class OrderProductAdapter extends RecyclerView.Adapter<OrderProductAdapter.ViewHolder> {

    private final Context context;
    private final List<Map<String, Object>> items;
    private final String orderStatus;
    private final OnReviewClickListener listener;

    public interface OnReviewClickListener {
        void onReviewClick(String productId, String productName, String productImage);
    }

    public OrderProductAdapter(Context context, List<Map<String, Object>> items, String orderStatus,
            OnReviewClickListener listener) {
        this.context = context;
        this.items = items;
        this.orderStatus = orderStatus;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_order_product, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Map<String, Object> item = items.get(position);

        String productId = (String) item.get("productId");
        String name = (String) item.get("productName");
        String imageUrl = (String) item.get("imageUrl");

        holder.tvName.setText(name != null ? name : "Unknown Product");

        Object priceObj = item.get("price");
        Object quantityObj = item.get("quantity");

        double price = 0;
        int qty = 1;

        if (priceObj instanceof Number)
            price = ((Number) priceObj).doubleValue();
        if (quantityObj instanceof Number)
            qty = ((Number) quantityObj).intValue();

        holder.tvPrice.setText(String.format(Locale.getDefault(), "LKR %.2f", price));
        holder.tvQty.setText("x " + qty);

        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(context).load(imageUrl).into(holder.ivProduct);
        } else {
            holder.ivProduct.setImageDrawable(null);
            holder.ivProduct.setBackgroundColor(android.graphics.Color.parseColor("#E0E0E0"));
        }

        if ("Delivered".equalsIgnoreCase(orderStatus)) {
            holder.btnReview.setVisibility(View.VISIBLE);
            holder.btnReview.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onReviewClick(productId, name, imageUrl);
                }
            });
        } else {
            holder.btnReview.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivProduct;
        TextView tvName, tvPrice, tvQty;
        MaterialButton btnReview;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivProduct = itemView.findViewById(R.id.iv_product);
            tvName = itemView.findViewById(R.id.tv_name);
            tvPrice = itemView.findViewById(R.id.tv_price);
            tvQty = itemView.findViewById(R.id.tv_qty);
            btnReview = itemView.findViewById(R.id.btn_review);
        }
    }
}
