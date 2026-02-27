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
import com.lexoratech.lexorafix.admin.adapters.AdminCategoryAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Lists all categories from Firestore with options to add, edit, and delete.
 */
public class ManageCategoriesActivity extends AppCompatActivity
        implements AdminCategoryAdapter.OnCategoryActionListener {

    private AdminCategoryAdapter adapter;
    private final List<Map<String, Object>> categoryList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_list);

        ImageView btnBack = findViewById(R.id.btn_back);
        TextView tvTitle = findViewById(R.id.tv_title);
        RecyclerView rvList = findViewById(R.id.rv_list);

        tvTitle.setText("Manage Categories");
        btnBack.setOnClickListener(v -> finish());

        rvList.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AdminCategoryAdapter(this, categoryList);
        adapter.setOnCategoryActionListener(this);
        rvList.setAdapter(adapter);

        rvList.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AdminCategoryAdapter(this, categoryList);
        adapter.setOnCategoryActionListener(this);
        rvList.setAdapter(adapter);

        // Use the animated FAB from the XML layout
        com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton fabAdd = findViewById(
                R.id.fab_add);
        if (fabAdd != null) {
            fabAdd.setVisibility(android.view.View.VISIBLE);
            fabAdd.setText("Add Category");
            fabAdd.setOnClickListener(v -> {
                Intent intent = new Intent(this, AddEditCategoryActivity.class);
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
        loadCategories();
    }

    private void loadCategories() {
        FirebaseFirestore.getInstance().collection("categories")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    categoryList.clear();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Map<String, Object> data = doc.getData();
                        if (data != null) {
                            data.put("_documentId", doc.getId());
                            categoryList.add(data);
                        }
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to load categories: " + e.getMessage(),
                        Toast.LENGTH_LONG).show());
    }

    @Override
    public void onEdit(String documentId, Map<String, Object> category) {
        Intent intent = new Intent(this, AddEditCategoryActivity.class);
        intent.putExtra("documentId", documentId);

        HashMap<String, Object> safeData = new HashMap<>();
        if (category.get("name") instanceof String)
            safeData.put("name", category.get("name"));
        if (category.get("description") instanceof String)
            safeData.put("description", category.get("description"));
        if (category.get("iconName") instanceof String)
            safeData.put("iconName", category.get("iconName"));
        if (category.get("imageUrl") instanceof String)
            safeData.put("imageUrl", category.get("imageUrl"));

        intent.putExtra("categoryData", safeData);
        startActivity(intent);
    }

    @Override
    public void onDelete(String documentId, String categoryName, String imageUrl) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Category")
                .setMessage("Are you sure you want to delete \"" + categoryName + "\"?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    FirebaseFirestore.getInstance().collection("categories")
                            .document(documentId)
                            .delete()
                            .addOnSuccessListener(aVoid -> {
                                if (imageUrl != null && !imageUrl.isEmpty()) {
                                    try {
                                        FirebaseStorage.getInstance()
                                                .getReferenceFromUrl(imageUrl)
                                                .delete();
                                    } catch (IllegalArgumentException ignored) {
                                    }
                                }
                                Toast.makeText(this, "Category deleted", Toast.LENGTH_SHORT).show();
                                loadCategories();
                            })
                            .addOnFailureListener(e -> Toast.makeText(this, "Delete failed: " + e.getMessage(),
                                    Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
