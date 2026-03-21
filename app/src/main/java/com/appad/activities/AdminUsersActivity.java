package com.appad.activities;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.appad.R;
import com.appad.adapters.AdminUserAdapter;
import com.appad.utils.RetrofitClient;
import com.google.android.material.chip.ChipGroup;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminUsersActivity extends AppCompatActivity implements AdminUserAdapter.OnUserActionEventListener {

    private RecyclerView rvUsers;
    private SwipeRefreshLayout swipeRefresh;
    private ProgressBar progressBar;
    private TextView txtNoResults;
    private EditText edtSearch;
    private ImageButton btnClearSearch;
    private ChipGroup chipGroupRole;

    private AdminUserAdapter adapter;
    private List<Map<String, Object>> allUsers = new ArrayList<>();
    private List<Map<String, Object>> filteredUsers = new ArrayList<>();

    private String currentQuery = "";
    private String currentRoleFilter = "ALL";
    private com.appad.utils.MiniPlayerHelper miniPlayerHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_users);

        initViews();
        setupListeners();
        loadUsers();
    }

    private void initViews() {
        Toolbar toolbar = findViewById(R.id.toolbarUsers);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        rvUsers = findViewById(R.id.rvAdminUsers);
        swipeRefresh = findViewById(R.id.swipeRefreshUsers);
        progressBar = findViewById(R.id.pbUsers);
        txtNoResults = findViewById(R.id.txtNoUsersFound);
        edtSearch = findViewById(R.id.edtSearchUsers);
        btnClearSearch = findViewById(R.id.btnClearSearch);
        chipGroupRole = findViewById(R.id.chipGroupRole);

        rvUsers.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AdminUserAdapter(this, filteredUsers, this);
        rvUsers.setAdapter(adapter);
    }

    private void setupListeners() {
        swipeRefresh.setOnRefreshListener(this::loadUsers);

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

        chipGroupRole.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chipAll) currentRoleFilter = "ALL";
            else if (checkedId == R.id.chipUser) currentRoleFilter = "USER";
            else if (checkedId == R.id.chipArtist) currentRoleFilter = "ARTIST";
            else if (checkedId == R.id.chipAdmin) currentRoleFilter = "ADMIN";
            else if (checkedId == R.id.chipBanned) currentRoleFilter = "BANNED";
            applyFilters();
        });
    }

    private void loadUsers() {
        swipeRefresh.setRefreshing(true);
        RetrofitClient.getApiService().getAdminUsers().enqueue(new Callback<List<Map<String, Object>>>() {
            @Override
            public void onResponse(Call<List<Map<String, Object>>> call, Response<List<Map<String, Object>>> response) {
                swipeRefresh.setRefreshing(false);
                if (response.isSuccessful() && response.body() != null) {
                    allUsers.clear();
                    allUsers.addAll(response.body());
                    applyFilters();
                }
            }

            @Override
            public void onFailure(Call<List<Map<String, Object>>> call, Throwable t) {
                swipeRefresh.setRefreshing(false);
                Toast.makeText(AdminUsersActivity.this, "Không thể kết nối đến máy chủ", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void applyFilters() {
        filteredUsers.clear();
        for (Map<String, Object> user : allUsers) {
            boolean matchesQuery = true;
            boolean matchesRole = true;

            // Search Filter
            if (!currentQuery.isEmpty()) {
                String username = ((String) user.get("username")).toLowerCase();
                String email = ((String) user.get("email")).toLowerCase();
                String fullName = user.get("full_name") != null ? ((String) user.get("full_name")).toLowerCase() : "";
                if (fullName.isEmpty() && user.get("fullName") != null) fullName = ((String) user.get("fullName")).toLowerCase();
                
                matchesQuery = username.contains(currentQuery) || email.contains(currentQuery) || fullName.contains(currentQuery);
            }

            // Role Filter
            String role = ((String) user.get("role")).toUpperCase();
            Integer isBanned = user.get("isBanned") != null ? ((Number) user.get("isBanned")).intValue() : 0;
            
            if (!currentRoleFilter.equals("ALL")) {
                if (currentRoleFilter.equals("BANNED")) {
                    matchesRole = isBanned == 1 || role.equals("BANNED");
                } else {
                    matchesRole = role.equals(currentRoleFilter);
                }
            }

            if (matchesQuery && matchesRole) {
                filteredUsers.add(user);
            }
        }
        
        adapter.notifyDataSetChanged();
        txtNoResults.setVisibility(filteredUsers.isEmpty() ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onManageUser(Map<String, Object> user) {
        Integer id = ((Number) user.get("user_id")).intValue();
        String role = (String) user.get("role");
        Integer isBanned = user.get("isBanned") != null ? ((Number) user.get("isBanned")).intValue() : 0;
        
        boolean currentlyBanned = isBanned == 1 || "BANNED".equals(role);
        String actionText = currentlyBanned ? "Mở khóa tài khoản" : "Khóa tài khoản";
        
        String[] options = {actionText, "Thay đổi quyền (Role)", "Xem chi tiết"};
        
        new AlertDialog.Builder(this)
                .setTitle("Quản lý: " + user.get("username"))
                .setItems(options, (dialog, which) -> {
                    switch (which) {
                        case 0: // Ban/Unban
                            if (currentlyBanned) unban(id);
                            else ban(id);
                            break;
                        case 1: // Change Role
                            showChangeRoleDialog(user);
                            break;
                        case 2: // View Details
                            Toast.makeText(this, "Tính năng đang phát triển", Toast.LENGTH_SHORT).show();
                            break;
                    }
                })
                .show();
    }

    private void showChangeRoleDialog(Map<String, Object> user) {
        String[] roles = {"USER", "ARTIST", "ADMIN"};
        Integer id = ((Number) user.get("user_id")).intValue();
        
        new AlertDialog.Builder(this)
                .setTitle("Thay đổi quyền")
                .setItems(roles, (dialog, which) -> {
                    String newRole = roles[which];
                    updateRole(id, newRole);
                })
                .show();
    }

    private void updateRole(Integer userId, String newRole) {
        java.util.HashMap<String, Object> payload = new java.util.HashMap<>();
        payload.put("role", newRole);
        
        RetrofitClient.getApiService().updateUserRole(userId, payload).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(AdminUsersActivity.this, "Cập nhật quyền thành công", Toast.LENGTH_SHORT).show();
                    loadUsers();
                } else {
                    Toast.makeText(AdminUsersActivity.this, "Lỗi cập nhật quyền", Toast.LENGTH_SHORT).show();
                }
            }
            @Override public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Toast.makeText(AdminUsersActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void ban(Integer id) {
        RetrofitClient.getApiService().banUser(id).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(AdminUsersActivity.this, "Đã khóa tài khoản thành công", Toast.LENGTH_SHORT).show();
                    loadUsers();
                }
            }
            @Override public void onFailure(Call<Map<String, Object>> call, Throwable t) {}
        });
    }

    private void unban(Integer id) {
        RetrofitClient.getApiService().unbanUser(id).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(AdminUsersActivity.this, "Đã mở khóa tài khoản thành công", Toast.LENGTH_SHORT).show();
                    loadUsers();
                }
            }
            @Override public void onFailure(Call<Map<String, Object>> call, Throwable t) {}
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
