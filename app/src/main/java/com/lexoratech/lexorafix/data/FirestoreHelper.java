package com.lexoratech.lexorafix.data;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;
import com.lexoratech.lexorafix.models.User;

import java.util.HashMap;
import java.util.Map;

/**
 * Singleton wrapper around FirebaseFirestore for cloud data operations.
 */
public class FirestoreHelper {

    private static FirestoreHelper instance;
    private final FirebaseFirestore db;

    // Collection names
    public static final String COLLECTION_USERS = "users";
    public static final String COLLECTION_ORDERS = "orders";
    public static final String COLLECTION_BOOKINGS = "bookings";
    public static final String COLLECTION_PRODUCTS = "products";
    public static final String COLLECTION_CATEGORIES = "categories";

    public interface FirestoreCallback<T> {
        void onSuccess(T result);

        void onError(String message);
    }

    private FirestoreHelper() {
        db = FirebaseFirestore.getInstance();
    }

    public static synchronized FirestoreHelper getInstance() {
        if (instance == null) {
            instance = new FirestoreHelper();
        }
        return instance;
    }

    public FirebaseFirestore getDb() {
        return db;
    }

    // ========== USER OPERATIONS ==========

    public void saveUserProfile(String uid, User user, FirestoreCallback<Void> callback) {
        Map<String, Object> data = user.toMap();
        db.collection(COLLECTION_USERS).document(uid)
                .set(data)
                .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void getUserProfile(String uid, FirestoreCallback<User> callback) {
        db.collection(COLLECTION_USERS).document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        User user = User.fromMap(doc.getData());
                        if (user != null) {
                            user.setFirebaseUid(uid);
                        }
                        callback.onSuccess(user);
                    } else {
                        callback.onError("User profile not found");
                    }
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void getUserRole(String uid, FirestoreCallback<String> callback) {
        db.collection(COLLECTION_USERS).document(uid)
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists()) {
                        String role = doc.getString("role");
                        callback.onSuccess(role != null ? role : "customer");
                    } else {
                        callback.onError("User not found");
                    }
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    /**
     * Update specific fields in a user's Firestore profile.
     */
    public void updateUserProfile(String uid, Map<String, Object> updates, FirestoreCallback<Void> callback) {
        db.collection(COLLECTION_USERS).document(uid)
                .update(updates)
                .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void toggleWishlist(String uid, String productId, boolean isAdding, FirestoreCallback<Void> callback) {
        db.collection(COLLECTION_USERS).document(uid)
                .update("wishlist", isAdding ? FieldValue.arrayUnion(productId) : FieldValue.arrayRemove(productId))
                .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    // ========== PRODUCT OPERATIONS ==========

    public void addProduct(Map<String, Object> productData, FirestoreCallback<String> callback) {
        db.collection(COLLECTION_PRODUCTS)
                .add(productData)
                .addOnSuccessListener(docRef -> callback.onSuccess(docRef.getId()))
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void updateProduct(String docId, Map<String, Object> productData, FirestoreCallback<Void> callback) {
        db.collection(COLLECTION_PRODUCTS).document(docId)
                .set(productData)
                .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void deleteProduct(String docId, FirestoreCallback<Void> callback) {
        db.collection(COLLECTION_PRODUCTS).document(docId)
                .delete()
                .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void getAllProducts(FirestoreCallback<QuerySnapshot> callback) {
        db.collection(COLLECTION_PRODUCTS)
                .orderBy("name")
                .get()
                .addOnSuccessListener(callback::onSuccess)
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void getFeaturedProducts(FirestoreCallback<QuerySnapshot> callback) {
        db.collection(COLLECTION_PRODUCTS)
                .whereEqualTo("featured", true)
                .get()
                .addOnSuccessListener(callback::onSuccess)
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void getProductsByCategory(String categoryDocId, FirestoreCallback<QuerySnapshot> callback) {
        db.collection(COLLECTION_PRODUCTS)
                .whereEqualTo("categoryDocId", categoryDocId)
                .get()
                .addOnSuccessListener(callback::onSuccess)
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    // ========== CATEGORY OPERATIONS ==========

    public void addCategory(Map<String, Object> categoryData, FirestoreCallback<String> callback) {
        db.collection(COLLECTION_CATEGORIES)
                .add(categoryData)
                .addOnSuccessListener(docRef -> callback.onSuccess(docRef.getId()))
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void updateCategory(String docId, Map<String, Object> categoryData, FirestoreCallback<Void> callback) {
        db.collection(COLLECTION_CATEGORIES).document(docId)
                .set(categoryData)
                .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void deleteCategory(String docId, FirestoreCallback<Void> callback) {
        db.collection(COLLECTION_CATEGORIES).document(docId)
                .delete()
                .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void getAllCategories(FirestoreCallback<QuerySnapshot> callback) {
        db.collection(COLLECTION_CATEGORIES)
                .orderBy("name")
                .get()
                .addOnSuccessListener(callback::onSuccess)
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    // ========== ORDER OPERATIONS ==========

    /**
     * Save order to BOTH user subcollection AND top-level orders collection.
     * This ensures admin/seller can easily see all orders with customer info.
     */
    public void saveOrderWithNotification(String uid, String orderId, Map<String, Object> orderData,
            FirestoreCallback<Void> callback) {
        // Write to user subcollection
        db.collection(COLLECTION_USERS).document(uid)
                .collection(COLLECTION_ORDERS).document(orderId)
                .set(orderData)
                .addOnSuccessListener(aVoid -> {
                    // Also write to top-level orders collection for admin access
                    db.collection(COLLECTION_ORDERS).document(orderId)
                            .set(orderData)
                            .addOnSuccessListener(aVoid2 -> callback.onSuccess(null))
                            .addOnFailureListener(e -> callback.onError(e.getMessage()));
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    /**
     * Legacy method: save order only to user subcollection.
     */
    public void saveOrder(String uid, String orderId, Map<String, Object> orderData,
            FirestoreCallback<Void> callback) {
        db.collection(COLLECTION_USERS).document(uid)
                .collection(COLLECTION_ORDERS).document(orderId)
                .set(orderData)
                .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void getUserOrders(String uid, FirestoreCallback<QuerySnapshot> callback) {
        db.collection(COLLECTION_USERS).document(uid)
                .collection(COLLECTION_ORDERS)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(callback::onSuccess)
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    /**
     * Get all orders from top-level collection (for admin).
     */
    public void getAllOrdersTopLevel(FirestoreCallback<QuerySnapshot> callback) {
        db.collection(COLLECTION_ORDERS)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(callback::onSuccess)
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    /**
     * Real-time listener on top-level orders (for admin notifications).
     */
    public ListenerRegistration listenForNewOrders(FirestoreCallback<QuerySnapshot> callback) {
        return db.collection(COLLECTION_ORDERS)
                .addSnapshotListener((querySnapshot, error) -> {
                    if (error != null) {
                        if (error.getMessage() != null && !error.getMessage().contains("PERMISSION_DENIED")) {
                            callback.onError(error.getMessage());
                        }
                        return;
                    }
                    if (querySnapshot != null) {
                        callback.onSuccess(querySnapshot);
                    }
                });
    }

    /**
     * Real-time listener on user's orders (for customer status updates).
     */
    public ListenerRegistration listenForUserOrders(String uid, FirestoreCallback<QuerySnapshot> callback) {
        return db.collection(COLLECTION_USERS).document(uid)
                .collection(COLLECTION_ORDERS)
                .addSnapshotListener((querySnapshot, error) -> {
                    if (error != null) {
                        if (error.getMessage() != null && !error.getMessage().contains("PERMISSION_DENIED")) {
                            callback.onError(error.getMessage());
                        }
                        return;
                    }
                    if (querySnapshot != null) {
                        callback.onSuccess(querySnapshot);
                    }
                });
    }

    /**
     * Update order status in BOTH top-level and user subcollection.
     */
    public void updateOrderStatusAndNotify(String orderId, String userId, String newStatus,
            FirestoreCallback<Void> callback) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("status", newStatus);
        updates.put("statusUpdatedAt", Timestamp.now());

        // Update top-level orders collection
        db.collection(COLLECTION_ORDERS).document(orderId)
                .update(updates)
                .addOnSuccessListener(aVoid -> {
                    // Also update user subcollection so customer listener picks it up
                    if (userId != null && !userId.isEmpty()) {
                        db.collection(COLLECTION_USERS).document(userId)
                                .collection(COLLECTION_ORDERS).document(orderId)
                                .update(updates)
                                .addOnSuccessListener(aVoid2 -> callback.onSuccess(null))
                                .addOnFailureListener(e -> callback.onError(e.getMessage()));
                    } else {
                        callback.onSuccess(null);
                    }
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    /**
     * Legacy update method for backward compatibility.
     */
    public void updateOrderStatus(String uid, String orderId, String newStatus,
            FirestoreCallback<Void> callback) {
        db.collection(COLLECTION_USERS).document(uid)
                .collection(COLLECTION_ORDERS).document(orderId)
                .update("status", newStatus)
                .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    /**
     * Get all orders across all users (collection group query — legacy admin).
     */
    public void getAllOrders(FirestoreCallback<QuerySnapshot> callback) {
        db.collectionGroup(COLLECTION_ORDERS)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(callback::onSuccess)
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    // ========== BOOKING OPERATIONS ==========

    /**
     * Save booking to BOTH user subcollection AND top-level bookings collection.
     */
    public void saveBookingWithNotification(String uid, String bookingId, Map<String, Object> bookingData,
            FirestoreCallback<Void> callback) {
        db.collection(COLLECTION_USERS).document(uid)
                .collection(COLLECTION_BOOKINGS).document(bookingId)
                .set(bookingData)
                .addOnSuccessListener(aVoid -> {
                    // Also write to top-level bookings for admin
                    db.collection(COLLECTION_BOOKINGS).document(bookingId)
                            .set(bookingData)
                            .addOnSuccessListener(aVoid2 -> callback.onSuccess(null))
                            .addOnFailureListener(e -> callback.onError(e.getMessage()));
                })
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    public void saveBooking(String uid, String bookingId, Map<String, Object> bookingData,
            FirestoreCallback<Void> callback) {
        db.collection(COLLECTION_USERS).document(uid)
                .collection(COLLECTION_BOOKINGS).document(bookingId)
                .set(bookingData)
                .addOnSuccessListener(aVoid -> callback.onSuccess(null))
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    /**
     * Real-time listener on top-level bookings (for admin notifications).
     */
    public ListenerRegistration listenForNewBookings(FirestoreCallback<QuerySnapshot> callback) {
        return db.collection(COLLECTION_BOOKINGS)
                .addSnapshotListener((querySnapshot, error) -> {
                    if (error != null) {
                        if (error.getMessage() != null && !error.getMessage().contains("PERMISSION_DENIED")) {
                            callback.onError(error.getMessage());
                        }
                        return;
                    }
                    if (querySnapshot != null) {
                        callback.onSuccess(querySnapshot);
                    }
                });
    }

    public void getAllBookings(FirestoreCallback<QuerySnapshot> callback) {
        db.collectionGroup(COLLECTION_BOOKINGS)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(callback::onSuccess)
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    /**
     * Get all bookings from top-level collection (for admin).
     */
    public void getAllBookingsTopLevel(FirestoreCallback<QuerySnapshot> callback) {
        db.collection(COLLECTION_BOOKINGS)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(callback::onSuccess)
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    /**
     * Real-time listener on user's bookings (for customer booking status updates).
     */
    public ListenerRegistration listenForUserBookings(String uid, FirestoreCallback<QuerySnapshot> callback) {
        return db.collection(COLLECTION_USERS).document(uid)
                .collection(COLLECTION_BOOKINGS)
                .addSnapshotListener((querySnapshot, error) -> {
                    if (error != null) {
                        if (error.getMessage() != null && !error.getMessage().contains("PERMISSION_DENIED")) {
                            callback.onError(error.getMessage());
                        }
                        return;
                    }
                    if (querySnapshot != null) {
                        callback.onSuccess(querySnapshot);
                    }
                });
    }

    // ========== ADMIN OPERATIONS ==========

    public void getAllUsers(FirestoreCallback<QuerySnapshot> callback) {
        db.collection(COLLECTION_USERS)
                .get()
                .addOnSuccessListener(callback::onSuccess)
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    /**
     * Real-time listener on a specific user's document (for user status updates).
     */
    public ListenerRegistration listenForUserStatus(String uid, FirestoreCallback<DocumentSnapshot> callback) {
        return db.collection(COLLECTION_USERS).document(uid)
                .addSnapshotListener((documentSnapshot, error) -> {
                    if (error != null) {
                        if (error.getMessage() != null && !error.getMessage().contains("PERMISSION_DENIED")) {
                            callback.onError(error.getMessage());
                        }
                        return;
                    }
                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        callback.onSuccess(documentSnapshot);
                    }
                });
    }
}
