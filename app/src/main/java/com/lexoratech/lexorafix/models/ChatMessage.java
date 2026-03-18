package com.lexoratech.lexorafix.models;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.Exclude;

public class ChatMessage {
    private String id;
    private String senderId;
    private String senderName;
    private String text;
    private Timestamp timestamp;
    private boolean read;

    public ChatMessage() {} // Required for Firestore

    public ChatMessage(String senderId, String senderName, String text, Timestamp timestamp) {
        this.senderId = senderId;
        this.senderName = senderName;
        this.text = text;
        this.timestamp = timestamp;
        this.read = false;
    }

    @Exclude
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getSenderId() { return senderId; }
    public void setSenderId(String senderId) { this.senderId = senderId; }
    public String getSenderName() { return senderName; }
    public void setSenderName(String senderName) { this.senderName = senderName; }
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public Timestamp getTimestamp() { return timestamp; }
    public void setTimestamp(Timestamp timestamp) { this.timestamp = timestamp; }
    public boolean isRead() { return read; }
    public void setRead(boolean read) { this.read = read; }
}
