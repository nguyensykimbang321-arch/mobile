package com.appad.activities;

import android.os.Bundle;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.appad.R;
import com.appad.adapters.SongAdapter;
import com.appad.models.Song;
import com.appad.utils.RetrofitClient;
import com.google.android.material.tabs.TabLayout;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LibraryActivity extends AppCompatActivity {

    private TabLayout tabLayout;
    private RecyclerView recyclerView;
    private SongAdapter songAdapter;
    private List<Song> songList = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_library);

        tabLayout = findViewById(R.id.tabLayout);
        recyclerView = findViewById(R.id.recyclerViewLibrary);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        songAdapter = new SongAdapter(this, songList);
        recyclerView.setAdapter(songAdapter);

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                loadLibraryData(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        // Load default tab (Favorites)
        loadLibraryData(0);
    }

    private void loadLibraryData(int position) {
        // Tạm thời chỉ load tất cả bài hát để demo cho Library
        // Trong thực tế sẽ gọi các endpoint khác nhau như /favorites, /playlists, /history
        RetrofitClient.getApiService().getAllSongs(20, 0).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Object data = response.body().get("data");
                    if (data instanceof List) {
                        com.google.gson.Gson gson = new com.google.gson.Gson();
                        String json = gson.toJson(data);
                        java.lang.reflect.Type listType = new com.google.gson.reflect.TypeToken<List<Song>>(){}.getType();
                        List<Song> fetchedSongs = gson.fromJson(json, listType);
                        
                        songList.clear();
                        songList.addAll(fetchedSongs);
                        songAdapter.notifyDataSetChanged();
                    }
                    Toast.makeText(LibraryActivity.this, "Đã tải dữ liệu thư viện", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Toast.makeText(LibraryActivity.this, "Lỗi tải dữ liệu", Toast.LENGTH_SHORT).show();
            }
        });
    }
}
