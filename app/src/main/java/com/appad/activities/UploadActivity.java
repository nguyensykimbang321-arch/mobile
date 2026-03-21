package com.appad.activities;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.appad.R;
import com.appad.utils.RetrofitClient;
import com.appad.utils.SessionManager;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class UploadActivity extends AppCompatActivity {

    private static final int PICK_AUDIO_REQUEST = 1;
    private static final int PICK_IMAGE_REQUEST = 2;

    private EditText edtTitle, edtPrice;
    private TextView txtAudioPath;
    private ImageView imgCoverPreview;
    private Button btnUpload;
    private android.widget.Spinner spinnerAlbum;
    private List<Map<String, Object>> albumList = new ArrayList<>();

    private Uri audioUri;
    private Uri imageUri;

    private android.widget.Spinner spinnerGenre;
    private List<Map<String, Object>> genreList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_upload);

        edtTitle = findViewById(R.id.edtSongTitle);
        edtPrice = findViewById(R.id.edtPrice);
        txtAudioPath = findViewById(R.id.txtAudioPath);
        imgCoverPreview = findViewById(R.id.imgCoverPreview);
        btnUpload = findViewById(R.id.btnUpload);
        spinnerGenre = findViewById(R.id.spinnerGenre);
        spinnerAlbum = findViewById(R.id.spinnerAlbum);

        findViewById(R.id.btnPickAudio).setOnClickListener(v -> pickAudio());
        findViewById(R.id.btnPickCover).setOnClickListener(v -> pickImage());

        btnUpload.setOnClickListener(v -> uploadSong());
        
        loadGenres();
        loadArtistData();
    }

    private void loadArtistData() {
        Integer userId = SessionManager.getInstance(this).getUserId();
        if (userId == null) return;

        RetrofitClient.getApiService().getArtistByUserId(userId).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Object> data = (Map<String, Object>) response.body().get("data");
                    if (data != null) {
                        Integer artistId = ((Number) data.get("artist_id")).intValue();
                        loadAlbums(artistId);
                    }
                }
            }
            @Override public void onFailure(Call<Map<String, Object>> call, Throwable t) {}
        });
    }

    private void loadAlbums(Integer artistId) {
        RetrofitClient.getApiService().getArtistAlbums(artistId).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Object data = response.body().get("data");
                    if (data instanceof List) {
                        albumList = (List<Map<String, Object>>) data;
                        List<String> names = new ArrayList<>();
                        names.add("-- Chọn Album (Không bắt buộc) --");
                        for (Map<String, Object> a : albumList) names.add((String) a.get("title"));
                        
                        android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(
                            UploadActivity.this, android.R.layout.simple_spinner_item, names);
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        spinnerAlbum.setAdapter(adapter);
                    }
                }
            }
            @Override public void onFailure(Call<Map<String, Object>> call, Throwable t) {}
        });
    }

    private void loadGenres() {
        RetrofitClient.getApiService().getGenres().enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Object data = response.body().get("data");
                    if (data instanceof List) {
                        genreList = (List<Map<String, Object>>) data;
                        List<String> genreNames = new ArrayList<>();
                        for (Map<String, Object> g : genreList) genreNames.add((String) g.get("name"));
                        
                        android.widget.ArrayAdapter<String> adapter = new android.widget.ArrayAdapter<>(
                            UploadActivity.this, android.R.layout.simple_spinner_item, genreNames);
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        spinnerGenre.setAdapter(adapter);
                    }
                }
            }
            @Override public void onFailure(Call<Map<String, Object>> call, Throwable t) {}
        });
    }

    private void pickAudio() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("audio/*");
        startActivityForResult(intent, PICK_AUDIO_REQUEST);
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
            if (requestCode == PICK_AUDIO_REQUEST) {
                audioUri = data.getData();
                String fileName = audioUri.getLastPathSegment();
                try {
                    android.media.MediaMetadataRetriever retriever = new android.media.MediaMetadataRetriever();
                    retriever.setDataSource(this, audioUri);
                    String time = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION);
                    long timeMs = Long.parseLong(time);
                    int totalSeconds = (int) (timeMs / 1000);
                    int minutes = totalSeconds / 60;
                    int seconds = totalSeconds % 60;
                    String durationStr = String.format(java.util.Locale.getDefault(), "%02d:%02d", minutes, seconds);
                    txtAudioPath.setText(fileName + " (" + durationStr + ")");
                    retriever.release();
                } catch (Exception e) {
                    txtAudioPath.setText(fileName);
                }
            } else if (requestCode == PICK_IMAGE_REQUEST) {
                imageUri = data.getData();
                imgCoverPreview.setImageURI(imageUri);
            }
        }
    }

    private void uploadSong() {
        String title = edtTitle.getText().toString();
        String priceStr = edtPrice.getText().toString();
        
        if (title.isEmpty() || audioUri == null || imageUri == null || priceStr.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập đủ thông tin", Toast.LENGTH_SHORT).show();
            return;
        }

        Double price = Double.parseDouble(priceStr);
        Integer isPremium = price > 0 ? 1 : 0;
        Integer userId = SessionManager.getInstance(this).getUserId();
        
        if (userId == null) return;

        Toast.makeText(this, "Đang tải lên...", Toast.LENGTH_SHORT).show();

        // Need artistId for upload
        RetrofitClient.getApiService().getArtistByUserId(userId).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Object> data = (Map<String, Object>) response.body().get("data");
                    if (data != null) {
                        Integer artistId = ((Number) data.get("artist_id")).intValue();
                        performRealUpload(artistId, title, price, isPremium);
                    }
                }
            }
            @Override public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Toast.makeText(UploadActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void performRealUpload(Integer artistId, String title, Double price, Integer isPremium) {
        File audioFile = getFileFromUri(audioUri, "audio.mp3");
        File imageFile = getFileFromUri(imageUri, "cover.jpg");

        if (audioFile == null || imageFile == null) return;

        // Parts for files
        okhttp3.RequestBody requestAudio = okhttp3.RequestBody.create(okhttp3.MediaType.parse("audio/*"), audioFile);
        okhttp3.MultipartBody.Part audioPart = okhttp3.MultipartBody.Part.createFormData("musicFile", audioFile.getName(), requestAudio);

        okhttp3.RequestBody requestImage = okhttp3.RequestBody.create(okhttp3.MediaType.parse("image/*"), imageFile);
        okhttp3.MultipartBody.Part imagePart = okhttp3.MultipartBody.Part.createFormData("coverFile", imageFile.getName(), requestImage);

        // Body for strings/numbers
        okhttp3.RequestBody titleBody = okhttp3.RequestBody.create(okhttp3.MediaType.parse("text/plain"), title);
        okhttp3.RequestBody artistIdBody = okhttp3.RequestBody.create(okhttp3.MediaType.parse("text/plain"), String.valueOf(artistId));
        okhttp3.RequestBody genreIdBody = okhttp3.RequestBody.create(okhttp3.MediaType.parse("text/plain"), String.valueOf(getSelectedGenreId()));
        okhttp3.RequestBody priceBody = okhttp3.RequestBody.create(okhttp3.MediaType.parse("text/plain"), String.valueOf(price));
        okhttp3.RequestBody isPremiumBody = okhttp3.RequestBody.create(okhttp3.MediaType.parse("text/plain"), String.valueOf(isPremium));
        
        Integer albumId = getSelectedAlbumId();
        okhttp3.RequestBody albumIdPart = albumId != null ? okhttp3.RequestBody.create(okhttp3.MediaType.parse("text/plain"), String.valueOf(albumId)) : null;
        
        okhttp3.RequestBody lyricsPart = okhttp3.RequestBody.create(okhttp3.MediaType.parse("text/plain"), "");
        okhttp3.RequestBody statusPart = okhttp3.RequestBody.create(okhttp3.MediaType.parse("text/plain"), "1");
        okhttp3.RequestBody releaseDatePart = okhttp3.RequestBody.create(okhttp3.MediaType.parse("text/plain"), new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm").format(new java.util.Date()));

        RetrofitClient.getApiService().uploadSong(
                titleBody, artistIdBody, genreIdBody, albumIdPart, priceBody, isPremiumBody, lyricsPart, statusPart, releaseDatePart, audioPart, imagePart
        ).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(UploadActivity.this, "Tải lên thành công!", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(UploadActivity.this, "Tải lên thất bại", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Toast.makeText(UploadActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private Integer getSelectedGenreId() {
        int pos = spinnerGenre.getSelectedItemPosition();
        if (pos >= 0 && pos < genreList.size()) {
            Object id = genreList.get(pos).get("genre_id");
            if (id == null) id = genreList.get(pos).get("genreId");
            return id != null ? ((Number) id).intValue() : 1;
        }
        return 1;
    }

    private Integer getSelectedAlbumId() {
        int pos = spinnerAlbum.getSelectedItemPosition();
        if (pos > 0 && pos <= albumList.size()) {
            Object id = albumList.get(pos - 1).get("album_id");
            if (id == null) id = albumList.get(pos - 1).get("albumId");
            return id != null ? ((Number) id).intValue() : null;
        }
        return null;
    }

    private File getFileFromUri(Uri uri, String name) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            File tempFile = new File(getCacheDir(), "upload_" + System.currentTimeMillis() + "_" + name);
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
}
