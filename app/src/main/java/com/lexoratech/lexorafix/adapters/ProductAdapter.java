package com.lexoratech.lexorafix.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.snackbar.Snackbar;
import com.lexoratech.lexorafix.R;
import com.lexoratech.lexorafix.data.DatabaseHelper;
import com.lexoratech.lexorafix.models.CartItem;
import com.lexoratech.lexorafix.models.Product;

import java.util.List;
import java.util.Locale;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ViewHolder> {

    private final List<Product> products;
    private final List<String> wishlist = new java.util.ArrayList<>();
    private final Context context;
    private OnProductClickListener listener;
    private boolean useHorizontalLayout = false;

    public interface OnProductClickListener {
        void onProductClick(Product product);

        void onWishlistClick(Product product, boolean isAdding);
    }

    public ProductAdapter(Context context, List<Product> products) {
        this.context = context;
        this.products = products;
    }

    public ProductAdapter(Context context, List<Product> products, boolean useHorizontalLayout) {
        this.context = context;
        this.products = products;
        this.useHorizontalLayout = useHorizontalLayout;
    }

    public void setOnProductClickListener(OnProductClickListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layoutRes = useHorizontalLayout ? R.layout.item_product_horizontal : R.layout.item_product;
        View view = LayoutInflater.from(context).inflate(layoutRes, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Product product = products.get(position);
        holder.tvName.setText(product.getName());
        if (product.hasVariants() && product.getPrice() == 0 && product.getLowestPrice() > 0) {
            holder.tvPrice.setText(String.format(Locale.getDefault(), "Starting from LKR %.2f", product.getLowestPrice()));
            holder.tvPrice.setTextSize(12); // Slightly smaller for long text if needed, but keeping simple first
        } else {
            holder.tvPrice.setText(String.format(Locale.getDefault(), "LKR %.2f", product.getPrice()));
        }

        int effectiveStock = product.getEffectiveStock();
        if (effectiveStock > 0) {
            holder.tvStock.setText(R.string.in_stock);
            holder.tvStock.setTextColor(context.getResources().getColor(R.color.colorSuccess, null));
        } else {
            holder.tvStock.setText(R.string.out_of_stock);
            holder.tvStock.setTextColor(context.getResources().getColor(R.color.colorError, null));
        }

        // Load product image with Glide
        String imageUrl = product.getImageUrl();
        if (imageUrl != null && !imageUrl.isEmpty()) {
            Glide.with(context)
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_store)
                    .error(R.drawable.ic_store)
                    .centerCrop()
                    .into(holder.ivProduct);
        } else {
            holder.ivProduct.setImageResource(R.drawable.ic_store);
            holder.ivProduct.setScaleType(ImageView.ScaleType.CENTER);
        }

        // Initialize Wishlist State
        boolean isWishlisted = wishlist.contains(product.getDocumentId());
        holder.ivWishlist.setImageResource(isWishlisted ? R.drawable.ic_heart_solid : R.drawable.ic_heart_outline);

        holder.ivWishlist.setOnClickListener(v -> {
            boolean willBeWishlisted = !wishlist.contains(product.getDocumentId());
            if (willBeWishlisted) {
                wishlist.add(product.getDocumentId());
                holder.ivWishlist.setImageResource(R.drawable.ic_heart_solid);
            } else {
                wishlist.remove(product.getDocumentId());
                holder.ivWishlist.setImageResource(R.drawable.ic_heart_outline);
            }

            if (listener != null) {
                listener.onWishlistClick(product, willBeWishlisted);
            }
        });

        holder.itemView.setOnClickListener(v -> {
            if (listener != null)
                listener.onProductClick(product);
        });

        holder.btnAddCart.setOnClickListener(v -> {
            if (com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser() == null) {
                context.startActivity(new android.content.Intent(context, com.lexoratech.lexorafix.activities.LoginActivity.class));
                return;
            }
            
            // If the product has variants, force the user to select them on the Detail view
            if (product.hasVariants()) {
                if (listener != null) {
                    listener.onProductClick(product);
                }
                return;
            }
            
            int stock = product.getEffectiveStock();
            if (stock <= 0) {
                Snackbar.make(v, "Product is out of stock.", Snackbar.LENGTH_SHORT).show();
                return;
            }
            DatabaseHelper db = DatabaseHelper.getInstance(context);
            int userId = 1; // Local DB legacy support
            int currentCartQty = db.getProductQuantityInCart(product.getId(), userId);
            if (currentCartQty >= stock) {
                Snackbar.make(v, "Cannot add more. Only " + stock + " available in stock.", Snackbar.LENGTH_LONG).show();
                return;
            }

            CartItem cartItem = new CartItem(0, product.getId(), product.getName(),
                    product.getPrice(), 1, product.getImageUrl());
            db.addToCart(cartItem, userId);
            Toast.makeText(context, "Added to cart", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public int getItemCount() {
        return products.size();
    }

    public void updateList(List<Product> newProducts) {
        products.clear();
        products.addAll(newProducts);
        notifyDataSetChanged();
    }

    public void updateWishlist(List<String> newWishlist) {
        wishlist.clear();
        if (newWishlist != null) {
            wishlist.addAll(newWishlist);
        }
        notifyDataSetChanged();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView ivProduct, ivWishlist;
        TextView tvName, tvPrice, tvStock;
        MaterialCardView btnAddCart;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            ivProduct = itemView.findViewById(R.id.iv_product);
            ivWishlist = itemView.findViewById(R.id.iv_wishlist);
            tvName = itemView.findViewById(R.id.tv_name);
            tvPrice = itemView.findViewById(R.id.tv_price);
            tvStock = itemView.findViewById(R.id.tv_stock);
            btnAddCart = itemView.findViewById(R.id.btn_add_cart);
        }
    }
}
