package com.lexoratech.lexorafix.admin.activities;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.tabs.TabLayout;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.lexoratech.lexorafix.admin.R;
import com.lexoratech.lexorafix.admin.adapters.WorkerBookingAdapter;
import com.lexoratech.lexorafix.admin.utils.ThemeUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class WorkerDashboardActivity extends AppCompatActivity {

    private static final String CHANNEL_ID = "worker_booking_channel";

    private RecyclerView rvWorkerTasks;
    private SwipeRefreshLayout swipeRefresh;
    private View emptyState;
    private TextView tvWorkerEmail, tvEmptyMessage, tvEmptyIcon;
    private ImageView btnLogout, btnProfile;
    private TabLayout tabLayout;

    private WorkerBookingAdapter adapter;
    private List<Map<String, Object>> fullBookingList = new ArrayList<>();
    private List<Map<String, Object>> bookingList = new ArrayList<>();
    
    private String currentFilter = "All";
    private ImageView btnFilter;

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private ListenerRegistration snapshotListener;
    private int currentTab = WorkerBookingAdapter.TAB_AVAILABLE;

    // Track count to detect new bookings
    private int previousBookingCount = -1;
    private boolean isFirstLoad = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeUtils.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_worker_dashboard);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        if (auth.getCurrentUser() == null) {
            startActivity(new Intent(this, AdminLoginActivity.class));
            finish();
            return;
        }

        createNotificationChannel();
        initViews();
        setupRecyclerView();
        setupTabs();
        startListening();
        saveFcmToken();

        swipeRefresh.setOnRefreshListener(() -> {
            swipeRefresh.setRefreshing(false);
        });

        btnLogout.setOnClickListener(v -> {
            auth.signOut();
            startActivity(new Intent(this, AdminLoginActivity.class));
            finish();
        });
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "New Booking Alerts",
                    NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Notifications when new service bookings are available");
            channel.enableVibration(true);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private void initViews() {
        rvWorkerTasks = findViewById(R.id.rv_worker_tasks);
        swipeRefresh = findViewById(R.id.swipe_refresh);
        emptyState = findViewById(R.id.empty_state);
        tvWorkerEmail = findViewById(R.id.tv_worker_email);
        tvEmptyMessage = findViewById(R.id.tv_empty_message);
        tvEmptyIcon = findViewById(R.id.tv_empty_icon);
        btnLogout = findViewById(R.id.btn_logout);
        btnProfile = findViewById(R.id.btn_profile);
        btnFilter = findViewById(R.id.btn_filter);
        tabLayout = findViewById(R.id.tab_layout);

        tvWorkerEmail.setText(auth.getCurrentUser().getEmail());

        btnFilter.setOnClickListener(v -> showFilterMenu());
        btnProfile.setOnClickListener(v -> showEditPhoneDialog());
    }

    private void setupRecyclerView() {
        rvWorkerTasks.setLayoutManager(new LinearLayoutManager(this));
        adapter = new WorkerBookingAdapter(this, bookingList);
        rvWorkerTasks.setAdapter(adapter);
    }

    private void setupTabs() {
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentTab = tab.getPosition();
                adapter.setCurrentTab(currentTab);
                isFirstLoad = true;
                previousBookingCount = -1;
                startListening();
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
    }

    private void startListening() {
        if (snapshotListener != null) {
            snapshotListener.remove();
        }

        swipeRefresh.setRefreshing(true);
        String workerId = auth.getCurrentUser().getUid();

        com.google.firebase.firestore.Query query;
        if (currentTab == WorkerBookingAdapter.TAB_AVAILABLE) {
            query = db.collection("bookings")
                    .whereEqualTo("status", "Confirmed");
        } else {
            query = db.collection("bookings")
                    .whereEqualTo("assignedWorkerId", workerId);
        }

        snapshotListener = query.addSnapshotListener((queryDocumentSnapshots, error) -> {
            swipeRefresh.setRefreshing(false);

            if (error != null) {
                bookingList.clear();
                adapter.notifyDataSetChanged();
                updateEmptyState();
                return;
            }

            fullBookingList.clear();
            if (queryDocumentSnapshots != null) {
                for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                    Map<String, Object> bookingMap = doc.getData();
                    bookingMap.put("_documentId", doc.getId());
                    fullBookingList.add(bookingMap);
                }
            }

            // Sort by timestamp descending (latest first)
            Collections.sort(fullBookingList, (a, b) -> {
                Object tsA = a.get("timestamp");
                Object tsB = b.get("timestamp");
                long timeA = 0, timeB = 0;
                
                if (tsA instanceof Timestamp) timeA = ((Timestamp) tsA).getSeconds();
                else if (tsA instanceof Long) timeA = (Long) tsA;
                
                if (tsB instanceof Timestamp) timeB = ((Timestamp) tsB).getSeconds();
                else if (tsB instanceof Long) timeB = (Long) tsB;
                
                int cmp = Long.compare(timeB, timeA); // Descending
                if (cmp == 0) {
                    String idA = (String) a.get("_documentId");
                    String idB = (String) b.get("_documentId");
                    if (idA != null && idB != null) {
                        return idB.compareTo(idA); // Descending
                    }
                }
                return cmp;
            });

            // Check if new bookings arrived (only on Available tab)
            int newCount = fullBookingList.size();
            if (currentTab == WorkerBookingAdapter.TAB_AVAILABLE && !isFirstLoad && newCount > previousBookingCount) {
                int diff = newCount - previousBookingCount;
                showNewBookingNotification(diff);
            }
            previousBookingCount = newCount;
            isFirstLoad = false;

            applyFilter();
        });
    }

    private void showFilterMenu() {
        androidx.appcompat.widget.PopupMenu popup = new androidx.appcompat.widget.PopupMenu(this, btnFilter);
        popup.getMenu().add("All");

        java.util.Set<String> categories = new java.util.HashSet<>();
        for (Map<String, Object> map : fullBookingList) {
            if (map.get("serviceType") instanceof String) {
                categories.add((String) map.get("serviceType"));
            }
        }
        
        List<String> sortedCategories = new ArrayList<>(categories);
        Collections.sort(sortedCategories);
        for (String cat : sortedCategories) {
            popup.getMenu().add(cat);
        }

        popup.setOnMenuItemClickListener(item -> {
            currentFilter = item.getTitle().toString();
            applyFilter();
            return true;
        });
        popup.show();
    }

    private void applyFilter() {
        bookingList.clear();
        for (Map<String, Object> map : fullBookingList) {
            if ("All".equals(currentFilter)) {
                bookingList.add(map);
            } else if (currentFilter.equals(map.get("serviceType"))) {
                bookingList.add(map);
            }
        }
        adapter.notifyDataSetChanged();
        updateEmptyState();
    }

    private void showNewBookingNotification(int count) {
        String title = "🔔 New Job Available!";
        String body = count == 1
                ? "A new service booking just came in. Tap to view!"
                : count + " new service bookings available!";

        // Get the latest booking's service type for a richer notification
        if (!bookingList.isEmpty()) {
            Object serviceType = bookingList.get(0).get("serviceType");
            if (serviceType != null) {
                body = "New booking: " + serviceType + ". Tap to accept!";
            }
        }

        Intent intent = new Intent(this, WorkerDashboardActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_email)
                .setContentTitle(title)
                .setContentText(body)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setVibrate(new long[]{0, 300, 200, 300});

        try {
            NotificationManagerCompat notificationManager = NotificationManagerCompat.from(this);
            notificationManager.notify((int) System.currentTimeMillis(), builder.build());
        } catch (SecurityException e) {
            // Notification permission not granted — silently ignore
        }
    }

    private void updateEmptyState() {
        boolean isEmpty = bookingList.isEmpty();
        emptyState.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        rvWorkerTasks.setVisibility(isEmpty ? View.GONE : View.VISIBLE);

        if (isEmpty) {
            if (currentTab == WorkerBookingAdapter.TAB_AVAILABLE) {
                tvEmptyIcon.setText("🔔");
                tvEmptyMessage.setText("No available jobs right now.\nPull down to refresh.");
            } else {
                tvEmptyIcon.setText("📋");
                tvEmptyMessage.setText("You haven't accepted any jobs yet.\nSwitch to 'Available Jobs' to find work!");
            }
        }
    }

    private void saveFcmToken() {
        com.google.firebase.messaging.FirebaseMessaging.getInstance().getToken()
                .addOnSuccessListener(token -> {
                    String uid = auth.getCurrentUser().getUid();
                    db.collection("users").document(uid)
                            .update("fcmToken", token)
                            .addOnSuccessListener(v -> android.util.Log.d("FCM", "Token saved for worker"))
                            .addOnFailureListener(e -> android.util.Log.e("FCM", "Token save failed: " + e.getMessage()));
                });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (snapshotListener != null) {
            snapshotListener.remove();
        }
    }

    private void showEditPhoneDialog() {
        com.google.android.material.bottomsheet.BottomSheetDialog dialog =
                new com.google.android.material.bottomsheet.BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.dialog_edit_phone, null);
        dialog.setContentView(view);

        TextView tvCurrentPhone = view.findViewById(R.id.tv_current_phone);
        EditText etPhone = view.findViewById(R.id.et_phone);
        com.google.android.material.button.MaterialButton btnSave = view.findViewById(R.id.btn_save_phone);

        // Load current phone from Firestore
        String uid = auth.getCurrentUser().getUid();
        db.collection("users").document(uid).get().addOnSuccessListener(doc -> {
            String currentPhone = doc.getString("phone");
            if (currentPhone != null && !currentPhone.isEmpty()) {
                tvCurrentPhone.setText(currentPhone);
                etPhone.setText(currentPhone);
                etPhone.setSelection(currentPhone.length());
            } else {
                tvCurrentPhone.setText("Not set");
            }
        });

        btnSave.setOnClickListener(v -> {
            String phone = etPhone.getText().toString().trim();
            if (phone.isEmpty()) {
                etPhone.setError("Please enter a phone number");
                return;
            }
            // Strict regex for Sri Lankan format: optional +94 or 0, followed by exactly 9 digits (started with 7)
            if (!phone.matches("^(?:\\+94|0)?[7][0-9]{8}$")) {
                etPhone.setError("Please enter a valid Sri Lankan phone number (e.g., 0771234567 or +94771234567)");
                return;
            }

            btnSave.setEnabled(false);
            btnSave.setText("Saving...");

            db.collection("users").document(uid)
                    .update("phone", phone)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "Phone number updated", Toast.LENGTH_SHORT).show();
                        dialog.dismiss();
                    })
                    .addOnFailureListener(e -> {
                        btnSave.setEnabled(true);
                        btnSave.setText("Save Phone Number");
                        Toast.makeText(this, "Failed to update: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        });

        dialog.show();
    }
}
