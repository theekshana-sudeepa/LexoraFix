package com.lexoratech.lexorafix.admin.adapters;

import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.textfield.TextInputEditText;
import com.lexoratech.lexorafix.admin.R;

import java.util.List;
import java.util.Map;

/**
 * Adapter for managing product variants in the admin Add/Edit Product form.
 * Each item contains editable fields for label, price, stock, and SKU.
 */
public class VariantAdapter extends RecyclerView.Adapter<VariantAdapter.VariantViewHolder> {

    /**
     * Simple data holder for variant fields.
     * We use a plain map-like structure to keep it decoupled from the customer
     * app's model.
     */
    public static class VariantData {
        public String label = "";
        public String price = "";
        public String stock = "";
        public String sku = "";

        public VariantData() {
        }

        public VariantData(String label, double price, int stock, String sku) {
            this.label = label != null ? label : "";
            this.price = String.valueOf(price);
            this.stock = String.valueOf(stock);
            this.sku = sku != null ? sku : "";
        }
    }

    private final List<VariantData> variants;

    public VariantAdapter(List<VariantData> variants) {
        this.variants = variants;
    }

    @NonNull
    @Override
    public VariantViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_variant, parent, false);
        return new VariantViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VariantViewHolder holder, int position) {
        VariantData data = variants.get(position);
        holder.bind(data, position);
    }

    @Override
    public int getItemCount() {
        return variants.size();
    }

    public List<VariantData> getVariants() {
        return variants;
    }

    class VariantViewHolder extends RecyclerView.ViewHolder {
        TextInputEditText etLabel, etPrice, etStock, etSku;
        ImageButton btnDelete;

        // TextWatchers we need to remove before rebinding
        TextWatcher labelWatcher, priceWatcher, stockWatcher, skuWatcher;

        VariantViewHolder(@NonNull View itemView) {
            super(itemView);
            etLabel = itemView.findViewById(R.id.et_variant_label);
            etPrice = itemView.findViewById(R.id.et_variant_price);
            etStock = itemView.findViewById(R.id.et_variant_stock);
            etSku = itemView.findViewById(R.id.et_variant_sku);
            btnDelete = itemView.findViewById(R.id.btn_delete_variant);
        }

        void bind(VariantData data, int position) {
            // Remove old watchers to prevent infinite loops during rebind
            if (labelWatcher != null)
                etLabel.removeTextChangedListener(labelWatcher);
            if (priceWatcher != null)
                etPrice.removeTextChangedListener(priceWatcher);
            if (stockWatcher != null)
                etStock.removeTextChangedListener(stockWatcher);
            if (skuWatcher != null)
                etSku.removeTextChangedListener(skuWatcher);

            etLabel.setText(data.label);
            etPrice.setText(data.price);
            etStock.setText(data.stock);
            etSku.setText(data.sku);

            // Create new watchers
            labelWatcher = simpleWatcher(s -> data.label = s);
            priceWatcher = simpleWatcher(s -> data.price = s);
            stockWatcher = simpleWatcher(s -> data.stock = s);
            skuWatcher = simpleWatcher(s -> data.sku = s);

            etLabel.addTextChangedListener(labelWatcher);
            etPrice.addTextChangedListener(priceWatcher);
            etStock.addTextChangedListener(stockWatcher);
            etSku.addTextChangedListener(skuWatcher);

            btnDelete.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && pos < variants.size()) {
                    variants.remove(pos);
                    notifyItemRemoved(pos);
                    notifyItemRangeChanged(pos, variants.size());
                }
            });
        }

        private TextWatcher simpleWatcher(java.util.function.Consumer<String> setter) {
            return new TextWatcher() {
                @Override
                public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                }

                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                }

                @Override
                public void afterTextChanged(Editable s) {
                    setter.accept(s.toString());
                }
            };
        }
    }
}
