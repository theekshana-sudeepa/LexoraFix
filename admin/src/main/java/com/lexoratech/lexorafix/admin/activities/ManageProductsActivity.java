package com.lexoratech.lexorafix.admin.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.lexoratech.lexorafix.admin.R;
import com.lexoratech.lexorafix.admin.adapters.AdminProductAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Lists all products from Firestore with options to add, edit, and delete.
 */
public class ManageProductsActivity extends AppCompatActivity
        implements AdminProductAdapter.OnProductActionListener {

    private AdminProductAdapter adapter;
    private final List<Map<String, Object>> productList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_list);

        ImageView btnBack = findViewById(R.id.btn_back);
        TextView tvTitle = findViewById(R.id.tv_title);
        RecyclerView rvList = findViewById(R.id.rv_list);

        tvTitle.setText("Manage Products");
        btnBack.setOnClickListener(v -> finish());

        rvList.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AdminProductAdapter(this, productList);
        adapter.setOnProductActionListener(this);
        rvList.setAdapter(adapter);

        rvList.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AdminProductAdapter(this, productList);
        adapter.setOnProductActionListener(this);
        rvList.setAdapter(adapter);

        // Use the animated FAB from the XML layout
        com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton fabAdd = findViewById(
                R.id.fab_add);
        if (fabAdd != null) {
            fabAdd.setVisibility(android.view.View.VISIBLE);
            fabAdd.setText("Add Product");
            fabAdd.setOnClickListener(v -> {
                Intent intent = new Intent(this, AddEditProductActivity.class);
                startActivity(intent);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            });

            // Hide FAB on scroll down, show on scroll up
            rvList.addOnScrollListener(new RecyclerView.OnScrollListener() {
                @Override
                public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                    if (dy > 0 && fabAdd.isExtended()) {
                        fabAdd.shrink();
                    } else if (dy < 0 && !fabAdd.isExtended()) {
                        fabAdd.extend();
                    }
                }
            });
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadProducts();
    }

    private void loadProducts() {
        FirebaseFirestore.getInstance().collection("products")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    productList.clear();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Map<String, Object> data = doc.getData();
                        if (data != null) {
                            data.put("_documentId", doc.getId());
                            productList.add(data);
                        }
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to load products: " + e.getMessage(),
                        Toast.LENGTH_LONG).show());
    }

    @Override
    public void onEdit(String documentId, Map<String, Object> product) {
        Intent intent = new Intent(this, AddEditProductActivity.class);
        intent.putExtra("documentId", documentId);

        // Sanitize product data for Intent (remove non-serializable Firestore types
        // like Timestamp)
        HashMap<String, Object> safeData = new HashMap<>();

        if (product.get("name") instanceof String)
            safeData.put("name", product.get("name"));
        if (product.get("description") instanceof String)
            safeData.put("description", product.get("description"));
        if (product.get("price") instanceof Number)
            safeData.put("price", ((Number) product.get("price")).doubleValue());
        if (product.get("stock") instanceof Number)
            safeData.put("stock", ((Number) product.get("stock")).intValue());
        if (product.get("featured") instanceof Boolean)
            safeData.put("featured", product.get("featured"));
        if (product.get("status") instanceof String)
            safeData.put("status", product.get("status"));
        if (product.get("categoryDocId") instanceof String)
            safeData.put("categoryDocId", product.get("categoryDocId"));
        if (product.get("imageUrls") instanceof List) {
            safeData.put("imageUrls", new ArrayList<>((List<String>) product.get("imageUrls")));
        }

        // ── Pass variant option types (e.g. Voltage → [12V, 24V]) ──────────────
        if (product.get("variantOptions") instanceof List) {
            ArrayList<HashMap<String, Object>> safeOptions = new ArrayList<>();
            for (Object item : (List<?>) product.get("variantOptions")) {
                if (item instanceof Map) {
                    Map<?, ?> rawOpt = (Map<?, ?>) item;
                    HashMap<String, Object> opt = new HashMap<>();
                    if (rawOpt.get("name") != null)
                        opt.put("name", rawOpt.get("name").toString());
                    if (rawOpt.get("values") instanceof List) {
                        ArrayList<String> vals = new ArrayList<>();
                        for (Object v : (List<?>) rawOpt.get("values")) {
                            if (v != null)
                                vals.add(v.toString());
                        }
                        opt.put("values", vals);
                    }
                    safeOptions.add(opt);
                }
            }
            safeData.put("variantOptions", safeOptions);
        }

        // ── Pass variant combinations (label + price + stock + SKU) ────────────
        if (product.get("variants") instanceof List) {
            ArrayList<HashMap<String, Object>> safeVariants = new ArrayList<>();
            for (Object item : (List<?>) product.get("variants")) {
                if (item instanceof Map) {
                    Map<?, ?> rawVar = (Map<?, ?>) item;
                    HashMap<String, Object> variant = new HashMap<>();
                    if (rawVar.get("label") != null)
                        variant.put("label", rawVar.get("label").toString());
                    if (rawVar.get("price") instanceof Number)
                        variant.put("price", ((Number) rawVar.get("price")).doubleValue());
                    if (rawVar.get("stock") instanceof Number)
                        variant.put("stock", ((Number) rawVar.get("stock")).intValue());
                    if (rawVar.get("sku") != null)
                        variant.put("sku", rawVar.get("sku").toString());
                    safeVariants.add(variant);
                }
            }
            safeData.put("variants", safeVariants);
        }

        intent.putExtra("productData", safeData);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    @Override
    public void onDelete(String documentId, String productName, String imageUrl) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Product")
                .setMessage("Are you sure you want to delete \"" + productName + "\"?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    FirebaseFirestore.getInstance().collection("products")
                            .document(documentId)
                            .delete()
                            .addOnSuccessListener(aVoid -> {
                                // Also delete image from storage
                                if (imageUrl != null && !imageUrl.isEmpty()) {
                                    try {
                                        FirebaseStorage.getInstance()
                                                .getReferenceFromUrl(imageUrl)
                                                .delete();
                                    } catch (IllegalArgumentException ignored) {
                                    }
                                }
                                Toast.makeText(this, "Product deleted", Toast.LENGTH_SHORT).show();
                                loadProducts();
                            })
                            .addOnFailureListener(e -> Toast.makeText(this, "Delete failed: " + e.getMessage(),
                                    Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    public void onToggleStatus(String documentId, boolean currentlyActive) {
        String newStatus = currentlyActive ? "inactive" : "active";
        Map<String, Object> update = new HashMap<>();
        update.put("status", newStatus);
        FirebaseFirestore.getInstance().collection("products")
                .document(documentId)
                .update(update)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Status: " + newStatus, Toast.LENGTH_SHORT).show();
                    loadProducts();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Update failed: " + e.getMessage(),
                        Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onViewReviews(String documentId, String productName) {
        Intent intent = new Intent(this, AdminProductReviewsActivity.class);
        intent.putExtra("productDocId", documentId);
        intent.putExtra("productName", productName);
        startActivity(intent);
    }
}
