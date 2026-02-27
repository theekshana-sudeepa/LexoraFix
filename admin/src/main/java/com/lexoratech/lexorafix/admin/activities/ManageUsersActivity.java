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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_list);

        ImageView btnBack = findViewById(R.id.btn_back);
        android.widget.TextView tvTitle = findViewById(R.id.tv_title);
        RecyclerView rvList = findViewById(R.id.rv_list);

        tvTitle.setText("Manage Users");
        btnBack.setOnClickListener(v -> finish());

        rvList.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AdminUserAdapter(this, userList);
        rvList.setAdapter(adapter);

        loadUsers();
    }

    private void loadUsers() {
        FirebaseFirestore.getInstance()
                .collection("users")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    userList.clear();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Map<String, Object> data = doc.getData();
                        if (data != null) {
                            data.put("uid", doc.getId());
                            userList.add(data);
                        }
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Failed to load users: " + e.getMessage(),
                                Toast.LENGTH_LONG).show());
    }
}
