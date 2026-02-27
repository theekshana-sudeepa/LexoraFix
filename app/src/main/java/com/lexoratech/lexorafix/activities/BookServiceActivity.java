package com.lexoratech.lexorafix.activities;

import android.Manifest;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.lexoratech.lexorafix.R;
import com.lexoratech.lexorafix.data.DatabaseHelper;
import com.lexoratech.lexorafix.data.FirestoreHelper;
import com.lexoratech.lexorafix.data.SharedPreferencesManager;
import com.lexoratech.lexorafix.models.ServiceBooking;
import com.lexoratech.lexorafix.notifications.NotificationHelper;

import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class BookServiceActivity extends AppCompatActivity {

    private static final int CALL_PERMISSION_REQUEST = 101;
    private static final int LOCATION_PERMISSION_REQUEST = 103;
    private TextInputEditText etDate, etTime, etNotes;
    private TextInputLayout tilDate, tilTime;
    private String serviceType;

    // User location for booking
    private double userLatitude = 0;
    private double userLongitude = 0;
    private String userLocationAddress = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_book_service);

        serviceType = getIntent().getStringExtra("service_type");
        if (serviceType == null)
            serviceType = "General";

        TextView tvTitle = findViewById(R.id.tv_title);
        TextView tvServiceType = findViewById(R.id.tv_service_type);
        ImageView btnBack = findViewById(R.id.btn_back);
        tilDate = findViewById(R.id.til_date);
        tilTime = findViewById(R.id.til_time);
        etDate = findViewById(R.id.et_date);
        etTime = findViewById(R.id.et_time);
        etNotes = findViewById(R.id.et_notes);
        MaterialButton btnCall = findViewById(R.id.btn_call);
        MaterialButton btnBook = findViewById(R.id.btn_book);

        TextView tvSelectedLocation = findViewById(R.id.tv_selected_location);
        MaterialButton btnCurrentLocation = findViewById(R.id.btn_current_location);
        MaterialButton btnChooseMap = findViewById(R.id.btn_choose_map);

        tvTitle.setText("Book " + serviceType);
        tvServiceType.setText(serviceType + " Service");

        btnBack.setOnClickListener(v -> finish());

        // Date picker
        etDate.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            new DatePickerDialog(this, (view, year, month, day) -> {
                etDate.setText(String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, day));
            }, cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)).show();
        });

        // Time picker
        etTime.setOnClickListener(v -> {
            Calendar cal = Calendar.getInstance();
            new TimePickerDialog(this, (view, hour, minute) -> {
                etTime.setText(String.format(Locale.getDefault(), "%02d:%02d", hour, minute));
            }, cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true).show();
        });

        // Location selection
        btnCurrentLocation.setOnClickListener(v -> {
            tvSelectedLocation.setText("Locating...");
            captureUserLocation();
        });

        btnChooseMap.setOnClickListener(v -> {
            Intent intent = new Intent(this, LocationPickerActivity.class);
            if (userLatitude != 0 && userLongitude != 0) {
                intent.putExtra("initial_lat", userLatitude);
                intent.putExtra("initial_lng", userLongitude);
            }
            startActivityForResult(intent, 105); // Request code for map picking
        });

        // Call service center (telephony feature)
        btnCall.setOnClickListener(v -> makePhoneCall());

        // Book service
        btnBook.setOnClickListener(v -> attemptBooking());
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 105 && resultCode == RESULT_OK && data != null) {
            userLatitude = data.getDoubleExtra("latitude", 0);
            userLongitude = data.getDoubleExtra("longitude", 0);
            userLocationAddress = data.getStringExtra("address");

            TextView tvSelectedLocation = findViewById(R.id.tv_selected_location);
            tvSelectedLocation.setText(userLocationAddress != null ? userLocationAddress : "Unknown Location");
        }
    }

    private void attemptBooking() {
        tilDate.setError(null);
        tilTime.setError(null);

        String date = etDate.getText() != null ? etDate.getText().toString().trim() : "";
        String time = etTime.getText() != null ? etTime.getText().toString().trim() : "";
        String notes = etNotes.getText() != null ? etNotes.getText().toString().trim() : "";

        if (TextUtils.isEmpty(date)) {
            tilDate.setError("Please select a date");
            return;
        }
        if (TextUtils.isEmpty(time)) {
            tilTime.setError("Please select a time");
            return;
        }

        int userId = 1; // Local DB legacy support
        ServiceBooking booking = new ServiceBooking(0, userId, "", serviceType, date, time, notes, "Confirmed", "");

        DatabaseHelper db = DatabaseHelper.getInstance(this);
        long id = db.addBooking(booking);
        android.util.Log.d("BookService", "Local DB booking ID: " + id);

        if (id > 0) {
            // Sync booking to Firestore — dual-write for admin visibility
            SharedPreferencesManager prefs = SharedPreferencesManager.getInstance(this);
            String firebaseUid = prefs.getFirebaseUid();
            android.util.Log.d("BookService", "Firebase UID: " + firebaseUid);

            if (firebaseUid != null && !firebaseUid.isEmpty()) {
                // CRITICAL: Check if Firebase Auth session is still active
                com.google.firebase.auth.FirebaseUser currentUser = com.google.firebase.auth.FirebaseAuth.getInstance()
                        .getCurrentUser();
                android.util.Log.d("BookService", "Firebase currentUser: " +
                        (currentUser != null ? currentUser.getUid() : "NULL — AUTH SESSION EXPIRED!"));

                if (currentUser == null) {
                    android.util.Log.e("BookService", "⚠️ Firebase Auth session is NULL! " +
                            "SharedPrefs has UID but Firebase Auth token expired. " +
                            "Firestore writes will be REJECTED by security rules.");
                    Toast.makeText(this, "⚠️ Session expired. Please log out and log in again.",
                            Toast.LENGTH_LONG).show();
                }

                String bookingDocId = "BK-" + System.currentTimeMillis();
                android.util.Log.d("BookService", "Using booking doc ID: " + bookingDocId);

                java.util.Map<String, Object> bookingData = new java.util.HashMap<>();
                bookingData.put("serviceType", serviceType);
                bookingData.put("date", date);
                bookingData.put("time", time);
                bookingData.put("notes", notes);
                bookingData.put("status", "Confirmed");
                bookingData.put("firebaseUid", firebaseUid);
                bookingData.put("userName", prefs.getUserName() != null ? prefs.getUserName() : "");
                bookingData.put("userEmail", prefs.getUserEmail() != null ? prefs.getUserEmail() : "");
                bookingData.put("timestamp", com.google.firebase.Timestamp.now());

                // Include user location
                if (userLatitude != 0 && userLongitude != 0) {
                    bookingData.put("latitude", userLatitude);
                    bookingData.put("longitude", userLongitude);
                    bookingData.put("locationAddress", userLocationAddress);
                    android.util.Log.d("BookService", "Location: " + userLatitude + ", " + userLongitude);
                }

                android.util.Log.d("BookService", "Writing to Firestore: bookings/" + bookingDocId);

                // Write DIRECTLY to top-level bookings collection first (for admin)
                com.google.firebase.firestore.FirebaseFirestore.getInstance()
                        .collection("bookings").document(bookingDocId)
                        .set(bookingData)
                        .addOnSuccessListener(aVoid -> {
                            android.util.Log.d("BookService", "✅ TOP-LEVEL booking written: bookings/" + bookingDocId);
                            Toast.makeText(BookServiceActivity.this, "☁️ Synced to cloud", Toast.LENGTH_SHORT).show();
                            // Also write to user subcollection
                            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                                    .collection("users").document(firebaseUid)
                                    .collection("bookings").document(bookingDocId)
                                    .set(bookingData)
                                    .addOnSuccessListener(aVoid2 -> android.util.Log.d("BookService",
                                            "✅ USER subcollection booking written"))
                                    .addOnFailureListener(e -> android.util.Log.e("BookService",
                                            "❌ User subcollection FAILED: " + e.getMessage()));
                        })
                        .addOnFailureListener(e -> {
                            android.util.Log.e("BookService", "❌ TOP-LEVEL booking FAILED: " + e.getMessage());
                            android.util.Log.e("BookService", "Error class: " + e.getClass().getSimpleName());
                            runOnUiThread(() -> Toast.makeText(BookServiceActivity.this,
                                    "❌ Cloud sync failed: " + e.getMessage(),
                                    Toast.LENGTH_LONG).show());
                        });
            } else {
                android.util.Log.e("BookService", "No Firebase UID — booking NOT synced to cloud");
                Toast.makeText(this, "⚠️ Booking saved locally but not synced (no login)",
                        Toast.LENGTH_LONG).show();
            }

            NotificationHelper notificationHelper = new NotificationHelper(this);
            notificationHelper.showBookingConfirmation(String.valueOf(id), serviceType);

            Toast.makeText(this, "Booking confirmed! 🔧", Toast.LENGTH_LONG).show();
            finish();
        } else {
            Toast.makeText(this, "Failed to create booking", Toast.LENGTH_SHORT).show();
        }
    }

    @SuppressWarnings("MissingPermission")
    private void captureUserLocation() {
        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[] { Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION },
                    LOCATION_PERMISSION_REQUEST);
            return;
        }

        // Use Android's built-in LocationManager (avoids SecurityException from GMS)
        LocationManager locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        if (locationManager == null)
            return;

        Location location = null;
        try {
            location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (location == null) {
                location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            }
        } catch (Exception e) {
            android.util.Log.e("BookService", "Location error: " + e.getMessage());
        }

        if (location != null) {
            userLatitude = location.getLatitude();
            userLongitude = location.getLongitude();
            android.util.Log.d("BookService", "Got location: " + userLatitude + ", " + userLongitude);

            // Try to get address from coordinates
            try {
                Geocoder geocoder = new Geocoder(this, Locale.getDefault());
                List<Address> addresses = geocoder.getFromLocation(userLatitude, userLongitude, 1);
                if (addresses != null && !addresses.isEmpty()) {
                    Address addr = addresses.get(0);
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i <= addr.getMaxAddressLineIndex(); i++) {
                        sb.append(addr.getAddressLine(i));
                        if (i < addr.getMaxAddressLineIndex())
                            sb.append(", ");
                    }
                    userLocationAddress = sb.toString();
                    android.util.Log.d("BookService", "Address: " + userLocationAddress);

                    runOnUiThread(() -> {
                        TextView tvSelectedLocation = findViewById(R.id.tv_selected_location);
                        if (tvSelectedLocation != null) {
                            tvSelectedLocation.setText(userLocationAddress);
                        }
                    });
                } else {
                    runOnUiThread(() -> {
                        TextView tvSelectedLocation = findViewById(R.id.tv_selected_location);
                        if (tvSelectedLocation != null) {
                            tvSelectedLocation.setText("Location found, but address unknown");
                        }
                    });
                }
            } catch (Exception e) {
                android.util.Log.e("BookService", "Geocoding failed: " + e.getMessage());
                runOnUiThread(() -> {
                    TextView tvSelectedLocation = findViewById(R.id.tv_selected_location);
                    if (tvSelectedLocation != null) {
                        tvSelectedLocation.setText("Lat: " + userLatitude + ", Lng: " + userLongitude);
                    }
                });
            }
        } else {
            android.util.Log.w("BookService", "Location is null — will book without location");
        }
    }

    private void makePhoneCall() {
        String phoneNumber = "tel:+94112345678"; // Service center number
        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED) {
            Intent callIntent = new Intent(Intent.ACTION_CALL, Uri.parse(phoneNumber));
            startActivity(callIntent);
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[] { Manifest.permission.CALL_PHONE }, CALL_PERMISSION_REQUEST);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == CALL_PERMISSION_REQUEST && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            makePhoneCall();
        } else if (requestCode == LOCATION_PERMISSION_REQUEST && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            captureUserLocation();
        } else if (requestCode == CALL_PERMISSION_REQUEST) {
            // Fallback to dialer
            Intent dialIntent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:+94112345678"));
            startActivity(dialIntent);
        }
    }
}
