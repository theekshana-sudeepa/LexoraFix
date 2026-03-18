package com.lexoratech.lexorafix.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.lexoratech.lexorafix.R;
import com.lexoratech.lexorafix.activities.ProductDetailActivity;
import com.lexoratech.lexorafix.adapters.ProductAdapter;
import com.lexoratech.lexorafix.data.FirestoreHelper;
import com.lexoratech.lexorafix.models.Category;
import com.lexoratech.lexorafix.models.Product;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class ProductListFragment extends Fragment {

    private static final String ARG_CATEGORY_DOC_ID = "category_doc_id";
    private static final String ARG_CATEGORY_NAME = "category_name";

    private ProductAdapter adapter;
    private List<Product> allProducts = new ArrayList<>();
    private List<Category> allCategories = new ArrayList<>();

    // Current filter state
    private String searchQuery = "";
    private float filterMinPrice = 0;
    private float filterMaxPrice = 50000;
    private String filterSort = "Newest";
    private String filterCategoryDocId = ""; // "" = All
    private int filterMinRating = 0; // 0 = All
    private boolean filterInStockOnly = false;

    private TextView tvFilterBadge;

    public static ProductListFragment newInstance(String categoryDocId, String categoryName) {
        ProductListFragment fragment = new ProductListFragment();
        Bundle args = new Bundle();
        args.putString(ARG_CATEGORY_DOC_ID, categoryDocId);
        args.putString(ARG_CATEGORY_NAME, categoryName);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_product_list, container, false);

        TextView tvTitle = view.findViewById(R.id.tv_title);
        ImageView ivBack = view.findViewById(R.id.iv_back);
        ImageView ivFilter = view.findViewById(R.id.iv_filter);
        tvFilterBadge = view.findViewById(R.id.tv_filter_badge);

        String categoryDocId = null;
        String categoryName = "Products";

        if (getArguments() != null) {
            categoryDocId = getArguments().getString(ARG_CATEGORY_DOC_ID, null);
            categoryName = getArguments().getString(ARG_CATEGORY_NAME, "Products");
        }
        tvTitle.setText(categoryName);

        // If opened via a specific category, pre-set the filter
        if (categoryDocId != null && !categoryDocId.isEmpty()) {
            filterCategoryDocId = categoryDocId;
        }

        ivBack.setOnClickListener(v -> {
            if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                getParentFragmentManager().popBackStack();
            }
        });

        // ── Filter icon ──
        ivFilter.setOnClickListener(v -> openFilterDrawer());

        // ── Search bar ──
        android.widget.EditText etSearchProduct = view.findViewById(R.id.et_search_product);
        etSearchProduct.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                searchQuery = s.toString();
                applyFilters();
            }

            @Override
            public void afterTextChanged(android.text.Editable s) {
            }
        });

        RecyclerView rvProducts = view.findViewById(R.id.rv_products);
        rvProducts.setLayoutManager(new GridLayoutManager(requireContext(), 2));

        // ── Load categories for the filter bottom sheet ──
        FirestoreHelper.getInstance().getAllCategories(
                new FirestoreHelper.FirestoreCallback<com.google.firebase.firestore.QuerySnapshot>() {
                    @Override
                    public void onSuccess(com.google.firebase.firestore.QuerySnapshot result) {
                        allCategories.clear();
                        for (DocumentSnapshot doc : result.getDocuments()) {
                            Category cat = Category.fromMap(doc.getData());
                            if (cat != null) {
                                cat.setDocumentId(doc.getId());
                                allCategories.add(cat);
                            }
                        }
                    }

                    @Override
                    public void onError(String message) {
                        // Categories remain empty — filter sheet will only show "All"
                    }
                });

        // ── Load products from Firestore ──
        // Always load ALL products from Firestore so the bottom sheet filter can change categories locally
        FirestoreHelper.getInstance().getAllProducts(
                new FirestoreHelper.FirestoreCallback<com.google.firebase.firestore.QuerySnapshot>() {
                    @Override
                    public void onSuccess(com.google.firebase.firestore.QuerySnapshot result) {
                        setAdapter(rvProducts, result);
                    }

                    @Override
                    public void onError(String message) {
                    }
                });

        updateFilterBadge();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        syncWishlist();
    }

    // ══════════════════════════════════════════════════
    // Filter Panel
    // ══════════════════════════════════════════════════

    private void openFilterDrawer() {
        FilterBottomSheetFragment sheet = new FilterBottomSheetFragment();
        sheet.setCategories(allCategories);
        sheet.setCurrentFilters(filterMinPrice, filterMaxPrice, filterSort,
                filterCategoryDocId, filterMinRating, filterInStockOnly);
        sheet.setFilterListener((minPrice, maxPrice, sortOrder, categoryDocId, minRating, inStockOnly) -> {
            filterMinPrice = minPrice;
            filterMaxPrice = maxPrice;
            filterSort = sortOrder;
            filterCategoryDocId = categoryDocId;
            filterMinRating = minRating;
            filterInStockOnly = inStockOnly;
            applyFilters();
            updateFilterBadge();
            
            if (getView() != null) {
                DrawerLayout drawer = getView().findViewById(R.id.drawer_layout);
                if (drawer != null) {
                    drawer.closeDrawer(GravityCompat.START);
                }
            }
        });

        getChildFragmentManager().beginTransaction()
                .replace(R.id.filter_container, sheet)
                .commit();

        if (getView() != null) {
            DrawerLayout drawer = getView().findViewById(R.id.drawer_layout);
            if (drawer != null) {
                drawer.openDrawer(GravityCompat.START);
            }
        }
    }

    // ══════════════════════════════════════════════════
    // Combined Filtering
    // ══════════════════════════════════════════════════

    private void applyFilters() {
        if (adapter == null)
            return;

        List<Product> filtered = new ArrayList<>();

        for (Product p : allProducts) {

            // 1) Text search
            if (!searchQuery.isEmpty()) {
                String q = searchQuery.toLowerCase().trim();
                boolean nameMatch = p.getName() != null && p.getName().toLowerCase().contains(q);
                boolean descMatch = p.getDescription() != null && p.getDescription().toLowerCase().contains(q);
                if (!nameMatch && !descMatch)
                    continue;
            }

            // 2) Price range
            if (p.getPrice() < filterMinPrice || p.getPrice() > filterMaxPrice)
                continue;

            // 3) Category
            if (filterCategoryDocId != null && !filterCategoryDocId.isEmpty()) {
                if (p.getCategoryDocId() == null ||
                        !p.getCategoryDocId().equals(filterCategoryDocId))
                    continue;
            }

            // 4) Stock availability
            if (filterInStockOnly && p.getStock() <= 0)
                continue;

            // Product passed all filters
            filtered.add(p);
        }

        // 5) Sort
        sortProducts(filtered, filterSort);

        adapter.updateList(filtered);
    }

    private void sortProducts(List<Product> products, String sortOrder) {
        switch (sortOrder) {
            case "Price: Low → High":
                Collections.sort(products, Comparator.comparingDouble(Product::getPrice));
                break;
            case "Price: High → Low":
                Collections.sort(products, (a, b) -> Double.compare(b.getPrice(), a.getPrice()));
                break;
            case "Popular":
                // Sort by stock descending (higher stock = more popular proxy)
                Collections.sort(products, (a, b) -> Integer.compare(b.getStock(), a.getStock()));
                break;
            case "Newest":
            default:
                // Keep original Firestore order (newest first)
                break;
        }
    }

    // ══════════════════════════════════════════════════
    // Badge
    // ══════════════════════════════════════════════════

    private void updateFilterBadge() {
        int count = 0;
        if (filterMinPrice > 0 || filterMaxPrice < 50000)
            count++;
        if (!"Newest".equalsIgnoreCase(filterSort))
            count++;
        if (filterCategoryDocId != null && !filterCategoryDocId.isEmpty())
            count++;
        if (filterMinRating > 0)
            count++;
        if (filterInStockOnly)
            count++;

        if (tvFilterBadge != null) {
            if (count > 0) {
                tvFilterBadge.setText(String.valueOf(count));
                tvFilterBadge.setVisibility(View.VISIBLE);
            } else {
                tvFilterBadge.setVisibility(View.GONE);
            }
        }
    }

    // ══════════════════════════════════════════════════
    // Wishlist
    // ══════════════════════════════════════════════════

    private void syncWishlist() {
        String uid = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser() != null
                ? com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;
        if (uid != null && adapter != null) {
            FirestoreHelper.getInstance().getUserProfile(uid,
                    new FirestoreHelper.FirestoreCallback<com.lexoratech.lexorafix.models.User>() {
                        @Override
                        public void onSuccess(com.lexoratech.lexorafix.models.User user) {
                            if (user != null && user.getWishlist() != null) {
                                adapter.updateWishlist(user.getWishlist());
                            }
                        }

                        @Override
                        public void onError(String message) {
                        }
                    });
        }
    }

    // ══════════════════════════════════════════════════
    // Adapter setup
    // ══════════════════════════════════════════════════

    private void setAdapter(RecyclerView rv, com.google.firebase.firestore.QuerySnapshot snapshot) {
        List<Product> products = new ArrayList<>();
        for (DocumentSnapshot doc : snapshot.getDocuments()) {
            Product p = Product.fromMap(doc.getData());
            if (p != null) {
                p.setDocumentId(doc.getId());
                products.add(p);
            }
        }
        allProducts.clear();
        allProducts.addAll(products);

        adapter = new ProductAdapter(requireContext(), products);
        adapter.setOnProductClickListener(new ProductAdapter.OnProductClickListener() {
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
        rv.setAdapter(adapter);
        syncWishlist();

        // Apply any pre-set filters (e.g. if opened via a specific category)
        applyFilters();
    }
}
