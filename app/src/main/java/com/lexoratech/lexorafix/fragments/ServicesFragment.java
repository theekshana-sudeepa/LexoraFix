package com.lexoratech.lexorafix.fragments;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.lexoratech.lexorafix.R;
import com.lexoratech.lexorafix.activities.BookServiceActivity;
import com.lexoratech.lexorafix.activities.MapActivity;
import com.lexoratech.lexorafix.adapters.BookingAdapter;
import com.lexoratech.lexorafix.data.SharedPreferencesManager;
import com.lexoratech.lexorafix.models.ServiceCenter;
import com.lexoratech.lexorafix.adapters.ServiceCenterAdapter;

import java.util.ArrayList;
import java.util.List;

public class ServicesFragment extends Fragment {

    private static final String TAG = "ServicesFragment";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_services, container, false);

        CardView cardInstallation = view.findViewById(R.id.card_installation);
        CardView cardRepair = view.findViewById(R.id.card_repair);
        CardView cardMaintenance = view.findViewById(R.id.card_maintenance);
        View btnFindCenters = view.findViewById(R.id.btn_find_centers);
        RecyclerView rvServiceCenters = view.findViewById(R.id.rv_service_centers);

        cardInstallation.setOnClickListener(v -> openBooking("Installation"));
        cardRepair.setOnClickListener(v -> openBooking("Repair"));
        cardMaintenance.setOnClickListener(v -> openBooking("Maintenance"));

        btnFindCenters.setOnClickListener(v -> {
            startActivity(new Intent(requireContext(), MapActivity.class));
        });

        List<ServiceCenter> centersList = new ArrayList<>();
        ServiceCenterAdapter centerAdapter = new ServiceCenterAdapter(requireContext(), centersList, null);
        rvServiceCenters.setLayoutManager(new LinearLayoutManager(requireContext()));
        rvServiceCenters.setAdapter(centerAdapter);

        FirebaseFirestore.getInstance().collection("service_centers")
                .limit(5)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    for (DocumentSnapshot doc : queryDocumentSnapshots) {
                        try {
                            ServiceCenter center = doc.toObject(ServiceCenter.class);
                            if (center != null) {
                                center.setDocumentId(doc.getId());
                                centersList.add(center);
                            }
                        } catch (Exception e) {
                            Log.e(TAG, "Error parsing center", e);
                        }
                    }
                    centerAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> Log.e(TAG, "Error fetching centers", e));

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    private void openBooking(String serviceType) {
        Intent intent = new Intent(requireContext(), BookServiceActivity.class);
        intent.putExtra("service_type", serviceType);
        startActivity(intent);
    }

    private void saveFcmToken(String uid) {
        com.google.firebase.messaging.FirebaseMessaging.getInstance().getToken()
                .addOnSuccessListener(token -> {
                    FirebaseFirestore.getInstance()
                            .collection("users").document(uid)
                            .update("fcmToken", token)
                            .addOnSuccessListener(v -> Log.d(TAG, "FCM token saved"))
                            .addOnFailureListener(e -> Log.e(TAG, "FCM token save failed: " + e.getMessage()));
                });
    }
}
