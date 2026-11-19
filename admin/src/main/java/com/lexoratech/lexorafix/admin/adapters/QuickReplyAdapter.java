package com.lexoratech.lexorafix.admin.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.lexoratech.lexorafix.admin.R;

import java.util.List;

public class QuickReplyAdapter extends RecyclerView.Adapter<QuickReplyAdapter.ViewHolder> {

    private final Context context;
    private final List<String> quickReplies;
    private final OnQuickReplyClickListener listener;

    public interface OnQuickReplyClickListener {
        void onQuickReplyClick(String replyText);
    }

    public QuickReplyAdapter(Context context, List<String> quickReplies, OnQuickReplyClickListener listener) {
        this.context = context;
        this.quickReplies = quickReplies;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_quick_reply, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String reply = quickReplies.get(position);
        holder.tvQuickReply.setText(reply);
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onQuickReplyClick(reply);
            }
        });
    }

    @Override
    public int getItemCount() {
        return quickReplies == null ? 0 : quickReplies.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvQuickReply;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvQuickReply = itemView.findViewById(R.id.tvQuickReply);
        }
    }
}
