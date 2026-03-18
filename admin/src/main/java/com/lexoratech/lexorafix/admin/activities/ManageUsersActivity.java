package com.lexoratech.lexorafix.admin.activities;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.lexoratech.lexorafix.admin.R;
import com.lexoratech.lexorafix.admin.adapters.AdminUserAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ManageUsersActivity extends AppCompatActivity {

    private AdminUserAdapter adapter;
    private List<Map<String, Object>> userList = new ArrayList<>();
    private final List<Map<String, Object>> masterUserList = new ArrayList<>();
    private String currentFilter = "All Users";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_list);

        ImageView btnBack = findViewById(R.id.btn_back);
        android.widget.TextView tvTitle = findViewById(R.id.tv_title);
        RecyclerView rvList = findViewById(R.id.rv_list);
        ImageView btnFilter = findViewById(R.id.btn_filter);

        tvTitle.setText("Manage Users");
        btnBack.setOnClickListener(v -> finish());
        btnFilter.setOnClickListener(v -> showFilterMenu(btnFilter));

        rvList.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AdminUserAdapter(this, userList);
        rvList.setAdapter(adapter);

        loadUsers();
    }

    private void showFilterMenu(android.view.View view) {
        androidx.appcompat.widget.PopupMenu popup = new androidx.appcompat.widget.PopupMenu(this, view);
        popup.getMenu().add("All Users");
        popup.getMenu().add("Administrators");
        popup.getMenu().add("Customers");
        popup.getMenu().add("Workers");
        popup.getMenu().add("Pending Workers");

        popup.setOnMenuItemClickListener(item -> {
            currentFilter = item.getTitle().toString();
            applyFilter();
            return true;
        });
        popup.show();
    }

    private void loadUsers() {
        FirebaseFirestore.getInstance()
                .collection("users")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    masterUserList.clear();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Map<String, Object> data = doc.getData();
                        if (data != null) {
                            data.put("uid", doc.getId());
                            masterUserList.add(data);
                        }
                    }
                    applyFilter();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load users: " + e.getMessage(),
                                Toast.LENGTH_LONG).show());
    }

    private void applyFilter() {
        userList.clear();
        for (Map<String, Object> user : masterUserList) {
            boolean matches = true;
            String role = user.get("role") != null ? user.get("role").toString() : "";
            
            if ("Administrators".equals(currentFilter)) {
                matches = "admin".equals(role);
            } else if ("Customers".equals(currentFilter)) {
                matches = "customer".equals(role) || (!"admin".equals(role) && !"worker".equals(role));
            } else if ("Workers".equals(currentFilter)) {
                matches = "worker".equals(role);
            } else if ("Pending Workers".equals(currentFilter)) {
                matches = "worker".equals(role) && !Boolean.TRUE.equals(user.get("adminApproved"));
            }
            
            if (matches) {
                userList.add(user);
            }
        }
        
        android.widget.TextView tvCount = findViewById(R.id.tv_count);
        if (tvCount != null) {
            tvCount.setText(String.valueOf(userList.size()));
            tvCount.setVisibility(android.view.View.VISIBLE);
        }
        
        android.widget.LinearLayout emptyState = findViewById(R.id.empty_state);
        if (emptyState != null) {
            emptyState.setVisibility(userList.isEmpty() ? android.view.View.VISIBLE : android.view.View.GONE);
        }
        
        adapter.notifyDataSetChanged();
    }
}
