package com.lexoratech.lexorafix.adapters;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.lexoratech.lexorafix.R;
import com.lexoratech.lexorafix.models.Category;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for ViewPager2-based category carousel.
 * Each page shows up to 4 categories arranged in 2 rows × 2 columns.
 */
public class CategoryCarouselAdapter extends RecyclerView.Adapter<CategoryCarouselAdapter.PageViewHolder> {

    private static final int ITEMS_PER_PAGE = 4;

    // Vibrant colors for category icon backgrounds (same palette as CategoryAdapter)
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

    private final Context context;
    private final List<List<Category>> pages;
    private OnCategoryClickListener listener;

    public interface OnCategoryClickListener {
        void onCategoryClick(Category category);
    }

    public CategoryCarouselAdapter(Context context, List<Category> categories) {
        this.context = context;
        this.pages = new ArrayList<>();

        // Split categories into pages of ITEMS_PER_PAGE
        for (int i = 0; i < categories.size(); i += ITEMS_PER_PAGE) {
            int end = Math.min(i + ITEMS_PER_PAGE, categories.size());
            pages.add(new ArrayList<>(categories.subList(i, end)));
        }
    }

    public void setOnCategoryClickListener(OnCategoryClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public PageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_category_carousel_page, parent, false);
        return new PageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PageViewHolder holder, int position) {
        List<Category> pageCategories = pages.get(position);
        int globalOffset = position * ITEMS_PER_PAGE;

        holder.row1.removeAllViews();
        holder.row2.removeAllViews();

        for (int i = 0; i < pageCategories.size(); i++) {
            Category category = pageCategories.get(i);
            View itemView = LayoutInflater.from(context).inflate(R.layout.item_category_home, null);

            // Set layout params: each item takes half the row width
            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
            params.setMargins(4, 4, 4, 4);
            itemView.setLayoutParams(params);

            // Bind data
            TextView tvName = itemView.findViewById(R.id.tv_name);
            ImageView ivIcon = itemView.findViewById(R.id.iv_icon);
            View iconBg = itemView.findViewById(R.id.icon_bg);

            tvName.setText(category.getName());

            boolean hasCustomImage = category.getImageUrl() != null && !category.getImageUrl().isEmpty();
            if (hasCustomImage) {
                Glide.with(context)
                        .load(category.getImageUrl())
                        .placeholder(R.drawable.ic_category)
                        .error(R.drawable.ic_category)
                        .into(ivIcon);
            } else {
                ivIcon.setImageResource(R.drawable.ic_category);
            }

            // Apply color to icon background
            if (iconBg != null) {
                if (hasCustomImage) {
                    // Remove the background circle so the uploaded photo is completely clear
                    iconBg.setBackground(null);
                    ivIcon.clearColorFilter();
                } else {
                    int colorIdx = (globalOffset + i) % CATEGORY_COLORS.length;
                    GradientDrawable bg = new GradientDrawable();
                    bg.setShape(GradientDrawable.OVAL);
                    bg.setColor(CATEGORY_COLORS[colorIdx]);
                    iconBg.setBackground(bg);
                    ivIcon.setColorFilter(0xFFFFFFFF);
                }
            }

            // Click listener
            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onCategoryClick(category);
            });

            // Add to appropriate row
            if (i < 2) {
                holder.row1.addView(itemView);
            } else {
                holder.row2.addView(itemView);
            }
        }

        // If row1 has only 1 item, add a spacer for balance
        if (holder.row1.getChildCount() == 1) {
            addSpacer(holder.row1);
        }
        // If row2 has only 1 item, add a spacer
        if (holder.row2.getChildCount() == 1) {
            addSpacer(holder.row2);
        }
    }

    private void addSpacer(LinearLayout row) {
        View spacer = new View(context);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        params.setMargins(4, 4, 4, 4);
        spacer.setLayoutParams(params);
        row.addView(spacer);
    }

    @Override
    public int getItemCount() {
        return pages.size();
    }

    public int getPageCount() {
        return pages.size();
    }

    static class PageViewHolder extends RecyclerView.ViewHolder {
        LinearLayout row1, row2;

        PageViewHolder(@NonNull View itemView) {
            super(itemView);
            row1 = itemView.findViewById(R.id.row1);
            row2 = itemView.findViewById(R.id.row2);
        }
    }
}
