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
import com.appad.models.Artist;
import com.bumptech.glide.Glide;
import java.util.List;

public class ArtistAdapter extends RecyclerView.Adapter<ArtistAdapter.ArtistViewHolder> {

    private Context context;
    private List<Artist> artistList;
    private OnArtistClickListener listener;

    public interface OnArtistClickListener {
        void onArtistClick(Artist artist);
    }

    public ArtistAdapter(Context context, List<Artist> artistList, OnArtistClickListener listener) {
        this.context = context;
        this.artistList = artistList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ArtistViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_artist_circle, parent, false);
        return new ArtistViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ArtistViewHolder holder, int position) {
        Artist artist = artistList.get(position);
        holder.txtName.setText(artist.getName());
        
        Glide.with(context)
                .load(com.appad.utils.ImageUrlUtils.fixUrl(artist.getImageUrl()))
                .placeholder(R.drawable.ic_launcher_background)
                .circleCrop()
                .into(holder.imgArtist);

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) listener.onArtistClick(artist);
        });
    }

    @Override
    public int getItemCount() {
        return artistList.size();
    }

    public static class ArtistViewHolder extends RecyclerView.ViewHolder {
        ImageView imgArtist;
        TextView txtName;

        public ArtistViewHolder(@NonNull View itemView) {
            super(itemView);
            imgArtist = itemView.findViewById(R.id.imgArtistCircle);
            txtName = itemView.findViewById(R.id.textArtistNameTitle);
        }
    }
}
