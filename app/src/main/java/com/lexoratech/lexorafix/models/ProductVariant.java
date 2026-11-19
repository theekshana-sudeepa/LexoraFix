package com.lexoratech.lexorafix.models;

import java.util.HashMap;
import java.util.Map;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents a product variant combination (e.g., label = "12V / 35Ah").
 * Each variant can override the base product's price and stock.
 * Lombok generates all getters and setters automatically.
 */
@Getter
@Setter
@NoArgsConstructor
public class ProductVariant {
    /** e.g., "Black", "5W-30", "12V / 35Ah" */
    private String label;
    private double price;
    private int stock;
    /** e.g., "BAT-BLK-45" */
    private String sku;

    public ProductVariant(String label, double price, int stock, String sku) {
        this.label = label;
        this.price = price;
        this.stock = stock;
        this.sku = sku;
    }

    // ========== Firestore Serialization ==========

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("label", label != null ? label : "");
        map.put("price", price);
        map.put("stock", stock);
        map.put("sku", sku != null ? sku : "");
        return map;
    }

    @SuppressWarnings("unchecked")
    public static ProductVariant fromMap(Map<String, Object> map) {
        if (map == null)
            return null;
        ProductVariant v = new ProductVariant();
        v.setLabel((String) map.get("label"));
        Object priceObj = map.get("price");
        if (priceObj instanceof Number)
            v.setPrice(((Number) priceObj).doubleValue());
        Object stockObj = map.get("stock");
        if (stockObj instanceof Number)
            v.setStock(((Number) stockObj).intValue());
        v.setSku((String) map.get("sku"));
        return v;
    }
}
