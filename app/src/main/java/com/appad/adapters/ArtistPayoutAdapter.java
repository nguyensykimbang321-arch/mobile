package com.appad.adapters;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.appad.R;
import com.bumptech.glide.Glide;

import java.text.DecimalFormat;
import java.util.List;
import java.util.Map;

public class ArtistPayoutAdapter extends RecyclerView.Adapter<ArtistPayoutAdapter.ViewHolder> {

    private Context context;
    private List<Map<String, Object>> shares;
    private DecimalFormat currencyFormat = new DecimalFormat("#,###đ");

    public ArtistPayoutAdapter(Context context, List<Map<String, Object>> shares) {
        this.context = context;
        this.shares = shares;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_artist_payout, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Map<String, Object> item = shares.get(position);

        String name = (String) item.get("artist_name");
        holder.txtArtistName.setText(name != null ? name : "Nghệ sĩ ẩn danh");

        Number duration = (Number) item.get("duration");
        long sec = duration != null ? duration.longValue() : 0;
        holder.txtDuration.setText((sec / 60) + "p " + (sec % 60) + "s");

        Number revenue = (Number) item.get("revenue");
        holder.txtRevenue.setText(currencyFormat.format(revenue != null ? revenue.doubleValue() : 0.0));

        Number percentage = (Number) item.get("percentage");
        holder.txtPercentage.setText((percentage != null ? percentage.doubleValue() : 0.0) + "%");

        String imageUrl = (String) item.get("image_url");
        if (imageUrl != null && !imageUrl.isEmpty()) {
            holder.imgArtist.setVisibility(View.VISIBLE);
            holder.txtAvatarLetter.setVisibility(View.GONE);
            Glide.with(context).load(imageUrl).into(holder.imgArtist);
        } else {
            holder.imgArtist.setVisibility(View.GONE);
            holder.txtAvatarLetter.setVisibility(View.VISIBLE);
            holder.txtAvatarLetter.setText(name != null && !name.isEmpty() ? name.substring(0, 1).toUpperCase() : "?");
        }
    }

    @Override
    public int getItemCount() {
        return shares.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgArtist;
        TextView txtAvatarLetter, txtArtistName, txtDuration, txtRevenue, txtPercentage;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgArtist = itemView.findViewById(R.id.imgArtist);
            txtAvatarLetter = itemView.findViewById(R.id.txtAvatarLetter);
            txtArtistName = itemView.findViewById(R.id.txtArtistName);
            txtDuration = itemView.findViewById(R.id.txtDuration);
            txtRevenue = itemView.findViewById(R.id.txtRevenue);
            txtPercentage = itemView.findViewById(R.id.txtPercentage);
        }
    }
}
