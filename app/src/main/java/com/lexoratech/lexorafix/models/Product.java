package com.lexoratech.lexorafix.models;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;

/**
 * Represents a product in the catalogue.
 *
 * Lombok generates standard getters/setters for all fields.
 * NOTE: setDocumentId() is manually defined below — it contains
 * business logic (auto-assigning integer id from hash) that Lombok
 * cannot replicate, so we suppress Lombok's setter for that field.
 */
@Getter
@Setter
public class Product {
    private int id;
    /** Firestore document ID */
    @Setter(lombok.AccessLevel.NONE) // suppress Lombok setter — custom one below
    private String documentId;
    private String name;
    private String description;
    private double price;
    private String imageUrl;
    private int categoryId;
    /** Firestore category document ID */
    private String categoryDocId;
    private int stock;
    private String videoUrl;
    private boolean featured;
    private List<String> imageUrls;
    private List<ProductVariant> variants;
    private List<VariantOption> variantOptions;

    public Product() {
        this.imageUrls = new ArrayList<>();
        this.variants = new ArrayList<>();
        this.variantOptions = new ArrayList<>();
    }

    public Product(int id, String name, String description, double price, String imageUrl,
            int categoryId, int stock, String videoUrl, boolean featured) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
        this.imageUrl = imageUrl;
        this.categoryId = categoryId;
        this.stock = stock;
        this.videoUrl = videoUrl;
        this.featured = featured;
        this.imageUrls = new ArrayList<>();
        this.variants = new ArrayList<>();
        this.variantOptions = new ArrayList<>();
        if (imageUrl != null && !imageUrl.isEmpty()) {
            this.imageUrls.add(imageUrl);
        }
    }

    /**
     * Custom setter: assigns a deterministic integer ID based on the Firestore
     * Document ID for legacy SQLite compatibility (carts, favorites).
     */
    public void setDocumentId(String documentId) {
        this.documentId = documentId;
        if (documentId != null && this.id == 0) {
            this.id = Math.abs(documentId.hashCode());
        }
    }

    /** @return true if this product has any pre-built variant combinations */
    public boolean hasVariants() {
        return variants != null && !variants.isEmpty();
    }

    /** @return true if this product has any variant option dimensions */
    public boolean hasVariantOptions() {
        return variantOptions != null && !variantOptions.isEmpty();
    }

    /**
     * Returns the real available stock, accounting for variants.
     * - Products WITH variants: sum of all variant stocks (base stock is ignored)
     * - Products WITHOUT variants: base product stock
     */
    public int getEffectiveStock() {
        if (hasVariants()) {
            int total = 0;
            for (ProductVariant v : variants) {
                total += v.getStock();
            }
            return total;
        }
        return stock;
    }

    // ========== Firestore Serialization ==========

    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("name", name != null ? name : "");
        map.put("description", description != null ? description : "");
        map.put("price", price);
        map.put("imageUrl", imageUrl != null ? imageUrl : "");
        map.put("categoryId", categoryId);
        map.put("categoryDocId", categoryDocId != null ? categoryDocId : "");
        map.put("stock", stock);
        map.put("videoUrl", videoUrl != null ? videoUrl : "");
        map.put("featured", featured);
        map.put("imageUrls", imageUrls != null ? imageUrls : new ArrayList<>());

        // Serialize variants
        List<Map<String, Object>> variantMaps = new ArrayList<>();
        if (variants != null) {
            for (ProductVariant v : variants)
                variantMaps.add(v.toMap());
        }
        map.put("variants", variantMaps);

        // Serialize variantOptions
        List<Map<String, Object>> optionMaps = new ArrayList<>();
        if (variantOptions != null) {
            for (VariantOption opt : variantOptions)
                optionMaps.add(opt.toMap());
        }
        map.put("variantOptions", optionMaps);
        return map;
    }

    @SuppressWarnings("unchecked")
    public static Product fromMap(Map<String, Object> map) {
        if (map == null)
            return null;
        Product p = new Product();
        p.setName((String) map.get("name"));
        p.setDescription((String) map.get("description"));
        Object priceObj = map.get("price");
        if (priceObj instanceof Number)
            p.setPrice(((Number) priceObj).doubleValue());
        p.setImageUrl((String) map.get("imageUrl"));
        Object catObj = map.get("categoryId");
        if (catObj instanceof Number)
            p.setCategoryId(((Number) catObj).intValue());
        p.setCategoryDocId((String) map.get("categoryDocId"));
        Object stockObj = map.get("stock");
        if (stockObj instanceof Number)
            p.setStock(((Number) stockObj).intValue());
        p.setVideoUrl((String) map.get("videoUrl"));
        Object featObj = map.get("featured");
        if (featObj instanceof Boolean)
            p.setFeatured((Boolean) featObj);

        if (map.get("imageUrls") != null) {
            p.setImageUrls((List<String>) map.get("imageUrls"));
        } else {
            p.setImageUrls(new ArrayList<>());
            if (p.getImageUrl() != null && !p.getImageUrl().isEmpty()) {
                p.getImageUrls().add(p.getImageUrl());
            }
        }

        // Deserialize variants
        List<ProductVariant> variantsList = new ArrayList<>();
        Object variantsObj = map.get("variants");
        if (variantsObj instanceof List) {
            for (Object item : (List<?>) variantsObj) {
                if (item instanceof Map) {
                    ProductVariant v = ProductVariant.fromMap((Map<String, Object>) item);
                    if (v != null)
                        variantsList.add(v);
                }
            }
        }
        p.setVariants(variantsList);

        // Deserialize variantOptions
        List<VariantOption> optionsList = new ArrayList<>();
        Object optionsObj = map.get("variantOptions");
        if (optionsObj instanceof List) {
            for (Object item : (List<?>) optionsObj) {
                if (item instanceof Map) {
                    VariantOption opt = VariantOption.fromMap((Map<String, Object>) item);
                    if (opt != null && opt.getName() != null && !opt.getName().isEmpty())
                        optionsList.add(opt);
                }
            }
        }
        p.setVariantOptions(optionsList);
        return p;
    }
}
