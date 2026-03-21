package com.appad.activities;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.appad.R;
import com.appad.utils.RetrofitClient;
import com.appad.utils.SessionManager;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminActivity extends AppCompatActivity {

    private TextView txtAdminName, txtTotalPlays, txtNewUsers;
    private androidx.swiperefreshlayout.widget.SwipeRefreshLayout swipeRefreshLayout;
    private com.appad.utils.MiniPlayerHelper miniPlayerHelper;
    
    // Stats Panels
    private View panelUsers, panelSongs, panelAlbums, panelAnalytics;
    
    // Menu Cards
    private View menuUsers, menuSongs, menuAlbums, menuReviews, menuDeposits, menuWithdrawals, menuMembership, menuPayout, menuBroadcast, menuApproveArtist;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_dashboard);

        initViews();
        setupClickListeners();
        loadStats();
    }

    private void initViews() {
        txtAdminName = findViewById(R.id.txtAdminName);
        txtTotalPlays = findViewById(R.id.txtTotalPlays);
        txtNewUsers = findViewById(R.id.txtNewUsers);
        swipeRefreshLayout = findViewById(R.id.swipeRefresh);
        
        // Profiles
        String username = SessionManager.getInstance(this).getUsername();
        if (username != null) txtAdminName.setText(username);

        // Panels
        panelUsers = findViewById(R.id.panelUsers);
        panelSongs = findViewById(R.id.panelSongs);
        panelAlbums = findViewById(R.id.panelAlbums);
        panelAnalytics = findViewById(R.id.panelAnalytics);
        
        setupPanel(panelUsers, "Người dùng", R.drawable.ic_people, "#3B82F6");
        setupPanel(panelSongs, "Bài hát", R.drawable.ic_musical_notes, "#10B981");
        setupPanel(panelAlbums, "Album", R.drawable.ic_albums_outline, "#F59E0B");
        setupPanel(panelAnalytics, "Phân tích", android.R.drawable.ic_menu_sort_by_size, "#8B5CF6");

        // Menus
        menuUsers = findViewById(R.id.menuUsers);
        menuSongs = findViewById(R.id.menuSongs);
        menuAlbums = findViewById(R.id.menuAlbums);
        menuReviews = findViewById(R.id.menuReviews);
        menuDeposits = findViewById(R.id.menuDeposits);
        menuWithdrawals = findViewById(R.id.menuWithdrawals);
        menuMembership = findViewById(R.id.menuMembership);
        menuPayout = findViewById(R.id.menuPayout);

        setupMenuCard(menuUsers, "Người dùng", "Ban/Unban, phân quyền tài khoản", R.drawable.ic_people, "#3B82F6");
        setupMenuCard(menuSongs, "Kho bài hát", "Kiểm duyệt và chỉnh sửa", R.drawable.ic_musical_notes, "#10B981");
        setupMenuCard(menuAlbums, "Bộ sưu tập Album", "Quản lý danh sách album", R.drawable.ic_albums_outline, "#F59E0B");
        setupMenuCard(menuReviews, "Quản lý Đánh giá", "Kiểm soát bình luận và rating", R.drawable.ic_star_badge, "#FCD34D");
        setupMenuCard(menuDeposits, "Duyệt nạp tiền", "Yêu cầu nạp từ người dùng", R.drawable.ic_wallet_outline, "#06B6D4");
        setupMenuCard(menuWithdrawals, "Duyệt rút tiền", "Yêu cầu rút từ nghệ sĩ", android.R.drawable.ic_menu_send, "#8B5CF6");
        setupMenuCard(menuMembership, "Gói hội viên", "Thống kê đăng ký thành viên", R.drawable.ic_star, "#EC4899");
        setupMenuCard(menuPayout, "Lương Premium", "Chia doanh thu cho nghệ sĩ", R.drawable.ic_wallet, "#8B5CF6");

        menuBroadcast = findViewById(R.id.menuBroadcast);
        menuApproveArtist = findViewById(R.id.menuApproveArtist);
        setupMenuCard(menuBroadcast, "Thông báo hệ thống", "Gửi tin nhắn đến toàn bộ người dùng", R.drawable.ic_notification, "#6366F1");
        setupMenuCard(menuApproveArtist, "Duyệt nghệ sĩ", "Xem xét các yêu cầu đăng ký nghệ sĩ", android.R.drawable.btn_star_big_on, "#A855F7");
    }

    private void setupPanel(View panel, String label, int iconRes, String colorHex) {
        TextView labelView = panel.findViewById(R.id.txtPanelLabel);
        ImageView iconView = panel.findViewById(R.id.panelIcon);
        FrameLayout iconContainer = panel.findViewById(R.id.iconContainer);
        
        labelView.setText(label);
        iconView.setImageResource(iconRes);
        iconView.setColorFilter(Color.parseColor(colorHex));
        
        String alphaColor = "#15" + colorHex.replace("#", "");
        iconContainer.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor(alphaColor)));
    }

    private void setupMenuCard(View menu, String title, String subtitle, int iconRes, String colorHex) {
        TextView titleView = menu.findViewById(R.id.txtMenuTitle);
        TextView subtitleView = menu.findViewById(R.id.txtMenuSubtitle);
        ImageView iconView = menu.findViewById(R.id.menuIcon);
        View iconContainer = menu.findViewById(R.id.menuIconContainer);
        
        titleView.setText(title);
        subtitleView.setText(subtitle);
        iconView.setImageResource(iconRes);
        iconView.setColorFilter(Color.parseColor(colorHex));
        
        if (iconContainer != null) {
            String alphaColor = "#20" + colorHex.replace("#", "");
            iconContainer.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor(alphaColor)));
        }
    }

    private void setupClickListeners() {
        swipeRefreshLayout.setOnRefreshListener(() -> loadStats());
        panelAnalytics.setOnClickListener(v -> navigateTo("analytics"));
        panelUsers.setOnClickListener(v -> navigateTo("users"));
        menuUsers.setOnClickListener(v -> navigateTo("users"));
        
        panelSongs.setOnClickListener(v -> navigateTo("songs"));
        menuSongs.setOnClickListener(v -> navigateTo("songs"));

        panelAlbums.setOnClickListener(v -> navigateTo("albums"));
        menuAlbums.setOnClickListener(v -> navigateTo("albums"));
        menuReviews.setOnClickListener(v -> navigateTo("reviews"));
        
        menuDeposits.setOnClickListener(v -> navigateTo("deposits"));
        menuWithdrawals.setOnClickListener(v -> navigateTo("withdrawals"));
        menuMembership.setOnClickListener(v -> navigateTo("membership"));
        menuPayout.setOnClickListener(v -> navigateTo("payout"));
        menuBroadcast.setOnClickListener(v -> navigateTo("broadcast"));
        menuApproveArtist.setOnClickListener(v -> navigateTo("approve_artist"));
        
        findViewById(R.id.btnSettings).setOnClickListener(v -> Toast.makeText(this, "Settings", Toast.LENGTH_SHORT).show());
        findViewById(R.id.btnProfile).setOnClickListener(v -> finish());
    }

    private void navigateTo(String target) {
        switch (target) {
            case "deposits":
                // Original functionality of AdminActivity (now Transactions)
                startActivity(new Intent(this, AdminTransactionsActivity.class));
                break;
            case "withdrawals":
                startActivity(new Intent(this, AdminWithdrawalsActivity.class));
                break;
            case "users":
                startActivity(new Intent(this, AdminUsersActivity.class));
                break;
            case "songs":
                startActivity(new Intent(this, AdminSongsActivity.class));
                break;
            case "analytics":
                startActivity(new Intent(this, AdminAnalyticsActivity.class));
                break;
            case "albums":
                startActivity(new Intent(this, AdminAlbumsActivity.class));
                break;
            case "reviews":
                startActivity(new Intent(this, AdminReviewsActivity.class));
                break;
            case "membership":
                startActivity(new Intent(this, AdminMembershipActivity.class));
                break;
            case "payout":
                startActivity(new Intent(this, AdminPremiumPayoutActivity.class));
                break;
            case "broadcast":
                startActivity(new Intent(this, AdminBroadcastActivity.class));
                break;
            case "approve_artist":
                startActivity(new Intent(this, AdminArtistApprovalActivity.class));
                break;
            default:
                Toast.makeText(this, "Tính năng đang hoàn thiện", Toast.LENGTH_SHORT).show();
                break;
        }
    }

    private void loadStats() {
        if (swipeRefreshLayout != null) {
            swipeRefreshLayout.setRefreshing(true);
        }
        RetrofitClient.getApiService().getAdminStats().enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (swipeRefreshLayout != null) {
                    swipeRefreshLayout.setRefreshing(false);
                }
                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Object> body = response.body();
                    if (Boolean.TRUE.equals(body.get("success"))) {
                        Map<String, Object> data = (Map<String, Object>) body.get("data");
                        updateUI(data);
                    }
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                if (swipeRefreshLayout != null) {
                    swipeRefreshLayout.setRefreshing(false);
                }
                Toast.makeText(AdminActivity.this, "Lỗi tải thống kê", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateUI(Map<String, Object> data) {
        if (data == null) return;
        
        long totalPlays = data.get("totalPlays") != null ? ((Number) data.get("totalPlays")).longValue() : 0;
        int newUsers = data.get("newUsersThisMonth") != null ? ((Number) data.get("newUsersThisMonth")).intValue() : 0;
        int totalUsers = data.get("totalUsers") != null ? ((Number) data.get("totalUsers")).intValue() : 0;
        int totalSongs = data.get("totalSongs") != null ? ((Number) data.get("totalSongs")).intValue() : 0;
        int totalAlbums = data.get("totalAlbums") != null ? ((Number) data.get("totalAlbums")).intValue() : 0;
        
        NumberFormat nf = NumberFormat.getInstance(Locale.getDefault());
        txtTotalPlays.setText(nf.format(totalPlays));
        txtNewUsers.setText("+" + nf.format(newUsers));
        
        ((TextView)panelUsers.findViewById(R.id.txtPanelValue)).setText(nf.format(totalUsers));
        ((TextView)panelSongs.findViewById(R.id.txtPanelValue)).setText(nf.format(totalSongs));
        ((TextView)panelAlbums.findViewById(R.id.txtPanelValue)).setText(nf.format(totalAlbums));
        ((TextView)panelAnalytics.findViewById(R.id.txtPanelValue)).setText(nf.format(totalPlays));
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
