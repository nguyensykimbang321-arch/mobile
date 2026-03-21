package com.appad.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.appad.R;
import com.appad.adapters.AdminWithdrawalAdapter;
import com.appad.utils.RetrofitClient;
import com.google.android.material.chip.ChipGroup;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminWithdrawalsActivity extends AppCompatActivity implements AdminWithdrawalAdapter.OnWithdrawalActionEventListener {

    private RecyclerView rvWithdrawals;
    private SwipeRefreshLayout swipeRefresh;
    private TextView txtPendingCount, txtTotalDisbursed, txtEmpty;
    private ChipGroup chipGroupStatus;

    private AdminWithdrawalAdapter adapter;
    private List<Map<String, Object>> withdrawals = new ArrayList<>();
    private String currentFilter = "pending";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_withdrawals);

        initViews();
        setupListeners();
        loadWithdrawals();
    }

    private void initViews() {
        rvWithdrawals = findViewById(R.id.rvAdminWithdrawals);
        swipeRefresh = findViewById(R.id.swipeRefreshWithdrawals);
        txtPendingCount = findViewById(R.id.txtPendingCount);
        txtTotalDisbursed = findViewById(R.id.txtTotalDisbursed);
        txtEmpty = findViewById(R.id.txtEmptyWithdrawals);
        chipGroupStatus = findViewById(R.id.chipGroupWithdrawalStatus);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        rvWithdrawals.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AdminWithdrawalAdapter(this, withdrawals, this);
        rvWithdrawals.setAdapter(adapter);
    }

    private void setupListeners() {
        swipeRefresh.setOnRefreshListener(this::loadWithdrawals);

        chipGroupStatus.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chipPending) currentFilter = "pending";
            else if (checkedId == R.id.chipCompleted) currentFilter = "completed";
            else if (checkedId == R.id.chipRejected) currentFilter = "rejected";
            else if (checkedId == R.id.chipAll) currentFilter = "all";
            loadWithdrawals();
        });
    }

    private void loadWithdrawals() {
        swipeRefresh.setRefreshing(true);
        String status = currentFilter.equals("all") ? null : currentFilter;
        RetrofitClient.getApiService().getAllWithdrawals(status, 100).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                swipeRefresh.setRefreshing(false);
                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Object> body = response.body();
                    
                    // Update list - backend returns data directly as list
                    Object dataObj = body.get("data");
                    withdrawals.clear();
                    if (dataObj instanceof List) {
                        withdrawals.addAll((List<Map<String, Object>>) dataObj);
                    } else if (dataObj instanceof Map) {
                        // Legacy nested format
                        List<Map<String, Object>> list = (List<Map<String, Object>>) ((Map)dataObj).get("withdrawals");
                        if (list != null) withdrawals.addAll(list);
                    }
                    
                    // Calculate stats from loaded data
                    long pendingCount = withdrawals.stream().filter(w -> "pending".equalsIgnoreCase((String)w.get("status"))).count();
                    double totalApproved = withdrawals.stream()
                            .filter(w -> "approved".equalsIgnoreCase((String)w.get("status")))
                            .mapToDouble(w -> w.get("amount") != null ? ((Number)w.get("amount")).doubleValue() : 0)
                            .sum();
                    
                    txtPendingCount.setText(String.valueOf(pendingCount));
                    java.text.NumberFormat nf = java.text.NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
                    txtTotalDisbursed.setText(nf.format(totalApproved));
                    
                    adapter.notifyDataSetChanged();
                    txtEmpty.setVisibility(withdrawals.isEmpty() ? View.VISIBLE : View.GONE);
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                swipeRefresh.setRefreshing(false);
                Toast.makeText(AdminWithdrawalsActivity.this, "Lỗi kết nối máy chủ", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onApprove(Map<String, Object> withdrawal) {
        Long id = ((Number) withdrawal.get("withdrawal_id")).longValue();
        new AlertDialog.Builder(this)
                .setTitle("Duyệt rút tiền")
                .setMessage("Xác nhận đã chuyển tiền cho nghệ sĩ " + withdrawal.get("artist_name") + "?")
                .setPositiveButton("Xác nhận", (dialog, which) -> {
                    approve(id);
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    @Override
    public void onReject(Map<String, Object> withdrawal) {
        Long id = ((Number) withdrawal.get("withdrawal_id")).longValue();
        
        final EditText input = new EditText(this);
        input.setHint("Nhập lý do từ chối...");
        
        new AlertDialog.Builder(this)
                .setTitle("Từ chối rút tiền")
                .setView(input)
                .setPositiveButton("Xác nhận", (dialog, which) -> {
                    String reason = input.getText().toString().trim();
                    if (reason.isEmpty()) {
                        Toast.makeText(this, "Vui lòng nhập lý do", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    reject(id, reason);
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void approve(Long id) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("admin_note", "Đã duyệt");
        RetrofitClient.getApiService().approveWithdrawal(id, payload).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(AdminWithdrawalsActivity.this, "Đã duyệt thành công", Toast.LENGTH_SHORT).show();
                    loadWithdrawals();
                }
            }
            @Override public void onFailure(Call<Map<String, Object>> call, Throwable t) {}
        });
    }

    private void reject(Long id, String reason) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("admin_note", reason);
        RetrofitClient.getApiService().rejectWithdrawal(id, payload).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(AdminWithdrawalsActivity.this, "Đã từ chối yêu cầu", Toast.LENGTH_SHORT).show();
                    loadWithdrawals();
                }
            }
            @Override public void onFailure(Call<Map<String, Object>> call, Throwable t) {}
        });
    }
}
