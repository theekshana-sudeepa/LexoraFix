package com.lexoratech.lexorafix.network;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.lexoratech.lexorafix.data.DatabaseHelper;
import com.lexoratech.lexorafix.models.Category;
import com.lexoratech.lexorafix.models.Product;
import com.lexoratech.lexorafix.models.ServiceCenter;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

/**
 * NetworkHelper handles HTTP JSON data fetching with offline fallback.
 * For this version, it loads from bundled assets (simulating API).
 * The architecture supports switching to a real API endpoint.
 */
public class NetworkHelper {

    private static final String TAG = "NetworkHelper";
    private static final String API_BASE_URL = "https://api.lexorafix.com/v1/";
    private final Context context;
    private final DatabaseHelper dbHelper;
    private final Gson gson;

    public interface DataLoadCallback {
        void onSuccess();

        void onError(String message);
    }

    public NetworkHelper(Context context) {
        this.context = context.getApplicationContext();
        this.dbHelper = DatabaseHelper.getInstance(context);
        this.gson = new Gson();
    }

    /**
     * Loads all data from JSON asset (simulating HTTP fetch) and caches to SQLite.
     */
    public void loadAllData(DataLoadCallback callback) {
        new Thread(() -> {
            try {
                String json = loadJsonFromAssets("data.json");
                if (json == null) {
                    if (callback != null)
                        callback.onError("Failed to load data");
                    return;
                }

                JsonObject root = JsonParser.parseString(json).getAsJsonObject();

                // Parse and cache categories
                JsonArray categoriesArray = root.getAsJsonArray("categories");
                for (JsonElement el : categoriesArray) {
                    Category cat = gson.fromJson(el, Category.class);
                    dbHelper.insertCategory(cat);
                }

                // Parse and cache products
                JsonArray productsArray = root.getAsJsonArray("products");
                for (JsonElement el : productsArray) {
                    Product product = gson.fromJson(el, Product.class);
                    dbHelper.insertProduct(product);
                }

                // Parse and cache service centers
                JsonArray centersArray = root.getAsJsonArray("serviceCenters");
                for (JsonElement el : centersArray) {
                    ServiceCenter sc = gson.fromJson(el, ServiceCenter.class);
                    dbHelper.insertServiceCenter(sc);
                }

                Log.d(TAG, "Data loaded and cached successfully");
                if (callback != null)
                    callback.onSuccess();

            } catch (Exception e) {
                Log.e(TAG, "Error loading data", e);
                if (callback != null)
                    callback.onError(e.getMessage());
            }
        }).start();
    }

    /**
     * Loads JSON string from assets folder.
     */
    private String loadJsonFromAssets(String filename) {
        try {
            InputStream is = context.getAssets().open(filename);
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            reader.close();
            is.close();
            return sb.toString();
        } catch (Exception e) {
            Log.e(TAG, "Error reading asset file: " + filename, e);
            return null;
        }
    }

    /**
     * Example HTTP fetch method (for future real API integration).
     * Currently not used, but demonstrates HTTP URLConnection usage per
     * requirement.
     */
    public String fetchFromUrl(String urlString) {
        HttpURLConnection connection = null;
        try {
            URL url = new URL(urlString);
            connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setConnectTimeout(10000);
            connection.setReadTimeout(10000);
            connection.setRequestProperty("Accept", "application/json");

            int responseCode = connection.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                return response.toString();
            } else {
                Log.e(TAG, "HTTP error: " + responseCode);
                return null;
            }
        } catch (Exception e) {
            Log.e(TAG, "Network error", e);
            return null;
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
    }
}
