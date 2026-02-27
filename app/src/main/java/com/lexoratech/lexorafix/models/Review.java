package com.lexoratech.lexorafix.models;

import com.google.firebase.Timestamp;

import java.util.HashMap;
import java.util.Map;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents a product review.
 * Lombok generates all getters and setters automatically.
 */
@Getter
@Setter
@NoArgsConstructor
public class Review {
    private String id;
    private String userId;
    private String userName;
    private float rating;
    private String comment;
    private Timestamp timestamp;

    public Review(String userId, String userName, float rating, String comment, Timestamp timestamp) {
        this.userId = userId;
        this.userName = userName;
        this.rating = rating;
        this.comment = comment;
        this.timestamp = timestamp;
    }

    // ========== Firestore Serialization ==========

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("userId", userId);
        map.put("userName", userName);
        map.put("rating", rating);
        map.put("comment", comment);
        map.put("timestamp", timestamp);
        return map;
    }

    public static Review fromMap(Map<String, Object> map) {
        if (map == null)
            return null;
        Review r = new Review();
        r.setUserId((String) map.get("userId"));
        r.setUserName((String) map.get("userName"));
        Object rateObj = map.get("rating");
        if (rateObj instanceof Number)
            r.setRating(((Number) rateObj).floatValue());
        r.setComment((String) map.get("comment"));
        Object timeObj = map.get("timestamp");
        if (timeObj instanceof Timestamp)
            r.setTimestamp((Timestamp) timeObj);
        return r;
    }
}
