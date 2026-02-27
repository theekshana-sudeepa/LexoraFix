package com.lexoratech.lexorafix.models;

import java.util.HashMap;
import java.util.Map;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents an item within an order (stored in Firestore order.items[]).
 * Lombok generates all getters and setters automatically.
 */
@Getter
@Setter
@NoArgsConstructor
public class OrderItem {
    private String productId;
    private String productName;
    private double price;
    private int quantity;
    private String imageUrl;

    public OrderItem(String productId, String productName, double price, int quantity, String imageUrl) {
        this.productId = productId;
        this.productName = productName;
        this.price = price;
        this.quantity = quantity;
        this.imageUrl = imageUrl;
    }

    // ========== Firestore Serialization ==========

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("productId", productId != null ? productId : "");
        map.put("productName", productName != null ? productName : "");
        map.put("price", price);
        map.put("quantity", quantity);
        map.put("imageUrl", imageUrl != null ? imageUrl : "");
        return map;
    }

    public static OrderItem fromMap(Map<String, Object> map) {
        if (map == null)
            return null;
        OrderItem item = new OrderItem();
        item.setProductId((String) map.get("productId"));
        item.setProductName((String) map.get("productName"));
        Object priceObj = map.get("price");
        if (priceObj instanceof Number)
            item.setPrice(((Number) priceObj).doubleValue());
        Object qtyObj = map.get("quantity");
        if (qtyObj instanceof Number)
            item.setQuantity(((Number) qtyObj).intValue());
        item.setImageUrl((String) map.get("imageUrl"));
        return item;
    }

    /** Derived — price × quantity */
    public double getSubtotal() {
        return price * quantity;
    }
}
