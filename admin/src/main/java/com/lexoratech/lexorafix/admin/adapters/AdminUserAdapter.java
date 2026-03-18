package com.lexoratech.lexorafix.admin.adapters;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.button.MaterialButton;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.firestore.SetOptions;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.lexoratech.lexorafix.admin.R;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminUserAdapter extends RecyclerView.Adapter<AdminUserAdapter.ViewHolder> {

    private final Context context;
    private final List<Map<String, Object>> users;

    public AdminUserAdapter(Context context, List<Map<String, Object>> users) {
        this.context = context;
        this.users = users;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_admin_user, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Map<String, Object> user = users.get(position);
        holder.tvName.setText(getStr(user, "name", "Unknown"));
        holder.tvEmail.setText(getStr(user, "email", "--"));
        
        String role = getStr(user, "role", "customer");
        holder.tvRole.setText(role);
        
        // Colorize role badge
        if ("admin".equalsIgnoreCase(role)) {
            holder.tvRole.setTextColor(ContextCompat.getColor(context, R.color.adminIconBlue));
            ((com.google.android.material.card.MaterialCardView)holder.tvRole.getParent()).setStrokeColor(
                    ContextCompat.getColor(context, R.color.adminTintBlue));
        } else if ("worker".equalsIgnoreCase(role)) {
             holder.tvRole.setTextColor(ContextCompat.getColor(context, R.color.adminWarning));
             ((com.google.android.material.card.MaterialCardView)holder.tvRole.getParent()).setStrokeColor(
                     ContextCompat.getColor(context, R.color.adminTintOrange));       
        } else {
             holder.tvRole.setTextColor(ContextCompat.getColor(context, R.color.adminIconPurple));
             ((com.google.android.material.card.MaterialCardView)holder.tvRole.getParent()).setStrokeColor(
                     ContextCompat.getColor(context, R.color.adminTintPurple));
        }

        String uid = getStr(user, "uid", "--");
        holder.tvUid.setText("UID: " + uid);

        String status = getStr(user, "status", "Active");
        holder.tvStatus.setText(status);

        if ("worker".equals(role) && !Boolean.TRUE.equals(user.get("adminApproved"))) {
            // Worker is pending approval
            holder.cardStatus.setCardBackgroundColor(ContextCompat.getColor(context, R.color.adminWarning));
            holder.btnBan.setText("Approve Worker");
            holder.btnBan.setTextColor(ContextCompat.getColor(context, R.color.adminIconOrange));
            holder.btnBan.setBackgroundTintList(
                    ColorStateList.valueOf(ContextCompat.getColor(context, R.color.adminTintOrange)));
            holder.btnBan.setStrokeWidth(0);

            holder.btnBan.setOnClickListener(v -> {
                holder.btnBan.setEnabled(false);
                holder.btnBan.setText("Approving...");

                Map<String, Object> updates = new HashMap<>();
                updates.put("adminApproved", true);
                updates.put("status", "Active");

                FirebaseFirestore.getInstance().collection("users").document(uid)
                        .set(updates, SetOptions.merge())
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(context, "Worker Approved", Toast.LENGTH_SHORT).show();
                            user.put("adminApproved", true);
                            user.put("status", "Active");
                            notifyItemChanged(position);

                            // Send Approval Email
                            String email = getStr(user, "email", "");
                            String name = getStr(user, "name", "Worker");
                            if (!email.isEmpty()) {
                                com.lexoratech.lexorafix.admin.network.EmailHelper.getInstance()
                                        .sendWorkerApprovalEmail(email, name, true);
                            }
                        })
                        .addOnFailureListener(e -> {
                            holder.btnBan.setEnabled(true);
                            holder.btnBan.setText("Approve Worker");
                            Toast.makeText(context, "Failed to approve", Toast.LENGTH_SHORT).show();
                        });
            });

        } else {
            // Standard ban/unban logic
            if ("Banned".equals(status)) {
                holder.cardStatus.setCardBackgroundColor(ContextCompat.getColor(context, R.color.adminError));
                holder.btnBan.setText("Unban User");
                holder.btnBan.setTextColor(ContextCompat.getColor(context, R.color.adminIconGreen));
                holder.btnBan.setBackgroundTintList(
                        ColorStateList.valueOf(ContextCompat.getColor(context, R.color.adminTintGreen)));
                holder.btnBan.setStrokeWidth(0);
            } else {
                holder.cardStatus.setCardBackgroundColor(ContextCompat.getColor(context, R.color.adminSuccess));
                holder.btnBan.setText("Ban User");
                holder.btnBan.setTextColor(ContextCompat.getColor(context, R.color.adminIconRed));
                holder.btnBan.setBackgroundTintList(
                        ColorStateList.valueOf(ContextCompat.getColor(context, R.color.adminTintRed)));
                holder.btnBan.setStrokeWidth(0);
            }

            holder.btnBan.setOnClickListener(v -> {
                boolean isBanning = "Active".equals(status);
                String newStatus = isBanning ? "Banned" : "Active";

                holder.btnBan.setEnabled(false);
                holder.btnBan.setText("Updating...");

                FirebaseFirestore.getInstance().collection("users").document(uid)
                        .set(java.util.Collections.singletonMap("status", newStatus), SetOptions.merge())
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(context, "User " + newStatus, Toast.LENGTH_SHORT).show();
                            user.put("status", newStatus);
                            notifyItemChanged(position);

                            String email = getStr(user, "email", "");
                            String name = getStr(user, "name", "User");
                            if (!email.isEmpty()) {
                                com.lexoratech.lexorafix.admin.network.EmailHelper.getInstance()
                                        .sendBanStatusEmail(email, name, "Banned".equals(newStatus));
                            }
                        })
                        .addOnFailureListener(e -> {
                            holder.btnBan.setEnabled(true);
                            Toast.makeText(context, "Failed to update status", Toast.LENGTH_SHORT).show();
                        });
            });
        }
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    private String getStr(Map<String, Object> map, String key, String def) {
        Object val = map.get(key);
        return val != null ? val.toString() : def;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvEmail, tvRole, tvUid, tvStatus;
        MaterialButton btnBan;
        MaterialCardView cardStatus;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_name);
            tvEmail = itemView.findViewById(R.id.tv_email);
            tvRole = itemView.findViewById(R.id.tv_role);
            tvUid = itemView.findViewById(R.id.tv_uid);
            tvStatus = itemView.findViewById(R.id.tv_status);
            btnBan = itemView.findViewById(R.id.btn_ban);
            cardStatus = itemView.findViewById(R.id.card_status);
        }
    }
}
