package com.appad.activities;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.appad.R;
import com.appad.utils.RetrofitClient;
import com.appad.utils.SessionManager;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ArtistDashboardActivity extends AppCompatActivity {

    private ImageButton btnBack, btnRefresh, btnWithdrawHistory;
    private Button btnWithdraw;
    private TextView txtWalletBalance, txtPendingAmount;
    private TextView txtStatSongs, txtStatPurchases, txtStatListens;
    private TextView txtNoRevenue, btnRevenueDetail;
    private LinearLayout revenueContainer;
    private FrameLayout loadingOverlay;
    private com.appad.utils.MiniPlayerHelper miniPlayerHelper;

    private Integer artistId;
    private NumberFormat currencyFormatter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_artist_dashboard);

        currencyFormatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
        
        initViews();
        setupListeners();
        setupActionItems();
        
        loadingOverlay.setVisibility(View.VISIBLE);
        loadArtistData();
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        btnRefresh = findViewById(R.id.btnRefresh);
        btnWithdrawHistory = findViewById(R.id.btnWithdrawHistory);
        btnWithdraw = findViewById(R.id.btnWithdraw);
        
        txtWalletBalance = findViewById(R.id.txtWalletBalance);
        txtPendingAmount = findViewById(R.id.txtPendingAmount);
        
        txtStatSongs = findViewById(R.id.txtStatSongs);
        txtStatPurchases = findViewById(R.id.txtStatPurchases);
        txtStatListens = findViewById(R.id.txtStatListens);
        
        txtNoRevenue = findViewById(R.id.txtNoRevenue);
        btnRevenueDetail = findViewById(R.id.btnRevenueDetail);
        revenueContainer = findViewById(R.id.revenueContainer);
        
        loadingOverlay = findViewById(R.id.loadingOverlay);
        
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());
        
        btnRefresh.setOnClickListener(v -> {
            loadingOverlay.setVisibility(View.VISIBLE);
            loadArtistData();
        });
        
        btnWithdraw.setOnClickListener(v -> 
            startActivity(new Intent(this, WithdrawActivity.class)));
        
        btnWithdrawHistory.setOnClickListener(v -> 
            startActivity(new Intent(this, TransactionHistoryActivity.class)));
        
        btnRevenueDetail.setOnClickListener(v -> 
            Toast.makeText(this, "Chi tiết doanh thu", Toast.LENGTH_SHORT).show());
    }

    private void setupActionItems() {
        // Songs
        View actionSongs = findViewById(R.id.actionSongs);
        setupAction(actionSongs, "Bài hát", R.drawable.ic_mic_badge, "#3B82F6", 
            v -> {
                Intent intent = new Intent(this, ManageSongsActivity.class);
                if (artistId != null) intent.putExtra("ARTIST_ID", artistId);
                startActivity(intent);
            });
        
        // Albums
        View actionAlbums = findViewById(R.id.actionAlbums);
        setupAction(actionAlbums, "Album", R.drawable.ic_disc_badge, "#A855F7",
            v -> {
                Intent intent = new Intent(this, ManageAlbumsActivity.class);
                if (artistId != null) intent.putExtra("ARTIST_ID", artistId);
                startActivity(intent);
            });
        
        // Reviews
        View actionReviews = findViewById(R.id.actionReviews);
        setupAction(actionReviews, "Đánh giá", R.drawable.ic_star_badge, "#FCD34D",
            v -> {
                Intent intent = new Intent(this, ArtistReviewsActivity.class);
                if (artistId != null) {
                    intent.putExtra("ARTIST_ID", artistId);
                    startActivity(intent);
                } else {
                    Toast.makeText(this, "Đang tải thông tin nghệ sĩ...", Toast.LENGTH_SHORT).show();
                }
            });
        
        // Profile
        View actionProfile = findViewById(R.id.actionProfile);
        setupAction(actionProfile, "Hồ sơ", android.R.drawable.ic_menu_edit, "#EC4899",
            v -> startActivity(new Intent(this, EditProfileActivity.class)));
        
        // Membership
        View actionMembership = findViewById(R.id.actionMembership);
        setupAction(actionMembership, "Hội viên", R.drawable.ic_people_badge, "#F59E0B",
            v -> startActivity(new Intent(this, ManageMembershipActivity.class)));
        
        // Bank
        View actionBank = findViewById(R.id.actionBank);
        setupAction(actionBank, "Ngân hàng", android.R.drawable.ic_menu_agenda, "#10B981",
            v -> Toast.makeText(this, "Thông tin ngân hàng", Toast.LENGTH_SHORT).show());
        
        // History
        View actionHistory = findViewById(R.id.actionHistory);
        setupAction(actionHistory, "Lịch sử rút", android.R.drawable.ic_menu_recent_history, "#64748B",
            v -> Toast.makeText(this, "Lịch sử rút tiền", Toast.LENGTH_SHORT).show());
        
    }

    private void setupAction(View actionView, String label, int iconRes, String colorHex, View.OnClickListener listener) {
        if (actionView == null) return;
        
        TextView labelView = actionView.findViewById(R.id.actionLabel);
        ImageView iconView = actionView.findViewById(R.id.actionIcon);
        FrameLayout iconContainer = actionView.findViewById(R.id.actionIconContainer);
        
        if (labelView != null) labelView.setText(label);
        if (iconView != null) {
            iconView.setImageResource(iconRes);
            iconView.setColorFilter(Color.parseColor(colorHex));
        }
        if (iconContainer != null) {
            String alphaColor = "#15" + colorHex.replace("#", "");
            iconContainer.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                Color.parseColor(alphaColor)));
        }
        
        actionView.setOnClickListener(listener);
    }

    private void loadArtistData() {
        Integer userId = SessionManager.getInstance(this).getUserId();
        if (userId == null) {
            loadingOverlay.setVisibility(View.GONE);
            return;
        }

        RetrofitClient.getApiService().getArtistByUserId(userId).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                loadingOverlay.setVisibility(View.GONE);
                
                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Object> data = (Map<String, Object>) response.body().get("data");
                    if (data != null) {
                        artistId = data.get("artist_id") != null ? ((Number) data.get("artist_id")).intValue() : null;
                        updateDashboard(data);
                        
                        // Load additional dashboard data
                        if (artistId != null) {
                            loadDashboardStats();
                        }
                    }
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                loadingOverlay.setVisibility(View.GONE);
                Toast.makeText(ArtistDashboardActivity.this, "Không thể tải dữ liệu", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateDashboard(Map<String, Object> data) {
        // Wallet Balance
        double balance = 0;
        if (data.get("wallet_balance") != null) {
            balance = ((Number) data.get("wallet_balance")).doubleValue();
        }
        txtWalletBalance.setText(formatCurrency(balance));
        
        // Pending Amount (simulated for now)
        txtPendingAmount.setText("0đ");
    }

    private void loadDashboardStats() {
        RetrofitClient.getApiService().getArtistDashboard(artistId).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Object> body = response.body();
                    if (Boolean.TRUE.equals(body.get("success"))) {
                        Map<String, Object> data = (Map<String, Object>) body.get("data");
                        if (data != null) {
                            updateStats(data);
                        }
                    }
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                // Stats loading failed silently
            }
        });
    }

    private void updateStats(Map<String, Object> data) {
        // Stats
        Map<String, Object> stats = (Map<String, Object>) data.get("stats");
        if (stats != null) {
            int songs = stats.get("total_songs") != null ? ((Number) stats.get("total_songs")).intValue() : 0;
            int purchases = stats.get("total_purchases") != null ? ((Number) stats.get("total_purchases")).intValue() : 0;
            int listens = stats.get("total_listens") != null ? ((Number) stats.get("total_listens")).intValue() : 0;
            
            txtStatSongs.setText(String.valueOf(songs));
            txtStatPurchases.setText(String.valueOf(purchases));
            txtStatListens.setText(formatNumber(listens));
        }
        
        // Wallet
        Map<String, Object> wallet = (Map<String, Object>) data.get("wallet");
        if (wallet != null) {
            double balance = wallet.get("balance") != null ? ((Number) wallet.get("balance")).doubleValue() : 0;
            txtWalletBalance.setText(formatCurrency(balance));
        }
        
        // Unpaid
        Map<String, Object> unpaid = (Map<String, Object>) data.get("unpaid");
        if (unpaid != null) {
            double unpaidAmount = unpaid.get("unpaid_amount") != null ? ((Number) unpaid.get("unpaid_amount")).doubleValue() : 0;
            txtPendingAmount.setText(formatCurrency(unpaidAmount));
        }
        
        // Revenue Stats
        List<Map<String, Object>> revenueStats = (List<Map<String, Object>>) data.get("revenue_stats");
        if (revenueStats != null && !revenueStats.isEmpty()) {
            txtNoRevenue.setVisibility(View.GONE);
            displayRevenueItems(revenueStats);
        } else {
            txtNoRevenue.setVisibility(View.VISIBLE);
        }
    }

    private void displayRevenueItems(List<Map<String, Object>> revenueStats) {
        // Clear existing items except the empty text
        for (int i = revenueContainer.getChildCount() - 1; i >= 0; i--) {
            View child = revenueContainer.getChildAt(i);
            if (child.getId() != R.id.txtNoRevenue) {
                revenueContainer.removeViewAt(i);
            }
        }
        
        for (Map<String, Object> stat : revenueStats) {
            View itemView = getLayoutInflater().inflate(R.layout.item_revenue_stat, revenueContainer, false);
            
            TextView txtType = itemView.findViewById(R.id.txtRevenueType);
            TextView txtCount = itemView.findViewById(R.id.txtRevenueCount);
            TextView txtAmount = itemView.findViewById(R.id.txtRevenueAmount);
            ImageView revenueIcon = itemView.findViewById(R.id.revenueIcon);
            FrameLayout revenueIconContainer = itemView.findViewById(R.id.revenueIconContainer);
            
            String shareType = (String) stat.get("share_type");
            String typeLabel;
            int iconRes;
            String colorHex;

            if ("direct_purchase".equals(shareType)) {
                typeLabel = "Mua bài hát";
                iconRes = R.drawable.ic_mic_badge;
                colorHex = "#3B82F6"; // Blue
            } else if ("album_purchase".equals(shareType)) {
                typeLabel = "Mua album";
                iconRes = R.drawable.ic_disc_badge;
                colorHex = "#A855F7"; // Purple
            } else {
                typeLabel = "Premium Stream";
                iconRes = android.R.drawable.ic_media_play;
                colorHex = "#F59E0B"; // Orange
            }
            
            int count = stat.get("count") != null ? ((Number) stat.get("count")).intValue() : 0;
            double amount = stat.get("total_artist_share") != null ? ((Number) stat.get("total_artist_share")).doubleValue() : 0;
            
            txtType.setText(typeLabel);
            txtCount.setText(count + " giao dịch thành công");
            txtAmount.setText("+" + formatCurrency(amount));

            if (revenueIcon != null) {
                revenueIcon.setImageResource(iconRes);
                revenueIcon.setColorFilter(Color.parseColor(colorHex));
            }
            if (revenueIconContainer != null) {
                String alphaColor = "#15" + colorHex.replace("#", "");
                revenueIconContainer.setBackgroundTintList(android.content.res.ColorStateList.valueOf(
                    Color.parseColor(alphaColor)));
            }
            
            revenueContainer.addView(itemView, 0); // Add to top
        }
    }

    private String formatCurrency(double amount) {
        return String.format(Locale.getDefault(), "%,.0fđ", amount);
    }

    private String formatNumber(int num) {
        if (num >= 1000) {
            return String.format(Locale.getDefault(), "%.1fK", num / 1000.0);
        }
        return String.valueOf(num);
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
