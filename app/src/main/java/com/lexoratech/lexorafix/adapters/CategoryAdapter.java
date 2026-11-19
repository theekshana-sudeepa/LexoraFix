package com.lexoratech.lexorafix.adapters;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.lexoratech.lexorafix.R;
import com.lexoratech.lexorafix.models.Category;

import java.util.List;

public class CategoryAdapter extends RecyclerView.Adapter<CategoryAdapter.ViewHolder> {

    private final List<Category> categories;
    private final Context context;
    private final boolean useHomeLayout;
    private OnCategoryClickListener listener;

    // Vibrant colors for category icon backgrounds (brand-aligned palette)
    private static final int[] CATEGORY_COLORS = {
            0xFF2563EB, // Brand Blue
            0xFF14B8A6, // Brand Teal
            0xFF06B6D4, // Cyan
            0xFF1D4ED8, // Deep Blue
            0xFF0D9488, // Dark Teal
            0xFF3B82F6, // Light Blue
            0xFF0EA5E9, // Sky Blue
            0xFF10B981, // Emerald
    };

    public interface OnCategoryClickListener {
        void onCategoryClick(Category category);
    }

    public CategoryAdapter(Context context, List<Category> categories) {
        this(context, categories, false);
    }

    public CategoryAdapter(Context context, List<Category> categories, boolean useHomeLayout) {
        this.context = context;
        this.categories = categories;
        this.useHomeLayout = useHomeLayout;
    }

    public void setOnCategoryClickListener(OnCategoryClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layoutRes = useHomeLayout ? R.layout.item_category_home : R.layout.item_category;
        View view = LayoutInflater.from(context).inflate(layoutRes, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Category category = categories.get(position);
        holder.tvName.setText(category.getName());

        boolean hasCustomImage = category.getImageUrl() != null && !category.getImageUrl().isEmpty();
        if (hasCustomImage) {
            Glide.with(context)
                    .load(category.getImageUrl())
                    .placeholder(R.drawable.ic_category)
                    .error(R.drawable.ic_category)
                    .into(holder.ivIcon);
        } else {
            holder.ivIcon.setImageResource(R.drawable.ic_category);
        }

        // Apply vibrant color to the icon background
        if (holder.iconBg != null) {
            if (hasCustomImage) {
                // Keep the background fully transparent/empty for custom images
                holder.iconBg.setBackground(null);
                holder.ivIcon.clearColorFilter();
            } else {
                int color = CATEGORY_COLORS[position % CATEGORY_COLORS.length];
                GradientDrawable bg = new GradientDrawable();
                if (useHomeLayout) {
                    bg.setShape(GradientDrawable.OVAL);
                } else {
                    bg.setShape(GradientDrawable.RECTANGLE);
                    bg.setCornerRadius(context.getResources().getDisplayMetrics().density * 16);
                }
                bg.setColor(color);
                holder.iconBg.setBackground(bg);
                
                // Tint the regular vector icons white for contrast
                holder.ivIcon.setColorFilter(0xFFFFFFFF);
            }
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null)
                listener.onCategoryClick(category);
        });
    }

    @Override
    public int getItemCount() {
        return categories.size();
    }

    public void filterList(List<Category> filteredList) {
        categories.clear();
        categories.addAll(filteredList);
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivIcon;
        TextView tvName;
        View iconBg;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivIcon = itemView.findViewById(R.id.iv_icon);
            tvName = itemView.findViewById(R.id.tv_name);
            iconBg = itemView.findViewById(R.id.icon_bg);
        }
    }
}
