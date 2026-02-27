package com.lexoratech.lexorafix.adapters;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.lexoratech.lexorafix.R;
import com.lexoratech.lexorafix.models.ServiceCenter;

import java.util.List;

public class ServiceCenterAdapter extends RecyclerView.Adapter<ServiceCenterAdapter.ViewHolder> {

    private final List<ServiceCenter> centers;
    private final Context context;

    public ServiceCenterAdapter(Context context, List<ServiceCenter> centers) {
        this.context = context;
        this.centers = centers;
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
        TextView tvName, tvAddress;
        ImageView btnCall;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_name);
            tvAddress = itemView.findViewById(R.id.tv_address);
            btnCall = itemView.findViewById(R.id.btn_call);
        }
    }
}
