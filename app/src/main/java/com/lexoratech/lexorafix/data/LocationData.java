package com.lexoratech.lexorafix.data;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Static utility providing Sri Lankan provinces and their districts
 * for cascading dropdown selection.
 */
public final class LocationData {

    private static final LinkedHashMap<String, String[]> PROVINCE_DISTRICTS = new LinkedHashMap<>();

    static {
        PROVINCE_DISTRICTS.put("Western", new String[] {
                "Colombo", "Gampaha", "Kalutara"
        });
        PROVINCE_DISTRICTS.put("Central", new String[] {
                "Kandy", "Matale", "Nuwara Eliya"
        });
        PROVINCE_DISTRICTS.put("Southern", new String[] {
                "Galle", "Matara", "Hambantota"
        });
        PROVINCE_DISTRICTS.put("Northern", new String[] {
                "Jaffna", "Kilinochchi", "Mannar", "Mullaitivu", "Vavuniya"
        });
        PROVINCE_DISTRICTS.put("Eastern", new String[] {
                "Batticaloa", "Ampara", "Trincomalee"
        });
        PROVINCE_DISTRICTS.put("North Western", new String[] {
                "Kurunegala", "Puttalam"
        });
        PROVINCE_DISTRICTS.put("North Central", new String[] {
                "Anuradhapura", "Polonnaruwa"
        });
        PROVINCE_DISTRICTS.put("Uva", new String[] {
                "Badulla", "Monaragala"
        });
        PROVINCE_DISTRICTS.put("Sabaragamuwa", new String[] {
                "Ratnapura", "Kegalle"
        });
    }

    private LocationData() {
        /* utility class */ }

    /** Returns all 9 province names. */
    public static String[] getProvinces() {
        return PROVINCE_DISTRICTS.keySet().toArray(new String[0]);
    }

    /** Returns districts for the given province, or empty array if not found. */
    public static String[] getDistricts(String province) {
        if (province == null)
            return new String[0];
        String[] districts = PROVINCE_DISTRICTS.get(province);
        return districts != null ? districts : new String[0];
    }
}
