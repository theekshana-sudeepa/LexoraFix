package com.lexoratech.lexorafix.adapters;

import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.lexoratech.lexorafix.R;
import com.lexoratech.lexorafix.models.ServiceCenter;

import java.util.List;
import java.util.Locale;

public class ServiceCenterAdapter extends RecyclerView.Adapter<ServiceCenterAdapter.ViewHolder> {

    private final List<ServiceCenter> centers;
    private final Context context;
    private Location userLocation;

    public ServiceCenterAdapter(Context context, List<ServiceCenter> centers, @Nullable Location userLocation) {
        this.context = context;
        this.centers = centers;
        this.userLocation = userLocation;
    }

    public void setUserLocation(@Nullable Location location) {
        this.userLocation = location;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_service_center, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ServiceCenter center = centers.get(position);
        holder.tvName.setText(center.getName());
        holder.tvAddress.setText(center.getAddress());

        // Phone number in bottom bar
        if (holder.tvPhone != null) {
            holder.tvPhone.setText(center.getPhone() != null ? center.getPhone() : "N/A");
        }

        // Distance badge
        if (holder.tvDistance != null) {
            if (userLocation != null && center.getLatitude() != 0 && center.getLongitude() != 0) {
                float[] results = new float[1];
                Location.distanceBetween(
                        userLocation.getLatitude(), userLocation.getLongitude(),
                        center.getLatitude(), center.getLongitude(),
                        results);
                float distanceKm = results[0] / 1000f;
                if (distanceKm < 1) {
                    holder.tvDistance.setText(String.format(Locale.getDefault(), "📍 %.0f m away", results[0]));
                } else {
                    holder.tvDistance.setText(String.format(Locale.getDefault(), "📍 %.1f km away", distanceKm));
                }
                holder.tvDistance.setVisibility(View.VISIBLE);
            } else {
                holder.tvDistance.setVisibility(View.GONE);
            }
        }

        holder.btnCall.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_DIAL, Uri.parse("tel:" + center.getPhone()));
            context.startActivity(intent);
        });

        holder.itemView.setOnClickListener(v -> {
            // Open in maps
            String uri = "geo:" + center.getLatitude() + "," + center.getLongitude() +
                    "?q=" + center.getLatitude() + "," + center.getLongitude() +
                    "(" + center.getName() + ")";
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return centers.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvAddress, tvDistance, tvPhone;
        View btnCall;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_name);
            tvAddress = itemView.findViewById(R.id.tv_address);
            tvDistance = itemView.findViewById(R.id.tv_distance);
            tvPhone = itemView.findViewById(R.id.tv_phone);
            btnCall = itemView.findViewById(R.id.btn_call);
        }
    }
}
