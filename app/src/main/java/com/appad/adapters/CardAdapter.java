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
import com.appad.models.Song;
import com.bumptech.glide.Glide;
import java.util.List;
import android.view.MotionEvent;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;


public class CardAdapter extends RecyclerView.Adapter<CardAdapter.CardViewHolder> {

    private Context context;
    private List<Song> items;
    private OnItemClickListener listener;

    public interface OnItemClickListener {
        void onItemClick(Song song);
    }

    public CardAdapter(Context context, List<Song> items, OnItemClickListener listener) {
        this.context = context;
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CardViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_card, parent, false);
        return new CardViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CardViewHolder holder, int position) {
        Song item = items.get(position);
        holder.textTitle.setText(item.getTitle());
        holder.textSubtitle.setText(item.getArtistName() != null ? item.getArtistName() : "Artist");

        Glide.with(context)
                .load(com.appad.utils.ImageUrlUtils.fixUrl(item.getCoverUrl()))
                .placeholder(R.drawable.ic_launcher_background)
                .into(holder.imgCover);

        // Highlight playing song
        Song currentSong = com.appad.utils.MusicPlayerManager.getInstance().getCurrentSong();
        boolean isCurrent = currentSong != null && currentSong.getSongId().equals(item.getSongId());
        boolean isPlaying = com.appad.utils.MusicPlayerManager.getInstance().isPlaying();

        if (isCurrent && isPlaying) {
            holder.playingOverlay.setVisibility(View.VISIBLE);
            holder.imgPlayingIndicator.setVisibility(View.VISIBLE);
            holder.itemView.setStrokeColor(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#8B5CF6")));
            holder.itemView.setStrokeWidth(4);
        } else {
            holder.playingOverlay.setVisibility(View.GONE);
            holder.imgPlayingIndicator.setVisibility(View.GONE);
            holder.itemView.setStrokeColor(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#33FFFFFF")));
            holder.itemView.setStrokeWidth(1);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onItemClick(item);
        });

        // Hover animation (lift and float)
        holder.itemView.setOnHoverListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_HOVER_ENTER:
                    Animation floatAnim = AnimationUtils.loadAnimation(context, R.anim.hover_float);
                    v.startAnimation(floatAnim);
                    v.animate().scaleX(1.03f).scaleY(1.03f).setDuration(200).start();
                    break;
                case MotionEvent.ACTION_HOVER_EXIT:
                    v.clearAnimation();
                    v.animate().scaleX(1.0f).scaleY(1.0f).translationY(0).setDuration(200).start();
                    break;
            }
            return false;
        });
    }


    @Override
    public int getItemCount() {
        return items.size();
    }

    public static class CardViewHolder extends RecyclerView.ViewHolder {
        ImageView imgCover, imgPlayingIndicator;
        TextView textTitle, textSubtitle;
        View playingOverlay;
        com.google.android.material.card.MaterialCardView itemView;

        public CardViewHolder(@NonNull View itemView) {
            super(itemView);
            this.itemView = (com.google.android.material.card.MaterialCardView) itemView;
            imgCover = itemView.findViewById(R.id.imgCardCover);
            textTitle = itemView.findViewById(R.id.textCardTitle);
            textSubtitle = itemView.findViewById(R.id.textCardSubtitle);
            playingOverlay = itemView.findViewById(R.id.playingOverlay);
            imgPlayingIndicator = itemView.findViewById(R.id.imgPlayingIndicator);
        }
    }
}
