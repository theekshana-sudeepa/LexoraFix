package com.lexoratech.lexorafix.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.material.button.MaterialButton;
import com.lexoratech.lexorafix.R;
import com.lexoratech.lexorafix.activities.EditProfileActivity;
import com.lexoratech.lexorafix.activities.LoginActivity;
import com.lexoratech.lexorafix.data.FirebaseAuthHelper;
import com.lexoratech.lexorafix.data.FirestoreHelper;
import com.lexoratech.lexorafix.data.SharedPreferencesManager;
import com.lexoratech.lexorafix.models.User;

import com.google.android.material.switchmaterial.SwitchMaterial;
import com.lexoratech.lexorafix.utils.BiometricHelper;
import com.lexoratech.lexorafix.utils.ThemeUtils;

public class ProfileFragment extends Fragment {

    private static final String TAG = "ProfileFragment";
    private boolean isBiometricUpdating = false; // guard against listener re-entry

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

        // ── Theme Toggle Setup ──
        View cardThemeToggle = view.findViewById(R.id.card_theme_toggle);
        TextView tvThemeStatus = view.findViewById(R.id.tv_theme_status);
        if (cardThemeToggle != null && tvThemeStatus != null) {
            updateThemeStatusText(tvThemeStatus, ThemeUtils.getThemeMode(requireContext()));
            cardThemeToggle.setOnClickListener(v2 -> showThemePickerDialog(tvThemeStatus));
        }

        SharedPreferencesManager prefs = SharedPreferencesManager.getInstance(requireContext());

        // ── Guest Mode Detection ──
        com.google.firebase.auth.FirebaseUser currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        boolean isGuest = (currentUser == null);

        if (isGuest) {
            tvName.setText("Guest User");
            tvEmail.setText("Sign in to access your account");
            if (tvPhone != null) tvPhone.setText("—");
            if (tvAddress != null) tvAddress.setText("—");
            btnEditProfile.setVisibility(View.GONE);

            // Hide user-specific account rows
            View btnWishlist = view.findViewById(R.id.btn_wishlist);
            View btnOrders = view.findViewById(R.id.btn_orders);
            View btnBookings = view.findViewById(R.id.btn_bookings);
            View cardBiometricLogin = view.findViewById(R.id.card_biometric_login);
            if (btnWishlist != null) btnWishlist.setVisibility(View.GONE);
            if (btnOrders != null) btnOrders.setVisibility(View.GONE);
            if (btnBookings != null) btnBookings.setVisibility(View.GONE);
            if (cardBiometricLogin != null) cardBiometricLogin.setVisibility(View.GONE);

            // Convert logout button to login button
            btnLogout.setText("Login / Sign Up");
            btnLogout.setIconResource(R.drawable.ic_profile);
            btnLogout.setOnClickListener(v -> {
                Intent intent = new Intent(requireContext(), LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                if (getActivity() != null) getActivity().finish();
            });

            return view;
        }

        // ── Logged-in user flow below ──
        tvName.setText(prefs.getUserName());
        tvEmail.setText(prefs.getUserEmail());

        // ── Wishlist Navigation ──
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

        // ── My Orders Navigation ──
        View btnOrders = view.findViewById(R.id.btn_orders);
        if (btnOrders != null) {
            btnOrders.setOnClickListener(v -> {
                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .setCustomAnimations(android.R.anim.slide_in_left, android.R.anim.slide_out_right,
                                android.R.anim.slide_in_left, android.R.anim.slide_out_right)
                        .replace(R.id.fragment_container, new OrderHistoryFragment())
                        .addToBackStack(null)
                        .commit();
            });
        }

        // ── My Bookings Navigation ──
        View btnBookings = view.findViewById(R.id.btn_bookings);
        if (btnBookings != null) {
            btnBookings.setOnClickListener(v -> {
                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .setCustomAnimations(android.R.anim.slide_in_left, android.R.anim.slide_out_right,
                                android.R.anim.slide_in_left, android.R.anim.slide_out_right)
                        .replace(R.id.fragment_container, new BookingsFragment())
                        .addToBackStack(null)
                        .commit();
            });
        }

        // ── Biometric Toggle Setup ──
        SwitchMaterial switchBiometric = view.findViewById(R.id.switch_biometric);
        if (switchBiometric != null) {
            boolean isAvailable = BiometricHelper.isBiometricAvailable(requireContext());
            View cardBiometricLogin = view.findViewById(R.id.card_biometric_login);

            if (!isAvailable) {
                if (cardBiometricLogin != null) cardBiometricLogin.setVisibility(View.GONE);
            } else {
                switchBiometric.setChecked(prefs.isBiometricEnabled());
                switchBiometric.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    if (isBiometricUpdating) return; // guard: skip programmatic changes

                    if (isChecked) {
                        // User trying to enable – verify first
                        isBiometricUpdating = true;
                        buttonView.setChecked(false); // keep off until verified
                        isBiometricUpdating = false;

                        BiometricHelper.showBiometricPrompt(requireActivity(),
                                "Enable Biometric Login",
                                "Verify your identity to enable biometric authentication.",
                                new BiometricHelper.BiometricCallback() {
                                    @Override
                                    public void onSuccess() {
                                        prefs.setBiometricEnabled(true);
                                        isBiometricUpdating = true;
                                        buttonView.setChecked(true);
                                        isBiometricUpdating = false;
                                        Toast.makeText(requireContext(), "Biometric Login enabled",
                                                Toast.LENGTH_SHORT).show();
                                    }

                                    @Override
                                    public void onError(String error) {
                                        Toast.makeText(requireContext(), "Verification failed: " + error,
                                                Toast.LENGTH_LONG).show();
                                    }

                                    @Override
                                    public void onCancelled() {
                                        Toast.makeText(requireContext(), "Verification cancelled",
                                                Toast.LENGTH_SHORT).show();
                                    }
                                });
                    } else {
                        // User turning it off
                        prefs.setBiometricEnabled(false);
                    }
                });
            }
        }

        // ── Logout ──
        btnLogout.setOnClickListener(v -> {
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
        if (getView() == null) return;

        SharedPreferencesManager prefs = SharedPreferencesManager.getInstance(requireContext());
        String uid = prefs.getFirebaseUid();

        // ── Re-sync biometric switch state on every resume ──
        SwitchMaterial switchBiometric = getView().findViewById(R.id.switch_biometric);
        if (switchBiometric != null) {
            isBiometricUpdating = true;
            switchBiometric.setChecked(prefs.isBiometricEnabled());
            isBiometricUpdating = false;
        }

        if (uid != null && !uid.isEmpty()) {
            FirestoreHelper.getInstance().getUserProfile(uid, new FirestoreHelper.FirestoreCallback<User>() {
                @Override
                public void onSuccess(User user) {
                    if (!isAdded() || getView() == null) return;

                    TextView tvName = getView().findViewById(R.id.tv_name);
                    TextView tvPhone = getView().findViewById(R.id.tv_phone);
                    TextView tvAddress = getView().findViewById(R.id.tv_address);
                    android.widget.ImageView ivAvatar = getView().findViewById(R.id.iv_avatar);

                    if (user.getName() != null && !user.getName().isEmpty() && tvName != null) {
                        tvName.setText(user.getName());
                    }
                    if (tvPhone != null)
                        tvPhone.setText(user.getPhone() != null && !user.getPhone().isEmpty()
                                ? user.getPhone() : "Not set");
                    if (tvAddress != null)
                        tvAddress.setText(user.getAddress() != null && !user.getAddress().isEmpty()
                                ? user.getAddress() : "Not set");

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

                    if (user.getProfileImageUrl() != null && !user.getProfileImageUrl().isEmpty()
                            && ivAvatar != null) {
                        ivAvatar.setPadding(0, 0, 0, 0);
                        ivAvatar.setBackground(null);
                        ivAvatar.setImageTintList(null);
                        ivAvatar.clearColorFilter();
                        com.bumptech.glide.Glide.with(requireContext())
                                .load(user.getProfileImageUrl())
                                .circleCrop()
                                .placeholder(R.drawable.ic_profile)
                                .into(ivAvatar);
                    }
                }

                @Override
                public void onError(String message) {
                    if (!isAdded() || getView() == null) return;
                    TextView tvPhone = getView().findViewById(R.id.tv_phone);
                    TextView tvAddress = getView().findViewById(R.id.tv_address);
                    if (tvPhone != null) tvPhone.setText("Not set");
                    if (tvAddress != null) tvAddress.setText("Not set");
                }
            });
        }
    }

    private void updateThemeStatusText(TextView tvThemeStatus, int mode) {
        if (tvThemeStatus == null) return;
        switch (mode) {
            case ThemeUtils.THEME_LIGHT:
                tvThemeStatus.setText("Light Theme");
                break;
            case ThemeUtils.THEME_DARK:
                tvThemeStatus.setText("Dark Theme");
                break;
            case ThemeUtils.THEME_SYSTEM:
            default:
                tvThemeStatus.setText("System Default");
                break;
        }
    }

    private void showThemePickerDialog(TextView tvThemeStatus) {
        com.google.android.material.bottomsheet.BottomSheetDialog bottomSheetDialog =
                new com.google.android.material.bottomsheet.BottomSheetDialog(requireContext());
        android.view.View dialogView = android.view.LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_theme_selector, null);
        bottomSheetDialog.setContentView(dialogView);

        android.view.View bottomSheet = bottomSheetDialog
                .findViewById(com.google.android.material.R.id.design_bottom_sheet);
        if (bottomSheet != null) {
            bottomSheet.setBackgroundResource(android.R.color.transparent);
        }

        android.widget.LinearLayout llSystem = dialogView.findViewById(R.id.ll_theme_system);
        android.widget.LinearLayout llLight = dialogView.findViewById(R.id.ll_theme_light);
        android.widget.LinearLayout llDark = dialogView.findViewById(R.id.ll_theme_dark);

        android.widget.ImageView ivCheckSystem = dialogView.findViewById(R.id.iv_check_system);
        android.widget.ImageView ivCheckLight = dialogView.findViewById(R.id.iv_check_light);
        android.widget.ImageView ivCheckDark = dialogView.findViewById(R.id.iv_check_dark);

        int currentSelection = ThemeUtils.getThemeMode(requireContext());

        if (currentSelection == ThemeUtils.THEME_SYSTEM)
            ivCheckSystem.setVisibility(android.view.View.VISIBLE);
        else if (currentSelection == ThemeUtils.THEME_LIGHT)
            ivCheckLight.setVisibility(android.view.View.VISIBLE);
        else if (currentSelection == ThemeUtils.THEME_DARK)
            ivCheckDark.setVisibility(android.view.View.VISIBLE);

        android.view.View.OnClickListener listener = v -> {
            int selectedTheme = ThemeUtils.THEME_SYSTEM;
            if (v.getId() == R.id.ll_theme_light) selectedTheme = ThemeUtils.THEME_LIGHT;
            else if (v.getId() == R.id.ll_theme_dark) selectedTheme = ThemeUtils.THEME_DARK;

            ThemeUtils.setThemeMode(requireContext(), selectedTheme);
            updateThemeStatusText(tvThemeStatus, selectedTheme);
            ThemeUtils.applyTheme(requireContext());
            bottomSheetDialog.dismiss();

            if (getActivity() != null) {
                getActivity().recreate();
            }
        };

        llSystem.setOnClickListener(listener);
        llLight.setOnClickListener(listener);
        llDark.setOnClickListener(listener);

        bottomSheetDialog.show();
    }
}
