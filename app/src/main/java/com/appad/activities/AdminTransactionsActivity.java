package com.appad.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.appad.R;
import com.appad.adapters.AdminTransactionAdapter;
import com.appad.utils.RetrofitClient;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminTransactionsActivity extends AppCompatActivity implements AdminTransactionAdapter.OnTransactionActionEventListener {

    private RecyclerView rvTransactions;
    private SwipeRefreshLayout swipeRefresh;
    private TextView txtEmpty;
    private ChipGroup chipGroupStatus;

    private AdminTransactionAdapter adapter;
    private List<Map<String, Object>> transactions = new ArrayList<>();
    private String currentFilter = "all"; // Default to show all
    private com.appad.utils.MiniPlayerHelper miniPlayerHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_transactions);

        initViews();
        setupListeners();
        loadTransactions();
    }

    private void initViews() {
        rvTransactions = findViewById(R.id.rvAdminDeposits);
        swipeRefresh = findViewById(R.id.swipeRefreshDeposits);
        txtEmpty = findViewById(R.id.txtEmptyDeposits);
        chipGroupStatus = findViewById(R.id.chipGroupDepositStatus);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        rvTransactions.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AdminTransactionAdapter(this, transactions, this);
        rvTransactions.setAdapter(adapter);
    }

    private void setupListeners() {
        swipeRefresh.setOnRefreshListener(this::loadTransactions);

        chipGroupStatus.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chipAll) currentFilter = "all";
            else if (checkedId == R.id.chipPending) currentFilter = "pending";
            else if (checkedId == R.id.chipCompleted) currentFilter = "completed";
            else if (checkedId == R.id.chipCancelled) currentFilter = "cancelled";
            loadTransactions();
        });
    }

    private void loadTransactions() {
        swipeRefresh.setRefreshing(true);
        String status = currentFilter.equals("all") ? null : currentFilter;
        RetrofitClient.getApiService().getAllTransactions("deposit", status, 100).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                swipeRefresh.setRefreshing(false);
                if (response.isSuccessful() && response.body() != null) {
                    List<Map<String, Object>> list = (List<Map<String, Object>>) response.body().get("data"); 
                    // Note: API returns just list or object? Let's check RN service.
                    // RN: if (response.success) setTransactions(response.data);
                    // Standard backend wrapper often has success/data. If backend returns raw list, we adapt.
                    // Based on previous files, backend wraps in success/data usually.
                    
                    // Actually, let's check ApiService definition again. 
                    // Call<Map<String, Object>> getAllTransactions...
                    
                    transactions.clear();
                    if (list == null && response.body().containsKey("data")) {
                         Object dataObj = response.body().get("data");
                         if (dataObj instanceof List) {
                             transactions.addAll((List<Map<String, Object>>) dataObj);
                         }
                    } else if (list != null) {
                        transactions.addAll(list);
                    }
                    
                    adapter.notifyDataSetChanged();
                    txtEmpty.setVisibility(transactions.isEmpty() ? View.VISIBLE : View.GONE);
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                swipeRefresh.setRefreshing(false);
                Toast.makeText(AdminTransactionsActivity.this, "Lỗi tải giao dịch", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void onApprove(Map<String, Object> transaction) {
        Integer id = ((Number) transaction.get("transaction_id")).intValue();
        new AlertDialog.Builder(this)
                .setTitle("Phê duyệt giao dịch")
                .setMessage("Xác nhận cộng tiền cho người dùng?")
                .setPositiveButton("Xác nhận", (dialog, which) -> approve(id))
                .setNegativeButton("Hủy", null)
                .show();
    }

    @Override
    public void onReject(Map<String, Object> transaction) {
        Integer id = ((Number) transaction.get("transaction_id")).intValue();
        final EditText input = new EditText(this);
        input.setHint("Nhập lý do từ chối...");
        
        new AlertDialog.Builder(this)
                .setTitle("Từ chối giao dịch")
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

    private void approve(Integer id) {
        RetrofitClient.getApiService().approveDeposit(id).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(AdminTransactionsActivity.this, "Phê duyệt thành công", Toast.LENGTH_SHORT).show();
                    loadTransactions();
                }
            }
            @Override public void onFailure(Call<Map<String, Object>> call, Throwable t) {}
        });
    }

    private void reject(Integer id, String reason) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("admin_note", reason); // If backend uses admin_note or description/reason
        RetrofitClient.getApiService().rejectDeposit(id, payload).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(AdminTransactionsActivity.this, "Đã từ chối giao dịch", Toast.LENGTH_SHORT).show();
                    loadTransactions();
                }
            }
            @Override public void onFailure(Call<Map<String, Object>> call, Throwable t) {}
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
