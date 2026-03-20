package com.lexoratech.lexorafix.activities;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.lexoratech.lexorafix.R;
import com.lexoratech.lexorafix.fragments.CartFragment;
import com.lexoratech.lexorafix.fragments.CategoryFragment;
import com.lexoratech.lexorafix.fragments.HomeFragment;
import com.lexoratech.lexorafix.fragments.ProfileFragment;
import com.lexoratech.lexorafix.fragments.ServicesFragment;
import com.lexoratech.lexorafix.notifications.NotificationHelper;
import com.lexoratech.lexorafix.services.OrderStatusService;
import com.google.firebase.auth.FirebaseAuth;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;

public class MainActivity extends AppCompatActivity implements SensorEventListener {

    private BottomNavigationView bottomNav;
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private long lastShakeTime = 0;
    private static final int SHAKE_THRESHOLD = 12;
    private static final int SHAKE_COOLDOWN = 2000;
    private static final int PERMISSION_REQUEST_CODE = 100;
    private ListenerRegistration maintenanceListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Fix status bar color
        getWindow().setStatusBarColor(ContextCompat.getColor(this, R.color.colorPrimaryDark));

        // Initialize notification channels
        new NotificationHelper(this);

        // Request notifications permission (Android 13+)
        requestPermissions();

        // Start real-time background listener services
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            startService(new android.content.Intent(this, OrderStatusService.class));
            startService(new android.content.Intent(this, com.lexoratech.lexorafix.services.UserStatusService.class));
        }

        bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setOnItemSelectedListener(item -> {
            Fragment fragment = null;
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                fragment = new HomeFragment();
            } else if (itemId == R.id.nav_categories) {
                fragment = new CategoryFragment();
            } else if (itemId == R.id.nav_cart) {
                fragment = new CartFragment();
            } else if (itemId == R.id.nav_services) {
                fragment = new ServicesFragment();
            } else if (itemId == R.id.nav_profile) {
                fragment = new ProfileFragment();
            }

            if (fragment != null) {
                getSupportFragmentManager()
                        .beginTransaction()
                        .setCustomAnimations(android.R.anim.fade_in, android.R.anim.fade_out)
                        .replace(R.id.fragment_container, fragment)
                        .commit();
                return true;
            }
            return false;
        });

        // Load home fragment by default
        if (savedInstanceState == null) {
            bottomNav.setSelectedItemId(R.id.nav_home);
        }

        // Setup shake sensor
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }
    }

    private void requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this,
                    Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[] { Manifest.permission.POST_NOTIFICATIONS }, PERMISSION_REQUEST_CODE);
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    // Navigate to cart tab programmatically (used by shake gesture previously)
    public void navigateToCart() {
        bottomNav.setSelectedItemId(R.id.nav_cart);
    }

    // Navigate to services tab programmatically (used by shake gesture)
    public void navigateToServices() {
        bottomNav.setSelectedItemId(R.id.nav_services);
    }

    // Navigate to categories tab programmatically
    public void navigateToCategories() {
        bottomNav.setSelectedItemId(R.id.nav_categories);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (accelerometer != null) {
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
        }
        // Start real-time maintenance mode listener
        maintenanceListener = FirebaseFirestore.getInstance()
                .collection("system").document("settings")
                .addSnapshotListener((snapshot, e) -> {
                    if (snapshot != null && snapshot.exists()) {
                        Boolean isMaintenance = snapshot.getBoolean("maintenance_mode");
                        if (isMaintenance != null && isMaintenance) {
                            android.content.Intent intent = new android.content.Intent(MainActivity.this, MaintenanceActivity.class);
                            intent.setFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK | android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK);
                            startActivity(intent);
                            finish();
                        }
                    }
                });
    }

    @Override
    protected void onPause() {
        super.onPause();
        sensorManager.unregisterListener(this);
        if (maintenanceListener != null) {
            maintenanceListener.remove();
            maintenanceListener = null;
        }
    }

    // Shake detection via accelerometer sensor
    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];

            double acceleration = Math.sqrt(x * x + y * y + z * z) - SensorManager.GRAVITY_EARTH;

            if (acceleration > SHAKE_THRESHOLD) {
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastShakeTime > SHAKE_COOLDOWN) {
                    lastShakeTime = currentTime;
                    Toast.makeText(this, "🛠 Shake detected! Opening Services...", Toast.LENGTH_SHORT).show();
                    navigateToServices();
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // Not needed
    }
}
