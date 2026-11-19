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
        
        adapter.setOnUserActionListener(new AdminUserAdapter.OnUserActionListener() {
            @Override
            public void onDeleteUser(Map<String, Object> user, int position) {
                new androidx.appcompat.app.AlertDialog.Builder(ManageUsersActivity.this)
                        .setTitle("Delete User")
                        .setMessage("Are you sure you want to permanently delete this user and all their data (orders, bookings, notifications)? This cannot be undone.")
                        .setPositiveButton("Delete", (dialog, which) -> deleteFullUser(user, position))
                        .setNegativeButton("Cancel", null)
                        .show();
            }
        });
        
        rvList.setAdapter(adapter);

        loadUsers();
    }

    private void deleteFullUser(Map<String, Object> user, int position) {
        String uid = user.get("uid") != null ? user.get("uid").toString() : null;
        if (uid == null) {
            Toast.makeText(this, "Invalid user ID.", Toast.LENGTH_SHORT).show();
            return;
        }

        android.app.ProgressDialog progressDialog = new android.app.ProgressDialog(this);
        progressDialog.setMessage("Deleting user and associated data...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // 1. Delete matching top-level orders
        db.collection("orders").whereEqualTo("firebaseUid", uid).get().addOnCompleteListener(topOrdersTask -> {
            if (topOrdersTask.isSuccessful() && topOrdersTask.getResult() != null) {
                com.google.firebase.firestore.WriteBatch batch1 = db.batch();
                for (DocumentSnapshot doc : topOrdersTask.getResult()) {
                    batch1.delete(doc.getReference());
                }
                batch1.commit();
            }

            // 2. Delete matching top-level bookings
            db.collection("bookings").whereEqualTo("firebaseUid", uid).get().addOnCompleteListener(topBookingsTask -> {
                if (topBookingsTask.isSuccessful() && topBookingsTask.getResult() != null) {
                    com.google.firebase.firestore.WriteBatch batch2 = db.batch();
                    for (DocumentSnapshot doc : topBookingsTask.getResult()) {
                        batch2.delete(doc.getReference());
                    }
                    batch2.commit();
                }

                deleteSubcollectionsAndUser(db, uid, progressDialog, user, position);
            });
        });
    }

    private void deleteSubcollectionsAndUser(FirebaseFirestore db, String uid, android.app.ProgressDialog progressDialog, Map<String, Object> user, int position) {
        db.collection("users").document(uid).collection("notifications").get().addOnCompleteListener(t1 -> {
            if (t1.isSuccessful() && t1.getResult() != null) {
                com.google.firebase.firestore.WriteBatch batch = db.batch();
                for (DocumentSnapshot d : t1.getResult()) batch.delete(d.getReference());
                batch.commit();
            }
            
            db.collection("users").document(uid).collection("orders").get().addOnCompleteListener(t2 -> {
                if (t2.isSuccessful() && t2.getResult() != null) {
                    com.google.firebase.firestore.WriteBatch batch = db.batch();
                    for (DocumentSnapshot d : t2.getResult()) batch.delete(d.getReference());
                    batch.commit();
                }
                
                db.collection("users").document(uid).collection("bookings").get().addOnCompleteListener(t3 -> {
                    if (t3.isSuccessful() && t3.getResult() != null) {
                        com.google.firebase.firestore.WriteBatch batch = db.batch();
                        for (DocumentSnapshot d : t3.getResult()) batch.delete(d.getReference());
                        batch.commit();
                    }
                    
                    db.collection("users").document(uid).delete().addOnCompleteListener(delTask -> {
                        if (delTask.isSuccessful()) {
                            // Now delete from Firebase Authentication via Cloud Function
                            java.util.Map<String, Object> data = new java.util.HashMap<>();
                            data.put("uid", uid);
                            com.google.firebase.functions.FirebaseFunctions.getInstance()
                                    .getHttpsCallable("deleteUser")
                                    .call(data)
                                    .addOnCompleteListener(cfTask -> {
                                        progressDialog.dismiss();
                                        if (cfTask.isSuccessful()) {
                                            Toast.makeText(ManageUsersActivity.this, "User fully deleted (Firestore + Auth)", Toast.LENGTH_SHORT).show();
                                        } else {
                                            Toast.makeText(ManageUsersActivity.this, "Firestore deleted, but Auth removal failed. Re-deploy Cloud Functions.", Toast.LENGTH_LONG).show();
                                        }
                                        userList.remove(user);
                                        masterUserList.remove(user);
                                        adapter.notifyDataSetChanged();
                                        
                                        android.widget.TextView tvCount = findViewById(R.id.tv_count);
                                        if (tvCount != null) tvCount.setText(String.valueOf(userList.size()));
                                        
                                        android.widget.LinearLayout emptyState = findViewById(R.id.empty_state);
                                        if (emptyState != null) emptyState.setVisibility(userList.isEmpty() ? android.view.View.VISIBLE : android.view.View.GONE);
                                    });
                        } else {
                            progressDialog.dismiss();
                            Toast.makeText(ManageUsersActivity.this, "Failed to delete user document", Toast.LENGTH_SHORT).show();
                        }
                    });
                });
            });
        });
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
