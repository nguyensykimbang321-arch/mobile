package com.appad.activities;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.appad.R;
import com.appad.utils.RetrofitClient;
import com.appad.utils.SessionManager;
import com.google.android.material.textfield.TextInputEditText;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class WithdrawActivity extends AppCompatActivity {

    private TextInputEditText edtAmount, edtBank, edtAccount, edtHolder;
    private TextView txtBalance;
    private Button btnSubmit;
    private double currentBalance = 0;
    private com.appad.utils.MiniPlayerHelper miniPlayerHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_withdraw);

        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbarWithdraw);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish());

        txtBalance = findViewById(R.id.txtAvailableBalance);
        edtAmount = findViewById(R.id.edtWithdrawAmount);
        edtBank = findViewById(R.id.edtBankName);
        edtAccount = findViewById(R.id.edtBankAccount);
        edtHolder = findViewById(R.id.edtAccountHolder);
        btnSubmit = findViewById(R.id.btnSubmitWithdraw);

        loadBalance();

        btnSubmit.setOnClickListener(v -> submitWithdrawal());
    }

    private Integer artistId = null;

    private void loadBalance() {
        Integer userId = SessionManager.getInstance(this).getUserId();
        if (userId == null) return;

        RetrofitClient.getApiService().getArtistByUserId(userId).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Object> data = (Map<String, Object>) response.body().get("data");
                    if (data != null) {
                        artistId = (data.get("artistId") != null) ? ((Number) data.get("artistId")).intValue() : 
                                   (data.get("artist_id") != null) ? ((Number) data.get("artist_id")).intValue() : null;
                        
                        if (data.get("walletBalance") != null) {
                            currentBalance = ((Number) data.get("walletBalance")).doubleValue();
                        } else if (data.get("wallet_balance") != null) {
                            currentBalance = ((Number) data.get("wallet_balance")).doubleValue();
                        }
                        NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
                        txtBalance.setText(formatter.format(currentBalance));
                    }
                }
            }
            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {}
        });
    }

    private void submitWithdrawal() {
        String amountStr = edtAmount.getText().toString();
        if (amountStr.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập số tiền", Toast.LENGTH_SHORT).show();
            return;
        }

        double amount = Double.parseDouble(amountStr);
        if (amount < 100000) {
            Toast.makeText(this, "Tối thiểu rút 100,000 đ", Toast.LENGTH_SHORT).show();
            return;
        }
        if (amount > currentBalance) {
            Toast.makeText(this, "Số dư không đủ", Toast.LENGTH_SHORT).show();
            return;
        }

        if (artistId == null) {
            Toast.makeText(this, "Lỗi: Không tìm thấy thông tin nghệ sĩ", Toast.LENGTH_SHORT).show();
            return;
        }

        String bank = edtBank.getText().toString();
        String account = edtAccount.getText().toString();
        String holder = edtHolder.getText().toString();

        if (bank.isEmpty() || account.isEmpty() || holder.isEmpty()) {
            Toast.makeText(this, "Vui lòng điền đủ thông tin ngân hàng", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("artistId", artistId);
        payload.put("amount", amount);
        payload.put("bankName", bank);
        payload.put("accountNumber", account);
        payload.put("accountHolderName", holder);

        btnSubmit.setEnabled(false);
        RetrofitClient.getApiService().requestWithdrawal(payload).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                btnSubmit.setEnabled(true);
                if (response.isSuccessful()) {
                    Toast.makeText(WithdrawActivity.this, "Đã gửi yêu cầu rút tiền thành công!", Toast.LENGTH_LONG).show();
                    finish();
                } else {
                    Toast.makeText(WithdrawActivity.this, "Gửi yêu cầu thất bại: " + response.message(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                btnSubmit.setEnabled(true);
                Toast.makeText(WithdrawActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        });
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
