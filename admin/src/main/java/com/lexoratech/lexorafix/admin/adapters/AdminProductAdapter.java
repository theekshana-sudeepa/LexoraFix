package com.lexoratech.lexorafix.admin.adapters;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
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
import java.util.Locale;
import java.util.Map;

public class AdminProductAdapter extends RecyclerView.Adapter<AdminProductAdapter.ViewHolder> {

    private final Context context;
    private final List<Map<String, Object>> products;
    private OnProductActionListener listener;

    public interface OnProductActionListener {
        void onEdit(String documentId, Map<String, Object> product);

        void onDelete(String documentId, String productName, String imageUrl);

        void onToggleStatus(String documentId, boolean currentlyActive);

        void onViewReviews(String documentId, String productName);
    }

    public AdminProductAdapter(Context context, List<Map<String, Object>> products) {
        this.context = context;
        this.products = products;
    }

    public void setOnProductActionListener(OnProductActionListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_admin_product, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Map<String, Object> product = products.get(position);
        String docId = getStr(product, "_documentId", "");

        holder.tvName.setText(getStr(product, "name", "Unnamed"));
        Object priceObj = product.get("price");
        double price = priceObj instanceof Number ? ((Number) priceObj).doubleValue() : 0;
        holder.tvPrice.setText(String.format(Locale.getDefault(), "LKR %.2f", price));

        Object stockObj = product.get("stock");
        int stock = stockObj instanceof Number ? ((Number) stockObj).intValue() : 0;
        holder.tvStock.setText("Stock: " + stock);

        Object featObj = product.get("featured");
        boolean featured = featObj instanceof Boolean && (Boolean) featObj;
        holder.tvFeatured.setVisibility(featured ? View.VISIBLE : View.GONE);

        // Status badge
        Object statusObj = product.get("status");
        String status = statusObj != null ? statusObj.toString() : "active";
        boolean isActive = "active".equalsIgnoreCase(status);
        holder.tvStatus.setText(isActive ? "Active" : "Inactive");
        GradientDrawable statusBg = new GradientDrawable();
        statusBg.setCornerRadius(8f);
        statusBg.setColor(isActive ? Color.parseColor("#10B981") : Color.parseColor("#EF4444"));
        holder.tvStatus.setBackground(statusBg);

        // Tap status badge to toggle
        holder.tvStatus.setOnClickListener(v -> {
            if (listener != null)
                listener.onToggleStatus(docId, isActive);
        });

        // Load image with Glide
        String imageUrl = getStr(product, "imageUrl", "");
        if (!imageUrl.isEmpty()) {
            Glide.with(context)
                    .load(imageUrl)
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_menu_gallery)
                    .centerCrop()
                    .into(holder.ivProduct);
        } else {
            holder.ivProduct.setImageResource(android.R.drawable.ic_menu_gallery);
        }

        holder.btnEdit.setOnClickListener(v -> {
            if (listener != null)
                listener.onEdit(docId, product);
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null)
                listener.onDelete(docId, getStr(product, "name", ""), imageUrl);
        });

        holder.btnReviews.setOnClickListener(v -> {
            if (listener != null)
                listener.onViewReviews(docId, getStr(product, "name", "Unnamed"));
        });
    }

    @Override
    public int getItemCount() {
        return products.size();
    }

    private String getStr(Map<String, Object> map, String key, String def) {
        Object val = map.get(key);
        return val != null ? val.toString() : def;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivProduct;
        View btnEdit, btnDelete, btnReviews;
        TextView tvName, tvPrice, tvStock, tvFeatured, tvStatus;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivProduct = itemView.findViewById(R.id.iv_product);
            tvName = itemView.findViewById(R.id.tv_name);
            tvPrice = itemView.findViewById(R.id.tv_price);
            tvStock = itemView.findViewById(R.id.tv_stock);
            tvFeatured = itemView.findViewById(R.id.tv_featured);
            tvStatus = itemView.findViewById(R.id.tv_status);
            btnEdit = itemView.findViewById(R.id.btn_edit);
            btnDelete = itemView.findViewById(R.id.btn_delete);
            btnReviews = itemView.findViewById(R.id.btn_reviews);
        }
    }
}
