package com.lexoratech.lexorafix.adapters;

import android.content.Context;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.lexoratech.lexorafix.R;
import com.lexoratech.lexorafix.models.AppNotification;

import java.util.List;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {

    private final Context context;
    private final List<AppNotification> notificationList;
    private final OnNotificationClickListener listener;

    public interface OnNotificationClickListener {
        void onNotificationClick(AppNotification notification);
    }

    public NotificationAdapter(Context context, List<AppNotification> notificationList, OnNotificationClickListener listener) {
        this.context = context;
        this.notificationList = notificationList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_notification, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        AppNotification notification = notificationList.get(position);

        holder.tvTitle.setText(notification.getTitle());
        holder.tvMessage.setText(notification.getMessage());

        if (notification.getTimestamp() != null) {
            CharSequence timeAgo = DateUtils.getRelativeTimeSpanString(
                    notification.getTimestamp().toDate().getTime(),
                    System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS
            );
            holder.tvTime.setText(timeAgo);
        } else {
            holder.tvTime.setText("Just now");
        }

        // Unread styling
        if (!notification.isRead()) {
            holder.tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
            holder.viewUnreadIndicator.setVisibility(View.VISIBLE);
        } else {
            holder.tvTitle.setTypeface(null, android.graphics.Typeface.NORMAL);
            holder.viewUnreadIndicator.setVisibility(View.GONE);
        }

        // Icon based on type
        if ("chat_message".equals(notification.getType())) {
            holder.ivIcon.setImageResource(R.drawable.ic_chat);
        } else if ("order_update".equals(notification.getType()) || "order_status_update".equals(notification.getType())) {
            holder.ivIcon.setImageResource(R.drawable.ic_orders);
        } else if ("booking_update".equals(notification.getType()) || "status_update".equals(notification.getType())) {
            holder.ivIcon.setImageResource(R.drawable.ic_services);
        } else {
            holder.ivIcon.setImageResource(R.drawable.ic_notifications);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onNotificationClick(notification);
        });
    }

    @Override
    public int getItemCount() {
        return notificationList == null ? 0 : notificationList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTitle, tvMessage, tvTime;
        ImageView ivIcon;
        View viewUnreadIndicator;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvNotificationTitle);
            tvMessage = itemView.findViewById(R.id.tvNotificationMessage);
            tvTime = itemView.findViewById(R.id.tvNotificationTime);
            ivIcon = itemView.findViewById(R.id.ivNotificationIcon);
            viewUnreadIndicator = itemView.findViewById(R.id.viewUnreadIndicator);
        }
    }
}
