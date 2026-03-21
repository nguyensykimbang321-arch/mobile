package com.appad.activities;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.appad.R;
import com.appad.utils.RetrofitClient;
import com.appad.utils.SessionManager;
import java.util.HashMap;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ManageMembershipActivity extends AppCompatActivity {

    private EditText edtPrice, edtDuration;
    private Integer artistId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_membership);

        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbarMembership);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Thiết lập Hội viên");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish());

        edtPrice = findViewById(R.id.edtMembershipPrice);
        edtDuration = findViewById(R.id.edtMembershipDuration);

        findViewById(R.id.btnSaveMembership).setOnClickListener(v -> saveSettings());

        loadCurrentSettings();
    }

    private void loadCurrentSettings() {
        Integer userId = SessionManager.getInstance(this).getUserId();
        if (userId == null) return;

        RetrofitClient.getApiService().getArtistByUserId(userId).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Object> data = (Map<String, Object>) response.body().get("data");
                    if (data != null) {
                        artistId = ((Number) data.get("artist_id")).intValue();
                        Object price = data.get("membership_price");
                        Object duration = data.get("membership_duration_days");
                        
                        if (price != null) edtPrice.setText(String.valueOf(((Number)price).intValue()));
                        if (duration != null) edtDuration.setText(String.valueOf(((Number)duration).intValue()));
                    }
                }
            }
            @Override public void onFailure(Call<Map<String, Object>> call, Throwable t) {}
        });
    }

    private void saveSettings() {
        if (artistId == null) {
            Toast.makeText(this, "Không tìm thấy thông tin nghệ sĩ", Toast.LENGTH_SHORT).show();
            return;
        }

        String priceStr = edtPrice.getText().toString();
        String durationStr = edtDuration.getText().toString();

        if (priceStr.isEmpty() || durationStr.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("membershipPrice", Double.parseDouble(priceStr));
        payload.put("membershipDurationDays", Integer.parseInt(durationStr));

        RetrofitClient.getApiService().updateArtistMembershipSettings(artistId, payload).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(ManageMembershipActivity.this, "Cập nhật thành công!", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    Toast.makeText(ManageMembershipActivity.this, "Cập nhật thất bại", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Toast.makeText(ManageMembershipActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
