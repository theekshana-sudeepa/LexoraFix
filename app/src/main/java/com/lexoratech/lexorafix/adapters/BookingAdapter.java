package com.lexoratech.lexorafix.adapters;

import android.content.Context;
import android.content.Intent;
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
        holder.tvServiceType.setText(booking.getServiceType());
        holder.tvDate.setText(booking.getDate() + " at " + booking.getTime());

        String status = booking.getStatus();
        holder.tvStatus.setText(status);
        setStatusColor(holder.tvStatus, status);

        holder.itemView.setOnClickListener(v -> showBookingDetailsDialog(booking));
    }

    private void setStatusColor(TextView tvStatus, String status) {
        if (status == null) return;
        switch (status) {
            case "Accepted":
                tvStatus.setTextColor(context.getResources().getColor(R.color.colorPrimary, null));
                break;
            case "In Progress":
                tvStatus.setTextColor(context.getResources().getColor(R.color.colorWarning, null));
                break;
            case "Completed":
                tvStatus.setTextColor(context.getResources().getColor(R.color.colorSuccess, null));
                break;
            case "Confirmed":
                tvStatus.setTextColor(context.getResources().getColor(R.color.colorSuccess, null));
                break;
            case "Cancelled":
                tvStatus.setTextColor(context.getResources().getColor(R.color.colorError, null));
                break;
            default:
                tvStatus.setTextColor(context.getResources().getColor(R.color.textSecondary, null));
                break;
        }
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

        tvServiceType.setText(booking.getServiceType());
        tvDateTime.setText(booking.getDate() + " at " + booking.getTime());
        tvStatus.setText(booking.getStatus());
        setStatusColor(tvStatus, booking.getStatus());

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
        TextView tvServiceType, tvDate, tvStatus;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvServiceType = itemView.findViewById(R.id.tv_service_type);
            tvDate = itemView.findViewById(R.id.tv_date);
            tvStatus = itemView.findViewById(R.id.tv_status);
        }
    }
}
