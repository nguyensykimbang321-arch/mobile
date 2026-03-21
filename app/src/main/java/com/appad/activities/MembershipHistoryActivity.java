package com.appad.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.appad.R;
import com.appad.adapters.MembershipAdapter;
import com.appad.models.ArtistMembership;
import com.appad.utils.RetrofitClient;
import com.appad.utils.SessionManager;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MembershipHistoryActivity extends AppCompatActivity {

    private RecyclerView rvMemberships;
    private MembershipAdapter adapter;
    private List<ArtistMembership> membershipList = new ArrayList<>();
    private ProgressBar progressBar;
    private LinearLayout layoutEmpty;
    private com.appad.utils.MiniPlayerHelper miniPlayerHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_membership_history);

        initViews();
        loadMemberships();
    }

    private void initViews() {
        Toolbar toolbar = findViewById(R.id.toolbarMembership);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        rvMemberships = findViewById(R.id.rvMemberships);
        progressBar = findViewById(R.id.progressBar);
        layoutEmpty = findViewById(R.id.layoutEmpty);

        rvMemberships.setLayoutManager(new LinearLayoutManager(this));
        adapter = new MembershipAdapter(this, membershipList);
        rvMemberships.setAdapter(adapter);
    }

    private void loadMemberships() {
        Integer userId = SessionManager.getInstance(this).getUserId();
        if (userId == null) return;

        progressBar.setVisibility(View.VISIBLE);
        RetrofitClient.getApiService().getUserMemberships(userId).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                progressBar.setVisibility(View.GONE);
                if (response.isSuccessful() && response.body() != null) {
                    Object data = response.body().get("data");
                    if (data instanceof List) {
                        Gson gson = new Gson();
                        String json = gson.toJson(data);
                        Type type = new TypeToken<List<ArtistMembership>>(){}.getType();
                        List<ArtistMembership> list = gson.fromJson(json, type);
                        
                        membershipList.clear();
                        membershipList.addAll(list);
                        adapter.notifyDataSetChanged();

                        if (membershipList.isEmpty()) {
                            layoutEmpty.setVisibility(View.VISIBLE);
                        } else {
                            layoutEmpty.setVisibility(View.GONE);
                        }
                    }
                } else {
                    Toast.makeText(MembershipHistoryActivity.this, "Lỗi tải dữ liệu", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                progressBar.setVisibility(View.GONE);
                Toast.makeText(MembershipHistoryActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
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
