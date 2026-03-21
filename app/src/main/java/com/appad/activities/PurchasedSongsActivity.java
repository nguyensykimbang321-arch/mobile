package com.appad.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.appad.R;
import com.appad.adapters.SongAdapter;
import com.appad.models.Song;
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

public class PurchasedSongsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefresh;
    private View layoutEmpty;
    private SongAdapter adapter;
    private List<Song> purchasedSongs = new ArrayList<>();
    private ApiService apiService;
    private com.appad.utils.MiniPlayerHelper miniPlayerHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_purchased_songs);

        apiService = RetrofitClient.getApiService();
        initViews();
        setupRecyclerView();
        loadPurchasedSongs();
    }

    private void initViews() {
        Toolbar toolbar = findViewById(R.id.toolbarPurchased);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        recyclerView = findViewById(R.id.rvPurchasedSongs);
        swipeRefresh = findViewById(R.id.swipeRefreshPurchased);
        layoutEmpty = findViewById(R.id.layoutEmptyPurchased);

        findViewById(R.id.btnBrowseSongs).setOnClickListener(v -> {
            // Navigate to search or home for browsing
            finish();
        });

        swipeRefresh.setOnRefreshListener(this::loadPurchasedSongs);
    }

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SongAdapter(this, purchasedSongs);
        recyclerView.setAdapter(adapter);
    }

    private void loadPurchasedSongs() {
        swipeRefresh.setRefreshing(true);
        apiService.getPurchasedSongs().enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                swipeRefresh.setRefreshing(false);
                if (response.isSuccessful() && response.body() != null) {
                    Object data = response.body().get("data");
                    if (data instanceof List) {
                        Gson gson = new Gson();
                        Type type = new TypeToken<List<Song>>(){}.getType();
                        List<Song> songs = gson.fromJson(gson.toJson(data), type);
                        
                        purchasedSongs.clear();
                        if (songs != null) purchasedSongs.addAll(songs);
                        adapter.notifyDataSetChanged();
                        
                        layoutEmpty.setVisibility(purchasedSongs.isEmpty() ? View.VISIBLE : View.GONE);
                        recyclerView.setVisibility(purchasedSongs.isEmpty() ? View.GONE : View.VISIBLE);
                    }
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                swipeRefresh.setRefreshing(false);
                Toast.makeText(PurchasedSongsActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
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
