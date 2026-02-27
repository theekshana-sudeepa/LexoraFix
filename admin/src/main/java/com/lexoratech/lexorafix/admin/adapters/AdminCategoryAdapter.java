package com.lexoratech.lexorafix.admin.adapters;

import android.content.Context;
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

public class AdminCategoryAdapter extends RecyclerView.Adapter<AdminCategoryAdapter.ViewHolder> {

    private final Context context;
    private final List<Map<String, Object>> categories;
    private OnCategoryActionListener listener;

    public interface OnCategoryActionListener {
        void onEdit(String documentId, Map<String, Object> category);

        void onDelete(String documentId, String categoryName, String imageUrl);
    }

    public AdminCategoryAdapter(Context context, List<Map<String, Object>> categories) {
        this.context = context;
        this.categories = categories;
    }

    public void setOnCategoryActionListener(OnCategoryActionListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_admin_category, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Map<String, Object> category = categories.get(position);
        String docId = getStr(category, "_documentId", "");

        holder.tvName.setText(getStr(category, "name", "Unnamed"));
        holder.tvDescription.setText(getStr(category, "description", ""));

        // Load image with Glide
        String imageUrl = getStr(category, "imageUrl", "");
        if (!imageUrl.isEmpty()) {
            Glide.with(context)
                    .load(imageUrl)
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .error(android.R.drawable.ic_menu_gallery)
                    .centerCrop()
                    .into(holder.ivCategory);
        } else {
            holder.ivCategory.setImageResource(android.R.drawable.ic_menu_gallery);
        }

        holder.btnEdit.setOnClickListener(v -> {
            if (listener != null)
                listener.onEdit(docId, category);
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null)
                listener.onDelete(docId, getStr(category, "name", ""), imageUrl);
        });
    }

    @Override
    public int getItemCount() {
        return categories.size();
    }

    private String getStr(Map<String, Object> map, String key, String def) {
        Object val = map.get(key);
        return val != null ? val.toString() : def;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivCategory;
        View btnEdit, btnDelete;
        TextView tvName, tvDescription;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivCategory = itemView.findViewById(R.id.iv_category);
            tvName = itemView.findViewById(R.id.tv_name);
            tvDescription = itemView.findViewById(R.id.tv_description);
            btnEdit = itemView.findViewById(R.id.btn_edit);
            btnDelete = itemView.findViewById(R.id.btn_delete);
        }
    }
}
