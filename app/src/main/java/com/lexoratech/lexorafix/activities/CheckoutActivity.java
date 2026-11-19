package com.lexoratech.lexorafix.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.lexoratech.lexorafix.BuildConfig;
import com.lexoratech.lexorafix.R;
import com.lexoratech.lexorafix.adapters.CartAdapter;
import com.lexoratech.lexorafix.data.DatabaseHelper;
import com.lexoratech.lexorafix.data.FirebaseAuthHelper;
import com.lexoratech.lexorafix.data.FirestoreHelper;
import com.lexoratech.lexorafix.data.LocationData;
import com.lexoratech.lexorafix.data.SharedPreferencesManager;
import com.lexoratech.lexorafix.models.CartItem;
import com.lexoratech.lexorafix.network.EmailHelper;
import com.lexoratech.lexorafix.notifications.NotificationHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import lk.payhere.androidsdk.PHConfigs;
import lk.payhere.androidsdk.PHConstants;
import lk.payhere.androidsdk.PHMainActivity;
import lk.payhere.androidsdk.PHResponse;
import lk.payhere.androidsdk.model.InitRequest;
import lk.payhere.androidsdk.model.StatusResponse;

public class CheckoutActivity extends AppCompatActivity {

    private static final String TAG = "CheckoutActivity";
    private static final int PAYHERE_REQUEST = 11010;

    private List<CartItem> cartItems;
    private DatabaseHelper db;
    private SharedPreferencesManager prefs;
    private double subtotal = 0;
    private double deliveryFee = 0;
    private double grandTotal = 0;
    private String pendingAddress = "";
    private android.widget.FrameLayout paymentOverlay;
    private MaterialButton btnPlaceOrder;

    // Delivery charge config (fetched from Firestore)
    private java.util.Map<String, Double> provinceRates = new java.util.HashMap<>();
    private double freeDeliveryThreshold = 5000.0;
    private double defaultDeliveryRate = 500.0;
    private boolean deliveryRatesLoaded = false;

    // Delivery UI views
    private TextView tvSubtotal, tvDeliveryFee, tvDeliveryProvince, tvGrandTotal, tvFreeDeliveryMsg;
    private android.widget.LinearLayout layoutFreeDeliveryBadge;

    // Structured shipping fields
    private com.google.android.material.textfield.TextInputEditText etAddress, etAddress2, etCity, etPostal;
    private AutoCompleteTextView actProvince, actDistrict;
    private com.google.android.material.textfield.TextInputEditText etBillingAddr, etBillingAddr2, etBillingCity,
            etBillingPostal;
    private AutoCompleteTextView actBillingProvince, actBillingDistrict;
    private com.google.android.material.checkbox.MaterialCheckBox cbBillingSame;
    private android.widget.LinearLayout layoutBilling;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_checkout);

        db = DatabaseHelper.getInstance(this);
        prefs = SharedPreferencesManager.getInstance(this);

        ImageView btnBack = findViewById(R.id.btn_back);
        TextView tvToolbarTitle = findViewById(R.id.tv_toolbar_title);
        if (tvToolbarTitle != null)
            tvToolbarTitle.setText("Checkout");
        MaterialButton btnSavedAddress = findViewById(R.id.btn_saved_address);
        RecyclerView rvItems = findViewById(R.id.rv_items);
        TextView tvTotal = findViewById(R.id.tv_total_bottom);
        btnPlaceOrder = findViewById(R.id.btn_place_order);
        paymentOverlay = findViewById(R.id.payment_overlay);

        // Price breakdown views
        tvSubtotal = findViewById(R.id.tv_subtotal);
        tvDeliveryFee = findViewById(R.id.tv_delivery_fee);
        tvDeliveryProvince = findViewById(R.id.tv_delivery_province);
        tvGrandTotal = findViewById(R.id.tv_grand_total);
        tvFreeDeliveryMsg = findViewById(R.id.tv_free_delivery_msg);
        layoutFreeDeliveryBadge = findViewById(R.id.layout_free_delivery_badge);

        btnBack.setOnClickListener(v -> finish());

        // ── Structured shipping fields ──────────────────────────────────
        etAddress = findViewById(R.id.et_address);
        etAddress2 = findViewById(R.id.et_address2);
        actProvince = findViewById(R.id.act_province);
        actDistrict = findViewById(R.id.act_district);
        etCity = findViewById(R.id.et_city);
        etPostal = findViewById(R.id.et_postal);
        etBillingAddr = findViewById(R.id.et_billing_address);
        etBillingAddr2 = findViewById(R.id.et_billing_address2);
        actBillingProvince = findViewById(R.id.act_billing_province);
        actBillingDistrict = findViewById(R.id.act_billing_district);
        etBillingCity = findViewById(R.id.et_billing_city);
        etBillingPostal = findViewById(R.id.et_billing_postal);
        cbBillingSame = findViewById(R.id.cb_billing_same);
        layoutBilling = findViewById(R.id.layout_billing);

        // ── Setup province/district cascading dropdowns ─────────────────
        setupProvinceDistrictDropdowns(actProvince, actDistrict, true);
        setupProvinceDistrictDropdowns(actBillingProvince, actBillingDistrict, false);

        cbBillingSame.setOnCheckedChangeListener((btn, isChecked) -> layoutBilling
                .setVisibility(isChecked ? android.view.View.GONE : android.view.View.VISIBLE));

        // Initial load from Preferences as a fast fallback
        String fallbackAddress = "";
        String lastAddress = prefs.getLastAddress();
        String profileAddress = prefs.getUserAddress();

        if (lastAddress != null && !lastAddress.trim().isEmpty()) {
            fallbackAddress = lastAddress;
        } else if (profileAddress != null && !profileAddress.trim().isEmpty()) {
            fallbackAddress = profileAddress;
        }

        if (!fallbackAddress.isEmpty()) {
            etAddress.setText(fallbackAddress);
            btnSavedAddress.setVisibility(android.view.View.VISIBLE);
            final String fAddress = fallbackAddress;
            btnSavedAddress.setOnClickListener(v -> etAddress.setText(fAddress));
        } else {
            btnSavedAddress.setVisibility(android.view.View.GONE);
        }

        // Fetch latest address from Firestore database
        String profileUid = prefs.getFirebaseUid();
        if (profileUid != null && !profileUid.isEmpty()) {
            FirestoreHelper.getInstance().getUserProfile(profileUid,
                    new FirestoreHelper.FirestoreCallback<com.lexoratech.lexorafix.models.User>() {
                        @Override
                        public void onSuccess(com.lexoratech.lexorafix.models.User user) {
                            if (user == null)
                                return;
                            runOnUiThread(() -> {
                                String addr1 = user.getAddress() != null ? user.getAddress() : "";
                                String addr2 = user.getAddressLine2() != null ? user.getAddressLine2() : "";
                                String provinceStr = user.getProvince() != null ? user.getProvince() : "";
                                String districtStr = user.getDistrict() != null ? user.getDistrict() : "";
                                String cityStr = user.getCity() != null ? user.getCity() : "";
                                String zip = user.getPostalCode() != null ? user.getPostalCode() : "";

                                // Populate shipping fields from saved profile
                                if (!addr1.isEmpty())
                                    etAddress.setText(addr1);
                                if (!addr2.isEmpty())
                                    etAddress2.setText(addr2);
                                if (!provinceStr.isEmpty()) {
                                    actProvince.setText(provinceStr, false);
                                    populateDistrictDropdown(actDistrict, provinceStr);
                                    if (!districtStr.isEmpty())
                                        actDistrict.setText(districtStr, false);
                                }
                                if (!cityStr.isEmpty())
                                    etCity.setText(cityStr);
                                if (!zip.isEmpty())
                                    etPostal.setText(zip);

                                prefs.saveUserAddress(addr1);

                                // "Use saved address" now restores all fields
                                if (!addr1.isEmpty()) {
                                    btnSavedAddress.setVisibility(android.view.View.VISIBLE);
                                    btnSavedAddress.setOnClickListener(v -> {
                                        etAddress.setText(addr1);
                                        etAddress2.setText(addr2);
                                        if (!provinceStr.isEmpty()) {
                                            actProvince.setText(provinceStr, false);
                                            populateDistrictDropdown(actDistrict, provinceStr);
                                            if (!districtStr.isEmpty())
                                                actDistrict.setText(districtStr, false);
                                        }
                                        etCity.setText(cityStr);
                                        etPostal.setText(zip);
                                    });
                                }
                            });
                        }

                        @Override
                        public void onError(String message) {
                            Log.e(TAG, "Failed to load address from Firestore: " + message);
                        }
                    });
        }

        // Load cart items (still using local DB with userId=1 for legacy support)
        int userId = 1;
        cartItems = db.getCartItems(userId);

        rvItems.setLayoutManager(new LinearLayoutManager(this));
        CartAdapter adapter = new CartAdapter(this, cartItems);

        adapter.setOnCartActionListener(new CartAdapter.OnCartActionListener() {
            @Override
            public void onQuantityChanged(CartItem item, int newQuantity) {
                if (newQuantity > item.getQuantity()) {
                                        // Increase requested: Check specific variant stock first
                                        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                                                .collection("products")
                                                .whereEqualTo("name", item.getProductName())
                                                .limit(1)
                                                .get()
                                                .addOnSuccessListener(snapshot -> {
                                                    if (!snapshot.isEmpty()) {
                                                        com.google.firebase.firestore.DocumentSnapshot doc = snapshot.getDocuments().get(0);
                                                        int stock = 0;
                                                        
                                                        // Check variants array for exact label
                                                        java.util.List<java.util.Map<String, Object>> variants = 
                                                            (java.util.List<java.util.Map<String, Object>>) doc.get("variants");
                                                        boolean foundVariant = false;
                                                                
                                                        if (variants != null) {
                                                            for (java.util.Map<String, Object> var : variants) {
                                                                String vLabel = var.get("label") != null ? var.get("label").toString() : "";
                                                                if (vLabel.equals(item.getVariantLabel())) {
                                                                    stock = var.get("stock") instanceof Number ? 
                                                                        ((Number)var.get("stock")).intValue() : 0;
                                                                    foundVariant = true;
                                                                    break;
                                                                }
                                                            }
                                                        }
                                                        
                                                        // Fallback to legacy stock if no variant matches
                                                        if (!foundVariant) {
                                                            Number stockNum = doc.getLong("stock");
                                                            stock = stockNum != null ? stockNum.intValue() : 0;
                                                        }

                                                        if (newQuantity > stock) {
                                                            Toast.makeText(CheckoutActivity.this,
                                                                    "Cannot add more. Only " + stock + " available in stock.",
                                                                    Toast.LENGTH_SHORT).show();
                                                        } else {
                                                            updateItemQuantity(item, newQuantity, adapter);
                                                        }
                                                    } else {
                                                        Toast.makeText(CheckoutActivity.this, "Product not found.", Toast.LENGTH_SHORT)
                                                                .show();
                                                    }
                                                })
                                                .addOnFailureListener(e -> Toast.makeText(CheckoutActivity.this,
                                                        "Failed to check stock.", Toast.LENGTH_SHORT).show());
                } else {
                    // Decrease requested
                    updateItemQuantity(item, newQuantity, adapter);
                }
            }

            @Override
            public void onRemoveItem(CartItem item) {
                db.removeCartItem(item.getId());
                cartItems.remove(item);
                adapter.notifyDataSetChanged();
                updateTotal();
            }
        });

        rvItems.setAdapter(adapter);

        updateTotal();
        fetchDeliveryRates();
        fetchStockLimits(adapter);

        btnPlaceOrder.setOnClickListener(v -> {
            // Build combined address string from the structured fields
            String addr1 = etAddress.getText() != null ? etAddress.getText().toString().trim() : "";
            String addr2 = etAddress2.getText() != null ? etAddress2.getText().toString().trim() : "";
            String provinceVal = actProvince.getText() != null ? actProvince.getText().toString().trim() : "";
            String districtVal = actDistrict.getText() != null ? actDistrict.getText().toString().trim() : "";
            String cityVal = etCity.getText() != null ? etCity.getText().toString().trim() : "";
            String postalVal = etPostal.getText() != null ? etPostal.getText().toString().trim() : "";

            if (TextUtils.isEmpty(addr1)) {
                ((com.google.android.material.textfield.TextInputLayout) findViewById(R.id.til_address))
                        .setError("Please enter Address Line 1");
                return;
            } else {
                ((com.google.android.material.textfield.TextInputLayout) findViewById(R.id.til_address))
                        .setError(null);
            }

            if (TextUtils.isEmpty(provinceVal)) {
                ((com.google.android.material.textfield.TextInputLayout) findViewById(R.id.til_province))
                        .setError("Please select a Province");
                return;
            } else {
                ((com.google.android.material.textfield.TextInputLayout) findViewById(R.id.til_province))
                        .setError(null);
            }

            if (TextUtils.isEmpty(districtVal)) {
                ((com.google.android.material.textfield.TextInputLayout) findViewById(R.id.til_district))
                        .setError("Please select a District");
                return;
            } else {
                ((com.google.android.material.textfield.TextInputLayout) findViewById(R.id.til_district))
                        .setError(null);
            }

            if (TextUtils.isEmpty(cityVal)) {
                ((com.google.android.material.textfield.TextInputLayout) findViewById(R.id.til_city))
                        .setError("Please enter a City");
                return;
            } else {
                ((com.google.android.material.textfield.TextInputLayout) findViewById(R.id.til_city))
                        .setError(null);
            }

            if (TextUtils.isEmpty(postalVal)) {
                ((com.google.android.material.textfield.TextInputLayout) findViewById(R.id.til_postal))
                        .setError("Please enter a Postal Code");
                return;
            } else {
                ((com.google.android.material.textfield.TextInputLayout) findViewById(R.id.til_postal))
                        .setError(null);
            }
            
            // Validate Billing if not same
            if (!cbBillingSame.isChecked()) {
                String billAddr1 = etBillingAddr.getText() != null ? etBillingAddr.getText().toString().trim() : "";
                String billProv = actBillingProvince.getText() != null ? actBillingProvince.getText().toString().trim() : "";
                String billDist = actBillingDistrict.getText() != null ? actBillingDistrict.getText().toString().trim() : "";
                String billCity = etBillingCity.getText() != null ? etBillingCity.getText().toString().trim() : "";
                String billPost = etBillingPostal.getText() != null ? etBillingPostal.getText().toString().trim() : "";

                if (TextUtils.isEmpty(billAddr1)) {
                    ((com.google.android.material.textfield.TextInputLayout) findViewById(R.id.til_billing_address)).setError("Please enter Billing Address");
                    return;
                } else {
                    ((com.google.android.material.textfield.TextInputLayout) findViewById(R.id.til_billing_address)).setError(null);
                }

                if (TextUtils.isEmpty(billProv)) {
                    ((com.google.android.material.textfield.TextInputLayout) findViewById(R.id.til_billing_province)).setError("Please select Billing Province");
                    return;
                } else {
                    ((com.google.android.material.textfield.TextInputLayout) findViewById(R.id.til_billing_province)).setError(null);
                }

                if (TextUtils.isEmpty(billDist)) {
                    ((com.google.android.material.textfield.TextInputLayout) findViewById(R.id.til_billing_district)).setError("Please select Billing District");
                    return;
                } else {
                    ((com.google.android.material.textfield.TextInputLayout) findViewById(R.id.til_billing_district)).setError(null);
                }

                if (TextUtils.isEmpty(billCity)) {
                    ((com.google.android.material.textfield.TextInputLayout) findViewById(R.id.til_billing_city)).setError("Please enter Billing City");
                    return;
                } else {
                    ((com.google.android.material.textfield.TextInputLayout) findViewById(R.id.til_billing_city)).setError(null);
                }

                if (TextUtils.isEmpty(billPost)) {
                    ((com.google.android.material.textfield.TextInputLayout) findViewById(R.id.til_billing_postal)).setError("Please enter Billing Postal Code");
                    return;
                } else {
                    ((com.google.android.material.textfield.TextInputLayout) findViewById(R.id.til_billing_postal)).setError(null);
                }
            }

            // Combine into a deliverable address string
            StringBuilder sb = new StringBuilder(addr1);
            if (!addr2.isEmpty())
                sb.append(", ").append(addr2);
            if (!districtVal.isEmpty())
                sb.append(", ").append(districtVal);
            if (!provinceVal.isEmpty())
                sb.append(", ").append(provinceVal);
            if (!cityVal.isEmpty())
                sb.append(", ").append(cityVal);
            if (!postalVal.isEmpty())
                sb.append(" ").append(postalVal);
            String address = sb.toString();

            if (cartItems.isEmpty()) {
                Toast.makeText(this, "Your cart is empty", Toast.LENGTH_SHORT).show();
                return;
            }

            // Check Firebase auth
            String firebaseUid = prefs.getFirebaseUid();
            if (firebaseUid == null || firebaseUid.isEmpty()) {
                Log.e(TAG, "No Firebase UID found — user not properly authenticated");
                Toast.makeText(this, "Please log in again to place order", Toast.LENGTH_LONG).show();
                return;
            }

            pendingAddress = address;
            btnPlaceOrder.setEnabled(false);
            if (paymentOverlay != null)
                paymentOverlay.setVisibility(android.view.View.VISIBLE);

            verifyStockBeforeCheckout(address);
        });
    }

    /** Set up cascading province → district dropdowns for a given pair. */
    private void setupProvinceDistrictDropdowns(AutoCompleteTextView provinceView, AutoCompleteTextView districtView, boolean isShipping) {
        ArrayAdapter<String> provinceAdapter = new ArrayAdapter<>(
                this, R.layout.item_dropdown, LocationData.getProvinces());
        provinceView.setAdapter(provinceAdapter);

        provinceView.setOnItemClickListener((parent, view, position, id) -> {
            String selectedProvince = (String) parent.getItemAtPosition(position);
            populateDistrictDropdown(districtView, selectedProvince);
            districtView.setText("", false);
            if (isShipping) {
                recalculateDeliveryFee(selectedProvince);
            }
        });
    }

    /** Populate district dropdown for the given province. */
    private void populateDistrictDropdown(AutoCompleteTextView districtView, String province) {
        String[] districts = LocationData.getDistricts(province);
        ArrayAdapter<String> districtAdapter = new ArrayAdapter<>(
                this, R.layout.item_dropdown, districts);
        districtView.setAdapter(districtAdapter);
    }

    /** Fetch delivery charge configuration from Firestore. */
    private void fetchDeliveryRates() {
        com.google.firebase.firestore.FirebaseFirestore.getInstance()
                .collection("system").document("deliveryCharges")
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        Number threshold = doc.getDouble("freeDeliveryThreshold");
                        if (threshold != null) freeDeliveryThreshold = threshold.doubleValue();

                        Number defRate = doc.getDouble("defaultRate");
                        if (defRate != null) defaultDeliveryRate = defRate.doubleValue();

                        @SuppressWarnings("unchecked")
                        Map<String, Object> rates = (Map<String, Object>) doc.get("provinceRates");
                        if (rates != null) {
                            for (Map.Entry<String, Object> entry : rates.entrySet()) {
                                if (entry.getValue() instanceof Number) {
                                    provinceRates.put(entry.getKey(), ((Number) entry.getValue()).doubleValue());
                                }
                            }
                        }
                        deliveryRatesLoaded = true;
                        Log.d(TAG, "Delivery rates loaded: " + provinceRates.size() + " provinces, threshold=" + freeDeliveryThreshold);

                        // If province already selected, recalculate
                        String currentProvince = actProvince.getText() != null ? actProvince.getText().toString().trim() : "";
                        if (!currentProvince.isEmpty()) {
                            recalculateDeliveryFee(currentProvince);
                        } else {
                            deliveryFee = defaultDeliveryRate;
                            updateTotalWithDelivery();
                        }
                    } else {
                        Log.w(TAG, "No delivery charges document found, using defaults");
                        deliveryFee = defaultDeliveryRate;
                        deliveryRatesLoaded = true;
                        updateTotalWithDelivery();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to fetch delivery rates: " + e.getMessage());
                    deliveryFee = defaultDeliveryRate;
                    deliveryRatesLoaded = true;
                    updateTotalWithDelivery();
                });
    }

    /** Recalculate delivery fee based on the selected province. */
    private void recalculateDeliveryFee(String province) {
        if (provinceRates.containsKey(province)) {
            deliveryFee = provinceRates.get(province);
        } else {
            deliveryFee = defaultDeliveryRate;
        }

        // Show province label
        if (tvDeliveryProvince != null) {
            tvDeliveryProvince.setText("(" + province + ")");
            tvDeliveryProvince.setVisibility(android.view.View.VISIBLE);
        }

        updateTotalWithDelivery();
    }

    /** Update the price breakdown UI with subtotal, delivery, and grand total. */
    private void updateTotalWithDelivery() {
        // Check free delivery threshold
        boolean isFreeDelivery = subtotal >= freeDeliveryThreshold;
        double effectiveDelivery = isFreeDelivery ? 0 : deliveryFee;
        grandTotal = subtotal + effectiveDelivery;

        if (tvSubtotal != null)
            tvSubtotal.setText(String.format(Locale.getDefault(), "LKR %.2f", subtotal));

        if (tvDeliveryFee != null) {
            if (isFreeDelivery) {
                tvDeliveryFee.setText("FREE");
                tvDeliveryFee.setTextColor(getResources().getColor(R.color.colorSuccess, null));
            } else {
                tvDeliveryFee.setText(String.format(Locale.getDefault(), "LKR %.2f", effectiveDelivery));
                tvDeliveryFee.setTextColor(getResources().getColor(R.color.textPrimary, null));
            }
        }

        if (layoutFreeDeliveryBadge != null) {
            layoutFreeDeliveryBadge.setVisibility(isFreeDelivery ? android.view.View.VISIBLE : android.view.View.GONE);
        }

        if (tvGrandTotal != null)
            tvGrandTotal.setText(String.format(Locale.getDefault(), "LKR %.2f", grandTotal));

        // Also update the bottom bar total
        TextView tvTotal = findViewById(R.id.tv_total_bottom);
        if (tvTotal != null)
            tvTotal.setText(String.format(Locale.getDefault(), "LKR %.2f", grandTotal));
    }

    private void fetchStockLimits(CartAdapter adapter) {
        com.google.firebase.firestore.FirebaseFirestore firestore = com.google.firebase.firestore.FirebaseFirestore
                .getInstance();
        java.util.Map<String, Integer> stockMap = new java.util.HashMap<>();

        for (CartItem item : cartItems) {
            firestore.collection("products")
                    .whereEqualTo("name", item.getProductName())
                    .limit(1)
                    .get()
                    .addOnSuccessListener(snapshot -> {
                        if (!snapshot.isEmpty()) {
                            com.google.firebase.firestore.DocumentSnapshot doc = snapshot.getDocuments().get(0);
                            int stock = 0;
                            
                            // Check variants array for exact label
                            java.util.List<java.util.Map<String, Object>> variants = 
                                (java.util.List<java.util.Map<String, Object>>) doc.get("variants");
                            boolean foundVariant = false;
                                    
                            if (variants != null) {
                                for (java.util.Map<String, Object> var : variants) {
                                    String vLabel = var.get("label") != null ? var.get("label").toString() : "";
                                    if (vLabel.equals(item.getVariantLabel())) {
                                        stock = var.get("stock") instanceof Number ? 
                                            ((Number)var.get("stock")).intValue() : 0;
                                        foundVariant = true;
                                        break;
                                    }
                                }
                            }
                            
                            // Fallback to legacy stock if no variant matches
                            if (!foundVariant) {
                                Number stockNum = doc.getLong("stock");
                                stock = stockNum != null ? stockNum.intValue() : 0;
                            }

                            stockMap.put(item.getProductName(), stock);
                            adapter.setStockMap(stockMap);
                        }
                    });
        }
    }

    private void updateItemQuantity(CartItem item, int newQuantity, CartAdapter adapter) {
        db.updateCartItemQuantity(item.getId(), newQuantity);
        item.setQuantity(newQuantity);
        adapter.notifyDataSetChanged();
        updateTotal();
    }

    private void updateTotal() {
        subtotal = 0;
        for (CartItem item : cartItems) {
            subtotal += item.getSubtotal();
        }
        updateTotalWithDelivery();
    }

    private void verifyStockBeforeCheckout(String address) {
        com.google.firebase.firestore.FirebaseFirestore firestore = com.google.firebase.firestore.FirebaseFirestore
                .getInstance();

        // Use a counter to track when all async checks finish
        final int[] checksCompleted = { 0 };
        final boolean[] hasStockIssue = { false };

        for (CartItem item : cartItems) {
            firestore.collection("products")
                    .whereEqualTo("name", item.getProductName())
                    .limit(1)
                    .get()
                    .addOnSuccessListener(snapshot -> {
                        if (!snapshot.isEmpty()) {
                            com.google.firebase.firestore.DocumentSnapshot doc = snapshot.getDocuments().get(0);
                            int stock = 0;
                            
                            // Check variants array for exact label
                            java.util.List<java.util.Map<String, Object>> variants = 
                                (java.util.List<java.util.Map<String, Object>>) doc.get("variants");
                            boolean foundVariant = false;
                                    
                            if (variants != null) {
                                for (java.util.Map<String, Object> var : variants) {
                                    String vLabel = var.get("label") != null ? var.get("label").toString() : "";
                                    if (vLabel.equals(item.getVariantLabel())) {
                                        stock = var.get("stock") instanceof Number ? 
                                            ((Number)var.get("stock")).intValue() : 0;
                                        foundVariant = true;
                                        break;
                                    }
                                }
                            }
                            
                            // Fallback to legacy stock if no variant matches
                            if (!foundVariant) {
                                Number stockNum = doc.getLong("stock");
                                stock = stockNum != null ? stockNum.intValue() : 0;
                            }

                            if (item.getQuantity() > stock) {
                                hasStockIssue[0] = true;
                                Toast.makeText(CheckoutActivity.this,
                                        "Error: '" + item.getProductName() + "' only has " + stock
                                                + " left in stock. Please adjust your cart.",
                                        Toast.LENGTH_LONG).show();
                            }
                        } else {
                            hasStockIssue[0] = true;
                            Toast.makeText(CheckoutActivity.this, "Error: Product unavailable.", Toast.LENGTH_SHORT)
                                    .show();
                        }
                    })
                    .addOnFailureListener(e -> {
                        hasStockIssue[0] = true;
                        Toast.makeText(CheckoutActivity.this, "Network error verifying stock.", Toast.LENGTH_SHORT)
                                .show();
                    })
                    .addOnCompleteListener(task -> {
                        checksCompleted[0]++;
                        if (checksCompleted[0] == cartItems.size()) {
                            if (!hasStockIssue[0]) {
                                // All items passed stock verify, proceed to payment
                                launchPayHerePayment(address);
                            } else {
                                // Stock limit failed somewhere, abort checkout setup
                                btnPlaceOrder.setEnabled(true);
                                if (paymentOverlay != null)
                                    paymentOverlay.setVisibility(android.view.View.GONE);
                            }
                        }
                    });
        }
    }

    /**
     * Launch PayHere sandbox payment gateway.
     */
    private void launchPayHerePayment(String address) {
        Log.d(TAG, "=== PayHere Debug ===");
        Log.d(TAG, "Merchant ID: " + BuildConfig.PAYHERE_MERCHANT_ID);
        Log.d(TAG, "Merchant Secret length: " +
                (BuildConfig.PAYHERE_MERCHANT_SECRET != null ? BuildConfig.PAYHERE_MERCHANT_SECRET.length() : "NULL"));
        Log.d(TAG, "Sandbox mode: " + BuildConfig.PAYHERE_SANDBOX);
        Log.d(TAG, "Total amount: " + grandTotal);

        InitRequest req = new InitRequest();
        req.setMerchantId(BuildConfig.PAYHERE_MERCHANT_ID);
        req.setMerchantSecret(BuildConfig.PAYHERE_MERCHANT_SECRET);
        req.setCurrency("LKR");
        req.setAmount(grandTotal);
        req.setOrderId("LF-" + System.currentTimeMillis());
        req.setItemsDescription("LexoraFix Order (" + cartItems.size() + " items)");
        // Sandbox: empty notify URL; in production set a real server endpoint
        req.setNotifyUrl(BuildConfig.PAYHERE_SANDBOX ? "" : "https://lexorafix.com/notify");

        // Add individual items (required by some SDK versions)
        for (CartItem item : cartItems) {
            req.getItems().add(new lk.payhere.androidsdk.model.Item(
                    null, item.getProductName(), item.getQuantity(), item.getPrice()));
        }

        // Customer details
        String customerName = prefs.getUserName() != null && !prefs.getUserName().isEmpty()
                ? prefs.getUserName()
                : "Customer";
        String customerEmail = prefs.getUserEmail() != null && !prefs.getUserEmail().isEmpty()
                ? prefs.getUserEmail()
                : "customer@lexorafix.com";

        req.getCustomer().setFirstName(customerName);
        req.getCustomer().setLastName("LexoraFix");
        req.getCustomer().setEmail(customerEmail);
        req.getCustomer().setPhone("0771234567");
        req.getCustomer().getAddress().setAddress(address);
        req.getCustomer().getAddress().setCity("Colombo");
        req.getCustomer().getAddress().setCountry("Sri Lanka");

        // Internal reference
        req.setCustom1(prefs.getFirebaseUid());
        req.setCustom2(address);

        // Set sandbox URL
        PHConfigs.setBaseUrl(BuildConfig.PAYHERE_SANDBOX ? PHConfigs.SANDBOX_URL : PHConfigs.LIVE_URL);
        Log.d(TAG, "PayHere URL: " + (BuildConfig.PAYHERE_SANDBOX ? "SANDBOX" : "LIVE"));

        try {
            Intent intent = new Intent(this, PHMainActivity.class);
            intent.putExtra(PHConstants.INTENT_EXTRA_DATA, req);
            startActivityForResult(intent, PAYHERE_REQUEST);
        } catch (Exception e) {
            Log.e(TAG, "PayHere launch error: " + e.getMessage(), e);
            Toast.makeText(this, "Payment SDK error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            if (paymentOverlay != null)
                paymentOverlay.setVisibility(android.view.View.GONE);
            if (btnPlaceOrder != null)
                btnPlaceOrder.setEnabled(true);

            // In sandbox mode, offer to skip payment for testing
            if (BuildConfig.PAYHERE_SANDBOX) {
                offerSandboxBypass(address);
            }
        }
    }

    /**
     * In sandbox mode, if PayHere fails, offer to place order without payment
     * for development/testing purposes.
     */
    private void offerSandboxBypass(String address) {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Sandbox Mode")
                .setMessage("PayHere sandbox payment failed. Place order without payment for testing?")
                .setPositiveButton("Yes, place order", (dialog, which) -> placeOrderAfterPayment(address))
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // Always hide overlay and re-enable button
        if (paymentOverlay != null)
            paymentOverlay.setVisibility(android.view.View.GONE);
        if (btnPlaceOrder != null)
            btnPlaceOrder.setEnabled(true);

        if (requestCode == PAYHERE_REQUEST) {
            Log.d(TAG, "PayHere result: resultCode=" + resultCode +
                    ", hasData=" + (data != null) +
                    ", hasExtra=" + (data != null && data.hasExtra(PHConstants.INTENT_EXTRA_RESULT)));

            // Log all extras for debugging
            if (data != null && data.getExtras() != null) {
                for (String key : data.getExtras().keySet()) {
                    Log.d(TAG, "PayHere extra: " + key + " = " + data.getExtras().get(key));
                }
            }

            if (resultCode == RESULT_OK && data != null && data.hasExtra(PHConstants.INTENT_EXTRA_RESULT)) {
                PHResponse<StatusResponse> response = (PHResponse<StatusResponse>) data
                        .getSerializableExtra(PHConstants.INTENT_EXTRA_RESULT);

                if (response != null && response.isSuccess()) {
                    Log.d(TAG, "PayHere payment successful!");
                    Toast.makeText(this, "Payment successful!", Toast.LENGTH_SHORT).show();
                    placeOrderAfterPayment(pendingAddress);
                } else {
                    String msg = response != null ? response.toString() : "Payment failed";
                    Log.e(TAG, "PayHere payment failed: " + msg);
                    Toast.makeText(this, "Payment failed: " + msg, Toast.LENGTH_LONG).show();
                    // In sandbox, offer bypass
                    if (BuildConfig.PAYHERE_SANDBOX) {
                        offerSandboxBypass(pendingAddress);
                    }
                }
            } else if (resultCode == RESULT_CANCELED) {
                Log.w(TAG, "PayHere payment cancelled. resultCode=RESULT_CANCELED");
                if (data != null && data.hasExtra(PHConstants.INTENT_EXTRA_RESULT)) {
                    PHResponse<StatusResponse> response = (PHResponse<StatusResponse>) data
                            .getSerializableExtra(PHConstants.INTENT_EXTRA_RESULT);
                    Log.w(TAG, "Cancel response: " + (response != null ? response.toString() : "null"));
                }

                // In sandbox mode, offer to bypass payment for testing
                if (BuildConfig.PAYHERE_SANDBOX) {
                    offerSandboxBypass(pendingAddress);
                } else {
                    Toast.makeText(this, "Payment cancelled. Check your payment details and try again.",
                            Toast.LENGTH_LONG).show();
                }
            }
        }
    }

    /**
     * Place order AFTER successful payment.
     */
    private void placeOrderAfterPayment(String address) {
        int userId = 1;

        // Place order in local DB
        String orderId = db.placeOrder(userId, address, grandTotal);

        if (orderId != null && !orderId.isEmpty()) {
            // Save address for next time
            prefs.saveLastAddress(address);

            // Build rich order data with customer info and items for seller
            String firebaseUid = prefs.getFirebaseUid();
            if (firebaseUid != null && !firebaseUid.isEmpty()) {
                Map<String, Object> orderData = new HashMap<>();
                orderData.put("orderId", orderId);
                orderData.put("firebaseUid", firebaseUid);
                orderData.put("userName", prefs.getUserName() != null ? prefs.getUserName() : "");
                orderData.put("userEmail", prefs.getUserEmail() != null ? prefs.getUserEmail() : "");
                orderData.put("totalPrice", grandTotal);
                orderData.put("subtotal", subtotal);
                orderData.put("deliveryFee", subtotal >= freeDeliveryThreshold ? 0 : deliveryFee);
                orderData.put("freeDelivery", subtotal >= freeDeliveryThreshold);
                orderData.put("address", address);
                orderData.put("status", "Confirmed");
                orderData.put("paymentStatus", "Paid");
                orderData.put("paymentMethod", "PayHere");
                orderData.put("itemCount", cartItems.size());
                orderData.put("timestamp", com.google.firebase.Timestamp.now());

                // Add all cart items with full details for seller
                List<Map<String, Object>> itemsList = new ArrayList<>();
                for (CartItem item : cartItems) {
                    Map<String, Object> itemMap = new HashMap<>();
                    itemMap.put("productId", String.valueOf(item.getProductId()));
                    itemMap.put("productName", item.getProductName() != null ? item.getProductName() : "");
                    itemMap.put("variantLabel", item.getVariantLabel() != null ? item.getVariantLabel() : "");
                    itemMap.put("price", item.getPrice());
                    itemMap.put("quantity", item.getQuantity());
                    itemMap.put("imageUrl", item.getImageUrl() != null ? item.getImageUrl() : "");
                    itemsList.add(itemMap);
                }
                orderData.put("items", itemsList);

                // Dual-write: both user subcollection AND top-level orders for admin
                FirestoreHelper.getInstance().saveOrderWithNotification(
                        firebaseUid, orderId, orderData,
                        new FirestoreHelper.FirestoreCallback<Void>() {
                            @Override
                            public void onSuccess(Void result) {
                                Log.d(TAG, "Order synced to Firestore: " + orderId);
                                // Decrement stock for each product — query by name since
                                // SQLite productId doesn't match Firestore document IDs
                                com.google.firebase.firestore.FirebaseFirestore firestore = com.google.firebase.firestore.FirebaseFirestore
                                        .getInstance();
                                for (CartItem item : cartItems) {
                                    String productName = item.getProductName();
                                    int qty = item.getQuantity();
                                    firestore.collection("products")
                                            .whereEqualTo("name", productName)
                                            .limit(1)
                                            .get()
                                            .addOnSuccessListener(snapshot -> {
                                                if (!snapshot.isEmpty()) {
                                                    String docId = snapshot.getDocuments().get(0).getId();
                                                    com.google.firebase.firestore.DocumentReference docRef = firestore.collection("products").document(docId);
                                                    
                                                    firestore.runTransaction(transaction -> {
                                                        com.google.firebase.firestore.DocumentSnapshot prodSnapshot = transaction.get(docRef);
                                                        if (!prodSnapshot.exists()) {
                                                            throw new com.google.firebase.firestore.FirebaseFirestoreException("Product not found", com.google.firebase.firestore.FirebaseFirestoreException.Code.NOT_FOUND);
                                                        }

                                                        // Deduct base stock always to keep legacy values in sync
                                                        long currentBaseStock = prodSnapshot.getLong("stock") != null ? prodSnapshot.getLong("stock") : 0;
                                                        transaction.update(docRef, "stock", Math.max(0, currentBaseStock - qty));

                                                        // Deduct variant stock
                                                        String variantLabel = item.getVariantLabel();
                                                        if (variantLabel != null && !variantLabel.isEmpty()) {
                                                            java.util.List<java.util.Map<String, Object>> variants = (java.util.List<java.util.Map<String, Object>>) prodSnapshot.get("variants");
                                                            if (variants != null) {
                                                                java.util.List<java.util.Map<String, Object>> updatedVariants = new java.util.ArrayList<>();
                                                                for (java.util.Map<String, Object> v : variants) {
                                                                    java.util.Map<String, Object> updatedV = new java.util.HashMap<>(v);
                                                                    String vLabel = (String) updatedV.get("label");
                                                                    if (variantLabel.equals(vLabel)) {
                                                                        long vStock = updatedV.get("stock") != null ? ((Number) updatedV.get("stock")).longValue() : 0;
                                                                        updatedV.put("stock", Math.max(0, vStock - qty));
                                                                    }
                                                                    updatedVariants.add(updatedV);
                                                                }
                                                                transaction.update(docRef, "variants", updatedVariants);
                                                            }
                                                        }
                                                        return null;
                                                    }).addOnSuccessListener(aVoid -> {
                                                        Log.d(TAG, "Stock decremented for: " + productName + (item.getVariantLabel() != null ? " (" + item.getVariantLabel() + ")" : ""));
                                                    }).addOnFailureListener(e -> {
                                                        Log.e(TAG, "Stock decrement failed for " + productName + ": " + e.getMessage());
                                                    });
                                                } else {
                                                    Log.e(TAG, "Product not found in Firestore: " + productName);
                                                }
                                            })
                                            .addOnFailureListener(e -> Log.e(TAG,
                                                    "Product query failed: " + e.getMessage()));
                                }
                            }

                            @Override
                            public void onError(String message) {
                                Log.e(TAG, "Firestore sync FAILED: " + message);
                                runOnUiThread(() -> Toast.makeText(CheckoutActivity.this,
                                        "Cloud sync failed: " + message,
                                        Toast.LENGTH_LONG).show());
                            }
                        });
            } else {
                Log.e(TAG, "No Firebase UID — order NOT saved to cloud!");
                Toast.makeText(this, "Order saved locally but not synced (no login)",
                        Toast.LENGTH_LONG).show();
            }

            // Show notification to customer
            NotificationHelper notificationHelper = new NotificationHelper(this);
            notificationHelper.showOrderConfirmation(orderId);

            // Send email confirmation to customer + admin/seller
            try {
                String customerEmail = prefs.getUserEmail();
                com.google.firebase.auth.FirebaseUser currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
                if ((customerEmail == null || customerEmail.isEmpty()) && currentUser != null) {
                    customerEmail = currentUser.getEmail();
                }
                
                String customerName = prefs.getUserName() != null ? prefs.getUserName() : "Customer";
                String customerPhone = prefs.getUserPhone() != null ? prefs.getUserPhone() : "";

                // Build items HTML for both customer and seller emails
                StringBuilder itemsHtml = new StringBuilder();
                for (CartItem item : cartItems) {
                    String varLabel = item.getVariantLabel();
                    String displayName = item.getProductName()
                            + (varLabel != null && !varLabel.trim().isEmpty() ? " [" + varLabel + "]" : "");
                    itemsHtml.append(
                            "<div style='display:flex;justify-content:space-between;padding:8px 0;border-bottom:1px solid rgba(255,255,255,0.1);'>")
                            .append("<span style='color:#E0E0E0;'>").append(displayName)
                            .append(" × ").append(item.getQuantity()).append("</span>")
                            .append("<span style='color:#10B981;font-weight:bold;'>LKR ")
                            .append(String.format(java.util.Locale.US, "%.2f", item.getSubtotal())).append("</span></div>");
                }
                String itemsHtmlStr = itemsHtml.toString();

                // Send customer confirmation
                EmailHelper.getInstance().sendOrderConfirmation(customerEmail, orderId, grandTotal, itemsHtmlStr);

                // Send rich seller/admin notification with full order details
                EmailHelper.getInstance().sendAdminOrderNotification(
                        orderId, customerName, customerEmail, customerPhone,
                        address, itemsHtmlStr, grandTotal);
            } catch (Exception e) {
                Log.w(TAG, "Email sending failed (non-critical): " + e.getMessage());
            }

            Toast.makeText(this, "Order placed successfully!", Toast.LENGTH_LONG).show();

            // Go back to main
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            finish();
        } else {
            Toast.makeText(this, "Failed to place order. Please try again.", Toast.LENGTH_SHORT).show();
        }
    }
}
