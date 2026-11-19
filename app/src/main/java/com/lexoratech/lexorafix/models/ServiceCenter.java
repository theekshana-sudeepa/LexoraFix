package com.lexoratech.lexorafix.models;

import java.util.HashMap;
import java.util.Map;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents a physical service center location.
 * Lombok generates all getters and setters automatically.
 */
@Getter
@Setter
@NoArgsConstructor
public class ServiceCenter {
    /** Firestore Document ID */
    private String documentId;
    /** Legacy SQLite ID if needed */
    private int id;
    private String name;
    private String address;
    private String phone;
    private double latitude;
    private double longitude;
    private String description;

    public ServiceCenter(int id, String name, String address, String phone,
            double latitude, double longitude, String description) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.phone = phone;
        this.latitude = latitude;
        this.longitude = longitude;
        this.description = description;
    }

    // ========== Firestore Serialization ==========

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("name", name);
        map.put("address", address);
        map.put("phone", phone);
        map.put("latitude", latitude);
        map.put("longitude", longitude);
        map.put("description", description);
        return map;
    }

    public static ServiceCenter fromMap(Map<String, Object> map) {
        if (map == null)
            return null;
        ServiceCenter center = new ServiceCenter();

        if (map.containsKey("id") && map.get("id") != null) {
            Object idObj = map.get("id");
            if (idObj instanceof Long)
                center.setId(((Long) idObj).intValue());
            else if (idObj instanceof Integer)
                center.setId((Integer) idObj);
            else if (idObj instanceof String) {
                try {
                    center.setId(Integer.parseInt((String) idObj));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        if (map.containsKey("name"))
            center.setName((String) map.get("name"));
        if (map.containsKey("address"))
            center.setAddress((String) map.get("address"));
        if (map.containsKey("phone"))
            center.setPhone((String) map.get("phone"));
        if (map.containsKey("description"))
            center.setDescription((String) map.get("description"));

        if (map.containsKey("latitude") && map.get("latitude") != null) {
            Object latObj = map.get("latitude");
            if (latObj instanceof Double)
                center.setLatitude((Double) latObj);
            else if (latObj instanceof Long)
                center.setLatitude(((Long) latObj).doubleValue());
            else if (latObj instanceof String) {
                try {
                    center.setLatitude(Double.parseDouble((String) latObj));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        if (map.containsKey("longitude") && map.get("longitude") != null) {
            Object lngObj = map.get("longitude");
            if (lngObj instanceof Double)
                center.setLongitude((Double) lngObj);
            else if (lngObj instanceof Long)
                center.setLongitude(((Long) lngObj).doubleValue());
            else if (lngObj instanceof String) {
                try {
                    center.setLongitude(Double.parseDouble((String) lngObj));
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return center;
    }
}
