package com.lexoratech.lexorafix.admin.activities;

import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.lexoratech.lexorafix.admin.R;
import com.lexoratech.lexorafix.admin.adapters.VariantAdapter;
import com.lexoratech.lexorafix.admin.adapters.VariantOptionAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Form to add or edit a product. Supports:
 * - Up to 3 product images (Firebase Storage upload)
 * - Variant Option Types (e.g. Voltage → 12V,24V | Capacity → 35Ah,55Ah)
 * - "Generate Combinations" to auto-create all cross-product variant rows
 * - Manual "Add Variant" for products without structured options
 */
public class AddEditProductActivity extends AppCompatActivity {

    private TextInputEditText etName, etDescription;
    private TextInputLayout tilName;
    private Spinner spinnerCategory, spinnerStatus;
    private SwitchMaterial switchFeatured;

    private static final String[] PRODUCT_STATUSES = { "Active", "Out of Stock", "Discontinued" };
    private final ImageView[] ivProducts = new ImageView[3];
    private final ImageButton[] btnClears = new ImageButton[3];
    private ProgressBar progressBar;

    private final Uri[] selectedImageUris = new Uri[3];
    private final String[] existingImageUrls = new String[3];
    private int currentPickerSlot = -1;

    private String editDocumentId;
    private boolean isEditMode = false;

    private final List<String> categoryNames = new ArrayList<>();
    private final List<String> categoryDocIds = new ArrayList<>();

    // Variant Option Types — driven directly by a LinearLayout (no RecyclerView)
    // Each element stores the live data for each row
    private LinearLayout llOptionContainer;
    private final List<VariantOptionAdapter.OptionData> optionDataList = new ArrayList<>();

    // Variant Combinations
    private VariantAdapter variantAdapter;
    private final List<VariantAdapter.VariantData> variantDataList = new ArrayList<>();

    private android.widget.ScrollView scrollView;

    private final ActivityResultLauncher<String> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(), uri -> {
                if (uri != null && currentPickerSlot >= 0 && currentPickerSlot < 3) {
                    selectedImageUris[currentPickerSlot] = uri;
                    ivProducts[currentPickerSlot].setScaleType(ImageView.ScaleType.CENTER_CROP);
                    ivProducts[currentPickerSlot].setPadding(0, 0, 0, 0);
                    ivProducts[currentPickerSlot].setImageURI(uri);
                    btnClears[currentPickerSlot].setVisibility(View.VISIBLE);
                }
            });

    @Override
    @SuppressWarnings("unchecked")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_edit_product);

        // ── Toolbar ──────────────────────────────────────────────────
        ImageView btnBack = findViewById(R.id.btn_back);
        TextView tvTitle = findViewById(R.id.tv_title);

        // ── Image pickers ─────────────────────────────────────────────
        ivProducts[0] = findViewById(R.id.iv_product_1);
        ivProducts[1] = findViewById(R.id.iv_product_2);
        ivProducts[2] = findViewById(R.id.iv_product_3);
        btnClears[0] = findViewById(R.id.btn_clear_1);
        btnClears[1] = findViewById(R.id.btn_clear_2);
        btnClears[2] = findViewById(R.id.btn_clear_3);

        // ── Form fields ───────────────────────────────────────────────
        tilName = findViewById(R.id.til_name);
        etName = findViewById(R.id.et_name);
        etDescription = findViewById(R.id.et_description);
        spinnerCategory = findViewById(R.id.spinner_category);
        spinnerStatus = findViewById(R.id.spinner_status);
        switchFeatured = findViewById(R.id.switch_featured);
        progressBar = findViewById(R.id.progress_bar);
        MaterialButton btnSave = findViewById(R.id.btn_save);

        // Status spinner
        ArrayAdapter<String> statusAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, PRODUCT_STATUSES);
        statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerStatus.setAdapter(statusAdapter);

        btnBack.setOnClickListener(v -> finish());

        // Root scroll view (used to reveal newly added rows)
        scrollView = findViewById(R.id.scroll_view);

        // Image slot setup
        for (int i = 0; i < 3; i++) {
            existingImageUrls[i] = "";
            final int slot = i;
            ivProducts[i].setOnClickListener(v -> {
                currentPickerSlot = slot;
                imagePickerLauncher.launch("image/*");
            });
            btnClears[i].setOnClickListener(v -> {
                selectedImageUris[slot] = null;
                existingImageUrls[slot] = "";
                ivProducts[slot].setImageResource(android.R.drawable.ic_menu_gallery);
                ivProducts[slot].setScaleType(ImageView.ScaleType.CENTER_INSIDE);
                ivProducts[slot].setPadding(32, 32, 32, 32);
                btnClears[slot].setVisibility(View.GONE);
            });
        }

        // Edit-mode detection
        editDocumentId = getIntent().getStringExtra("documentId");
        if (editDocumentId != null && !editDocumentId.isEmpty()) {
            isEditMode = true;
            tvTitle.setText("Edit Product");
            btnSave.setText("Update Product");
        }

        loadCategories();

        // ── Variant Option Types (LinearLayout, not RecyclerView) ────────
        llOptionContainer = findViewById(R.id.rv_variant_options);

        MaterialButton btnAddOption = findViewById(R.id.btn_add_option);
        btnAddOption.setOnClickListener(v -> {
            VariantOptionAdapter.OptionData data = new VariantOptionAdapter.OptionData();
            optionDataList.add(data);
            addOptionRow(data);
            scrollToBottom();
        });

        // ── Generate Combinations button ──────────────────────────────
        MaterialButton btnGenerate = findViewById(R.id.btn_generate_combinations);
        btnGenerate.setOnClickListener(v -> generateCombinations());

        // ── Variant Combinations RecyclerView ─────────────────────────
        RecyclerView rvVariants = findViewById(R.id.rv_variants);
        rvVariants.setLayoutManager(new LinearLayoutManager(this));
        variantAdapter = new VariantAdapter(variantDataList);
        rvVariants.setAdapter(variantAdapter);

        MaterialButton btnAddVariant = findViewById(R.id.btn_add_variant);
        btnAddVariant.setOnClickListener(v -> {
            variantDataList.add(new VariantAdapter.VariantData());
            variantAdapter.notifyItemInserted(variantDataList.size() - 1);
            scrollToBottom();
        });

        btnSave.setOnClickListener(v -> saveProduct());
    }

    /**
     * Inflates one item_variant_option.xml row, binds it to the given OptionData,
     * and appends it to llOptionContainer. The row's delete button removes both
     * the view and its data entry.
     */
    private void addOptionRow(VariantOptionAdapter.OptionData data) {
        View row = LayoutInflater.from(this).inflate(R.layout.item_variant_option, llOptionContainer, false);

        TextInputEditText etName = row.findViewById(R.id.et_option_name);
        TextInputEditText etValues = row.findViewById(R.id.et_option_values);
        ImageButton btnDelete = row.findViewById(R.id.btn_delete_option);

        // Pre-fill if data already has content (used when loading Edit mode)
        etName.setText(data.name);
        etValues.setText(data.valuesRaw);

        // Keep the data object in sync as the user types
        etName.addTextChangedListener(new android.text.TextWatcher() {
            public void beforeTextChanged(CharSequence s, int st, int c, int a) {
            }

            public void onTextChanged(CharSequence s, int st, int b, int c) {
            }

            public void afterTextChanged(android.text.Editable s) {
                data.name = s.toString();
            }
        });
        etValues.addTextChangedListener(new android.text.TextWatcher() {
            public void beforeTextChanged(CharSequence s, int st, int c, int a) {
            }

            public void onTextChanged(CharSequence s, int st, int b, int c) {
            }

            public void afterTextChanged(android.text.Editable s) {
                data.valuesRaw = s.toString();
            }
        });

        btnDelete.setOnClickListener(v -> {
            optionDataList.remove(data);
            llOptionContainer.removeView(row);
        });

        llOptionContainer.addView(row);
    }

    /**
     * Scroll the root ScrollView to the very bottom, delayed one frame so the new
     * row renders first.
     */
    private void scrollToBottom() {
        if (scrollView == null)
            return;
        scrollView.post(() -> scrollView.fullScroll(android.view.View.FOCUS_DOWN));
    }

    // ─────────────────────────────────────────────────────────────────
    // Generate Combinations (Cartesian product of option values)
    // ─────────────────────────────────────────────────────────────────

    private void generateCombinations() {
        // Collect valid option groups
        List<List<String>> groups = new ArrayList<>();
        for (VariantOptionAdapter.OptionData opt : optionDataList) {
            List<String> vals = opt.getParsedValues();
            if (!vals.isEmpty() && !opt.name.trim().isEmpty()) {
                groups.add(vals);
            }
        }

        if (groups.isEmpty()) {
            Toast.makeText(this, "Add at least one option type with values first", Toast.LENGTH_SHORT).show();
            return;
        }

        // Build all label combinations from existing variants (to preserve
        // price/stock/SKU)
        Map<String, VariantAdapter.VariantData> existingMap = new HashMap<>();
        for (VariantAdapter.VariantData vd : variantDataList) {
            if (vd.label != null && !vd.label.trim().isEmpty()) {
                existingMap.put(vd.label.trim(), vd);
            }
        }

        // Cartesian product
        List<String> combinations = new ArrayList<>();
        cartesian(groups, 0, "", combinations);

        // Replace variant list, preserving existing data where labels match
        variantDataList.clear();
        for (String combo : combinations) {
            VariantAdapter.VariantData existing = existingMap.get(combo);
            if (existing != null) {
                variantDataList.add(existing);
            } else {
                VariantAdapter.VariantData vd = new VariantAdapter.VariantData();
                vd.label = combo;
                variantDataList.add(vd);
            }
        }
        variantAdapter.notifyDataSetChanged();
        Toast.makeText(this, combinations.size() + " combinations generated ✅", Toast.LENGTH_SHORT).show();
    }

    /**
     * Recursive Cartesian product. Each group contributes one segment, joined by "
     * / ".
     */
    private void cartesian(List<List<String>> groups, int depth, String current, List<String> results) {
        if (depth == groups.size()) {
            // Remove leading " / "
            results.add(current.startsWith(" / ") ? current.substring(3) : current);
            return;
        }
        for (String value : groups.get(depth)) {
            cartesian(groups, depth + 1, current + " / " + value, results);
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // Load categories from Firestore
    // ─────────────────────────────────────────────────────────────────

    private void loadCategories() {
        categoryNames.clear();
        categoryDocIds.clear();
        categoryNames.add("— Select Category —");
        categoryDocIds.add("");

        FirebaseFirestore.getInstance().collection("categories")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        String name = doc.getString("name");
                        categoryNames.add(name != null ? name : doc.getId());
                        categoryDocIds.add(doc.getId());
                    }
                    bindCategorySpinner();
                })
                .addOnFailureListener(e -> bindCategorySpinner());
    }

    private void bindCategorySpinner() {
        ArrayAdapter<String> catAdapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item, categoryNames);
        catAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(catAdapter);
        if (isEditMode)
            populateForm();
    }

    // ─────────────────────────────────────────────────────────────────
    // Populate form for Edit mode
    // ─────────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private void populateForm() {
        HashMap<String, Object> data = (HashMap<String, Object>) getIntent().getSerializableExtra("productData");
        if (data == null)
            return;

        etName.setText(getStr(data, "name"));
        etDescription.setText(getStr(data, "description"));

        // BaseModel stock is no longer used UI-side; variants hold the stock.

        Object featObj = data.get("featured");
        if (featObj instanceof Boolean)
            switchFeatured.setChecked((Boolean) featObj);

        // Images
        List<String> loadedImageUrls = (List<String>) data.get("imageUrls");
        if (loadedImageUrls != null) {
            for (int i = 0; i < loadedImageUrls.size() && i < 3; i++) {
                String url = loadedImageUrls.get(i);
                if (url != null && !url.isEmpty()) {
                    existingImageUrls[i] = url;
                    ivProducts[i].setScaleType(ImageView.ScaleType.CENTER_CROP);
                    ivProducts[i].setPadding(0, 0, 0, 0);
                    btnClears[i].setVisibility(View.VISIBLE);
                    Glide.with(this).load(url)
                            .placeholder(android.R.drawable.ic_menu_gallery)
                            .into(ivProducts[i]);
                }
            }
        }

        // Category spinner
        String catDocId = getStr(data, "categoryDocId");
        if (!catDocId.isEmpty()) {
            for (int i = 0; i < categoryDocIds.size(); i++) {
                if (categoryDocIds.get(i).equals(catDocId)) {
                    spinnerCategory.setSelection(i);
                    break;
                }
            }
        }

        // Status spinner
        String status = getStr(data, "status");
        for (int i = 0; i < PRODUCT_STATUSES.length; i++) {
            if (PRODUCT_STATUSES[i].equals(status)) {
                spinnerStatus.setSelection(i);
                break;
            }
        }

        // Load variantOptions — inflate a row for each one
        Object optionsObj = data.get("variantOptions");
        if (optionsObj instanceof List) {
            optionDataList.clear();
            llOptionContainer.removeAllViews();
            for (Object item : (List<?>) optionsObj) {
                if (item instanceof Map) {
                    Map<String, Object> om = (Map<String, Object>) item;
                    String oName = om.get("name") != null ? om.get("name").toString() : "";
                    List<String> oValues = new ArrayList<>();
                    if (om.get("values") instanceof List) {
                        for (Object v : (List<?>) om.get("values")) {
                            if (v != null)
                                oValues.add(v.toString());
                        }
                    }
                    VariantOptionAdapter.OptionData od = new VariantOptionAdapter.OptionData(oName, oValues);
                    optionDataList.add(od);
                    addOptionRow(od);
                }
            }
        }

        // Load variants (combinations)
        Object variantsObj = data.get("variants");
        if (variantsObj instanceof List) {
            variantDataList.clear();
            for (Object item : (List<?>) variantsObj) {
                if (item instanceof Map) {
                    Map<String, Object> vm = (Map<String, Object>) item;
                    String vLabel = vm.get("label") != null ? vm.get("label").toString() : "";
                    double vPrice = 0;
                    if (vm.get("price") instanceof Number)
                        vPrice = ((Number) vm.get("price")).doubleValue();
                    int vStock = 0;
                    if (vm.get("stock") instanceof Number)
                        vStock = ((Number) vm.get("stock")).intValue();
                    String vSku = vm.get("sku") != null ? vm.get("sku").toString() : "";
                    variantDataList.add(new VariantAdapter.VariantData(vLabel, vPrice, vStock, vSku));
                }
            }
            variantAdapter.notifyDataSetChanged();
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // Save
    // ─────────────────────────────────────────────────────────────────

    private void saveProduct() {
        tilName.setError(null);

        String name = getText(etName);
        String description = getText(etDescription);
        int stock = 0; // Legacy base stock set to 0. Real stock is pulled from variants.

        boolean featured = switchFeatured.isChecked();
        int catIndex = spinnerCategory.getSelectedItemPosition();
        String categoryDocId = catIndex > 0 ? categoryDocIds.get(catIndex) : "";
        String productStatus = PRODUCT_STATUSES[spinnerStatus.getSelectedItemPosition()];

        setLoading(true);
        List<String> finalUrls = new ArrayList<>();
        uploadImageSlot(0, name, description, 0.0, stock, featured, categoryDocId, catIndex, productStatus,
                finalUrls);
    }

    private void uploadImageSlot(int slotIndex, String name, String description, double price,
            int stock, boolean featured, String categoryDocId, int catIndex,
            String productStatus, List<String> finalUrls) {
        if (slotIndex >= 3) {
            saveToFirestore(name, description, price, stock, featured, categoryDocId, catIndex, finalUrls,
                    productStatus);
            return;
        }
        Uri uri = selectedImageUris[slotIndex];
        String existing = existingImageUrls[slotIndex];

        if (uri != null) {
            String fileName = "products/" + UUID.randomUUID() + ".jpg";
            StorageReference ref = FirebaseStorage.getInstance().getReference().child(fileName);
            ref.putFile(uri)
                    .addOnSuccessListener(taskSnapshot -> ref.getDownloadUrl()
                            .addOnSuccessListener(downloadUrl -> {
                                if (isEditMode && existing != null && !existing.isEmpty()) {
                                    try {
                                        FirebaseStorage.getInstance().getReferenceFromUrl(existing).delete();
                                    } catch (IllegalArgumentException ignored) {
                                    }
                                }
                                finalUrls.add(downloadUrl.toString());
                                uploadImageSlot(slotIndex + 1, name, description, price, stock, featured,
                                        categoryDocId, catIndex, productStatus, finalUrls);
                            })
                            .addOnFailureListener(e -> {
                                setLoading(false);
                                Toast.makeText(this, "Image URL failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            }))
                    .addOnFailureListener(e -> {
                        setLoading(false);
                        Toast.makeText(this, "Upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        } else if (existing != null && !existing.isEmpty()) {
            finalUrls.add(existing);
            uploadImageSlot(slotIndex + 1, name, description, price, stock, featured,
                    categoryDocId, catIndex, productStatus, finalUrls);
        } else {
            if (isEditMode && existing != null && !existing.isEmpty()) {
                try {
                    FirebaseStorage.getInstance().getReferenceFromUrl(existing).delete();
                } catch (IllegalArgumentException ignored) {
                }
            }
            uploadImageSlot(slotIndex + 1, name, description, price, stock, featured,
                    categoryDocId, catIndex, productStatus, finalUrls);
        }
    }

    private void saveToFirestore(String name, String description, double price, int stock,
            boolean featured, String categoryDocId, int catIndex,
            List<String> imageUrls, String status) {

        Map<String, Object> productData = new HashMap<>();
        productData.put("name", name);
        productData.put("description", description);
        productData.put("price", price);
        productData.put("stock", stock);
        productData.put("featured", featured);
        productData.put("status", status);
        productData.put("categoryDocId", categoryDocId);
        productData.put("categoryId", catIndex > 0 ? catIndex : 0);
        productData.put("imageUrls", imageUrls);
        productData.put("imageUrl", imageUrls != null && !imageUrls.isEmpty() ? imageUrls.get(0) : "");
        productData.put("videoUrl", "");

        // ── Variant Option Types ─────────────────────────────────────
        List<Map<String, Object>> optionMaps = new ArrayList<>();
        for (VariantOptionAdapter.OptionData od : optionDataList) {
            if (od.name == null || od.name.trim().isEmpty())
                continue;
            List<String> vals = od.getParsedValues();
            if (vals.isEmpty())
                continue;
            Map<String, Object> om = new HashMap<>();
            om.put("name", od.name.trim());
            om.put("values", vals);
            optionMaps.add(om);
        }
        productData.put("variantOptions", optionMaps);

        // ── Variant Combinations ─────────────────────────────────────
        List<Map<String, Object>> variantMaps = new ArrayList<>();
        for (VariantAdapter.VariantData vd : variantDataList) {
            if (vd.label == null || vd.label.trim().isEmpty())
                continue;
            Map<String, Object> vm = new HashMap<>();
            vm.put("label", vd.label.trim());
            double vPrice = 0;
            try {
                vPrice = Double.parseDouble(vd.price);
            } catch (Exception ignored) {
            }
            vm.put("price", vPrice);
            int vStock = 0;
            try {
                vStock = Integer.parseInt(vd.stock);
            } catch (Exception ignored) {
            }
            vm.put("stock", vStock);
            vm.put("sku", vd.sku != null ? vd.sku.trim() : "");
            variantMaps.add(vm);
        }
        productData.put("variants", variantMaps);

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        if (isEditMode) {
            db.collection("products").document(editDocumentId)
                    .update(productData)
                    .addOnSuccessListener(aVoid -> {
                        setLoading(false);
                        Toast.makeText(this, "Product updated ✅", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        setLoading(false);
                        Toast.makeText(this, "Update failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        } else {
            db.collection("products")
                    .add(productData)
                    .addOnSuccessListener(docRef -> {
                        setLoading(false);
                        Toast.makeText(this, "Product added ✅", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e -> {
                        setLoading(false);
                        Toast.makeText(this, "Add failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }

    // ─────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────

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
