package com.appad.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.appad.R;
import com.bumptech.glide.Glide;
import java.util.List;
import java.util.Map;

public class CommentsAdapter extends RecyclerView.Adapter<CommentsAdapter.CommentViewHolder> {

    public interface OnCommentActionListener {
        void onDeleteComment(long commentId);
    }

    private Context context;
    private List<Map<String, Object>> commentList;
    private Integer currentUserId;
    private OnCommentActionListener actionListener;

    public CommentsAdapter(Context context, List<Map<String, Object>> commentList, Integer currentUserId, OnCommentActionListener actionListener) {
        this.context = context;
        this.commentList = commentList;
        this.currentUserId = currentUserId;
        this.actionListener = actionListener;
    }

    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_comment, parent, false);
        return new CommentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        Map<String, Object> comment = commentList.get(position);
        
        // Handle nested user object if provided by API
        Map<String, Object> user = (Map<String, Object>) comment.get("user");
        Integer commentUserId = null;

        if (user != null) {
            Object uId = user.get("userId") != null ? user.get("userId") : user.get("user_id");
            if (uId instanceof Number) commentUserId = ((Number) uId).intValue();

            String username = (String) user.get("username");
            if (username == null) username = (String) user.get("full_name");
            holder.txtName.setText(username != null ? username : "Người dùng");
            
            String avatarUrl = (String) (user.get("avatar_url") != null ? user.get("avatar_url") : user.get("avatarUrl"));
            Glide.with(context)
                    .load(com.appad.utils.ImageUrlUtils.fixUrl(avatarUrl))
                    .placeholder(R.drawable.ic_launcher_background)
                    .circleCrop()
                    .into(holder.imgAvatar);
        } else {
            holder.txtName.setText("Ẩn danh");
            holder.imgAvatar.setImageResource(R.drawable.ic_launcher_background);
        }

        holder.txtContent.setText((String) comment.get("content"));

        // Show rating if exists
        Object ratingObj = comment.get("rating");
        if (ratingObj instanceof Number) {
            int rating = ((Number) ratingObj).intValue();
            if (rating > 0) {
                StringBuilder stars = new StringBuilder();
                for (int i = 0; i < rating; i++) stars.append("⭐");
                holder.txtName.append(" " + stars.toString());
            }
        }
        
        // Time formatting
        Object createdAt = comment.get("created_at") != null ? comment.get("created_at") : comment.get("createdAt");
        if (createdAt != null) {
            holder.txtTime.setText(formatDate(createdAt.toString()));
        } else {
            holder.txtTime.setText("Vừa xong");
        }

        // Show delete button if it's user's own comment
        if (currentUserId != null && currentUserId.equals(commentUserId)) {
            holder.btnDelete.setVisibility(View.VISIBLE);
            holder.btnDelete.setOnClickListener(v -> {
                Object cId = comment.get("commentId") != null ? comment.get("commentId") : comment.get("comment_id");
                if (cId instanceof Number && actionListener != null) {
                    actionListener.onDeleteComment(((Number) cId).longValue());
                }
            });
        } else {
            holder.btnDelete.setVisibility(View.GONE);
        }
    }

    private String formatDate(String dateStr) {
        try {
            // Handled by ISO 8601 usually
            if (dateStr.contains("T")) {
                String date = dateStr.split("T")[0];
                String time = dateStr.split("T")[1].substring(0, 5);
                return date + " " + time;
            }
        } catch (Exception e) {}
        return "Vừa xong";
    }

    @Override
    public int getItemCount() {
        return commentList.size();
    }

    public static class CommentViewHolder extends RecyclerView.ViewHolder {
        ImageView imgAvatar;
        TextView txtName, txtTime, txtContent;
        View btnDelete;

        public CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            imgAvatar = itemView.findViewById(R.id.imgCommentAvatar);
            txtName = itemView.findViewById(R.id.txtCommentUserName);
            txtTime = itemView.findViewById(R.id.txtCommentTime);
            txtContent = itemView.findViewById(R.id.txtCommentContent);
            btnDelete = itemView.findViewById(R.id.btnDeleteComment);
        }
    }
}
