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
import com.appad.utils.ImageUrlUtils;
import com.bumptech.glide.Glide;

import java.util.List;
import java.util.Map;

public class AdminAlbumAdapter extends RecyclerView.Adapter<AdminAlbumAdapter.ViewHolder> {

    private Context context;
    private List<Map<String, Object>> albums;
    private OnAlbumActionEventListener listener;

    public interface OnAlbumActionEventListener {
        void onEditAlbum(Map<String, Object> album);
        void onDeleteAlbum(Map<String, Object> album);
    }

    public AdminAlbumAdapter(Context context, List<Map<String, Object>> albums, OnAlbumActionEventListener listener) {
        this.context = context;
        this.albums = albums;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_admin_album, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Map<String, Object> album = albums.get(position);
        
        holder.txtTitle.setText((String) album.get("title"));
        holder.txtArtist.setText((String) album.get("artistName"));
        if (album.get("artistName") == null) holder.txtArtist.setText((String) album.get("artist_name"));

        Object songCount = album.get("songCount");
        if (songCount == null) songCount = album.get("song_count");
        holder.txtSongs.setText((songCount != null ? songCount : 0) + " bài hát");
        
        String coverUrl = (String) album.get("coverUrl");
        if (coverUrl == null) coverUrl = (String) album.get("cover_url");
        
        Glide.with(context)
                .load(ImageUrlUtils.fixUrl(coverUrl))
                .placeholder(R.drawable.placeholder_song)
                .into(holder.imgCover);

        holder.btnEdit.setOnClickListener(v -> {
            if (listener != null) listener.onEditAlbum(album);
        });

        holder.btnDelete.setOnClickListener(v -> {
            if (listener != null) listener.onDeleteAlbum(album);
        });
    }

    @Override
    public int getItemCount() {
        return albums.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgCover;
        TextView txtTitle, txtArtist, txtSongs;
        View btnEdit, btnDelete;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgCover = itemView.findViewById(R.id.imgAlbumCover);
            txtTitle = itemView.findViewById(R.id.txtAlbumTitle);
            txtArtist = itemView.findViewById(R.id.txtAlbumArtist);
            txtSongs = itemView.findViewById(R.id.txtAlbumSongs);
            btnEdit = itemView.findViewById(R.id.btnEditAlbum);
            btnDelete = itemView.findViewById(R.id.btnDeleteAlbum);
        }
    }
}
