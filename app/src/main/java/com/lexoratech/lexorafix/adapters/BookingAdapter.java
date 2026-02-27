package com.lexoratech.lexorafix.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

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
        holder.tvStatus.setText(booking.getStatus());

        if ("Confirmed".equals(booking.getStatus()) || "Completed".equals(booking.getStatus())) {
            holder.tvStatus.setTextColor(context.getResources().getColor(R.color.colorSuccess, null));
        } else if ("Cancelled".equals(booking.getStatus())) {
            holder.tvStatus.setTextColor(context.getResources().getColor(R.color.colorError, null));
        }

        holder.itemView.setOnClickListener(v -> showBookingDetailsDialog(booking));
    }

    private void showBookingDetailsDialog(ServiceBooking booking) {
        com.google.android.material.bottomsheet.BottomSheetDialog dialog = new com.google.android.material.bottomsheet.BottomSheetDialog(
                context, R.style.AppBottomSheetDialogTheme);

        View view = LayoutInflater.from(context).inflate(R.layout.dialog_booking_status, null);

        TextView tvServiceType = view.findViewById(R.id.tv_dialog_service_type);
        TextView tvDateTime = view.findViewById(R.id.tv_dialog_date_time);
        TextView tvStatus = view.findViewById(R.id.tv_dialog_status);
        TextView tvNotes = view.findViewById(R.id.tv_dialog_notes);
        com.google.android.material.button.MaterialButton btnClose = view.findViewById(R.id.btn_dialog_close);

        tvServiceType.setText(booking.getServiceType());
        tvDateTime.setText(booking.getDate() + " at " + booking.getTime());
        tvStatus.setText(booking.getStatus());

        if (booking.getNotes() != null && !booking.getNotes().isEmpty()) {
            tvNotes.setText(booking.getNotes());
        }

        if ("Confirmed".equals(booking.getStatus()) || "Completed".equals(booking.getStatus())) {
            tvStatus.setTextColor(context.getResources().getColor(R.color.colorSuccess, null));
        } else if ("Cancelled".equals(booking.getStatus())) {
            tvStatus.setTextColor(context.getResources().getColor(R.color.colorError, null));
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
