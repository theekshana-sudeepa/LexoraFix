package com.lexoratech.lexorafix.admin.activities;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.navigation.NavigationView;
import android.view.MenuItem;
import android.os.Handler;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.lexoratech.lexorafix.admin.R;
import com.lexoratech.lexorafix.admin.services.AdminOrderListenerService;

import java.util.Locale;
import android.content.Context;
import com.lexoratech.lexorafix.admin.utils.ThemeUtils;

public class AdminDashboardActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private static final String TAG = "AdminDashboard";
    private static final int NOTIFICATION_PERMISSION_REQUEST = 200;

    private TextView tvTotalUsers, tvTotalOrders, tvTotalBookings,
            tvTotalProducts, tvTotalCategories, tvTotalRevenue, tvLowStock;

    private ListenerRegistration ordersListener;
    private ListenerRegistration bookingsListener;

    private DrawerLayout drawerLayout;
    private NavigationView navigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeUtils.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        tvTotalUsers = findViewById(R.id.tv_total_users);
        tvTotalOrders = findViewById(R.id.tv_total_orders);
        tvTotalBookings = findViewById(R.id.tv_total_bookings);
        tvTotalProducts = findViewById(R.id.tv_total_products);
        tvTotalCategories = findViewById(R.id.tv_total_categories);
        tvTotalRevenue = findViewById(R.id.tv_total_revenue);

        // Drawer Setup
        drawerLayout = findViewById(R.id.drawer_layout);
        navigationView = findViewById(R.id.nav_view_admin);

        if (navigationView != null) {
            navigationView.setNavigationItemSelectedListener(this);
            // Setup header email
            android.view.View headerView = navigationView.getHeaderView(0);
            if (headerView != null) {
                TextView tvHeaderEmail = headerView.findViewById(R.id.tv_header_email);
                if (tvHeaderEmail != null && FirebaseAuth.getInstance().getCurrentUser() != null) {
                    tvHeaderEmail.setText(FirebaseAuth.getInstance().getCurrentUser().getEmail());
                }
            }
        }

        // Dynamic Greeting
        TextView tvGreeting = findViewById(R.id.tv_greeting);
        if (tvGreeting != null) {
            int hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);
            String greeting;
            if (hour < 12) {
                greeting = "Good Morning, Admin \u2600\uFE0F";
            } else if (hour < 17) {
                greeting = "Good Afternoon, Admin \uD83D\uDC4B";
            } else {
                greeting = "Good Evening, Admin \uD83C\uDF19";
            }
            tvGreeting.setText(greeting);
        }

        // Open Drawer on Menu Icon Click
        android.view.View btnMenu = findViewById(R.id.btn_menu);
        if (btnMenu != null) {
            btnMenu.setOnClickListener(v -> {
                if (drawerLayout != null) {
                    drawerLayout.openDrawer(GravityCompat.START);
                }
            });
        }



        // Quick action buttons
        findViewById(R.id.qa_add_product).setOnClickListener(v -> {
            startActivity(new Intent(this, AddEditProductActivity.class));
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

        // Request notification permission (Android 13+) and start listener
        requestNotificationPermissionAndStartService();

        // Apply Entrance Animations
        applyEntranceAnimations();
        
        // Auto-seed delivery charges if missing
        seedDeliveryChargesIfMissing();
    }



    private void applyEntranceAnimations() {
        // We'll apply layout animations later if requested in a refined way
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
    
    private void seedDeliveryChargesIfMissing() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("system").document("deliveryCharges").get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        java.util.Map<String, Object> data = new java.util.HashMap<>();
                        data.put("freeDeliveryThreshold", 5000.0);
                        data.put("defaultRate", 500.0);
                        
                        java.util.Map<String, Object> provinceRates = new java.util.HashMap<>();
                        provinceRates.put("Western", 250.0);
                        provinceRates.put("Central", 400.0);
                        provinceRates.put("Southern", 450.0);
                        provinceRates.put("North Western", 400.0);
                        provinceRates.put("North Central", 500.0);
                        provinceRates.put("Uva", 550.0);
                        provinceRates.put("Sabaragamuwa", 450.0);
                        provinceRates.put("Eastern", 600.0);
                        provinceRates.put("Northern", 650.0);
                        
                        data.put("provinceRates", provinceRates);
                        
                        db.collection("system").document("deliveryCharges").set(data)
                                .addOnSuccessListener(aVoid -> Log.d(TAG, "Successfully seeded default delivery charges."));
                    }
                });
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
                        animateRevenueText(totalRevenue);
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
        // Categories count
        db.collection("categories").get()
                .addOnSuccessListener(q -> tvTotalCategories.setText(String.valueOf(q.size())))
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Categories count failed: " + e.getMessage());
                    tvTotalCategories.setText("--");
                });
    }

    private void animateRevenueText(double targetRevenue) {
        // Parse current revenue if possible, or start from 0
        double currentVal = 0;
        try {
            String currentText = tvTotalRevenue.getText().toString();
            if (currentText.startsWith("LKR ")) {
                currentVal = Double.parseDouble(currentText.substring(4).replaceAll(",", ""));
            }
        } catch (Exception ignored) {}

        if (currentVal == targetRevenue) return;

        android.animation.ValueAnimator animator = android.animation.ValueAnimator.ofFloat((float) currentVal, (float) targetRevenue);
        animator.setDuration(1200); // 1.2s smooth animation
        animator.setInterpolator(new android.view.animation.DecelerateInterpolator());
        animator.addUpdateListener(animation -> {
            float animatedValue = (float) animation.getAnimatedValue();
            tvTotalRevenue.setText(String.format(java.util.Locale.getDefault(), "LKR %,.2f", animatedValue));
        });
        animator.start();
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Close drawer first
        if (drawerLayout != null) {
            drawerLayout.closeDrawer(GravityCompat.START);
        }

        int id = item.getItemId();

        // Delay to allow drawer to close smoothly before launching new activity
        new Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            if (id == R.id.nav_dashboard) {
                // Already on dashboard
            } else if (id == R.id.nav_users) {
                startActivity(new Intent(this, ManageUsersActivity.class));
            } else if (id == R.id.nav_products) {
                startActivity(new Intent(this, ManageProductsActivity.class));
            } else if (id == R.id.nav_categories) {
                startActivity(new Intent(this, ManageCategoriesActivity.class));
            } else if (id == R.id.nav_orders) {
                startActivity(new Intent(this, ManageOrdersActivity.class));
            } else if (id == R.id.nav_bookings) {
                startActivity(new Intent(this, ManageBookingsActivity.class));
            } else if (id == R.id.nav_service_centers) {
                startActivity(new Intent(this, ManageServiceCentersActivity.class));
            } else if (id == R.id.nav_settings) {
                startActivity(new Intent(this, AdminSettingsActivity.class));

            } else if (id == R.id.nav_logout) {
                FirebaseAuth.getInstance().signOut();
                startActivity(new Intent(this, AdminLoginActivity.class));
                finish();
            }

            // Apply fade transition
            if (id != R.id.nav_dashboard) {
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            }
        }, 250);

        return true;
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout != null && drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}
