package com.appad.components;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.appad.R;
import com.appad.models.Song;
import com.appad.utils.MusicPlayerManager;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import java.util.List;

public class QueueFragment extends BottomSheetDialogFragment {

    private RecyclerView rvQueue;
    private QueueAdapter adapter;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.layout_queue_sheet, container, false);
        rvQueue = view.findViewById(R.id.rvQueue);
        rvQueue.setLayoutManager(new LinearLayoutManager(getContext()));
        
        // Lấy playlist từ singleton (Lưu ý: cần thêm getter cho playlist trong MusicPlayerManager nếu chưa có)
        // Hiện tại dùng tạm List rỗng nếu chưa public
        List<Song> queue = MusicPlayerManager.getInstance().getPlaylist();
        
        adapter = new QueueAdapter(queue);
        rvQueue.setAdapter(adapter);

        return view;
    }

    private class QueueAdapter extends RecyclerView.Adapter<QueueAdapter.VH> {
        private List<Song> songs;

        QueueAdapter(List<Song> songs) {
            this.songs = songs;
        }

        @NonNull
        @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_queue_song, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            Song song = songs.get(position);
            holder.title.setText(song.getTitle());
            holder.artist.setText(song.getArtistName());
            
            Song current = MusicPlayerManager.getInstance().getCurrentSong();
            boolean isPlaying = current != null && current.getSongId().equals(song.getSongId());
            
            holder.imgPlaying.setVisibility(isPlaying ? View.VISIBLE : View.INVISIBLE);
            holder.title.setTextColor(isPlaying ? 0xFF8b5cf6 : 0xFFFFFFFF);

            holder.itemView.setOnClickListener(v -> {
                MusicPlayerManager.getInstance().playSong(song);
                // Cần update currentIndex trong Manager
                dismiss();
            });

            holder.btnRemove.setOnClickListener(v -> {
                if (!isPlaying) {
                    songs.remove(position);
                    notifyItemRemoved(position);
                }
            });
        }

        @Override
        public int getItemCount() {
            return songs.size();
        }

        class VH extends RecyclerView.ViewHolder {
            TextView title, artist;
            ImageView imgPlaying;
            ImageButton btnRemove;

            VH(View itemView) {
                super(itemView);
                title = itemView.findViewById(R.id.txtQueueTitle);
                artist = itemView.findViewById(R.id.txtQueueArtist);
                imgPlaying = itemView.findViewById(R.id.imgQueuePlaying);
                btnRemove = itemView.findViewById(R.id.btnRemoveFromQueue);
            }
        }
    }
}
