package com.lexoratech.lexorafix.models;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;

/**
 * Represents a customer order.
 * Lombok generates all getters and setters automatically.
 */
@Getter
@Setter
public class Order {
    private int id;
    /** Firestore document ID */
    private String documentId;
    private String orderId;
    private int userId;
    private String firebaseUid;
    private String userName;
    private String userEmail;
    private String userPhone;
    private double totalPrice;
    private String address;
    private String status;
    private String timestamp;
    private int itemCount;
    /** Order line items */
    private List<Map<String, Object>> items;

    public Order() {
        this.items = new ArrayList<>();
    }

    public Order(int id, String orderId, int userId, double totalPrice, String address,
            String status, String timestamp, int itemCount) {
        this.id = id;
        this.orderId = orderId;
        this.userId = userId;
        this.totalPrice = totalPrice;
        this.address = address;
        this.status = status;
        this.timestamp = timestamp;
        this.itemCount = itemCount;
        this.items = new ArrayList<>();
    }

    // ========== Firestore Serialization ==========

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("orderId", orderId != null ? orderId : "");
        map.put("firebaseUid", firebaseUid != null ? firebaseUid : "");
        map.put("userName", userName != null ? userName : "");
        map.put("userEmail", userEmail != null ? userEmail : "");
        map.put("userPhone", userPhone != null ? userPhone : "");
        map.put("totalPrice", totalPrice);
        map.put("address", address != null ? address : "");
        map.put("status", status != null ? status : "Pending");
        map.put("itemCount", itemCount);
        map.put("items", items != null ? items : new ArrayList<>());
        map.put("timestamp", com.google.firebase.Timestamp.now());
        return map;
    }

    @SuppressWarnings("unchecked")
    public static Order fromMap(Map<String, Object> map) {
        if (map == null)
            return null;
        Order order = new Order();
        order.setOrderId((String) map.get("orderId"));
        order.setFirebaseUid((String) map.get("firebaseUid"));
        order.setUserName((String) map.get("userName"));
        order.setUserEmail((String) map.get("userEmail"));
        order.setUserPhone((String) map.get("userPhone"));
        Object priceObj = map.get("totalPrice");
        if (priceObj instanceof Number)
            order.setTotalPrice(((Number) priceObj).doubleValue());
        order.setAddress((String) map.get("address"));
        order.setStatus((String) map.get("status"));
        Object countObj = map.get("itemCount");
        if (countObj instanceof Number)
            order.setItemCount(((Number) countObj).intValue());
        Object itemsObj = map.get("items");
        if (itemsObj instanceof List)
            order.setItems((List<Map<String, Object>>) itemsObj);
        Object tsObj = map.get("timestamp");
        if (tsObj instanceof com.google.firebase.Timestamp) {
            order.setTimestamp(((com.google.firebase.Timestamp) tsObj).toDate().toString());
        } else if (tsObj instanceof String) {
            order.setTimestamp((String) tsObj);
        }
        return order;
    }
}
