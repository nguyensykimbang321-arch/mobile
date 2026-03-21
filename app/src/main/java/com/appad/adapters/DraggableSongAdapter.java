package com.appad.adapters;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;
import com.appad.R;
import com.appad.models.Song;
import java.util.Collections;
import java.util.List;

public class DraggableSongAdapter extends RecyclerView.Adapter<DraggableSongAdapter.DraggableViewHolder> {

    private Context context;
    private int playlistId;
    private List<Song> songList;
    private ItemTouchHelper itemTouchHelper;
    private OnOrderChangeListener onOrderChangeListener;
    private com.appad.utils.MusicPlayerManager.OnPlayerStatusChangeListener playerStatusChangeListener;

    public interface OnOrderChangeListener {
        void onOrderChanged(List<Song> newOrder);
    }

    public DraggableSongAdapter(Context context, int playlistId, List<Song> songList) {
        this.context = context;
        this.playlistId = playlistId;
        this.songList = songList;
        setupPlayerListener();
    }

    public void setOnOrderChangeListener(OnOrderChangeListener listener) {
        this.onOrderChangeListener = listener;
    }

    public void setItemTouchHelper(ItemTouchHelper helper) {
        this.itemTouchHelper = helper;
    }

    private void setupPlayerListener() {
        playerStatusChangeListener = new com.appad.utils.MusicPlayerManager.OnPlayerStatusChangeListener() {
            @Override
            public void onSongChanged(Song song) {
                notifyDataSetChanged();
            }

            @Override
            public void onPlaybackStatusChanged(boolean isPlaying) {
                notifyDataSetChanged();
            }

            @Override
            public void onProgressUpdated(int position, int duration) {
                // Không cần refresh list khi progress chạy
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
    public DraggableViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Sử dụng layout item_song_playlist để có nút xóa thay vì nút thêm
        View view = LayoutInflater.from(context).inflate(R.layout.item_song_playlist, parent, false);
        return new DraggableViewHolder(view);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onBindViewHolder(@NonNull DraggableViewHolder holder, int position) {
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
            
            if (isSongPremium) {
                holder.badgePremium.setVisibility(View.VISIBLE);
                holder.badgePremium.setText("PREMIUM");
            } else if (isAlbumPremium) {
                holder.badgePremium.setVisibility(View.VISIBLE);
                holder.badgePremium.setText("ALBUM PRE");
            }

            if (isSongPremium || isAlbumPremium) {
                String accessType = null;
                if (Boolean.TRUE.equals(song.getBought())) {
                    accessType = "purchased";
                } else if (Boolean.TRUE.equals(song.getAlbumBought())) {
                    accessType = "album_purchased";
                } else if (Boolean.TRUE.equals(song.getIsArtistOwner())) {
                    accessType = "artist_owner";
                } else if (Boolean.TRUE.equals(song.getArtistMember())) {
                    accessType = "artist_membership";
                } else if (userIsPremium) {
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

        // Load image
        String coverUrl = song.getCoverUrl();
        if (coverUrl != null && !coverUrl.isEmpty()) {
            com.bumptech.glide.Glide.with(context)
                .load(com.appad.utils.ImageUrlUtils.fixUrl(coverUrl))
                .placeholder(R.drawable.ic_launcher_background)
                .into(holder.imgCover);
        }

        // Long press trên toàn bộ thẻ để kéo (không cần drag handle icon)
        // ItemTouchHelper sẽ tự động xử lý long press

        holder.btnRemove.setOnClickListener(v -> showRemoveConfirmation(song, position));

        holder.textArtistGenre.setOnClickListener(v -> {
            android.content.Intent intent = new android.content.Intent(context, com.appad.activities.ArtistDetailActivity.class);
            intent.putExtra("artistId", song.getArtistId());
            context.startActivity(intent);
        });

        holder.btnPlayFast.setOnClickListener(v -> {
            if (isCurrent) {
                com.appad.utils.MusicPlayerManager.getInstance().togglePlayPause();
            } else {
                com.appad.utils.AccessHelper.checkAccess(context, song, true, () -> {
                    com.appad.utils.MusicPlayerManager.getInstance().setPlaylist(songList, position);
                    notifyDataSetChanged();
                });
            }
        });

        holder.llSongBody.setOnClickListener(v -> {
            com.appad.utils.AccessHelper.checkAccess(context, song, true, () -> {
                android.content.Intent intent = new android.content.Intent(context, com.appad.activities.FullPlayerActivity.class);
                intent.putExtra("title", song.getTitle());
                intent.putExtra("artist", song.getArtistName());
                intent.putExtra("cover", song.getCoverUrl());

                if (!isCurrent) {
                    intent.putExtra("playlist_json", new com.google.gson.Gson().toJson(songList));
                    intent.putExtra("start_index", position);
                    intent.putExtra("auto_play", true);
                }
                
                context.startActivity(intent);
            });
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

    // Di chuyển item trong danh sách
    public void onItemMove(int fromPosition, int toPosition) {
        if (fromPosition < toPosition) {
            for (int i = fromPosition; i < toPosition; i++) {
                Collections.swap(songList, i, i + 1);
            }
        } else {
            for (int i = fromPosition; i > toPosition; i--) {
                Collections.swap(songList, i, i - 1);
            }
        }
        notifyItemMoved(fromPosition, toPosition);
    }

    // Gọi khi kéo thả hoàn tất
    public void onItemMoveFinished() {
        if (onOrderChangeListener != null) {
            onOrderChangeListener.onOrderChanged(songList);
        }
    }

    private void showRemoveConfirmation(Song song, int position) {
        new com.google.android.material.dialog.MaterialAlertDialogBuilder(context)
                .setTitle("Xóa khỏi playlist")
                .setMessage("Bạn có muốn xóa bài hát \"" + song.getTitle() + "\" khỏi playlist này không?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    removeSongFromPlaylist(song, position);
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void removeSongFromPlaylist(Song song, int position) {
        com.appad.utils.RetrofitClient.getApiService().removeSongFromPlaylist(playlistId, song.getSongId())
                .enqueue(new retrofit2.Callback<java.util.Map<String, Object>>() {
                    @Override
                    public void onResponse(retrofit2.Call<java.util.Map<String, Object>> call, retrofit2.Response<java.util.Map<String, Object>> response) {
                        if (response.isSuccessful()) {
                            songList.remove(position);
                            notifyItemRemoved(position);
                            notifyItemRangeChanged(position, songList.size());
                            if (onOrderChangeListener != null) {
                                onOrderChangeListener.onOrderChanged(songList);
                            }
                            android.widget.Toast.makeText(context, "Đã xóa bài hát khỏi playlist", android.widget.Toast.LENGTH_SHORT).show();
                        } else {
                            android.widget.Toast.makeText(context, "Lỗi khi xóa bài hát: " + response.code(), android.widget.Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(retrofit2.Call<java.util.Map<String, Object>> call, Throwable t) {
                        android.widget.Toast.makeText(context, "Lỗi kết nối: " + t.getMessage(), android.widget.Toast.LENGTH_SHORT).show();
                    }
                });
    }

    public static class DraggableViewHolder extends RecyclerView.ViewHolder {
        ImageView imgCover;
        com.appad.components.PremiumBadgeView badgePremium;
        com.appad.components.AccessBadgeView accessBadge;
        TextView textTitle, textArtistGenre, textListens, textStars, textDuration;
        ImageButton btnRemove, btnPlayFast;
        View llSongBody;

        public DraggableViewHolder(@NonNull View itemView) {
            super(itemView);
            imgCover = itemView.findViewById(R.id.imgSongCover);
            badgePremium = itemView.findViewById(R.id.badgePremium);
            textTitle = itemView.findViewById(R.id.textSongTitle);
            textArtistGenre = itemView.findViewById(R.id.textArtistGenre);
            textListens = itemView.findViewById(R.id.textListens);
            textStars = itemView.findViewById(R.id.textStars);
            textDuration = itemView.findViewById(R.id.textDuration);
            accessBadge = itemView.findViewById(R.id.accessBadge);
            btnRemove = itemView.findViewById(R.id.btnRemove);
            btnPlayFast = itemView.findViewById(R.id.btnPlayFast);
            llSongBody = itemView.findViewById(R.id.llSongBody);
        }
    }
}
