package com.lexoratech.lexorafix.admin.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.lexoratech.lexorafix.admin.R;

import java.util.List;
import java.util.Map;

public class AdminServiceCenterAdapter extends RecyclerView.Adapter<AdminServiceCenterAdapter.ViewHolder> {

    private final Context context;
    private final List<Map<String, Object>> centerList;
    private OnCenterActionListener listener;

    public interface OnCenterActionListener {
        void onEdit(String documentId, Map<String, Object> centerData);

        void onDelete(String documentId, String centerName);
    }

    public AdminServiceCenterAdapter(Context context, List<Map<String, Object>> centerList) {
        this.context = context;
        this.centerList = centerList;
    }

    public void setOnCenterActionListener(OnCenterActionListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_service_center, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Map<String, Object> centerData = centerList.get(position);

        String name = centerData.containsKey("name") ? (String) centerData.get("name") : "Unknown Center";
        String address = centerData.containsKey("address") ? (String) centerData.get("address") : "";
        String phone = centerData.containsKey("phone") ? (String) centerData.get("phone") : "";

        holder.tvName.setText(name);
        holder.tvAddress.setText(address);
        holder.tvPhone.setText(phone);

        holder.btnEdit.setOnClickListener(v -> {
            if (listener != null) {
                String docId = (String) centerData.get("_documentId");
                listener.onEdit(docId, centerData);
            }
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) {
                String docId = (String) centerData.get("_documentId");
                listener.onDelete(docId, name);
            }
        });
    }

    @Override
    public int getItemCount() {
        return centerList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvAddress, tvPhone;
        View btnEdit, btnDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_center_name);
            tvAddress = itemView.findViewById(R.id.tv_center_address);
            tvPhone = itemView.findViewById(R.id.tv_center_phone);
            btnEdit = itemView.findViewById(R.id.btn_edit);
            btnDelete = itemView.findViewById(R.id.btn_delete);
        }
    }
}
