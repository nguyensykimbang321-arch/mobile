package com.appad.activities;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.appad.R;
import com.appad.adapters.CardAdapter;
import com.appad.adapters.SongAdapter;
import com.appad.models.Song;
import com.appad.utils.MusicPlayerManager;
import com.appad.utils.RetrofitClient;
import com.appad.utils.SessionManager;
import com.bumptech.glide.Glide;
import com.google.android.material.appbar.CollapsingToolbarLayout;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ArtistDetailActivity extends AppCompatActivity {

    private Integer artistId;
    private ImageView imgHeader;
    private TextView txtBio, txtFollowerCount, txtAlbumCountStat, txtSongCountStat, txtListenCountStat;
    private TextView txtPremiumAlbumsTitle, txtPremiumSongsTitle;
    private Button btnFollow, btnPlayAll, btnJoinMembership;
    private RecyclerView rvAlbums, rvUpcomingAlbums, rvSongs, rvPremiumAlbums, rvPremiumSongs;
    private Toolbar toolbar;
    private CollapsingToolbarLayout collapsingToolbar;
    private LinearLayout layoutMemberBenefits, layoutUpcomingAlbums;

    private List<Song> artistSongs = new ArrayList<>();
    private List<Song> releasedAlbums = new ArrayList<>();
    private List<Song> upcomingAlbums = new ArrayList<>();
    private List<Song> premiumSongs = new ArrayList<>();
    private List<Song> premiumAlbums = new ArrayList<>();

    private SongAdapter songAdapter;
    private CardAdapter albumAdapter;
    private CardAdapter upcomingAlbumAdapter;
    private CardAdapter premiumAlbumAdapter;
    private CardAdapter premiumSongAdapter;
    private com.appad.utils.MiniPlayerHelper miniPlayerHelper;

    private boolean isFollowing = false;
    private double membershipPrice = 0;
    private int membershipDuration = 30;
    private double userBalance = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_artist_detail);

        artistId = getIntent().getIntExtra("artistId", -1);
        if (artistId == -1) {
            finish();
            return;
        }

        initViews();
        setupAdapters();
        loadArtistData();
        loadStats();
        checkFollowStatus();
        checkMembership();
        loadUserBalance();
    }

    private void initViews() {
        imgHeader = findViewById(R.id.imgArtistHeader);
        txtBio = findViewById(R.id.txtArtistBio);
        txtFollowerCount = findViewById(R.id.txtFollowerCount);
        txtAlbumCountStat = findViewById(R.id.txtAlbumCountStat);
        txtSongCountStat = findViewById(R.id.txtSongCountStat);
        txtListenCountStat = findViewById(R.id.txtListenCountStat);
        
        txtPremiumAlbumsTitle = findViewById(R.id.txtPremiumAlbumsTitle);
        txtPremiumSongsTitle = findViewById(R.id.txtPremiumSongsTitle);
        
        btnFollow = findViewById(R.id.btnFollowArtist);
        btnPlayAll = findViewById(R.id.btnPlayArtistAll);
        btnJoinMembership = findViewById(R.id.btnJoinMembership);
        
        rvAlbums = findViewById(R.id.rvArtistAlbums);
        rvUpcomingAlbums = findViewById(R.id.rvUpcomingAlbums);
        rvSongs = findViewById(R.id.rvArtistSongs);
        rvPremiumAlbums = findViewById(R.id.rvPremiumAlbums);
        rvPremiumSongs = findViewById(R.id.rvPremiumSongs);
        
        layoutMemberBenefits = findViewById(R.id.layoutMemberBenefits);
        layoutUpcomingAlbums = findViewById(R.id.layoutUpcomingAlbums);
        
        toolbar = findViewById(R.id.toolbarArtist);
        collapsingToolbar = findViewById(R.id.collapsingToolbarArtist);

        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        btnFollow.setOnClickListener(v -> toggleFollow());
        btnJoinMembership.setOnClickListener(v -> handleMembershipClick());
        btnPlayAll.setOnClickListener(v -> playAll());
    }

    private void setupAdapters() {
        rvAlbums.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        albumAdapter = new CardAdapter(this, releasedAlbums, song -> {
            Intent intent = new Intent(this, AlbumDetailActivity.class);
            intent.putExtra("albumId", song.getAlbumId());
            startActivity(intent);
        });
        rvAlbums.setAdapter(albumAdapter);

        rvUpcomingAlbums.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        upcomingAlbumAdapter = new CardAdapter(this, upcomingAlbums, song -> {
            Toast.makeText(this, "Album sắp ra mắt vào: " + song.getReleaseDate(), Toast.LENGTH_SHORT).show();
        });
        rvUpcomingAlbums.setAdapter(upcomingAlbumAdapter);

        rvSongs.setLayoutManager(new LinearLayoutManager(this));
        songAdapter = new SongAdapter(this, artistSongs);
        rvSongs.setAdapter(songAdapter);

        rvPremiumAlbums.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        premiumAlbumAdapter = new CardAdapter(this, premiumAlbums, song -> {
            Intent intent = new Intent(this, AlbumDetailActivity.class);
            intent.putExtra("albumId", song.getAlbumId());
            startActivity(intent);
        });
        rvPremiumAlbums.setAdapter(premiumAlbumAdapter);

        rvPremiumSongs.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        premiumSongAdapter = new CardAdapter(this, premiumSongs, song -> {
            MusicPlayerManager.getInstance().setPlaylist(premiumSongs, premiumSongs.indexOf(song));
            startActivity(new Intent(this, FullPlayerActivity.class));
        });
        rvPremiumSongs.setAdapter(premiumSongAdapter);
    }

    private void loadArtistData() {
        RetrofitClient.getApiService().getArtistById(artistId).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Object> data = (Map<String, Object>) response.body().get("data");
                    if (data != null) {
                        String name = (String) data.get("name");
                        String bio = (String) data.get("bio");
                        String imageUrl = (String) (data.get("image_url") != null ? data.get("image_url") : data.get("imageUrl"));
                        
                        membershipPrice = data.containsKey("membership_price") ? ((Number) data.get("membership_price")).doubleValue() : 0;
                        membershipDuration = data.containsKey("membership_duration_days") ? ((Number) data.get("membership_duration_days")).intValue() : 30;

                        collapsingToolbar.setTitle(name);
                        txtBio.setText(bio != null && !bio.isEmpty() ? bio : "Chưa có thông tin tiểu sử.");
                        
                        Glide.with(ArtistDetailActivity.this)
                                .load(com.appad.utils.ImageUrlUtils.fixUrl(imageUrl))
                                .placeholder(R.drawable.ic_launcher_background)
                                .into(imgHeader);
                        
                        txtFollowerCount.setText(com.appad.utils.FormatUtils.formatCount(data.containsKey("follower_count") ? ((Number) data.get("follower_count")).longValue() : 0L));
                        updateMembershipUI();
                    }
                }
            }
            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {}
        });

        // Load Songs
        RetrofitClient.getApiService().getArtistSongs(artistId).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Object data = response.body().get("data");
                    if (data instanceof List) {
                        com.google.gson.Gson gson = new com.google.gson.Gson();
                        String json = gson.toJson(data);
                        java.lang.reflect.Type listType = new com.google.gson.reflect.TypeToken<List<Song>>(){}.getType();
                        List<Song> songs = gson.fromJson(json, listType);
                        
                        artistSongs.clear();
                        artistSongs.addAll(songs);
                        songAdapter.notifyDataSetChanged();
                        
                        // Filter premium songs
                        premiumSongs.clear();
                        for (Song s : songs) {
                            if (s.isPremium() || (s.getAlbumIsPremium() != null && s.getAlbumIsPremium() == 1)) {
                                premiumSongs.add(s);
                            }
                        }
                        
                        if (!premiumSongs.isEmpty()) {
                            layoutMemberBenefits.setVisibility(View.VISIBLE);
                            txtPremiumSongsTitle.setVisibility(View.VISIBLE);
                            rvPremiumSongs.setVisibility(View.VISIBLE);
                            premiumSongAdapter.notifyDataSetChanged();
                        }
                        
                        txtSongCountStat.setText(String.valueOf(songs.size()));
                    }
                }
            }
            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {}
        });

        // Load Albums
        RetrofitClient.getApiService().getArtistAlbums(artistId).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Object data = response.body().get("data");
                    if (data instanceof List) {
                        com.google.gson.Gson gson = new com.google.gson.Gson();
                        String json = gson.toJson(data);
                        java.lang.reflect.Type listType = new com.google.gson.reflect.TypeToken<List<Song>>(){}.getType();
                        List<Song> albums = gson.fromJson(json, listType);
                        
                        releasedAlbums.clear();
                        upcomingAlbums.clear();
                        premiumAlbums.clear();
                        
                        Date now = new Date();
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                        
                        for (Song a : albums) {
                            boolean isUpcoming = false;
                            try {
                                if (a.getReleaseDate() != null) {
                                    Date rd = sdf.parse(a.getReleaseDate());
                                    if (rd != null && rd.after(now)) isUpcoming = true;
                                }
                            } catch (Exception ignored) {}
                            
                            if (isUpcoming) {
                                upcomingAlbums.add(a);
                            } else {
                                releasedAlbums.add(a);
                                if (a.getIsPremium() != null && a.getIsPremium() == 1) {
                                    premiumAlbums.add(a);
                                }
                            }
                        }
                        
                        albumAdapter.notifyDataSetChanged();
                        txtAlbumCountStat.setText(String.valueOf(albums.size()));
                        
                        if (!upcomingAlbums.isEmpty()) {
                            layoutUpcomingAlbums.setVisibility(View.VISIBLE);
                            upcomingAlbumAdapter.notifyDataSetChanged();
                        } else {
                            layoutUpcomingAlbums.setVisibility(View.GONE);
                        }
                        
                        if (!premiumAlbums.isEmpty()) {
                            layoutMemberBenefits.setVisibility(View.VISIBLE);
                            txtPremiumAlbumsTitle.setVisibility(View.VISIBLE);
                            rvPremiumAlbums.setVisibility(View.VISIBLE);
                            premiumAlbumAdapter.notifyDataSetChanged();
                        }
                    }
                }
            }
            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {}
        });
    }

    private void loadStats() {
        RetrofitClient.getApiService().getArtistDashboard(artistId).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Object> data = (Map<String, Object>) response.body().get("data");
                    if (data != null && data.containsKey("stats")) {
                        Map<String, Object> stats = (Map<String, Object>) data.get("stats");
                        txtListenCountStat.setText(com.appad.utils.FormatUtils.formatCount(((Number) stats.get("total_listens")).longValue()));
                    }
                }
            }
            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {}
        });
    }

    private void checkFollowStatus() {
        Integer userId = SessionManager.getInstance(this).getUserId();
        if (userId == null) return;
        RetrofitClient.getApiService().checkFollow(userId, artistId).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    isFollowing = Boolean.TRUE.equals(response.body().get("following"));
                    updateFollowButton();
                }
            }
            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {}
        });
    }

    private void toggleFollow() {
        Integer userId = SessionManager.getInstance(this).getUserId();
        if (userId == null) {
            Toast.makeText(this, "Vui lòng đăng nhập để theo dõi", Toast.LENGTH_SHORT).show();
            return;
        }
        Map<String, Integer> payload = new java.util.HashMap<>();
        payload.put("userId", userId);
        payload.put("artistId", artistId);
        
        RetrofitClient.getApiService().toggleFollow(payload).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    isFollowing = Boolean.TRUE.equals(response.body().get("following"));
                    updateFollowButton();
                    Toast.makeText(ArtistDetailActivity.this, isFollowing ? "Đã theo dõi" : "Đã bỏ theo dõi", Toast.LENGTH_SHORT).show();
                }
            }
            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {}
        });
    }

    private void updateFollowButton() {
        if (isFollowing) {
            btnFollow.setText("Đang theo dõi");
            btnFollow.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#333333")));
        } else {
            btnFollow.setText("Theo dõi");
            btnFollow.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#8b5cf6")));
        }
    }

    private void checkMembership() {
        Integer userId = SessionManager.getInstance(this).getUserId();
        if (userId == null) return;
        RetrofitClient.getApiService().checkArtistMembership(userId, artistId).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    boolean active = Boolean.TRUE.equals(response.body().get("active"));
                    if (active) {
                        btnJoinMembership.setVisibility(View.VISIBLE);
                        btnJoinMembership.setText("⭐ Bạn là Fan Cứng");
                        btnJoinMembership.setEnabled(false);
                        btnJoinMembership.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#4CAF50")));
                        btnJoinMembership.setTextColor(Color.WHITE);
                    } else {
                        updateMembershipUI();
                    }
                }
            }
            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {}
        });
    }

    private void updateMembershipUI() {
        if (membershipPrice > 0) {
            btnJoinMembership.setVisibility(View.VISIBLE);
            btnJoinMembership.setText("⭐ Fan Cứng (" + formatCurrency(membershipPrice) + ")");
            btnJoinMembership.setEnabled(true);
        } else {
            btnJoinMembership.setVisibility(View.GONE);
        }
    }

    private void loadUserBalance() {
        RetrofitClient.getApiService().getProfile().enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Object> data = (Map<String, Object>) response.body().get("data");
                    if (data != null && data.containsKey("balance")) {
                        userBalance = ((Number) data.get("balance")).doubleValue();
                    }
                }
            }
            @Override public void onFailure(Call<Map<String, Object>> call, Throwable t) {}
        });
    }

    private void handleMembershipClick() {
        // Refresh balance before showing dialog to be sure
        RetrofitClient.getApiService().getProfile().enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Object> data = (Map<String, Object>) response.body().get("data");
                    if (data != null && data.containsKey("balance")) {
                        userBalance = ((Number) data.get("balance")).doubleValue();
                    }
                }
                showMembershipDialog();
            }
            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                showMembershipDialog();
            }
        });
    }

    private void showMembershipDialog() {
        String message = "Bạn muốn đăng ký gói Fan Cứng " + membershipDuration + " ngày với giá " + formatCurrency(membershipPrice) + "?\n\n"
                       + "Số dư hiện tại: " + formatCurrency(userBalance);

        androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(this);
        builder.setTitle("Tham gia Fan Cứng");
        builder.setMessage(message);

        if (userBalance >= membershipPrice) {
            builder.setPositiveButton("Đăng ký", (dialog, which) -> subscribeMembership());
            builder.setNegativeButton("Hủy", null);
        } else {
            builder.setPositiveButton("Nạp thêm tiền", (dialog, which) -> {
                startActivity(new Intent(ArtistDetailActivity.this, DepositActivity.class));
            });
            builder.setNegativeButton("Đóng", null);
            builder.setMessage(message + "\n\n(Số dư không đủ để thực hiện giao dịch)");
        }
        builder.show();
    }

    private void subscribeMembership() {
        Integer userId = SessionManager.getInstance(this).getUserId();
        if (userId == null) return;
        
        Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("userId", userId);
        payload.put("artistId", artistId);
        
        RetrofitClient.getApiService().subscribeArtist(payload).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(ArtistDetailActivity.this, "Chào mừng bạn gia nhập Fan Cứng!", Toast.LENGTH_LONG).show();
                    checkMembership();
                } else {
                    String errorMsg = "Giao dịch thất bại";
                    try {
                        if (response.errorBody() != null) {
                            String errorStr = response.errorBody().string();
                            org.json.JSONObject json = new org.json.JSONObject(errorStr);
                            if (json.has("message")) {
                                errorMsg = json.getString("message");
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    Toast.makeText(ArtistDetailActivity.this, errorMsg, Toast.LENGTH_LONG).show();
                }
            }
            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {}
        });
    }

    private void playAll() {
        if (!artistSongs.isEmpty()) {
            Song firstSong = artistSongs.get(0);
            com.appad.utils.AccessHelper.checkAccess(this, firstSong, true, () -> {
                MusicPlayerManager.getInstance().setPlaylist(artistSongs, 0);
                startActivity(new Intent(this, FullPlayerActivity.class));
            });
        }
    }

    private String formatCount(long count) {
        return com.appad.utils.FormatUtils.formatCount(count);
    }

    private String formatCurrency(double amount) {
        NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        return formatter.format(amount);
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
