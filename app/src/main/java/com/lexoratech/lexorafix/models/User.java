package com.lexoratech.lexorafix.models;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;

/**
 * Represents a registered user.
 * Lombok generates all getters and setters automatically.
 */
@Getter
@Setter
public class User {
    private int id;
    private String name;
    private String email;
    private String password;
    private String phone;
    /** Address Line 1 */
    private String address;
    /** Address Line 2 (optional) */
    private String addressLine2;
    private String province;
    private String district;
    private String city;
    private String postalCode;
    private String firebaseUid;
    /** "customer" or "admin" */
    private String role;
    private String profileImageUrl;
    private List<String> wishlist;
    private String status;

    public User() {
        this.role = "customer";
        this.wishlist = new ArrayList<>();
        this.status = "Active";
    }

    public User(int id, String name, String email, String password, String phone, String address) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.password = password;
        this.phone = phone;
        this.address = address;
        this.role = "customer";
        this.status = "Active";
    }

    // ========== Firestore Serialization ==========

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("name", name);
        map.put("email", email);
        map.put("phone", phone != null ? phone : "");
        map.put("address", address != null ? address : "");
        map.put("addressLine2", addressLine2 != null ? addressLine2 : "");
        map.put("province", province != null ? province : "");
        map.put("district", district != null ? district : "");
        map.put("city", city != null ? city : "");
        map.put("postalCode", postalCode != null ? postalCode : "");
        map.put("role", role != null ? role : "customer");
        map.put("profileImageUrl", profileImageUrl != null ? profileImageUrl : "");
        map.put("wishlist", wishlist != null ? wishlist : new ArrayList<>());
        map.put("status", status != null ? status : "Active");
        return map;
    }

    @SuppressWarnings("unchecked")
    public static User fromMap(Map<String, Object> map) {
        if (map == null)
            return null;
        User user = new User();
        user.setName((String) map.get("name"));
        user.setEmail((String) map.get("email"));
        user.setPhone((String) map.get("phone"));
        user.setAddress((String) map.get("address"));
        user.setAddressLine2((String) map.get("addressLine2"));
        user.setProvince((String) map.get("province"));
        user.setDistrict((String) map.get("district"));
        user.setCity((String) map.get("city"));
        user.setPostalCode((String) map.get("postalCode"));
        user.setRole(map.get("role") != null ? (String) map.get("role") : "customer");
        user.setProfileImageUrl((String) map.get("profileImageUrl"));
        if (map.get("wishlist") != null) {
            user.setWishlist((List<String>) map.get("wishlist"));
        } else {
            user.setWishlist(new ArrayList<>());
        }
        user.setStatus(map.get("status") != null ? (String) map.get("status") : "Active");
        return user;
    }
}
