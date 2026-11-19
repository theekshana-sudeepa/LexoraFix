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

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.lexoratech.lexorafix.admin.R;
import com.lexoratech.lexorafix.admin.adapters.AdminServiceCenterAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ManageServiceCentersActivity extends AppCompatActivity
        implements AdminServiceCenterAdapter.OnCenterActionListener {

    private AdminServiceCenterAdapter adapter;
    private final List<Map<String, Object>> centerList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_list);

        ImageView btnBack = findViewById(R.id.btn_back);
        TextView tvTitle = findViewById(R.id.tv_title);
        RecyclerView rvList = findViewById(R.id.rv_list);

        tvTitle.setText("Manage Service Centers");
        btnBack.setOnClickListener(v -> finish());

        rvList.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AdminServiceCenterAdapter(this, centerList);
        adapter.setOnCenterActionListener(this);
        rvList.setAdapter(adapter);

        com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton fabAdd = findViewById(
                R.id.fab_add);
        if (fabAdd != null) {
            fabAdd.setVisibility(android.view.View.VISIBLE);
            fabAdd.setText("Add Center");
            fabAdd.setOnClickListener(v -> {
                Intent intent = new Intent(this, AddEditServiceCenterActivity.class);
                startActivity(intent);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            });

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
        loadCenters();
    }

    private void loadCenters() {
        FirebaseFirestore.getInstance().collection("service_centers")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    centerList.clear();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Map<String, Object> data = doc.getData();
                        if (data != null) {
                            data.put("_documentId", doc.getId());
                            centerList.add(data);
                        }
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to load centers: " + e.getMessage(),
                        Toast.LENGTH_LONG).show());
    }

    @Override
    public void onEdit(String documentId, Map<String, Object> center) {
        Intent intent = new Intent(this, AddEditServiceCenterActivity.class);
        intent.putExtra("documentId", documentId);

        HashMap<String, Object> safeData = new HashMap<>();
        if (center.get("name") instanceof String)
            safeData.put("name", center.get("name"));
        if (center.get("address") instanceof String)
            safeData.put("address", center.get("address"));
        if (center.get("phone") instanceof String)
            safeData.put("phone", center.get("phone"));
        if (center.get("description") instanceof String)
            safeData.put("description", center.get("description"));
        if (center.get("latitude") instanceof Number)
            safeData.put("latitude", ((Number) center.get("latitude")).doubleValue());
        if (center.get("longitude") instanceof Number)
            safeData.put("longitude", ((Number) center.get("longitude")).doubleValue());

        intent.putExtra("centerData", safeData);
        startActivity(intent);
    }

    @Override
    public void onDelete(String documentId, String centerName) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Service Center")
                .setMessage("Are you sure you want to delete \"" + centerName + "\"?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    FirebaseFirestore.getInstance().collection("service_centers")
                            .document(documentId)
                            .delete()
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "Service Center deleted", Toast.LENGTH_SHORT).show();
                                loadCenters();
                            })
                            .addOnFailureListener(e -> Toast.makeText(this, "Delete failed: " + e.getMessage(),
                                    Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}
