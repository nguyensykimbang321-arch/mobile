package com.appad.components;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.appad.R;
import com.appad.models.Song;
import com.appad.utils.MusicPlayerManager;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;

public class MoreActionsFragment extends BottomSheetDialogFragment {

    private TextView txtTitle;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.layout_more_actions_sheet, container, false);
        txtTitle = view.findViewById(R.id.txtMoreSongTitle);
        
        Song current = MusicPlayerManager.getInstance().getCurrentSong();
        if (current != null) {
            txtTitle.setText(current.getTitle());
        }

        view.findViewById(R.id.btnAddToPlaylist).setOnClickListener(v -> {
            dismiss();
            if (current != null) {
                AddToPlaylistFragment.newInstance(current.getSongId())
                        .show(getParentFragmentManager(), "AddToPlaylist");
            }
        });

        view.findViewById(R.id.btnViewArtist).setOnClickListener(v -> {
            dismiss();
            if (current != null && current.getArtistId() != null) {
                Intent intent = new Intent(getContext(), com.appad.activities.ArtistDetailActivity.class);
                intent.putExtra("artistId", current.getArtistId());
                startActivity(intent);
            }
        });

        view.findViewById(R.id.btnViewAlbum).setOnClickListener(v -> {
            dismiss();
            if (current != null && current.getAlbumId() != null) {
                Intent intent = new Intent(getContext(), com.appad.activities.AlbumDetailActivity.class);
                intent.putExtra("albumId", current.getAlbumId());
                startActivity(intent);
            } else {
                Toast.makeText(getContext(), "Bài hát này không thuộc Album nào", Toast.LENGTH_SHORT).show();
            }
        });

        view.findViewById(R.id.btnReport).setOnClickListener(v -> {
            dismiss();
            if (current != null) {
                ReportDialogFragment.newInstance(current.getSongId(), "song")
                        .show(getParentFragmentManager(), "ReportDialog");
            }
        });

        return view;
    }
}
