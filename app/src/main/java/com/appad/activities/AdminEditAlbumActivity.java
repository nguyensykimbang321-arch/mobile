package com.appad.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.appad.R;
import com.appad.models.Album;
import com.appad.utils.RetrofitClient;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.bitmap.RoundedCorners;
import com.google.android.material.switchmaterial.SwitchMaterial;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import java.text.SimpleDateFormat;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminEditAlbumActivity extends AppCompatActivity {

    private EditText edtTitle, edtReleaseDate, edtPrice;
    private TextView txtSelectorArtist, txtCoverStatus, txtSongCount;
    private SwitchMaterial switchPremium;
    private ImageView imgCover;
    private ProgressBar progressCover;
    private LinearLayout layoutPrice, layoutSongs, containerSongs;
    private TextView btnDeleteAlbum;

    private Integer albumId = null;
    private Album editingAlbum;
    private List<Map<String, Object>> albumSongs = new ArrayList<>();

    private Map<String, Object> formData = new HashMap<>();
    private List<Map<String, Object>> artists = new ArrayList<>();

    private final ActivityResultLauncher<String> pickImage = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    uploadCover(uri);
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_edit_album);

        if (getIntent().hasExtra("album")) {
            editingAlbum = (Album) getIntent().getSerializableExtra("album");
            if (editingAlbum != null) {
                albumId = editingAlbum.getAlbumId();
            }
        }

        initViews();
        setupListeners();
        loadArtists();

        if (editingAlbum != null) {
            populateData();
            loadAlbumSongs();
        } else {
             // Default Date
             java.util.Calendar calendar = java.util.Calendar.getInstance();
             SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
             edtReleaseDate.setText(sdf.format(calendar.getTime()));
             formData.put("release_date", edtReleaseDate.getText().toString());
        }
    }

    private void initViews() {
        edtTitle = findViewById(R.id.edtTitle);
        edtReleaseDate = findViewById(R.id.edtReleaseDate);
        edtPrice = findViewById(R.id.edtPrice);
        
        txtSelectorArtist = findViewById(R.id.txtSelectorArtist);
        txtCoverStatus = findViewById(R.id.txtCoverStatus);
        txtSongCount = findViewById(R.id.txtSongCount);
        
        switchPremium = findViewById(R.id.switchPremium);
        imgCover = findViewById(R.id.imgCover);
        progressCover = findViewById(R.id.progressCover);
        
        layoutPrice = findViewById(R.id.layoutPrice);
        layoutSongs = findViewById(R.id.layoutSongs);
        containerSongs = findViewById(R.id.containerSongs);
        btnDeleteAlbum = findViewById(R.id.btnDeleteAlbum);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnSave).setOnClickListener(v -> saveAlbum());
        findViewById(R.id.layoutCover).setOnClickListener(v -> pickImage.launch("image/*"));
        findViewById(R.id.btnAddSongToAlbum).setOnClickListener(v -> {
            Intent intent = new Intent(this, AdminEditSongActivity.class);
            intent.putExtra("initialAlbumId", albumId);
            startActivity(intent);
        });
        
        btnDeleteAlbum.setOnClickListener(v -> confirmDelete());
    }

    private void setupListeners() {
        switchPremium.setOnCheckedChangeListener((buttonView, isChecked) -> {
            layoutPrice.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            formData.put("is_premium", isChecked);
        });

        txtSelectorArtist.setOnClickListener(v -> showArtistSelection());
    }

    private void populateData() {
        TextView headerTitle = findViewById(R.id.txtHeaderTitle);
        headerTitle.setText("CẬP NHẬT ALBUM");
        
        layoutSongs.setVisibility(View.VISIBLE);
        btnDeleteAlbum.setVisibility(View.VISIBLE);

        edtTitle.setText(editingAlbum.getTitle());
        formData.put("title", editingAlbum.getTitle());
        
        formData.put("artist_id", editingAlbum.getArtistId());
        txtSelectorArtist.setText(editingAlbum.getArtistName());
        
        edtReleaseDate.setText(editingAlbum.getReleaseDate() != null ? editingAlbum.getReleaseDate().split("T")[0] : "");
        formData.put("release_date", edtReleaseDate.getText().toString());
        
        boolean isPrem = editingAlbum.getIsPremium() == 1;
        switchPremium.setChecked(isPrem);
        formData.put("is_premium", isPrem);
        
        if (editingAlbum.getPrice() != null) {
            edtPrice.setText(String.valueOf(editingAlbum.getPrice().intValue()));
            formData.put("price", editingAlbum.getPrice().intValue());
        }
        
        formData.put("cover_url", editingAlbum.getCoverUrl());
        if (editingAlbum.getCoverUrl() != null) {
            Glide.with(this).load(editingAlbum.getCoverUrl()).transform(new RoundedCorners(24)).into(imgCover);
            txtCoverStatus.setText("Đã chọn ảnh bìa");
            txtCoverStatus.setTextColor(getColor(android.R.color.holo_green_light));
        }
    }

    private void loadArtists() {
        RetrofitClient.getApiService().getAllArtistsAdmin().enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    artists = (List<Map<String, Object>>) response.body().get("data");
                    if (editingAlbum != null && artists != null) {
                        for (Map<String, Object> a : artists) {
                            if (matchesId(a.get("artist_id"), editingAlbum.getArtistId())) {
                                txtSelectorArtist.setText((String) a.get("name"));
                                break;
                            }
                        }
                    }
                }
            }
            @Override public void onFailure(Call<Map<String, Object>> call, Throwable t) {}
        });
    }

    private void loadAlbumSongs() {
        if (albumId == null) return;
        RetrofitClient.getApiService().getSongsByAlbumAdmin(albumId).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Object data = response.body().get("data");
                    if (data instanceof List) {
                        albumSongs = (List<Map<String, Object>>) data;
                        updateSongsList();
                    }
                }
            }
            @Override public void onFailure(Call<Map<String, Object>> call, Throwable t) {}
        });
    }

    private void updateSongsList() {
        containerSongs.removeAllViews();
        txtSongCount.setText(albumSongs.size() + " bài hát trong album");
        
        LayoutInflater inflater = LayoutInflater.from(this);
        int rank = 1;
        
        for (Map<String, Object> song : albumSongs) {
             View itemView = inflater.inflate(R.layout.item_admin_song_simple, containerSongs, false);
             TextView txtIndex = itemView.findViewById(R.id.txtIndex);
             TextView txtTitle = itemView.findViewById(R.id.txtTitle);
             
             txtIndex.setText(rank + ".");
             txtTitle.setText((String) song.get("title"));
             
             containerSongs.addView(itemView);
             rank++;
        }
    }

    private void showArtistSelection() {
        if (artists == null || artists.isEmpty()) return;
        String[] names = new String[artists.size()];
        for (int i = 0; i < artists.size(); i++) names[i] = (String) artists.get(i).get("name");
        
        new AlertDialog.Builder(this)
                .setTitle("Chọn nghệ sĩ")
                .setItems(names, (dialog, which) -> {
                    Map<String, Object> sel = artists.get(which);
                    formData.put("artist_id", sel.get("artist_id"));
                    txtSelectorArtist.setText((String) sel.get("name"));
                })
                .show();
    }

    private void uploadCover(Uri uri) {
        try {
            InputStream is = getContentResolver().openInputStream(uri);
            File file = new File(getCacheDir(), "cover_" + System.currentTimeMillis() + ".jpg");
            FileOutputStream fos = new FileOutputStream(file);
            byte[] buf = new byte[1024];
            int len;
            while ((len = is.read(buf)) > 0) fos.write(buf, 0, len);
            is.close();
            fos.close();

            RequestBody requestFile = RequestBody.create(MediaType.parse("multipart/form-data"), file);
            MultipartBody.Part body = MultipartBody.Part.createFormData("file", file.getName(), requestFile);

            progressCover.setVisibility(View.VISIBLE);
            RetrofitClient.getApiService().uploadAdminCoverFile(body).enqueue(new Callback<Map<String, Object>>() {
                @Override
                public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                    progressCover.setVisibility(View.GONE);
                    if (response.isSuccessful() && response.body() != null) {
                         Map<String, Object> data = (Map<String, Object>) response.body().get("data");
                         if (data == null) data = response.body();
                         
                         String url = (String) data.get("file_url");
                         if (url == null) url = (String) data.get("url");
                         
                         formData.put("cover_url", url);
                         Glide.with(AdminEditAlbumActivity.this).load(url).transform(new RoundedCorners(24)).into(imgCover);
                         txtCoverStatus.setText("Upload thành công!");
                    } else {
                        Toast.makeText(AdminEditAlbumActivity.this, "Upload thất bại", Toast.LENGTH_SHORT).show();
                    }
                }
                @Override public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                    progressCover.setVisibility(View.GONE);
                    Toast.makeText(AdminEditAlbumActivity.this, "Lỗi upload", Toast.LENGTH_SHORT).show();
                }
            });
        } catch(Exception e) { e.printStackTrace(); }
    }

    private void saveAlbum() {
        formData.put("title", edtTitle.getText().toString().trim());
        formData.put("release_date", edtReleaseDate.getText().toString().trim());
        if (switchPremium.isChecked()) {
             try {
                formData.put("price", Double.parseDouble(edtPrice.getText().toString().trim()));
             } catch (Exception e) { formData.put("price", 0); }
        }

        if (formData.get("title").toString().isEmpty() || formData.get("artist_id") == null) {
            Toast.makeText(this, "Thiếu thông tin bắt buộc", Toast.LENGTH_SHORT).show();
            return;
        }

        Call<Map<String, Object>> call;
        if (albumId != null) call = RetrofitClient.getApiService().updateAlbumAdmin(albumId, formData);
        else call = RetrofitClient.getApiService().createAlbumAdmin(formData);

        call.enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(AdminEditAlbumActivity.this, "Lưu thành công", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(AdminEditAlbumActivity.this, "Lỗi lưu dữ liệu " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }
            @Override public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                 Toast.makeText(AdminEditAlbumActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setTitle("Xóa Album")
                .setMessage("Bạn có chắc chắn muốn xóa vĩnh viễn album này?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    RetrofitClient.getApiService().deleteAlbum(albumId).enqueue(new Callback<Map<String, Object>>() {
                        @Override
                        public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                             if (response.isSuccessful()) {
                                 Toast.makeText(AdminEditAlbumActivity.this, "Đã xóa album", Toast.LENGTH_SHORT).show();
                                 finish();
                             }
                        }
                        @Override public void onFailure(Call<Map<String, Object>> call, Throwable t) {}
                    });
                })
                .setNegativeButton("Hủy", null)
                .show();
    }
    
    private boolean matchesId(Object val1, Object val2) {
        if (val1 == null || val2 == null) return false;
        return String.valueOf(val1).equals(String.valueOf(val2));
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        if (albumId != null) loadAlbumSongs(); // Refresh songs when coming back from adding a song
    }
}
