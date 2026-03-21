package com.appad.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.appad.R;
import com.appad.adapters.SongAdapter;
import com.appad.models.Song;
import com.appad.utils.MusicPlayerManager;
import com.appad.utils.RetrofitClient;
import com.bumptech.glide.Glide;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AlbumDetailActivity extends AppCompatActivity {

    private Integer albumId;
    private ImageView imgCover;
    private TextView txtTitle, txtArtist, txtMeta;
    private Button btnPlay;
    private RecyclerView rvSongs;
    private Toolbar toolbar;
    private android.view.View rootLayout;
    private com.google.android.material.appbar.CollapsingToolbarLayout collapsingToolbar;

    private List<Song> albumSongs = new ArrayList<>();
    private SongAdapter songAdapter;
    private com.appad.utils.MiniPlayerHelper miniPlayerHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Immersive mode / Transparent Status Bar
        getWindow().getDecorView().setSystemUiVisibility(
            android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE | android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        );
        getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
        
        setContentView(R.layout.activity_album_detail);

        albumId = getIntent().getIntExtra("albumId", -1);
        if (albumId == -1) {
            finish();
            return;
        }

        initViews();
        setupAdapters();
        loadAlbumData();
    }

    private void initViews() {
        imgCover = findViewById(R.id.imgAlbumCoverLarge);
        txtTitle = findViewById(R.id.txtAlbumTitle);
        txtArtist = findViewById(R.id.txtAlbumArtist);
        txtMeta = findViewById(R.id.txtAlbumMeta);
        btnPlay = findViewById(R.id.btnPlayAlbum);
        rvSongs = findViewById(R.id.rvAlbumSongs);
        toolbar = findViewById(R.id.toolbarAlbum);
        rootLayout = findViewById(R.id.albumRootLayout);
        collapsingToolbar = findViewById(R.id.albumCollapsingToolbar);

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("");
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupAdapters() {
        rvSongs.setLayoutManager(new LinearLayoutManager(this));
        songAdapter = new SongAdapter(this, albumSongs);
        rvSongs.setAdapter(songAdapter);

        btnPlay.setOnClickListener(v -> {
            if (!albumSongs.isEmpty()) {
                com.appad.utils.AccessHelper.checkAccess(this, albumSongs.get(0), true, () -> {
                    MusicPlayerManager.getInstance().setPlaylist(albumSongs, 0);
                    startActivity(new Intent(this, FullPlayerActivity.class));
                });
            }
        });
    }

    private void loadAlbumData() {
        RetrofitClient.getApiService().getAlbumById(albumId).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Object> data = (Map<String, Object>) response.body().get("data");
                    if (data != null) {
                        String title = (String) (data.get("title") != null ? data.get("title") : data.get("name"));
                        String artistName = (String) (data.get("artist_name") != null ? data.get("artist_name") : data.get("artistName"));
                        String coverUrl = (String) (data.get("cover_url") != null ? data.get("cover_url") : data.get("coverUrl"));
                        
                        Object releaseObj = data.get("release_date") != null ? data.get("release_date") : data.get("releaseDate");
                        String year = "2024";
                        if (releaseObj != null) {
                            String rel = String.valueOf(releaseObj);
                            if (rel.length() >= 4) year = rel.substring(0, 4);
                        }
                        
                        txtTitle.setText(title);
                        txtArtist.setText(artistName != null ? artistName : "Nghệ sĩ");
                        
                        if (albumSongs.isEmpty()) {
                            txtMeta.setText(year + " • Đang tải...");
                        } else {
                            txtMeta.setText(year + " • " + albumSongs.size() + " bài hát");
                        }
                        
                        Glide.with(AlbumDetailActivity.this)
                                .asBitmap()
                                .load(com.appad.utils.ImageUrlUtils.fixUrl(coverUrl))
                                .placeholder(R.drawable.ic_launcher_background)
                                .into(new com.bumptech.glide.request.target.CustomTarget<android.graphics.Bitmap>() {
                                    @Override
                                    public void onResourceReady(@androidx.annotation.NonNull android.graphics.Bitmap resource, @androidx.annotation.Nullable com.bumptech.glide.request.transition.Transition<? super android.graphics.Bitmap> transition) {
                                        imgCover.setImageBitmap(resource);
                                        androidx.palette.graphics.Palette.from(resource).generate(palette -> {
                                            if (palette != null) {
                                                int dominantColor = palette.getDominantColor(0xFF121212);
                                                int vibrantColor = palette.getVibrantColor(dominantColor);
                                                int darkMutedColor = palette.getDarkMutedColor(0xFF000000);
                                                int lightColor = palette.getLightVibrantColor(vibrantColor);
                                                
                                                // Create a lush gradient starting from the top
                                                android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable(
                                                    android.graphics.drawable.GradientDrawable.Orientation.TOP_BOTTOM,
                                                    new int[] {lightColor, dominantColor, darkMutedColor}
                                                );
                                                gd.setCornerRadius(0f);
                                                
                                                if (rootLayout != null) {
                                                    rootLayout.setBackground(gd);
                                                }
                                                
                                                if (collapsingToolbar != null) {
                                                    // Set scrim color for when toolbar collapses
                                                    collapsingToolbar.setContentScrimColor(dominantColor);
                                                    collapsingToolbar.setStatusBarScrimColor(dominantColor);
                                                }
                                            }
                                        });
                                    }
                                    @Override public void onLoadCleared(@androidx.annotation.Nullable android.graphics.drawable.Drawable placeholder) {}
                                });
                    }
                }
            }
            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {}
        });

        RetrofitClient.getApiService().getSongsByAlbum(albumId).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    processResponse(response.body(), albumSongs, songAdapter);
                    String currentMeta = txtMeta.getText().toString();
                    if (currentMeta.contains(" • ")) {
                        String year = currentMeta.split(" • ")[0];
                        txtMeta.setText(year + " • " + albumSongs.size() + " bài hát");
                    } else {
                        txtMeta.setText(albumSongs.size() + " bài hát");
                    }
                }
            }
            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {}
        });
    }

    private void processResponse(Map<String, Object> body, List<Song> list, SongAdapter adapter) {
        Object data = body.get("data");
        if (data instanceof List) {
            com.google.gson.Gson gson = new com.google.gson.Gson();
            String json = gson.toJson(data);
            java.lang.reflect.Type listType = new com.google.gson.reflect.TypeToken<List<Song>>(){}.getType();
            List<Song> songs = gson.fromJson(json, listType);
            
            list.clear();
            list.addAll(songs);
            adapter.notifyDataSetChanged();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
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
}
