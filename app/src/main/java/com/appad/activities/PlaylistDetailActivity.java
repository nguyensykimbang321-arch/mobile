package com.appad.activities;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
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
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.appad.R;
import com.appad.adapters.DraggableSongAdapter;
import com.appad.models.Song;
import com.appad.utils.ImageUrlUtils;
import com.appad.utils.MusicPlayerManager;
import com.appad.utils.RetrofitClient;
import com.appad.utils.SongDragCallback;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PlaylistDetailActivity extends AppCompatActivity {

    private static final int PICK_IMAGE_REQUEST = 1001;

    private int playlistId;
    private String playlistName;
    private String playlistCoverUrl;
    
    private ImageButton btnBack, btnPlayAll, btnChangeCover, btnShare;
    private ImageView imgBackground, imgPlaylistCover;
    private TextView txtPlaylistTitle, txtPlaylistName, txtPlaylistSongCount, txtPlaylistDescription, txtEmpty;
    private RecyclerView rvPlaylistSongs;
    private ProgressBar progressBar;
    
    private List<Song> songList = new ArrayList<>();
    private DraggableSongAdapter adapter;
    private ItemTouchHelper itemTouchHelper;
    private com.appad.utils.MiniPlayerHelper miniPlayerHelper;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_playlist_detail);

        playlistId = getIntent().getIntExtra("PLAYLIST_ID", -1);
        playlistName = getIntent().getStringExtra("PLAYLIST_NAME");
        playlistCoverUrl = getIntent().getStringExtra("PLAYLIST_COVER_URL");

        initViews();
        setupListeners();
        
        if (playlistName != null) {
            txtPlaylistTitle.setText(playlistName);
            if (txtPlaylistName != null) txtPlaylistName.setText(playlistName);
        }
        
        loadPlaylistData();
        loadPlaylistSongs();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        btnPlayAll = findViewById(R.id.btnPlayAll);
        btnChangeCover = findViewById(R.id.btnChangeCover);
        imgBackground = findViewById(R.id.imgBackground);
        imgPlaylistCover = findViewById(R.id.imgPlaylistCover);
        txtPlaylistTitle = findViewById(R.id.txtPlaylistTitle);
        txtPlaylistName = findViewById(R.id.txtPlaylistName);
        txtPlaylistSongCount = findViewById(R.id.txtPlaylistSongCount);
        txtPlaylistDescription = findViewById(R.id.txtPlaylistDescription);
        txtEmpty = findViewById(R.id.txtEmpty);
        rvPlaylistSongs = findViewById(R.id.rvPlaylistSongs);
        progressBar = findViewById(R.id.progressBar);
        
        btnShare = findViewById(R.id.btnShare);
        
        rvPlaylistSongs.setLayoutManager(new LinearLayoutManager(this));
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());
        
        btnPlayAll.setOnClickListener(v -> {
            if (!songList.isEmpty()) {
                Song first = songList.get(0);
                com.appad.utils.AccessHelper.checkAccess(this, first, true, () -> {
                   MusicPlayerManager.getInstance().setPlaylist(songList, 0);
                   Intent intent = new Intent(this, FullPlayerActivity.class);
                   intent.putExtra("title", first.getTitle());
                   intent.putExtra("artist", first.getArtistName());
                   intent.putExtra("cover", first.getCoverUrl());
                   startActivity(intent);
                });
            }
        });

        if (btnChangeCover != null) {
            btnChangeCover.setOnClickListener(v -> openImagePicker());
        }

        btnShare.setOnClickListener(v -> generateShareCode());
    }

    private void generateShareCode() {
        if (playlistId == -1) return;
        
        Toast.makeText(this, "Đang tạo mã chia sẻ...", Toast.LENGTH_SHORT).show();
        RetrofitClient.getApiService().getShareCode(playlistId).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Object data = response.body().get("data");
                    if (data != null) {
                        showShareCodeDialog(data.toString());
                    }
                }
            }
            @Override public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Toast.makeText(PlaylistDetailActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showShareCodeDialog(String code) {
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_share_playlist, null);
        TextView txtCode = dialogView.findViewById(R.id.txtShareCode);
        ImageButton btnCopy = dialogView.findViewById(R.id.btnCopyCode);
        
        txtCode.setText(code);
        
        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(this)
                .setView(dialogView)
                .create();
        
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        btnCopy.setOnClickListener(v -> {
            android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(android.content.Context.CLIPBOARD_SERVICE);
            android.content.ClipData clip = android.content.ClipData.newPlainText("Playlist Code", code);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "Đã sao chép mã", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });
        
        dialog.show();
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_IMAGE_REQUEST && resultCode == RESULT_OK && data != null && data.getData() != null) {
            Uri imageUri = data.getData();
            uploadPlaylistCover(imageUri);
        }
    }

    private void uploadPlaylistCover(Uri imageUri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(imageUri);
            File tempFile = new File(getCacheDir(), "playlist_cover_" + playlistId + ".jpg");
            FileOutputStream outputStream = new FileOutputStream(tempFile);
            byte[] buffer = new byte[1024];
            int length;
            while ((length = inputStream.read(buffer)) > 0) {
                outputStream.write(buffer, 0, length);
            }
            outputStream.close();
            inputStream.close();

            RequestBody requestBody = RequestBody.create(MediaType.parse("image/*"), tempFile);
            MultipartBody.Part coverPart = MultipartBody.Part.createFormData("cover", tempFile.getName(), requestBody);

            Toast.makeText(this, "Đang tải ảnh lên...", Toast.LENGTH_SHORT).show();

            RetrofitClient.getApiService().updatePlaylistCover(playlistId, coverPart).enqueue(new Callback<Map<String, Object>>() {
                @Override
                public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        // Success - silently update background
                        Object data = response.body().get("data");
                        if (data instanceof Map) {
                            Object newUrl = ((Map<?, ?>) data).get("cover_url");
                            if (newUrl != null) {
                                playlistCoverUrl = newUrl.toString();
                                loadBackgroundImage(playlistCoverUrl);
                            }
                        }
                    } else {
                        String errorMsg = "Lỗi server: " + response.code();
                        try {
                            if (response.errorBody() != null) {
                                Map<String, Object> errorMap = new com.google.gson.Gson().fromJson(response.errorBody().string(), Map.class);
                                if (errorMap.containsKey("message")) errorMsg = errorMap.get("message").toString();
                            }
                        } catch (Exception e) {}
                        Toast.makeText(PlaylistDetailActivity.this, "Lỗi cập nhật ảnh bìa: " + errorMsg, Toast.LENGTH_LONG).show();
                    }
                }

                @Override
                public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                    Toast.makeText(PlaylistDetailActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_LONG).show();
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Lỗi xử lý ảnh", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadPlaylistData() {
        // Load playlist info to get cover_url
        RetrofitClient.getApiService().getPlaylistById(playlistId).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Object data = response.body().get("data");
                    if (data instanceof Map) {
                        Map<?, ?> playlist = (Map<?, ?>) data;
                        Object coverUrl = playlist.get("cover_url");
                        if (coverUrl != null && !coverUrl.toString().isEmpty()) {
                            playlistCoverUrl = coverUrl.toString();
                            loadBackgroundImage(playlistCoverUrl);
                        }
                    }
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                // Ignore, will use first song's cover as fallback
            }
        });
    }

    private void loadPlaylistSongs() {
        if (playlistId == -1) return;
        
        progressBar.setVisibility(View.VISIBLE);
        txtEmpty.setVisibility(View.GONE);
        
        RetrofitClient.getApiService().getPlaylistSongs(playlistId).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                progressBar.setVisibility(View.GONE);
                
                if (response.isSuccessful() && response.body() != null) {
                    Object data = response.body().get("data");
                    if (data != null) {
                        try {
                            com.google.gson.Gson gson = new com.google.gson.Gson();
                            String json = gson.toJson(data);
                            java.lang.reflect.Type listType = new com.google.gson.reflect.TypeToken<List<Song>>(){}.getType();
                            List<Song> songs = gson.fromJson(json, listType);
                            
                            songList.clear();
                            if (songs != null) songList.addAll(songs);
                            
                            updateUI();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                txtEmpty.setVisibility(View.VISIBLE);
                txtEmpty.setText("Lỗi tải danh sách bài hát");
            }
        });
    }

    private void updateUI() {
        if (txtPlaylistSongCount != null) {
            txtPlaylistSongCount.setText(songList.size() + " bài hát");
        }
        
        if (songList.isEmpty()) {
            txtEmpty.setVisibility(View.VISIBLE);
            rvPlaylistSongs.setVisibility(View.GONE);
        } else {
            txtEmpty.setVisibility(View.GONE);
            rvPlaylistSongs.setVisibility(View.VISIBLE);
            
            // Sử dụng DraggableSongAdapter để hỗ trợ kéo thả và xóa bài hát
            adapter = new DraggableSongAdapter(this, playlistId, songList);
            adapter.setOnOrderChangeListener(newOrder -> {
                saveNewOrder(newOrder);
            });
            
            // Thiết lập ItemTouchHelper cho kéo thả
            SongDragCallback callback = new SongDragCallback(adapter);
            itemTouchHelper = new ItemTouchHelper(callback);
            itemTouchHelper.attachToRecyclerView(rvPlaylistSongs);
            adapter.setItemTouchHelper(itemTouchHelper);
            
            rvPlaylistSongs.setAdapter(adapter);
            
            // Load background: use playlist cover or first song's cover
            if (playlistCoverUrl == null || playlistCoverUrl.isEmpty()) {
                String fallbackCover = songList.get(0).getCoverUrl();
                if (fallbackCover != null && !fallbackCover.isEmpty()) {
                    loadBackgroundImage(fallbackCover);
                }
            }
        }
    }

    private void loadBackgroundImage(String imageUrl) {
        if (imgBackground == null || imageUrl == null) return;
        
        Glide.with(this)
            .asBitmap()
            .load(ImageUrlUtils.fixUrl(imageUrl))
            .into(new CustomTarget<Bitmap>() {
                @Override
                public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                    // Apply blur effect
                    Bitmap blurredBitmap = blurBitmap(resource, 25f);
                    imgBackground.setImageBitmap(blurredBitmap);
                }

                @Override
                public void onLoadCleared(@Nullable Drawable placeholder) {}
            });
    }

    private Bitmap blurBitmap(Bitmap bitmap, float radius) {
        try {
            // Scale down for better performance
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
        } catch (Exception e) {
            e.printStackTrace();
            return bitmap;
        }
    }

    private void saveNewOrder(List<Song> newOrder) {
        List<Integer> songIds = new ArrayList<>();
        for (Song song : newOrder) {
            songIds.add(song.getSongId());
        }
        
        java.util.Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("song_ids", songIds);
        
        RetrofitClient.getApiService().reorderPlaylistSongs(playlistId, payload).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                // Silent success - không cần thông báo
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                // Silent fail
            }
        });

        // Đồng bộ với trình phát nhạc nếu người dùng đang phát từ playlist này
        syncWithPlayer(newOrder);
    }

    private void syncWithPlayer(List<Song> currentList) {
        MusicPlayerManager playerManager = MusicPlayerManager.getInstance();
        Song currentSong = playerManager.getCurrentSong();
        
        if (currentSong != null) {
            List<Song> playerPlaylist = playerManager.getPlaylist();
            
            // Kiểm tra xem trình phát có đang sử dụng playlist này không
            // Chúng ta dựa vào việc bài hát hiện tại có trong playlist này và kích thước playlist khớp
            // (vì trong manager không lưu playlistId)
            boolean isCurrentInPlaylist = false;
            for (Song s : currentList) {
                if (s.getSongId().equals(currentSong.getSongId())) {
                    isCurrentInPlaylist = true;
                    break;
                }
            }

            if (isCurrentInPlaylist && playerPlaylist != null) {
                // Nếu kích thước khớp hoặc kích thước manager lớn hơn (có thể do đã load thêm/hoặc playlist cũ)
                // nhưng quan trọng là bài hát hiện tại thuộc playlist này, ta nên cập nhật để người dùng thấy sự thay đổi
                playerManager.updatePlaylist(new ArrayList<>(currentList));
            }
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
