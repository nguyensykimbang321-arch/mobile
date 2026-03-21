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
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AdminTransactionAdapter extends RecyclerView.Adapter<AdminTransactionAdapter.ViewHolder> {

    private Context context;
    private List<Map<String, Object>> transactions;
    private OnTransactionActionEventListener listener;

    public interface OnTransactionActionEventListener {
        void onApprove(Map<String, Object> transaction);
        void onReject(Map<String, Object> transaction);
    }

    public AdminTransactionAdapter(Context context, List<Map<String, Object>> transactions, OnTransactionActionEventListener listener) {
        this.context = context;
        this.transactions = transactions;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_admin_transaction, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Map<String, Object> item = transactions.get(position);
        
        String fullName = (String) item.get("full_name");
        String username = (String) item.get("username");
        String email = (String) item.get("email");
        
        holder.txtName.setText(fullName != null && !fullName.isEmpty() ? fullName : (username != null ? username : email));
        holder.txtEmail.setText(email);
        
        String status = (String) item.get("status");
        boolean isPending = "pending".equalsIgnoreCase(status);
        boolean isCompleted = "completed".equalsIgnoreCase(status);
        
        holder.txtStatus.setText(isPending ? "Chờ duyệt" : isCompleted ? "Thành công" : "Đã hủy");
        holder.txtStatus.setTextColor(isPending ? Color.parseColor("#F59E0B") : isCompleted ? Color.parseColor("#10B981") : Color.parseColor("#EF4444"));
        holder.txtStatus.getBackground().setTint(isPending ? Color.parseColor("#20F59E0B") : isCompleted ? Color.parseColor("#2010B981") : Color.parseColor("#20EF4444"));

        double amount = 0;
        if (item.get("amount") != null) amount = ((Number) item.get("amount")).doubleValue();
        NumberFormat nf = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        holder.txtAmount.setText(nf.format(amount));
        
        String desc = (String) item.get("description");
        if (desc != null && !desc.isEmpty()) {
            holder.layoutDesc.setVisibility(View.VISIBLE);
            holder.txtDesc.setText(desc);
        } else {
            holder.layoutDesc.setVisibility(View.GONE);
        }
        
        String createdAt = (String) item.get("created_at");
        holder.txtDate.setText("📅 " + (createdAt != null ? createdAt.replace("T", " ").substring(0, 16) : "Unknown"));

        String ref = (String) item.get("reference_code");
        if (ref != null) {
            holder.txtRef.setText("Ref: " + ref);
            holder.txtRef.setVisibility(View.VISIBLE);
        } else {
            holder.txtRef.setVisibility(View.GONE);
        }

        holder.layoutActions.setVisibility(isPending ? View.VISIBLE : View.GONE);
        holder.btnApprove.setOnClickListener(v -> listener.onApprove(item));
        holder.btnReject.setOnClickListener(v -> listener.onReject(item));
    }

    @Override
    public int getItemCount() {
        return transactions.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtName, txtEmail, txtStatus, txtAmount, txtDesc, txtDate, txtRef;
        View layoutDesc, layoutActions, btnApprove, btnReject;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtName = itemView.findViewById(R.id.txtUserName);
            txtEmail = itemView.findViewById(R.id.txtUserEmail);
            txtStatus = itemView.findViewById(R.id.txtTransactionStatus);
            txtAmount = itemView.findViewById(R.id.txtAmount);
            txtDesc = itemView.findViewById(R.id.txtDescription);
            txtDate = itemView.findViewById(R.id.txtDate);
            txtRef = itemView.findViewById(R.id.txtRef);
            layoutDesc = itemView.findViewById(R.id.layoutDescription);
            layoutActions = itemView.findViewById(R.id.layoutActions);
            btnApprove = itemView.findViewById(R.id.btnApprove);
            btnReject = itemView.findViewById(R.id.btnReject);
        }
    }
}
