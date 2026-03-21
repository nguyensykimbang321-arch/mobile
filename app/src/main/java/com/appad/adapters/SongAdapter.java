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
import java.util.List;
import android.view.MotionEvent;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;


public class SongAdapter extends RecyclerView.Adapter<SongAdapter.SongViewHolder> {

    private Context context;
    private List<Song> songList;
    private com.appad.utils.MusicPlayerManager.OnPlayerStatusChangeListener playerStatusChangeListener;

    public SongAdapter(Context context, List<Song> songList) {
        this.context = context;
        this.songList = songList;
        setupPlayerListener();
    }

    private void setupPlayerListener() {
        playerStatusChangeListener = new com.appad.utils.MusicPlayerManager.OnPlayerStatusChangeListener() {
            @Override
            public void onSongChanged(com.appad.models.Song song) {
                notifyDataSetChanged();
            }

            @Override
            public void onPlaybackStatusChanged(boolean isPlaying) {
                notifyDataSetChanged();
            }

            @Override
            public void onProgressUpdated(int position, int duration) {
                // Không cần refresh list khi progress chạy để tránh tốn hiệu năng
            }
        };
        com.appad.utils.MusicPlayerManager.getInstance().addStatusChangeListener(playerStatusChangeListener);
    }

    @Override
    public void onDetachedFromRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);
        if (playerStatusChangeListener != null) {
            com.appad.utils.MusicPlayerManager.getInstance().removeStatusChangeListener(playerStatusChangeListener);
        }
    }

    @NonNull
    @Override
    public SongViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_song, parent, false);
        return new SongViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull SongViewHolder holder, int position) {
        Song song = songList.get(position);
        holder.textTitle.setText(song.getTitle());
        
        String artistGenre = "Bài hát • " + (song.getArtistName() != null ? song.getArtistName() : "Unknown");
        holder.textArtistGenre.setText(artistGenre);
        
        holder.textListens.setText(com.appad.utils.FormatUtils.formatCount(song.getListenCount()));
        holder.textStars.setText(String.format("%.1f", song.getStars() != null ? song.getStars() : 0.0));
        holder.textDuration.setText(formatTime(song.getDuration()));
        
        // Premium and Access Badge Logic
        holder.accessBadge.setVisibility(View.GONE);
        holder.badgePremium.setVisibility(View.GONE);
        
        com.appad.models.User user = com.appad.utils.SessionManager.getInstance(context).getUser();
        boolean userIsPremium = user != null && Integer.valueOf(1).equals(user.getIsPremium());
        
        if (song != null) {
            boolean isSongPremium = Integer.valueOf(1).equals(song.getIsPremium());
            boolean isAlbumPremium = Integer.valueOf(1).equals(song.getIsAlbumPremium());
            
            // Show Premium label if the song or its album requires it
            if (isSongPremium) {
                holder.badgePremium.setVisibility(View.VISIBLE);
                holder.badgePremium.setText("PREMIUM");
            } else if (isAlbumPremium) {
                holder.badgePremium.setVisibility(View.VISIBLE);
                holder.badgePremium.setText("ALBUM PRE");
            }

            // CHECK ACCESS BADGE PRIORITY (Only for Premium items)
            if (isSongPremium || isAlbumPremium) {
                String accessType = null;

                // 1. Priority: Individual Purchase (Mua lẻ)
                if (Boolean.TRUE.equals(song.getBought())) {
                    accessType = "purchased";
                } 
                // 2. Priority: Album Purchase (Mua nguyên album)
                else if (Boolean.TRUE.equals(song.getAlbumBought())) {
                    accessType = "album_purchased";
                }
                // 3. Priority: Artist Owner (Chủ sở hữu - Nghệ sĩ)
                else if (Boolean.TRUE.equals(song.getIsArtistOwner())) {
                    accessType = "artist_owner";
                }
                // 4. Priority: Artist Membership (Hội viên Artist)
                else if (Boolean.TRUE.equals(song.getArtistMember())) {
                    accessType = "artist_membership";
                }
                // 5. Priority: Global Premium (User Premium)
                else if (userIsPremium) {
                    accessType = "premium";
                }

                if (accessType != null) {
                    holder.accessBadge.setVisibility(View.VISIBLE);
                    holder.accessBadge.setAccessType(accessType);
                }
            }
        }

        // Highlight active song
        Song currentSong = com.appad.utils.MusicPlayerManager.getInstance().getCurrentSong();
        boolean isCurrent = currentSong != null && currentSong.getSongId().equals(song.getSongId());
        boolean isPlaying = com.appad.utils.MusicPlayerManager.getInstance().isPlaying();

        if (isCurrent) {
            holder.llSongBody.setBackgroundResource(R.drawable.bg_song_card_active);
            holder.btnPlayFast.setImageResource(isPlaying ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play);
            holder.btnPlayFast.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.WHITE));
            holder.btnPlayFast.setImageTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#8B5CF6")));
        } else {
            holder.llSongBody.setBackgroundResource(R.drawable.bg_song_card);
            holder.btnPlayFast.setImageResource(android.R.drawable.ic_media_play);
            holder.btnPlayFast.setBackgroundTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#2D2D2D")));
            holder.btnPlayFast.setImageTintList(android.content.res.ColorStateList.valueOf(android.graphics.Color.parseColor("#8B5CF6")));
        }

        // Load image using Glide
        String coverUrl = song.getCoverUrl();
        if (coverUrl != null && !coverUrl.isEmpty()) {
            com.bumptech.glide.Glide.with(context)
                .load(com.appad.utils.ImageUrlUtils.fixUrl(coverUrl))
                .placeholder(R.drawable.ic_launcher_background)
                .into(holder.imgCover);
        }

        holder.btnMore.setOnClickListener(v -> showPlaylistDialog(song));

        holder.textArtistGenre.setOnClickListener(v -> {
            android.content.Intent intent = new android.content.Intent(context, com.appad.activities.ArtistDetailActivity.class);
            intent.putExtra("artistId", song.getArtistId());
            context.startActivity(intent);
        });

        holder.btnPlayFast.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos == RecyclerView.NO_POSITION) return;
            Song currentSongAtPos = songList.get(pos);
            
            if (isCurrent) {
                com.appad.utils.MusicPlayerManager.getInstance().togglePlayPause();
            } else {
                com.appad.utils.AccessHelper.checkAccess(context, currentSongAtPos, true, () -> {
                    com.appad.utils.MusicPlayerManager.getInstance().setPlaylist(songList, pos);
                    notifyDataSetChanged();
                });
            }
        });

        holder.llSongBody.setOnClickListener(v -> {
            int pos = holder.getAdapterPosition();
            if (pos == RecyclerView.NO_POSITION) return;
            Song currentSongAtPos = songList.get(pos);

            com.appad.utils.AccessHelper.checkAccess(context, currentSongAtPos, true, () -> {
                android.content.Intent intent = new android.content.Intent(context, com.appad.activities.FullPlayerActivity.class);
                intent.putExtra("title", currentSongAtPos.getTitle());
                intent.putExtra("artist", currentSongAtPos.getArtistName());
                intent.putExtra("cover", currentSongAtPos.getCoverUrl());

                if (!isCurrent) {
                    intent.putExtra("playlist_json", new com.google.gson.Gson().toJson(songList));
                    intent.putExtra("start_index", pos);
                    intent.putExtra("auto_play", true); // Auto play when clicking card body
                }
                
                context.startActivity(intent);
            });
        });

        // Hover animation (lift and float)
        holder.llSongBody.setOnHoverListener((v, event) -> {
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


    private String formatTime(Integer seconds) {
        if (seconds == null) return "0:00";
        int m = seconds / 60;
        int s = seconds % 60;
        return String.format("%d:%02d", m, s);
    }

    @Override
    public int getItemCount() {
        return songList.size();
    }

    public void onItemMove(int fromPosition, int toPosition) {
        if (fromPosition < toPosition) {
            for (int i = fromPosition; i < toPosition; i++) {
                java.util.Collections.swap(songList, i, i + 1);
            }
        } else {
            for (int i = fromPosition; i > toPosition; i--) {
                java.util.Collections.swap(songList, i, i - 1);
            }
        }
        notifyItemMoved(fromPosition, toPosition);
    }

    private void toggleFavorite(Song song) {
        Integer userId = com.appad.utils.SessionManager.getInstance(context).getUserId();
        if (userId == null) return;
        java.util.Map<String, Integer> payload = new java.util.HashMap<>();
        payload.put("userId", userId);
        payload.put("songId", song.getSongId());
        com.appad.utils.RetrofitClient.getApiService().toggleFavorite(payload).enqueue(new retrofit2.Callback<java.util.Map<String, Object>>() {
            @Override
            public void onResponse(retrofit2.Call<java.util.Map<String, Object>> call, retrofit2.Response<java.util.Map<String, Object>> response) {
                if (response.isSuccessful()) {
                    android.widget.Toast.makeText(context, "Đã cập nhật yêu thích", android.widget.Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(retrofit2.Call<java.util.Map<String, Object>> call, Throwable t) {}
        });
    }

    private void showPlaylistDialog(Song song) {
        if (context instanceof androidx.fragment.app.FragmentActivity) {
            com.appad.components.AddToPlaylistFragment fragment = com.appad.components.AddToPlaylistFragment.newInstance(song);
            fragment.show(((androidx.fragment.app.FragmentActivity) context).getSupportFragmentManager(), "add_to_playlist");
        }
    }

    private void addSongToPlaylist(Integer playlistId, Integer songId) {
        java.util.Map<String, Long> payload = new java.util.HashMap<>();
        payload.put("song_id", songId.longValue());
        com.appad.utils.RetrofitClient.getApiService().addSongToPlaylist(playlistId, payload).enqueue(new retrofit2.Callback<java.util.Map<String, Object>>() {
            @Override
            public void onResponse(retrofit2.Call<java.util.Map<String, Object>> call, retrofit2.Response<java.util.Map<String, Object>> response) {
                if (response.isSuccessful()) {
                    android.widget.Toast.makeText(context, "Đã thêm vào danh sách phát", android.widget.Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(retrofit2.Call<java.util.Map<String, Object>> call, Throwable t) {}
        });
    }
    public static class SongViewHolder extends RecyclerView.ViewHolder {
        ImageView imgCover;
        com.appad.components.PremiumBadgeView badgePremium;
        com.appad.components.AccessBadgeView accessBadge;
        TextView textTitle, textArtistGenre, textListens, textStars, textDuration;
        android.widget.ImageButton btnMore;
        android.widget.ImageButton btnPlayFast;
        android.view.View llSongBody;

        public SongViewHolder(@NonNull View itemView) {
            super(itemView);
            imgCover = itemView.findViewById(R.id.imgSongCover);
            badgePremium = itemView.findViewById(R.id.badgePremium);
            textTitle = itemView.findViewById(R.id.textSongTitle);
            textArtistGenre = itemView.findViewById(R.id.textArtistGenre);
            textListens = itemView.findViewById(R.id.textListens);
            textStars = itemView.findViewById(R.id.textStars);
            textDuration = itemView.findViewById(R.id.textDuration);
            accessBadge = itemView.findViewById(R.id.accessBadge);
            btnMore = itemView.findViewById(R.id.btnMore);
            btnPlayFast = itemView.findViewById(R.id.btnPlayFast);
            llSongBody = itemView.findViewById(R.id.llSongBody);
        }
    }
}
