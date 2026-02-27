package com.lexoratech.lexorafix.fragments;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.lexoratech.lexorafix.R;
import com.lexoratech.lexorafix.activities.CheckoutActivity;
import com.lexoratech.lexorafix.adapters.CartAdapter;
import com.lexoratech.lexorafix.data.DatabaseHelper;
import com.lexoratech.lexorafix.data.SharedPreferencesManager;
import com.lexoratech.lexorafix.models.CartItem;

import java.util.List;
import java.util.Locale;

public class CartFragment extends Fragment implements CartAdapter.OnCartActionListener {

    private RecyclerView rvCart;
    private LinearLayout layoutEmpty;
    private View layoutBottom;
    private TextView tvTotal;
    private TextView tvItemCount;
    private CartAdapter adapter;
    private List<CartItem> cartItems;
    private DatabaseHelper db;
    private int userId;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_cart, container, false);

        rvCart = view.findViewById(R.id.rv_cart);
        layoutEmpty = view.findViewById(R.id.layout_empty);
        layoutBottom = view.findViewById(R.id.layout_bottom);
        tvTotal = view.findViewById(R.id.tv_total);
        tvItemCount = view.findViewById(R.id.tv_item_count);
        MaterialButton btnCheckout = view.findViewById(R.id.btn_checkout);
        MaterialButton btnBrowse = view.findViewById(R.id.btn_browse);

        db = DatabaseHelper.getInstance(requireContext());
        userId = 1; // Local DB legacy support

        rvCart.setLayoutManager(new LinearLayoutManager(requireContext()));

        loadCart();

        btnCheckout.setOnClickListener(v -> {
            if (cartItems != null && !cartItems.isEmpty()) {
                startActivity(new Intent(requireContext(), CheckoutActivity.class));
            }
        });

        btnBrowse.setOnClickListener(v -> {
            // Navigate to home tab
            if (getActivity() != null) {
                getActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.fragment_container, new HomeFragment())
                        .commit();
            }
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadCart();
    }

    private void loadCart() {
        cartItems = db.getCartItems(userId);

        if (cartItems.isEmpty()) {
            rvCart.setVisibility(View.GONE);
            layoutEmpty.setVisibility(View.VISIBLE);
            layoutBottom.setVisibility(View.GONE);
        } else {
            rvCart.setVisibility(View.VISIBLE);
            layoutEmpty.setVisibility(View.GONE);
            layoutBottom.setVisibility(View.VISIBLE);

            adapter = new CartAdapter(requireContext(), cartItems);
            adapter.setOnCartActionListener(this);
            rvCart.setAdapter(adapter);
            updateTotal();

            fetchStockLimits(adapter);
        }
    }

    private void fetchStockLimits(CartAdapter adapter) {
        com.google.firebase.firestore.FirebaseFirestore firestore = com.google.firebase.firestore.FirebaseFirestore
                .getInstance();
        java.util.Map<String, Integer> stockMap = new java.util.HashMap<>();

        for (CartItem item : cartItems) {
            firestore.collection("products")
                    .whereEqualTo("name", item.getProductName())
                    .limit(1)
                    .get()
                    .addOnSuccessListener(snapshot -> {
                        if (!snapshot.isEmpty()) {
                            Number stockNum = snapshot.getDocuments().get(0).getLong("stock");
                            int stock = stockNum != null ? stockNum.intValue() : 0;
                            stockMap.put(item.getProductName(), stock);
                            adapter.setStockMap(stockMap);
                        }
                    });
        }
    }

    private void updateTotal() {
        double total = 0;
        int itemCount = 0;
        for (CartItem item : cartItems) {
            total += item.getSubtotal();
            itemCount += item.getQuantity();
        }
        tvTotal.setText(String.format(Locale.getDefault(), "LKR %.2f", total));

        if (tvItemCount != null) {
            tvItemCount.setText(itemCount + (itemCount == 1 ? " item" : " items"));
        }
    }

    @Override
    public void onQuantityChanged(CartItem item, int newQuantity) {
        if (newQuantity > item.getQuantity()) {
            // Increase requested: Check Firestore stock first
            com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    .collection("products")
                    .whereEqualTo("name", item.getProductName())
                    .limit(1)
                    .get()
                    .addOnSuccessListener(snapshot -> {
                        if (!snapshot.isEmpty()) {
                            Number stockNum = snapshot.getDocuments().get(0).getLong("stock");
                            int stock = stockNum != null ? stockNum.intValue() : 0;
                            if (newQuantity > stock) {
                                android.widget.Toast.makeText(requireContext(),
                                        "Cannot add more. Only " + stock + " available in stock.",
                                        android.widget.Toast.LENGTH_SHORT).show();
                            } else {
                                db.updateCartItemQuantity(item.getId(), newQuantity);
                                loadCart();
                            }
                        } else {
                            android.widget.Toast
                                    .makeText(requireContext(), "Product not found.", android.widget.Toast.LENGTH_SHORT)
                                    .show();
                        }
                    })
                    .addOnFailureListener(e -> android.widget.Toast.makeText(requireContext(),
                            "Failed to check stock.", android.widget.Toast.LENGTH_SHORT).show());
        } else {
            // Decrease requested
            db.updateCartItemQuantity(item.getId(), newQuantity);
            loadCart();
        }
    }

    @Override
    public void onRemoveItem(CartItem item) {
        db.removeCartItem(item.getId());
        loadCart();
    }
}
