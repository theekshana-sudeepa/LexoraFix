package com.lexoratech.lexorafix.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RatingBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.lexoratech.lexorafix.R;
import com.lexoratech.lexorafix.data.FirestoreHelper;

public class RateWorkerBottomSheetFragment extends BottomSheetDialogFragment {

    private String bookingId;
    private RatingSubmitListener submitListener;

    public interface RatingSubmitListener {
        void onRatingSubmitted();
    }

    public static RateWorkerBottomSheetFragment newInstance(String bookingId, RatingSubmitListener listener) {
        RateWorkerBottomSheetFragment fragment = new RateWorkerBottomSheetFragment();
        fragment.bookingId = bookingId;
        fragment.submitListener = listener;
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_rate_worker_bottom_sheet, container, false);
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        RatingBar ratingBar = view.findViewById(R.id.ratingBarWorker);
        TextInputEditText inputReviewText = view.findViewById(R.id.inputReviewText);
        MaterialButton btnSubmitRating = view.findViewById(R.id.btnSubmitRating);

        btnSubmitRating.setOnClickListener(v -> {
            float rating = ratingBar.getRating();
            String reviewText = inputReviewText.getText() != null ? inputReviewText.getText().toString().trim() : "";

            if (rating < 1.0f) {
                Toast.makeText(getContext(), "Please select a rating", Toast.LENGTH_SHORT).show();
                return;
            }

            btnSubmitRating.setEnabled(false);
            btnSubmitRating.setText("Submitting...");

            String uid = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser() != null ? 
                    com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid() : "";

            FirestoreHelper.getInstance().submitBookingRating(uid, bookingId, rating, reviewText, new FirestoreHelper.FirestoreCallback<Void>() {
                @Override
                public void onSuccess(Void result) {
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Thank you for the feedback!", Toast.LENGTH_SHORT).show();
                    }
                    if (submitListener != null) {
                        submitListener.onRatingSubmitted();
                    }
                    dismiss();
                }

                @Override
                public void onError(String message) {
                    btnSubmitRating.setEnabled(true);
                    btnSubmitRating.setText("Submit Rating");
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Failed to submit rating: " + message, Toast.LENGTH_SHORT).show();
                    }
                }
            });
        });
    }
}
