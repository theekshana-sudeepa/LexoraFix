package com.lexoratech.lexorafix.data;

import android.net.Uri;

import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.UUID;

/**
 * Singleton helper for Firebase Storage operations (image upload/delete).
 */
public class StorageHelper {

    private static StorageHelper instance;
    private final StorageReference storageRef;

    public interface StorageCallback {
        void onSuccess(String downloadUrl);

        void onError(String message);
    }

    public interface DeleteCallback {
        void onSuccess();

        void onError(String message);
    }

    private StorageHelper() {
        storageRef = FirebaseStorage.getInstance().getReference();
    }

    public static synchronized StorageHelper getInstance() {
        if (instance == null) {
            instance = new StorageHelper();
        }
        return instance;
    }

    /**
     * Upload a product image to Firebase Storage.
     * Path: products/{uuid}.jpg
     */
    public void uploadProductImage(Uri imageUri, StorageCallback callback) {
        String fileName = "products/" + UUID.randomUUID().toString() + ".jpg";
        uploadImage(imageUri, fileName, callback);
    }

    /**
     * Upload a profile image to Firebase Storage.
     * Path: users/profiles/{userId}.jpg
     */
    public void uploadProfileImage(Uri imageUri, String userId, StorageCallback callback) {
        String fileName = "users/profiles/" + userId + ".jpg";
        uploadImage(imageUri, fileName, callback);
    }

    /**
     * Upload a category image to Firebase Storage.
     * Path: categories/{uuid}.jpg
     */
    public void uploadCategoryImage(Uri imageUri, StorageCallback callback) {
        String fileName = "categories/" + UUID.randomUUID().toString() + ".jpg";
        uploadImage(imageUri, fileName, callback);
    }

    private void uploadImage(Uri imageUri, String path, StorageCallback callback) {
        StorageReference fileRef = storageRef.child(path);
        fileRef.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> fileRef.getDownloadUrl()
                        .addOnSuccessListener(uri -> callback.onSuccess(uri.toString()))
                        .addOnFailureListener(e -> callback.onError(e.getMessage())))
                .addOnFailureListener(e -> callback.onError(e.getMessage()));
    }

    /**
     * Delete an image from Firebase Storage by its download URL.
     */
    public void deleteImage(String downloadUrl, DeleteCallback callback) {
        if (downloadUrl == null || downloadUrl.isEmpty()) {
            callback.onSuccess();
            return;
        }
        try {
            StorageReference fileRef = FirebaseStorage.getInstance().getReferenceFromUrl(downloadUrl);
            fileRef.delete()
                    .addOnSuccessListener(aVoid -> callback.onSuccess())
                    .addOnFailureListener(e -> callback.onError(e.getMessage()));
        } catch (IllegalArgumentException e) {
            callback.onError("Invalid storage URL");
        }
    }
}
