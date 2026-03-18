package com.lexoratech.lexorafix.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldPath;
import com.lexoratech.lexorafix.R;
import com.lexoratech.lexorafix.activities.ProductDetailActivity;
import com.lexoratech.lexorafix.adapters.ProductAdapter;
import com.lexoratech.lexorafix.data.FirestoreHelper;
import com.lexoratech.lexorafix.models.Product;
import com.lexoratech.lexorafix.models.User;

import java.util.ArrayList;
import java.util.List;

public class WishlistFragment extends Fragment {

    private RecyclerView rvWishlist;
    private LinearLayout layoutEmpty;
    private ProgressBar progressBar;
    private ProductAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_wishlist, container, false);

        rvWishlist = view.findViewById(R.id.rv_wishlist);
        layoutEmpty = view.findViewById(R.id.layout_empty);
        progressBar = view.findViewById(R.id.progress_bar);
        ImageView btnBack = view.findViewById(R.id.btn_back);
        android.widget.TextView tvToolbarTitle = view.findViewById(R.id.tv_toolbar_title);
        if (tvToolbarTitle != null) tvToolbarTitle.setText("My Wishlist");

        btnBack.setOnClickListener(v -> {
            if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                getParentFragmentManager().popBackStack();
            }
        });

        rvWishlist.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(requireContext()));

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadWishlistItems();
    }

    private void loadWishlistItems() {
        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;

        if (uid == null) {
            showEmptyState();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        layoutEmpty.setVisibility(View.GONE);
        rvWishlist.setVisibility(View.GONE);

        FirestoreHelper.getInstance().getUserProfile(uid, new FirestoreHelper.FirestoreCallback<User>() {
            @Override
            public void onSuccess(User user) {
                if (user != null && user.getWishlist() != null && !user.getWishlist().isEmpty()) {
                    fetchProductsFromIds(user.getWishlist());
                } else {
                    showEmptyState();
                }
            }

            @Override
            public void onError(String message) {
                showEmptyState();
            }
        });
    }

    private void fetchProductsFromIds(List<String> productIds) {
        // Firestore 'in' query has a limit of 10. For robust implementations, we
        // partition.
        // For simplicity assuming wishlist < 10 for now in this demo, or we chunk it.
        List<List<String>> partitions = new ArrayList<>();
        for (int i = 0; i < productIds.size(); i += 10) {
            partitions.add(productIds.subList(i, Math.min(i + 10, productIds.size())));
        }

        List<Product> finalProducts = new ArrayList<>();
        int[] queriesCompleted = { 0 };

        for (List<String> chunk : partitions) {
            FirestoreHelper.getInstance().getDb().collection(FirestoreHelper.COLLECTION_PRODUCTS)
                    .whereIn(FieldPath.documentId(), chunk)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        for (DocumentSnapshot doc : queryDocumentSnapshots) {
                            Product p = Product.fromMap(doc.getData());
                            if (p != null) {
                                p.setDocumentId(doc.getId());
                                finalProducts.add(p);
                            }
                        }
                        queriesCompleted[0]++;
                        if (queriesCompleted[0] == partitions.size()) {
                            displayProducts(finalProducts);
                        }
                    })
                    .addOnFailureListener(e -> {
                        queriesCompleted[0]++;
                        if (queriesCompleted[0] == partitions.size()) {
                            displayProducts(finalProducts);
                        }
                    });
        }
    }

    private void displayProducts(List<Product> products) {
        progressBar.setVisibility(View.GONE);
        if (products.isEmpty()) {
            showEmptyState();
        } else {
            layoutEmpty.setVisibility(View.GONE);
            rvWishlist.setVisibility(View.VISIBLE);

            adapter = new ProductAdapter(requireContext(), products, true);
            adapter.setOnProductClickListener(new ProductAdapter.OnProductClickListener() {
                @Override
                public void onProductClick(Product product) {
                    Intent intent = new Intent(requireContext(), ProductDetailActivity.class);
                    intent.putExtra("document_id", product.getDocumentId());
                    startActivity(intent);
                }

                @Override
                public void onWishlistClick(Product product, boolean isAdding) {
                    if (FirebaseAuth.getInstance().getCurrentUser() == null) return;
                    String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
                    FirestoreHelper.getInstance().toggleWishlist(uid, product.getDocumentId(), isAdding,
                            new FirestoreHelper.FirestoreCallback<Void>() {
                                @Override
                                public void onSuccess(Void result) {
                                    if (isAdded()) {
                                        android.widget.Toast.makeText(requireContext(),
                                                isAdding ? "Added to Wishlist ❤️" : "Removed from Wishlist",
                                                android.widget.Toast.LENGTH_SHORT).show();
                                    }
                                    if (!isAdding) {
                                        // If removed from wishlist screen, reload list or remove item
                                        loadWishlistItems();
                                    }
                                }

                                @Override
                                public void onError(String message) {
                                }
                            });
                }
            });

            // Set current wishlist state for the adapter so hearts are filled!
            List<String> currentIds = new ArrayList<>();
            for (Product p : products)
                currentIds.add(p.getDocumentId());
            adapter.updateWishlist(currentIds);

            rvWishlist.setAdapter(adapter);
        }
    }

    private void showEmptyState() {
        progressBar.setVisibility(View.GONE);
        rvWishlist.setVisibility(View.GONE);
        layoutEmpty.setVisibility(View.VISIBLE);
    }
}
