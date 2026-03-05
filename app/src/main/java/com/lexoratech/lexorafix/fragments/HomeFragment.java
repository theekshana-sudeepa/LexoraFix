package com.lexoratech.lexorafix.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.DocumentSnapshot;
import com.lexoratech.lexorafix.R;
import com.lexoratech.lexorafix.activities.MainActivity;
import com.lexoratech.lexorafix.activities.ProductDetailActivity;
import com.lexoratech.lexorafix.activities.MapActivity;
import com.lexoratech.lexorafix.adapters.CategoryAdapter;
import com.lexoratech.lexorafix.adapters.ProductAdapter;
import com.lexoratech.lexorafix.data.FirestoreHelper;
import com.lexoratech.lexorafix.data.SharedPreferencesManager;
import com.lexoratech.lexorafix.models.Category;
import com.lexoratech.lexorafix.models.Product;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private ProductAdapter productAdapter;
    private CategoryAdapter categoryAdapter;
    private List<Product> allProducts = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        // Greeting
        TextView tvGreeting = view.findViewById(R.id.tv_greeting);
        SharedPreferencesManager prefs = SharedPreferencesManager.getInstance(requireContext());
        if (prefs.isLoggedIn()) {
            tvGreeting.setText("Hello, " + prefs.getUserName() + "!");
        }

        // Map icon
        ImageView ivMap = view.findViewById(R.id.iv_map);
        ivMap.setOnClickListener(v -> {
            startActivity(new Intent(requireContext(), MapActivity.class));
        });

        // Categories RecyclerView
        RecyclerView rvCategories = view.findViewById(R.id.rv_categories);
        rvCategories.setLayoutManager(new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));

        // Load categories from Firestore
        FirestoreHelper firestoreHelper = FirestoreHelper.getInstance();
        firestoreHelper
                .getAllCategories(new FirestoreHelper.FirestoreCallback<com.google.firebase.firestore.QuerySnapshot>() {
                    @Override
                    public void onSuccess(com.google.firebase.firestore.QuerySnapshot result) {
                        List<Category> categories = new ArrayList<>();
                        for (DocumentSnapshot doc : result.getDocuments()) {
                            Category cat = Category.fromMap(doc.getData());
                            if (cat != null) {
                                cat.setDocumentId(doc.getId());
                                categories.add(cat);
                            }
                        }
                        categoryAdapter = new CategoryAdapter(requireContext(), categories, true);
                        categoryAdapter.setOnCategoryClickListener(category -> {
                            ProductListFragment fragment = ProductListFragment.newInstance(
                                    category.getDocumentId(), category.getName());
                            requireActivity().getSupportFragmentManager()
                                    .beginTransaction()
                                    .setCustomAnimations(android.R.anim.slide_in_left, android.R.anim.slide_out_right,
                                            android.R.anim.slide_in_left, android.R.anim.slide_out_right)
                                    .replace(R.id.fragment_container, fragment)
                                    .addToBackStack(null)
                                    .commit();
                        });
                        rvCategories.setAdapter(categoryAdapter);
                    }

                    @Override
                    public void onError(String message) {
                        // Silent fail — categories remain empty
                    }
                });

        // Featured Products RecyclerView
        RecyclerView rvFeatured = view.findViewById(R.id.rv_featured);
        rvFeatured.setLayoutManager(new GridLayoutManager(requireContext(), 2));

        // See All clicks
        TextView tvSeeAllCategories = view.findViewById(R.id.tv_see_all_categories);
        TextView tvSeeAllFeatured = view.findViewById(R.id.tv_see_all_featured);

        if (tvSeeAllCategories != null) {
            tvSeeAllCategories.setOnClickListener(v -> {
                if (requireActivity() instanceof MainActivity) {
                    ((MainActivity) requireActivity()).navigateToCategories();
                }
            });
        }

        if (tvSeeAllFeatured != null) {
            tvSeeAllFeatured.setOnClickListener(v -> {
                ProductListFragment fragment = ProductListFragment.newInstance(null, "All Products");
                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .setCustomAnimations(android.R.anim.slide_in_left, android.R.anim.slide_out_right,
                                android.R.anim.slide_in_left, android.R.anim.slide_out_right)
                        .replace(R.id.fragment_container, fragment)
                        .addToBackStack(null)
                        .commit();
            });
        }

        productAdapter = new ProductAdapter(requireContext(), new ArrayList<>());
        productAdapter.setOnProductClickListener(new ProductAdapter.OnProductClickListener() {
            @Override
            public void onProductClick(Product product) {
                Intent intent = new Intent(requireContext(), ProductDetailActivity.class);
                intent.putExtra("document_id", product.getDocumentId());
                startActivity(intent);
            }

            @Override
            public void onWishlistClick(Product product, boolean isAdding) {
                String uid = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser() != null
                        ? com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid()
                        : null;
                if (uid != null) {
                    FirestoreHelper.getInstance().toggleWishlist(uid, product.getDocumentId(), isAdding,
                            new FirestoreHelper.FirestoreCallback<Void>() {
                                @Override
                                public void onSuccess(Void result) {
                                }

                                @Override
                                public void onError(String message) {
                                }
                            });
                } else {
                    android.widget.Toast.makeText(requireContext(), "Please login to save favorites",
                            android.widget.Toast.LENGTH_SHORT).show();
                }
            }
        });
        rvFeatured.setAdapter(productAdapter);

        // Load featured products from Firestore
        firestoreHelper.getFeaturedProducts(
                new FirestoreHelper.FirestoreCallback<com.google.firebase.firestore.QuerySnapshot>() {

                    @Override
                    public void onSuccess(com.google.firebase.firestore.QuerySnapshot result) {
                        allProducts.clear();
                        for (DocumentSnapshot doc : result.getDocuments()) {
                            Product p = Product.fromMap(doc.getData());
                            if (p != null) {
                                p.setDocumentId(doc.getId());
                                allProducts.add(p);
                            }
                        }
                        // If no featured products, fall back to all products
                        if (allProducts.isEmpty()) {
                            firestoreHelper.getAllProducts(
                                    new FirestoreHelper.FirestoreCallback<com.google.firebase.firestore.QuerySnapshot>() {
                                        @Override
                                        public void onSuccess(com.google.firebase.firestore.QuerySnapshot result) {
                                            for (DocumentSnapshot doc : result.getDocuments()) {
                                                Product p = Product.fromMap(doc.getData());
                                                if (p != null) {
                                                    p.setDocumentId(doc.getId());
                                                    allProducts.add(p);
                                                }
                                            }
                                            productAdapter.updateList(allProducts);
                                        }

                                        @Override
                                        public void onError(String message) {
                                        }
                                    });
                        } else {
                            productAdapter.updateList(allProducts);
                        }
                    }

                    @Override
                    public void onError(String message) {
                    }
                });

        // Search
        android.widget.EditText etSearch = view.findViewById(R.id.et_search);
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterProducts(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        String uid = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser() != null
                ? com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;
        if (uid != null && productAdapter != null) {
            FirestoreHelper.getInstance().getUserProfile(uid,
                    new FirestoreHelper.FirestoreCallback<com.lexoratech.lexorafix.models.User>() {
                        @Override
                        public void onSuccess(com.lexoratech.lexorafix.models.User user) {
                            if (user != null && user.getWishlist() != null) {
                                productAdapter.updateWishlist(user.getWishlist());
                            }
                        }

                        @Override
                        public void onError(String message) {
                        }
                    });
        }
    }

    private void filterProducts(String query) {
        if (query.isEmpty()) {
            productAdapter.updateList(allProducts);
        } else {
            List<Product> filtered = new ArrayList<>();
            for (Product p : allProducts) {
                if (p.getName().toLowerCase().contains(query.toLowerCase())) {
                    filtered.add(p);
                }
            }
            productAdapter.updateList(filtered);
        }
    }
}
