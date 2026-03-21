package com.appad.activities;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.appad.R;
import com.appad.utils.RetrofitClient;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class DepositActivity extends AppCompatActivity {

    private TextView txtCurrentBalance;
    private EditText edtCustomAmount;
    private Button btnDeposit;
    private double selectedAmount = 0;
    private com.appad.utils.MiniPlayerHelper miniPlayerHelper;

    // QR Mode Views
    private android.widget.LinearLayout layoutDepositInput, layoutDepositQR;
    private android.widget.ImageView imgQRCode;
    private TextView txtQRAmount, txtQRAccountNumber, txtQRAccountName, txtQRInfo;
    private android.widget.ImageView btnCopyAccount, btnCopyInfo;
    private Button btnComplete, btnCancelQR;

    private int[] buttonIds = {R.id.btn10k, R.id.btn50k, R.id.btn100k, R.id.btn200k, R.id.btn500k, R.id.btn1m};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_deposit);

        txtCurrentBalance = findViewById(R.id.txtCurrentBalance);
        edtCustomAmount = findViewById(R.id.edtCustomAmount);
        btnDeposit = findViewById(R.id.btnDeposit);

        // QR Mode Views Init
        layoutDepositInput = findViewById(R.id.layoutDepositInput);
        layoutDepositQR = findViewById(R.id.layoutDepositQR);
        imgQRCode = findViewById(R.id.imgQRCode);
        txtQRAmount = findViewById(R.id.txtQRAmount);
        txtQRAccountNumber = findViewById(R.id.txtQRAccountNumber);
        txtQRAccountName = findViewById(R.id.txtQRAccountName);
        txtQRInfo = findViewById(R.id.txtQRInfo);
        btnCopyAccount = findViewById(R.id.btnCopyAccount);
        btnCopyInfo = findViewById(R.id.btnCopyInfo);
        btnComplete = findViewById(R.id.btnComplete);
        btnCancelQR = findViewById(R.id.btnCancelQR);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        loadBalance();

        for (int id : buttonIds) {
            Button btn = findViewById(id);
            btn.setOnClickListener(v -> {
                selectedAmount = Double.parseDouble(btn.getTag().toString());
                edtCustomAmount.setText("");
                highlightButton(btn);
            });
        }

        btnDeposit.setOnClickListener(v -> processDeposit());
        
        btnComplete.setOnClickListener(v -> {
            new android.app.AlertDialog.Builder(this)
                .setTitle("Xác nhận")
                .setMessage("Bạn đã chuyển khoản thành công chưa? Hệ thống sẽ cập nhật số dư sau khi kiểm tra.")
                .setPositiveButton("Đã chuyển", (dialog, which) -> confirmDepositAtServer())
                .setNegativeButton("Chưa", null)
                .show();
        });

        btnCancelQR.setOnClickListener(v -> {
            layoutDepositQR.setVisibility(android.view.View.GONE);
            layoutDepositInput.setVisibility(android.view.View.VISIBLE);
        });

        // Copy Listeners
        btnCopyAccount.setOnClickListener(v -> copyText(txtQRAccountNumber.getText().toString(), "Số tài khoản"));
        btnCopyInfo.setOnClickListener(v -> copyText(txtQRInfo.getText().toString(), "Nội dung"));
    }

    private void copyText(String text, String label) {
        android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(android.content.Context.CLIPBOARD_SERVICE);
        android.content.ClipData clip = android.content.ClipData.newPlainText(label, text);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(this, "Đã sao chép " + label, Toast.LENGTH_SHORT).show();
    }

    private void highlightButton(Button selected) {
        for (int id : buttonIds) {
            Button btn = findViewById(id);
            btn.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                btn == selected ? 0xFF8b5cf6 : 0xFF333333
            ));
        }
    }

    private void loadBalance() {
        RetrofitClient.getApiService().getBalance().enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Object> data = (Map<String, Object>) response.body().get("data");
                    if (data != null && data.containsKey("balance")) {
                        double balance = ((Number) data.get("balance")).doubleValue();
                        NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
                        txtCurrentBalance.setText(formatter.format(balance));
                    }
                }
            }
            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {}
        });
    }

    private void processDeposit() {
        String customText = edtCustomAmount.getText().toString().trim();
        double amount = selectedAmount;

        if (!customText.isEmpty()) {
            try {
                amount = Double.parseDouble(customText);
            } catch (NumberFormatException e) {
                Toast.makeText(this, "Số tiền không hợp lệ", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        if (amount <= 0) {
            Toast.makeText(this, "Vui lòng chọn hoặc nhập số tiền", Toast.LENGTH_SHORT).show();
            return;
        }

        Map<String, Object> payload = new HashMap<>();
        payload.put("amount", amount);

        RetrofitClient.getApiService().deposit(payload).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null && Boolean.TRUE.equals(response.body().get("success"))) {
                    Map<String, Object> data = (Map<String, Object>) response.body().get("data");
                    if (data != null) {
                        showQRCode(data);
                    }
                } else {
                    Toast.makeText(DepositActivity.this, "Nạp tiền thất bại", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Toast.makeText(DepositActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showQRCode(Map<String, Object> data) {
        Map<String, Object> bankInfo = (Map<String, Object>) data.get("bank_info");
        String qrUrl = (String) bankInfo.get("qr_url");
        String refCode = (String) data.get("reference_code");
        double amount = ((Number) data.get("amount")).doubleValue();

        this.qrAmount = amount;
        this.qrRefCode = refCode;
        
        NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        txtQRAmount.setText(formatter.format(amount));
        txtQRAccountNumber.setText((String) bankInfo.get("account_number"));
        txtQRAccountName.setText((String) bankInfo.get("account_name"));
        txtQRInfo.setText(refCode);

        com.bumptech.glide.Glide.with(this).load(qrUrl).into(imgQRCode);

        layoutDepositInput.setVisibility(android.view.View.GONE);
        layoutDepositQR.setVisibility(android.view.View.VISIBLE);
    }

    private double qrAmount = 0;
    private String qrRefCode = "";

    private void confirmDepositAtServer() {
        Map<String, Object> payload = new HashMap<>();
        payload.put("amount", qrAmount);
        payload.put("reference_code", qrRefCode);

        RetrofitClient.getApiService().confirmDeposit(payload).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(DepositActivity.this, "Gửi xác nhận thành công!", Toast.LENGTH_LONG).show();
                    finish();
                } else {
                    Toast.makeText(DepositActivity.this, "Lỗi xác nhận", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Toast.makeText(DepositActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
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
