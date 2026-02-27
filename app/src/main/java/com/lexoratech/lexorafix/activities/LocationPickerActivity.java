package com.lexoratech.lexorafix.activities;

import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.os.Bundle;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.lexoratech.lexorafix.R;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class LocationPickerActivity extends AppCompatActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private TextView tvSelectedAddress;
    private MaterialButton btnConfirm;

    private LatLng currentCenter;
    private String currentAddress = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_picker);

        tvSelectedAddress = findViewById(R.id.tv_selected_address);
        btnConfirm = findViewById(R.id.btn_confirm_location);
        MaterialCardView btnBackCard = findViewById(R.id.btn_back_card);

        btnBackCard.setOnClickListener(v -> finish());

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        btnConfirm.setOnClickListener(v -> {
            if (currentCenter != null) {
                Intent resultIntent = new Intent();
                resultIntent.putExtra("latitude", currentCenter.latitude);
                resultIntent.putExtra("longitude", currentCenter.longitude);
                resultIntent.putExtra("address", currentAddress);
                setResult(RESULT_OK, resultIntent);
                finish();
            }
        });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        // Default location (e.g., Colombo, Sri Lanka)
        LatLng defaultLocation = new LatLng(6.9271, 79.8612);

        // Check if initial coords passed
        double initialLat = getIntent().getDoubleExtra("initial_lat", 0);
        double initialLng = getIntent().getDoubleExtra("initial_lng", 0);
        if (initialLat != 0 && initialLng != 0) {
            defaultLocation = new LatLng(initialLat, initialLng);
        }

        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 15f));

        mMap.setOnCameraIdleListener(() -> {
            currentCenter = mMap.getCameraPosition().target;
            updateAddressForLocation(currentCenter);
        });

        mMap.setOnCameraMoveStartedListener(reason -> {
            tvSelectedAddress.setText("Moving...");
            btnConfirm.setEnabled(false);
        });
    }

    private void updateAddressForLocation(LatLng location) {
        tvSelectedAddress.setText("Locating...");
        new Thread(() -> {
            try {
                Geocoder geocoder = new Geocoder(this, Locale.getDefault());
                List<Address> addresses = geocoder.getFromLocation(location.latitude, location.longitude, 1);
                if (addresses != null && !addresses.isEmpty()) {
                    Address addr = addresses.get(0);
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i <= addr.getMaxAddressLineIndex(); i++) {
                        sb.append(addr.getAddressLine(i));
                        if (i < addr.getMaxAddressLineIndex())
                            sb.append(", ");
                    }
                    currentAddress = sb.toString();
                    runOnUiThread(() -> {
                        tvSelectedAddress.setText(currentAddress);
                        btnConfirm.setEnabled(true);
                    });
                } else {
                    currentAddress = "Unknown Location (Drag map to refine)";
                    runOnUiThread(() -> {
                        tvSelectedAddress.setText(currentAddress);
                        btnConfirm.setEnabled(true);
                    });
                }
            } catch (IOException e) {
                e.printStackTrace();
                currentAddress = "Unknown Location (Drag map to refine)";
                runOnUiThread(() -> {
                    tvSelectedAddress.setText(currentAddress);
                    btnConfirm.setEnabled(true);
                });
            }
        }).start();
    }
}
