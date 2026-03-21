package com.appad.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.appad.R;
import com.appad.adapters.AdminSongAdapter;
import com.appad.models.Song;
import com.appad.utils.RetrofitClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminSongsActivity extends AppCompatActivity implements AdminSongAdapter.OnSongActionEventListener {

    private RecyclerView rvSongs;
    private SwipeRefreshLayout swipeRefresh;
    private EditText edtSearch;
    private ImageButton btnClearSearch;
    private TextView txtEmpty;

    private AdminSongAdapter adapter;
    private List<Song> allSongs = new ArrayList<>();
    private List<Song> filteredSongs = new ArrayList<>();
    private String currentQuery = "";
    private com.appad.utils.MiniPlayerHelper miniPlayerHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_songs);

        initViews();
        setupListeners();
        loadSongs();
    }

    private void initViews() {
        rvSongs = findViewById(R.id.rvAdminSongs);
        swipeRefresh = findViewById(R.id.swipeRefreshSongs);
        edtSearch = findViewById(R.id.edtSearchSongs);
        btnClearSearch = findViewById(R.id.btnClearSearch);
        txtEmpty = findViewById(R.id.txtEmptySongs);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnAddSong).setOnClickListener(v -> {
            // Navigate to EditSongActivity with null song (Add mode)
            Intent intent = new Intent(this, AdminEditSongActivity.class);
            startActivity(intent);
        });

        rvSongs.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AdminSongAdapter(this, filteredSongs, this);
        rvSongs.setAdapter(adapter);
    }

    private void setupListeners() {
        swipeRefresh.setOnRefreshListener(this::loadSongs);

        edtSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentQuery = s.toString().toLowerCase().trim();
                btnClearSearch.setVisibility(currentQuery.isEmpty() ? View.GONE : View.VISIBLE);
                applyFilters();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        btnClearSearch.setOnClickListener(v -> edtSearch.setText(""));
    }

    private void loadSongs() {
        swipeRefresh.setRefreshing(true);
        RetrofitClient.getApiService().getAllSongsAdmin(100, currentQuery).enqueue(new Callback<List<Song>>() {
            @Override
            public void onResponse(Call<List<Song>> call, Response<List<Song>> response) {
                swipeRefresh.setRefreshing(false);
                if (response.isSuccessful() && response.body() != null) {
                    allSongs.clear();
                    allSongs.addAll(response.body());
                    applyFilters();
                }
            }

            @Override
            public void onFailure(Call<List<Song>> call, Throwable t) {
                swipeRefresh.setRefreshing(false);
                Toast.makeText(AdminSongsActivity.this, "Lỗi tải danh sách bài hát", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void applyFilters() {
        filteredSongs.clear();
        if (currentQuery.isEmpty()) {
            filteredSongs.addAll(allSongs);
        } else {
            for (Song song : allSongs) {
                if (song.getTitle().toLowerCase().contains(currentQuery) || 
                    (song.getArtistName() != null && song.getArtistName().toLowerCase().contains(currentQuery))) {
                    filteredSongs.add(song);
                }
            }
        }
        adapter.notifyDataSetChanged();
        txtEmpty.setVisibility(filteredSongs.isEmpty() ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onEditSong(Song song) {
        Intent intent = new Intent(this, AdminEditSongActivity.class);
        intent.putExtra("song", song);
        startActivity(intent);
    }

    @Override
    public void onDeleteSong(Song song) {
        new AlertDialog.Builder(this)
                .setTitle("Xóa bài hát")
                .setMessage("Bạn có chắc chắn muốn xóa bài hát '" + song.getTitle() + "' khỏi hệ thống?")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    deleteSong(song);
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void deleteSong(Song song) {
        RetrofitClient.getApiService().deleteSongAdmin(song.getSongId()).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(AdminSongsActivity.this, "Đã xóa bài hát", Toast.LENGTH_SHORT).show();
                    loadSongs();
                } else {
                    Toast.makeText(AdminSongsActivity.this, "Lỗi khi xóa bài hát", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Toast.makeText(AdminSongsActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadSongs();
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
