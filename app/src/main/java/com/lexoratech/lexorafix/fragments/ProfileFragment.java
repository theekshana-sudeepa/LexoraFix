package com.lexoratech.lexorafix.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;
import com.lexoratech.lexorafix.R;
import com.lexoratech.lexorafix.activities.EditProfileActivity;
import com.lexoratech.lexorafix.activities.LoginActivity;
import com.lexoratech.lexorafix.adapters.OrderAdapter;
import com.lexoratech.lexorafix.data.FirebaseAuthHelper;
import com.lexoratech.lexorafix.data.FirestoreHelper;
import com.lexoratech.lexorafix.data.SharedPreferencesManager;
import com.lexoratech.lexorafix.models.Order;
import com.lexoratech.lexorafix.models.User;
import com.lexoratech.lexorafix.notifications.NotificationHelper;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.android.material.materialswitch.MaterialSwitch;
import com.lexoratech.lexorafix.utils.BiometricHelper;

public class ProfileFragment extends Fragment {

    private static final String TAG = "ProfileFragment";
    private ListenerRegistration orderListener;
    private OrderAdapter adapter;
    private final List<Order> orders = new ArrayList<>();
    // Track previous statuses to detect changes
    private final Map<String, String> previousStatuses = new HashMap<>();
    private boolean isFirstLoad = true;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        TextView tvName = view.findViewById(R.id.tv_name);
        TextView tvEmail = view.findViewById(R.id.tv_email);
        TextView tvPhone = view.findViewById(R.id.tv_phone);
        TextView tvAddress = view.findViewById(R.id.tv_address);
        MaterialButton btnLogout = view.findViewById(R.id.btn_logout);
        MaterialButton btnEditProfile = view.findViewById(R.id.btn_edit_profile);
        android.widget.ImageView ivAvatar = view.findViewById(R.id.iv_avatar);
        View cardContactInfo = view.findViewById(R.id.card_contact_info);

        // Start premium entry animations
        if (ivAvatar != null) {
            ivAvatar.startAnimation(
                    android.view.animation.AnimationUtils.loadAnimation(requireContext(), R.anim.pulse));
        }
        if (cardContactInfo != null) {
            cardContactInfo.startAnimation(
                    android.view.animation.AnimationUtils.loadAnimation(requireContext(), R.anim.slide_up));
        }

        btnEditProfile.setOnClickListener(v -> startActivity(new Intent(requireContext(), EditProfileActivity.class)));

        SharedPreferencesManager prefs = SharedPreferencesManager.getInstance(requireContext());
        tvName.setText(prefs.getUserName());
        tvEmail.setText(prefs.getUserEmail());

        // Initial load happens in onResume so we removed the duplicate call here

        // Wishlist Navigation
        View btnWishlist = view.findViewById(R.id.btn_wishlist);
        if (btnWishlist != null) {
            btnWishlist.setOnClickListener(v -> {
                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .setCustomAnimations(android.R.anim.slide_in_left, android.R.anim.slide_out_right,
                                android.R.anim.slide_in_left, android.R.anim.slide_out_right)
                        .replace(R.id.fragment_container, new WishlistFragment())
                        .addToBackStack(null)
                        .commit();
            });
        }

        // Biometric Toggle Setup
        MaterialSwitch switchBiometric = view.findViewById(R.id.switch_biometric);
        if (switchBiometric != null) {
            boolean isAvailable = BiometricHelper.isBiometricAvailable(requireContext());
            View cardBiometricLogin = view.findViewById(R.id.card_biometric_login);
            
            if (!isAvailable) {
                if (cardBiometricLogin != null) cardBiometricLogin.setVisibility(View.GONE);
            } else {
                switchBiometric.setChecked(prefs.isBiometricEnabled());
                switchBiometric.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    if (isChecked) {
                        // User trying to enable, we must verify them first
                        buttonView.setChecked(false); // keep it off until verified
                        BiometricHelper.showBiometricPrompt(requireActivity(), "Enable Biometric Login", "Verify your identity to enable biometric authentication.", new BiometricHelper.BiometricCallback() {
                            @Override
                            public void onSuccess() {
                                prefs.setBiometricEnabled(true);
                                buttonView.setChecked(true); // actually turn it on
                                Toast.makeText(requireContext(), "Biometric Login enabled", Toast.LENGTH_SHORT).show();
                            }

                            @Override
                            public void onError(String error) {
                                Toast.makeText(requireContext(), "Verification failed: " + error, Toast.LENGTH_LONG).show();
                            }

                            @Override
                            public void onCancelled() {
                                Toast.makeText(requireContext(), "Verification cancelled", Toast.LENGTH_SHORT).show();
                            }
                        });
                    } else {
                        // User turning it off
                        prefs.setBiometricEnabled(false);
                    }
                });
            }
        }

        // Order history — from Firestore with real-time updates + notifications
        RecyclerView rvOrders = view.findViewById(R.id.rv_orders);
        rvOrders.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new OrderAdapter(requireContext(), orders);
        rvOrders.setAdapter(adapter);

        // Start real-time listener for orders from Firestore
        String uid = prefs.getFirebaseUid();
        if (uid != null && !uid.isEmpty()) {
            startOrderListener(uid);
        }

        btnLogout.setOnClickListener(v -> {
            // Stop background services BEFORE revoking auth to prevent Permission Denied
            // errors
            requireContext().stopService(
                    new Intent(requireContext(), com.lexoratech.lexorafix.services.OrderStatusService.class));
            requireContext().stopService(
                    new Intent(requireContext(), com.lexoratech.lexorafix.services.UserStatusService.class));

            FirebaseAuthHelper.getInstance().signOut();
            prefs.logout();
            Intent intent = new Intent(requireContext(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            if (getActivity() != null)
                getActivity().finish();
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getView() == null)
            return;

        SharedPreferencesManager prefs = SharedPreferencesManager.getInstance(requireContext());
        String uid = prefs.getFirebaseUid();

        if (uid != null && !uid.isEmpty()) {
            FirestoreHelper.getInstance().getUserProfile(uid, new FirestoreHelper.FirestoreCallback<User>() {
                @Override
                public void onSuccess(User user) {
                    if (!isAdded() || getView() == null)
                        return;

                    TextView tvName = getView().findViewById(R.id.tv_name);
                    TextView tvPhone = getView().findViewById(R.id.tv_phone);
                    TextView tvAddress = getView().findViewById(R.id.tv_address);
                    android.widget.ImageView ivAvatar = getView().findViewById(R.id.iv_avatar);

                    if (user.getName() != null && !user.getName().isEmpty() && tvName != null) {
                        tvName.setText(user.getName());
                    }
                    if (tvPhone != null)
                        tvPhone.setText(
                                user.getPhone() != null && !user.getPhone().isEmpty() ? user.getPhone() : "Not set");
                    if (tvAddress != null)
                        tvAddress.setText(
                                user.getAddress() != null && !user.getAddress().isEmpty() ? user.getAddress()
                                        : "Not set");

                    // Province / District display
                    TextView tvProvinceDistrict = getView().findViewById(R.id.tv_province_district);
                    if (tvProvinceDistrict != null) {
                        String province = user.getProvince();
                        String district = user.getDistrict();
                        boolean hasProvince = province != null && !province.isEmpty();
                        boolean hasDistrict = district != null && !district.isEmpty();
                        if (hasProvince && hasDistrict) {
                            tvProvinceDistrict.setText(province + ", " + district);
                        } else if (hasProvince) {
                            tvProvinceDistrict.setText(province);
                        } else {
                            tvProvinceDistrict.setText("Not set");
                        }
                    }

                    if (user.getProfileImageUrl() != null && !user.getProfileImageUrl().isEmpty() && ivAvatar != null) {
                        ivAvatar.setPadding(0, 0, 0, 0);
                        ivAvatar.setBackground(null);
                        ivAvatar.setImageTintList(null);
                        ivAvatar.clearColorFilter(); // Explicitly clear any color filters applied via XML
                        com.bumptech.glide.Glide.with(requireContext())
                                .load(user.getProfileImageUrl())
                                .circleCrop()
                                .placeholder(R.drawable.ic_profile)
                                .into(ivAvatar);
                    }
                }

                @Override
                public void onError(String message) {
                    if (!isAdded() || getView() == null)
                        return;
                    TextView tvPhone = getView().findViewById(R.id.tv_phone);
                    TextView tvAddress = getView().findViewById(R.id.tv_address);
                    if (tvPhone != null)
                        tvPhone.setText("Not set");
                    if (tvAddress != null)
                        tvAddress.setText("Not set");
                }
            });
        }
    }

    private void startOrderListener(String uid) {
        orderListener = FirestoreHelper.getInstance().listenForUserOrders(uid,
                new FirestoreHelper.FirestoreCallback<QuerySnapshot>() {
                    @Override
                    public void onSuccess(QuerySnapshot result) {
                        if (!isAdded())
                            return;

                        // Detect status changes and show notifications
                        if (!isFirstLoad) {
                            for (DocumentChange change : result.getDocumentChanges()) {
                                if (change.getType() == DocumentChange.Type.MODIFIED) {
                                    DocumentSnapshot doc = change.getDocument();
                                    String docId = doc.getId();
                                    String newStatus = doc.getString("status");
                                    String orderId = doc.getString("orderId");
                                    String prevStatus = previousStatuses.get(docId);

                                    if (newStatus != null && prevStatus != null
                                            && !newStatus.equals(prevStatus)) {
                                        Log.d(TAG, "Order status changed: " + orderId +
                                                " " + prevStatus + " → " + newStatus);
                                        // Show notification
                                        NotificationHelper helper = new NotificationHelper(requireContext());
                                        helper.showOrderStatusUpdate(
                                                orderId != null ? orderId : docId, newStatus);
                                    }
                                }
                            }
                        }

                        // Update order list and cache statuses
                        orders.clear();
                        previousStatuses.clear();
                        for (DocumentSnapshot doc : result.getDocuments()) {
                            Map<String, Object> data = doc.getData();
                            if (data != null) {
                                Order order = Order.fromMap(data);
                                if (order != null) {
                                    order.setDocumentId(doc.getId());
                                    orders.add(order);
                                    // Cache status for next change detection
                                    String status = doc.getString("status");
                                    if (status != null) {
                                        previousStatuses.put(doc.getId(), status);
                                    }
                                }
                            }
                        }
                        adapter.notifyDataSetChanged();
                        isFirstLoad = false;
                    }

                    @Override
                    public void onError(String message) {
                        if (!isAdded())
                            return;

                        // Ignore permission denied errors during logout
                        if (message != null && message.contains("PERMISSION_DENIED")) {
                            return;
                        }

                        Toast.makeText(requireContext(),
                                "Failed to load orders: " + message, Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (orderListener != null) {
            orderListener.remove();
            orderListener = null;
        }
    }
}
