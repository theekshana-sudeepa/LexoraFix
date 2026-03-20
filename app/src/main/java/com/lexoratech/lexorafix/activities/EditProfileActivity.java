package com.lexoratech.lexorafix.activities;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.lexoratech.lexorafix.R;
import com.lexoratech.lexorafix.data.FirestoreHelper;
import com.lexoratech.lexorafix.data.LocationData;
import com.lexoratech.lexorafix.data.SharedPreferencesManager;
import com.lexoratech.lexorafix.data.StorageHelper;
import com.lexoratech.lexorafix.models.User;

import java.util.HashMap;
import java.util.Map;

/**
 * Allows the user to edit their profile: name, phone, and structured address.
 * Email is read-only (tied to Firebase Auth).
 */
public class EditProfileActivity extends AppCompatActivity {

    private TextInputEditText etName, etPhone, etAddress, etAddress2, etCity, etPostal;
    private TextInputLayout tilName, tilPhone, tilAddress, tilProvince, tilDistrict;
    private AutoCompleteTextView actProvince, actDistrict;
    private MaterialButton btnSave;
    private String firebaseUid;

    private ImageView ivProfilePhoto;
    private android.net.Uri selectedImageUri = null;
    private String existingPhotoUrl = null;

    private final androidx.activity.result.ActivityResultLauncher<String> imagePickerLauncher = registerForActivityResult(
            new androidx.activity.result.contract.ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    startCrop(uri);
                }
            });

    private final androidx.activity.result.ActivityResultLauncher<android.content.Intent> cropLauncher = registerForActivityResult(
            new androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    android.net.Uri resultUri = com.yalantis.ucrop.UCrop.getOutput(result.getData());
                    if (resultUri != null) {
                        selectedImageUri = resultUri;
                        ivProfilePhoto.setPadding(0, 0, 0, 0);
                        ivProfilePhoto.setBackground(null);
                        ivProfilePhoto.setImageTintList(null);
                        Glide.with(this).load(resultUri).centerCrop().into(ivProfilePhoto);
                    }
                } else if (result.getResultCode() == com.yalantis.ucrop.UCrop.RESULT_ERROR
                        && result.getData() != null) {
                    Throwable cropError = com.yalantis.ucrop.UCrop.getError(result.getData());
                    if (cropError != null) {
                        Toast.makeText(this, "Crop error: " + cropError.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            });

    private void startCrop(android.net.Uri sourceUri) {
        String destinationFileName = "cropped_profile_" + System.currentTimeMillis() + ".jpg";
        android.net.Uri destinationUri = android.net.Uri.fromFile(new java.io.File(getCacheDir(), destinationFileName));

        com.yalantis.ucrop.UCrop uCrop = com.yalantis.ucrop.UCrop.of(sourceUri, destinationUri)
                .withAspectRatio(1, 1)
                .withMaxResultSize(800, 800);

        com.yalantis.ucrop.UCrop.Options options = new com.yalantis.ucrop.UCrop.Options();
        options.setCircleDimmedLayer(true);
        options.setCompressionFormat(android.graphics.Bitmap.CompressFormat.JPEG);
        options.setCompressionQuality(90);
        options.setHideBottomControls(false);
        options.setFreeStyleCropEnabled(false);

        try {
            options.setToolbarColor(androidx.core.content.ContextCompat.getColor(this, R.color.colorPrimary));
            options.setStatusBarColor(androidx.core.content.ContextCompat.getColor(this, R.color.colorPrimaryDark));
            options.setActiveControlsWidgetColor(
                    androidx.core.content.ContextCompat.getColor(this, R.color.colorPrimary));
        } catch (Exception e) {
            // Ignore color exceptions if resources missing
        }

        uCrop.withOptions(options);
        cropLauncher.launch(uCrop.getIntent(this));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        ImageView btnBack = findViewById(R.id.btn_back);
        TextView tvToolbarTitle = findViewById(R.id.tv_toolbar_title);
        if (tvToolbarTitle != null)
            tvToolbarTitle.setText("Edit Profile");
        etName = findViewById(R.id.et_name);
        etPhone = findViewById(R.id.et_phone);
        etAddress = findViewById(R.id.et_address);
        etAddress2 = findViewById(R.id.et_address2);
        etCity = findViewById(R.id.et_city);
        etPostal = findViewById(R.id.et_postal);
        tilName = findViewById(R.id.til_name);
        tilPhone = findViewById(R.id.til_phone);
        tilAddress = findViewById(R.id.til_address);
        tilProvince = findViewById(R.id.til_province);
        tilDistrict = findViewById(R.id.til_district);
        actProvince = findViewById(R.id.act_province);
        actDistrict = findViewById(R.id.act_district);
        btnSave = findViewById(R.id.btn_save);
        ivProfilePhoto = findViewById(R.id.iv_profile_photo);
        android.view.View tvChangePhoto = findViewById(R.id.tv_change_photo);

        android.view.View.OnClickListener pickImageListener = v -> imagePickerLauncher.launch("image/*");
        ivProfilePhoto.setOnClickListener(pickImageListener);
        tvChangePhoto.setOnClickListener(pickImageListener);

        btnBack.setOnClickListener(v -> finish());

        // ── Province / District cascading dropdowns ─────────────────────
        setupProvinceDistrictDropdowns();

        SharedPreferencesManager prefs = SharedPreferencesManager.getInstance(this);
        firebaseUid = prefs.getFirebaseUid();

        // Pre-fill from SharedPreferences as fast fallback
        etName.setText(prefs.getUserName());

        // Load latest from Firestore
        if (firebaseUid != null && !firebaseUid.isEmpty()) {
            FirestoreHelper.getInstance().getUserProfile(firebaseUid,
                    new FirestoreHelper.FirestoreCallback<User>() {
                        @Override
                        public void onSuccess(User user) {
                            if (isFinishing())
                                return;
                            if (user.getName() != null)
                                etName.setText(user.getName());
                            if (user.getPhone() != null)
                                etPhone.setText(user.getPhone());
                            if (user.getAddress() != null)
                                etAddress.setText(user.getAddress());
                            if (user.getAddressLine2() != null)
                                etAddress2.setText(user.getAddressLine2());

                            // Pre-fill province → then district
                            if (user.getProvince() != null && !user.getProvince().isEmpty()) {
                                actProvince.setText(user.getProvince(), false);
                                populateDistrictDropdown(user.getProvince());
                                if (user.getDistrict() != null && !user.getDistrict().isEmpty()) {
                                    actDistrict.setText(user.getDistrict(), false);
                                }
                            }

                            if (user.getCity() != null)
                                etCity.setText(user.getCity());
                            if (user.getPostalCode() != null)
                                etPostal.setText(user.getPostalCode());

                            existingPhotoUrl = user.getProfileImageUrl();
                            if (existingPhotoUrl != null && !existingPhotoUrl.isEmpty()) {
                                ivProfilePhoto.setPadding(0, 0, 0, 0);
                                ivProfilePhoto.setBackground(null);
                                ivProfilePhoto.setImageTintList(null);
                                Glide.with(EditProfileActivity.this)
                                        .load(existingPhotoUrl)
                                        .centerCrop()
                                        .into(ivProfilePhoto);
                            }
                        }

                        @Override
                        public void onError(String message) {
                            // Use whatever's in SharedPrefs
                        }
                    });
        }

        btnSave.setOnClickListener(v -> saveProfile());
    }

    /** Set up the province dropdown and wire cascading district logic. */
    private void setupProvinceDistrictDropdowns() {
        ArrayAdapter<String> provinceAdapter = new ArrayAdapter<>(
                this, R.layout.item_dropdown, LocationData.getProvinces());
        actProvince.setAdapter(provinceAdapter);

        actProvince.setOnItemClickListener((parent, view, position, id) -> {
            String selectedProvince = (String) parent.getItemAtPosition(position);
            populateDistrictDropdown(selectedProvince);
            actDistrict.setText("", false); // reset district on province change
        });
    }

    /** Populate district dropdown for the given province. */
    private void populateDistrictDropdown(String province) {
        String[] districts = LocationData.getDistricts(province);
        ArrayAdapter<String> districtAdapter = new ArrayAdapter<>(
                this, R.layout.item_dropdown, districts);
        actDistrict.setAdapter(districtAdapter);
    }

    private void saveProfile() {
        tilName.setError(null);
        tilPhone.setError(null);
        tilAddress.setError(null);

        String name = etName.getText() != null ? etName.getText().toString().trim() : "";
        String phone = etPhone.getText() != null ? etPhone.getText().toString().trim() : "";
        String address = etAddress.getText() != null ? etAddress.getText().toString().trim() : "";
        String address2 = etAddress2.getText() != null ? etAddress2.getText().toString().trim() : "";
        String province = actProvince.getText() != null ? actProvince.getText().toString().trim() : "";
        String district = actDistrict.getText() != null ? actDistrict.getText().toString().trim() : "";
        String city = etCity.getText() != null ? etCity.getText().toString().trim() : "";
        String postal = etPostal.getText() != null ? etPostal.getText().toString().trim() : "";

        if (TextUtils.isEmpty(name)) {
            tilName.setError("Name is required");
            return;
        }
        if (TextUtils.isEmpty(phone)) {
            tilPhone.setError("Phone number is required");
            return;
        }
        if (!phone.matches("^(\\+94|0)\\d{9}$")) {
            tilPhone.setError("Enter a valid Sri Lankan number (e.g. 0771234567 or +94771234567)");
            return;
        }
        if (TextUtils.isEmpty(address)) {
            tilAddress.setError("Address is required");
            return;
        }
        if (TextUtils.isEmpty(province)) {
            tilProvince.setError("Province is required");
            return;
        }
        if (TextUtils.isEmpty(district)) {
            tilDistrict.setError("District is required");
            return;
        }

        if (firebaseUid == null || firebaseUid.isEmpty()) {
            Toast.makeText(this, "Please log in first", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSave.setEnabled(false);
        btnSave.setText("Saving...");

        if (selectedImageUri != null) {
            btnSave.setText("Uploading photo...");
            StorageHelper.getInstance().uploadProfileImage(selectedImageUri, firebaseUid,
                    new StorageHelper.StorageCallback() {
                        @Override
                        public void onSuccess(String downloadUrl) {
                            executeFirestoreUpdate(name, phone, address, address2, province, district, city, postal,
                                    downloadUrl);
                        }

                        @Override
                        public void onError(String message) {
                            Toast.makeText(EditProfileActivity.this, "Image upload failed: " + message,
                                    Toast.LENGTH_SHORT).show();
                            btnSave.setEnabled(true);
                            btnSave.setText("Save Changes");
                        }
                    });
        } else {
            executeFirestoreUpdate(name, phone, address, address2, province, district, city, postal, existingPhotoUrl);
        }
    }

    private void executeFirestoreUpdate(String name, String phone,
            String address, String address2, String province, String district,
            String city, String postal, String photoUrl) {
        btnSave.setText("Saving profile...");
        Map<String, Object> updates = new HashMap<>();
        updates.put("name", name);
        updates.put("phone", phone);
        updates.put("address", address);
        updates.put("addressLine2", address2);
        updates.put("province", province);
        updates.put("district", district);
        updates.put("city", city);
        updates.put("postalCode", postal);
        if (photoUrl != null) {
            updates.put("profileImageUrl", photoUrl);
        }

        FirestoreHelper.getInstance().updateUserProfile(firebaseUid, updates,
                new FirestoreHelper.FirestoreCallback<Void>() {
                    @Override
                    public void onSuccess(Void result) {
                        if (isFinishing())
                            return;
                        // Update local prefs
                        SharedPreferencesManager prefs = SharedPreferencesManager.getInstance(EditProfileActivity.this);
                        prefs.saveUserName(name);
                        prefs.saveUserPhone(phone);
                        prefs.saveUserAddress(address);

                        // Send Security Email Notification
                        com.google.firebase.auth.FirebaseUser fbUser = com.google.firebase.auth.FirebaseAuth
                                .getInstance().getCurrentUser();
                        if (fbUser != null && fbUser.getEmail() != null) {
                            com.lexoratech.lexorafix.network.EmailHelper.getInstance()
                                    .sendProfileUpdateEmail(fbUser.getEmail(), name);
                        }

                        Toast.makeText(EditProfileActivity.this, "Profile updated", Toast.LENGTH_SHORT).show();
                        finish();
                    }

                    @Override
                    public void onError(String message) {
                        if (isFinishing())
                            return;
                        btnSave.setEnabled(true);
                        btnSave.setText("Save Changes");
                        Toast.makeText(EditProfileActivity.this,
                                "Update failed: " + message, Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
