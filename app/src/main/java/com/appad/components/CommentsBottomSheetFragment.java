package com.appad.components;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.appad.R;
import com.appad.adapters.CommentsAdapter;
import com.appad.models.Song;
import com.appad.utils.RetrofitClient;
import com.appad.utils.SessionManager;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class CommentsBottomSheetFragment extends BottomSheetDialogFragment {

    private Song song;
    private RecyclerView rvComments;
    private EditText editComment;
    private ImageButton btnSend, btnSort;
    private TextView txtAvgRating, txtListenCount;
    
    private ImageView[] ratingStars = new ImageView[5];
    private int selectedRating = 5; // Default 5 stars
    private List<Map<String, Object>> allComments = new ArrayList<>();
    private List<Map<String, Object>> filteredComments = new ArrayList<>();
    private CommentsAdapter adapter;
    private boolean isSortNewest = true;

    public static CommentsBottomSheetFragment newInstance(Song song) {
        CommentsBottomSheetFragment fragment = new CommentsBottomSheetFragment();
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
        return inflater.inflate(R.layout.layout_comments_sheet, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        rvComments = view.findViewById(R.id.rvCommentsSheet);
        editComment = view.findViewById(R.id.editCommentSheet);
        btnSend = view.findViewById(R.id.btnSendCommentSheet);
        btnSort = view.findViewById(R.id.btnSortComments);
        txtAvgRating = view.findViewById(R.id.txtAvgRating);
        txtListenCount = view.findViewById(R.id.txtListenCount);

        ratingStars[0] = view.findViewById(R.id.sheetStar1);
        ratingStars[1] = view.findViewById(R.id.sheetStar2);
        ratingStars[2] = view.findViewById(R.id.sheetStar3);
        ratingStars[3] = view.findViewById(R.id.sheetStar4);
        ratingStars[4] = view.findViewById(R.id.sheetStar5);

        rvComments.setLayoutManager(new LinearLayoutManager(getContext()));
        
        setupListeners();
        updateRatingUI();
        updateSongStats();
        loadComments();
    }

    private void updateSongStats() {
        if (song != null) {
            txtAvgRating.setText(String.format("%.1f", song.getStars()));
            
            long count = song.getListenCount();
            if (count >= 1000000) {
                txtListenCount.setText(String.format("%.1fM", count / 1000000.0));
            } else if (count >= 1000) {
                txtListenCount.setText(String.format("%.1fK", count / 1000.0));
            } else {
                txtListenCount.setText(String.valueOf(count));
            }
        }
    }

    private void setupListeners() {
        btnSend.setOnClickListener(v -> postComment());

        btnSort.setOnClickListener(v -> {
            isSortNewest = !isSortNewest;
            Toast.makeText(getContext(), isSortNewest ? "Mới nhất" : "Cũ nhất", Toast.LENGTH_SHORT).show();
            applyFilter();
        });

        for (int i = 0; i < 5; i++) {
            final int rating = i + 1;
            ratingStars[i].setOnClickListener(v -> {
                selectedRating = rating;
                updateRatingUI();
            });
        }
    }

    private void updateRatingUI() {
        for (int i = 0; i < 5; i++) {
            if (i < selectedRating) {
                ratingStars[i].setColorFilter(android.graphics.Color.parseColor("#FFD700"));
                ratingStars[i].setAlpha(1.0f);
            } else {
                ratingStars[i].setColorFilter(android.graphics.Color.parseColor("#888888"));
                ratingStars[i].setAlpha(0.5f);
            }
        }
    }

    private void loadComments() {
        if (song == null) return;

        RetrofitClient.getApiService().getComments("song", song.getSongId()).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (isAdded() && response.isSuccessful() && response.body() != null) {
                    Map<String, Object> body = response.body();
                    Object dataObj = body.get("data");
                    Object commentsObj = null;
                    
                    if (dataObj instanceof Map) {
                        commentsObj = ((Map) dataObj).get("comments");
                    }
                    
                    if (commentsObj instanceof List) {
                        String json = new Gson().toJson(commentsObj);
                        allComments = new Gson().fromJson(json, new TypeToken<List<Map<String, Object>>>(){}.getType());
                        applyFilter();
                    }
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {}
        });
    }

    private void applyFilter() {
        filteredComments.clear();
        filteredComments.addAll(allComments);

        if (!isSortNewest) {
            Collections.reverse(filteredComments);
        }

        Integer currentUserId = SessionManager.getInstance(getContext()).getUserId();
        adapter = new CommentsAdapter(getContext(), filteredComments, currentUserId, this::deleteComment);
        rvComments.setAdapter(adapter);
    }

    private void deleteComment(long commentId) {
        RetrofitClient.getApiService().deleteComment(commentId).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (isAdded() && response.isSuccessful()) {
                    loadComments(); // Refresh
                }
            }
            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {}
        });
    }

    private void postComment() {
        String content = editComment.getText().toString().trim();
        Integer userId = SessionManager.getInstance(getContext()).getUserId();

        if (userId == null) {
            Toast.makeText(getContext(), "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show();
            return;
        }
        if (content.isEmpty()) return;

        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", userId);
        payload.put("targetId", song.getSongId());
        payload.put("type", "song");
        payload.put("content", content);
        payload.put("rating", selectedRating); 

        RetrofitClient.getApiService().addComment(payload).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (isAdded() && response.isSuccessful()) {
                    editComment.setText("");
                    selectedRating = 5;
                    updateRatingUI();
                    loadComments();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {}
        });
    }
}
