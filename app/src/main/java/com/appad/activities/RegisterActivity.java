package com.appad.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.appad.R;
import com.appad.utils.RetrofitClient;
import java.util.HashMap;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class RegisterActivity extends AppCompatActivity {

    private EditText editTextUsername, editTextEmail, editTextFullName, editTextPassword, editTextCountry, editTextBio, editTextArtistImage;
    private CheckBox checkBoxArtist;
    private LinearLayout layoutArtistFields;
    private Button buttonRegister;
    private TextView textViewLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        // Initialize views
        editTextUsername = findViewById(R.id.editTextUsername);
        editTextEmail = findViewById(R.id.editTextEmail);
        editTextFullName = findViewById(R.id.editTextFullName);
        editTextPassword = findViewById(R.id.editTextPassword);
        editTextCountry = findViewById(R.id.editTextCountry);
        editTextBio = findViewById(R.id.editTextBio);
        editTextArtistImage = findViewById(R.id.editTextArtistImage);
        checkBoxArtist = findViewById(R.id.checkBoxArtist);
        layoutArtistFields = findViewById(R.id.layoutArtistFields);
        buttonRegister = findViewById(R.id.buttonRegister);
        textViewLogin = findViewById(R.id.textViewLogin);

        // Artist toggle logic
        checkBoxArtist.setOnCheckedChangeListener((buttonView, isChecked) -> {
            layoutArtistFields.setVisibility(isChecked ? View.VISIBLE : View.GONE);
        });

        // Register button logic
        buttonRegister.setOnClickListener(v -> handleRegister());

        // Back to login
        textViewLogin.setOnClickListener(v -> finish());
    }

    private void handleRegister() {
        String username = editTextUsername.getText().toString().trim();
        String email = editTextEmail.getText().toString().trim();
        String password = editTextPassword.getText().toString();
        String fullName = editTextFullName.getText().toString().trim();
        boolean isArtist = checkBoxArtist.isChecked();

        if (username.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập đầy đủ các trường bắt buộc (*)", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 6) {
            Toast.makeText(this, "Mật khẩu phải có ít nhất 6 ký tự", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> registerRequest = new HashMap<>();
        registerRequest.put("username", username);
        registerRequest.put("email", email);
        registerRequest.put("password", password);
        registerRequest.put("full_name", fullName);
        registerRequest.put("artist_register", isArtist);

        buttonRegister.setEnabled(false);

        RetrofitClient.getApiService().register(registerRequest).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                buttonRegister.setEnabled(true);
                if (response.isSuccessful()) {
                    Toast.makeText(RegisterActivity.this, "Đăng ký thành công!", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(RegisterActivity.this, "Đăng ký thất bại", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                buttonRegister.setEnabled(true);
                Toast.makeText(RegisterActivity.this, "Lỗi kết nối: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
