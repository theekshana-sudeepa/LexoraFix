package com.lexoratech.lexorafix.admin.activities;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.firestore.FirebaseFirestore;
import com.lexoratech.lexorafix.admin.R;

import java.util.HashMap;
import java.util.Map;

public class AddEditServiceCenterActivity extends AppCompatActivity {

    private TextInputEditText etName, etAddress, etPhone, etLatitude, etLongitude, etDescription;
    private String documentId = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_service_center);

        ImageView btnBack = findViewById(R.id.btn_back);
        TextView tvTitle = findViewById(R.id.tv_title);
        etName = findViewById(R.id.et_name);
        etAddress = findViewById(R.id.et_address);
        etPhone = findViewById(R.id.et_phone);
        etLatitude = findViewById(R.id.et_latitude);
        etLongitude = findViewById(R.id.et_longitude);
        etDescription = findViewById(R.id.et_description);
        Button btnSave = findViewById(R.id.btn_save);

        btnBack.setOnClickListener(v -> finish());

        if (getIntent().hasExtra("documentId")) {
            documentId = getIntent().getStringExtra("documentId");
            tvTitle.setText("Edit Service Center");

            HashMap<String, Object> data = (HashMap<String, Object>) getIntent().getSerializableExtra("centerData");
            if (data != null) {
                if (data.containsKey("name"))
                    etName.setText((String) data.get("name"));
                if (data.containsKey("address"))
                    etAddress.setText((String) data.get("address"));
                if (data.containsKey("phone"))
                    etPhone.setText((String) data.get("phone"));
                if (data.containsKey("description"))
                    etDescription.setText((String) data.get("description"));

                if (data.containsKey("latitude") && data.get("latitude") != null) {
                    etLatitude.setText(String.valueOf(data.get("latitude")));
                }
                if (data.containsKey("longitude") && data.get("longitude") != null) {
                    etLongitude.setText(String.valueOf(data.get("longitude")));
                }
            }
        } else {
            tvTitle.setText("Add Service Center");
        }

        btnSave.setOnClickListener(v -> saveCenter());
    }

    private void saveCenter() {
        String name = etName.getText().toString().trim();
        String address = etAddress.getText().toString().trim();
        String phone = etPhone.getText().toString().trim();
        String latStr = etLatitude.getText().toString().trim();
        String lngStr = etLongitude.getText().toString().trim();
        String desc = etDescription.getText().toString().trim();

        if (TextUtils.isEmpty(name)) {
            etName.setError("Name is required");
            return;
        }

        double latitude = 0, longitude = 0;
        try {
            if (!TextUtils.isEmpty(latStr))
                latitude = Double.parseDouble(latStr);
            if (!TextUtils.isEmpty(lngStr))
                longitude = Double.parseDouble(lngStr);
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Invalid latitude or longitude format", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSaveState(false);

        Map<String, Object> data = new HashMap<>();
        data.put("name", name);
        data.put("address", address);
        data.put("phone", phone);
        data.put("latitude", latitude);
        data.put("longitude", longitude);
        data.put("description", desc);

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        if (documentId == null) {
            // Include an incremental ID generation for legacy SQLite sync just in case
            db.collection("service_centers").get().addOnSuccessListener(querySnapshot -> {
                data.put("id", querySnapshot.size() + 1); // Simple max ID fallback
                db.collection("service_centers")
                        .add(data)
                        .addOnSuccessListener(documentReference -> {
                            Toast.makeText(this, "Center added successfully", Toast.LENGTH_SHORT).show();
                            finish();
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(this, "Failed to add center", Toast.LENGTH_SHORT).show();
                            btnSaveState(true);
                        });
            });
        } else {
            db.collection("service_centers").document(documentId)
                    .update(data)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Center updated successfully", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to update center", Toast.LENGTH_SHORT).show();
                        btnSaveState(true);
                    });
        }
    }

    private void btnSaveState(boolean enabled) {
        Button btnSave = findViewById(R.id.btn_save);
        btnSave.setEnabled(enabled);
        btnSave.setText(enabled ? "Save Center" : "Saving...");
    }
}
