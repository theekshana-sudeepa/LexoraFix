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

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.DocumentSnapshot;
import com.lexoratech.lexorafix.R;
import com.lexoratech.lexorafix.activities.MainActivity;
import com.lexoratech.lexorafix.activities.ProductDetailActivity;
import com.lexoratech.lexorafix.activities.MapActivity;

import com.lexoratech.lexorafix.adapters.ProductAdapter;
import com.lexoratech.lexorafix.data.FirestoreHelper;
import com.lexoratech.lexorafix.data.SharedPreferencesManager;
import com.lexoratech.lexorafix.models.Category;
import com.lexoratech.lexorafix.models.Product;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private ProductAdapter productAdapter;
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
            tvGreeting.setText(prefs.getUserName());
        }

        // Map icon
        ImageView ivMap = view.findViewById(R.id.iv_map);
        ivMap.setOnClickListener(v -> {
            startActivity(new Intent(requireContext(), MapActivity.class));
        });

        // Notification bell — navigates to NotificationFragment
        ImageView ivNotifications = view.findViewById(R.id.iv_notifications);
        View notificationBadge = view.findViewById(R.id.notification_badge);
        ivNotifications.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager()
                    .beginTransaction()
                    .setCustomAnimations(android.R.anim.slide_in_left, android.R.anim.slide_out_right,
                            android.R.anim.slide_in_left, android.R.anim.slide_out_right)
                    .replace(R.id.fragment_container, new NotificationFragment())
                    .addToBackStack(null)
                    .commit();
        });

        // Show badge dot if there are active bookings
        checkForActiveBookings(notificationBadge);

        // Categories Carousel — swipeable pages with 2 rows × 2 columns
        androidx.viewpager2.widget.ViewPager2 vpCategories = view.findViewById(R.id.vp_categories);
        android.widget.LinearLayout llDots = view.findViewById(R.id.ll_category_dots);

        // Load categories from Firestore
        FirestoreHelper firestoreHelper = FirestoreHelper.getInstance();
        firestoreHelper
                .getAllCategories(new FirestoreHelper.FirestoreCallback<com.google.firebase.firestore.QuerySnapshot>() {
                    @Override
                    public void onSuccess(com.google.firebase.firestore.QuerySnapshot result) {
                        if (!isAdded() || getContext() == null) return;
                        List<Category> categories = new ArrayList<>();
                        for (DocumentSnapshot doc : result.getDocuments()) {
                            Category cat = Category.fromMap(doc.getData());
                            if (cat != null) {
                                cat.setDocumentId(doc.getId());
                                categories.add(cat);
                            }
                        }
                        com.lexoratech.lexorafix.adapters.CategoryCarouselAdapter carouselAdapter =
                                new com.lexoratech.lexorafix.adapters.CategoryCarouselAdapter(requireContext(), categories);
                        carouselAdapter.setOnCategoryClickListener(category -> {
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
                        vpCategories.setAdapter(carouselAdapter);

                        // Setup dot indicators
                        setupDotIndicators(llDots, carouselAdapter.getPageCount(), 0);
                        vpCategories.registerOnPageChangeCallback(new androidx.viewpager2.widget.ViewPager2.OnPageChangeCallback() {
                            @Override
                            public void onPageSelected(int position) {
                                setupDotIndicators(llDots, carouselAdapter.getPageCount(), position);
                            }
                        });
                    }

                    @Override
                    public void onError(String message) {
                        // Silent fail — categories remain empty
                    }
                });

        // Featured Products RecyclerView — vertical list with horizontal cards
        RecyclerView rvFeatured = view.findViewById(R.id.rv_featured);
        rvFeatured.setLayoutManager(new LinearLayoutManager(requireContext()));

        // See All clicks (Now LinearLayouts containing the text and arrow)
        View llSeeAllCategories = view.findViewById(R.id.ll_see_all_categories);
        View llSeeAllFeatured = view.findViewById(R.id.ll_see_all_featured);

        if (llSeeAllCategories != null) {
            llSeeAllCategories.setOnClickListener(v -> {
                if (requireActivity() instanceof MainActivity) {
                    ((MainActivity) requireActivity()).navigateToCategories();
                }
            });
        }

        if (llSeeAllFeatured != null) {
            llSeeAllFeatured.setOnClickListener(v -> {
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

        productAdapter = new ProductAdapter(requireContext(), new java.util.ArrayList<>(), true);
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
                                    if (isAdded()) {
                                        android.widget.Toast.makeText(requireContext(),
                                                isAdding ? "Added to Wishlist ❤️" : "Removed from Wishlist",
                                                android.widget.Toast.LENGTH_SHORT).show();
                                    }
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
                        if (!isAdded() || getContext() == null) return;
                        allProducts.clear();
                        for (DocumentSnapshot doc : result.getDocuments()) {
                            Product p = Product.fromMap(doc.getData());
                            if (p != null && p.getEffectiveStock() > 0) {
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
                                            if (!isAdded() || getContext() == null) return;
                                            for (DocumentSnapshot doc : result.getDocuments()) {
                                                Product p = Product.fromMap(doc.getData());
                                                if (p != null && p.getEffectiveStock() > 0) {
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
                            if (!isAdded() || getContext() == null) return;
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
                if (p.getName().toLowerCase(java.util.Locale.getDefault()).contains(query.toLowerCase(java.util.Locale.getDefault()))) {
                    filtered.add(p);
                }
            }
            productAdapter.updateList(filtered);
        }
    }

    /**
     * Check if the user has any unread notifications.
     * If so, show the red notification badge dot on the bell icon.
     */
    private void checkForActiveBookings(View badgeView) {
        if (badgeView == null) return;
        String uid = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser() != null
                ? com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;
        if (uid == null) return;

        // Check for unread notifications in the user's notifications subcollection
        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("users").document(uid)
                .collection("notifications")
                .whereEqualTo("read", false)
                .limit(1)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (isAdded() && !snapshot.isEmpty()) {
                        badgeView.setVisibility(View.VISIBLE);
                    } else if (isAdded()) {
                        badgeView.setVisibility(View.GONE);
                    }
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("HomeFragment", "Failed to check notifications: " + e.getMessage());
                });
    }

    /**
     * Create/update dot indicators for the category carousel.
     */
    private void setupDotIndicators(android.widget.LinearLayout container, int totalPages, int currentPage) {
        if (container == null || totalPages <= 1) {
            if (container != null) container.setVisibility(View.GONE);
            return;
        }
        container.setVisibility(View.VISIBLE);
        container.removeAllViews();

        for (int i = 0; i < totalPages; i++) {
            View dot = new View(requireContext());
            int size = (int) (8 * getResources().getDisplayMetrics().density);
            android.widget.LinearLayout.LayoutParams params =
                    new android.widget.LinearLayout.LayoutParams(size, size);
            int margin = (int) (4 * getResources().getDisplayMetrics().density);
            params.setMargins(margin, 0, margin, 0);
            dot.setLayoutParams(params);

            android.graphics.drawable.GradientDrawable drawable = new android.graphics.drawable.GradientDrawable();
            drawable.setShape(android.graphics.drawable.GradientDrawable.OVAL);
            drawable.setColor(i == currentPage ? 0xFF2563EB : 0xFFD1D5DB);
            dot.setBackground(drawable);

            container.addView(dot);
        }
    }
}
