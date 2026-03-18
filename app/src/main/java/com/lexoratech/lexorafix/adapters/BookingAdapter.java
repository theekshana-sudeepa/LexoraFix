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

        // Random-ish rating based on position
        double[] ratings = {4.8, 4.9, 4.7, 4.6, 5.0, 4.5};
        holder.tvRating.setText(String.valueOf(ratings[position % ratings.length]));

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

    private void showBookingDetailsDialog(ServiceBooking booking) {
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
        MaterialButton btnCallWorker = view.findViewById(R.id.btn_call_worker);
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
        LinearLayout workerRow;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvServiceType = itemView.findViewById(R.id.tv_service_type);
            tvDate = itemView.findViewById(R.id.tv_date);
            tvTime = itemView.findViewById(R.id.tv_time);
            tvStatus = itemView.findViewById(R.id.tv_status);
            tvRating = itemView.findViewById(R.id.tv_rating);
            tvWorkerName = itemView.findViewById(R.id.tv_worker_name);
            workerRow = itemView.findViewById(R.id.worker_row);
        }
    }
}
