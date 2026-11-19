package com.lexoratech.lexorafix.adapters;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.lexoratech.lexorafix.R;
import com.lexoratech.lexorafix.models.ServiceBooking;

import java.util.List;

public class BookingAdapter extends RecyclerView.Adapter<BookingAdapter.ViewHolder> {

    private final List<ServiceBooking> bookings;
    private final Context context;

    public BookingAdapter(Context context, List<ServiceBooking> bookings) {
        this.context = context;
        this.bookings = bookings;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_booking, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ServiceBooking booking = bookings.get(position);
        holder.tvServiceType.setText(booking.getServiceType() + " Service");
        holder.tvDate.setText(booking.getDate());
        holder.tvTime.setText(booking.getTime());

        // Show real rating if booking has been rated
        Boolean isRated = booking.getIsRated();
        if (isRated != null && isRated && booking.getRating() != null) {
            holder.ivStar.setVisibility(android.view.View.VISIBLE);
            holder.tvRating.setVisibility(android.view.View.VISIBLE);
            holder.tvRating.setText(String.format(java.util.Locale.getDefault(), "%.1f", booking.getRating()));
        } else {
            holder.ivStar.setVisibility(android.view.View.GONE);
            holder.tvRating.setVisibility(android.view.View.GONE);
        }

        String status = booking.getStatus();
        holder.tvStatus.setText(status);
        setStatusBadge(holder.tvStatus, status);

        // Worker info
        String workerName = booking.getAssignedWorkerName();
        if (workerName != null && !workerName.isEmpty()) {
            holder.workerRow.setVisibility(View.VISIBLE);
            holder.tvWorkerName.setText(workerName);
        } else {
            holder.workerRow.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> showBookingDetailsDialog(booking));
    }

    private void setStatusBadge(TextView tvStatus, String status) {
        if (status == null) return;

        int textColor;
        int bgColor;
        switch (status) {
            case "Confirmed":
            case "Accepted":
                textColor = 0xFF0CA678;
                bgColor = 0x1A0CA678;
                break;
            case "In Progress":
                textColor = 0xFFF59F00;
                bgColor = 0x1AF59F00;
                break;
            case "Completed":
                textColor = 0xFF2563EB;
                bgColor = 0x1A2563EB;
                break;
            case "Cancelled":
                textColor = 0xFFE03131;
                bgColor = 0x1AE03131;
                break;
            case "Pending":
            default:
                textColor = 0xFFFF922B;
                bgColor = 0x1AFF922B;
                break;
        }

        tvStatus.setTextColor(textColor);
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(bgColor);
        bg.setCornerRadius(20f);
        tvStatus.setBackground(bg);
    }

    public void showBookingDetailsDialog(ServiceBooking booking) {
        BottomSheetDialog dialog = new BottomSheetDialog(context, R.style.AppBottomSheetDialogTheme);
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_booking_status, null);

        TextView tvServiceType = view.findViewById(R.id.tv_dialog_service_type);
        TextView tvDateTime = view.findViewById(R.id.tv_dialog_date_time);
        TextView tvStatus = view.findViewById(R.id.tv_dialog_status);
        TextView tvNotes = view.findViewById(R.id.tv_dialog_notes);
        MaterialButton btnClose = view.findViewById(R.id.btn_dialog_close);

        // Worker info views
        LinearLayout workerInfoSection = view.findViewById(R.id.worker_info_section);
        TextView tvWorkerName = view.findViewById(R.id.tv_dialog_worker_name);
        TextView tvWorkerPhone = view.findViewById(R.id.tv_dialog_worker_phone);
        TextView tvWorkerRating = view.findViewById(R.id.tv_dialog_worker_rating);
        MaterialButton btnCallWorker = view.findViewById(R.id.btn_call_worker);
        MaterialButton btnChatWorker = view.findViewById(R.id.btn_chat_worker);
        MaterialButton btnRateWorker = view.findViewById(R.id.btn_rate_worker);

        tvServiceType.setText(booking.getServiceType());
        tvDateTime.setText(booking.getDate() + " at " + booking.getTime());
        tvStatus.setText(booking.getStatus());
        setStatusBadge(tvStatus, booking.getStatus());

        if (booking.getNotes() != null && !booking.getNotes().isEmpty()) {
            tvNotes.setText(booking.getNotes());
        }

        // Show worker info if a worker has been assigned
        String workerName = booking.getAssignedWorkerName();
        String workerPhone = booking.getAssignedWorkerPhone();
        if (workerName != null && !workerName.isEmpty()) {
            workerInfoSection.setVisibility(View.VISIBLE);
            tvWorkerName.setText(workerName);
            tvWorkerPhone.setText(workerPhone != null && !workerPhone.isEmpty()
                    ? "📞 " + workerPhone
                    : "No phone provided");

            String workerEmail = booking.getAssignedWorkerEmail();
            if (workerEmail != null && !workerEmail.isEmpty() && tvWorkerRating != null) {
                com.lexoratech.lexorafix.data.FirestoreHelper.getInstance().getWorkerAverageRatingByEmail(workerEmail, new com.lexoratech.lexorafix.data.FirestoreHelper.FirestoreCallback<android.util.Pair<Float, Integer>>() {
                    @Override
                    public void onSuccess(android.util.Pair<Float, Integer> result) {
                        tvWorkerRating.setVisibility(View.VISIBLE);
                        if (result.second > 0) {
                            tvWorkerRating.setText(String.format("⭐ %.1f (%d reviews)", result.first, result.second));
                        } else {
                            tvWorkerRating.setText("⭐ No reviews yet");
                        }
                    }
                    @Override
                    public void onError(String message) {
                        tvWorkerRating.setVisibility(View.GONE);
                    }
                });
            }

            btnCallWorker.setOnClickListener(v -> {
                if (workerPhone != null && !workerPhone.isEmpty()) {
                    Intent callIntent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + workerPhone));
                    context.startActivity(callIntent);
                }
            });

            // Disable call button if no phone
            if (workerPhone == null || workerPhone.isEmpty()) {
                btnCallWorker.setEnabled(false);
                btnCallWorker.setText("No phone available");
            }

            // Chat with Worker button
            btnChatWorker.setOnClickListener(v -> {
                dialog.dismiss();
                Intent chatIntent = new Intent(context, com.lexoratech.lexorafix.activities.ChatActivity.class);
                chatIntent.putExtra(com.lexoratech.lexorafix.activities.ChatActivity.EXTRA_BOOKING_ID, booking.getOrderId());
                chatIntent.putExtra(com.lexoratech.lexorafix.activities.ChatActivity.EXTRA_WORKER_NAME, workerName);
                chatIntent.putExtra(com.lexoratech.lexorafix.activities.ChatActivity.EXTRA_SERVICE_TYPE, booking.getServiceType());
                context.startActivity(chatIntent);
            });

            // Show Rate Worker button if completed and not rated
            if ("Completed".equals(booking.getStatus())) {
                boolean isRated = booking.getIsRated() != null && booking.getIsRated();
                if (!isRated) {
                    btnRateWorker.setVisibility(View.VISIBLE);
                    btnRateWorker.setOnClickListener(v -> {
                        dialog.dismiss();
                        if (context instanceof androidx.fragment.app.FragmentActivity) {
                            com.lexoratech.lexorafix.fragments.RateWorkerBottomSheetFragment rateFragment =
                                    com.lexoratech.lexorafix.fragments.RateWorkerBottomSheetFragment.newInstance(
                                            booking.getOrderId(),
                                            () -> {
                                                booking.setIsRated(true);
                                                notifyDataSetChanged();
                                            }
                                    );
                            rateFragment.show(((androidx.fragment.app.FragmentActivity) context).getSupportFragmentManager(), "RateWorkerBottomSheet");
                        }
                    });
                } else {
                    btnRateWorker.setVisibility(View.VISIBLE);
                    btnRateWorker.setText("⭐ Rated " + (booking.getRating() != null ? booking.getRating() : "5.0"));
                    btnRateWorker.setEnabled(false);
                }
            }
        }

        btnClose.setOnClickListener(v -> dialog.dismiss());

        dialog.setContentView(view);
        dialog.show();
    }

    @Override
    public int getItemCount() {
        return bookings.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvServiceType, tvDate, tvTime, tvStatus, tvRating, tvWorkerName;
        android.widget.ImageView ivStar;
        LinearLayout workerRow;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvServiceType = itemView.findViewById(R.id.tv_service_type);
            tvDate = itemView.findViewById(R.id.tv_date);
            tvTime = itemView.findViewById(R.id.tv_time);
            tvStatus = itemView.findViewById(R.id.tv_status);
            tvRating = itemView.findViewById(R.id.tv_rating);
            ivStar = itemView.findViewById(R.id.iv_star);
            tvWorkerName = itemView.findViewById(R.id.tv_worker_name);
            workerRow = itemView.findViewById(R.id.worker_row);
        }
    }
}
