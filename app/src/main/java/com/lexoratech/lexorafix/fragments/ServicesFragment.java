package com.lexoratech.lexorafix.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
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
import com.lexoratech.lexorafix.models.ServiceBooking;

import java.util.ArrayList;
import java.util.List;

public class ServicesFragment extends Fragment {

    private static final String TAG = "ServicesFragment";
    private final List<ServiceBooking> bookings = new ArrayList<>();
    private BookingAdapter adapter;
    private ListenerRegistration bookingListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_services, container, false);

        CardView cardInstallation = view.findViewById(R.id.card_installation);
        CardView cardRepair = view.findViewById(R.id.card_repair);
        CardView cardMaintenance = view.findViewById(R.id.card_maintenance);
        MaterialButton btnFindCenters = view.findViewById(R.id.btn_find_centers);

        cardInstallation.setOnClickListener(v -> openBooking("Installation"));
        cardRepair.setOnClickListener(v -> openBooking("Repair"));
        cardMaintenance.setOnClickListener(v -> openBooking("Maintenance"));

        btnFindCenters.setOnClickListener(v -> {
            startActivity(new Intent(requireContext(), MapActivity.class));
        });

        // Bookings list with real-time Firestore listener
        RecyclerView rvBookings = view.findViewById(R.id.rv_bookings);
        rvBookings.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new BookingAdapter(requireContext(), bookings);
        rvBookings.setAdapter(adapter);

        // Start real-time listener for bookings from Firestore
        String uid = SharedPreferencesManager.getInstance(requireContext()).getFirebaseUid();
        if (uid != null && !uid.isEmpty()) {
            startBookingListener(uid);
        }

        return view;
    }

    /**
     * Real-time Firestore listener on user's bookings subcollection.
     * Picks up status changes made by admin immediately.
     */
    private void startBookingListener(String uid) {
        bookingListener = FirebaseFirestore.getInstance()
                .collection("users").document(uid)
                .collection("bookings")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((querySnapshot, error) -> {
                    if (error != null) {
                        Log.e(TAG, "Bookings listener error: " + error.getMessage());
                        if (error.getMessage() != null && error.getMessage().contains("PERMISSION_DENIED")) {
                            return;
                        }
                        // Fallback: try without ordering
                        loadBookingsFallback(uid);
                        return;
                    }
                    if (querySnapshot != null && isAdded()) {
                        bookings.clear();
                        for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                            ServiceBooking booking = new ServiceBooking();
                            booking.setServiceType(doc.getString("serviceType") != null
                                    ? doc.getString("serviceType")
                                    : "");
                            booking.setDate(doc.getString("date") != null
                                    ? doc.getString("date")
                                    : "");
                            booking.setTime(doc.getString("time") != null
                                    ? doc.getString("time")
                                    : "");
                            booking.setNotes(doc.getString("notes") != null
                                    ? doc.getString("notes")
                                    : "");
                            booking.setStatus(doc.getString("status") != null
                                    ? doc.getString("status")
                                    : "Pending");
                            bookings.add(booking);
                        }
                        Log.d(TAG, "Loaded " + bookings.size() + " bookings from Firestore");
                        adapter.notifyDataSetChanged();
                    }
                });
    }

    private void loadBookingsFallback(String uid) {
        FirebaseFirestore.getInstance()
                .collection("users").document(uid)
                .collection("bookings")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    if (!isAdded())
                        return;
                    bookings.clear();
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        ServiceBooking booking = new ServiceBooking();
                        booking.setServiceType(doc.getString("serviceType") != null
                                ? doc.getString("serviceType")
                                : "");
                        booking.setDate(doc.getString("date") != null
                                ? doc.getString("date")
                                : "");
                        booking.setTime(doc.getString("time") != null
                                ? doc.getString("time")
                                : "");
                        booking.setStatus(doc.getString("status") != null
                                ? doc.getString("status")
                                : "Pending");
                        bookings.add(booking);
                    }
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> Log.e(TAG, "Fallback load failed: " + e.getMessage()));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (bookingListener != null) {
            bookingListener.remove();
            bookingListener = null;
        }
    }

    private void openBooking(String serviceType) {
        Intent intent = new Intent(requireContext(), BookServiceActivity.class);
        intent.putExtra("service_type", serviceType);
        startActivity(intent);
    }
}
