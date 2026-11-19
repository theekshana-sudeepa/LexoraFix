package com.lexoratech.lexorafix.admin.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.lexoratech.lexorafix.admin.R;
import com.lexoratech.lexorafix.admin.adapters.AdminReviewAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminProductReviewsActivity extends AppCompatActivity {

    private String productDocId;
    private RecyclerView rvReviews;
    private TextView tvEmpty;
    private ProgressBar progressBar;
    private AdminReviewAdapter adapter;
    private List<Map<String, Object>> reviewsList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_product_reviews);

        productDocId = getIntent().getStringExtra("productDocId");
        String productName = getIntent().getStringExtra("productName");

        ImageView btnBack = findViewById(R.id.btn_back);
        TextView tvTitle = findViewById(R.id.tv_title);
        rvReviews = findViewById(R.id.rv_reviews);
        tvEmpty = findViewById(R.id.tv_empty);
        progressBar = findViewById(R.id.progress_bar);

        if (productName != null && !productName.isEmpty()) {
            tvTitle.setText("Reviews: " + productName);
        }

        btnBack.setOnClickListener(v -> finish());

        rvReviews.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AdminReviewAdapter(this, reviewsList);
        
        adapter.setOnReviewActionListener((reviewDocId, position) -> {
            new AlertDialog.Builder(this)
                    .setTitle("Delete Review")
                    .setMessage("Are you sure you want to delete this review?")
                    .setPositiveButton("Delete", (dialog, which) -> deleteReview(reviewDocId, position))
                    .setNegativeButton("Cancel", null)
                    .show();
        });
        
        rvReviews.setAdapter(adapter);

        if (productDocId != null) {
            loadReviews();
        } else {
            Toast.makeText(this, "Empty Product", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void loadReviews() {
        progressBar.setVisibility(View.VISIBLE);
        tvEmpty.setVisibility(View.GONE);
        
        FirebaseFirestore.getInstance()
                .collection("products")
                .document(productDocId)
                .collection("reviews")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    progressBar.setVisibility(View.GONE);
                    reviewsList.clear();
                    if (queryDocumentSnapshots != null && !queryDocumentSnapshots.isEmpty()) {
                        for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                            Map<String, Object> data = doc.getData();
                            if (data != null) {
                                data.put("_documentId", doc.getId());
                                reviewsList.add(data);
                            }
                        }
                    }
                    adapter.notifyDataSetChanged();
                    
                    if (reviewsList.isEmpty()) {
                        tvEmpty.setVisibility(View.VISIBLE);
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, "Failed to load reviews.", Toast.LENGTH_SHORT).show();
                });
    }

    private void deleteReview(String reviewDocId, int position) {
        FirebaseFirestore.getInstance()
                .collection("products")
                .document(productDocId)
                .collection("reviews")
                .document(reviewDocId)
                .delete()
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Review deleted.", Toast.LENGTH_SHORT).show();
                    if (position < reviewsList.size()) {
                        reviewsList.remove(position);
                        adapter.notifyItemRemoved(position);
                    }
                    if (reviewsList.isEmpty()) {
                        tvEmpty.setVisibility(View.VISIBLE);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to delete review.", Toast.LENGTH_SHORT).show();
                });
    }
}
