package com.appad.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.appad.R;
import com.appad.adapters.NotificationAdapter;
import com.appad.models.Notification;
import com.appad.utils.RetrofitClient;
import com.appad.utils.SessionManager;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class NotificationActivity extends AppCompatActivity implements NotificationAdapter.OnNotificationClickListener {

    private RecyclerView rvNotifications;
    private NotificationAdapter adapter;
    private List<Notification> notificationList = new ArrayList<>();
    private SwipeRefreshLayout swipeRefresh;
    private LinearLayout layoutEmpty;
    private ImageButton btnMarkAllRead, btnDeleteAll;
    private View btnAdminCreate;
    private boolean isAdmin = false;
    private com.appad.utils.MiniPlayerHelper miniPlayerHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.activity_notification);
            initViews();
            checkUserRole();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadNotifications();
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

    private void initViews() {
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbarNotification);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            toolbar.setNavigationOnClickListener(v -> finish());
        }

        // Try to find views from toolbar first if direct find fails
        btnMarkAllRead = findViewById(R.id.btnMarkAllRead);
        if (btnMarkAllRead == null && toolbar != null) btnMarkAllRead = toolbar.findViewById(R.id.btnMarkAllRead);
        
        btnDeleteAll = findViewById(R.id.btnDeleteAll);
        if (btnDeleteAll == null && toolbar != null) btnDeleteAll = toolbar.findViewById(R.id.btnDeleteAll);
        
        btnAdminCreate = findViewById(R.id.btnAdminCreate);
        if (btnAdminCreate == null && toolbar != null) btnAdminCreate = toolbar.findViewById(R.id.btnAdminCreate);
        
        layoutEmpty = findViewById(R.id.layoutEmpty);

        rvNotifications = findViewById(R.id.rvNotifications);
        rvNotifications.setLayoutManager(new LinearLayoutManager(this));
        
        adapter = new NotificationAdapter(notificationList, this);
        rvNotifications.setAdapter(adapter);

        swipeRefresh = findViewById(R.id.swipeRefresh);
        swipeRefresh.setOnRefreshListener(this::loadNotifications);

        if (btnMarkAllRead != null) btnMarkAllRead.setOnClickListener(v -> markAllAsRead());
        if (btnDeleteAll != null) btnDeleteAll.setOnClickListener(v -> deleteAllNotifications());
        if (btnAdminCreate != null) btnAdminCreate.setOnClickListener(v -> showCreateNotificationDialog());
    }

    private void checkUserRole() {
        SessionManager sm = SessionManager.getInstance(this);
        if (sm == null) return;
        
        String role = sm.getRole();
        isAdmin = "admin".equalsIgnoreCase(role);
        if (btnAdminCreate != null) {
            btnAdminCreate.setVisibility(isAdmin ? View.VISIBLE : View.GONE);
        }
        
        // Always refresh role from server in case it changed
        RetrofitClient.getApiService().getProfile().enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    Map<String, Object> data = (Map<String, Object>) response.body().get("data");
                    if (data != null && data.containsKey("role")) {
                        String r = (String) data.get("role");
                        isAdmin = "admin".equalsIgnoreCase(r);
                        if (btnAdminCreate != null) {
                            btnAdminCreate.setVisibility(isAdmin ? View.VISIBLE : View.GONE);
                        }
                    }
                }
            }
            @Override public void onFailure(Call<Map<String, Object>> call, Throwable t) {}
        });
    }

    private void loadNotifications() {
        if (swipeRefresh != null) swipeRefresh.setRefreshing(true);
        RetrofitClient.getApiService().getNotifications().enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
                if (response.isSuccessful() && response.body() != null) {
                    try {
                        Map<String, Object> dataObj = (Map<String, Object>) response.body().get("data");
                        if (dataObj == null) return;
                        
                        List<Map<String, Object>> list = (List<Map<String, Object>>) dataObj.get("notifications");
                        Object unreadObj = dataObj.get("unread_count");
                        int unreadCount = 0;
                        if (unreadObj instanceof Number) unreadCount = ((Number) unreadObj).intValue();

                        notificationList.clear();
                        if (list != null) {
                            Gson gson = new Gson();
                            for (Map<String, Object> map : list) {
                                Notification n = new Notification();
                                Object idObj = map.get("notification_id");
                                if (idObj == null) idObj = map.get("notificationId");
                                if (idObj instanceof Number) n.setNotificationId(((Number) idObj).longValue());
                                 
                                n.setTitle((String) map.get("title"));
                                n.setMessage((String) map.get("message"));
                                n.setType((String) map.get("type"));
                                 
                                // Handle both "read" and "isRead" keys
                                Object readVal = map.get("is_read");
                                if (readVal == null) readVal = map.get("read");
                                if (readVal == null) readVal = map.get("isRead");
                                n.setRead(Boolean.TRUE.equals(readVal));
                                 
                                String createdVal = (String) map.get("created_at");
                                if (createdVal == null) createdVal = (String) map.get("createdAt");
                                n.setCreatedAt(createdVal);
                                
                                Object d = map.get("data");
                                if (d instanceof String) n.setData((String) d);
                                else if (d != null) n.setData(gson.toJson(d));
                                notificationList.add(n);
                            }
                        }
                        adapter.notifyDataSetChanged();
                        
                        if (layoutEmpty != null) layoutEmpty.setVisibility(notificationList.isEmpty() ? View.VISIBLE : View.GONE);
                        if (btnMarkAllRead != null) btnMarkAllRead.setVisibility(unreadCount > 0 ? View.VISIBLE : View.GONE);
                        if (btnDeleteAll != null) btnDeleteAll.setVisibility(!notificationList.isEmpty() ? View.VISIBLE : View.GONE);
                    } catch (Exception e) {
                        e.printStackTrace();
                        Toast.makeText(NotificationActivity.this, "Lỗi xử lý dữ liệu", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
                Toast.makeText(NotificationActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void markAllAsRead() {
        RetrofitClient.getApiService().markAllRead().enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful()) {
                    loadNotifications();
                    Toast.makeText(NotificationActivity.this, "Đã đánh dấu tất cả là đã đọc", Toast.LENGTH_SHORT).show();
                }
            }
            @Override public void onFailure(Call<Map<String, Object>> call, Throwable t) {}
        });
    }

    private void deleteAllNotifications() {
        new AlertDialog.Builder(this)
            .setTitle("Xóa tất cả")
            .setMessage("Bạn có chắc muốn xóa toàn bộ thông báo?")
            .setPositiveButton("Xóa", (dialog, which) -> {
                RetrofitClient.getApiService().deleteAllNotifications().enqueue(new Callback<Map<String, Object>>() {
                    @Override
                    public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                        if (response.isSuccessful()) {
                            notificationList.clear();
                            adapter.notifyDataSetChanged();
                            if (layoutEmpty != null) layoutEmpty.setVisibility(View.VISIBLE);
                            if (btnMarkAllRead != null) btnMarkAllRead.setVisibility(View.GONE);
                            if (btnDeleteAll != null) btnDeleteAll.setVisibility(View.GONE);
                            Toast.makeText(NotificationActivity.this, "Đã xóa toàn bộ thông báo", Toast.LENGTH_SHORT).show();
                        }
                    }
                    @Override public void onFailure(Call<Map<String, Object>> call, Throwable t) {}
                });
            })
            .setNegativeButton("Hủy", null)
            .show();
    }

    @Override
    public void onNotificationClick(Notification notification) {
        if (!notification.isRead()) {
            // Optimistic update
            notification.setRead(true);
            adapter.notifyDataSetChanged();
            
            RetrofitClient.getApiService().markAsRead(notification.getNotificationId()).enqueue(new Callback<Map<String, Object>>() {
                @Override
                public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                    System.out.println("NotificationActivity: markAsRead response code = " + response.code() + " body = " + response.body());
                }
                @Override 
                public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                    System.out.println("NotificationActivity: markAsRead call failed: " + t.getMessage());
                    t.printStackTrace();
                }
            });
        }
        
        navigateToNotification(notification);
    }

    @Override
    public void onNotificationLongClick(Notification notification) {
        new AlertDialog.Builder(this)
            .setTitle("Xóa thông báo")
            .setMessage("Bạn có chắc muốn xóa thông báo này?")
            .setPositiveButton("Xóa", (dialog, which) -> {
                RetrofitClient.getApiService().deleteNotification(notification.getNotificationId()).enqueue(new Callback<Map<String, Object>>() {
                    @Override
                    public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                        if (response.isSuccessful()) {
                            notificationList.remove(notification);
                            adapter.notifyDataSetChanged();
                            if (notificationList.isEmpty() && layoutEmpty != null) layoutEmpty.setVisibility(View.VISIBLE);
                        }
                    }
                    @Override public void onFailure(Call<Map<String, Object>> call, Throwable t) {}
                });
            })
            .setNegativeButton("Hủy", null)
            .show();
    }

    private void navigateToNotification(Notification notification) {
        String type = notification.getType();
        String dataJson = notification.getData();
        if (type == null) return;

        try {
            JsonObject data = new Gson().fromJson(dataJson, JsonObject.class);
            if (data == null) data = new JsonObject();

            switch (type) {
                case "new_song":
                case "new_album":
                case "new_follower":
                case "revenue":
                case "spend":
                    int artistIdForNav = -1;
                    if (data.has("artist_id")) artistIdForNav = data.get("artist_id").getAsInt();
                    else if (data.has("artistId")) artistIdForNav = data.get("artistId").getAsInt();

                    if (type.equals("spend") || type.equals("revenue")) {
                        if (data.has("type") && "artist_membership".equals(data.get("type").getAsString()) && artistIdForNav != -1) {
                            Intent intent = new Intent(this, ArtistDetailActivity.class);
                            intent.putExtra("artistId", artistIdForNav);
                            startActivity(intent);
                        } else {
                            startActivity(new Intent(this, WalletActivity.class));
                        }
                    } else if (artistIdForNav != -1) {
                        Intent intent = new Intent(this, ArtistDetailActivity.class);
                        intent.putExtra("artistId", artistIdForNav);
                        startActivity(intent);
                    }
                    break;

                case "withdrawal_approved":
                case "withdrawal_rejected":
                case "deposit_approved":
                case "deposit_rejected":
                    startActivity(new Intent(this, TransactionHistoryActivity.class));
                    break;

                case "system":
                    if (data.has("action")) {
                        String action = data.get("action").getAsString();
                        if ("approve_deposit".equals(action)) startActivity(new Intent(this, AdminTransactionsActivity.class));
                        else if ("approve_withdrawal".equals(action)) startActivity(new Intent(this, AdminWithdrawalsActivity.class));
                        else if ("approve_artist".equals(action)) startActivity(new Intent(this, AdminUsersActivity.class));
                    }
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void showCreateNotificationDialog() {
        try {
            View view = LayoutInflater.from(this).inflate(R.layout.dialog_create_notification, null);
            EditText etTitle = view.findViewById(R.id.etNotificationTitle);
            EditText etMessage = view.findViewById(R.id.etNotificationMessage);

            new AlertDialog.Builder(this)
                .setTitle("Gửi thông báo hệ thống")
                .setView(view)
                .setPositiveButton("Gửi", (dialog, which) -> {
                    String title = etTitle.getText().toString().trim();
                    String message = etMessage.getText().toString().trim();

                    if (title.isEmpty() || message.isEmpty()) {
                        Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    Map<String, String> payload = new HashMap<>();
                    payload.put("title", title);
                    payload.put("message", message);

                    RetrofitClient.getApiService().broadcastNotification(payload).enqueue(new Callback<Map<String, Object>>() {
                        @Override
                        public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                            if (response.isSuccessful()) {
                                Toast.makeText(NotificationActivity.this, "Đã gửi thông báo", Toast.LENGTH_SHORT).show();
                                loadNotifications();
                            } else {
                                Toast.makeText(NotificationActivity.this, "Lỗi gửi thông báo", Toast.LENGTH_SHORT).show();
                            }
                        }
                        @Override public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                            Toast.makeText(NotificationActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("Hủy", null)
                .show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Lỗi mở dialog", Toast.LENGTH_SHORT).show();
        }
    }
}
