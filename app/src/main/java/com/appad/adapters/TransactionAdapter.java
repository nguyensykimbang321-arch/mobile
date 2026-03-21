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
import com.appad.models.Transaction;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;

public class TransactionAdapter extends RecyclerView.Adapter<TransactionAdapter.ViewHolder> {

    private List<Transaction> transactions;
    private Context context;

    public TransactionAdapter(Context context, List<Transaction> transactions) {
        this.context = context;
        this.transactions = transactions;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_transaction, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Transaction t = transactions.get(position);
        NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        
        String type = t.getType() != null ? t.getType() : "unknown";
        String status = t.getStatus() != null ? t.getStatus() : "unknown";
        
        boolean isPlus = type.equals("deposit") || type.equals("revenue") || type.equals("refund");
        String prefix = isPlus ? "+" : "-";
        int color = isPlus ? Color.parseColor("#10B981") : Color.parseColor("#EF4444");
        
        holder.txtAmount.setText(prefix + formatter.format(t.getAmount() != null ? t.getAmount() : 0.0));
        holder.txtAmount.setTextColor(color);
        holder.viewIndicator.setBackgroundColor(color);

        String typeDesc = type.toUpperCase().replace("_", " ");
        if (type.equals("deposit")) typeDesc = "Nạp tiền";
        else if (type.equals("purchase_song")) typeDesc = "Mua bài hát";
        else if (type.equals("purchase_album")) typeDesc = "Mua album";
        else if (type.equals("premium")) typeDesc = "Đăng ký Premium";
        else if (type.equals("withdrawal")) typeDesc = "Rút tiền";
        
        holder.txtType.setText(typeDesc);
        
        String dateDesc = t.getCreatedAt() != null ? t.getCreatedAt().replace("T", " ") : "";
        if (dateDesc.length() > 16) dateDesc = dateDesc.substring(0, 16);
        holder.txtDate.setText(dateDesc);
        
        String statusText = status.toUpperCase();
        if (status.equals("success") || status.equals("completed") || status.equals("approved")) statusText = "Hoàn tất";
        else if (status.equals("pending")) statusText = "Đang xử lý";
        else if (status.equals("failed") || status.equals("rejected")) statusText = "Thất bại";
        
        holder.txtStatus.setText(statusText);
        holder.txtStatus.setTextColor(statusText.equals("Hoàn tất") ? Color.parseColor("#10B981") : 
                                     statusText.equals("Đang xử lý") ? Color.parseColor("#F59E0B") : Color.parseColor("#EF4444"));
    }

    @Override
    public int getItemCount() {
        return transactions.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtAmount, txtType, txtDate, txtStatus;
        View viewIndicator;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtAmount = itemView.findViewById(R.id.txtTransactionAmount);
            txtType = itemView.findViewById(R.id.txtTransactionType);
            txtDate = itemView.findViewById(R.id.txtTransactionDate);
            txtStatus = itemView.findViewById(R.id.txtTransactionStatus);
            viewIndicator = itemView.findViewById(R.id.viewTypeIndicator);
        }
    }
}
