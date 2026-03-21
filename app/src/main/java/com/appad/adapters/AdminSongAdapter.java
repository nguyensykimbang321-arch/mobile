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
import com.appad.models.Song;
import com.appad.utils.ImageUrlUtils;
import com.bumptech.glide.Glide;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import android.view.MotionEvent;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;


public class AdminSongAdapter extends RecyclerView.Adapter<AdminSongAdapter.ViewHolder> {

    private Context context;
    private List<Song> songs;
    private OnSongActionEventListener listener;

    public interface OnSongActionEventListener {
        void onEditSong(Song song);
        void onDeleteSong(Song song);
    }

    public AdminSongAdapter(Context context, List<Song> songs, OnSongActionEventListener listener) {
        this.context = context;
        this.songs = songs;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_admin_song, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Song song = songs.get(position);
        
        holder.txtTitle.setText(song.getTitle());
        holder.txtArtist.setText("🎤 " + (song.getArtistName() != null ? song.getArtistName() : "Unknown"));
        
        holder.txtPlays.setText(com.appad.utils.FormatUtils.formatCount(song.getListenCount()));
        holder.txtGenre.setText(song.getGenreName() != null ? song.getGenreName() : "Khác");
        
        Glide.with(context)
                .load(ImageUrlUtils.fixUrl(song.getCoverUrl()))
                .placeholder(R.drawable.placeholder_song)
                .into(holder.imgCover);

        holder.btnEdit.setOnClickListener(v -> {
            if (listener != null) listener.onEditSong(song);
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDeleteSong(song);
        });

        // Hover animation (lift and float)
        holder.itemView.setOnHoverListener((v, event) -> {
            switch (event.getAction()) {
                case MotionEvent.ACTION_HOVER_ENTER:
                    Animation floatAnim = AnimationUtils.loadAnimation(context, R.anim.hover_float);
                    v.startAnimation(floatAnim);
                    v.animate().scaleX(1.02f).scaleY(1.02f).setDuration(200).start();
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
        return songs.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgCover;
        TextView txtTitle, txtArtist, txtPlays, txtGenre;
        View btnEdit, btnDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgCover = itemView.findViewById(R.id.imgSongCover);
            txtTitle = itemView.findViewById(R.id.txtSongTitle);
            txtArtist = itemView.findViewById(R.id.txtSongArtist);
            txtPlays = itemView.findViewById(R.id.txtSongPlays);
            txtGenre = itemView.findViewById(R.id.txtSongGenre);
            btnEdit = itemView.findViewById(R.id.btnEditSong);
            btnDelete = itemView.findViewById(R.id.btnDeleteSong);
        }
    }
}
