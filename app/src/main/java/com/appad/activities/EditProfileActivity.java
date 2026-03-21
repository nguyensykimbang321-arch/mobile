package com.appad.activities;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import com.appad.R;
import com.appad.utils.ImageUrlUtils;
import com.appad.utils.RetrofitClient;
import com.bumptech.glide.Glide;
import com.google.android.material.textfield.TextInputEditText;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EditProfileActivity extends AppCompatActivity {

    private ImageButton btnBack;
    private ImageView imgAvatar, btnChangeAvatar;
    private TextInputEditText edtFullName, edtUsername, edtEmail;
    private Button btnSave, btnCancel;
    private Uri selectedImageUri;
    private String currentAvatarUrl;
    private com.appad.utils.MiniPlayerHelper miniPlayerHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        initViews();
        setupListeners();
        loadCurrentProfile();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        imgAvatar = findViewById(R.id.imgEditAvatar);
        btnChangeAvatar = findViewById(R.id.btnChangeAvatar);
        edtFullName = findViewById(R.id.edtFullName);
        edtUsername = findViewById(R.id.edtUsername);
        edtEmail = findViewById(R.id.edtEmail);
        btnSave = findViewById(R.id.btnSaveProfile);
        btnCancel = findViewById(R.id.btnCancel);
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());
        btnCancel.setOnClickListener(v -> finish());
        
        btnChangeAvatar.setOnClickListener(v -> pickImage());
        imgAvatar.setOnClickListener(v -> pickImage());
        
        btnSave.setOnClickListener(v -> updateProfile());
    }

    private void loadCurrentProfile() {
        RetrofitClient.getApiService().getProfile().enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Object> data = (Map<String, Object>) response.body().get("data");
                    if (data != null) {
                        String fullName = (String) (data.get("full_name") != null ? data.get("full_name") : data.get("fullName"));
                        String username = (String) data.get("username");
                        String email = (String) data.get("email");
                        currentAvatarUrl = (String) (data.get("avatar_url") != null ? data.get("avatar_url") : data.get("avatarUrl"));

                        if (fullName != null) edtFullName.setText(fullName);
                        if (username != null) edtUsername.setText(username);
                        if (email != null) edtEmail.setText(email);

                        if (currentAvatarUrl != null && !currentAvatarUrl.isEmpty()) {
                            Glide.with(EditProfileActivity.this)
                                .load(ImageUrlUtils.fixUrl(currentAvatarUrl))
                                .circleCrop()
                                .placeholder(R.drawable.ic_launcher_background)
                                .into(imgAvatar);
                        }
                    }
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Toast.makeText(EditProfileActivity.this, "Không thể tải thông tin", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void pickImage() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(intent, 100);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == Activity.RESULT_OK && data != null) {
            selectedImageUri = data.getData();
            Glide.with(this).load(selectedImageUri).circleCrop().into(imgAvatar);
        }
    }

    private void updateProfile() {
        String fullName = edtFullName.getText() != null ? edtFullName.getText().toString().trim() : "";
        String username = edtUsername.getText() != null ? edtUsername.getText().toString().trim() : "";

        if (username.isEmpty()) {
            Toast.makeText(this, "Tên người dùng không được để trống", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSave.setEnabled(false);
        btnSave.setText("Đang lưu...");

        // If image was selected, upload it first then update profile or vice versa
        if (selectedImageUri != null) {
            uploadAvatarAndFinish(fullName, username);
        } else {
            updateProfileOnly(fullName, username);
        }
    }

    private void updateProfileOnly(String fullName, String username) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("full_name", fullName);
        payload.put("username", username);

        RetrofitClient.getApiService().updateProfile(payload).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                btnSave.setEnabled(true);
                btnSave.setText("Lưu thay đổi");
                
                if (response.isSuccessful()) {
                    Toast.makeText(EditProfileActivity.this, "Cập nhật thành công!", Toast.LENGTH_SHORT).show();
                    setResult(RESULT_OK);
                    finish();
                } else {
                    Toast.makeText(EditProfileActivity.this, "Cập nhật thất bại: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                btnSave.setEnabled(true);
                btnSave.setText("Lưu thay đổi");
                Toast.makeText(EditProfileActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void uploadAvatarAndFinish(String fullName, String username) {
        File file = getFileFromUri(selectedImageUri, "avatar.jpg");
        if (file == null) {
            updateProfileOnly(fullName, username);
            return;
        }

        RequestBody requestFile = RequestBody.create(MediaType.parse("image/*"), file);
        MultipartBody.Part body = MultipartBody.Part.createFormData("avatar", file.getName(), requestFile);

        RetrofitClient.getApiService().uploadAvatar(body).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful()) {
                    // After avatar success, update text fields
                    updateProfileOnly(fullName, username);
                } else {
                    btnSave.setEnabled(true);
                    btnSave.setText("Lưu thay đổi");
                    Toast.makeText(EditProfileActivity.this, "Lỗi upload ảnh: " + response.code(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                btnSave.setEnabled(true);
                btnSave.setText("Lưu thay đổi");
                Toast.makeText(EditProfileActivity.this, "Lỗi kết nối upload ảnh", Toast.LENGTH_SHORT).show();
            }
        });
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
