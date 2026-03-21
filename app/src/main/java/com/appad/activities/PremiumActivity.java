package com.appad.activities;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.appad.R;
import com.appad.services.ApiService;
import com.appad.utils.RetrofitClient;
import com.appad.utils.SessionManager;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class PremiumActivity extends AppCompatActivity {

    private LinearLayout layoutActiveStatus, layoutSubscribe, btnQuickTopUp, btnTransactionHistory;
    private TextView txtExpiryDate, txtBalance, txtTopUpNeeded, txtPremiumPrice;
    private Button btnSubscribePremium;
    private ProgressBar pbLoading;
    
    private double currentBalance = 0;
    private final int PREMIUM_PRICE = 99000;
    private com.appad.utils.MiniPlayerHelper miniPlayerHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_premium);

        initViews();
        setupListeners();
        fetchData();
    }

    private void initViews() {
        layoutActiveStatus = findViewById(R.id.layoutActiveStatus);
        layoutSubscribe = findViewById(R.id.layoutSubscribe);
        btnQuickTopUp = findViewById(R.id.btnQuickTopUp);
        btnTransactionHistory = findViewById(R.id.btnTransactionHistory);
        
        txtExpiryDate = findViewById(R.id.txtExpiryDate);
        txtBalance = findViewById(R.id.txtBalance);
        txtTopUpNeeded = findViewById(R.id.txtTopUpNeeded);
        txtPremiumPrice = findViewById(R.id.txtPremiumPrice);
        
        btnSubscribePremium = findViewById(R.id.btnSubscribePremium);
        pbLoading = findViewById(R.id.pbLoading);

        txtPremiumPrice.setText(formatCurrencyNoSymbol(PREMIUM_PRICE));
    }

    private void setupListeners() {
        btnSubscribePremium.setOnClickListener(v -> handleSubscribe());
        btnQuickTopUp.setOnClickListener(v -> {
            startActivity(new Intent(this, DepositActivity.class));
        });
        btnTransactionHistory.setOnClickListener(v -> {
            startActivity(new Intent(this, MembershipHistoryActivity.class));
        });
    }

    private void fetchData() {
        pbLoading.setVisibility(View.VISIBLE);
        layoutActiveStatus.setVisibility(View.GONE);
        layoutSubscribe.setVisibility(View.GONE);

        // Fetch Profile & Balance
        RetrofitClient.getApiService().getProfile().enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Object> data = (Map<String, Object>) response.body().get("data");
                    android.util.Log.d("PremiumActivity", "Full Profile Data: " + response.body().toString());
                    if (data != null) {
                        android.util.Log.d("PremiumActivity", "User Data Map: " + data.toString());
                        updatePremiumUI(data);
                    }
                }
                checkLoadingDone();
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                checkLoadingDone();
                Toast.makeText(PremiumActivity.this, "Lỗi tải thông tin", Toast.LENGTH_SHORT).show();
            }
        });

        RetrofitClient.getApiService().getBalance().enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Object> data = (Map<String, Object>) response.body().get("data");
                    if (data != null && data.containsKey("balance")) {
                        currentBalance = ((Number) data.get("balance")).doubleValue();
                        updateBalanceUI();
                    }
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {}
        });
    }

    private void checkLoadingDone() {
        pbLoading.setVisibility(View.GONE);
    }

    private void updatePremiumUI(Map<String, Object> data) {
        boolean isPremium = false;
        if (data.containsKey("is_premium")) {
            Object pre = data.get("is_premium");
            android.util.Log.d("PremiumActivity", "is_premium value: " + pre + " (Type: " + (pre != null ? pre.getClass().getSimpleName() : "null") + ")");
            isPremium = (pre instanceof Number && ((Number)pre).intValue() == 1) || Boolean.TRUE.equals(pre);
        } else if (data.containsKey("isPremium")) {
            Object pre = data.get("isPremium");
            android.util.Log.d("PremiumActivity", "isPremium (camelCase) value: " + pre + " (Type: " + (pre != null ? pre.getClass().getSimpleName() : "null") + ")");
            isPremium = (pre instanceof Number && ((Number)pre).intValue() == 1) || Boolean.TRUE.equals(pre);
        }

        android.util.Log.d("PremiumActivity", "Final isPremium result: " + isPremium);

        if (isPremium) {
            layoutActiveStatus.setVisibility(View.VISIBLE);
            layoutSubscribe.setVisibility(View.GONE);
            
            String expiryStr = null;
            if (data.containsKey("premium_expiry")) {
                expiryStr = String.valueOf(data.get("premium_expiry"));
            } else if (data.containsKey("premiumExpiry")) {
                expiryStr = String.valueOf(data.get("premiumExpiry"));
            }

            android.util.Log.d("PremiumActivity", "Premium expiry raw value: " + expiryStr);

            if (expiryStr != null && !expiryStr.equals("null") && !expiryStr.isEmpty()) {
                txtExpiryDate.setText(formatDate(expiryStr));
            } else {
                txtExpiryDate.setText("Không thời hạn");
            }
        } else {
            layoutActiveStatus.setVisibility(View.GONE);
            layoutSubscribe.setVisibility(View.VISIBLE);
        }
    }

    private void updateBalanceUI() {
        NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        txtBalance.setText(formatter.format(currentBalance));

        if (currentBalance < PREMIUM_PRICE) {
            txtBalance.setTextColor(Color.parseColor("#EF5350"));
            btnQuickTopUp.setVisibility(View.VISIBLE);
            double needed = PREMIUM_PRICE - currentBalance;
            txtTopUpNeeded.setText("Nạp thêm " + formatCurrency(needed));
        } else {
            txtBalance.setTextColor(Color.parseColor("#4CAF50"));
            btnQuickTopUp.setVisibility(View.GONE);
        }
    }

    private void handleSubscribe() {
        if (currentBalance < PREMIUM_PRICE) {
            showTopUpDialog();
            return;
        }

        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Đăng ký Premium")
            .setMessage("Bạn có muốn đăng ký gói Premium 30 ngày với giá " + formatCurrency(PREMIUM_PRICE) + "?")
            .setPositiveButton("Đăng ký", (dialog, which) -> performSubscription())
            .setNegativeButton("Hủy", null)
            .show();
    }

    private void showTopUpDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Số dư không đủ")
            .setMessage("Bạn cần thêm " + formatCurrency(PREMIUM_PRICE - currentBalance) + " để đăng ký Premium. Bạn có muốn nạp tiền không?")
            .setPositiveButton("Nạp tiền", (dialog, which) -> {
                startActivity(new Intent(this, DepositActivity.class));
            })
            .setNegativeButton("Hủy", null)
            .show();
    }

    private void performSubscription() {
        pbLoading.setVisibility(View.VISIBLE);
        btnSubscribePremium.setEnabled(false);

        Map<String, Object> payload = new HashMap<>();
        payload.put("duration", 30); // 30 days like React Native

        RetrofitClient.getApiService().subscribePremium(payload).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                pbLoading.setVisibility(View.GONE);
                btnSubscribePremium.setEnabled(true);
                
                if (response.isSuccessful()) {
                    Toast.makeText(PremiumActivity.this, "Chúc mừng! Bạn đã trở thành thành viên Premium.", Toast.LENGTH_LONG).show();
                    fetchData(); // Refresh UI
                } else {
                    Toast.makeText(PremiumActivity.this, "Đăng ký thất bại. Vui lòng thử lại sau.", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                pbLoading.setVisibility(View.GONE);
                btnSubscribePremium.setEnabled(true);
                Toast.makeText(PremiumActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String formatDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) return "Không xác định";
        try {
            // Flexible parsing for various ISO formats
            String cleanDate = dateStr;
            if (cleanDate.contains(".")) {
                cleanDate = cleanDate.substring(0, cleanDate.lastIndexOf("."));
            }
            if (cleanDate.endsWith("Z")) {
                cleanDate = cleanDate.substring(0, cleanDate.length() - 1);
            }
            
            SimpleDateFormat inputFormat;
            if (cleanDate.contains("T")) {
                inputFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US);
            } else {
                inputFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
            }
            
            Date date = inputFormat.parse(cleanDate);
            SimpleDateFormat outputFormat = new SimpleDateFormat("dd 'Tháng' MM, yyyy", new Locale("vi", "VN"));
            return outputFormat.format(date);
        } catch (Exception e) {
            e.printStackTrace();
            return dateStr;
        }
    }

    private String formatCurrency(double amount) {
        NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        return formatter.format(amount);
    }

    private String formatCurrencyNoSymbol(double amount) {
        NumberFormat formatter = NumberFormat.getInstance(new Locale("vi", "VN"));
        return formatter.format(amount);
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

