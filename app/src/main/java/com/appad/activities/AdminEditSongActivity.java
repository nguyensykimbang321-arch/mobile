package com.appad.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
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
import com.appad.models.Song;
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
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminEditSongActivity extends AppCompatActivity {

    private EditText edtTitle, edtReleaseDate, edtPrice, edtLyrics;
    private TextView txtSelectorArtist, txtSelectorAlbum, txtSelectorGenre, txtAudioStatus;
    private SwitchMaterial switchPremium;
    private ImageView imgCover, iconAudio;
    private ProgressBar progressCover, progressAudio;
    private LinearLayout btnUploadAudio, layoutPrice;

    private Integer songId = null;
    private Integer initialAlbumId = null;
    private Song editingSong;

    private Map<String, Object> formData = new HashMap<>();
    private List<Map<String, Object>> artists = new ArrayList<>();
    private List<Map<String, Object>> albums = new ArrayList<>();
    private List<Map<String, Object>> genres = new ArrayList<>();

    private File selectedCoverFile;
    private File selectedAudioFile;

    private final ActivityResultLauncher<String> pickAudio = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    uploadFile(uri, "audio");
                }
            }
    );

    private final ActivityResultLauncher<String> pickImage = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    uploadFile(uri, "image");
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_edit_song);

        // Get extras
        if (getIntent().hasExtra("song")) {
             editingSong = (Song) getIntent().getSerializableExtra("song");
             if (editingSong != null) {
                 songId = editingSong.getSong_id();
             }
        }
        if (getIntent().hasExtra("initialAlbumId")) {
            initialAlbumId = getIntent().getIntExtra("initialAlbumId", -1);
            if (initialAlbumId == -1) initialAlbumId = null;
        }

        initViews();
        setupListeners();
        loadReferenceData();
        
        if (editingSong != null) {
            populateData();
        } else {
            // Default Date
            java.util.Calendar calendar = java.util.Calendar.getInstance();
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
            edtReleaseDate.setText(sdf.format(calendar.getTime()));
            formData.put("release_date", edtReleaseDate.getText().toString());
            
            if (initialAlbumId != null) {
                formData.put("album_id", initialAlbumId);
                // We'll set artist when albums load
            }
        }
    }

    private void initViews() {
        edtTitle = findViewById(R.id.edtTitle);
        edtReleaseDate = findViewById(R.id.edtReleaseDate);
        edtPrice = findViewById(R.id.edtPrice);
        edtLyrics = findViewById(R.id.edtLyrics);
        
        txtSelectorArtist = findViewById(R.id.txtSelectorArtist);
        txtSelectorAlbum = findViewById(R.id.txtSelectorAlbum);
        txtSelectorGenre = findViewById(R.id.txtSelectorGenre);
        txtAudioStatus = findViewById(R.id.txtAudioStatus);
        
        switchPremium = findViewById(R.id.switchPremium);
        imgCover = findViewById(R.id.imgCover);
        iconAudio = findViewById(R.id.iconAudio);
        
        progressCover = findViewById(R.id.progressCover);
        progressAudio = findViewById(R.id.progressAudio);
        
        btnUploadAudio = findViewById(R.id.btnUploadAudio);
        layoutPrice = findViewById(R.id.layoutPrice);
        
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnSave).setOnClickListener(v -> saveSong());
        findViewById(R.id.layoutCover).setOnClickListener(v -> pickImage.launch("image/*"));
    }

    private void setupListeners() {
        btnUploadAudio.setOnClickListener(v -> pickAudio.launch("audio/*"));
        
        switchPremium.setOnCheckedChangeListener((buttonView, isChecked) -> {
            layoutPrice.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            formData.put("is_premium", isChecked);
        });

        txtSelectorArtist.setOnClickListener(v -> showSelectionDialog("Chọn nghệ sĩ", artists, "name", "artist_id"));
        txtSelectorAlbum.setOnClickListener(v -> showSelectionDialog("Chọn album", albums, "title", "album_id"));
        txtSelectorGenre.setOnClickListener(v -> showSelectionDialog("Chọn thể loại", genres, "name", "genre_id"));
    }

    private void populateData() {
        TextView headerTitle = findViewById(R.id.txtHeaderTitle);
        headerTitle.setText("CHỈNH SỬA NHẠC");
        
        edtTitle.setText(editingSong.getTitle());
        formData.put("title", editingSong.getTitle());
        
        formData.put("artist_id", editingSong.getArtist_id());
        txtSelectorArtist.setText(editingSong.getArtist_name());
        
        formData.put("album_id", editingSong.getAlbum_id());
        // Album title requires finding in list or passed data. We'll start blank and fill on reference load if needed
        
        formData.put("genre_id", editingSong.getGenre_id());
        // Genre name same issue
        
        edtReleaseDate.setText(editingSong.getRelease_date() != null ? editingSong.getRelease_date().split("T")[0] : "");
        formData.put("release_date", edtReleaseDate.getText().toString());
        
        boolean isPrem = editingSong.getIs_premium() == 1;
        switchPremium.setChecked(isPrem);
        formData.put("is_premium", isPrem);
        
        if (editingSong.getPrice() != null) {
            edtPrice.setText(String.valueOf(editingSong.getPrice().intValue()));
            formData.put("price", editingSong.getPrice().intValue());
        }
        
        edtLyrics.setText(editingSong.getLyrics());
        formData.put("lyrics", editingSong.getLyrics());
        
        formData.put("file_url", editingSong.getFile_url());
        if (editingSong.getFile_url() != null && !editingSong.getFile_url().isEmpty()) {
            txtAudioStatus.setText("Đã có file nhạc");
            iconAudio.setImageResource(android.R.drawable.checkbox_on_background);
            iconAudio.setColorFilter(getColor(R.color.green_500));
        }
        
        formData.put("cover_url", editingSong.getCover_url());
        if (editingSong.getCover_url() != null) {
            Glide.with(this).load(editingSong.getCover_url()).transform(new RoundedCorners(24)).into(imgCover);
        }
    }

    private void loadReferenceData() {
        // Genres
        RetrofitClient.getApiService().getGenres().enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    genres = (List<Map<String, Object>>) response.body().get("data");
                    if (editingSong != null) {
                        for (Map<String, Object> g : genres) {
                            if (matchesId(g.get("genre_id"), editingSong.getGenre_id())) {
                                txtSelectorGenre.setText((String) g.get("name"));
                                break;
                            }
                        }
                    }
                }
            }
            @Override public void onFailure(Call<Map<String, Object>> call, Throwable t) {}
        });

        // Artists
        RetrofitClient.getApiService().getAllArtistsAdmin().enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    artists = (List<Map<String, Object>>) response.body().get("data");
                    // Assuming API returns {success: true, data: [...]}
                    // If simple list, catch exception or check type
                     if (editingSong != null) {
                        for (Map<String, Object> a : artists) {
                            if (matchesId(a.get("artist_id"), editingSong.getArtist_id())) {
                                txtSelectorArtist.setText((String) a.get("name"));
                                break;
                            }
                        }
                    } else if (initialAlbumId != null && !albums.isEmpty()) {
                         updateArtistFromAlbum();
                     }
                }
            }
             @Override public void onFailure(Call<Map<String, Object>> call, Throwable t) {}
        });

        // Albums
        RetrofitClient.getApiService().getAllAlbumsSimpleAdmin().enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    albums = (List<Map<String, Object>>) response.body().get("data");
                    if (editingSong != null) {
                        for (Map<String, Object> a : albums) {
                            if (matchesId(a.get("album_id"), editingSong.getAlbum_id())) {
                                txtSelectorAlbum.setText((String) a.get("title"));
                                break;
                            }
                        }
                    } else if (initialAlbumId != null) {
                        for (Map<String, Object> a : albums) {
                            if (matchesId(a.get("album_id"), initialAlbumId)) {
                                txtSelectorAlbum.setText((String) a.get("title"));
                                if (!artists.isEmpty()) updateArtistFromAlbum();
                                break;
                            }
                        }
                    }
                }
            }
             @Override public void onFailure(Call<Map<String, Object>> call, Throwable t) {}
        });
    }

    private void updateArtistFromAlbum() {
        if (initialAlbumId == null) return;
        for (Map<String, Object> album : albums) {
             if (matchesId(album.get("album_id"), initialAlbumId)) {
                 Object aId = album.get("artist_id");
                 formData.put("artist_id", aId);
                 
                 for (Map<String, Object> artist : artists) {
                     if (matchesId(artist.get("artist_id"), aId)) {
                         txtSelectorArtist.setText((String) artist.get("name"));
                         return;
                     }
                 }
             }
        }
    }
    
    private boolean matchesId(Object val1, Object val2) {
        if (val1 == null || val2 == null) return false;
        return String.valueOf(val1).equals(String.valueOf(val2));
    }

    private void showSelectionDialog(String title, List<Map<String, Object>> items, String displayKey, String idKey) {
        if (items == null || items.isEmpty()) {
            Toast.makeText(this, "Không có dữ liệu", Toast.LENGTH_SHORT).show();
            return;
        }

        String[] names = new String[items.size()];
        for (int i = 0; i < items.size(); i++) {
            names[i] = String.valueOf(items.get(i).get(displayKey));
        }

        new AlertDialog.Builder(this)
                .setTitle(title)
                .setItems(names, (dialog, which) -> {
                    Map<String, Object> selected = items.get(which);
                    formData.put(idKey, selected.get(idKey));
                    
                    TextView target = null;
                    if (idKey.equals("artist_id")) target = txtSelectorArtist;
                    else if (idKey.equals("album_id")) target = txtSelectorAlbum;
                    else if (idKey.equals("genre_id")) target = txtSelectorGenre;
                    
                    if (target != null) target.setText(names[which]);
                })
                .show();
    }

    private void uploadFile(Uri uri, String type) {
        try {
            InputStream is = getContentResolver().openInputStream(uri);
            File file = new File(getCacheDir(), "upload_" + System.currentTimeMillis() + (type.equals("audio") ? ".mp3" : ".jpg"));
            FileOutputStream fos = new FileOutputStream(file);
            byte[] buf = new byte[1024];
            int len;
            while ((len = is.read(buf)) > 0) fos.write(buf, 0, len);
            is.close();
            fos.close();

            RequestBody requestFile = RequestBody.create(MediaType.parse("multipart/form-data"), file);
            MultipartBody.Part body = MultipartBody.Part.createFormData("file", file.getName(), requestFile);

            Call<Map<String, Object>> call;
            if (type.equals("audio")) {
                progressAudio.setVisibility(View.VISIBLE);
                call = RetrofitClient.getApiService().uploadAdminSongFile(body);
            } else {
                progressCover.setVisibility(View.VISIBLE);
                call = RetrofitClient.getApiService().uploadAdminCoverFile(body);
            }

            call.enqueue(new Callback<Map<String, Object>>() {
                @Override
                public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                    if (type.equals("audio")) progressAudio.setVisibility(View.GONE);
                    else progressCover.setVisibility(View.GONE);

                    if (response.isSuccessful() && response.body() != null) {
                         Map<String, Object> data = (Map<String, Object>) response.body().get("data");
                         if (data == null) data = response.body(); // sometimes directly returning object
                         
                        if (type.equals("audio")) {
                            formData.put("file_url", data.get("url"));
                            formData.put("duration", data.get("duration"));
                            txtAudioStatus.setText("Upload thành công!");
                            iconAudio.setImageResource(android.R.drawable.checkbox_on_background);
                        } else {
                            String url = (String) data.get("file_url");
                            if (url == null) url = (String) data.get("url");
                            formData.put("cover_url", url);
                            Glide.with(AdminEditSongActivity.this).load(url).transform(new RoundedCorners(24)).into(imgCover);
                        }
                    } else {
                         Toast.makeText(AdminEditSongActivity.this, "Upload thất bại", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                    if (type.equals("audio")) progressAudio.setVisibility(View.GONE);
                    else progressCover.setVisibility(View.GONE);
                    Toast.makeText(AdminEditSongActivity.this, "Lỗi upload: " + t.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Lỗi file", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveSong() {
        // Collect manual inputs
        formData.put("title", edtTitle.getText().toString().trim());
        formData.put("release_date", edtReleaseDate.getText().toString().trim());
        formData.put("lyrics", edtLyrics.getText().toString().trim());
        if (switchPremium.isChecked()) {
            try {
                formData.put("price", Double.parseDouble(edtPrice.getText().toString().trim()));
            } catch (Exception e) {
                formData.put("price", 0);
            }
        }
        
        // Validation
        if (formData.get("title").toString().isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập tên bài hát", Toast.LENGTH_SHORT).show();
            return;
        }
        if (formData.get("file_url") == null && songId == null) {
            Toast.makeText(this, "Vui lòng upload file nhạc", Toast.LENGTH_SHORT).show();
            return;
        }

        Call<Map<String, Object>> call;
        if (songId != null) {
            call = RetrofitClient.getApiService().updateSongAdmin(songId, formData);
        } else {
            call = RetrofitClient.getApiService().createSongAdmin(formData);
        }

        call.enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(AdminEditSongActivity.this, "Lưu thành công", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(AdminEditSongActivity.this, "Lỗi lưu dữ liệu code: " + response.code(), Toast.LENGTH_SHORT).show();
                    try {
                        Log.e("AdminEdit", "Error: " + response.errorBody().string());
                    } catch(Exception e){}
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Toast.makeText(AdminEditSongActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
