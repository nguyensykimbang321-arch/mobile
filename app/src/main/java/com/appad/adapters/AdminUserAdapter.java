package com.appad.adapters;

import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.appad.R;
import com.appad.utils.ImageUrlUtils;
import com.bumptech.glide.Glide;

import java.util.List;
import java.util.Map;

public class AdminUserAdapter extends RecyclerView.Adapter<AdminUserAdapter.ViewHolder> {

    private Context context;
    private List<Map<String, Object>> users;
    private OnUserActionEventListener listener;

    public interface OnUserActionEventListener {
        void onManageUser(Map<String, Object> user);
    }

    public AdminUserAdapter(Context context, List<Map<String, Object>> users, OnUserActionEventListener listener) {
        this.context = context;
        this.users = users;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_admin_user, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Map<String, Object> user = users.get(position);
        
        String username = (String) user.get("username");
        String fullName = (String) user.get("full_name");
        if (fullName == null) fullName = (String) user.get("fullName");
        
        holder.txtName.setText(fullName != null && !fullName.isEmpty() ? fullName : username);
        holder.txtEmail.setText((String) user.get("email"));
        
        String avatarUrl = (String) user.get("avatar_url");
        if (avatarUrl == null) avatarUrl = (String) user.get("avatarUrl");
        
        Glide.with(context)
                .load(ImageUrlUtils.fixUrl(avatarUrl))
                .placeholder(R.drawable.ic_nav_profile)
                .circleCrop()
                .into(holder.imgAvatar);

        String role = ((String) user.get("role")).toUpperCase();
        holder.txtRole.setText(role);
        
        switch (role) {
            case "ADMIN":
                holder.txtRole.setBackgroundResource(R.drawable.bg_role_badge_admin);
                break;
            case "ARTIST":
                holder.txtRole.setBackgroundResource(R.drawable.bg_role_badge_artist);
                break;
            default:
                holder.txtRole.setBackgroundResource(R.drawable.bg_role_badge_user);
                break;
        }

        Integer isBanned = user.get("isBanned") != null ? ((Number) user.get("isBanned")).intValue() : 0;
        if (isBanned == 1 || "BANNED".equals(role)) {
            holder.txtStatus.setText("Đã bị khóa");
            holder.txtStatus.setTextColor(Color.parseColor("#EF4444"));
        } else if (isBanned == 2) {
            holder.txtStatus.setText("Chờ duyệt nghệ sĩ");
            holder.txtStatus.setTextColor(Color.parseColor("#F59E0B"));
        } else {
            holder.txtStatus.setText("Đang hoạt động");
            holder.txtStatus.setTextColor(Color.parseColor("#10B981"));
        }

        holder.btnManage.setOnClickListener(v -> {
            if (listener != null) listener.onManageUser(user);
        });
    }

    @Override
    public int getItemCount() {
        return users.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgAvatar;
        TextView txtName, txtEmail, txtRole, txtStatus;
        View btnManage;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgAvatar = itemView.findViewById(R.id.imgUserAvatar);
            txtName = itemView.findViewById(R.id.txtUserName);
            txtEmail = itemView.findViewById(R.id.txtUserEmail);
            txtRole = itemView.findViewById(R.id.txtUserRole);
            txtStatus = itemView.findViewById(R.id.txtUserStatus);
            btnManage = itemView.findViewById(R.id.btnActionManage);
        }
    }
}
