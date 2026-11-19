package com.lexoratech.lexorafix.admin.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.Timestamp;
import com.lexoratech.lexorafix.admin.R;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ChatAdapter extends RecyclerView.Adapter<ChatAdapter.MessageViewHolder> {

    private static final int VIEW_TYPE_SENT = 1;
    private static final int VIEW_TYPE_RECEIVED = 2;

    private final List<Map<String, Object>> messages;
    private final String currentUserId;
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());

    public ChatAdapter(List<Map<String, Object>> messages, String currentUserId) {
        this.messages = messages;
        this.currentUserId = currentUserId;
    }

    @Override
    public int getItemViewType(int position) {
        Map<String, Object> message = messages.get(position);
        Object senderId = message.get("senderId");
        if (senderId != null && senderId.toString().equals(currentUserId)) {
            return VIEW_TYPE_SENT;
        }
        return VIEW_TYPE_RECEIVED;
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layoutRes = (viewType == VIEW_TYPE_SENT)
                ? R.layout.item_message_sent
                : R.layout.item_message_received;
        View view = LayoutInflater.from(parent.getContext()).inflate(layoutRes, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        Map<String, Object> message = messages.get(position);
        
        Object text = message.get("text");
        holder.tvMessageText.setText(text != null ? text.toString() : "");

        Object timestamp = message.get("timestamp");
        if (timestamp instanceof Timestamp) {
            holder.tvMessageTime.setText(timeFormat.format(((Timestamp) timestamp).toDate()));
        } else {
            holder.tvMessageTime.setText("");
        }
    }

    @Override
    public int getItemCount() {
        return messages.size();
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView tvMessageText, tvMessageTime;

        MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMessageText = itemView.findViewById(R.id.tvMessageText);
            tvMessageTime = itemView.findViewById(R.id.tvMessageTime);
        }
    }
}
