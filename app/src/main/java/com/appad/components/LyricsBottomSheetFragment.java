package com.appad.components;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.appad.R;
import com.appad.models.Song;
import com.appad.utils.RetrofitClient;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class LyricsBottomSheetFragment extends BottomSheetDialogFragment {

    private Song song;
    private TextView textLyricsDetail;

    public static LyricsBottomSheetFragment newInstance(Song song) {
        LyricsBottomSheetFragment fragment = new LyricsBottomSheetFragment();
        fragment.song = song;
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
        return inflater.inflate(R.layout.layout_lyrics_sheet, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        textLyricsDetail = view.findViewById(R.id.textLyricsDetail);
        TextView txtTitle = view.findViewById(R.id.txtLyricsTitle);
        
        if (song != null) {
            txtTitle.setText("Lời bài hát: " + song.getTitle());
            loadLyrics();
        }
    }

    private void loadLyrics() {
        if (song == null) return;

        RetrofitClient.getApiService().getSongById(song.getSongId()).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (isAdded() && response.isSuccessful() && response.body() != null) {
                    java.util.Map<String, Object> data = (java.util.Map<String, Object>) response.body().get("data");
                    String lyrics = null;
                    if (data != null) {
                        Object lyricsObj = data.get("lyrics");
                        lyrics = lyricsObj != null ? lyricsObj.toString() : null;
                    }
                    
                    if (lyrics == null || lyrics.isEmpty() || lyrics.equals("null")) {
                        lyrics = "Không có lời bài hát cho bài này.";
                    }
                    textLyricsDetail.setText(lyrics);
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                if (isAdded()) {
                    textLyricsDetail.setText("Lỗi khi tải lời bài hát.");
                }
            }
        });
    }
}
