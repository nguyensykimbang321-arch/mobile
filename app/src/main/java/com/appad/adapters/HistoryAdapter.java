package com.appad.adapters;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.appad.R;
import com.appad.activities.FullPlayerActivity;
import com.appad.models.Song;
import com.appad.utils.ImageUrlUtils;
import com.appad.utils.MusicPlayerManager;
import com.bumptech.glide.Glide;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class HistoryAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_SONG = 1;

    private Context context;
    private List<Object> items = new ArrayList<>();

    public HistoryAdapter(Context context, List<Map<String, Object>> data) {
        this.context = context;
        setData(data);
    }

    public void setData(List<Map<String, Object>> data) {
        items.clear();
        for (Map<String, Object> dayGroup : data) {
            items.add(dayGroup); // Header
            List<Map<String, Object>> songs = (List<Map<String, Object>>) dayGroup.get("songs");
            if (songs != null) {
                items.addAll(songs);
            }
        }
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return (items.get(position) instanceof Map && ((Map) items.get(position)).containsKey("day")) ? TYPE_HEADER : TYPE_SONG;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_HEADER) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_history_header, parent, false);
            return new HeaderViewHolder(view);
        } else {
            View view = LayoutInflater.from(context).inflate(R.layout.item_history_song, parent, false);
            return new SongViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (getItemViewType(position) == TYPE_HEADER) {
            HeaderViewHolder vh = (HeaderViewHolder) holder;
            Map<String, Object> day = (Map<String, Object>) items.get(position);
            vh.txtDayTitle.setText(formatDate((String) day.get("day")));
            vh.txtDaySubtitle.setText(((Number) day.get("song_count")).intValue() + " bài hát");
        } else {
            SongViewHolder vh = (SongViewHolder) holder;
            Map<String, Object> songMap = (Map<String, Object>) items.get(position);
            
            vh.txtSongTitle.setText((String) songMap.get("title"));
            vh.txtSongArtist.setText((String) songMap.get("artist_name"));
            vh.txtHistoryCount.setText(((Number) songMap.get("count")).intValue() + " lần");
            
            int count = ((Number) songMap.get("count")).intValue();
            int completed = ((Number) songMap.get("completed_count")).intValue();
            int totalDuration = songMap.containsKey("total_duration") ? ((Number) songMap.get("total_duration")).intValue() : 0;
            
            // Format duration to mm:ss
            int minutes = totalDuration / 60;
            int seconds = totalDuration % 60;
            vh.txtDuration.setText(String.format("%d:%02d", minutes, seconds));
            
            if (completed == count && count > 0) {
                vh.txtStatusSeparator.setVisibility(View.VISIBLE);
                vh.imgStatus.setVisibility(View.VISIBLE);
                vh.txtHistoryStatus.setVisibility(View.VISIBLE);
                vh.txtHistoryStatus.setText("Hoàn thành");
                vh.imgStatus.setImageResource(android.R.drawable.checkbox_on_background);
            } else {
                vh.txtStatusSeparator.setVisibility(View.GONE);
                vh.imgStatus.setVisibility(View.GONE);
                vh.txtHistoryStatus.setVisibility(View.GONE);
            }

            Glide.with(context)
                    .load(ImageUrlUtils.fixUrl((String) songMap.get("cover_url")))
                    .placeholder(R.drawable.ic_launcher_background)
                    .into(vh.imgSongCover);

            vh.itemView.setOnClickListener(v -> {
                // Play song logic
                Song song = new com.google.gson.Gson().fromJson(new com.google.gson.Gson().toJson(songMap), Song.class);
                com.appad.utils.AccessHelper.checkAccess(context, song, true, new com.appad.utils.AccessHelper.AccessCallback() {
                    @Override
                    public void onAccessGranted() {
                        MusicPlayerManager.getInstance().setPlaylist(java.util.Collections.singletonList(song), 0);
                        context.startActivity(new Intent(context, FullPlayerActivity.class));
                    }
                    @Override
                    public void onAccessDenied(Song song) {
                        // Modal is shown by access helper
                    }
                });
            });
        }
    }

    private String formatDate(String dateStr) {
        try {
            java.text.SimpleDateFormat parser = new java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault());
            java.util.Date date = parser.parse(dateStr);
            if (date == null) return dateStr;

            java.util.Calendar cal = java.util.Calendar.getInstance();
            // Check today
            String todayStr = parser.format(cal.getTime());
            if (dateStr.equals(todayStr)) return "Hôm nay";
            
            // Check yesterday
            cal.add(java.util.Calendar.DAY_OF_YEAR, -1);
            String yesterdayStr = parser.format(cal.getTime());
            if (dateStr.equals(yesterdayStr)) return "Hôm qua";
            
            java.text.SimpleDateFormat formatter = new java.text.SimpleDateFormat("EEEE, dd MMMM yyyy", new java.util.Locale("vi", "VN"));
            return formatter.format(date);
        } catch (Exception e) {
            return dateStr;
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        TextView txtDayTitle, txtDaySubtitle;
        HeaderViewHolder(View itemView) {
            super(itemView);
            txtDayTitle = itemView.findViewById(R.id.txtDayTitle);
            txtDaySubtitle = itemView.findViewById(R.id.txtDaySubtitle);
        }
    }

    static class SongViewHolder extends RecyclerView.ViewHolder {
        ImageView imgSongCover, imgStatus, btnPlay;
        TextView txtSongTitle, txtSongArtist, txtHistoryCount, txtHistoryStatus, txtDuration, txtStatusSeparator;
        SongViewHolder(View itemView) {
            super(itemView);
            imgSongCover = itemView.findViewById(R.id.imgSongCover);
            imgStatus = itemView.findViewById(R.id.imgStatus);
            btnPlay = itemView.findViewById(R.id.btnPlay);
            txtSongTitle = itemView.findViewById(R.id.txtSongTitle);
            txtSongArtist = itemView.findViewById(R.id.txtSongArtist);
            txtHistoryCount = itemView.findViewById(R.id.txtHistoryCount);
            txtHistoryStatus = itemView.findViewById(R.id.txtHistoryStatus);
            txtDuration = itemView.findViewById(R.id.txtDuration);
            txtStatusSeparator = itemView.findViewById(R.id.txtStatusSeparator);
        }
    }
}
