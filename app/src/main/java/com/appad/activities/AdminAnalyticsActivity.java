package com.appad.activities;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.appad.R;
import com.appad.utils.RetrofitClient;
import com.google.android.material.chip.ChipGroup;

import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminAnalyticsActivity extends AppCompatActivity {

    private SwipeRefreshLayout swipeRefresh;
    private TextView txtActiveUsers, txtInactiveUsers, txtPremiumUsers;
    private LinearLayout layoutTopSongs;
    private ChipGroup chipGroupPeriod;

    private View boxUsers, boxSongs, boxPlays, boxAlbums;
    private String selectedPeriod = "30d";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_analytics);

        initViews();
        setupListeners();
        loadAllAnalytics();
    }

    private void initViews() {
        swipeRefresh = findViewById(R.id.swipeRefreshAnalytics);
        txtActiveUsers = findViewById(R.id.txtActiveUsers);
        txtInactiveUsers = findViewById(R.id.txtInactiveUsers);
        txtPremiumUsers = findViewById(R.id.txtPremiumUsers);
        layoutTopSongs = findViewById(R.id.layoutTopSongs);
        chipGroupPeriod = findViewById(R.id.chipGroupPeriod);

        boxUsers = findViewById(R.id.boxUsers);
        boxSongs = findViewById(R.id.boxSongs);
        boxPlays = findViewById(R.id.boxPlays);
        boxAlbums = findViewById(R.id.boxAlbums);

        setupStatBox(boxUsers, "Người dùng", "#3B82F6", android.R.drawable.ic_menu_myplaces);
        setupStatBox(boxSongs, "Bài hát", "#10B981", R.drawable.ic_nav_library);
        setupStatBox(boxPlays, "Lượt nghe", "#8B5CF6", android.R.drawable.ic_media_play);
        setupStatBox(boxAlbums, "Album", "#F59E0B", R.drawable.ic_nav_library);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        findViewById(R.id.btnRefresh).setOnClickListener(v -> loadAllAnalytics());
    }

    private void setupStatBox(View box, String title, String colorStr, int iconRes) {
        int color = Color.parseColor(colorStr);
        TextView txtTitle = box.findViewById(R.id.txtStatTitle);
        TextView txtSub = box.findViewById(R.id.txtStatSub);
        ImageView imgIcon = box.findViewById(R.id.imgStatIcon);
        View iconContainer = box.findViewById(R.id.iconContainer);

        txtTitle.setText(title);
        imgIcon.setImageResource(iconRes);
        imgIcon.setImageTintList(ColorStateList.valueOf(color));
        iconContainer.setBackgroundTintList(ColorStateList.valueOf(adjustAlpha(color, 0.15f)));
        txtSub.setTextColor(color);
    }

    private int adjustAlpha(int color, float factor) {
        int alpha = Math.round(Color.alpha(color) * factor);
        int red = Color.red(color);
        int green = Color.green(color);
        int blue = Color.blue(color);
        return Color.argb(alpha, red, green, blue);
    }

    private void setupListeners() {
        swipeRefresh.setOnRefreshListener(this::loadAllAnalytics);

        chipGroupPeriod.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chip7d) selectedPeriod = "7d";
            else if (checkedId == R.id.chip30d) selectedPeriod = "30d";
            else if (checkedId == R.id.chip90d) selectedPeriod = "90d";
            else if (checkedId == R.id.chip1y) selectedPeriod = "1y";
            loadPeriodAnalytics();
        });
    }

    private void loadAllAnalytics() {
        swipeRefresh.setRefreshing(true);
        loadPeriodAnalytics();
        loadUserAnalytics();
        loadSongAnalytics();
    }

    private void loadPeriodAnalytics() {
        RetrofitClient.getApiService().getAnalytics(selectedPeriod).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Object> data = (Map<String, Object>) response.body().get("data");
                    if (data != null) {
                        updateStatValue(boxUsers, data.get("totalUsers"), "+" + data.get("newUsers") + " mới");
                        updateStatValue(boxSongs, data.get("totalSongs"), "+" + data.get("newSongs") + " mới");
                        updateStatValue(boxPlays, data.get("totalPlays"), NumberFormat.getInstance().format(data.get("dailyPlays")) + "/ngày");
                        updateStatValue(boxAlbums, data.get("totalAlbums"), "+" + data.get("newAlbums") + " mới");
                    }
                }
                checkAllLoaded();
            }
            @Override public void onFailure(Call<Map<String, Object>> call, Throwable t) { checkAllLoaded(); }
        });
    }

    private void updateStatValue(View box, Object value, String sub) {
        TextView txtVal = box.findViewById(R.id.txtStatValue);
        TextView txtSub = box.findViewById(R.id.txtStatSub);
        if (value instanceof Number) {
            txtVal.setText(NumberFormat.getInstance().format(value));
        } else {
            txtVal.setText(String.valueOf(value));
        }
        txtSub.setText(sub);
    }

    private void loadUserAnalytics() {
        RetrofitClient.getApiService().getUserAnalytics().enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Object> data = (Map<String, Object>) response.body().get("data");
                    if (data != null) {
                        txtActiveUsers.setText(String.valueOf(data.get("activeUsers")));
                        txtInactiveUsers.setText(String.valueOf(data.get("inactiveUsers")));
                        txtPremiumUsers.setText(String.valueOf(data.get("premiumUsers")));
                    }
                }
                checkAllLoaded();
            }
            @Override public void onFailure(Call<Map<String, Object>> call, Throwable t) { checkAllLoaded(); }
        });
    }

    private void loadSongAnalytics() {
        RetrofitClient.getApiService().getSongAnalytics().enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Object> data = (Map<String, Object>) response.body().get("data");
                    if (data != null) {
                        List<Map<String, Object>> topSongs = (List<Map<String, Object>>) data.get("mostPlayed");
                        updateTopSongs(topSongs);
                    }
                }
                checkAllLoaded();
            }
            @Override public void onFailure(Call<Map<String, Object>> call, Throwable t) { checkAllLoaded(); }
        });
    }

    private void updateTopSongs(List<Map<String, Object>> songs) {
        layoutTopSongs.removeAllViews();
        if (songs == null) return;

        LayoutInflater inflater = LayoutInflater.from(this);
        int rank = 1;
        for (Map<String, Object> song : songs) {
            View itemView = inflater.inflate(R.layout.item_analytics_top_song, layoutTopSongs, false);
            TextView txtRank = itemView.findViewById(R.id.txtRank);
            TextView txtTitle = itemView.findViewById(R.id.txtSongTitle);
            TextView txtArtist = itemView.findViewById(R.id.txtArtistName);
            TextView txtPlays = itemView.findViewById(R.id.txtPlayCount);

            txtRank.setText(String.valueOf(rank));
            txtTitle.setText((String) song.get("title"));
            txtArtist.setText((String) song.get("artist_name"));
            
            Object plays = song.get("plays");
            txtPlays.setText(NumberFormat.getInstance().format(plays != null ? plays : 0));

            if (rank > 3) {
                txtRank.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#1AFFFFFF")));
                txtRank.setTextColor(Color.parseColor("#888888"));
            }

            layoutTopSongs.addView(itemView);
            rank++;
            if (rank > 10) break;
        }
    }

    private void checkAllLoaded() {
        // Simple way to stop refresh
        swipeRefresh.setRefreshing(false);
    }
}
