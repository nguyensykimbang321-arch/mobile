package com.appad.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.appad.R;
import com.appad.models.Album;
import com.bumptech.glide.Glide;
import java.util.List;

public class AlbumCardAdapter extends RecyclerView.Adapter<AlbumCardAdapter.ViewHolder> {

    private Context context;
    private List<Album> items;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(Album album);
    }

    public AlbumCardAdapter(Context context, List<Album> items, OnItemClickListener listener) {
        this.context = context;
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Album item = items.get(position);
        holder.textTitle.setText(item.getTitle());
        holder.textSubtitle.setText(item.getArtistName() != null ? item.getArtistName() : "Artist");

        Glide.with(context)
                .load(com.appad.utils.ImageUrlUtils.fixUrl(item.getCoverUrl()))
                .placeholder(R.drawable.ic_launcher_background)
                .into(holder.imgCover);

        if (item.getIsPremium() != null && item.getIsPremium() == 1) {
            holder.accessBadge.setAccessType("premium");
        } else {
            holder.accessBadge.setVisibility(View.GONE);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(item);
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgCover;
        TextView textTitle, textSubtitle;
        com.appad.components.AccessBadgeView accessBadge;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgCover = itemView.findViewById(R.id.imgCardCover);
            textTitle = itemView.findViewById(R.id.textCardTitle);
            textSubtitle = itemView.findViewById(R.id.textCardSubtitle);
            accessBadge = itemView.findViewById(R.id.accessBadge);
        }
    }
}
