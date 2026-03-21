package com.appad.activities;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.appad.R;
import com.appad.utils.RetrofitClient;
import com.appad.utils.SessionManager;
import com.bumptech.glide.Glide;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
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

public class EditSongActivity extends AppCompatActivity {
    private static final int PICK_IMAGE_REQUEST = 1;
    private static final int PICK_AUDIO_REQUEST = 2;

    private Integer songId; 
    private EditText edtTitle, edtPrice, edtLyrics, edtReleaseDate;
    private ImageView imgCover;
    private Spinner spinnerGenre, spinnerAlbum;
    private TextView txtAudioFileName;
    private com.google.android.material.button.MaterialButton btnUpdateSong;
    private com.google.android.material.switchmaterial.SwitchMaterial switchStatus;
    
    private Uri newImageUri, newAudioUri;
    private List<Map<String, Object>> genres = new ArrayList<>();
    private List<Map<String, Object>> albums = new ArrayList<>();

    private android.media.MediaPlayer mediaPlayer;
    private com.google.android.material.button.MaterialButton btnPlayAudio;
    private String currentAudioUrl;
    private boolean isPlaying = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_song);

        if (getIntent().hasExtra("SONG_ID")) {
            songId = getIntent().getIntExtra("SONG_ID", -1);
            if (songId == -1) songId = null;
        }

        setupUI();
        loadMetadata(); 
    }

    private void loadMetadata() {
        // Load Genres
        RetrofitClient.getApiService().getGenres().enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Map<String, Object>> data = (List<Map<String, Object>>) response.body().get("data");
                    if (data != null) {
                        genres.clear();
                        genres.addAll(data);
                        List<String> genreNames = new ArrayList<>();
                        for (Map<String, Object> g : genres) genreNames.add((String) g.get("name"));
                        
                        ArrayAdapter<String> adapter = new ArrayAdapter<>(EditSongActivity.this, android.R.layout.simple_spinner_item, genreNames);
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        spinnerGenre.setAdapter(adapter);
                        
                        if (songId != null) loadSongData();
                    }
                }
            }
            @Override public void onFailure(Call<Map<String, Object>> call, Throwable t) {}
        });

        // Load Artist then their Albums
        Integer userId = SessionManager.getInstance(this).getUserId();
        RetrofitClient.getApiService().getArtistByUserId(userId).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Object> artistData = (Map<String, Object>) response.body().get("data");
                    if (artistData != null) {
                        Integer artistId = ((Number) artistData.get("artist_id")).intValue();
                        loadAlbumsForArtist(artistId);
                    }
                }
            }
            @Override public void onFailure(Call<Map<String, Object>> call, Throwable t) {}
        });
    }

    private void loadAlbumsForArtist(Integer artistId) {
        RetrofitClient.getApiService().getArtistAlbums(artistId).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Map<String, Object>> data = (List<Map<String, Object>>) response.body().get("data");
                    if (data != null) {
                        albums.clear();
                        albums.addAll(data);
                        List<String> albumNames = new ArrayList<>();
                        albumNames.add("-- Không chọn Album --");
                        for (Map<String, Object> a : albums) albumNames.add((String) a.get("title"));
                        
                        ArrayAdapter<String> adapter = new ArrayAdapter<>(EditSongActivity.this, android.R.layout.simple_spinner_item, albumNames);
                        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
                        spinnerAlbum.setAdapter(adapter);
                        
                        if (songId != null) loadSongData();
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

    private void pickAudio() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("audio/*");
        startActivityForResult(intent, PICK_AUDIO_REQUEST);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK && data != null && data.getData() != null) {
            if (requestCode == PICK_IMAGE_REQUEST) {
                newImageUri = data.getData();
                imgCover.setImageURI(newImageUri);
            } else if (requestCode == PICK_AUDIO_REQUEST) {
                newAudioUri = data.getData();
                String fileName = newAudioUri.getLastPathSegment();
                
                // Extract duration locally
                try {
                    android.media.MediaMetadataRetriever retriever = new android.media.MediaMetadataRetriever();
                    retriever.setDataSource(this, newAudioUri);
                    String time = retriever.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION);
                    long timeMs = Long.parseLong(time);
                    int totalSeconds = (int) (timeMs / 1000);
                    int minutes = totalSeconds / 60;
                    int seconds = totalSeconds % 60;
                    String durationStr = String.format(java.util.Locale.getDefault(), "%02d:%02d", minutes, seconds);
                    txtAudioFileName.setText(fileName + " (" + durationStr + ")");
                    retriever.release();
                } catch (Exception e) {
                    txtAudioFileName.setText(fileName);
                }
            }
        }
    }

    private void setupUI() {
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbarEditSong);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle(songId == null ? "Thêm bài hát mới" : "Sửa bài hát");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish());

        edtTitle = findViewById(R.id.edtEditSongTitle);
        edtPrice = findViewById(R.id.edtEditSongPrice);
        edtLyrics = findViewById(R.id.edtLyrics);
        edtReleaseDate = findViewById(R.id.edtReleaseDate);
        imgCover = findViewById(R.id.imgEditSongCover);
        switchStatus = findViewById(R.id.switchStatus);
        spinnerGenre = findViewById(R.id.spinnerGenre);
        spinnerAlbum = findViewById(R.id.spinnerAlbum);
        txtAudioFileName = findViewById(R.id.txtAudioFileName);
        btnPlayAudio = findViewById(R.id.btnPlayAudio);
        btnUpdateSong = findViewById(R.id.btnUpdateSong);

        findViewById(R.id.btnChangeCover).setOnClickListener(v -> pickImage());
        findViewById(R.id.btnPickAudio).setOnClickListener(v -> pickAudio());
        btnUpdateSong.setOnClickListener(v -> submitForm());
        btnPlayAudio.setOnClickListener(v -> togglePlayback());

        // Default release date to now
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm", java.util.Locale.getDefault());
        edtReleaseDate.setText(sdf.format(new java.util.Date()));
        switchStatus.setChecked(true); // Default active

        if (songId == null) {
            btnUpdateSong.setText("TIẾN HÀNH UPLOAD");
        } else {
            btnUpdateSong.setText("LƯU THAY ĐỔI");
        }
    }

    private void loadSongData() {
        RetrofitClient.getApiService().getSongById(songId).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Object> data = (Map<String, Object>) response.body().get("data");
                    if (data != null) {
                        edtTitle.setText((String) data.get("title"));
                        
                        Object price = data.get("price");
                        if (price != null) edtPrice.setText(String.valueOf(((Number)price).intValue()));
                        
                        Object status = data.get("status");
                        switchStatus.setChecked(status == null || ((Number)status).intValue() == 1);

                        Object rDate = data.get("release_date") != null ? data.get("release_date") : data.get("releaseDate");
                        if (rDate != null) {
                            String d = (String) rDate;
                            if(d.contains("T")) d = d.replace("T", " ");
                            if(d.length() > 16) d = d.substring(0, 16);
                            edtReleaseDate.setText(d);
                        }

                        String coverUrl = (String) (data.get("cover_url") != null ? data.get("cover_url") : data.get("coverUrl"));
                        Glide.with(EditSongActivity.this).load(com.appad.utils.ImageUrlUtils.fixUrl(coverUrl)).into(imgCover);
                        
                        if (data.containsKey("lyrics") && data.get("lyrics") != null) {
                             edtLyrics.setText((String) data.get("lyrics"));
                        }

                        currentAudioUrl = (String) (data.get("file_url") != null ? data.get("file_url") : data.get("fileUrl"));
                        if (currentAudioUrl != null) {
                            btnPlayAudio.setVisibility(View.VISIBLE);
                        }
                        
                            Object gid = data.get("genre_id") != null ? data.get("genre_id") : data.get("genreId");
                            if (gid != null) {
                                int genreId = ((Number)gid).intValue();
                                for(int i=0; i<genres.size(); i++) {
                                    Object gIdObj = genres.get(i).get("genre_id") != null ? genres.get(i).get("genre_id") : genres.get(i).get("genreId");
                                    if (gIdObj != null && ((Number)gIdObj).intValue() == genreId) {
                                        spinnerGenre.setSelection(i); 
                                        break;
                                    }
                                }
                            }

                            Object aid = data.get("album_id") != null ? data.get("album_id") : data.get("albumId");
                            if (aid != null) {
                                int albumId = ((Number)aid).intValue();
                                for(int i=0; i<albums.size(); i++) {
                                    Object aIdObj = albums.get(i).get("album_id") != null ? albums.get(i).get("album_id") : albums.get(i).get("albumId");
                                    if (aIdObj != null && ((Number)aIdObj).intValue() == albumId) {
                                        spinnerAlbum.setSelection(i + 1);
                                        break;
                                    }
                                }
                            }
                    }
                }
            }
            @Override public void onFailure(Call<Map<String, Object>> call, Throwable t) {}
        });
    }

    private void submitForm() {
        String title = edtTitle.getText().toString().trim();
        String priceStr = edtPrice.getText().toString().trim();
        Double price = priceStr.isEmpty() ? 0.0 : Double.parseDouble(priceStr);
        Integer isPremium = price > 0 ? 1 : 0;
        String lyrics = edtLyrics.getText().toString();
        Integer status = switchStatus.isChecked() ? 1 : 0;
        String releaseDate = edtReleaseDate.getText().toString().trim();
        
        Integer genreId = null;
        if (spinnerGenre.getSelectedItemPosition() >= 0 && !genres.isEmpty()) {
            Object gIdObj = genres.get(spinnerGenre.getSelectedItemPosition()).get("genre_id");
            if (gIdObj == null) gIdObj = genres.get(spinnerGenre.getSelectedItemPosition()).get("genreId");
            if (gIdObj != null) genreId = ((Number)gIdObj).intValue();
        }
        
        Integer albumId = null;
        if (spinnerAlbum.getSelectedItemPosition() > 0 && !albums.isEmpty()) { 
            Object aIdObj = albums.get(spinnerAlbum.getSelectedItemPosition()-1).get("album_id");
            if (aIdObj == null) aIdObj = albums.get(spinnerAlbum.getSelectedItemPosition()-1).get("albumId");
            if (aIdObj != null) albumId = ((Number)aIdObj).intValue();
        }

        if (title.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập tên bài hát", Toast.LENGTH_SHORT).show(); return;
        }

        if (songId == null) {
             if (newAudioUri == null || newImageUri == null) {
                 Toast.makeText(this, "Vui lòng chọn file nhạc và ảnh bìa", Toast.LENGTH_SHORT).show(); return;
             }
             uploadNewSong(title, price, isPremium, lyrics, genreId, albumId, status, releaseDate);
        } else {
             updateExistingSong(title, price, isPremium, lyrics, genreId, albumId, status, releaseDate);
        }
    }

    private void uploadNewSong(String title, Double price, Integer isPremium, String lyrics, Integer genreId, Integer albumId, Integer status, String releaseDate) {
        Integer userId = SessionManager.getInstance(this).getUserId();
        RetrofitClient.getApiService().getArtistByUserId(userId).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Object> data = (Map<String, Object>) response.body().get("data");
                    Object aid = data.get("artist_id");
                    if (aid == null) aid = data.get("artistId");
                    
                    if (aid == null) {
                        Toast.makeText(EditSongActivity.this, "Không tìm thấy thông tin Artist", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    Integer artistId = ((Number) aid).intValue();

                    File musicFile = getFileFromUri(newAudioUri, "music.mp3");
                    File coverFile = getFileFromUri(newImageUri, "cover.jpg");
                    
                    if (musicFile == null || coverFile == null) return;

                    RequestBody reqTitle = RequestBody.create(MediaType.parse("text/plain"), title);
                    RequestBody reqArtistId = RequestBody.create(MediaType.parse("text/plain"), String.valueOf(artistId));
                    RequestBody reqGenreId = RequestBody.create(MediaType.parse("text/plain"), String.valueOf(genreId));
                    RequestBody reqAlbumId = albumId != null ? RequestBody.create(MediaType.parse("text/plain"), String.valueOf(albumId)) : null;
                    
                     RequestBody reqPrice = RequestBody.create(MediaType.parse("text/plain"), String.valueOf(price));
                     RequestBody reqPremium = RequestBody.create(MediaType.parse("text/plain"), String.valueOf(isPremium));
                     RequestBody reqLyrics = RequestBody.create(MediaType.parse("text/plain"), lyrics != null ? lyrics : "");
                     RequestBody reqStatus = RequestBody.create(MediaType.parse("text/plain"), String.valueOf(status));
                     RequestBody reqReleaseDate = RequestBody.create(MediaType.parse("text/plain"), releaseDate);
                     
                     MultipartBody.Part mpMusic = MultipartBody.Part.createFormData("musicFile", musicFile.getName(), RequestBody.create(MediaType.parse("audio/*"), musicFile));
                     MultipartBody.Part mpCover = MultipartBody.Part.createFormData("coverFile", coverFile.getName(), RequestBody.create(MediaType.parse("image/*"), coverFile));

                     RetrofitClient.getApiService().uploadSong(
                             reqTitle, reqArtistId, reqGenreId, reqAlbumId, reqPrice, reqPremium, reqLyrics, reqStatus, reqReleaseDate, mpMusic, mpCover
                     ).enqueue(new Callback<Map<String, Object>>() {
                         @Override
                         public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                             if(response.isSuccessful()) {
                                 Toast.makeText(EditSongActivity.this, "Upload thành công!", Toast.LENGTH_SHORT).show();
                                 finish();
                             } else {
                                 Toast.makeText(EditSongActivity.this, "Upload thất bại: " + response.code(), Toast.LENGTH_SHORT).show();
                             }
                         }
                         @Override public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                             Toast.makeText(EditSongActivity.this, "Lỗi kết nối upload", Toast.LENGTH_SHORT).show();
                         }
                     });
                }
            }
            @Override public void onFailure(Call<Map<String, Object>> call, Throwable t) {}
        });
    }

    private void updateExistingSong(String title, Double price, Integer isPremium, String lyrics, Integer genreId, Integer albumId, Integer status, String releaseDate) {
        MultipartBody.Part imagePart = null;
        if (newImageUri != null) {
            File file = getFileFromUri(newImageUri, "cover.jpg");
            if (file != null) {
                RequestBody reqFile = RequestBody.create(MediaType.parse("image/*"), file);
                imagePart = MultipartBody.Part.createFormData("coverFile", file.getName(), reqFile);
            }
        }

        MultipartBody.Part musicPart = null;
        if (newAudioUri != null) {
            File file = getFileFromUri(newAudioUri, "music.mp3");
            if (file != null) {
                RequestBody reqFile = RequestBody.create(MediaType.parse("audio/*"), file);
                musicPart = MultipartBody.Part.createFormData("musicFile", file.getName(), reqFile);
            }
        }

        // Use RequestBody for all parts to avoid JSON quotes issue
        RequestBody reqTitle = RequestBody.create(MediaType.parse("text/plain"), title);
        RequestBody reqPrice = RequestBody.create(MediaType.parse("text/plain"), String.valueOf(price));
        RequestBody reqPremium = RequestBody.create(MediaType.parse("text/plain"), String.valueOf(isPremium));
        RequestBody reqLyrics = RequestBody.create(MediaType.parse("text/plain"), lyrics != null ? lyrics : "");
        RequestBody reqGenreId = RequestBody.create(MediaType.parse("text/plain"), String.valueOf(genreId));
        RequestBody reqAlbumId = albumId != null ? RequestBody.create(MediaType.parse("text/plain"), String.valueOf(albumId)) : null;
        RequestBody reqStatus = RequestBody.create(MediaType.parse("text/plain"), String.valueOf(status));
        RequestBody reqReleaseDate = RequestBody.create(MediaType.parse("text/plain"), releaseDate);
        
        RetrofitClient.getApiService().updateSong(
                songId, reqTitle, reqPrice, reqPremium, reqLyrics, reqGenreId, reqAlbumId, reqStatus, reqReleaseDate, imagePart, musicPart
        ).enqueue(new Callback<Map<String, Object>>() {
             @Override
             public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                 if (response.isSuccessful()) {
                     Toast.makeText(EditSongActivity.this, "Cập nhật thành công!", Toast.LENGTH_SHORT).show();
                     finish();
                 } else {
                     Toast.makeText(EditSongActivity.this, "Lỗi cập nhật: " + response.code(), Toast.LENGTH_SHORT).show();
                 }
             }
             @Override public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                 Toast.makeText(EditSongActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
             }
        });
    }

    private void togglePlayback() {
        if (isPlaying) {
            if (mediaPlayer != null) {
                mediaPlayer.pause();
                isPlaying = false;
                btnPlayAudio.setIconResource(android.R.drawable.ic_media_play);
            }
        } else {
            if (mediaPlayer == null) {
                mediaPlayer = new android.media.MediaPlayer();
                try {
                    String url = com.appad.utils.ImageUrlUtils.fixUrl(currentAudioUrl);
                    mediaPlayer.setDataSource(url);
                    mediaPlayer.prepareAsync();
                    mediaPlayer.setOnPreparedListener(mp -> {
                        mp.start();
                        isPlaying = true;
                        btnPlayAudio.setIconResource(android.R.drawable.ic_media_pause);
                    });
                    mediaPlayer.setOnCompletionListener(mp -> {
                        isPlaying = false;
                        btnPlayAudio.setIconResource(android.R.drawable.ic_media_play);
                    });
                } catch (Exception e) {
                    Toast.makeText(this, "Lỗi phát nhạc", Toast.LENGTH_SHORT).show();
                }
            } else {
                mediaPlayer.start();
                isPlaying = true;
                btnPlayAudio.setIconResource(android.R.drawable.ic_media_pause);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    private File getFileFromUri(Uri uri, String filename) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(uri);
            File tempFile = new File(getCacheDir(), filename + "_" + System.currentTimeMillis());
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
