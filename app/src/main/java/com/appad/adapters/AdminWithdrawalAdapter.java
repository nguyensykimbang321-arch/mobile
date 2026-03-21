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

public class AdminWithdrawalAdapter extends RecyclerView.Adapter<AdminWithdrawalAdapter.ViewHolder> {

    private Context context;
    private List<Map<String, Object>> withdrawals;
    private OnWithdrawalActionEventListener listener;

    public interface OnWithdrawalActionEventListener {
        void onApprove(Map<String, Object> withdrawal);
        void onReject(Map<String, Object> withdrawal);
    }

    public AdminWithdrawalAdapter(Context context, List<Map<String, Object>> withdrawals, OnWithdrawalActionEventListener listener) {
        this.context = context;
        this.withdrawals = withdrawals;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_admin_withdrawal, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Map<String, Object> item = withdrawals.get(position);
        
        holder.txtArtist.setText((String) item.get("artist_name"));
        holder.txtBank.setText(item.get("bank_name") != null ? (String) item.get("bank_name") : "Ngân hàng chưa cập nhật");
        
        String status = (String) item.get("status");
        boolean isPending = "pending".equals(status);
        boolean isCompleted = "completed".equals(status);
        
        holder.txtStatus.setText(isPending ? "Chờ duyệt" : isCompleted ? "Hoàn thành" : "Từ chối");
        holder.txtStatus.setTextColor(isPending ? Color.parseColor("#F59E0B") : isCompleted ? Color.parseColor("#10B981") : Color.parseColor("#EF4444"));
        holder.txtStatus.getBackground().setTint(isPending ? Color.parseColor("#20F59E0B") : isCompleted ? Color.parseColor("#2010B981") : Color.parseColor("#20EF4444"));

        double amount = 0;
        if (item.get("amount") != null) amount = ((Number) item.get("amount")).doubleValue();
        NumberFormat nf = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        holder.txtAmount.setText(nf.format(amount));
        
        holder.txtBankAccName.setText((String) item.get("bank_account_name"));
        holder.txtBankAccNumber.setText((String) item.get("bank_account"));
        
        String requestedAt = (String) item.get("requested_at");
        holder.txtDate.setText("📅 " + (requestedAt != null ? requestedAt.replace("T", " ").substring(0, 16) : "Unknown"));

        holder.layoutActions.setVisibility(isPending ? View.VISIBLE : View.GONE);
        holder.btnApprove.setOnClickListener(v -> listener.onApprove(item));
        holder.btnReject.setOnClickListener(v -> listener.onReject(item));
    }

    @Override
    public int getItemCount() {
        return withdrawals.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtArtist, txtBank, txtStatus, txtAmount, txtBankAccName, txtBankAccNumber, txtDate;
        View layoutActions, btnApprove, btnReject;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtArtist = itemView.findViewById(R.id.txtArtistName);
            txtBank = itemView.findViewById(R.id.txtBankName);
            txtStatus = itemView.findViewById(R.id.txtWithdrawalStatus);
            txtAmount = itemView.findViewById(R.id.txtWithdrawalAmount);
            txtBankAccName = itemView.findViewById(R.id.txtBankAccountName);
            txtBankAccNumber = itemView.findViewById(R.id.txtBankAccountNumber);
            txtDate = itemView.findViewById(R.id.txtRequestedDate);
            layoutActions = itemView.findViewById(R.id.layoutActions);
            btnApprove = itemView.findViewById(R.id.btnApprove);
            btnReject = itemView.findViewById(R.id.btnReject);
        }
    }
}
