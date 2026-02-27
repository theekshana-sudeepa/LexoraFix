package com.lexoratech.lexorafix.admin.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.lexoratech.lexorafix.admin.R;
import com.lexoratech.lexorafix.admin.services.AdminOrderListenerService;

import java.util.Locale;

public class AdminDashboardActivity extends AppCompatActivity {

    private static final String TAG = "AdminDashboard";
    private static final int NOTIFICATION_PERMISSION_REQUEST = 200;

    private TextView tvTotalUsers, tvTotalOrders, tvTotalBookings,
            tvTotalProducts, tvTotalCategories, tvTotalRevenue, tvLowStock;

    private ListenerRegistration ordersListener;
    private ListenerRegistration bookingsListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        tvTotalUsers = findViewById(R.id.tv_total_users);
        tvTotalOrders = findViewById(R.id.tv_total_orders);
        tvTotalBookings = findViewById(R.id.tv_total_bookings);
        tvTotalProducts = findViewById(R.id.tv_total_products);
        tvTotalCategories = findViewById(R.id.tv_total_categories);
        tvTotalRevenue = findViewById(R.id.tv_total_revenue);
        tvLowStock = findViewById(R.id.tv_low_stock);

        // Card click navigation (stat cards)
        findViewById(R.id.card_users).setOnClickListener(v -> {
            startActivity(new Intent(this, ManageUsersActivity.class));
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });
        findViewById(R.id.card_orders).setOnClickListener(v -> {
            startActivity(new Intent(this, ManageOrdersActivity.class));
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });
        findViewById(R.id.card_bookings).setOnClickListener(v -> {
            startActivity(new Intent(this, ManageBookingsActivity.class));
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });
        findViewById(R.id.card_products).setOnClickListener(v -> {
            startActivity(new Intent(this, ManageProductsActivity.class));
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });
        findViewById(R.id.card_categories).setOnClickListener(v -> {
            startActivity(new Intent(this, ManageCategoriesActivity.class));
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });

        // Quick action buttons
        findViewById(R.id.qa_add_product).setOnClickListener(v -> {
            startActivity(new Intent(this, AddEditProductActivity.class));
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });
        findViewById(R.id.qa_add_category).setOnClickListener(v -> {
            startActivity(new Intent(this, AddEditCategoryActivity.class));
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });
        findViewById(R.id.qa_manage_orders).setOnClickListener(v -> {
            startActivity(new Intent(this, ManageOrdersActivity.class));
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });
        findViewById(R.id.qa_manage_bookings).setOnClickListener(v -> {
            startActivity(new Intent(this, ManageBookingsActivity.class));
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });
        findViewById(R.id.qa_manage_users).setOnClickListener(v -> {
            startActivity(new Intent(this, ManageUsersActivity.class));
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });
        findViewById(R.id.qa_manage_products).setOnClickListener(v -> {
            startActivity(new Intent(this, ManageProductsActivity.class));
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });
        findViewById(R.id.qa_manage_service_centers).setOnClickListener(v -> {
            startActivity(new Intent(this, ManageServiceCentersActivity.class));
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });

        android.view.View btnMigrate = findViewById(R.id.qa_migrate_data);
        if (btnMigrate != null) {
            btnMigrate.setOnClickListener(v -> migrateMockDataToFirestore());
        }

        // Sign out
        findViewById(R.id.btn_logout).setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(this, AdminLoginActivity.class));
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        });

        // Request notification permission (Android 13+) and start listener
        requestNotificationPermissionAndStartService();

        // Apply Entrance Animations
        applyEntranceAnimations();
    }

    private void migrateMockDataToFirestore() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // The 5 static service centers from assets/data.json
        Object[][] mockCenters = {
                { 1, "LexoraFix Main Hub - Colombo", "123 Galle Road, Colombo 03", "+94112345678", 6.9271, 79.8612,
                        "Main service center with full tool repair and installation services" },
                { 2, "LexoraFix Kandy Center", "45 Peradeniya Road, Kandy", "+94812345678", 7.2906, 80.6337,
                        "Service center covering central province" },
                { 3, "LexoraFix Galle Branch", "78 Main Street, Galle Fort", "+94912345678", 6.0324, 80.2170,
                        "Southern province service branch" },
                { 4, "LexoraFix Negombo", "12 Lewis Place, Negombo", "+94312345678", 7.2083, 79.8358,
                        "Western coast service point" },
                { 5, "LexoraFix Jaffna", "56 Hospital Road, Jaffna", "+94212345678", 9.6615, 80.0255,
                        "Northern province service center" }
        };

        android.widget.Toast.makeText(this, "Starting migration...", android.widget.Toast.LENGTH_SHORT).show();

        for (Object[] center : mockCenters) {
            java.util.Map<String, Object> data = new java.util.HashMap<>();
            data.put("id", center[0]);
            data.put("name", center[1]);
            data.put("address", center[2]);
            data.put("phone", center[3]);
            data.put("latitude", center[4]);
            data.put("longitude", center[5]);
            data.put("description", center[6]);

            db.collection("service_centers")
                    .add(data)
                    .addOnSuccessListener(doc -> Log.d(TAG, "Migrated: " + center[1]))
                    .addOnFailureListener(e -> Log.e(TAG, "Failed migration: " + center[1], e));
        }

        android.widget.Toast
                .makeText(this, "Migration tasks queued. Check logs/Firestore.", android.widget.Toast.LENGTH_LONG)
                .show();
    }

    private void applyEntranceAnimations() {
        android.widget.LinearLayout contentLayout = findViewById(R.id.ll_dashboard_content);
        if (contentLayout != null) {
            android.view.animation.LayoutAnimationController controller = android.view.animation.AnimationUtils
                    .loadLayoutAnimation(this, R.anim.layout_animation_fall_down);
            contentLayout.setLayoutAnimation(controller);
        }

        android.view.View header = findViewById(R.id.ll_dashboard_header);
        if (header != null) {
            header.setAlpha(0f);
            header.setTranslationY(-50f);
            header.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(600)
                    .setInterpolator(new android.view.animation.DecelerateInterpolator(2f))
                    .start();
        }
    }

    private void requestNotificationPermissionAndStartService() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[] { Manifest.permission.POST_NOTIFICATIONS },
                        NOTIFICATION_PERMISSION_REQUEST);
            } else {
                startListenerService();
            }
        } else {
            startListenerService();
        }
    }

    private void startListenerService() {
        Intent serviceIntent = new Intent(this, AdminOrderListenerService.class);
        startService(serviceIntent);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == NOTIFICATION_PERMISSION_REQUEST) {
            startListenerService();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadDashboardData();
        startRealtimeCountListeners();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (ordersListener != null) {
            ordersListener.remove();
            ordersListener = null;
        }
        if (bookingsListener != null) {
            bookingsListener.remove();
            bookingsListener = null;
        }
    }

    /**
     * Real-time listeners for orders and bookings counts so they update live.
     */
    private void startRealtimeCountListeners() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Real-time order count + revenue
        ordersListener = db.collection("orders")
                .addSnapshotListener((querySnapshot, error) -> {
                    if (error != null) {
                        Log.w(TAG, "Orders listener error: " + error.getMessage());
                        return;
                    }
                    if (querySnapshot != null) {
                        tvTotalOrders.setText(String.valueOf(querySnapshot.size()));
                        double totalRevenue = 0;
                        for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                            String status = doc.getString("status");
                            // Count revenue only for non-cancelled orders
                            if (status != null && !status.equals("Cancelled")) {
                                Object price = doc.get("totalPrice");
                                if (price instanceof Number) {
                                    totalRevenue += ((Number) price).doubleValue();
                                }
                            }
                        }
                        tvTotalRevenue.setText(String.format(Locale.getDefault(), "LKR %.2f", totalRevenue));
                    }
                });

        // Real-time booking count
        bookingsListener = db.collection("bookings")
                .addSnapshotListener((querySnapshot, error) -> {
                    if (error != null) {
                        Log.w(TAG, "Bookings listener error: " + error.getMessage());
                        return;
                    }
                    if (querySnapshot != null) {
                        tvTotalBookings.setText(String.valueOf(querySnapshot.size()));
                    }
                });
    }

    private void loadDashboardData() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // Users count (one-time load is fine for users)
        db.collection("users").get()
                .addOnSuccessListener(q -> tvTotalUsers.setText(String.valueOf(q.size())))
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Users count failed: " + e.getMessage());
                    tvTotalUsers.setText("--");
                });

        // Products count
        db.collection("products").get()
                .addOnSuccessListener(q -> tvTotalProducts.setText(String.valueOf(q.size())))
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Products count failed: " + e.getMessage());
                    tvTotalProducts.setText("--");
                });

        // Low stock count (<= 5)
        db.collection("products").whereLessThanOrEqualTo("stock", 5).get()
                .addOnSuccessListener(q -> {
                    if (tvLowStock != null)
                        tvLowStock.setText(String.valueOf(q.size()));
                })
                .addOnFailureListener(e -> {
                    if (tvLowStock != null)
                        tvLowStock.setText("--");
                });

        // Categories count
        db.collection("categories").get()
                .addOnSuccessListener(q -> tvTotalCategories.setText(String.valueOf(q.size())))
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Categories count failed: " + e.getMessage());
                    tvTotalCategories.setText("--");
                });
    }
}
