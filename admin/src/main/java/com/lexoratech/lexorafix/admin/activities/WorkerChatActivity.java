package com.lexoratech.lexorafix.admin.activities;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.lexoratech.lexorafix.admin.R;
import com.lexoratech.lexorafix.admin.adapters.ChatAdapter;
import com.lexoratech.lexorafix.admin.utils.ThemeUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WorkerChatActivity extends AppCompatActivity {

    public static final String EXTRA_BOOKING_ID = "bookingId";
    public static final String EXTRA_CUSTOMER_NAME = "customerName";
    public static final String EXTRA_SERVICE_TYPE = "serviceType";

    private RecyclerView rvMessages;
    private EditText etMessage;
    private ImageButton btnSend;
    private LinearLayout layoutEmptyChat;
    private ChatAdapter chatAdapter;
    private List<Map<String, Object>> messageList;
    private ListenerRegistration messageListener;

    private String chatId;
    private String currentUserId;
    private String currentUserName;

    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeUtils.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_worker_chat);

        db = FirebaseFirestore.getInstance();

        // Get extras
        String bookingId = getIntent().getStringExtra(EXTRA_BOOKING_ID);
        String customerName = getIntent().getStringExtra(EXTRA_CUSTOMER_NAME);
        String serviceType = getIntent().getStringExtra(EXTRA_SERVICE_TYPE);

        // Current user
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) { finish(); return; }
        currentUserId = user.getUid();
        currentUserName = user.getDisplayName() != null ? user.getDisplayName() : user.getEmail();

        // Chat ID — deterministic based on booking (same as customer side)
        chatId = "booking_" + bookingId;

        // Setup toolbar
        MaterialToolbar toolbar = findViewById(R.id.toolbarChat);
        TextView tvRecipientName = findViewById(R.id.tvChatRecipientName);
        TextView tvBookingInfo = findViewById(R.id.tvChatBookingInfo);
        tvRecipientName.setText(customerName != null ? customerName : "Customer");
        tvBookingInfo.setText(serviceType != null ? serviceType + " Service" : "Service Chat");
        toolbar.setNavigationOnClickListener(v -> finish());

        // Setup views
        rvMessages = findViewById(R.id.rvMessages);
        etMessage = findViewById(R.id.etMessage);
        btnSend = findViewById(R.id.btnSend);
        layoutEmptyChat = findViewById(R.id.layoutEmptyChat);

        // Setup RecyclerView
        messageList = new ArrayList<>();
        chatAdapter = new ChatAdapter(messageList, currentUserId);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        rvMessages.setLayoutManager(layoutManager);
        rvMessages.setAdapter(chatAdapter);

        // Setup Quick Replies
        RecyclerView rvQuickReplies = findViewById(R.id.rvQuickReplies);
        List<String> workerQuickReplies = java.util.Arrays.asList(
                "I'm on my way!",
                "Running 5 minutes late",
                "I have arrived",
                "Can you provide more details?"
        );
        com.lexoratech.lexorafix.admin.adapters.QuickReplyAdapter quickReplyAdapter = new com.lexoratech.lexorafix.admin.adapters.QuickReplyAdapter(this, workerQuickReplies, reply -> {
            etMessage.setText(reply);
            sendMessage();
        });
        rvQuickReplies.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvQuickReplies.setAdapter(quickReplyAdapter);

        // Call Button functionality
        ImageButton btnCallUser = findViewById(R.id.btnCallUser);
        btnCallUser.setOnClickListener(v -> initiateCallToCustomer(bookingId));

        // Ensure chat document exists
        ensureChatDocumentExists(bookingId, customerName);

        // Listen for messages
        listenForMessages();

        // Send button
        btnSend.setOnClickListener(v -> sendMessage());
    }

    private void initiateCallToCustomer(String bookingId) {
        if (bookingId == null || bookingId.isEmpty()) return;

        db.collection("bookings").document(bookingId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists() && doc.contains("customerPhone") && doc.getString("customerPhone") != null) {
                        String phone = doc.getString("customerPhone");
                        if (!phone.isEmpty()) {
                            dialPhoneNumber(phone);
                            return;
                        }
                    }
                    
                    // Fallback to searching users collection
                    if (doc.exists() && doc.contains("customerId")) {
                        String customerId = doc.getString("customerId");
                        if (customerId != null) {
                             db.collection("users").document(customerId).get().addOnSuccessListener(userDoc -> {
                                 if (userDoc.exists() && userDoc.contains("phone")) {
                                     dialPhoneNumber(userDoc.getString("phone"));
                                 } else {
                                     android.widget.Toast.makeText(this, "Customer phone number not available", android.widget.Toast.LENGTH_SHORT).show();
                                 }
                             });
                        }
                    } else {
                        android.widget.Toast.makeText(this, "Customer phone number not available", android.widget.Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> android.widget.Toast.makeText(this, "Failed to retrieve phone number", android.widget.Toast.LENGTH_SHORT).show());
    }

    private void dialPhoneNumber(String phoneNumber) {
        android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_DIAL);
        intent.setData(android.net.Uri.parse("tel:" + phoneNumber));
        if (intent.resolveActivity(getPackageManager()) != null) {
            startActivity(intent);
        } else {
            android.widget.Toast.makeText(this, "No dialer app found", android.widget.Toast.LENGTH_SHORT).show();
        }
    }

    private void ensureChatDocumentExists(String bookingId, String customerName) {
        db.collection("chats").document(chatId).get().addOnSuccessListener(doc -> {
            if (!doc.exists()) {
                Map<String, Object> chatData = new HashMap<>();
                chatData.put("bookingId", bookingId);
                chatData.put("customerName", customerName);
                chatData.put("workerName", currentUserName);
                chatData.put("lastMessage", "");
                chatData.put("lastMessageTimestamp", Timestamp.now());
                List<String> participants = new ArrayList<>();
                participants.add(currentUserId);
                chatData.put("participants", participants);
                db.collection("chats").document(chatId).set(chatData);
            }
        });
    }

    private void listenForMessages() {
        messageListener = db.collection("chats").document(chatId)
                .collection("messages")
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .addSnapshotListener((snapshots, error) -> {
                    if (error != null || snapshots == null) return;

                    // Rebuild complete list from snapshot for reliable real-time sync
                    messageList.clear();
                    for (com.google.firebase.firestore.DocumentSnapshot doc : snapshots.getDocuments()) {
                        Map<String, Object> message = doc.getData();
                        if (message != null) {
                            message.put("_id", doc.getId());
                            messageList.add(message);
                        }
                    }

                    chatAdapter.notifyDataSetChanged();

                    // Scroll to bottom
                    if (!messageList.isEmpty()) {
                        rvMessages.scrollToPosition(messageList.size() - 1);
                    }

                    // Toggle empty state
                    updateEmptyState();
                });
    }

    private void sendMessage() {
        String text = etMessage.getText() != null ? etMessage.getText().toString().trim() : "";
        if (TextUtils.isEmpty(text)) return;

        Map<String, Object> message = new HashMap<>();
        message.put("senderId", currentUserId);
        message.put("senderName", currentUserName);
        message.put("text", text);
        message.put("timestamp", Timestamp.now());
        message.put("read", false);

        db.collection("chats").document(chatId)
                .collection("messages")
                .add(message)
                .addOnSuccessListener(ref -> {
                    Map<String, Object> updates = new HashMap<>();
                    updates.put("lastMessage", text);
                    updates.put("lastMessageTimestamp", Timestamp.now());
                    db.collection("chats").document(chatId).update(updates);
                });

        etMessage.setText("");
    }

    private void updateEmptyState() {
        if (messageList.isEmpty()) {
            layoutEmptyChat.setVisibility(View.VISIBLE);
            rvMessages.setVisibility(View.GONE);
        } else {
            layoutEmptyChat.setVisibility(View.GONE);
            rvMessages.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (messageListener != null) {
            messageListener.remove();
        }
    }
}
