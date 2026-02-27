package com.lexoratech.lexorafix.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.lexoratech.lexorafix.models.CartItem;
import com.lexoratech.lexorafix.models.Category;
import com.lexoratech.lexorafix.models.Order;
import com.lexoratech.lexorafix.models.Product;
import com.lexoratech.lexorafix.models.ServiceBooking;
import com.lexoratech.lexorafix.models.ServiceCenter;
import com.lexoratech.lexorafix.models.User;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "lexorafix.db";
    private static final int DB_VERSION = 2;
    private static DatabaseHelper instance;

    // Table names
    private static final String TABLE_USERS = "users";
    private static final String TABLE_CATEGORIES = "categories";
    private static final String TABLE_PRODUCTS = "products";
    private static final String TABLE_CART = "cart_items";
    private static final String TABLE_ORDERS = "orders";
    private static final String TABLE_ORDER_ITEMS = "order_items";
    private static final String TABLE_BOOKINGS = "service_bookings";
    private static final String TABLE_SERVICE_CENTERS = "service_centers";

    public static synchronized DatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new DatabaseHelper(context.getApplicationContext());
        }
        return instance;
    }

    private DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_USERS + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "name TEXT NOT NULL, " +
                "email TEXT UNIQUE NOT NULL, " +
                "password TEXT NOT NULL, " +
                "phone TEXT, " +
                "address TEXT)");

        db.execSQL("CREATE TABLE " + TABLE_CATEGORIES + " (" +
                "id INTEGER PRIMARY KEY, " +
                "name TEXT NOT NULL, " +
                "icon_name TEXT, " +
                "description TEXT)");

        db.execSQL("CREATE TABLE " + TABLE_PRODUCTS + " (" +
                "id INTEGER PRIMARY KEY, " +
                "name TEXT NOT NULL, " +
                "description TEXT, " +
                "price REAL NOT NULL, " +
                "image_url TEXT, " +
                "category_id INTEGER, " +
                "stock INTEGER DEFAULT 0, " +
                "video_url TEXT, " +
                "featured INTEGER DEFAULT 0, " +
                "FOREIGN KEY(category_id) REFERENCES " + TABLE_CATEGORIES + "(id))");

        db.execSQL("CREATE TABLE " + TABLE_CART + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "product_id INTEGER NOT NULL, " +
                "product_name TEXT, " +
                "price REAL, " +
                "quantity INTEGER DEFAULT 1, " +
                "image_url TEXT, " +
                "user_id INTEGER, " +
                "variant_label TEXT DEFAULT '', " +
                "FOREIGN KEY(product_id) REFERENCES " + TABLE_PRODUCTS + "(id))");

        db.execSQL("CREATE TABLE " + TABLE_ORDERS + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "order_id TEXT UNIQUE NOT NULL, " +
                "user_id INTEGER, " +
                "total_price REAL, " +
                "address TEXT, " +
                "status TEXT DEFAULT 'Pending', " +
                "timestamp DATETIME DEFAULT CURRENT_TIMESTAMP, " +
                "item_count INTEGER DEFAULT 0)");

        db.execSQL("CREATE TABLE " + TABLE_ORDER_ITEMS + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "order_id TEXT, " +
                "product_id INTEGER, " +
                "product_name TEXT, " +
                "price REAL, " +
                "quantity INTEGER, " +
                "FOREIGN KEY(order_id) REFERENCES " + TABLE_ORDERS + "(order_id))");

        db.execSQL("CREATE TABLE " + TABLE_BOOKINGS + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "user_id INTEGER, " +
                "order_id TEXT, " +
                "service_type TEXT NOT NULL, " +
                "date TEXT, " +
                "time TEXT, " +
                "notes TEXT, " +
                "status TEXT DEFAULT 'Pending', " +
                "timestamp DATETIME DEFAULT CURRENT_TIMESTAMP)");

        db.execSQL("CREATE TABLE " + TABLE_SERVICE_CENTERS + " (" +
                "id INTEGER PRIMARY KEY, " +
                "name TEXT NOT NULL, " +
                "address TEXT, " +
                "phone TEXT, " +
                "latitude REAL, " +
                "longitude REAL, " +
                "description TEXT)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_ORDER_ITEMS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_BOOKINGS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_ORDERS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CART);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PRODUCTS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CATEGORIES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_USERS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SERVICE_CENTERS);
        onCreate(db);
    }

    // ========== USER OPERATIONS ==========

    public long registerUser(User user) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("name", user.getName());
        cv.put("email", user.getEmail());
        cv.put("password", user.getPassword());
        cv.put("phone", user.getPhone());
        cv.put("address", user.getAddress());
        return db.insert(TABLE_USERS, null, cv);
    }

    public User loginUser(String email, String password) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM " + TABLE_USERS +
                " WHERE email = ? AND password = ?", new String[] { email, password });
        User user = null;
        if (c.moveToFirst()) {
            user = new User();
            user.setId(c.getInt(c.getColumnIndexOrThrow("id")));
            user.setName(c.getString(c.getColumnIndexOrThrow("name")));
            user.setEmail(c.getString(c.getColumnIndexOrThrow("email")));
            user.setPhone(c.getString(c.getColumnIndexOrThrow("phone")));
            user.setAddress(c.getString(c.getColumnIndexOrThrow("address")));
        }
        c.close();
        return user;
    }

    public boolean isEmailExists(String email) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT id FROM " + TABLE_USERS + " WHERE email = ?", new String[] { email });
        boolean exists = c.getCount() > 0;
        c.close();
        return exists;
    }

    public User getUserById(int userId) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM " + TABLE_USERS + " WHERE id = ?",
                new String[] { String.valueOf(userId) });
        User user = null;
        if (c.moveToFirst()) {
            user = new User();
            user.setId(c.getInt(c.getColumnIndexOrThrow("id")));
            user.setName(c.getString(c.getColumnIndexOrThrow("name")));
            user.setEmail(c.getString(c.getColumnIndexOrThrow("email")));
            user.setPhone(c.getString(c.getColumnIndexOrThrow("phone")));
            user.setAddress(c.getString(c.getColumnIndexOrThrow("address")));
        }
        c.close();
        return user;
    }

    public void updateUser(User user) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("name", user.getName());
        cv.put("phone", user.getPhone());
        cv.put("address", user.getAddress());
        db.update(TABLE_USERS, cv, "id = ?", new String[] { String.valueOf(user.getId()) });
    }

    // ========== CATEGORY OPERATIONS ==========

    public void insertCategory(Category cat) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("id", cat.getId());
        cv.put("name", cat.getName());
        cv.put("icon_name", cat.getIconName());
        cv.put("description", cat.getDescription());
        db.insertWithOnConflict(TABLE_CATEGORIES, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public List<Category> getAllCategories() {
        List<Category> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM " + TABLE_CATEGORIES, null);
        while (c.moveToNext()) {
            Category cat = new Category();
            cat.setId(c.getInt(c.getColumnIndexOrThrow("id")));
            cat.setName(c.getString(c.getColumnIndexOrThrow("name")));
            cat.setIconName(c.getString(c.getColumnIndexOrThrow("icon_name")));
            cat.setDescription(c.getString(c.getColumnIndexOrThrow("description")));
            list.add(cat);
        }
        c.close();
        return list;
    }

    // ========== PRODUCT OPERATIONS ==========

    public void insertProduct(Product p) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("id", p.getId());
        cv.put("name", p.getName());
        cv.put("description", p.getDescription());
        cv.put("price", p.getPrice());
        cv.put("image_url", p.getImageUrl());
        cv.put("category_id", p.getCategoryId());
        cv.put("stock", p.getStock());
        cv.put("video_url", p.getVideoUrl());
        cv.put("featured", p.isFeatured() ? 1 : 0);
        db.insertWithOnConflict(TABLE_PRODUCTS, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public List<Product> getAllProducts() {
        List<Product> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM " + TABLE_PRODUCTS, null);
        while (c.moveToNext()) {
            list.add(cursorToProduct(c));
        }
        c.close();
        return list;
    }

    public List<Product> getProductsByCategory(int categoryId) {
        List<Product> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM " + TABLE_PRODUCTS + " WHERE category_id = ?",
                new String[] { String.valueOf(categoryId) });
        while (c.moveToNext()) {
            list.add(cursorToProduct(c));
        }
        c.close();
        return list;
    }

    public List<Product> getFeaturedProducts() {
        List<Product> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM " + TABLE_PRODUCTS + " WHERE featured = 1", null);
        while (c.moveToNext()) {
            list.add(cursorToProduct(c));
        }
        c.close();
        return list;
    }

    public Product getProductById(int productId) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM " + TABLE_PRODUCTS + " WHERE id = ?",
                new String[] { String.valueOf(productId) });
        Product p = null;
        if (c.moveToFirst()) {
            p = cursorToProduct(c);
        }
        c.close();
        return p;
    }

    public List<Product> searchProducts(String query) {
        List<Product> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM " + TABLE_PRODUCTS +
                " WHERE name LIKE ? OR description LIKE ?",
                new String[] { "%" + query + "%", "%" + query + "%" });
        while (c.moveToNext()) {
            list.add(cursorToProduct(c));
        }
        c.close();
        return list;
    }

    private Product cursorToProduct(Cursor c) {
        Product p = new Product();
        p.setId(c.getInt(c.getColumnIndexOrThrow("id")));
        p.setName(c.getString(c.getColumnIndexOrThrow("name")));
        p.setDescription(c.getString(c.getColumnIndexOrThrow("description")));
        p.setPrice(c.getDouble(c.getColumnIndexOrThrow("price")));
        p.setImageUrl(c.getString(c.getColumnIndexOrThrow("image_url")));
        p.setCategoryId(c.getInt(c.getColumnIndexOrThrow("category_id")));
        p.setStock(c.getInt(c.getColumnIndexOrThrow("stock")));
        p.setVideoUrl(c.getString(c.getColumnIndexOrThrow("video_url")));
        p.setFeatured(c.getInt(c.getColumnIndexOrThrow("featured")) == 1);
        return p;
    }

    // ========== CART OPERATIONS ==========

    public void addToCart(CartItem item, int userId) {
        SQLiteDatabase db = getWritableDatabase();
        String variantLabel = item.getVariantLabel() != null ? item.getVariantLabel() : "";
        // Treat same product + same variant as the same cart row
        Cursor c = db.rawQuery("SELECT * FROM " + TABLE_CART +
                " WHERE product_id = ? AND user_id = ? AND COALESCE(variant_label,'') = ?",
                new String[] { String.valueOf(item.getProductId()), String.valueOf(userId), variantLabel });
        if (c.moveToFirst()) {
            int currentQty = c.getInt(c.getColumnIndexOrThrow("quantity"));
            ContentValues cv = new ContentValues();
            cv.put("quantity", currentQty + item.getQuantity());
            db.update(TABLE_CART, cv,
                    "product_id = ? AND user_id = ? AND COALESCE(variant_label,'') = ?",
                    new String[] { String.valueOf(item.getProductId()), String.valueOf(userId), variantLabel });
        } else {
            ContentValues cv = new ContentValues();
            cv.put("product_id", item.getProductId());
            cv.put("product_name", item.getProductName());
            cv.put("price", item.getPrice());
            cv.put("quantity", item.getQuantity());
            cv.put("image_url", item.getImageUrl());
            cv.put("user_id", userId);
            cv.put("variant_label", variantLabel);
            db.insert(TABLE_CART, null, cv);
        }
        c.close();
    }

    public List<CartItem> getCartItems(int userId) {
        List<CartItem> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM " + TABLE_CART + " WHERE user_id = ?",
                new String[] { String.valueOf(userId) });
        while (c.moveToNext()) {
            CartItem item = new CartItem();
            item.setId(c.getInt(c.getColumnIndexOrThrow("id")));
            item.setProductId(c.getInt(c.getColumnIndexOrThrow("product_id")));
            item.setProductName(c.getString(c.getColumnIndexOrThrow("product_name")));
            item.setPrice(c.getDouble(c.getColumnIndexOrThrow("price")));
            item.setQuantity(c.getInt(c.getColumnIndexOrThrow("quantity")));
            item.setImageUrl(c.getString(c.getColumnIndexOrThrow("image_url")));
            int variantColIdx = c.getColumnIndex("variant_label");
            if (variantColIdx != -1) {
                item.setVariantLabel(c.getString(variantColIdx));
            }
            list.add(item);
        }
        c.close();
        return list;
    }

    public void updateCartItemQuantity(int cartItemId, int quantity) {
        SQLiteDatabase db = getWritableDatabase();
        if (quantity <= 0) {
            db.delete(TABLE_CART, "id = ?", new String[] { String.valueOf(cartItemId) });
        } else {
            ContentValues cv = new ContentValues();
            cv.put("quantity", quantity);
            db.update(TABLE_CART, cv, "id = ?", new String[] { String.valueOf(cartItemId) });
        }
    }

    public void removeCartItem(int cartItemId) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_CART, "id = ?", new String[] { String.valueOf(cartItemId) });
    }

    public void clearCart(int userId) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(TABLE_CART, "user_id = ?", new String[] { String.valueOf(userId) });
    }

    public double getCartTotal(int userId) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT SUM(price * quantity) FROM " + TABLE_CART +
                " WHERE user_id = ?", new String[] { String.valueOf(userId) });
        double total = 0;
        if (c.moveToFirst()) {
            total = c.getDouble(0);
        }
        c.close();
        return total;
    }

    public int getCartItemCount(int userId) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT SUM(quantity) FROM " + TABLE_CART +
                " WHERE user_id = ?", new String[] { String.valueOf(userId) });
        int count = 0;
        if (c.moveToFirst()) {
            count = c.getInt(0);
        }
        c.close();
        return count;
    }

    public int getProductQuantityInCart(int productId, int userId) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT quantity FROM " + TABLE_CART +
                " WHERE product_id = ? AND user_id = ?",
                new String[] { String.valueOf(productId), String.valueOf(userId) });
        int quantity = 0;
        if (c.moveToFirst()) {
            quantity = c.getInt(0);
        }
        c.close();
        return quantity;
    }

    // ========== ORDER OPERATIONS ==========

    public String placeOrder(int userId, String address, double totalPrice) {
        SQLiteDatabase db = getWritableDatabase();
        String orderId = "LF" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        List<CartItem> cartItems = getCartItems(userId);

        ContentValues orderCv = new ContentValues();
        orderCv.put("order_id", orderId);
        orderCv.put("user_id", userId);
        orderCv.put("total_price", totalPrice);
        orderCv.put("address", address);
        orderCv.put("status", "Confirmed");
        orderCv.put("item_count", cartItems.size());
        db.insert(TABLE_ORDERS, null, orderCv);

        for (CartItem item : cartItems) {
            ContentValues itemCv = new ContentValues();
            itemCv.put("order_id", orderId);
            itemCv.put("product_id", item.getProductId());
            itemCv.put("product_name", item.getProductName());
            itemCv.put("price", item.getPrice());
            itemCv.put("quantity", item.getQuantity());
            db.insert(TABLE_ORDER_ITEMS, null, itemCv);
        }

        clearCart(userId);
        return orderId;
    }

    public List<Order> getUserOrders(int userId) {
        List<Order> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM " + TABLE_ORDERS +
                " WHERE user_id = ? ORDER BY timestamp DESC",
                new String[] { String.valueOf(userId) });
        while (c.moveToNext()) {
            Order order = new Order();
            order.setId(c.getInt(c.getColumnIndexOrThrow("id")));
            order.setOrderId(c.getString(c.getColumnIndexOrThrow("order_id")));
            order.setUserId(c.getInt(c.getColumnIndexOrThrow("user_id")));
            order.setTotalPrice(c.getDouble(c.getColumnIndexOrThrow("total_price")));
            order.setAddress(c.getString(c.getColumnIndexOrThrow("address")));
            order.setStatus(c.getString(c.getColumnIndexOrThrow("status")));
            order.setTimestamp(c.getString(c.getColumnIndexOrThrow("timestamp")));
            order.setItemCount(c.getInt(c.getColumnIndexOrThrow("item_count")));
            list.add(order);
        }
        c.close();
        return list;
    }

    // ========== SERVICE BOOKING OPERATIONS ==========

    public long addBooking(ServiceBooking booking) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("user_id", booking.getUserId());
        cv.put("order_id", booking.getOrderId());
        cv.put("service_type", booking.getServiceType());
        cv.put("date", booking.getDate());
        cv.put("time", booking.getTime());
        cv.put("notes", booking.getNotes());
        cv.put("status", "Confirmed");
        return db.insert(TABLE_BOOKINGS, null, cv);
    }

    public List<ServiceBooking> getUserBookings(int userId) {
        List<ServiceBooking> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM " + TABLE_BOOKINGS +
                " WHERE user_id = ? ORDER BY timestamp DESC",
                new String[] { String.valueOf(userId) });
        while (c.moveToNext()) {
            ServiceBooking b = new ServiceBooking();
            b.setId(c.getInt(c.getColumnIndexOrThrow("id")));
            b.setUserId(c.getInt(c.getColumnIndexOrThrow("user_id")));
            b.setOrderId(c.getString(c.getColumnIndexOrThrow("order_id")));
            b.setServiceType(c.getString(c.getColumnIndexOrThrow("service_type")));
            b.setDate(c.getString(c.getColumnIndexOrThrow("date")));
            b.setTime(c.getString(c.getColumnIndexOrThrow("time")));
            b.setNotes(c.getString(c.getColumnIndexOrThrow("notes")));
            b.setStatus(c.getString(c.getColumnIndexOrThrow("status")));
            b.setTimestamp(c.getString(c.getColumnIndexOrThrow("timestamp")));
            list.add(b);
        }
        c.close();
        return list;
    }

    // ========== SERVICE CENTER OPERATIONS ==========

    public void insertServiceCenter(ServiceCenter sc) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("id", sc.getId());
        cv.put("name", sc.getName());
        cv.put("address", sc.getAddress());
        cv.put("phone", sc.getPhone());
        cv.put("latitude", sc.getLatitude());
        cv.put("longitude", sc.getLongitude());
        cv.put("description", sc.getDescription());
        db.insertWithOnConflict(TABLE_SERVICE_CENTERS, null, cv, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public List<ServiceCenter> getAllServiceCenters() {
        List<ServiceCenter> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM " + TABLE_SERVICE_CENTERS, null);
        while (c.moveToNext()) {
            ServiceCenter sc = new ServiceCenter();
            sc.setId(c.getInt(c.getColumnIndexOrThrow("id")));
            sc.setName(c.getString(c.getColumnIndexOrThrow("name")));
            sc.setAddress(c.getString(c.getColumnIndexOrThrow("address")));
            sc.setPhone(c.getString(c.getColumnIndexOrThrow("phone")));
            sc.setLatitude(c.getDouble(c.getColumnIndexOrThrow("latitude")));
            sc.setLongitude(c.getDouble(c.getColumnIndexOrThrow("longitude")));
            sc.setDescription(c.getString(c.getColumnIndexOrThrow("description")));
            list.add(sc);
        }
        c.close();
        return list;
    }
}
