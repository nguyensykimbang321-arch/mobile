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
import com.appad.activities.GenreDetailActivity;
import com.appad.models.Genre;
import com.bumptech.glide.Glide;

import java.util.List;

public class GenreAdapter extends RecyclerView.Adapter<GenreAdapter.ViewHolder> {

    private Context context;
    private List<Genre> genreList;

    public GenreAdapter(Context context, List<Genre> genreList) {
        this.context = context;
        this.genreList = genreList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_genre_card, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Genre genre = genreList.get(position);
        holder.txtName.setText(genre.getName());

        String coverUrl = genre.getCoverUrl();
        if (coverUrl != null && !coverUrl.isEmpty()) {
            Glide.with(context)
                    .load(com.appad.utils.ImageUrlUtils.fixUrl(coverUrl))
                    .placeholder(R.drawable.ic_launcher_background)
                    .into(holder.imgCover);
        }

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, GenreDetailActivity.class);
            intent.putExtra("genreId", genre.getGenreId());
            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return genreList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgCover;
        TextView txtName;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgCover = itemView.findViewById(R.id.imgGenreBackground);
            txtName = itemView.findViewById(R.id.txtGenreName);
        }
    }
}
