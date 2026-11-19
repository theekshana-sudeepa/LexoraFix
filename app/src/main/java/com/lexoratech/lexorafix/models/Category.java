package com.lexoratech.lexorafix.models;

import java.util.HashMap;
import java.util.Map;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents a product category.
 * Lombok generates all getters and setters automatically.
 */
@Getter
@Setter
@NoArgsConstructor
public class Category {
    private int id;
    /** Firestore document ID */
    private String documentId;
    private String name;
    private String iconName;
    private String description;
    private String imageUrl;

    public Category(int id, String name, String iconName, String description) {
        this.id = id;
        this.name = name;
        this.iconName = iconName;
        this.description = description;
    }

    // ========== Firestore Serialization ==========

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("name", name != null ? name : "");
        map.put("iconName", iconName != null ? iconName : "");
        map.put("description", description != null ? description : "");
        map.put("imageUrl", imageUrl != null ? imageUrl : "");
        return map;
    }

    public static Category fromMap(Map<String, Object> map) {
        if (map == null)
            return null;
        Category c = new Category();
        c.setName((String) map.get("name"));
        c.setIconName((String) map.get("iconName"));
        c.setDescription((String) map.get("description"));
        c.setImageUrl((String) map.get("imageUrl"));
        return c;
    }
}
