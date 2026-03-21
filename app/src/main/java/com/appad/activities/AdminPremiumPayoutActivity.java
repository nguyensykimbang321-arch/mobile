package com.appad.activities;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.appad.R;
import com.appad.adapters.ArtistPayoutAdapter;
import com.appad.adapters.PayoutHistoryAdapter;
import com.appad.utils.RetrofitClient;

import java.text.DecimalFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminPremiumPayoutActivity extends AppCompatActivity {

    private TextView tabPayout, tabHistory, txtHeaderTitle, txtDistributionTitle, lblTotalPool, lblTotalDuration;
    private View swipeRefreshPayout, swipeRefreshHistory, tabContainer;
    private RecyclerView rvArtistShares, rvPayoutHistory;
    private TextView txtPeriod, txtTotalPool, txtTotalDuration, txtEmptyPayout, txtEmptyHistory;
    private Button btnApplyPayout;
    private ProgressBar progressBar;
    private SwipeRefreshLayout refreshPayout, refreshHistory;

    private ArtistPayoutAdapter sharesAdapter;
    private PayoutHistoryAdapter historyAdapter;
    
    private List<Map<String, Object>> sharesList = new ArrayList<>();
    private List<Map<String, Object>> historyList = new ArrayList<>();
    
    private Map<String, Object> currentPayoutData = null;
    private DecimalFormat currencyFormat = new DecimalFormat("#,###đ");
    private boolean isActiveHistory = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_premium_payout);

        initViews();
        setupListeners();
        
        loadPayoutData();
    }

    private void initViews() {
        tabPayout = findViewById(R.id.tabPayout);
        tabHistory = findViewById(R.id.tabHistory);
        swipeRefreshPayout = findViewById(R.id.swipeRefreshPayout);
        swipeRefreshHistory = findViewById(R.id.swipeRefreshHistory);
        
        rvArtistShares = findViewById(R.id.rvArtistShares);
        rvPayoutHistory = findViewById(R.id.rvPayoutHistory);
        
        txtPeriod = findViewById(R.id.txtPeriod);
        txtTotalPool = findViewById(R.id.txtTotalPool);
        txtTotalDuration = findViewById(R.id.txtTotalDuration);
        txtEmptyPayout = findViewById(R.id.txtEmptyPayout);
        txtEmptyHistory = findViewById(R.id.txtEmptyHistory);
        
        btnApplyPayout = findViewById(R.id.btnApplyPayout);
        progressBar = findViewById(R.id.progressBar);
        
        txtHeaderTitle = findViewById(R.id.txtHeaderTitle);
        txtDistributionTitle = findViewById(R.id.txtDistributionTitle);
        lblTotalPool = findViewById(R.id.lblTotalPool);
        lblTotalDuration = findViewById(R.id.lblTotalDuration);

        refreshPayout = findViewById(R.id.swipeRefreshPayout);
        refreshHistory = findViewById(R.id.swipeRefreshHistory);

        rvArtistShares.setLayoutManager(new LinearLayoutManager(this));
        sharesAdapter = new ArtistPayoutAdapter(this, sharesList);
        rvArtistShares.setAdapter(sharesAdapter);

        rvPayoutHistory.setLayoutManager(new LinearLayoutManager(this));
        historyAdapter = new PayoutHistoryAdapter(this, historyList, this::showBatchDetails);
        rvPayoutHistory.setAdapter(historyAdapter);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
    }

    private void setupListeners() {
        tabPayout.setOnClickListener(v -> switchTab(false));
        tabHistory.setOnClickListener(v -> switchTab(true));
        
        btnApplyPayout.setOnClickListener(v -> confirmApplyPayout());
        
        refreshPayout.setOnRefreshListener(this::loadPayoutData);
        refreshHistory.setOnRefreshListener(this::loadHistoryData);
        
        findViewById(R.id.btnRefresh).setOnClickListener(v -> {
            if (isActiveHistory) loadHistoryData();
            else loadPayoutData();
        });
    }

    private void switchTab(boolean isHistory) {
        isActiveHistory = isHistory;
        
        txtHeaderTitle.setText("QUẢN LÝ LƯƠNG PREMIUM");
        txtDistributionTitle.setText("CHI TIẾT PHÂN PHỐI THEO THỜI LƯỢNG");
        lblTotalPool.setText("Tổng quỹ lương");
        lblTotalDuration.setText("Tổng thời lượng");

        tabPayout.setBackgroundResource(isHistory ? R.drawable.bg_tab_unselected : R.drawable.bg_tab_selected);
        tabPayout.setTextColor(isHistory ? Color.parseColor("#888888") : Color.parseColor("#8B5CF6"));
        
        tabHistory.setBackgroundResource(isHistory ? R.drawable.bg_tab_selected : R.drawable.bg_tab_unselected);
        tabHistory.setTextColor(isHistory ? Color.parseColor("#8B5CF6") : Color.parseColor("#888888"));
        
        swipeRefreshPayout.setVisibility(isHistory ? View.GONE : View.VISIBLE);
        swipeRefreshHistory.setVisibility(isHistory ? View.VISIBLE : View.GONE);
        
        if (isHistory) {
            loadHistoryData();
        } else {
            loadPayoutData();
        }
    }

    private void loadPayoutData() {
        refreshPayout.setRefreshing(true);
        RetrofitClient.getApiService().calculatePremiumPayout(new HashMap<>()).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                refreshPayout.setRefreshing(false);
                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Object> data = (Map<String, Object>) response.body().get("data");
                    if (data != null) {
                        currentPayoutData = data;
                        displayPayoutData(data);
                    }
                } else {
                    Toast.makeText(AdminPremiumPayoutActivity.this, "Không thể tải dữ liệu tính toán", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                refreshPayout.setRefreshing(false);
                Toast.makeText(AdminPremiumPayoutActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void displayPayoutData(Map<String, Object> data) {
        txtPeriod.setVisibility(View.VISIBLE);
        String start = (String) data.get("start_date");
        String end = (String) data.get("end_date");
        if (start != null && end != null) {
            // Simplify dates
            txtPeriod.setText("Kỳ: " + start.substring(0, 10) + " - " + end.substring(0, 10));
        }

        Number pool = (Number) data.get("total_pool");
        txtTotalPool.setText(currencyFormat.format(pool != null ? pool.doubleValue() : 0.0));

        Number duration = (Number) data.get("total_duration");
        long totalSec = duration != null ? duration.longValue() : 0;
        txtTotalDuration.setText((totalSec / 60) + "p " + (totalSec % 60) + "s");

        List<Map<String, Object>> shares = (List<Map<String, Object>>) data.get("artist_shares");
        sharesList.clear();
        if (shares != null) {
            sharesList.addAll(shares);
        }
        sharesAdapter.notifyDataSetChanged();

        boolean hasData = !sharesList.isEmpty();
        txtEmptyPayout.setVisibility(hasData ? View.GONE : View.VISIBLE);
        btnApplyPayout.setVisibility(hasData ? View.VISIBLE : View.GONE);
    }

    private void loadHistoryData() {
        refreshHistory.setRefreshing(true);
        RetrofitClient.getApiService().getPayoutHistory().enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                refreshHistory.setRefreshing(false);
                if (response.isSuccessful() && response.body() != null) {
                    List<Map<String, Object>> list = (List<Map<String, Object>>) response.body().get("data");
                    historyList.clear();
                    if (list != null) {
                        historyList.addAll(list);
                    }
                    historyAdapter.notifyDataSetChanged();
                    txtEmptyHistory.setVisibility(historyList.isEmpty() ? View.VISIBLE : View.GONE);
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                refreshHistory.setRefreshing(false);
            }
        });
    }

    private void confirmApplyPayout() {
        if (currentPayoutData == null) return;
        
        Number pool = (Number) currentPayoutData.get("total_pool");
        final String amountStr = currencyFormat.format(pool != null ? pool.doubleValue() : 0.0);
        
        new AlertDialog.Builder(this)
                .setTitle("Xác nhận phát lương")
                .setMessage("Bạn có chắc chắn muốn phát " + amountStr + " cho " + sharesList.size() + " nghệ sĩ không?\nSố tiền sẽ được cộng trực tiếp vào ví nghệ sĩ.")
                .setPositiveButton("Xác nhận", (dialog, which) -> applyPayout())
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void applyPayout() {
        progressBar.setVisibility(View.VISIBLE);
        btnApplyPayout.setEnabled(false);
        
        RetrofitClient.getApiService().applyPremiumPayout(currentPayoutData).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                progressBar.setVisibility(View.GONE);
                btnApplyPayout.setEnabled(true);
                if (response.isSuccessful()) {
                    Toast.makeText(AdminPremiumPayoutActivity.this, "Đã phát lương thành công!", Toast.LENGTH_LONG).show();
                    switchTab(true); // Go to history
                } else {
                    Toast.makeText(AdminPremiumPayoutActivity.this, "Lỗi khi phát lương", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                btnApplyPayout.setEnabled(true);
                Toast.makeText(AdminPremiumPayoutActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showBatchDetails(Map<String, Object> batch) {
        String batchTime = (String) batch.get("batch_time");
        RetrofitClient.getApiService().getPayoutBatchDetails(batchTime).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Object> data = (Map<String, Object>) response.body().get("data");
                    if (data != null) {
                        displayBatchDetails(data);
                    }
                }
            }
            @Override public void onFailure(Call<Map<String, Object>> call, Throwable t) {}
        });
    }

    private void displayBatchDetails(Map<String, Object> data) {
        isActiveHistory = false;
        
        txtHeaderTitle.setText("CHI TIẾT ĐỢT PHÁT LƯƠNG");
        txtDistributionTitle.setText("DANH SÁCH CHI TRẢ CHI TIẾT");
        lblTotalPool.setText("Đã phát lương");
        lblTotalDuration.setText("Số lượng nghệ sĩ");

        // Keep History tab highlighted to show context
        tabPayout.setBackgroundResource(R.drawable.bg_tab_unselected);
        tabPayout.setTextColor(Color.parseColor("#888888"));
        tabHistory.setBackgroundResource(R.drawable.bg_tab_selected);
        tabHistory.setTextColor(Color.parseColor("#8B5CF6"));

        swipeRefreshPayout.setVisibility(View.VISIBLE);
        swipeRefreshHistory.setVisibility(View.GONE);
        
        txtPeriod.setVisibility(View.VISIBLE);
        txtPeriod.setText("Thời gian: " + (data.get("period_start") != null ? data.get("period_start") : "N/A"));
        
        Number total = (Number) data.get("total_paid");
        txtTotalPool.setText(currencyFormat.format(total != null ? total.doubleValue() : 0.0));
        
        txtTotalDuration.setText(data.get("artist_count") + " Nghệ sĩ");
        
        List<Map<String, Object>> artists = (List<Map<String, Object>>) data.get("artists");
        sharesList.clear();
        if (artists != null) sharesList.addAll(artists);
        sharesAdapter.notifyDataSetChanged();
        
        btnApplyPayout.setVisibility(View.GONE); // Hide apply button for historical view
        txtEmptyPayout.setVisibility(sharesList.isEmpty() ? View.VISIBLE : View.GONE);
    }
}
