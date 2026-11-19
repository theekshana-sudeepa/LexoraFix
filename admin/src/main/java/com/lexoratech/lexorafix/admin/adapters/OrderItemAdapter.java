package com.lexoratech.lexorafix.admin.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.lexoratech.lexorafix.admin.R;

import java.util.List;
import java.util.Map;

/**
 * Adapter for displaying order line items in OrderDetailActivity.
 */
public class OrderItemAdapter extends RecyclerView.Adapter<OrderItemAdapter.ViewHolder> {

    private final List<Map<String, Object>> items;

    public OrderItemAdapter(List<Map<String, Object>> items) {
        this.items = items;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_order_detail_product, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Map<String, Object> item = items.get(position);

        String productName = (String) item.get("productName");
        Object priceObj = item.get("price");
        Object qtyObj = item.get("quantity");
        String imageUrl = (String) item.get("imageUrl");

        double price = priceObj instanceof Number ? ((Number) priceObj).doubleValue() : 0;
        int qty = qtyObj instanceof Number ? ((Number) qtyObj).intValue() : 0;

        holder.tvName.setText(productName != null ? productName : "Product");
        holder.tvPrice.setText(String.format(java.util.Locale.US, "LKR %.2f", price));
        holder.tvQuantity.setText("Qty: " + qty);
        holder.tvSubtotal.setText(String.format(java.util.Locale.US, "LKR %.2f", price * qty));

        // Show variant label (e.g. "12V / 35Ah") when present
        Object varObj = item.get("variantLabel");
        String varLabel = varObj instanceof String ? (String) varObj : null;
        if (varLabel != null && !varLabel.trim().isEmpty()) {
            holder.tvVariantLabel.setVisibility(android.view.View.VISIBLE);
            holder.tvVariantLabel.setText(varLabel);
        } else {
            holder.tvVariantLabel.setVisibility(android.view.View.GONE);
        }

        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(imageUrl)
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .into(holder.ivProduct);
        }
    }

    @Override
    public int getItemCount() {
        return items != null ? items.size() : 0;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivProduct;
        TextView tvName, tvVariantLabel, tvPrice, tvQuantity, tvSubtotal;

        ViewHolder(View view) {
            super(view);
            ivProduct = view.findViewById(R.id.iv_product);
            tvName = view.findViewById(R.id.tv_product_name);
            tvVariantLabel = view.findViewById(R.id.tv_variant_label);
            tvPrice = view.findViewById(R.id.tv_price);
            tvQuantity = view.findViewById(R.id.tv_quantity);
            tvSubtotal = view.findViewById(R.id.tv_subtotal);
        }
    }
}
