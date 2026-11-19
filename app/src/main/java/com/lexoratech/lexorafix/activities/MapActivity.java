package com.lexoratech.lexorafix.activities;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.lexoratech.lexorafix.R;
import com.lexoratech.lexorafix.adapters.ServiceCenterAdapter;
import com.lexoratech.lexorafix.models.ServiceCenter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Shows an interactive map with the user's location and nearby service centers.
 * Uses the native Google Maps SDK for Android.
 */
public class MapActivity extends AppCompatActivity implements OnMapReadyCallback {

    private static final String TAG = "MapActivity";
    private static final int LOCATION_PERMISSION_REQUEST = 102;
    private TextView tvLocation;
    private TextView tvCenterCount;
    private GoogleMap mMap;
    private List<ServiceCenter> centers = new ArrayList<>();
    private ServiceCenterAdapter adapter;
    private Location userLocation = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);

        ImageView btnBack = findViewById(R.id.btn_back);
        tvLocation = findViewById(R.id.tv_location);
        tvCenterCount = findViewById(R.id.tv_center_count);
        RecyclerView rvCenters = findViewById(R.id.rv_centers);

        btnBack.setOnClickListener(v -> finish());

        // Setup RecyclerView
        rvCenters.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ServiceCenterAdapter(this, centers, null);
        rvCenters.setAdapter(adapter);

        // Fetch centers from Firestore, then request location to render map
        fetchServiceCentersFromFirestore();

        // Obtain the SupportMapFragment and get notified when the map is ready to be
        // used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map_fragment);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
    }

    private void fetchServiceCentersFromFirestore() {
        FirebaseFirestore.getInstance().collection("service_centers")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    centers.clear();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Map<String, Object> data = doc.getData();
                        if (data != null) {
                            ServiceCenter center = ServiceCenter.fromMap(data);
                            if (center != null) {
                                center.setDocumentId(doc.getId());
                                centers.add(center);
                            }
                        }
                    }
                    // Sort by distance if we already have user location
                    sortCentersByDistance();
                    adapter.notifyDataSetChanged();
                    updateCenterCount();
                    updateMapMarkers();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to load service centers.", Toast.LENGTH_SHORT).show();
                });
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        mMap = googleMap;

        // Initial fallback position (Colombo)
        LatLng colombo = new LatLng(6.9271, 79.8612);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(colombo, 11));

        requestLocation();
        updateMapMarkers();
    }

    private void requestLocation() {
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

        if (mMap != null) {
            mMap.setMyLocationEnabled(true);
            mMap.getUiSettings().setMyLocationButtonEnabled(true);
        }

        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (locationManager == null) {
            Log.e(TAG, "LocationManager not available");
            return;
        }

        // Try to get last known location first
        Location lastKnown = null;
        try {
            lastKnown = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (lastKnown == null) {
                lastKnown = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting last known location: " + e.getMessage());
        }

        if (lastKnown != null) {
            handleNewLocation(lastKnown);
        } else {
            // Request fresh location
            LocationListener locationListener = new LocationListener() {
                @Override
                public void onLocationChanged(@NonNull Location location) {
                    handleNewLocation(location);
                    locationManager.removeUpdates(this);
                }

                @Override
                public void onProviderDisabled(@NonNull String provider) {
                }
            };

            try {
                if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                            1000, 10, locationListener);
                }
                if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                    locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER,
                            1000, 10, locationListener);
                }
            } catch (Exception e) {
                Log.e(TAG, "Error requesting location updates: " + e.getMessage());
            }
        }
    }

    private void handleNewLocation(Location location) {
        userLocation = location;
        tvLocation.setText(String.format(Locale.getDefault(),
                "%.4f, %.4f", location.getLatitude(), location.getLongitude()));

        if (mMap != null) {
            LatLng userLatLng = new LatLng(location.getLatitude(), location.getLongitude());
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 13));
        }

        // Update adapter with user location for distance calculation
        adapter.setUserLocation(location);

        // Re-sort centers by distance
        sortCentersByDistance();
        adapter.notifyDataSetChanged();
    }

    private void sortCentersByDistance() {
        if (userLocation == null || centers.isEmpty()) return;

        Collections.sort(centers, (a, b) -> {
            float[] distA = new float[1];
            float[] distB = new float[1];
            Location.distanceBetween(userLocation.getLatitude(), userLocation.getLongitude(),
                    a.getLatitude(), a.getLongitude(), distA);
            Location.distanceBetween(userLocation.getLatitude(), userLocation.getLongitude(),
                    b.getLatitude(), b.getLongitude(), distB);
            return Float.compare(distA[0], distB[0]);
        });
    }

    private void updateCenterCount() {
        if (tvCenterCount != null) {
            int count = centers.size();
            tvCenterCount.setText(count + (count == 1 ? " center" : " centers"));
        }
    }

    private void updateMapMarkers() {
        if (mMap == null || centers.isEmpty())
            return;

        mMap.clear();

        for (ServiceCenter center : centers) {
            if (center.getLatitude() != 0 && center.getLongitude() != 0) {
                LatLng pos = new LatLng(center.getLatitude(), center.getLongitude());
                mMap.addMarker(new MarkerOptions()
                        .position(pos)
                        .title(center.getName())
                        .snippet(center.getAddress() + " | 📞 " + center.getPhone())
                        .icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE)));
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
            @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            requestLocation();
        } else {
            tvLocation.setText("Location permission denied");
        }
    }
}
