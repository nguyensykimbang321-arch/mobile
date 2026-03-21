package com.appad.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.appad.R;
import com.appad.utils.RetrofitClient;
import com.appad.utils.SessionManager;
import com.bumptech.glide.Glide;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ManageSongsActivity extends AppCompatActivity {

    private RecyclerView rvSongs;
    private ManageSongAdapter adapter;
    private List<Map<String, Object>> songs = new ArrayList<>();
    private com.appad.utils.MiniPlayerHelper miniPlayerHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_songs);

        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbarManageSongs);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish());

        rvSongs = findViewById(R.id.rvManageSongs);
        rvSongs.setLayoutManager(new LinearLayoutManager(this));
        
        com.google.android.material.floatingactionbutton.FloatingActionButton fab = findViewById(R.id.fabAddSong);
        fab.setOnClickListener(v -> {
            Intent intent = new Intent(ManageSongsActivity.this, EditSongActivity.class);
            // No SONG_ID extra means create mode
            startActivity(intent);
        });
        
        loadMySongs();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadMySongs(); // Refresh list on return
        if (miniPlayerHelper == null) {
            miniPlayerHelper = new com.appad.utils.MiniPlayerHelper(this);
        }
        miniPlayerHelper.setupMiniPlayer();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (miniPlayerHelper != null) {
            miniPlayerHelper.detach();
        }
    }

    private void loadMySongs() {
        // Try getting artistId from intent first
        Integer intentArtistId = getIntent().hasExtra("ARTIST_ID") ? getIntent().getIntExtra("ARTIST_ID", -1) : -1;
        if (intentArtistId != -1) {
            fetchSongsByArtistId(intentArtistId);
            return;
        }

        // Fallback: Fetch artist info by userId
        Integer userId = SessionManager.getInstance(this).getUserId();
        if (userId == null) return;

        RetrofitClient.getApiService().getArtistByUserId(userId).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Object> data = (Map<String, Object>) response.body().get("data");
                    if (data != null && data.get("artist_id") != null) {
                        fetchSongsByArtistId(((Number) data.get("artist_id")).intValue());
                    }
                }
            }
            @Override public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Toast.makeText(ManageSongsActivity.this, "Lỗi xác thực nghệ sĩ", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchSongsByArtistId(Integer artistId) {
        RetrofitClient.getApiService().getArtistSongs(artistId).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Object data = response.body().get("data");
                    if (data instanceof List) {
                        songs.clear();
                        songs.addAll((List<Map<String, Object>>) data);
                        adapter = new ManageSongAdapter(songs);
                        rvSongs.setAdapter(adapter);
                    }
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Toast.makeText(ManageSongsActivity.this, "Lỗi tải bài hát", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void confirmDelete(Integer songId, int position) {
        new android.app.AlertDialog.Builder(this)
            .setTitle("Xóa bài hát?")
            .setMessage("Hành động này không thể hoàn tác.")
            .setPositiveButton("Xóa", (dialog, which) -> {
                RetrofitClient.getApiService().deleteSong(songId).enqueue(new Callback<Map<String, Object>>() {
                    @Override
                    public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                        if (response.isSuccessful()) {
                            songs.remove(position);
                            adapter.notifyItemRemoved(position);
                            Toast.makeText(ManageSongsActivity.this, "Đã xóa bài hát thành công", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(ManageSongsActivity.this, "Không thể xóa bài hát", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                        Toast.makeText(ManageSongsActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
                    }
                });
            })
            .setNegativeButton("Hủy", null)
            .show();
    }

    private class ManageSongAdapter extends RecyclerView.Adapter<ManageSongAdapter.SongViewHolder> {
        private List<Map<String, Object>> songs;

        ManageSongAdapter(List<Map<String, Object>> songs) {
            this.songs = songs;
        }

        @NonNull
        @Override
        public SongViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_manage_song, parent, false);
            return new SongViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull SongViewHolder holder, int position) {
            Map<String, Object> song = songs.get(position);
            holder.txtTitle.setText((String) song.get("title"));
            
            // Album Name
            String albumTitle = "Single";
            if (song.get("album_title") != null) albumTitle = (String) song.get("album_title");
            else if (song.get("albumTitle") != null) albumTitle = (String) song.get("albumTitle");
            holder.txtAlbum.setText(albumTitle);

            // Cover
            String cover = (String) (song.get("cover_url") != null ? song.get("cover_url") : song.get("coverUrl"));
            Glide.with(ManageSongsActivity.this).load(com.appad.utils.ImageUrlUtils.fixUrl(cover)).into(holder.imgCover);
            
            // Listen count
            Object listens = song.get("listen_count") != null ? song.get("listen_count") : song.get("listenCount");
            holder.txtListenCount.setText(listens != null ? String.valueOf(listens) : "0");
            
            // Duration
            Object dur = song.get("duration");
            if(dur instanceof Number) {
                int seconds = ((Number) dur).intValue();
                int min = seconds / 60;
                int sec = seconds % 60;
                holder.txtDuration.setText(String.format("%d:%02d", min, sec));
            } else {
                holder.txtDuration.setText("0:00");
            }

            // Premium / Price
            boolean isPrem = false;
            Object p = song.get("is_premium") != null ? song.get("is_premium") : song.get("isPremium");
            if (p != null && (p instanceof  Number && ((Number)p).intValue() == 1) || (p instanceof Boolean && (Boolean)p)) {
                isPrem = true;
            }
            if(isPrem) {
                holder.iconPremium.setVisibility(View.VISIBLE);
                holder.txtPrice.setVisibility(View.VISIBLE);
                Object price = song.get("price");
                double priceVal = price instanceof Number ? ((Number)price).doubleValue() : 0.0;
                holder.txtPrice.setText(String.format("%,.0fđ", priceVal));
            } else {
                 holder.iconPremium.setVisibility(View.GONE);
                 holder.txtPrice.setVisibility(View.GONE);
            }
            
            // Status: 0=Hidden, 1=Active
            Object st = song.get("status");
            boolean isHidden = (st != null && ((Number)st).intValue() == 0);
            
            if(isHidden) {
                holder.layoutHiddenOverlay.setVisibility(View.VISIBLE);
                holder.txtHiddenBadge.setVisibility(View.VISIBLE);
                // Dark red tint background
                holder.layoutBackground.setBackgroundColor(android.graphics.Color.parseColor("#2D1F1F"));
            } else {
                holder.layoutHiddenOverlay.setVisibility(View.GONE);
                holder.txtHiddenBadge.setVisibility(View.GONE);
                 holder.layoutBackground.setBackgroundColor(android.graphics.Color.parseColor("#161616"));
            }

            // Edit
            holder.btnEdit.setOnClickListener(v -> {
                Intent intent = new Intent(ManageSongsActivity.this, EditSongActivity.class);
                Object sid = song.get("song_id") != null ? song.get("song_id") : song.get("songId");
                intent.putExtra("SONG_ID", ((Number)sid).intValue());
                startActivity(intent);
            });

            // Delete
            holder.btnDelete.setOnClickListener(v -> {
                Object sid = song.get("song_id") != null ? song.get("song_id") : song.get("songId");
                confirmDelete(((Number)sid).intValue(), position);
            });
        }

        @Override
        public int getItemCount() {
            return songs.size();
        }

        class SongViewHolder extends RecyclerView.ViewHolder {
            TextView txtTitle, txtAlbum, txtListenCount, txtDuration, txtPrice, txtHiddenBadge;
            ImageView imgCover, iconPremium;
            View layoutHiddenOverlay, layoutBackground;
            ImageButton btnEdit, btnDelete;

            SongViewHolder(View itemView) {
                super(itemView);
                txtTitle = itemView.findViewById(R.id.txtSongTitle);
                txtAlbum = itemView.findViewById(R.id.txtSongAlbum);
                txtListenCount = itemView.findViewById(R.id.txtListenCount);
                txtDuration = itemView.findViewById(R.id.txtDuration);
                txtPrice = itemView.findViewById(R.id.txtPrice);
                txtHiddenBadge = itemView.findViewById(R.id.txtHiddenBadge);
                imgCover = itemView.findViewById(R.id.imgSongCover);
                iconPremium = itemView.findViewById(R.id.iconPremium);
                layoutHiddenOverlay = itemView.findViewById(R.id.layoutHiddenOverlay);
                layoutBackground = itemView.findViewById(R.id.layoutSongBackground);
                btnEdit = itemView.findViewById(R.id.btnEditSong);
                btnDelete = itemView.findViewById(R.id.btnDeleteSong);
            }
        }
    }
}
