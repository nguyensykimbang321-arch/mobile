package com.appad.activities;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.appad.R;
import com.appad.adapters.AdminMembershipAdapter;
import com.appad.utils.RetrofitClient;
import com.google.android.material.chip.ChipGroup;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminMembershipActivity extends AppCompatActivity {

    private RecyclerView rvMembers;
    private SwipeRefreshLayout swipeRefresh;
    private EditText edtSearch;
    private TextView txtActiveCount, txtTotalRevenue, txtEmpty;
    private ChipGroup chipGroupStatus;

    private AdminMembershipAdapter adapter;
    private List<Map<String, Object>> allMembers = new ArrayList<>();
    private List<Map<String, Object>> filteredMembers = new ArrayList<>();
    
    private String currentQuery = "";
    private String currentStatus = "all";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_membership);

        initViews();
        setupListeners();
        loadStats();
        loadMemberships();
    }

    private void initViews() {
        rvMembers = findViewById(R.id.rvAdminMembership);
        swipeRefresh = findViewById(R.id.swipeRefreshMembership);
        edtSearch = findViewById(R.id.edtSearchMembership);
        txtActiveCount = findViewById(R.id.txtActiveMembersCount);
        txtTotalRevenue = findViewById(R.id.txtTotalRevenue);
        txtEmpty = findViewById(R.id.txtEmptyMembership);
        chipGroupStatus = findViewById(R.id.chipGroupStatus);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        rvMembers.setLayoutManager(new LinearLayoutManager(this));
        adapter = new AdminMembershipAdapter(this, filteredMembers);
        rvMembers.setAdapter(adapter);
    }

    private void setupListeners() {
        swipeRefresh.setOnRefreshListener(() -> {
            loadStats();
            loadMemberships();
        });

        edtSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentQuery = s.toString().toLowerCase().trim();
                applyFilters();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        chipGroupStatus.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chipAll) currentStatus = "all";
            else if (checkedId == R.id.chipActive) currentStatus = "active";
            else if (checkedId == R.id.chipExpired) currentStatus = "expired";
            loadMemberships(); // API supports status filtering
        });
    }

    private void loadStats() {
        RetrofitClient.getApiService().getMembershipStats().enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Object> data = (Map<String, Object>) response.body().get("data");
                    if (data != null) {
                        Object activeObj = data.get("active_count");
                        txtActiveCount.setText(activeObj != null ? String.valueOf(((Number)activeObj).intValue()) : "0");
                        
                        double rev = 0;
                        if (data.get("total_revenue") != null) rev = ((Number) data.get("total_revenue")).doubleValue();
                        NumberFormat nf = NumberFormat.getCurrencyInstance(new Locale("vi", "VN"));
                        txtTotalRevenue.setText(nf.format(rev));
                    }
                }
            }
            @Override public void onFailure(Call<Map<String, Object>> call, Throwable t) {}
        });
    }

    private void loadMemberships() {
        swipeRefresh.setRefreshing(true);
        String statusParam = currentStatus.equals("all") ? null : currentStatus;
        RetrofitClient.getApiService().getAllMemberships(100, statusParam, currentQuery).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                swipeRefresh.setRefreshing(false);
                if (response.isSuccessful() && response.body() != null) {
                    Object dataObj = response.body().get("data");
                    allMembers.clear();
                    if (dataObj instanceof List) {
                        // Direct list format
                        allMembers.addAll((List<Map<String, Object>>) dataObj);
                    } else if (dataObj instanceof Map) {
                        // Nested format {data: {memberships: [...]}}
                        List<Map<String, Object>> list = (List<Map<String, Object>>) ((Map)dataObj).get("memberships");
                        if (list != null) allMembers.addAll(list);
                    }
                    applyFilters();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                swipeRefresh.setRefreshing(false);
                Toast.makeText(AdminMembershipActivity.this, "Lỗi tải danh sách hội viên", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void applyFilters() {
        filteredMembers.clear();
        if (currentQuery.isEmpty()) {
            filteredMembers.addAll(allMembers);
        } else {
            for (Map<String, Object> m : allMembers) {
                String username = ((String) m.get("username")).toLowerCase();
                String fullName = m.get("full_name") != null ? ((String) m.get("full_name")).toLowerCase() : "";
                if (username.contains(currentQuery) || fullName.contains(currentQuery)) {
                    filteredMembers.add(m);
                }
            }
        }
        adapter.notifyDataSetChanged();
        txtEmpty.setVisibility(filteredMembers.isEmpty() ? View.VISIBLE : View.GONE);
    }
}
