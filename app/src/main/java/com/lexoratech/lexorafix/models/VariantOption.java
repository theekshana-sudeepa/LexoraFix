package com.lexoratech.lexorafix.models;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;

/**
 * Represents one variant "dimension" for a product.
 * E.g. name="Voltage", values=["12V", "24V"]
 * name="Capacity", values=["35Ah", "55Ah", "75Ah"]
 *
 * Stored in Firestore under products/{id}/variantOptions.
 * Full combinations are stored in variants[] as ProductVariant with
 * label = "12V / 35Ah" (values joined by " / " in option order).
 *
 * Lombok generates all getters and setters automatically.
 */
@Getter
@Setter
public class VariantOption {
    /** e.g., "Voltage" */
    private String name;
    /** e.g., ["12V", "24V"] */
    private List<String> values;

    public VariantOption() {
        this.values = new ArrayList<>();
    }

    public VariantOption(String name, List<String> values) {
        this.name = name;
        this.values = values != null ? values : new ArrayList<>();
    }

    // ========== Firestore Serialization ==========

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("name", name != null ? name : "");
        map.put("values", values != null ? values : new ArrayList<>());
        return map;
    }

    @SuppressWarnings("unchecked")
    public static VariantOption fromMap(Map<String, Object> map) {
        if (map == null)
            return null;
        VariantOption opt = new VariantOption();
        Object nameObj = map.get("name");
        opt.setName(nameObj != null ? nameObj.toString() : "");
        Object valuesObj = map.get("values");
        if (valuesObj instanceof List) {
            List<String> vals = new ArrayList<>();
            for (Object v : (List<?>) valuesObj) {
                if (v != null)
                    vals.add(v.toString());
            }
            opt.setValues(vals);
        }
        return opt;
    }
}
