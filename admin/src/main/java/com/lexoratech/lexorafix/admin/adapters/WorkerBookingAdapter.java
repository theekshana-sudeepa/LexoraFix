package com.lexoratech.lexorafix.admin.adapters;

import android.content.Context;
import android.content.Intent;
import com.lexoratech.lexorafix.admin.activities.WorkerChatActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.firestore.Transaction;
import com.lexoratech.lexorafix.admin.R;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WorkerBookingAdapter extends RecyclerView.Adapter<WorkerBookingAdapter.ViewHolder> {

    public static final int TAB_AVAILABLE = 0;
    public static final int TAB_MY_JOBS = 1;

    private final Context context;
    private final List<Map<String, Object>> bookings;
    private int currentTab = TAB_AVAILABLE;

    public WorkerBookingAdapter(Context context, List<Map<String, Object>> bookings) {
        this.context = context;
        this.bookings = bookings;
    }

    public void setCurrentTab(int tab) {
        this.currentTab = tab;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_worker_booking, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Map<String, Object> booking = bookings.get(position);

        holder.tvServiceName.setText(getStr(booking, "serviceType", "Service"));

        String date = getStr(booking, "date", "");
        String time = getStr(booking, "time", "");
        String dateTime = "";
        if (!date.isEmpty()) dateTime += "📅 " + date;
        if (!time.isEmpty()) dateTime += "  🕐 " + time;
        holder.tvCategory.setText(dateTime.isEmpty() ? "" : dateTime);

        holder.tvCustomerName.setText(getStr(booking, "userName", "Unknown Customer"));

        String address = getStr(booking, "locationAddress", "");
        holder.tvAddress.setText(address.isEmpty() ? "📍 No address provided" : "📍 " + address);

        String notes = getStr(booking, "notes", "");
        if (!notes.isEmpty()) {
            holder.tvDescription.setVisibility(View.VISIBLE);
            holder.tvDescription.setText("📝 " + notes);
        } else {
            holder.tvDescription.setVisibility(View.GONE);
        }

        String status = getStr(booking, "status", "Pending");
        holder.tvStatus.setText(status);
        setStatusColors(holder, status);

        String docId = getStr(booking, "_documentId", "");

        if (currentTab == TAB_AVAILABLE) {
            holder.btnAction.setEnabled(true);
            holder.btnAction.setText("🤝  Accept Job");
            holder.btnAction.setBackgroundTintList(
                    android.content.res.ColorStateList.valueOf(ContextCompat.getColor(context, R.color.adminPrimary)));
            holder.btnAction.setOnClickListener(v -> acceptBooking(holder, booking, docId, position));
            holder.btnChat.setVisibility(View.GONE);
        } else {
            setupStatusButton(holder, status, booking, docId, position);
            // Show chat button on My Jobs tab for accepted/in-progress bookings
            if ("Accepted".equals(status) || "In Progress".equals(status)) {
                holder.btnChat.setVisibility(View.VISIBLE);
                holder.btnChat.setOnClickListener(v -> {
                    Intent chatIntent = new Intent(context, WorkerChatActivity.class);
                    chatIntent.putExtra(WorkerChatActivity.EXTRA_BOOKING_ID, docId);
                    chatIntent.putExtra(WorkerChatActivity.EXTRA_CUSTOMER_NAME, getStr(booking, "userName", "Customer"));
                    chatIntent.putExtra(WorkerChatActivity.EXTRA_SERVICE_TYPE, getStr(booking, "serviceType", "Service"));
                    context.startActivity(chatIntent);
                });
            } else {
                holder.btnChat.setVisibility(View.GONE);
            }
        }
    }

    private void setStatusColors(ViewHolder holder, String status) {
        int textColor, bgColor;
        switch (status) {
            case "Confirmed":
                textColor = R.color.adminInfo;
                bgColor = R.color.adminTintBlue;
                break;
            case "Accepted":
                textColor = R.color.adminInfo;
                bgColor = R.color.adminTintBlue;
                break;
            case "In Progress":
                textColor = R.color.adminWarning;
                bgColor = R.color.adminTintOrange;
                break;
            case "Completed":
                textColor = R.color.adminSuccess;
                bgColor = R.color.adminTintGreen;
                break;
            default:
                textColor = R.color.adminTextSecondary;
                bgColor = R.color.adminTintPurple;
                break;
        }
        holder.tvStatus.setTextColor(ContextCompat.getColor(context, textColor));
        holder.cardStatus.setCardBackgroundColor(ContextCompat.getColor(context, bgColor));
    }

    private void setupStatusButton(ViewHolder holder, String currentStatus, Map<String, Object> booking, String docId, int position) {
        holder.btnAction.setEnabled(true);
        switch (currentStatus) {
            case "Accepted":
                holder.btnAction.setText("🔧  Start Working");
                holder.btnAction.setBackgroundTintList(
                        android.content.res.ColorStateList.valueOf(ContextCompat.getColor(context, R.color.adminWarning)));
                holder.btnAction.setOnClickListener(v -> updateStatus(holder, booking, docId, "In Progress", position));
                break;
            case "In Progress":
                holder.btnAction.setText(" Mark Completed");
                holder.btnAction.setBackgroundTintList(
                        android.content.res.ColorStateList.valueOf(ContextCompat.getColor(context, R.color.adminSuccess)));
                holder.btnAction.setOnClickListener(v -> updateStatus(holder, booking, docId, "Completed", position));
                break;
            case "Completed":
                holder.btnAction.setText("✓  Completed");
                holder.btnAction.setEnabled(false);
                holder.btnAction.setBackgroundTintList(
                        android.content.res.ColorStateList.valueOf(ContextCompat.getColor(context, R.color.adminTintGreen)));
                break;
        }
    }

    private void acceptBooking(ViewHolder holder, Map<String, Object> booking, String docId, int position) {
        holder.btnAction.setEnabled(false);
        holder.btnAction.setText("Accepting...");

        String workerId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        String workerEmail = FirebaseAuth.getInstance().getCurrentUser().getEmail();

        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // First, fetch worker's profile to get their name and phone
        db.collection("users").document(workerId).get()
                .addOnSuccessListener(workerDoc -> {
                    String workerName = workerDoc.getString("name");
                    String workerPhone = workerDoc.getString("phone");
                    if (workerName == null) workerName = workerEmail;
                    if (workerPhone == null) workerPhone = "";

                    final String finalWorkerName = workerName;
                    final String finalWorkerPhone = workerPhone;

                    // Use a transaction to safely claim the booking
                    db.runTransaction((Transaction.Function<Void>) transaction -> {
                        com.google.firebase.firestore.DocumentReference docRef = db.collection("bookings").document(docId);
                        com.google.firebase.firestore.DocumentSnapshot snapshot = transaction.get(docRef);

                        String currentAssigned = snapshot.getString("assignedWorkerId");
                        if (currentAssigned != null && !currentAssigned.isEmpty()) {
                            throw new com.google.firebase.firestore.FirebaseFirestoreException(
                                    "Already taken",
                                    com.google.firebase.firestore.FirebaseFirestoreException.Code.ABORTED);
                        }

                        transaction.update(docRef, "assignedWorkerId", workerId);
                        transaction.update(docRef, "assignedWorkerName", finalWorkerName);
                        transaction.update(docRef, "assignedWorkerPhone", finalWorkerPhone);
                        transaction.update(docRef, "assignedWorkerEmail", workerEmail);
                        transaction.update(docRef, "status", "Accepted");
                        return null;
                    }).addOnSuccessListener(aVoid -> {
                        Toast.makeText(context, "Job accepted!", Toast.LENGTH_SHORT).show();

                        // Sync to customer's subcollection
                        syncToCustomerSubcollection(booking, docId, "Accepted", finalWorkerName, finalWorkerPhone, workerEmail);
                    }).addOnFailureListener(e -> {
                        holder.btnAction.setEnabled(true);
                        holder.btnAction.setText("🤝  Accept Job");
                        Toast.makeText(context, "This job was already taken by another worker.", Toast.LENGTH_LONG).show();
                    });
                })
                .addOnFailureListener(e -> {
                    holder.btnAction.setEnabled(true);
                    holder.btnAction.setText("🤝  Accept Job");
                    Toast.makeText(context, "Failed to fetch worker profile", Toast.LENGTH_SHORT).show();
                });
    }

    private void updateStatus(ViewHolder holder, Map<String, Object> booking, String docId, String newStatus, int position) {
        holder.btnAction.setEnabled(false);
        holder.btnAction.setText("Updating...");

        FirebaseFirestore.getInstance().collection("bookings").document(docId)
                .update("status", newStatus)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(context, "Status → " + newStatus, Toast.LENGTH_SHORT).show();

                    // Also sync to customer's subcollection
                    syncToCustomerSubcollection(booking, docId, newStatus, null, null, null);
                })
                .addOnFailureListener(e -> {
                    holder.btnAction.setEnabled(true);
                    Toast.makeText(context, "Failed to update status", Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Mirror status and worker details to the customer's bookings subcollection
     * so the customer app picks up changes via its existing snapshot listener.
     */
    private void syncToCustomerSubcollection(Map<String, Object> booking, String docId,
                                              String newStatus, String workerName,
                                              String workerPhone, String workerEmail) {
        String customerUid = getStr(booking, "firebaseUid", "");
        if (customerUid.isEmpty()) return;

        Map<String, Object> updates = new HashMap<>();
        updates.put("status", newStatus);

        if (workerName != null) {
            updates.put("assignedWorkerName", workerName);
            updates.put("assignedWorkerPhone", workerPhone);
            updates.put("assignedWorkerEmail", workerEmail);
        }

        FirebaseFirestore.getInstance()
                .collection("users").document(customerUid)
                .collection("bookings").document(docId)
                .set(updates, SetOptions.merge())
                .addOnSuccessListener(aVoid -> 
                    android.util.Log.d("WorkerBooking", "Synced status to customer subcollection"))
                .addOnFailureListener(e ->
                    android.util.Log.e("WorkerBooking", "Failed to sync: " + e.getMessage()));
    }

    @Override
    public int getItemCount() {
        return bookings.size();
    }

    private String getStr(Map<String, Object> map, String key, String def) {
        Object val = map.get(key);
        return val != null ? val.toString() : def;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvServiceName, tvCategory, tvCustomerName, tvAddress, tvDescription, tvStatus;
        MaterialCardView cardStatus;
        MaterialButton btnAction, btnChat;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvServiceName = itemView.findViewById(R.id.tv_service_name);
            tvCategory = itemView.findViewById(R.id.tv_category);
            tvCustomerName = itemView.findViewById(R.id.tv_customer_name);
            tvAddress = itemView.findViewById(R.id.tv_address);
            tvDescription = itemView.findViewById(R.id.tv_description);
            tvStatus = itemView.findViewById(R.id.tv_status);
            cardStatus = itemView.findViewById(R.id.card_status);
            btnAction = itemView.findViewById(R.id.btn_action);
            btnChat = itemView.findViewById(R.id.btn_chat);
        }
    }
}
