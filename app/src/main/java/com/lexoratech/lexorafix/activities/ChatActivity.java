package com.lexoratech.lexorafix.activities;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.lexoratech.lexorafix.R;
import com.lexoratech.lexorafix.adapters.ChatAdapter;
import com.lexoratech.lexorafix.models.ChatMessage;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ChatActivity extends AppCompatActivity {

    public static final String EXTRA_BOOKING_ID = "bookingId";
    public static final String EXTRA_WORKER_NAME = "workerName";
    public static final String EXTRA_SERVICE_TYPE = "serviceType";

    private RecyclerView rvMessages;
    private TextInputEditText etMessage;
    private ImageButton btnSend;
    private LinearLayout layoutEmptyChat;
    private ChatAdapter chatAdapter;
    private List<ChatMessage> messageList;
    private ListenerRegistration messageListener;

    private String chatId;
    private String currentUserId;
    private String currentUserName;
    private String bookingId;
    private String workerName;

    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        com.lexoratech.lexorafix.utils.ThemeUtils.applyTheme(this);
        setContentView(R.layout.activity_chat);

        db = FirebaseFirestore.getInstance();

        // Get extras
        bookingId = getIntent().getStringExtra(EXTRA_BOOKING_ID);
        workerName = getIntent().getStringExtra(EXTRA_WORKER_NAME);
        String serviceType = getIntent().getStringExtra(EXTRA_SERVICE_TYPE);

        // Current user
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) { finish(); return; }
        currentUserId = user.getUid();
        currentUserName = user.getDisplayName() != null ? user.getDisplayName() : "Customer";

        // Chat ID is deterministic based on booking
        chatId = "booking_" + bookingId;

        // Setup toolbar
        MaterialToolbar toolbar = findViewById(R.id.toolbarChat);
        TextView tvRecipientName = findViewById(R.id.tvChatRecipientName);
        TextView tvBookingInfo = findViewById(R.id.tvChatBookingInfo);
        tvRecipientName.setText(workerName != null ? workerName : "Worker");
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
        List<String> customerQuickReplies = java.util.Arrays.asList(
                "Are you on your way?",
                "Can you come earlier?",
                "I am at the location",
                "Please call me"
        );
        com.lexoratech.lexorafix.adapters.QuickReplyAdapter quickReplyAdapter = new com.lexoratech.lexorafix.adapters.QuickReplyAdapter(this, customerQuickReplies, reply -> {
            etMessage.setText(reply);
            sendMessage();
        });
        rvQuickReplies.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        rvQuickReplies.setAdapter(quickReplyAdapter);

        // Call Button functionality
        ImageButton btnCallUser = findViewById(R.id.btnCallUser);
        btnCallUser.setOnClickListener(v -> initiateCallToWorker());

        // Check if booking is completed and disable chat
        checkBookingStatus();

        // Ensure chat document exists
        ensureChatDocumentExists();

        // Listen for messages
        listenForMessages();

        // Send button
        btnSend.setOnClickListener(v -> sendMessage());
    }

    private void checkBookingStatus() {
        if (bookingId == null || bookingId.isEmpty()) return;
        db.collection("bookings").document(bookingId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists() && "Completed".equals(doc.getString("status"))) {
                        etMessage.setEnabled(false);
                        etMessage.setHint("Chat disabled (Booking Completed)");
                        btnSend.setEnabled(false);
                        RecyclerView rvQuickReplies = findViewById(R.id.rvQuickReplies);
                        if (rvQuickReplies != null) rvQuickReplies.setVisibility(View.GONE);
                    }
                });
    }

    private void initiateCallToWorker() {
        if (bookingId == null || bookingId.isEmpty()) return;

        // Try getting phone from booking first, then fallback to users collection if needed
        db.collection("bookings").document(bookingId).get()
                .addOnSuccessListener(doc -> {
                    if (doc.exists() && doc.contains("assignedWorkerPhone") && doc.getString("assignedWorkerPhone") != null) {
                        String phone = doc.getString("assignedWorkerPhone");
                        if (!phone.isEmpty()) {
                            dialPhoneNumber(phone);
                            return;
                        }
                    }
                    
                    // Fallback to searching users collection if not in booking
                    if (doc.exists() && doc.contains("assignedWorkerId")) {
                        String workerId = doc.getString("assignedWorkerId");
                        if (workerId != null) {
                             db.collection("users").document(workerId).get().addOnSuccessListener(userDoc -> {
                                 if (userDoc.exists() && userDoc.contains("phone")) {
                                     dialPhoneNumber(userDoc.getString("phone"));
                                 } else {
                                     android.widget.Toast.makeText(this, "Worker phone number not available", android.widget.Toast.LENGTH_SHORT).show();
                                 }
                             });
                        }
                    } else {
                        android.widget.Toast.makeText(this, "Worker phone number not available", android.widget.Toast.LENGTH_SHORT).show();
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

    private void ensureChatDocumentExists() {
        db.collection("chats").document(chatId).get().addOnSuccessListener(doc -> {
            if (!doc.exists()) {
                Map<String, Object> chatData = new HashMap<>();
                chatData.put("bookingId", bookingId);
                chatData.put("customerName", currentUserName);
                chatData.put("workerName", workerName);
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
                        ChatMessage message = doc.toObject(ChatMessage.class);
                        if (message != null) {
                            message.setId(doc.getId());
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

        // Use a Map for consistency with worker side
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
