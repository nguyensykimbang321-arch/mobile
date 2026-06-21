package com.appad.fragments;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.appad.R;
import com.appad.activities.AdminActivity;
import com.appad.activities.ArtistDashboardActivity;
import com.appad.activities.ChangePasswordActivity;
import com.appad.activities.DepositActivity;
import com.appad.activities.EditProfileActivity;
import com.appad.activities.LoginActivity;
import com.appad.activities.MembershipHistoryActivity;
import com.appad.activities.PremiumActivity;
import com.appad.activities.WalletActivity;
import com.appad.utils.RetrofitClient;
import com.appad.utils.SessionManager;
import com.bumptech.glide.Glide;
import java.text.NumberFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ProfileFragment extends Fragment {

    private ImageView imgAvatar, imgPremiumCrown;
    private TextView txtName, txtEmail, txtRoleBadge;
    private TextView txtStatFollowing, txtStatFollowingLabel;
    private TextView txtStatFollowers, txtStatFollowersLabel;
    private TextView txtStatPlaylists, txtStatPlaylistsLabel;
    private TextView txtBalanceSubtitle, txtPremiumSubtitle, txtApplyArtistTitle;
    private com.google.android.material.tabs.TabLayout tabLayout;
    private View layoutMenu, layoutDownloads, layoutDownloadsEmpty;
    private androidx.recyclerview.widget.RecyclerView rvDownloads;

    // Menu Buttons
    private LinearLayout btnEditProfile, btnSettings;
    private LinearLayout btnWallet, btnPremium, btnMembershipHistory;
    private LinearLayout btnArtistDashboard, btnAdminDashboard, btnApplyArtist, btnHelp;
    private View sepArtist, sepApplyArtist, lblDashboard;
    private LinearLayout btnLogout;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);
        initViews(view);
        setupListeners();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        loadUserData();
    }

    private void initViews(View view) {
        // Header
        imgAvatar = view.findViewById(R.id.imgProfileAvatar);
        imgPremiumCrown = view.findViewById(R.id.imgPremiumCrown);
        txtName = view.findViewById(R.id.txtProfileName);
        txtEmail = view.findViewById(R.id.txtProfileEmail);
        txtRoleBadge = view.findViewById(R.id.txtRoleBadge);

        // Stats
        txtStatFollowing = view.findViewById(R.id.txtStatFollowing);
        txtStatFollowers = view.findViewById(R.id.txtStatFollowers);
        txtStatPlaylists = view.findViewById(R.id.txtStatPlaylists);

        // Menu Sections
        btnEditProfile = view.findViewById(R.id.btnEditProfile);
        btnSettings = view.findViewById(R.id.btnSettings);
        
        btnWallet = view.findViewById(R.id.btnWallet);
        btnPremium = view.findViewById(R.id.btnPremium);
        btnMembershipHistory = view.findViewById(R.id.btnMembershipHistory);
        txtBalanceSubtitle = view.findViewById(R.id.txtBalanceSubtitle);
        txtPremiumSubtitle = view.findViewById(R.id.txtPremiumSubtitle);
        txtApplyArtistTitle = view.findViewById(R.id.txtApplyArtistTitle);

        lblDashboard = view.findViewById(R.id.lblDashboard);
        btnArtistDashboard = view.findViewById(R.id.btnArtistDashboard);
        btnAdminDashboard = view.findViewById(R.id.btnAdminDashboard);
        sepArtist = view.findViewById(R.id.sepArtist);
        btnApplyArtist = view.findViewById(R.id.btnApplyArtist);
        sepApplyArtist = view.findViewById(R.id.sepApplyArtist);
        btnHelp = view.findViewById(R.id.btnHelp);

        btnLogout = view.findViewById(R.id.btnLogout);

        tabLayout = view.findViewById(R.id.tabLayoutProfile);
        layoutMenu = view.findViewById(R.id.layoutProfileMenu);
        layoutDownloads = view.findViewById(R.id.layoutDownloads);
        layoutDownloadsEmpty = view.findViewById(R.id.layoutDownloadsEmpty);
        rvDownloads = view.findViewById(R.id.rvDownloads);
    }

    private void setupListeners() {
        btnEditProfile.setOnClickListener(v -> startActivity(new Intent(getContext(), EditProfileActivity.class)));
        btnSettings.setOnClickListener(v -> startActivity(new Intent(getContext(), ChangePasswordActivity.class))); // Placeholder for Settings
        
        btnWallet.setOnClickListener(v -> startActivity(new Intent(getContext(), WalletActivity.class))); 
        btnPremium.setOnClickListener(v -> startActivity(new Intent(getContext(), PremiumActivity.class)));
        btnMembershipHistory.setOnClickListener(v -> startActivity(new Intent(getContext(), MembershipHistoryActivity.class)));
        
        btnArtistDashboard.setOnClickListener(v -> startActivity(new Intent(getContext(), ArtistDashboardActivity.class)));
        btnAdminDashboard.setOnClickListener(v -> startActivity(new Intent(getContext(), AdminActivity.class)));
        btnApplyArtist.setOnClickListener(v -> handleApplyArtist());

        btnHelp.setOnClickListener(v -> showHelpDialog());
        
        btnLogout.setOnClickListener(v -> handleLogout());

        tabLayout.addOnTabSelectedListener(new com.google.android.material.tabs.TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(com.google.android.material.tabs.TabLayout.Tab tab) {
                if (tab.getPosition() == 0) {
                    layoutMenu.setVisibility(View.VISIBLE);
                    layoutDownloads.setVisibility(View.GONE);
                } else {
                    layoutMenu.setVisibility(View.GONE);
                    layoutDownloads.setVisibility(View.VISIBLE);
                    loadLocalDownloads();
                }
            }
            @Override public void onTabUnselected(com.google.android.material.tabs.TabLayout.Tab tab) {}
            @Override public void onTabReselected(com.google.android.material.tabs.TabLayout.Tab tab) {}
        });
    }

    private void handleLogout() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(getContext());
        builder.setTitle("Đăng xuất");
        builder.setMessage("Bạn có chắc chắn muốn đăng xuất?");
        builder.setPositiveButton("Đăng xuất", (dialog, which) -> {
            com.appad.utils.MusicPlayerManager.getInstance().recordCurrentSongHistory();
            com.appad.utils.MusicPlayerManager.getInstance().stopMusic();
            SessionManager.getInstance(getContext()).logout();
            Intent intent = new Intent(getContext(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });
        builder.setNegativeButton("Hủy", null);
        builder.show();
    }

    private void loadUserData() {
        RetrofitClient.getApiService().getProfile().enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (isAdded() && response.isSuccessful() && response.body() != null) {
                    Map<String, Object> data = (Map<String, Object>) response.body().get("data");
                    if (data != null) {
                        updateUI(data);
                    }
                }
            }
            @Override public void onFailure(Call<Map<String, Object>> call, Throwable t) {}
        });
    }

    private void updateUI(Map<String, Object> data) {
        String fullName = (String) (data.get("full_name") != null ? data.get("full_name") : data.get("fullName"));
        String username = (String) data.get("username");
        String email = (String) data.get("email");
        String avatarUrl = (String) (data.get("avatar_url") != null ? data.get("avatar_url") : data.get("avatarUrl"));
        String role = (String) data.get("role");
        Integer isBanned = data.get("isBanned") != null ? ((Number) data.get("isBanned")).intValue() : 0;
        
        // ID handling
        Integer userId = null;
        if (data.containsKey("userId")) userId = ((Number) data.get("userId")).intValue();
        else if (data.containsKey("user_id")) userId = ((Number) data.get("user_id")).intValue();

        // 1. Basic Info
        txtName.setText(fullName != null && !fullName.isEmpty() ? fullName : username);
        txtEmail.setText(email);

        // 2. Avatar
        Glide.with(this)
            .load(com.appad.utils.ImageUrlUtils.fixUrl(avatarUrl))
            .placeholder(R.drawable.ic_launcher_background)
            .error(R.drawable.ic_launcher_background)
            .circleCrop()
            .into(imgAvatar);

        // 3. Role Badge & Dashboard Visibility
        updateRoleUI(role, isBanned);

        // 4. Premium Status
        boolean isPremium = false;
        if (data.containsKey("is_premium")) {
            Object pre = data.get("is_premium");
            isPremium = (pre instanceof Number && ((Number)pre).intValue() == 1) || Boolean.TRUE.equals(pre);
        }
        
        imgPremiumCrown.setVisibility(isPremium ? View.VISIBLE : View.GONE);
        if (isPremium) {
            txtPremiumSubtitle.setText("Bạn đang sử dụng Premium");
            txtPremiumSubtitle.setTextColor(Color.parseColor("#F59E0B"));
        } else {
            txtPremiumSubtitle.setText("Nâng cấp trải nghiệm âm nhạc");
            txtPremiumSubtitle.setTextColor(Color.parseColor("#888888"));
        }

        // 5. Balance
        if (data.containsKey("walletBalance") || data.containsKey("balance")) {
             double balance = 0;
             if (data.get("walletBalance") != null) balance = ((Number) data.get("walletBalance")).doubleValue();
             else if (data.get("balance") != null) balance = ((Number) data.get("balance")).doubleValue();
             
             NumberFormat formatter = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
             txtBalanceSubtitle.setText("Số dư: " + formatter.format(balance));
        }

        // 6. Load Stats
        if (userId != null) loadStats(userId);
    }

    private void updateRoleUI(String role, int isBanned) {
        if (role == null) role = "user";
        
        // Reset visibilities
        btnArtistDashboard.setVisibility(View.GONE);
        btnAdminDashboard.setVisibility(View.GONE);
        btnApplyArtist.setVisibility(View.GONE);
        sepArtist.setVisibility(View.GONE);
        sepApplyArtist.setVisibility(View.GONE);
        lblDashboard.setVisibility(View.GONE);

        switch (role.toLowerCase()) {
            case "admin":
                txtRoleBadge.setText("ADMIN");
                txtRoleBadge.setTextColor(Color.parseColor("#EF4444")); // Red
                btnAdminDashboard.setVisibility(View.VISIBLE);
                lblDashboard.setVisibility(View.VISIBLE);
                break;
            case "artist":
                txtRoleBadge.setText("ARTIST");
                txtRoleBadge.setTextColor(Color.parseColor("#8B5CF6")); // Purple
                btnArtistDashboard.setVisibility(View.VISIBLE);
                lblDashboard.setVisibility(View.VISIBLE);
                break;
            case "user":
            default:
                if (isBanned == 2) {
                    txtRoleBadge.setText("PENDING");
                    txtRoleBadge.setTextColor(Color.parseColor("#F59E0B")); // Amber
                    lblDashboard.setVisibility(View.VISIBLE);
                    btnApplyArtist.setVisibility(View.VISIBLE);
                    if (txtApplyArtistTitle != null) {
                        txtApplyArtistTitle.setText("Đang chờ duyệt...");
                    }
                    btnApplyArtist.setEnabled(false);
                } else {
                    txtRoleBadge.setText("USER");
                    txtRoleBadge.setTextColor(Color.parseColor("#3B82F6")); // Blue
                    btnApplyArtist.setVisibility(View.VISIBLE);
                    lblDashboard.setVisibility(View.VISIBLE);
                }
                break;
        }
    }

    private void handleApplyArtist() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(getContext());
        builder.setTitle("Trở thành Nghệ sĩ");
        builder.setMessage("Bạn muốn đăng ký trở thành nghệ sĩ để đăng tải âm nhạc của riêng mình?");
        builder.setPositiveButton("Đăng ký", (dialog, which) -> {
            RetrofitClient.getApiService().applyArtist().enqueue(new Callback<Map<String, Object>>() {
                @Override
                public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                    if (response.isSuccessful()) {
                        Toast.makeText(getContext(), "Gửi yêu cầu thành công, vui lòng chờ duyệt!", Toast.LENGTH_LONG).show();
                        loadUserData(); // Reload
                    } else {
                        Toast.makeText(getContext(), "Lỗi: " + response.message(), Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                    Toast.makeText(getContext(), "Lỗi kết nối", Toast.LENGTH_SHORT).show();
                }
            });
        });
        builder.setNegativeButton("Hủy", null);
        builder.show();
    }

    private void showHelpDialog() {
        if (getContext() == null) return;
        View dialogView = LayoutInflater.from(getContext()).inflate(R.layout.dialog_help_support, null);
        
        android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(getContext())
                .setView(dialogView)
                .create();
        
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        View btnFacebook = dialogView.findViewById(R.id.btnHelpFacebook);
        View btnEmail = dialogView.findViewById(R.id.btnHelpEmail);
        View btnClose = dialogView.findViewById(R.id.btnHelpClose);

        if (btnFacebook != null) {
            btnFacebook.setOnClickListener(v -> {
                dialog.dismiss();
                try {
                    Intent intent = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://www.facebook.com/share/1JsN4J2huN/"));
                    startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(getContext(), "Không thể mở liên kết Facebook", Toast.LENGTH_SHORT).show();
                }
            });
        }

        if (btnEmail != null) {
            btnEmail.setOnClickListener(v -> {
                dialog.dismiss();
                try {
                    Intent intent = new Intent(Intent.ACTION_SENDTO);
                    intent.setData(android.net.Uri.parse("mailto:"));
                    intent.putExtra(Intent.EXTRA_EMAIL, new String[]{"nguyensykimbang324@gmail.com"});
                    intent.putExtra(Intent.EXTRA_SUBJECT, "[APPAD] Yêu cầu hỗ trợ & phản hồi");
                    startActivity(Intent.createChooser(intent, "Gửi email bằng..."));
                } catch (Exception e) {
                    Toast.makeText(getContext(), "Không tìm thấy ứng dụng gửi email", Toast.LENGTH_SHORT).show();
                }
            });
        }

        if (btnClose != null) {
            btnClose.setOnClickListener(v -> dialog.dismiss());
        }

        dialog.show();
    }

    private void loadStats(Integer userId) {
        RetrofitClient.getApiService().getUserStats(userId).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (isAdded() && response.isSuccessful() && response.body() != null) {
                    Map<String, Object> body = response.body();
                    if (Boolean.TRUE.equals(body.get("success"))) {
                        Map<String, Object> stats = (Map<String, Object>) body.get("data");
                        if (stats != null) {
                            Number following = (Number) (stats.get("following") != null ? stats.get("following") : 0);
                            Number followers = (Number) (stats.get("followers") != null ? stats.get("followers") : 0);
                            Number playlists = (Number) (stats.get("playlists") != null ? stats.get("playlists") : 0);
                            
                            txtStatFollowing.setText(String.valueOf(following.intValue()));
                            txtStatFollowers.setText(String.valueOf(followers.intValue()));
                            txtStatPlaylists.setText(String.valueOf(playlists.intValue()));
                        }
                    }
                }
            }
            @Override public void onFailure(Call<Map<String, Object>> call, Throwable t) {}
        });
    }

    private void loadLocalDownloads() {
        List<com.appad.models.Song> songs = com.appad.utils.DownloadHelper.getInstance(getContext()).getDownloadedSongs();
        if (songs.isEmpty()) {
            layoutDownloadsEmpty.setVisibility(View.VISIBLE);
            rvDownloads.setVisibility(View.GONE);
        } else {
            layoutDownloadsEmpty.setVisibility(View.GONE);
            rvDownloads.setVisibility(View.VISIBLE);
            rvDownloads.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(getContext()));
            rvDownloads.setAdapter(new DownloadAdapter(songs));
        }
    }

    private class DownloadAdapter extends androidx.recyclerview.widget.RecyclerView.Adapter<DownloadAdapter.ViewHolder> {
        List<com.appad.models.Song> items;
        DownloadAdapter(List<com.appad.models.Song> items) { this.items = items; }

        @NonNull @Override public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_download_row, parent, false);
            return new ViewHolder(v);
        }

        @Override public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            com.appad.models.Song song = items.get(position);
            holder.txtTitle.setText(song.getTitle());
            holder.txtArtist.setText(song.getArtistName());
            
            Glide.with(holder.itemView)
                .load(com.appad.utils.ImageUrlUtils.fixUrl(song.getCoverUrl()))
                .centerCrop()
                .placeholder(R.drawable.ic_launcher_background)
                .into(holder.imgCover);
            
            holder.btnDelete.setOnClickListener(v -> {
                new android.app.AlertDialog.Builder(getContext())
                    .setTitle("Xóa bản tải xuống")
                    .setMessage("Bạn có chắc chắn muốn xóa bài hát '" + song.getTitle() + "' khỏi bộ nhớ máy không?")
                    .setPositiveButton("Xóa", (dialog, which) -> {
                        com.appad.utils.DownloadHelper.getInstance(getContext()).removeDownload(song.getSongId(), true);
                        loadLocalDownloads();
                        Toast.makeText(getContext(), " Đã xóa khỏi bộ nhớ", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Hủy", null)
                    .show();
            });

            holder.itemView.setOnClickListener(v -> {
                // Play local file
                com.appad.utils.MusicPlayerManager.getInstance().setPlaylist(items, position);
                startActivity(new Intent(getContext(), com.appad.activities.FullPlayerActivity.class));
            });
        }

        @Override public int getItemCount() { return items.size(); }

        class ViewHolder extends androidx.recyclerview.widget.RecyclerView.ViewHolder {
            ImageView imgCover, btnDelete;
            TextView txtTitle, txtArtist;
            ViewHolder(View v) {
                super(v);
                imgCover = v.findViewById(R.id.imgDownloadCover);
                txtTitle = v.findViewById(R.id.txtDownloadTitle);
                txtArtist = v.findViewById(R.id.txtDownloadArtist);
                btnDelete = v.findViewById(R.id.btnDeleteDownload);
            }
        }
    }

    public void refreshData() {
        if (isAdded()) {
            loadUserData();
        }
    }
}
