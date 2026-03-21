package com.appad.activities;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.appad.R;
import com.appad.models.User;
import com.appad.utils.RetrofitClient;
import com.appad.utils.ImageUrlUtils;
import com.bumptech.glide.Glide;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AdminArtistApprovalActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private SwipeRefreshLayout swipeRefresh;
    private TextView txtEmpty;
    private ApprovalAdapter adapter;
    private List<User> pendingUsers = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_artist_approval);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        recyclerView = findViewById(R.id.recyclerView);
        swipeRefresh = findViewById(R.id.swipeRefresh);
        txtEmpty = findViewById(R.id.txtEmpty);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ApprovalAdapter();
        recyclerView.setAdapter(adapter);

        swipeRefresh.setOnRefreshListener(this::loadPendingArtists);
        loadPendingArtists();
    }

    private void loadPendingArtists() {
        swipeRefresh.setRefreshing(true);
        RetrofitClient.getApiService().getPendingArtists().enqueue(new Callback<List<User>>() {
            @Override
            public void onResponse(Call<List<User>> call, Response<List<User>> response) {
                swipeRefresh.setRefreshing(false);
                if (response.isSuccessful() && response.body() != null) {
                    pendingUsers = response.body();
                    adapter.notifyDataSetChanged();
                    txtEmpty.setVisibility(pendingUsers.isEmpty() ? View.VISIBLE : View.GONE);
                }
            }

            @Override
            public void onFailure(Call<List<User>> call, Throwable t) {
                swipeRefresh.setRefreshing(false);
                Toast.makeText(AdminArtistApprovalActivity.this, "Lỗi tải dữ liệu", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private class ApprovalAdapter extends RecyclerView.Adapter<ApprovalAdapter.ViewHolder> {

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_pending_artist, parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            User user = pendingUsers.get(position);
            holder.txtName.setText(user.getFullName() != null && !user.getFullName().isEmpty() ? user.getFullName() : user.getUsername());
            holder.txtEmail.setText(user.getEmail());
            
            if (user.getAvatarUrl() != null) {
                Glide.with(holder.itemView.getContext())
                        .load(ImageUrlUtils.fixUrl(user.getAvatarUrl()))
                        .circleCrop()
                        .into(holder.imgAvatar);
            }

            holder.btnApprove.setOnClickListener(v -> handleApproval(user, true));
            holder.btnReject.setOnClickListener(v -> handleApproval(user, false));
        }

        @Override
        public int getItemCount() {
            return pendingUsers.size();
        }

        private void handleApproval(User user, boolean approved) {
            Call<Map<String, Object>> call = approved ? 
                    RetrofitClient.getApiService().approveArtist(user.getUserId()) : 
                    RetrofitClient.getApiService().rejectArtist(user.getUserId());

            call.enqueue(new Callback<Map<String, Object>>() {
                @Override
                public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                    if (response.isSuccessful()) {
                        Toast.makeText(AdminArtistApprovalActivity.this, approved ? "Đã duyệt nghệ sĩ" : "Đã từ chối yêu cầu", Toast.LENGTH_SHORT).show();
                        loadPendingArtists();
                    }
                }
                @Override public void onFailure(Call<Map<String, Object>> call, Throwable t) {}
            });
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            ImageView imgAvatar;
            TextView txtName, txtEmail;
            ImageButton btnApprove, btnReject;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                imgAvatar = itemView.findViewById(R.id.imgAvatar);
                txtName = itemView.findViewById(R.id.txtName);
                txtEmail = itemView.findViewById(R.id.txtEmail);
                btnApprove = itemView.findViewById(R.id.btnApprove);
                btnReject = itemView.findViewById(R.id.btnReject);
            }
        }
    }
}
