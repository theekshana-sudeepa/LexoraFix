package com.lexoratech.lexorafix.admin.activities;

import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.lexoratech.lexorafix.admin.R;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Form to add or edit a category. Supports image picking with Firebase Storage upload.
 */
public class AddEditCategoryActivity extends AppCompatActivity {

    private TextInputEditText etName, etDescription, etIcon;
    private TextInputLayout tilName;
    private ImageView ivCategory;
    private ProgressBar progressBar;

    private Uri selectedImageUri;
    private String existingImageUrl;
    private String editDocumentId;
    private boolean isEditMode = false;

    private final ActivityResultLauncher<String> imagePickerLauncher =
            registerForActivityResult(new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null) {
                    selectedImageUri = uri;
                    ivCategory.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    ivCategory.setImageURI(uri);
                }
            });

    @Override
    @SuppressWarnings("unchecked")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_category);

        ImageView btnBack = findViewById(R.id.btn_back);
        TextView tvTitle = findViewById(R.id.tv_title);
        ivCategory = findViewById(R.id.iv_category);
        MaterialButton btnPickImage = findViewById(R.id.btn_pick_image);
        tilName = findViewById(R.id.til_name);
        etName = findViewById(R.id.et_name);
        etDescription = findViewById(R.id.et_description);
        etIcon = findViewById(R.id.et_icon);
        progressBar = findViewById(R.id.progress_bar);
        MaterialButton btnSave = findViewById(R.id.btn_save);

        btnBack.setOnClickListener(v -> finish());
        btnPickImage.setOnClickListener(v -> imagePickerLauncher.launch("image/*"));

        editDocumentId = getIntent().getStringExtra("documentId");
        if (editDocumentId != null && !editDocumentId.isEmpty()) {
            isEditMode = true;
            tvTitle.setText("Edit Category");
            btnSave.setText("Update Category");
            populateForm();
        }

        btnSave.setOnClickListener(v -> saveCategory());
    }

    @SuppressWarnings("unchecked")
    private void populateForm() {
        HashMap<String, Object> data = (HashMap<String, Object>) getIntent().getSerializableExtra("categoryData");
        if (data == null) return;

        etName.setText(getStr(data, "name"));
        etDescription.setText(getStr(data, "description"));
        etIcon.setText(getStr(data, "iconName"));

        existingImageUrl = getStr(data, "imageUrl");
        if (!existingImageUrl.isEmpty()) {
            Glide.with(this)
                    .load(existingImageUrl)
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .into(ivCategory);
        }
    }

    private void saveCategory() {
        tilName.setError(null);

        String name = getText(etName);
        String description = getText(etDescription);
        String iconName = getText(etIcon);

        if (TextUtils.isEmpty(name)) {
            tilName.setError("Required");
            return;
        }

        setLoading(true);

        if (selectedImageUri != null) {
            String fileName = "categories/" + UUID.randomUUID().toString() + ".jpg";
            StorageReference ref = FirebaseStorage.getInstance().getReference().child(fileName);

            ref.putFile(selectedImageUri)
                    .addOnSuccessListener(taskSnapshot -> ref.getDownloadUrl()
                            .addOnSuccessListener(uri -> {
                                if (isEditMode && existingImageUrl != null && !existingImageUrl.isEmpty()) {
                                    try {
                                        FirebaseStorage.getInstance()
                                                .getReferenceFromUrl(existingImageUrl).delete();
                                    } catch (IllegalArgumentException ignored) {
                                    }
                                }
                                saveToFirestore(name, description, iconName, uri.toString());
                            })
                            .addOnFailureListener(e -> {
                                setLoading(false);
                                Toast.makeText(this, "Image URL failed: " + e.getMessage(),
                                        Toast.LENGTH_SHORT).show();
                            }))
                    .addOnFailureListener(e -> {
                        setLoading(false);
                        Toast.makeText(this, "Upload failed: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    });
        } else {
            String imageUrl = existingImageUrl != null ? existingImageUrl : "";
            saveToFirestore(name, description, iconName, imageUrl);
        }
    }

    private void saveToFirestore(String name, String description, String iconName, String imageUrl) {
        Map<String, Object> categoryData = new HashMap<>();
        categoryData.put("name", name);
        categoryData.put("description", description);
        categoryData.put("iconName", iconName);
        categoryData.put("imageUrl", imageUrl);

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        if (isEditMode) {
            db.collection("categories").document(editDocumentId)
                    .update(categoryData)
                    .addOnSuccessListener(aVoid -> {
                        setLoading(false);
                        Toast.makeText(this, "Category updated ✅", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        setLoading(false);
                        Toast.makeText(this, "Update failed: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    });
        } else {
            db.collection("categories")
                    .add(categoryData)
                    .addOnSuccessListener(docRef -> {
                        setLoading(false);
                        Toast.makeText(this, "Category added ✅", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        setLoading(false);
                        Toast.makeText(this, "Add failed: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show();
                    });
        }
    }

    private void setLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        findViewById(R.id.btn_save).setEnabled(!loading);
    }

    private String getText(TextInputEditText et) {
        return et.getText() != null ? et.getText().toString().trim() : "";
    }

    private String getStr(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val != null ? val.toString() : "";
    }
}
