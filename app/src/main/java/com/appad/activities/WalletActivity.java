package com.appad.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.appad.R;
import com.appad.adapters.TransactionAdapter;
import com.appad.models.Transaction;
import com.appad.services.ApiService;
import com.appad.utils.RetrofitClient;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class WalletActivity extends AppCompatActivity {

    private TextView txtBalance;
    private RecyclerView rvRecent;
    private SwipeRefreshLayout swipeRefresh;
    private TransactionAdapter adapter;
    private List<Transaction> transactionList = new ArrayList<>();
    private ApiService apiService;
    private com.appad.utils.MiniPlayerHelper miniPlayerHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wallet);

        apiService = RetrofitClient.getApiService();
        initViews();
        setupRecyclerView();
        loadData();
    }

    private void initViews() {
        txtBalance = findViewById(R.id.txtBalance);
        rvRecent = findViewById(R.id.rvRecentTransactions);
        swipeRefresh = findViewById(R.id.swipeRefreshWallet);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnDeposit).setOnClickListener(v -> startActivity(new Intent(this, DepositActivity.class)));
        findViewById(R.id.btnViewAllTransactions).setOnClickListener(v -> startActivity(new Intent(this, TransactionHistoryActivity.class)));

        swipeRefresh.setOnRefreshListener(this::loadData);
    }

    private void setupRecyclerView() {
        rvRecent.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TransactionAdapter(this, transactionList);
        rvRecent.setAdapter(adapter);
    }

    private void loadData() {
        swipeRefresh.setRefreshing(true);

        // Load Balance
        apiService.getBalance().enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Object> data = (Map<String, Object>) response.body().get("data");
                    if (data != null) {
                        Double balance = ((Number) data.get("balance")).doubleValue();
                        txtBalance.setText(String.format("%,.0fđ", balance));
                    }
                }
            }
            @Override public void onFailure(Call<Map<String, Object>> call, Throwable t) {}
        });


        // Load Recent Transactions
        apiService.getTransactionHistory().enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                swipeRefresh.setRefreshing(false);
                if (response.isSuccessful() && response.body() != null) {
                    Object data = response.body().get("data");
                    if (data instanceof List) {
                        Gson gson = new Gson();
                        Type type = new TypeToken<List<Transaction>>(){}.getType();
                        List<Transaction> list = gson.fromJson(gson.toJson(data), type);
                        
                        transactionList.clear();
                        // Show only top 5 recent
                        if (list != null) {
                            for (int i = 0; i < Math.min(list.size(), 5); i++) {
                                transactionList.add(list.get(i));
                            }
                        }
                        adapter.notifyDataSetChanged();
                        
                        findViewById(R.id.txtNoTransactions).setVisibility(transactionList.isEmpty() ? View.VISIBLE : View.GONE);
                    }
                }
            }
            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                swipeRefresh.setRefreshing(false);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadData();
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
