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

public class ManageAlbumsActivity extends AppCompatActivity {

    private RecyclerView rvAlbums;
    private ManageAlbumAdapter adapter;
    private List<Map<String, Object>> albums = new ArrayList<>();
    private com.appad.utils.MiniPlayerHelper miniPlayerHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_albums);

        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbarManageAlbums);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Quản lý Album");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish());

        rvAlbums = findViewById(R.id.rvManageAlbums);
        rvAlbums.setLayoutManager(new LinearLayoutManager(this));
        
        findViewById(R.id.fabAddAlbum).setOnClickListener(v -> {
            Intent intent = new Intent(ManageAlbumsActivity.this, EditAlbumActivity.class);
            startActivity(intent);
        });
        
        loadMyAlbums();
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        loadMyAlbums();
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

    private void loadMyAlbums() {
        // Try getting artistId from intent first
        Integer intentArtistId = getIntent().hasExtra("ARTIST_ID") ? getIntent().getIntExtra("ARTIST_ID", -1) : -1;
        if (intentArtistId != -1) {
            fetchAlbumsByArtistId(intentArtistId);
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
                        fetchAlbumsByArtistId(((Number) data.get("artist_id")).intValue());
                    }
                }
            }
            @Override public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Toast.makeText(ManageAlbumsActivity.this, "Lỗi xác thực nghệ sĩ", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void fetchAlbumsByArtistId(Integer artistId) {
        RetrofitClient.getApiService().getArtistAlbums(artistId).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Object data = response.body().get("data");
                    if (data instanceof List) {
                        albums.clear();
                        albums.addAll((List<Map<String, Object>>) data);
                        adapter = new ManageAlbumAdapter(albums);
                        rvAlbums.setAdapter(adapter);
                    }
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Toast.makeText(ManageAlbumsActivity.this, "Lỗi tải album", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void deleteAlbum(Integer albumId, int position) {
        new android.app.AlertDialog.Builder(this)
            .setTitle("Xóa album?")
            .setMessage("Hành động này không thể hoàn tác.")
            .setPositiveButton("Xóa", (dialog, which) -> {
                RetrofitClient.getApiService().deleteAlbum(albumId).enqueue(new Callback<Map<String, Object>>() {
                    @Override
                    public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                        if (response.isSuccessful()) {
                            albums.remove(position);
                            adapter.notifyItemRemoved(position);
                            Toast.makeText(ManageAlbumsActivity.this, "Đã xóa album thành công", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(ManageAlbumsActivity.this, "Không thể xóa album", Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                        Toast.makeText(ManageAlbumsActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
                    }
                });
            })
            .setNegativeButton("Hủy", null)
            .show();
    }

    private class ManageAlbumAdapter extends RecyclerView.Adapter<ManageAlbumAdapter.VH> {
        private List<Map<String, Object>> albums;

        ManageAlbumAdapter(List<Map<String, Object>> albums) {
            this.albums = albums;
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_manage_album, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            Map<String, Object> album = albums.get(position);
            holder.title.setText((String) album.get("title"));
            
            Object songCount = album.get("songCount");
            if (songCount == null) songCount = album.get("song_count");
            holder.stats.setText((songCount != null ? songCount : 0) + " bài hát");
            
            String coverUrl = (String) album.get("cover_url");
            Glide.with(ManageAlbumsActivity.this).load(com.appad.utils.ImageUrlUtils.fixUrl(coverUrl)).into(holder.cover);

            holder.btnDelete.setOnClickListener(v -> {
                Integer id = ((Number) album.get("album_id")).intValue();
                deleteAlbum(id, position);
            });
            
            holder.btnEdit.setOnClickListener(v -> {
                Integer id = ((Number) album.get("album_id")).intValue();
                Intent intent = new Intent(ManageAlbumsActivity.this, EditAlbumActivity.class);
                intent.putExtra("ALBUM_ID", id);
                startActivity(intent);
            });
        }

        @Override
        public int getItemCount() {
            return albums.size();
        }

        class VH extends RecyclerView.ViewHolder {
            TextView title, stats;
            ImageView cover;
            ImageButton btnEdit, btnDelete;

            VH(View itemView) {
                super(itemView);
                title = itemView.findViewById(R.id.txtManageAlbumTitle);
                stats = itemView.findViewById(R.id.txtManageAlbumStats);
                cover = itemView.findViewById(R.id.imgManageAlbumCover);
                btnEdit = itemView.findViewById(R.id.btnEditAlbum);
                btnDelete = itemView.findViewById(R.id.btnDeleteAlbum);
            }
        }
    }
}
