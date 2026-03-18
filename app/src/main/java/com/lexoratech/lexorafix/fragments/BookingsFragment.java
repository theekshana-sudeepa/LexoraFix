package com.lexoratech.lexorafix.fragments;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.ChipGroup;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.lexoratech.lexorafix.R;
import com.lexoratech.lexorafix.adapters.BookingAdapter;
import com.lexoratech.lexorafix.data.SharedPreferencesManager;
import com.lexoratech.lexorafix.models.ServiceBooking;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class BookingsFragment extends Fragment {

    private static final String TAG = "BookingsFragment";
    private ListenerRegistration bookingListener;
    private BookingAdapter adapter;
    private final List<ServiceBooking> allBookings = new ArrayList<>();
    private final List<ServiceBooking> displayedBookings = new ArrayList<>();
    private String currentSort = "newest";

    private RecyclerView rvBookings;
    private LinearLayout layoutEmpty;
    private ProgressBar progressBar;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_bookings, container, false);

        // Action bar
        ImageView btnBack = view.findViewById(R.id.btn_back);
        TextView tvToolbarTitle = view.findViewById(R.id.tv_toolbar_title);
        if (tvToolbarTitle != null) tvToolbarTitle.setText("My Bookings");
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                if (getParentFragmentManager().getBackStackEntryCount() > 0) {
                    getParentFragmentManager().popBackStack();
                }
            });
        }

        rvBookings = view.findViewById(R.id.rv_bookings);
        layoutEmpty = view.findViewById(R.id.layout_empty);
        progressBar = view.findViewById(R.id.progress_bar);

        rvBookings.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new BookingAdapter(requireContext(), displayedBookings);
        rvBookings.setAdapter(adapter);

        // Sort chips
        ChipGroup chipGroup = view.findViewById(R.id.chip_group_sort);
        chipGroup.setOnCheckedStateChangeListener((group, checkedIds) -> {
            if (checkedIds.isEmpty()) return;
            int id = checkedIds.get(0);
            if (id == R.id.chip_newest) currentSort = "newest";
            else if (id == R.id.chip_oldest) currentSort = "oldest";
            else if (id == R.id.chip_by_status) currentSort = "status";
            applySort();
        });

        // Show loading
        progressBar.setVisibility(View.VISIBLE);

        // Start listening
        String uid = SharedPreferencesManager.getInstance(requireContext()).getFirebaseUid();
        if (uid != null && !uid.isEmpty()) {
            startBookingListener(uid);
        } else {
            progressBar.setVisibility(View.GONE);
            showEmptyState();
        }

        return view;
    }

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
                        progressBar.setVisibility(View.GONE);
                        return;
                    }
                    if (querySnapshot != null && isAdded()) {
                        progressBar.setVisibility(View.GONE);
                        allBookings.clear();
                        for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                            ServiceBooking booking = new ServiceBooking();
                            booking.setServiceType(doc.getString("serviceType") != null
                                    ? doc.getString("serviceType") : "");
                            booking.setDate(doc.getString("date") != null
                                    ? doc.getString("date") : "");
                            booking.setTime(doc.getString("time") != null
                                    ? doc.getString("time") : "");
                            booking.setNotes(doc.getString("notes") != null
                                    ? doc.getString("notes") : "");
                            booking.setStatus(doc.getString("status") != null
                                    ? doc.getString("status") : "Pending");
                            booking.setAssignedWorkerName(doc.getString("assignedWorkerName"));
                            booking.setAssignedWorkerPhone(doc.getString("assignedWorkerPhone"));
                            booking.setAssignedWorkerEmail(doc.getString("assignedWorkerEmail"));
                            allBookings.add(booking);
                        }
                        applySort();
                    }
                });
    }

    private void applySort() {
        displayedBookings.clear();
        displayedBookings.addAll(allBookings);

        switch (currentSort) {
            case "oldest":
                Collections.reverse(displayedBookings);
                break;
            case "status":
                Collections.sort(displayedBookings, Comparator.comparing(ServiceBooking::getStatus));
                break;
            case "newest":
            default:
                // Already in descending order from Firestore
                break;
        }
        adapter.notifyDataSetChanged();

        if (displayedBookings.isEmpty()) {
            showEmptyState();
        } else {
            layoutEmpty.setVisibility(View.GONE);
            rvBookings.setVisibility(View.VISIBLE);
        }
    }

    private void showEmptyState() {
        rvBookings.setVisibility(View.GONE);
        layoutEmpty.setVisibility(View.VISIBLE);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (bookingListener != null) {
            bookingListener.remove();
            bookingListener = null;
        }
    }
}
