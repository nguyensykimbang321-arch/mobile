package com.appad.activities;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.appad.R;
import com.appad.utils.RetrofitClient;
import java.util.HashMap;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminBroadcastActivity extends AppCompatActivity {

    private EditText edtTitle, edtMessage;
    private Button btnSend;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_broadcast);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        edtTitle = findViewById(R.id.edtTitle);
        edtMessage = findViewById(R.id.edtMessage);
        btnSend = findViewById(R.id.btnSend);

        btnSend.setOnClickListener(v -> handleSend());
    }

    private void handleSend() {
        String title = edtTitle.getText().toString().trim();
        String message = edtMessage.getText().toString().trim();

        if (title.isEmpty() || message.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSend.setEnabled(false);
        btnSend.setText("Đang gửi...");

        Map<String, String> payload = new HashMap<>();
        payload.put("title", title);
        payload.put("message", message);

        RetrofitClient.getApiService().broadcast(payload).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                btnSend.setEnabled(true);
                btnSend.setText("Gửi thông báo ngay");
                
                if (response.isSuccessful()) {
                    Toast.makeText(AdminBroadcastActivity.this, "Đã gửi thông báo cho tất cả người dùng!", Toast.LENGTH_LONG).show();
                    finish();
                } else {
                    Toast.makeText(AdminBroadcastActivity.this, "Lỗi khi gửi thông báo", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                btnSend.setEnabled(true);
                btnSend.setText("Gửi thông báo ngay");
                Toast.makeText(AdminBroadcastActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
