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
import com.lexoratech.lexorafix.R;
import com.lexoratech.lexorafix.models.CartItem;

import java.util.List;
import java.util.Locale;

public class CartAdapter extends RecyclerView.Adapter<CartAdapter.ViewHolder> {

    private final List<CartItem> cartItems;
    private final Context context;
    private OnCartActionListener listener;
    private java.util.Map<String, Integer> stockMap = new java.util.HashMap<>();

    public interface OnCartActionListener {
        void onQuantityChanged(CartItem item, int newQuantity);

        void onRemoveItem(CartItem item);
    }

    public CartAdapter(Context context, List<CartItem> cartItems) {
        this.context = context;
        this.cartItems = cartItems;
    }

    public void setOnCartActionListener(OnCartActionListener listener) {
        this.listener = listener;
    }

    public void setStockMap(java.util.Map<String, Integer> stockMap) {
        this.stockMap = stockMap;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_cart, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CartItem item = cartItems.get(position);
        holder.tvName.setText(item.getProductName());
        holder.tvPrice.setText(String.format(Locale.getDefault(), "LKR %.2f", item.getPrice()));
        holder.tvQuantity.setText(String.valueOf(item.getQuantity()));
        holder.tvSubtotal.setText(String.format(Locale.getDefault(), "LKR %.2f", item.getSubtotal()));

        String imageUrl = item.getImageUrl();
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(context)
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_store)
                    .error(R.drawable.ic_store)
                    .centerCrop()
                    .into(holder.ivProduct);
        } else {
            holder.ivProduct.setImageResource(R.drawable.ic_store);
            holder.ivProduct.setScaleType(ImageView.ScaleType.CENTER);
        }

        // Show variant label (e.g. "12V / 35Ah") when present
        String variantLabel = item.getVariantLabel();
        if (variantLabel != null && !variantLabel.trim().isEmpty()) {
            holder.tvVariantLabel.setVisibility(View.VISIBLE);
            holder.tvVariantLabel.setText(variantLabel);
        } else {
            holder.tvVariantLabel.setVisibility(View.GONE);
        }

        Integer maxStock = stockMap.get(item.getProductName());
        if (maxStock != null && item.getQuantity() >= maxStock) {
            holder.btnIncrease.setEnabled(false);
            holder.btnIncrease.setAlpha(0.3f);
        } else {
            holder.btnIncrease.setEnabled(true);
            holder.btnIncrease.setAlpha(1.0f);
        }

        holder.btnIncrease.setOnClickListener(v -> {
            if (listener != null)
                listener.onQuantityChanged(item, item.getQuantity() + 1);
        });

        holder.btnDecrease.setOnClickListener(v -> {
            if (listener != null && item.getQuantity() > 1) {
                listener.onQuantityChanged(item, item.getQuantity() - 1);
            }
        });

        holder.btnRemove.setOnClickListener(v -> {
            if (listener != null)
                listener.onRemoveItem(item);
        });

        holder.itemView.setOnClickListener(v -> {
            android.content.Intent intent = new android.content.Intent(context,
                    com.lexoratech.lexorafix.activities.ProductDetailActivity.class);
            if (item.getFirestoreDocId() != null && !item.getFirestoreDocId().isEmpty()) {
                intent.putExtra("document_id", item.getFirestoreDocId());
            } else {
                intent.putExtra("product_id", item.getProductId());
            }
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return cartItems.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivProduct, btnIncrease, btnDecrease, btnRemove;
        TextView tvName, tvPrice, tvVariantLabel, tvQuantity, tvSubtotal;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivProduct = itemView.findViewById(R.id.iv_product);
            tvName = itemView.findViewById(R.id.tv_name);
            tvPrice = itemView.findViewById(R.id.tv_price);
            tvVariantLabel = itemView.findViewById(R.id.tv_variant_label);
            tvQuantity = itemView.findViewById(R.id.tv_quantity);
            tvSubtotal = itemView.findViewById(R.id.tv_subtotal);
            btnIncrease = itemView.findViewById(R.id.btn_increase);
            btnDecrease = itemView.findViewById(R.id.btn_decrease);
            btnRemove = itemView.findViewById(R.id.btn_remove);
        }
    }
}
