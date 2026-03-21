package com.appad.components;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.appad.R;
import com.appad.models.Song;
import com.appad.utils.ImageUrlUtils;
import com.bumptech.glide.Glide;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class PlayerActionsBottomSheet extends BottomSheetDialogFragment {

    public interface OnActionClickListener {
        void onActionClick(int actionId);
    }

    private Song song;
    private boolean isFavorite;
    private boolean canDownload;
    private OnActionClickListener listener;

    public static PlayerActionsBottomSheet newInstance(Song song, boolean isFavorite, boolean canDownload, OnActionClickListener listener) {
        PlayerActionsBottomSheet fragment = new PlayerActionsBottomSheet();
        fragment.song = song;
        fragment.isFavorite = isFavorite;
        fragment.canDownload = canDownload;
        fragment.listener = listener;
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NORMAL, R.style.CustomBottomSheetDialogTheme);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.bottom_sheet_player_actions, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        if (song == null) return;

        // Header Info
        TextView txtTitle = view.findViewById(R.id.textSheetSongTitle);
        TextView txtArtist = view.findViewById(R.id.textSheetArtistName);
        ImageView imgCover = view.findViewById(R.id.imgSheetAlbumArt);

        txtTitle.setText(song.getTitle());
        txtArtist.setText(song.getArtistName());
        Glide.with(this)
                .load(ImageUrlUtils.fixUrl(song.getCoverUrl()))
                .placeholder(R.drawable.ic_launcher_background)
                .into(imgCover);

        // Setup Actions
        setupAction(view.findViewById(R.id.itemFavorite), 
                isFavorite ? R.drawable.ic_heart_filled : R.drawable.ic_heart_outline, 
                isFavorite ? "Xóa khỏi yêu thích" : "Thêm vào yêu thích", 
                isFavorite ? 0xFFE91E63 : 0xFFFFFFFF, 1);

        setupAction(view.findViewById(R.id.itemLyrics), R.drawable.ic_musical_notes, "Lời bài hát", 0xFFFFFFFF, 2);
        setupAction(view.findViewById(R.id.itemComments), R.drawable.ic_comment, "Bình luận", 0xFFFFFFFF, 3);
        setupAction(view.findViewById(R.id.itemQueue), android.R.drawable.ic_menu_sort_by_size, "Danh sách phát", 0xFFFFFFFF, 4);
        setupAction(view.findViewById(R.id.itemSleepTimer), android.R.drawable.ic_lock_idle_alarm, "Hẹn giờ tắt", 0xFFFFFFFF, 5);
        setupAction(view.findViewById(R.id.itemAddToPlaylist), R.drawable.ic_add, "Thêm vào Playlist", 0xFFFFFFFF, 6);
        
        View downloadView = view.findViewById(R.id.itemDownload);
        if (canDownload) {
            setupAction(downloadView, android.R.drawable.stat_sys_download, "Tải nhạc", 0xFFFFFFFF, 7);
        } else {
            downloadView.setVisibility(View.GONE);
        }

        setupAction(view.findViewById(R.id.itemReport), android.R.drawable.ic_menu_report_image, "Báo cáo vi phạm", 0xFFFFFFFF, 8);
    }

    private void setupAction(View itemView, int iconRes, String title, int tintColor, int actionId) {
        ImageView icon = itemView.findViewById(R.id.actionIcon);
        TextView text = itemView.findViewById(R.id.actionTitle);

        icon.setImageResource(iconRes);
        icon.setColorFilter(tintColor);
        text.setText(title);

        itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onActionClick(actionId);
            }
            dismiss();
        });
    }
}
