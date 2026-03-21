package com.appad.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.appad.R;
import com.appad.adapters.SongAdapter;
import com.appad.models.Genre;
import com.appad.models.Song;
import com.appad.services.ApiService;
import com.appad.utils.MusicPlayerManager;
import com.appad.utils.RetrofitClient;
import com.bumptech.glide.Glide;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class GenreDetailActivity extends AppCompatActivity {

    private Integer genreId;
    private ImageView imgCover;
    private TextView txtTitle, txtDescription, txtMeta;
    private Button btnPlay;
    private RecyclerView rvSongs;
    private Toolbar toolbar;

    private List<Song> genreSongs = new ArrayList<>();
    private SongAdapter songAdapter;
    private ApiService apiService;
    private com.appad.utils.MiniPlayerHelper miniPlayerHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_genre_detail);

        genreId = getIntent().getIntExtra("genreId", -1);
        if (genreId == -1) {
            finish();
            return;
        }

        apiService = RetrofitClient.getApiService();
        initViews();
        setupAdapters();
        loadGenreData();
    }

    private void initViews() {
        imgCover = findViewById(R.id.imgGenreCoverLarge);
        txtTitle = findViewById(R.id.txtGenreTitle);
        txtDescription = findViewById(R.id.txtGenreDescription);
        txtMeta = findViewById(R.id.txtGenreMeta);
        btnPlay = findViewById(R.id.btnPlayGenre);
        rvSongs = findViewById(R.id.rvGenreSongs);
        toolbar = findViewById(R.id.toolbarGenre);

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("");
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupAdapters() {
        rvSongs.setLayoutManager(new LinearLayoutManager(this));
        songAdapter = new SongAdapter(this, genreSongs);
        rvSongs.setAdapter(songAdapter);

        btnPlay.setOnClickListener(v -> {
            if (!genreSongs.isEmpty()) {
                com.appad.utils.AccessHelper.checkAccess(this, genreSongs.get(0), true, () -> {
                    MusicPlayerManager.getInstance().setPlaylist(genreSongs, 0);
                    startActivity(new Intent(this, FullPlayerActivity.class));
                });
            } else {
                Toast.makeText(this, "Không có bài hát để phát", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadGenreData() {
        // Load Genre Info
        apiService.getGenreById(genreId).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Object> data = (Map<String, Object>) response.body().get("data");
                    if (data != null) {
                        String name = (String) data.get("name");
                        String description = (String) data.get("description");
                        String coverUrl = (String) data.get("coverUrl");
                        if (coverUrl == null) coverUrl = (String) data.get("cover_url");

                        txtTitle.setText(name);
                        txtDescription.setText(description != null ? description : "Khám phá các bài hát trong " + name);
                        
                        if (coverUrl != null && !coverUrl.isEmpty()) {
                            Glide.with(GenreDetailActivity.this)
                                    .load(com.appad.utils.ImageUrlUtils.fixUrl(coverUrl))
                                    .placeholder(R.drawable.ic_launcher_background)
                                    .into(imgCover);
                        }
                    }
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Toast.makeText(GenreDetailActivity.this, "Lỗi khi tải thông tin thể loại", Toast.LENGTH_SHORT).show();
            }
        });

        // Load Songs
        apiService.getSongsByGenre(genreId).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Object data = response.body().get("data");
                    if (data instanceof List) {
                        Gson gson = new Gson();
                        String json = gson.toJson(data);
                        Type listType = new TypeToken<List<Song>>(){}.getType();
                        List<Song> songs = gson.fromJson(json, listType);
                        
                        genreSongs.clear();
                        genreSongs.addAll(songs);
                        songAdapter.notifyDataSetChanged();
                        
                        txtMeta.setText(genreSongs.size() + " bài hát");

                        // If genre doesn't have a cover, use first song's cover
                        if (imgCover.getDrawable() == null && !songs.isEmpty()) {
                             Glide.with(GenreDetailActivity.this)
                                    .load(com.appad.utils.ImageUrlUtils.fixUrl(songs.get(0).getCoverUrl()))
                                    .placeholder(R.drawable.ic_launcher_background)
                                    .into(imgCover);
                        }
                    }
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Toast.makeText(GenreDetailActivity.this, "Lỗi khi tải danh sách bài hát", Toast.LENGTH_SHORT).show();
            }
        });
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
