package com.lexoratech.lexorafix.admin.adapters;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import android.widget.LinearLayout;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.lexoratech.lexorafix.admin.R;
import com.lexoratech.lexorafix.admin.activities.ManageBookingsActivity;

import java.util.List;
import java.util.Map;

public class AdminBookingAdapter extends RecyclerView.Adapter<AdminBookingAdapter.ViewHolder> {

    private final Context context;
    private final List<Map<String, Object>> bookings;
    private final String[] statuses = { "Confirmed", "In Progress", "Completed", "Cancelled" };

    public AdminBookingAdapter(Context context, List<Map<String, Object>> bookings) {
        this.context = context;
        this.bookings = bookings;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_admin_booking, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Map<String, Object> booking = bookings.get(position);

        // Service type
        String serviceType = getStr(booking, "serviceType", "Service");
        holder.tvServiceType.setText("🔧 " + serviceType);

        // Customer name
        String customerName = getStr(booking, "userName", "");
        holder.tvCustomerName.setText(customerName.isEmpty() ? "Unknown Customer" : customerName);

        // Date + time
        String date = getStr(booking, "date", "--");
        String time = getStr(booking, "time", "--");
        holder.tvDate.setText("📅 " + date + " at " + time);

        // Notes
        String notes = getStr(booking, "notes", "");
        holder.tvNotes.setText(notes.isEmpty() ? "📝 No notes" : "📝 " + notes);

        // Status badge
        String status = getStr(booking, "status", "Unknown");
        holder.tvStatus.setText(status);
        setStatusBadgeColor(holder.tvStatus, status);

        // Location
        String locationAddr = getStr(booking, "locationAddress", "");
        Object lat = booking.get("latitude");
        Object lng = booking.get("longitude");

        if (!locationAddr.isEmpty() || (lat != null && lng != null)) {
            holder.layoutLocation.setVisibility(View.VISIBLE);

            if (!locationAddr.isEmpty()) {
                holder.tvLocation.setText(locationAddr);
            } else {
                holder.tvLocation.setText(String.format(java.util.Locale.US,
                        "%.4f, %.4f", Double.parseDouble(lat.toString()),
                        Double.parseDouble(lng.toString())));
            }

            holder.layoutLocation.setOnClickListener(v -> {
                String uri;
                if (lat != null && lng != null) {
                    // Coordinates + Label
                    String label = locationAddr.isEmpty() ? "Service Location" : locationAddr;
                    uri = "geo:0,0?q=" + lat + "," + lng + "(" + Uri.encode(label) + ")";
                } else {
                    // Address only (fallback)
                    uri = "geo:0,0?q=" + Uri.encode(locationAddr);
                }

                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
                try {
                    context.startActivity(intent);
                } catch (Exception e) {
                    // Try web browser fallback
                    String url = "https://www.google.com/maps/search/?api=1&query=";
                    if (lat != null && lng != null) {
                        url += lat + "," + lng;
                    } else {
                        url += Uri.encode(locationAddr);
                    }
                    Intent webIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                    context.startActivity(webIntent);
                }
            });

        } else {
            holder.layoutLocation.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos != RecyclerView.NO_POSITION && context instanceof ManageBookingsActivity) {
                showBookingStatusDialog(context, pos);
            }
        });
    }

    private void setStatusBadgeColor(TextView tv, String status) {
        int color;
        switch (status) {
            case "Completed":
                color = 0xFF10B981; // Teal
                break;
            case "Cancelled":
                color = 0xFFEF4444; // Red
                break;
            case "In Progress":
                color = 0xFFF59E0B; // Orange
                break;
            case "Confirmed":
                color = 0xFF3B82F6; // Blue
                break;
            default:
                color = 0xFF6366F1; // Indigo
                break;
        }
        tv.setTextColor(color);
        if (tv.getBackground() instanceof GradientDrawable) {
            GradientDrawable bg = (GradientDrawable) tv.getBackground().mutate();
            int alphaColor = (color & 0x00FFFFFF) | 0x33000000; // 20% alpha
            bg.setColor(alphaColor);
        }
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
        TextView tvServiceType, tvCustomerName, tvDate, tvStatus, tvNotes, tvLocation;
        View layoutLocation;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvServiceType = itemView.findViewById(R.id.tv_service_type);
            tvCustomerName = itemView.findViewById(R.id.tv_customer_name);
            tvDate = itemView.findViewById(R.id.tv_date);
            tvStatus = itemView.findViewById(R.id.tv_status);
            tvNotes = itemView.findViewById(R.id.tv_notes);
            tvLocation = itemView.findViewById(R.id.tv_location);
            layoutLocation = itemView.findViewById(R.id.layout_location);
        }
    }
    private void showBookingStatusDialog(Context context, int pos) {
        BottomSheetDialog dialog = new BottomSheetDialog(context);
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_booking_status, null);
        dialog.setContentView(view);
        
        // Ensure transparent background around the top curved corners
        if (dialog.getWindow() != null) {
            dialog.getWindow().findViewById(com.google.android.material.R.id.design_bottom_sheet).setBackgroundResource(android.R.color.transparent);
        }

        LinearLayout llOptions = view.findViewById(R.id.ll_status_options);

        for (String s : statuses) {
            View optionView = LayoutInflater.from(context).inflate(R.layout.item_status_option, llOptions, false);
            TextView tvName = optionView.findViewById(R.id.tv_status_name);
            
            View vBg = optionView.findViewById(R.id.v_icon_bg);

            tvName.setText(s);

            // Set standard icon mappings and tint backgrounds based on the statuses
            if (s.equals("Completed")) {
                vBg.setBackgroundResource(R.drawable.bg_icon_tint_green);
                
            } else if (s.equals("Cancelled")) {
                vBg.setBackgroundResource(R.drawable.bg_icon_tint_red);
                
            } else if (s.equals("In Progress")) {
                vBg.setBackgroundResource(R.drawable.bg_icon_tint_orange);
                
            } else {
                vBg.setBackgroundResource(R.drawable.bg_icon_tint_blue);
                
            }

            optionView.setOnClickListener(v -> {
                dialog.dismiss();
                ((ManageBookingsActivity) context).updateBookingStatus(pos, s);
            });

            llOptions.addView(optionView);
        }

        dialog.show();
    }
}
