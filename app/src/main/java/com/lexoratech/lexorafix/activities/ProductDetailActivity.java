package com.lexoratech.lexorafix.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import androidx.viewpager2.widget.ViewPager2;
import com.lexoratech.lexorafix.R;
import com.lexoratech.lexorafix.data.DatabaseHelper;
import com.lexoratech.lexorafix.models.CartItem;
import com.lexoratech.lexorafix.models.Product;
import com.lexoratech.lexorafix.models.ProductVariant;
import com.lexoratech.lexorafix.models.VariantOption;
import com.lexoratech.lexorafix.adapters.ProductImageSliderAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ProductDetailActivity extends AppCompatActivity {

    private static final String TAG = "ProductDetailActivity";

    private Product product;
    private String documentId;
    private ListenerRegistration productListener;
    private boolean isWishlisted = false;
    private int selectedQuantity = 1;
    private ProductVariant selectedVariant = null;

    // Per-group selection tracking: index = option group index, value = selected
    // chip label
    private final List<String> selectedOptionValues = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_detail);

        documentId = getIntent().getStringExtra("document_id");

        // Legacy fallback for int product_id
        if (documentId == null || documentId.isEmpty()) {
            int productId = getIntent().getIntExtra("product_id", -1);
            if (productId == -1) {
                finish();
                return;
            }
            // Fallback to local DB
            DatabaseHelper db = DatabaseHelper.getInstance(this);
            product = db.getProductById(productId);
            if (product != null) {
                bindProduct();
                return;
            }

            // If not in local DB, it was likely fetched from Firestore and added to the
            // SQLite cart with a hashcode ID.
            // Find it in Firestore by matching the hashcode.
            FirebaseFirestore.getInstance().collection("products").get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        for (com.google.firebase.firestore.DocumentSnapshot doc : queryDocumentSnapshots) {
                            if (Math.abs(doc.getId().hashCode()) == productId) {
                                documentId = doc.getId();
                                attachProductListener();
                                return;
                            }
                        }
                        finish();
                    }).addOnFailureListener(e -> finish());
            return;
        }

        // Real-time Firestore listener — keeps stock updated automatically
        attachProductListener();
    }

    /**
     * Attach a real-time Firestore snapshot listener so stock (and other fields)
     * update live — e.g. after an order decrements the count.
     */
    private void attachProductListener() {
        if (documentId == null || documentId.isEmpty())
            return;

        productListener = FirebaseFirestore.getInstance()
                .collection("products")
                .document(documentId)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Product listener error: " + error.getMessage());
                        Toast.makeText(this, "Failed to load product", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (snapshot != null && snapshot.exists()) {
                        product = Product.fromMap(snapshot.getData());
                        if (product != null) {
                            product.setDocumentId(snapshot.getId());
                            bindProduct();
                        } else {
                            finish();
                        }
                    } else {
                        Log.w(TAG, "Product document does not exist");
                        finish();
                    }
                });
    }

    private void bindProduct() {
        // Views
        ViewPager2 viewPagerImages = findViewById(R.id.view_pager_images);
        TextView tvImageIndicator = findViewById(R.id.tv_image_indicator);
        ImageView btnBack = findViewById(R.id.btn_back);
        ImageView btnPlayVideo = findViewById(R.id.btn_play_video);
        TextView tvName = findViewById(R.id.tv_name);
        TextView tvPrice = findViewById(R.id.tv_price);
        TextView tvStock = findViewById(R.id.tv_stock);
        TextView tvDescription = findViewById(R.id.tv_description);
        MaterialButton btnVideo = findViewById(R.id.btn_video);
        MaterialButton btnAddCart = findViewById(R.id.btn_add_cart);
        MaterialButton btnBuyNow = findViewById(R.id.btn_buy_now);

        // Set data
        tvName.setText(product.getName());
        tvPrice.setText(String.format(Locale.getDefault(), "LKR %.2f", product.getPrice()));
        tvDescription.setText(product.getDescription());

        // Setup Image Slider
        List<String> imageUrls = product.getImageUrls() != null ? new ArrayList<>(product.getImageUrls())
                : new ArrayList<>();
        if (imageUrls.isEmpty() && product.getImageUrl() != null && !product.getImageUrl().isEmpty()) {
            imageUrls.add(product.getImageUrl());
        }

        if (imageUrls.isEmpty()) {
            // Fallback empty view or placeholder inside the dataset itself
            imageUrls.add(""); // adapter placeholder will trigger
        }

        ProductImageSliderAdapter adapter = new ProductImageSliderAdapter(this, imageUrls);
        viewPagerImages.setAdapter(adapter);

        if (imageUrls.size() > 1) {
            tvImageIndicator.setVisibility(View.VISIBLE);
            tvImageIndicator.setText(1 + "/" + imageUrls.size());
            viewPagerImages.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
                @Override
                public void onPageSelected(int position) {
                    tvImageIndicator.setText((position + 1) + "/" + imageUrls.size());
                }
            });
        } else {
            tvImageIndicator.setVisibility(View.GONE);
        }

        if (product.getStock() > 0) {
            tvStock.setText(getString(R.string.in_stock) + " (" + product.getStock() + " available)");
            tvStock.setTextColor(getResources().getColor(R.color.colorSuccess, null));
            btnAddCart.setEnabled(true);
            btnBuyNow.setEnabled(true);
        } else {
            tvStock.setText(R.string.out_of_stock);
            tvStock.setTextColor(getResources().getColor(R.color.colorError, null));
            btnAddCart.setEnabled(false);
            btnBuyNow.setEnabled(false);
        }

        // === Variant Selectors ===
        LinearLayout layoutVariantOptions = findViewById(R.id.layout_variant_options);
        layoutVariantOptions.removeAllViews();

        if (product.hasVariantOptions()) {
            // ── Multi-attribute mode (e.g. Voltage + Capacity) ─────────────
            layoutVariantOptions.setVisibility(View.VISIBLE);

            List<VariantOption> options = product.getVariantOptions();
            // Initialise selectedOptionValues if needed
            selectedOptionValues.clear();
            for (VariantOption opt : options) {
                // Default to first value of each option
                String firstVal = (opt.getValues() != null && !opt.getValues().isEmpty())
                        ? opt.getValues().get(0)
                        : "";
                selectedOptionValues.add(firstVal);
            }
            // Pre-select the matching variant from defaults
            selectedVariant = findVariant();
            updatePriceStock(tvPrice, tvStock, btnAddCart, btnBuyNow);

            for (int g = 0; g < options.size(); g++) {
                final int groupIdx = g;
                VariantOption opt = options.get(g);

                // Group title
                TextView tvGroupTitle = new TextView(this);
                tvGroupTitle.setText(opt.getName());
                tvGroupTitle.setTextColor(getResources().getColor(R.color.textPrimary, null));
                tvGroupTitle.setTextSize(15f);
                tvGroupTitle.setTypeface(null, android.graphics.Typeface.BOLD);
                tvGroupTitle.setPadding(0, 16, 0, 4);
                layoutVariantOptions.addView(tvGroupTitle);

                // Chip group for this option
                ChipGroup cg = new ChipGroup(this);
                cg.setSingleSelection(true);
                cg.setSelectionRequired(true);
                layoutVariantOptions.addView(cg);

                if (opt.getValues() == null)
                    continue;
                for (int c = 0; c < opt.getValues().size(); c++) {
                    final int chipIdx = c;
                    String val = opt.getValues().get(c);
                    Chip chip = new Chip(this);
                    chip.setText(val);
                    chip.setCheckable(true);
                    chip.setChipBackgroundColorResource(R.color.colorSurface);
                    chip.setTextColor(getResources().getColor(R.color.textPrimary, null));

                    chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
                        if (isChecked) {
                            selectedOptionValues.set(groupIdx, opt.getValues().get(chipIdx));
                            selectedVariant = findVariant();
                            updatePriceStock(tvPrice, tvStock, btnAddCart, btnBuyNow);
                            // Reset quantity
                            selectedQuantity = 1;
                            TextView tvQty = findViewById(R.id.tv_quantity);
                            if (tvQty != null)
                                tvQty.setText("1");
                        }
                    });
                    cg.addView(chip);
                    if (c == 0)
                        chip.setChecked(true);
                }
            }

        } else if (product.hasVariants()) {
            // ── Legacy single-label mode (backwards compatible) ────────────
            layoutVariantOptions.setVisibility(View.VISIBLE);

            TextView tvGroupTitle = new TextView(this);
            tvGroupTitle.setText("Select Variant");
            tvGroupTitle.setTextColor(getResources().getColor(R.color.textPrimary, null));
            tvGroupTitle.setTextSize(16f);
            tvGroupTitle.setTypeface(null, android.graphics.Typeface.BOLD);
            tvGroupTitle.setPadding(0, 0, 0, 8);
            layoutVariantOptions.addView(tvGroupTitle);

            ChipGroup cg = new ChipGroup(this);
            cg.setSingleSelection(true);
            cg.setSelectionRequired(true);
            layoutVariantOptions.addView(cg);

            for (int i = 0; i < product.getVariants().size(); i++) {
                ProductVariant variant = product.getVariants().get(i);
                Chip chip = new Chip(this);
                chip.setText(variant.getLabel());
                chip.setCheckable(true);
                chip.setChipBackgroundColorResource(R.color.colorSurface);
                chip.setTextColor(getResources().getColor(R.color.textPrimary, null));
                final int idx = i;
                chip.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    if (isChecked) {
                        selectedVariant = product.getVariants().get(idx);
                        updatePriceStock(tvPrice, tvStock, btnAddCart, btnBuyNow);
                        selectedQuantity = 1;
                        TextView tvQty = findViewById(R.id.tv_quantity);
                        if (tvQty != null)
                            tvQty.setText("1");
                    }
                });
                cg.addView(chip);
                if (i == 0)
                    chip.setChecked(true);
            }
        } else {
            layoutVariantOptions.setVisibility(View.GONE);
            selectedVariant = null;
        }

        // === Quantity Picker ===
        MaterialButton btnMinus = findViewById(R.id.btn_qty_minus);
        MaterialButton btnPlus = findViewById(R.id.btn_qty_plus);
        TextView tvQuantity = findViewById(R.id.tv_quantity);

        selectedQuantity = 1;
        tvQuantity.setText(String.valueOf(selectedQuantity));

        btnMinus.setOnClickListener(v -> {
            if (selectedQuantity > 1) {
                selectedQuantity--;
                tvQuantity.setText(String.valueOf(selectedQuantity));
            }
        });

        btnPlus.setOnClickListener(v -> {
            int maxStock = selectedVariant != null ? selectedVariant.getStock() : product.getStock();
            if (selectedQuantity < maxStock) {
                selectedQuantity++;
                tvQuantity.setText(String.valueOf(selectedQuantity));
            } else {
                Snackbar.make(findViewById(android.R.id.content), "Maximum stock reached", Snackbar.LENGTH_SHORT).show();
            }
        });

        // Video
        if (product.getVideoUrl() != null && !product.getVideoUrl().isEmpty()) {
            btnPlayVideo.setVisibility(View.VISIBLE);
            btnVideo.setVisibility(View.VISIBLE);

            View.OnClickListener videoListener = v -> playVideo(product.getVideoUrl());
            btnPlayVideo.setOnClickListener(videoListener);
            btnVideo.setOnClickListener(videoListener);
        }

        btnBack.setOnClickListener(v -> finish());

        // Wishlist Setup
        ImageView ivWishlist = findViewById(R.id.iv_wishlist);
        String uid = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser() != null
                ? com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;

        if (uid != null && documentId != null) {
            com.lexoratech.lexorafix.data.FirestoreHelper.getInstance().getUserProfile(uid,
                    new com.lexoratech.lexorafix.data.FirestoreHelper.FirestoreCallback<com.lexoratech.lexorafix.models.User>() {
                        @Override
                        public void onSuccess(com.lexoratech.lexorafix.models.User user) {
                            if (user != null && user.getWishlist() != null) {
                                isWishlisted = user.getWishlist().contains(documentId);
                                ivWishlist.setImageResource(
                                        isWishlisted ? R.drawable.ic_heart_solid : R.drawable.ic_heart_outline);
                            }
                        }

                        @Override
                        public void onError(String message) {
                        }
                    });

            ivWishlist.setOnClickListener(v -> {
                isWishlisted = !isWishlisted; // Optimistically toggle
                ivWishlist.setImageResource(isWishlisted ? R.drawable.ic_heart_solid : R.drawable.ic_heart_outline);
                com.lexoratech.lexorafix.data.FirestoreHelper.getInstance().toggleWishlist(uid, documentId,
                        isWishlisted, new com.lexoratech.lexorafix.data.FirestoreHelper.FirestoreCallback<Void>() {
                            @Override
                            public void onSuccess(Void result) {
                            }

                            @Override
                            public void onError(String message) {
                            }
                        });
            });
        } else {
            ivWishlist.setOnClickListener(
                    v -> Snackbar.make(findViewById(android.R.id.content), "Please login to save favorites", Snackbar.LENGTH_SHORT).show());
        }

        btnAddCart.setOnClickListener(v -> addToCartIfStockAvailable(false));
        btnBuyNow.setOnClickListener(v -> addToCartIfStockAvailable(true));

        // Start listening to reviews
        attachReviewsListener();
    }

    /**
     * Build a combined label from the currently selected option values
     * (e.g. ["12V","35Ah"] → "12V / 35Ah") and find the matching ProductVariant.
     * Returns null if no match is found (combination not yet priced by admin).
     */
    private ProductVariant findVariant() {
        if (product == null || product.getVariants() == null)
            return null;
        String combinedLabel = android.text.TextUtils.join(" / ", selectedOptionValues);
        for (ProductVariant v : product.getVariants()) {
            if (combinedLabel.equals(v.getLabel()))
                return v;
        }
        return null; // Combination not stocked / priced yet
    }

    /**
     * Refresh price, stock badge, and button states from the current
     * selectedVariant.
     * Falls back to base product price/stock when no variant is selected.
     */
    private void updatePriceStock(TextView tvPrice, TextView tvStock,
            MaterialButton btnAddCart, MaterialButton btnBuyNow) {
        double price = selectedVariant != null ? selectedVariant.getPrice() : product.getPrice();
        int stock = selectedVariant != null ? selectedVariant.getStock() : product.getStock();
        tvPrice.setText(String.format(Locale.getDefault(), "LKR %.2f", price));
        if (stock > 0) {
            tvStock.setText(getString(R.string.in_stock) + " (" + stock + " available)");
            tvStock.setTextColor(getResources().getColor(R.color.colorSuccess, null));
            btnAddCart.setEnabled(true);
            btnBuyNow.setEnabled(true);
        } else {
            tvStock.setText(R.string.out_of_stock);
            tvStock.setTextColor(getResources().getColor(R.color.colorError, null));
            btnAddCart.setEnabled(false);
            btnBuyNow.setEnabled(false);
        }
    }

    private void attachReviewsListener() {
        if (documentId == null || documentId.isEmpty())
            return;

        androidx.recyclerview.widget.RecyclerView rvReviews = findViewById(R.id.rv_reviews);
        TextView tvNoReviews = findViewById(R.id.tv_no_reviews);
        android.widget.RatingBar ratingBarAvg = findViewById(R.id.rating_bar_average);
        TextView tvRatingAvg = findViewById(R.id.tv_rating_average);

        rvReviews.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this));

        FirebaseFirestore.getInstance()
                .collection("products")
                .document(documentId)
                .collection("reviews")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null)
                        return;
                    if (snapshot != null) {
                        java.util.List<com.lexoratech.lexorafix.models.Review> reviewsList = new java.util.ArrayList<>();
                        float totalRating = 0;
                        for (com.google.firebase.firestore.DocumentSnapshot doc : snapshot.getDocuments()) {
                            com.lexoratech.lexorafix.models.Review review = com.lexoratech.lexorafix.models.Review
                                    .fromMap(doc.getData());
                            if (review != null) {
                                review.setId(doc.getId());
                                reviewsList.add(review);
                                totalRating += review.getRating();
                            }
                        }

                        if (reviewsList.isEmpty()) {
                            tvNoReviews.setVisibility(View.VISIBLE);
                            rvReviews.setVisibility(View.GONE);
                            ratingBarAvg.setRating(0);
                            tvRatingAvg.setText("0.0");
                        } else {
                            tvNoReviews.setVisibility(View.GONE);
                            rvReviews.setVisibility(View.VISIBLE);

                            float average = totalRating / reviewsList.size();
                            ratingBarAvg.setRating(average);
                            tvRatingAvg.setText(String.format(Locale.getDefault(), "%.1f", average));

                            com.lexoratech.lexorafix.adapters.ReviewAdapter adapter = new com.lexoratech.lexorafix.adapters.ReviewAdapter(
                                    this, reviewsList);
                            rvReviews.setAdapter(adapter);
                        }
                    }
                });
    }

    private void addToCartIfStockAvailable(boolean isBuyNow) {
        if (product == null)
            return;

        DatabaseHelper db = DatabaseHelper.getInstance(this);
        int userId = 1; // Local DB legacy support

        int maxStock = selectedVariant != null ? selectedVariant.getStock() : product.getStock();
        double effectivePrice = selectedVariant != null ? selectedVariant.getPrice() : product.getPrice();
        String variantLabel = selectedVariant != null ? selectedVariant.getLabel() : null;

        int currentCartQty = db.getProductQuantityInCart(product.getId(), userId);
        if (currentCartQty + selectedQuantity > maxStock) {
            String msg = "Cannot add more. Only " + maxStock + " available in stock.";
            Snackbar.make(findViewById(android.R.id.content), msg, Snackbar.LENGTH_LONG).show();
            return;
        }

        CartItem cartItem = new CartItem(0, product.getId(), product.getName(),
                effectivePrice, selectedQuantity, product.getImageUrl());
        if (variantLabel != null) {
            cartItem.setVariantLabel(variantLabel);
        }
        db.addToCart(cartItem, userId);

        if (isBuyNow) {
            Intent intent = new Intent(ProductDetailActivity.this, CheckoutActivity.class);
            startActivity(intent);
        } else {
            String itemName = product.getName();
            if (variantLabel != null)
                itemName += " (" + variantLabel + ")";
            Toast.makeText(ProductDetailActivity.this, "Added to cart", Toast.LENGTH_SHORT).show();
        }
    }

    private void playVideo(String url) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(Uri.parse(url), "video/*");
            startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(this, "No video player available", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Remove Firestore listener to prevent memory leaks
        if (productListener != null) {
            productListener.remove();
            productListener = null;
        }
    }
}
