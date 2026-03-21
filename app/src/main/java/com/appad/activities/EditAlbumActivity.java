package com.appad.activities;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.view.ViewGroup;
import com.appad.R;
import com.appad.utils.RetrofitClient;
import com.bumptech.glide.Glide;
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

public class EditAlbumActivity extends AppCompatActivity {
    private static final int PICK_IMAGE_REQUEST = 1;

    private EditText edtTitle, edtPrice, edtReleaseDate;
    private ImageView imgCover;
    private CheckBox chkPremium;
    private RecyclerView rvSongs;
    private com.google.android.material.button.MaterialButton btnAction;
    
    private Integer albumId; // Null if creating
    private Uri newImageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_album);

        albumId = getIntent().getIntExtra("ALBUM_ID", -1);
        if (albumId == -1) albumId = null;

        setupUI();
        if (albumId != null) {
            loadAlbumData();
            loadAlbumSongs();
        } else {
             // Create mode defaults
             java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault());
             edtReleaseDate.setText(sdf.format(new java.util.Date()));
        }
    }

    private void setupUI() {
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbarEditAlbum);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(albumId == null ? "Tạo Album Mới" : "Sửa Album");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish());

        edtTitle = findViewById(R.id.edtEditAlbumTitle);
        edtPrice = findViewById(R.id.edtEditAlbumPrice);
        edtReleaseDate = findViewById(R.id.edtReleaseDate);
        imgCover = findViewById(R.id.imgEditAlbumCover);
        chkPremium = findViewById(R.id.chkEditAlbumPremium);
        rvSongs = findViewById(R.id.rvAlbumSongs);
        rvSongs.setLayoutManager(new LinearLayoutManager(this));
        
        btnAction = findViewById(R.id.btnUpdateAlbum);
        btnAction.setText(albumId == null ? "TẠO ALBUM" : "LƯU THAY ĐỔI");

        findViewById(R.id.btnChangeAlbumCover).setOnClickListener(v -> pickImage());
        btnAction.setOnClickListener(v -> submitForm());
    }

    private void loadAlbumData() {
        RetrofitClient.getApiService().getAlbumById(albumId).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Object> data = (Map<String, Object>) response.body().get("data");
                    if (data != null) {
                        edtTitle.setText((String) data.get("title"));
                        
                        Object price = data.get("price");
                        if (price != null) edtPrice.setText(String.valueOf(((Number)price).intValue()));
                        
                        Object isPre = data.get("is_premium") != null ? data.get("is_premium") : data.get("isPremium");
                        chkPremium.setChecked((isPre instanceof Number && ((Number)isPre).intValue() == 1) || Boolean.TRUE.equals(isPre));
                        
                        Object rDate = data.get("release_date") != null ? data.get("release_date") : data.get("releaseDate");
                        if (rDate != null) {
                             String d = (String) rDate;
                             if(d.contains("T")) d = d.replace("T", " ");
                             if(d.length() > 16) d = d.substring(0, 16);
                             edtReleaseDate.setText(d);
                        }

                        String coverUrl = (String) (data.get("cover_url") != null ? data.get("cover_url") : data.get("coverUrl"));
                        Glide.with(EditAlbumActivity.this).load(com.appad.utils.ImageUrlUtils.fixUrl(coverUrl)).into(imgCover);
                    }
                }
            }
            @Override public void onFailure(Call<Map<String, Object>> call, Throwable t) {}
        });
    }

    private void loadAlbumSongs() {
        RetrofitClient.getApiService().getSongsByAlbum(albumId).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Object data = response.body().get("data");
                    if (data instanceof List) {
                        String json = new com.google.gson.Gson().toJson(data);
                        java.lang.reflect.Type listType = new com.google.gson.reflect.TypeToken<List<com.appad.models.Song>>(){}.getType();
                        List<com.appad.models.Song> songs = new com.google.gson.Gson().fromJson(json, listType);
                        
                        rvSongs.setAdapter(new com.appad.adapters.SongAdapter(EditAlbumActivity.this, songs));
                    }
                }
            }
            @Override public void onFailure(Call<Map<String, Object>> call, Throwable t) {}
        });
    }

    private void pickImage() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(intent, PICK_IMAGE_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
            if (requestCode == PICK_IMAGE_REQUEST) {
                newImageUri = data.getData();
                imgCover.setImageURI(newImageUri);
            }
        }
    }

    private void submitForm() {
        String title = edtTitle.getText().toString().trim();
        String priceStr = edtPrice.getText().toString().trim();
        Integer isPremium = chkPremium.isChecked() ? 1 : 0;
        String releaseDate = edtReleaseDate.getText().toString().trim();

        if (title.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập tên Album", Toast.LENGTH_SHORT).show();
            return;
        }
        
        Double price = 0.0;
        if (!priceStr.isEmpty()) {
             try { price = Double.parseDouble(priceStr); } catch(Exception e) {}
        }

        if (albumId == null) {
            if (newImageUri == null) {
                Toast.makeText(this, "Vui lòng chọn ảnh bìa", Toast.LENGTH_SHORT).show();
                return;
            }
            createAlbum(title, price, isPremium, releaseDate);
        } else {
            updateAlbum(title, price, isPremium, releaseDate);
        }
    }

    private void createAlbum(String title, Double price, Integer isPremium, String releaseDate) {
        Integer userId = com.appad.utils.SessionManager.getInstance(this).getUserId();
        RetrofitClient.getApiService().getArtistByUserId(userId).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Object> data = (Map<String, Object>) response.body().get("data");
                    Integer artistId = ((Number) data.get("artistId")).intValue();

                    File coverFile = getFileFromUri(newImageUri);
                    if (coverFile == null) return;
                    
                    RequestBody reqTitle = RequestBody.create(MediaType.parse("text/plain"), title);
                    RequestBody reqArtistId = RequestBody.create(MediaType.parse("text/plain"), String.valueOf(artistId));
                    RequestBody reqPrice = RequestBody.create(MediaType.parse("text/plain"), String.valueOf(price));
                    RequestBody reqPremium = RequestBody.create(MediaType.parse("text/plain"), String.valueOf(isPremium));
                    RequestBody reqReleaseDate = RequestBody.create(MediaType.parse("text/plain"), releaseDate);
                    
                    MultipartBody.Part mpCover = MultipartBody.Part.createFormData("coverFile", coverFile.getName(), RequestBody.create(MediaType.parse("image/*"), coverFile));

                    RetrofitClient.getApiService().createAlbum(reqTitle, reqArtistId, reqPrice, reqPremium, reqReleaseDate, mpCover)
                        .enqueue(new Callback<Map<String, Object>>() {
                            @Override
                            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                                if (response.isSuccessful()) {
                                    Toast.makeText(EditAlbumActivity.this, "Tạo album thành công!", Toast.LENGTH_SHORT).show();
                                    finish();
                                } else {
                                    Toast.makeText(EditAlbumActivity.this, "Tạo thất bại: " + response.code(), Toast.LENGTH_SHORT).show();
                                }
                            }
                            @Override public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                                Toast.makeText(EditAlbumActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
                            }
                        });
                }
            }
            @Override public void onFailure(Call<Map<String, Object>> call, Throwable t) {}
        });
    }

    private void updateAlbum(String title, Double price, Integer isPremium, String releaseDate) {
        MultipartBody.Part imagePart = null;
        if (newImageUri != null) {
            File file = getFileFromUri(newImageUri);
            if (file != null) {
                RequestBody requestFile = RequestBody.create(MediaType.parse("image/*"), file);
                imagePart = MultipartBody.Part.createFormData("coverFile", file.getName(), requestFile);
            }
        }
        
        RetrofitClient.getApiService().updateAlbum(albumId, title, price, isPremium, releaseDate, imagePart)
            .enqueue(new Callback<Map<String, Object>>() {
                @Override
                public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                    if (response.isSuccessful()) {
                        Toast.makeText(EditAlbumActivity.this, "Cập nhật thành công!", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(EditAlbumActivity.this, "Cập nhật thất bại", Toast.LENGTH_SHORT).show();
                    }
                }
                @Override public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                    Toast.makeText(EditAlbumActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
                }
            });
    }

    private File getFileFromUri(Uri uri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            File tempFile = new File(getCacheDir(), "temp_album_" + System.currentTimeMillis() + ".jpg");
            FileOutputStream outputStream = new FileOutputStream(tempFile);
            byte[] buffer = new byte[1024];
            int read;
            while ((read = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, read);
            }
            outputStream.flush();
            outputStream.close();
            inputStream.close();
            return tempFile;
        } catch (Exception e) {
            return null;
        }
    }

    // Removed SimpleSongAdapter
}
