package com.lexoratech.lexorafix.adapters;

import android.content.Context;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.lexoratech.lexorafix.R;
import com.lexoratech.lexorafix.models.Review;

import java.util.List;

public class ReviewAdapter extends RecyclerView.Adapter<ReviewAdapter.ViewHolder> {

    private final Context context;
    private final List<Review> reviews;
    private OnReviewActionListener actionListener;

    public interface OnReviewActionListener {
        void onEditReview(Review review);
        void onDeleteReview(Review review);
    }

    public ReviewAdapter(Context context, List<Review> reviews) {
        this.context = context;
        this.reviews = reviews;
    }

    public void setOnReviewActionListener(OnReviewActionListener listener) {
        this.actionListener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_review, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Review review = reviews.get(position);
        holder.tvName.setText(review.getUserName() != null ? review.getUserName() : "Anonymous");
        holder.ratingBar.setRating(review.getRating());
        
        if (holder.tvRatingText != null) {
            holder.tvRatingText.setText(String.valueOf(review.getRating()));
        }

        if (review.getComment() != null && !review.getComment().trim().isEmpty()) {
            holder.tvComment.setVisibility(View.VISIBLE);
            holder.tvComment.setText(review.getComment());
        } else {
            holder.tvComment.setVisibility(View.GONE);
        }

        if (review.getTimestamp() != null) {
            String dateString = DateFormat.format("MMM d, yyyy", review.getTimestamp().toDate()).toString();
            holder.tvDate.setText(dateString);
            holder.tvDate.setVisibility(View.VISIBLE);
        } else {
            holder.tvDate.setVisibility(View.GONE);
        }

        // Show edit/delete only for the current user's own review
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null && currentUser.getUid().equals(review.getUserId())) {
            holder.llActions.setVisibility(View.VISIBLE);
            holder.btnEdit.setOnClickListener(v -> {
                if (actionListener != null) actionListener.onEditReview(review);
            });
            holder.btnDelete.setOnClickListener(v -> {
                if (actionListener != null) actionListener.onDeleteReview(review);
            });
        } else {
            holder.llActions.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return reviews.size();
    }

    public void updateList(List<Review> newReviews) {
        reviews.clear();
        reviews.addAll(newReviews);
        notifyDataSetChanged();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvDate, tvComment, tvRatingText;
        TextView btnEdit, btnDelete;
        LinearLayout llActions;
        RatingBar ratingBar;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_reviewer_name);
            tvDate = itemView.findViewById(R.id.tv_date);
            tvComment = itemView.findViewById(R.id.tv_comment);
            tvRatingText = itemView.findViewById(R.id.tv_rating_text);
            ratingBar = itemView.findViewById(R.id.rating_bar);
            llActions = itemView.findViewById(R.id.ll_actions);
            btnEdit = itemView.findViewById(R.id.btn_edit_review);
            btnDelete = itemView.findViewById(R.id.btn_delete_review);
        }
    }
}
