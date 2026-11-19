package com.lexoratech.lexorafix.models;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents an item in the user's shopping cart.
 * Lombok generates all getters and setters automatically.
 */
@Getter
@Setter
@NoArgsConstructor
public class CartItem {
    private int id;
    private int productId;
    private String productName;
    private double price;
    private int quantity;
    private String imageUrl;
    /** Firestore product document ID for stock updates */
    private String firestoreDocId;
    /** Selected variant label (e.g., "Black", "45Ah") */
    private String variantLabel;

    public CartItem(int id, int productId, String productName, double price, int quantity, String imageUrl) {
        this.id = id;
        this.productId = productId;
        this.productName = productName;
        this.price = price;
        this.quantity = quantity;
        this.imageUrl = imageUrl;
    }

    /** Derived — not stored; calculated from price × quantity */
    public double getSubtotal() {
        return price * quantity;
    }
}
