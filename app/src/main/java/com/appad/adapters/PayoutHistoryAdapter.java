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

import java.text.DecimalFormat;
import java.util.List;
import java.util.Map;

public class PayoutHistoryAdapter extends RecyclerView.Adapter<PayoutHistoryAdapter.ViewHolder> {

    private Context context;
    private List<Map<String, Object>> batches;
    private DecimalFormat currencyFormat = new DecimalFormat("#,###đ");
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(Map<String, Object> batch);
    }

    public PayoutHistoryAdapter(Context context, List<Map<String, Object>> batches, OnItemClickListener listener) {
        this.context = context;
        this.batches = batches;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_payout_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Map<String, Object> batch = batches.get(position);

        String batchTime = (String) batch.get("batch_time");
        if (batchTime != null) {
            // Simplify ISO format: 2026-01-16T21:00:00.000 -> 16/01/2026 21:00
            batchTime = batchTime.replace("T", " ");
            if (batchTime.contains(".")) {
                batchTime = batchTime.substring(0, batchTime.indexOf("."));
            }
        }
        holder.txtBatchTime.setText(batchTime != null ? batchTime : "Không rõ thời gian");

        Number count = (Number) batch.get("artist_count");
        holder.txtArtistCount.setText((count != null ? count.intValue() : 0) + " nghệ sĩ");

        Number total = (Number) batch.get("total_paid");
        holder.txtTotalPaid.setText(currencyFormat.format(total != null ? total.doubleValue() : 0.0));

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(batch);
        });
    }

    @Override
    public int getItemCount() {
        return batches.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtBatchTime, txtArtistCount, txtTotalPaid;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtBatchTime = itemView.findViewById(R.id.txtBatchTime);
            txtArtistCount = itemView.findViewById(R.id.txtArtistCount);
            txtTotalPaid = itemView.findViewById(R.id.txtTotalPaid);
        }
    }
}
