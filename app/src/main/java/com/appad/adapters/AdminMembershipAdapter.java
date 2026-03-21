package com.appad.adapters;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.appad.R;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AdminMembershipAdapter extends RecyclerView.Adapter<AdminMembershipAdapter.ViewHolder> {

    private Context context;
    private List<Map<String, Object>> memberships;
    private SimpleDateFormat sdfInput = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
    private SimpleDateFormat sdfOutput = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());

    public AdminMembershipAdapter(Context context, List<Map<String, Object>> memberships) {
        this.context = context;
        this.memberships = memberships;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_admin_membership, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Map<String, Object> item = memberships.get(position);
        
        String username = (String) item.get("username");
        String fullName = (String) item.get("full_name");
        holder.txtName.setText(fullName != null && !fullName.isEmpty() ? fullName : username);
        
        holder.txtPlan.setText("Hội viên: " + (item.get("artist_name") != null ? item.get("artist_name") : "Hệ thống"));
        
        String status = (String) item.get("status");
        boolean isActive = "active".equals(status);
        holder.txtStatus.setText(isActive ? "Đang hoạt động" : "Hết hạn");
        holder.txtStatus.setTextColor(isActive ? Color.parseColor("#10B981") : Color.parseColor("#EF4444"));
        holder.txtStatus.getBackground().setTint(isActive ? Color.parseColor("#2010B981") : Color.parseColor("#20EF4444"));

        double price = 0;
        if (item.get("price_paid") != null) price = ((Number) item.get("price_paid")).doubleValue();
        NumberFormat nf = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        holder.txtPrice.setText(nf.format(price));

        try {
            String startStr = (String) item.get("start_date");
            String expiryStr = (String) item.get("expiry_date");
            if (startStr != null && expiryStr != null) {
                Date start = sdfInput.parse(startStr.substring(0, 10));
                Date end = sdfInput.parse(expiryStr.substring(0, 10));
                holder.txtDates.setText(sdfOutput.format(start) + " - " + sdfOutput.format(end));
            }
        } catch (Exception e) {
            holder.txtDates.setText("N/A");
        }
    }

    @Override
    public int getItemCount() {
        return memberships.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtName, txtPlan, txtStatus, txtPrice, txtDates;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtName = itemView.findViewById(R.id.txtMemberName);
            txtPlan = itemView.findViewById(R.id.txtPlanName);
            txtStatus = itemView.findViewById(R.id.txtMemberStatus);
            txtPrice = itemView.findViewById(R.id.txtPricePaid);
            txtDates = itemView.findViewById(R.id.txtExpiryDates);
        }
    }
}
