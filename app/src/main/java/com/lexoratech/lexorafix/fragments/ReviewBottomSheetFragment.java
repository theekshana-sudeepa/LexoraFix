package com.lexoratech.lexorafix.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.lexoratech.lexorafix.R;
import com.lexoratech.lexorafix.data.FirestoreHelper;
import com.lexoratech.lexorafix.data.SharedPreferencesManager;
import com.lexoratech.lexorafix.models.Review;
import com.lexoratech.lexorafix.network.EmailHelper;

public class ReviewBottomSheetFragment extends BottomSheetDialogFragment {

    private static final String ARG_PRODUCT_ID = "product_id";
    private static final String ARG_PRODUCT_NAME = "product_name";
    private static final String ARG_EDIT_REVIEW_ID = "edit_review_id";
    private static final String ARG_EDIT_RATING = "edit_rating";
    private static final String ARG_EDIT_COMMENT = "edit_comment";

    private String productId;
    private String productName;
    private String editReviewId;
    private float editRating;
    private String editComment;

    private OnReviewActionListener actionListener;

    public interface OnReviewActionListener {
        void onReviewSubmitted();
    }

    public void setOnReviewActionListener(OnReviewActionListener listener) {
        this.actionListener = listener;
    }

    public static ReviewBottomSheetFragment newInstance(String productId, String productName) {
        ReviewBottomSheetFragment fragment = new ReviewBottomSheetFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PRODUCT_ID, productId);
        args.putString(ARG_PRODUCT_NAME, productName);
        fragment.setArguments(args);
        return fragment;
    }

    public static ReviewBottomSheetFragment newInstanceForEdit(String productId, String productName,
            String reviewDocId, float existingRating, String existingComment) {
        ReviewBottomSheetFragment fragment = new ReviewBottomSheetFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PRODUCT_ID, productId);
        args.putString(ARG_PRODUCT_NAME, productName);
        args.putString(ARG_EDIT_REVIEW_ID, reviewDocId);
        args.putFloat(ARG_EDIT_RATING, existingRating);
        args.putString(ARG_EDIT_COMMENT, existingComment);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            productId = getArguments().getString(ARG_PRODUCT_ID);
            productName = getArguments().getString(ARG_PRODUCT_NAME);
            editReviewId = getArguments().getString(ARG_EDIT_REVIEW_ID);
            editRating = getArguments().getFloat(ARG_EDIT_RATING, 5f);
            editComment = getArguments().getString(ARG_EDIT_COMMENT, "");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_review_bottom_sheet, container, false);

        TextView tvTitle = view.findViewById(R.id.tv_title);
        RatingBar ratingBar = view.findViewById(R.id.rating_bar);
        TextInputEditText etComment = view.findViewById(R.id.et_comment);
        MaterialButton btnSubmit = view.findViewById(R.id.btn_submit);

        boolean isEditMode = editReviewId != null && !editReviewId.isEmpty();

        if (isEditMode) {
            tvTitle.setText("Edit Review");
            btnSubmit.setText("Update Review");
            ratingBar.setRating(editRating);
            if (editComment != null) etComment.setText(editComment);
        } else if (productName != null) {
            tvTitle.setText("Rate " + productName);
        }

        btnSubmit.setOnClickListener(v -> {
            float rating = ratingBar.getRating();
            String comment = etComment.getText() != null ? etComment.getText().toString().trim() : "";

            btnSubmit.setEnabled(false);
            btnSubmit.setText(isEditMode ? "Updating..." : "Submitting...");

            if (FirebaseAuth.getInstance().getCurrentUser() == null) {
                if (getContext() != null) {
                    Toast.makeText(requireContext(), "Please login to submit a review", Toast.LENGTH_SHORT).show();
                }
                btnSubmit.setEnabled(true);
                btnSubmit.setText(isEditMode ? "Update Review" : "Submit Review");
                return;
            }
            String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
            String userName = SharedPreferencesManager.getInstance(requireContext()).getUserName();

            FirestoreHelper.getInstance().getDb()
                    .collection("products")
                    .whereEqualTo("name", productName)
                    .limit(1)
                    .get()
                    .addOnSuccessListener(snapshot -> {
                        if (!snapshot.isEmpty()) {
                            String realDocId = snapshot.getDocuments().get(0).getId();

                            if (isEditMode) {
                                // Update existing review
                                java.util.Map<String, Object> updates = new java.util.HashMap<>();
                                updates.put("rating", rating);
                                updates.put("comment", comment);
                                updates.put("timestamp", Timestamp.now());

                                FirestoreHelper.getInstance().getDb()
                                        .collection("products").document(realDocId)
                                        .collection("reviews").document(editReviewId)
                                        .update(updates)
                                        .addOnSuccessListener(aVoid -> {
                                            if (getContext() != null)
                                                Toast.makeText(requireContext(), "Review updated!", Toast.LENGTH_SHORT).show();
                                            if (actionListener != null) actionListener.onReviewSubmitted();
                                            dismiss();
                                        })
                                        .addOnFailureListener(e -> {
                                            btnSubmit.setEnabled(true);
                                            btnSubmit.setText("Update Review");
                                            if (getContext() != null)
                                                Toast.makeText(requireContext(), "Failed to update: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                        });
                            } else {
                                // Check if user already reviewed this product
                                FirestoreHelper.getInstance().getDb()
                                        .collection("products").document(realDocId)
                                        .collection("reviews")
                                        .whereEqualTo("userId", uid)
                                        .limit(1)
                                        .get()
                                        .addOnSuccessListener(existingReviews -> {
                                            if (!existingReviews.isEmpty()) {
                                                btnSubmit.setEnabled(true);
                                                btnSubmit.setText("Submit Review");
                                                if (getContext() != null)
                                                    Toast.makeText(requireContext(), "You have already reviewed this product.", Toast.LENGTH_SHORT).show();
                                                return;
                                            }

                                            Review review = new Review(uid, userName, rating, comment, Timestamp.now());
                                            FirestoreHelper.getInstance().getDb()
                                                    .collection("products").document(realDocId)
                                                    .collection("reviews")
                                                    .add(review.toMap())
                                                    .addOnSuccessListener(documentReference -> {
                                                        EmailHelper.getInstance().sendAdminNewReviewNotification(productName, userName, rating, comment);
                                                        if (getContext() != null)
                                                            Toast.makeText(requireContext(), "Review submitted successfully!", Toast.LENGTH_SHORT).show();
                                                        if (actionListener != null) actionListener.onReviewSubmitted();
                                                        dismiss();
                                                    })
                                                    .addOnFailureListener(e -> {
                                                        btnSubmit.setEnabled(true);
                                                        btnSubmit.setText("Submit Review");
                                                        if (getContext() != null)
                                                            Toast.makeText(requireContext(), "Failed to submit review: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                                    });
                                        })
                                        .addOnFailureListener(e -> {
                                            btnSubmit.setEnabled(true);
                                            btnSubmit.setText("Submit Review");
                                            if (getContext() != null)
                                                Toast.makeText(requireContext(), "Failed to check existing review.", Toast.LENGTH_SHORT).show();
                                        });
                            }
                        } else {
                            btnSubmit.setEnabled(true);
                            btnSubmit.setText(isEditMode ? "Update Review" : "Submit Review");
                            if (getContext() != null)
                                Toast.makeText(requireContext(), "Error: Product not found in database.", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        btnSubmit.setEnabled(true);
                        btnSubmit.setText(isEditMode ? "Update Review" : "Submit Review");
                        if (getContext() != null)
                            Toast.makeText(requireContext(), "Failed to query product.", Toast.LENGTH_SHORT).show();
                    });
        });

        return view;
    }
}
