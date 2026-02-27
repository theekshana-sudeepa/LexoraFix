package com.lexoratech.lexorafix.admin.adapters;

import android.content.Context;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.lexoratech.lexorafix.admin.R;

import java.util.Date;
import java.util.List;
import java.util.Map;

public class AdminReviewAdapter extends RecyclerView.Adapter<AdminReviewAdapter.ReviewViewHolder> {

    private final Context context;
    private final List<Map<String, Object>> reviews;
    private OnReviewActionListener listener;

    public interface OnReviewActionListener {
        void onDeleteClick(String reviewDocId, int position);
    }

    public AdminReviewAdapter(Context context, List<Map<String, Object>> reviews) {
        this.context = context;
        this.reviews = reviews;
    }

    public void setOnReviewActionListener(OnReviewActionListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ReviewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_admin_review, parent, false);
        return new ReviewViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReviewViewHolder holder, int position) {
        Map<String, Object> review = reviews.get(position);

        holder.tvUserName.setText(getStr(review, "userName", "Unknown User"));
        holder.tvReviewText.setText(getStr(review, "reviewText", ""));

        Object ratingObj = review.get("rating");
        float rating = ratingObj instanceof Number ? ((Number) ratingObj).floatValue() : 0f;
        holder.ratingBar.setRating(rating);

        Object timestampObj = review.get("timestamp");
        if (timestampObj instanceof com.google.firebase.Timestamp) {
            Date date = ((com.google.firebase.Timestamp) timestampObj).toDate();
            holder.tvDate.setText(DateFormat.format("MMM dd, yyyy - hh:mm a", date));
        } else if (timestampObj instanceof Long) {
            Date date = new Date((Long) timestampObj);
            holder.tvDate.setText(DateFormat.format("MMM dd, yyyy - hh:mm a", date));
        } else {
            holder.tvDate.setText("Unknown Date");
        }

        String docId = getStr(review, "_documentId", "");
        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null)
                listener.onDeleteClick(docId, position);
        });
    }

    @Override
    public int getItemCount() {
        return reviews.size();
    }

    private String getStr(Map<String, Object> map, String key, String def) {
        Object val = map.get(key);
        return val != null ? val.toString() : def;
    }

    static class ReviewViewHolder extends RecyclerView.ViewHolder {
        TextView tvUserName, tvReviewText, tvDate;
        RatingBar ratingBar;
        View btnDelete;

        public ReviewViewHolder(@NonNull View itemView) {
            super(itemView);
            tvUserName = itemView.findViewById(R.id.tv_user_name);
            tvReviewText = itemView.findViewById(R.id.tv_review_text);
            tvDate = itemView.findViewById(R.id.tv_date);
            ratingBar = itemView.findViewById(R.id.rating_bar);
            btnDelete = itemView.findViewById(R.id.btn_delete_review);
        }
    }
}
