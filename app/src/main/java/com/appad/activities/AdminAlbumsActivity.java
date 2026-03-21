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
import com.appad.adapters.AdminAlbumAdapter;
import com.appad.utils.RetrofitClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminAlbumsActivity extends AppCompatActivity implements AdminAlbumAdapter.OnAlbumActionEventListener {

    private RecyclerView rvAlbums;
    private SwipeRefreshLayout swipeRefresh;
    private EditText edtSearch;
    private TextView txtEmpty;

    private AdminAlbumAdapter adapter;
    private List<Map<String, Object>> allAlbums = new ArrayList<>();
    private List<Map<String, Object>> filteredAlbums = new ArrayList<>();
    private String currentQuery = "";
    private com.appad.utils.MiniPlayerHelper miniPlayerHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_albums);

        initViews();
        setupListeners();
        loadAlbums();
    }

    private void initViews() {
        rvAlbums = findViewById(R.id.rvAdminAlbums);
        swipeRefresh = findViewById(R.id.swipeRefreshAlbums);
        edtSearch = findViewById(R.id.edtSearchAlbums);
        txtEmpty = findViewById(R.id.txtEmptyAlbums);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnAddAlbum).setOnClickListener(v -> {
            Intent intent = new Intent(this, AdminEditAlbumActivity.class);
            startActivity(intent);
        });

        rvAlbums.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AdminAlbumAdapter(this, filteredAlbums, this);
        rvAlbums.setAdapter(adapter);
    }

    private void setupListeners() {
        swipeRefresh.setOnRefreshListener(this::loadAlbums);

        edtSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentQuery = s.toString().toLowerCase().trim();
                applyFilters();
            }
            @Override public void afterTextChanged(Editable s) {}
        });
    }

    private void loadAlbums() {
        swipeRefresh.setRefreshing(true);
        RetrofitClient.getApiService().getAllAlbumsAdmin(100).enqueue(new Callback<List<Map<String, Object>>>() {
            @Override
            public void onResponse(Call<List<Map<String, Object>>> call, Response<List<Map<String, Object>>> response) {
                swipeRefresh.setRefreshing(false);
                if (response.isSuccessful() && response.body() != null) {
                    allAlbums.clear();
                    allAlbums.addAll(response.body());
                    applyFilters();
                }
            }

            @Override
            public void onFailure(Call<List<Map<String, Object>>> call, Throwable t) {
                swipeRefresh.setRefreshing(false);
                Toast.makeText(AdminAlbumsActivity.this, "Lỗi tải danh sách album", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void applyFilters() {
        filteredAlbums.clear();
        if (currentQuery.isEmpty()) {
            filteredAlbums.addAll(allAlbums);
        } else {
            for (Map<String, Object> album : allAlbums) {
                String title = ((String) album.get("title")).toLowerCase();
                String artist = album.get("artistName") != null ? ((String) album.get("artistName")).toLowerCase() : "";
                if (artist.isEmpty() && album.get("artist_name") != null) artist = ((String) album.get("artist_name")).toLowerCase();

                if (title.contains(currentQuery) || artist.contains(currentQuery)) {
                    filteredAlbums.add(album);
                }
            }
        }
        adapter.notifyDataSetChanged();
        txtEmpty.setVisibility(filteredAlbums.isEmpty() ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onEditAlbum(Map<String, Object> albumMap) {
        com.appad.models.Album album = new com.appad.models.Album();
        if (albumMap.get("album_id") != null) album.setAlbumId(((Number)albumMap.get("album_id")).intValue());
        if (albumMap.get("title") != null) album.setTitle((String)albumMap.get("title"));
        if (albumMap.get("artist_id") != null) album.setArtistId(((Number)albumMap.get("artist_id")).intValue());
        if (albumMap.get("artist_name") != null) album.setArtistName((String)albumMap.get("artist_name"));
        else if (albumMap.get("artistName") != null) album.setArtistName((String)albumMap.get("artistName"));
        if (albumMap.get("cover_url") != null) album.setCoverUrl((String)albumMap.get("cover_url"));
        if (albumMap.get("release_date") != null) album.setReleaseDate((String)albumMap.get("release_date"));
        if (albumMap.get("price") != null) album.setPrice(((Number)albumMap.get("price")).doubleValue());
        if (albumMap.get("is_premium") != null) {
            Object premium = albumMap.get("is_premium");
            album.setIsPremium(premium instanceof Boolean ? ((Boolean)premium ? 1 : 0) : ((Number)premium).intValue());
        }

        Intent intent = new Intent(this, AdminEditAlbumActivity.class);
        intent.putExtra("album", album);
        startActivity(intent);
    }

    @Override
    public void onDeleteAlbum(Map<String, Object> album) {
        new AlertDialog.Builder(this)
                .setTitle("Xóa album")
                .setMessage("Bạn có chắc chắn muốn xóa album '" + album.get("title") + "'? Dữ liệu này không thể phục hồi.")
                .setPositiveButton("Xóa", (dialog, which) -> {
                    deleteAlbum(album);
                })
                .setNegativeButton("Hủy", null)
                .show();
    }

    private void deleteAlbum(Map<String, Object> album) {
        Integer id = ((Number) album.get("album_id")).intValue();
        RetrofitClient.getApiService().deleteAlbumAdmin(id).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(AdminAlbumsActivity.this, "Đã xóa album", Toast.LENGTH_SHORT).show();
                    loadAlbums();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Toast.makeText(AdminAlbumsActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadAlbums();
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
