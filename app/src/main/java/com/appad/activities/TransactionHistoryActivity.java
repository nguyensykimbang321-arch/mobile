package com.appad.activities;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.appad.R;
import com.appad.adapters.TransactionAdapter;
import com.appad.models.Transaction;
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

public class TransactionHistoryActivity extends AppCompatActivity {

    private RecyclerView rvHistory;
    private TransactionAdapter adapter;
    private List<Transaction> transactionList = new ArrayList<>();
    private com.appad.utils.MiniPlayerHelper miniPlayerHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_transaction_history);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        rvHistory = findViewById(R.id.rvTransactionHistory);
        rvHistory.setLayoutManager(new LinearLayoutManager(this));
        adapter = new TransactionAdapter(this, transactionList);
        rvHistory.setAdapter(adapter);

        loadHistory();
    }

    private void loadHistory() {
        RetrofitClient.getApiService().getTransactionHistory().enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Object data = response.body().get("data");
                    if (data instanceof List) {
                        Gson gson = new Gson();
                        String json = gson.toJson(data);
                        Type listType = new TypeToken<List<Transaction>>(){}.getType();
                        List<Transaction> transactions = gson.fromJson(json, listType);
                        
                        if (transactions != null) {
                            transactionList.clear();
                            transactionList.addAll(transactions);
                            adapter.notifyDataSetChanged();
                        }
                    }
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                t.printStackTrace();
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

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
