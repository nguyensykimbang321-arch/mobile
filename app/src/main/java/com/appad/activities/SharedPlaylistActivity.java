package com.appad.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.appad.R;
import com.appad.adapters.SongAdapter;
import com.appad.models.Song;
import com.appad.utils.ImageUrlUtils;
import com.appad.utils.MusicPlayerManager;
import com.appad.utils.RetrofitClient;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class SharedPlaylistActivity extends AppCompatActivity {

    private String shareCode;
    private String playlistName;
    private List<Song> songList = new ArrayList<>();
    
    private ImageButton btnBack, btnPlayAll, btnImport;
    private ImageView imgBackground;
    private TextView txtSnapshotTitle, txtSnapshotSub;
    private RecyclerView rvSnapshotSongs;
    private ProgressBar progressBar;
    
    private com.appad.utils.MiniPlayerHelper miniPlayerHelper;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shared_playlist);

        shareCode = getIntent().getStringExtra("SHARE_CODE");
        if (shareCode == null) {
            finish();
            return;
        }

        initViews();
        setupListeners();
        loadSnapshotData();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        btnPlayAll = findViewById(R.id.btnPlayAll);
        btnImport = findViewById(R.id.btnImport);
        imgBackground = findViewById(R.id.imgBackground);
        txtSnapshotTitle = findViewById(R.id.txtSnapshotTitle);
        txtSnapshotSub = findViewById(R.id.txtSnapshotSub);
        rvSnapshotSongs = findViewById(R.id.rvSnapshotSongs);
        progressBar = findViewById(R.id.progressBar);
        
        rvSnapshotSongs.setLayoutManager(new LinearLayoutManager(this));
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());
        
        btnPlayAll.setOnClickListener(v -> {
            if (!songList.isEmpty()) {
                MusicPlayerManager.getInstance().setPlaylist(songList, 0);
                startActivity(new Intent(this, FullPlayerActivity.class));
            }
        });

        btnImport.setOnClickListener(v -> importPlaylist());
    }

    private void loadSnapshotData() {
        progressBar.setVisibility(View.VISIBLE);
        RetrofitClient.getApiService().getPlaylistByShareCode(shareCode).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Object> data = (Map<String, Object>) response.body().get("data");
                    if (data != null) {
                        playlistName = (String) data.get("name");
                        txtSnapshotTitle.setText(playlistName);
                        
                        Object songsData = data.get("songs");
                        com.google.gson.Gson gson = new com.google.gson.Gson();
                        String json = gson.toJson(songsData);
                        java.lang.reflect.Type listType = new com.google.gson.reflect.TypeToken<List<Song>>(){}.getType();
                        List<Song> songs = gson.fromJson(json, listType);
                        
                        songList.clear();
                        if (songs != null) songList.addAll(songs);
                        
                        updateUI();
                    }
                } else {
                    Toast.makeText(SharedPlaylistActivity.this, "Không thể tải snapshot", Toast.LENGTH_SHORT).show();
                    finish();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(SharedPlaylistActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
                finish();
            }
        });
    }

    private void updateUI() {
        txtSnapshotSub.setText(songList.size() + " bài hát • Snapshot");
        
        SongAdapter adapter = new SongAdapter(this, songList);
        rvSnapshotSongs.setAdapter(adapter);
        
        if (!songList.isEmpty() && songList.get(0).getCoverUrl() != null) {
            loadBackgroundImage(songList.get(0).getCoverUrl());
        }
    }

    private void loadBackgroundImage(String imageUrl) {
        Glide.with(this)
            .asBitmap()
            .load(ImageUrlUtils.fixUrl(imageUrl))
            .into(new CustomTarget<Bitmap>() {
                @Override
                public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                    Bitmap blurredBitmap = blurBitmap(resource, 25f);
                    imgBackground.setImageBitmap(blurredBitmap);
                }
                @Override public void onLoadCleared(@Nullable Drawable placeholder) {}
            });
    }

    private Bitmap blurBitmap(Bitmap bitmap, float radius) {
        try {
            float scale = 0.2f;
            int width = Math.round(bitmap.getWidth() * scale);
            int height = Math.round(bitmap.getHeight() * scale);
            Bitmap inputBitmap = Bitmap.createScaledBitmap(bitmap, width, height, false);
            Bitmap outputBitmap = Bitmap.createBitmap(inputBitmap);
            RenderScript rs = RenderScript.create(this);
            ScriptIntrinsicBlur script = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs));
            Allocation inputAllocation = Allocation.createFromBitmap(rs, inputBitmap);
            Allocation outputAllocation = Allocation.createFromBitmap(rs, outputBitmap);
            script.setRadius(Math.min(radius, 25f));
            script.setInput(inputAllocation);
            script.forEach(outputAllocation);
            outputAllocation.copyTo(outputBitmap);
            rs.destroy();
            return outputBitmap;
        } catch (Exception e) { return bitmap; }
    }

    private void importPlaylist() {
        btnImport.setEnabled(false);
        Toast.makeText(this, "Đang nhập playlist...", Toast.LENGTH_SHORT).show();
        
        Map<String, String> payload = new java.util.HashMap<>();
        payload.put("code", shareCode);
        
        RetrofitClient.getApiService().importPlaylist(payload).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(SharedPlaylistActivity.this, "Đã nhập playlist thành công vào thư viện!", Toast.LENGTH_LONG).show();
                    btnImport.setVisibility(View.GONE);
                } else {
                    btnImport.setEnabled(true);
                    Toast.makeText(SharedPlaylistActivity.this, "Lỗi khi nhập playlist", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                btnImport.setEnabled(true);
                Toast.makeText(SharedPlaylistActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (miniPlayerHelper == null) miniPlayerHelper = new com.appad.utils.MiniPlayerHelper(this);
        miniPlayerHelper.setupMiniPlayer();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (miniPlayerHelper != null) miniPlayerHelper.detach();
    }
}
