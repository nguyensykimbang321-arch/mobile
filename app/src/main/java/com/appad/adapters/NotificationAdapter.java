package com.appad.adapters;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.widget.ImageViewCompat;
import androidx.recyclerview.widget.RecyclerView;
import com.appad.R;
import com.appad.models.Notification;
import java.util.List;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {

    private List<Notification> notifications;
    private OnNotificationClickListener listener;

    public interface OnNotificationClickListener {
        void onNotificationClick(Notification notification);
        void onNotificationLongClick(Notification notification);
    }

    public NotificationAdapter(List<Notification> notifications, OnNotificationClickListener listener) {
        this.notifications = notifications;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        try {
            Notification notification = notifications.get(position);
            Context context = holder.itemView.getContext();
            
            holder.txtTitle.setText(notification.getTitle() != null ? notification.getTitle() : "Thông báo");
            holder.txtMessage.setText(notification.getMessage() != null ? notification.getMessage() : "");
            holder.txtTime.setText(formatTime(notification.getCreatedAt()));
            
            boolean isRead = notification.isRead();
            if (holder.viewUnread != null) {
                holder.viewUnread.setVisibility(isRead ? View.GONE : View.VISIBLE);
            }
            
            // Background tint for unread
            View cardLayout = holder.itemView.findViewById(R.id.cardLayout);
            if (cardLayout != null) {
                if (!isRead) {
                    cardLayout.setBackgroundColor(Color.parseColor("#156366F1"));
                } else {
                    cardLayout.setBackgroundColor(Color.TRANSPARENT);
                }
            }

            // Icon logic based on type (Parity with React Native)
            String type = notification.getType();
            int iconRes = R.drawable.ic_notification;
            String colorHex = "#64748B";
            String bgHex = "#1594A3B8";

            if ("new_song".equals(type) || "new_album".equals(type)) {
                iconRes = R.drawable.ic_musical_notes;
                colorHex = "#A78BFA";
                bgHex = "#15A78BFA";
            } else if ("new_follower".equals(type)) {
                iconRes = R.drawable.ic_people;
                colorHex = "#34D399";
                bgHex = "#1534D399";
            } else if (type != null && (type.contains("approved") || type.contains("success"))) {
                iconRes = android.R.drawable.checkbox_on_background;
                colorHex = "#34D399";
                bgHex = "#1534D399";
            } else if (type != null && (type.contains("rejected") || type.contains("cancel"))) {
                iconRes = android.R.drawable.ic_dialog_alert;
                colorHex = "#F87171";
                bgHex = "#15F87171";
            } else if ("revenue".equals(type)) {
                iconRes = R.drawable.ic_wallet;
                colorHex = "#34D399";
                bgHex = "#1534D399";
            } else if ("spend".equals(type)) {
                iconRes = R.drawable.ic_wallet_outline;
                colorHex = "#FBBF24";
                bgHex = "#15FBBF24";
            } else if ("system".equals(type)) {
                iconRes = android.R.drawable.ic_menu_info_details;
                colorHex = "#60A5FA";
                bgHex = "#1560A5FA";
            }

            if (holder.imgIcon != null) {
                holder.imgIcon.setImageResource(iconRes);
                
                // Safe color filtering
                try {
                    int mainColor = Color.parseColor(colorHex);
                    int bgColor = Color.parseColor(bgHex);
                    
                    holder.imgIcon.setColorFilter(mainColor);
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                        holder.imgIcon.setBackgroundTintList(ColorStateList.valueOf(bgColor));
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            holder.itemView.setOnClickListener(v -> {
                if (listener != null) listener.onNotificationClick(notification);
            });
            
            holder.itemView.setOnLongClickListener(v -> {
                if (listener != null) listener.onNotificationLongClick(notification);
                return true;
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String formatTime(String createdAt) {
        if (createdAt == null) return "";
        try {
            // Very simple formatter for now, similar to React Native
            String clean = createdAt.replace("T", " ");
            if (clean.length() > 16) return clean.substring(0, 16);
            return clean;
        } catch (Exception e) {
            return createdAt;
        }
    }

    @Override
    public int getItemCount() {
        return notifications != null ? notifications.size() : 0;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txtTitle, txtMessage, txtTime;
        ImageView imgIcon;
        View viewUnread;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtTitle = itemView.findViewById(R.id.txtNotificationTitle);
            txtMessage = itemView.findViewById(R.id.txtNotificationMessage);
            txtTime = itemView.findViewById(R.id.txtNotificationTime);
            imgIcon = itemView.findViewById(R.id.imgNotificationIcon);
            viewUnread = itemView.findViewById(R.id.unreadDot);
        }
    }
}
