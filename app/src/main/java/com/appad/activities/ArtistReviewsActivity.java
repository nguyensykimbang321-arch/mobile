package com.appad.activities;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.appad.R;
import com.appad.utils.ImageUrlUtils;
import com.appad.utils.RetrofitClient;
import com.bumptech.glide.Glide;
import com.google.android.material.chip.ChipGroup;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ArtistReviewsActivity extends AppCompatActivity {

    private RecyclerView rvReviews;
    private ReviewAdapter adapter;
    private List<Map<String, Object>> allReviews = new ArrayList<>();
    private List<Map<String, Object>> displayReviews = new ArrayList<>();
    private Integer artistId;
    private com.appad.utils.MiniPlayerHelper miniPlayerHelper;

    private TextView txtAvgRating, txtTotalReviews, txtTopSong, txtResultsHeader;
    private RatingBar ratingBarSmall;
    private ChipGroup chipGroupFilters;
    private Spinner spinnerSort;

    private int currentFilterRating = 0; // 0 = All, 5, 4, 3, 2 (for lower than 3)
    private String currentSort = "Mới nhất";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_artist_reviews);

        artistId = getIntent().getIntExtra("ARTIST_ID", -1);
        if (artistId == -1) {
            Toast.makeText(this, "Không tìm thấy thông tin nghệ sĩ", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViews();
        loadReviews();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (miniPlayerHelper == null) {
            miniPlayerHelper = new com.appad.utils.MiniPlayerHelper(this);
        }
        miniPlayerHelper.setupMiniPlayer();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (miniPlayerHelper != null) {
            miniPlayerHelper.detach();
        }
    }

    private void initViews() {
        Toolbar toolbar = findViewById(R.id.toolbarReviews);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        toolbar.setNavigationOnClickListener(v -> finish());

        txtAvgRating = findViewById(R.id.txtAvgRating);
        txtTotalReviews = findViewById(R.id.txtTotalReviews);
        txtTopSong = findViewById(R.id.txtTopSong);
        txtResultsHeader = findViewById(R.id.txtResultsHeader);
        ratingBarSmall = findViewById(R.id.ratingBarSmall);
        chipGroupFilters = findViewById(R.id.chipGroupFilters);
        spinnerSort = findViewById(R.id.spinnerSort);

        rvReviews = findViewById(R.id.rvReviews);
        rvReviews.setLayoutManager(new LinearLayoutManager(this));
        adapter = new ReviewAdapter(displayReviews);
        rvReviews.setAdapter(adapter);

        // Setup Sort Spinner
        String[] sortOptions = {"Mới nhất", "Cũ nhất", "Đánh giá cao", "Đánh giá thấp"};
        ArrayAdapter<String> sortAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, sortOptions);
        sortAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerSort.setAdapter(sortAdapter);

        spinnerSort.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentSort = sortOptions[position];
                applyFilterAndSort();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });

        // Setup Filter Chips
        chipGroupFilters.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chipAll) currentFilterRating = 0;
            else if (checkedId == R.id.chip5) currentFilterRating = 5;
            else if (checkedId == R.id.chip4) currentFilterRating = 4;
            else if (checkedId == R.id.chip3) currentFilterRating = 3;
            else if (checkedId == R.id.chipLower) currentFilterRating = 2; // used for < 3
            applyFilterAndSort();
        });
    }

    private void loadReviews() {
        RetrofitClient.getApiService().getArtistReviews(artistId).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<Map<String, Object>> data = (List<Map<String, Object>>) response.body().get("data");
                    if (data != null) {
                        allReviews.clear();
                        allReviews.addAll(data);
                        calculateStats();
                        applyFilterAndSort();
                    }
                } else {
                    Toast.makeText(ArtistReviewsActivity.this, "Lỗi tải đánh giá", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                Toast.makeText(ArtistReviewsActivity.this, "Lỗi kết nối", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void calculateStats() {
        if (allReviews.isEmpty()) return;

        double sum = 0;
        Map<String, Double> songRatings = new HashMap<>();
        Map<String, Integer> songCounts = new HashMap<>();

        for (Map<String, Object> r : allReviews) {
            Object ratingObj = r.get("rating");
            if (ratingObj != null) {
                double val = ((Number) ratingObj).doubleValue();
                sum += val;
                
                String songTitle = (String) r.get("songTitle");
                if (songTitle != null) {
                    songRatings.put(songTitle, songRatings.getOrDefault(songTitle, 0.0) + val);
                    songCounts.put(songTitle, songCounts.getOrDefault(songTitle, 0) + 1);
                }
            }
        }

        double avg = sum / allReviews.size();
        txtAvgRating.setText(String.format("%.1f", avg));
        ratingBarSmall.setRating((float) avg);
        txtTotalReviews.setText(allReviews.size() + " nhận xét");

        // Find top song
        String topSong = "--";
        double maxAvg = -1;
        for (String song : songRatings.keySet()) {
            double songAvg = songRatings.get(song) / songCounts.get(song);
            if (songAvg > maxAvg) {
                maxAvg = songAvg;
                topSong = song;
            }
        }
        txtTopSong.setText("Bài hát tốt nhất: " + topSong + " (" + String.format("%.1f", maxAvg) + "★)");
    }

    private void applyFilterAndSort() {
        displayReviews.clear();
        
        // Filter
        for (Map<String, Object> r : allReviews) {
            Object ratingObj = r.get("rating");
            int rating = ratingObj != null ? ((Number) ratingObj).intValue() : 0;

            if (currentFilterRating == 0) {
                displayReviews.add(r);
            } else if (currentFilterRating == 2) {
                if (rating < 3) displayReviews.add(r);
            } else {
                if (rating == currentFilterRating) displayReviews.add(r);
            }
        }

        // Sort
        Collections.sort(displayReviews, (a, b) -> {
            switch (currentSort) {
                case "Mới nhất":
                    return String.valueOf(b.get("createdAt")).compareTo(String.valueOf(a.get("createdAt")));
                case "Cũ nhất":
                    return String.valueOf(a.get("createdAt")).compareTo(String.valueOf(b.get("createdAt")));
                case "Đánh giá cao":
                    return ((Number) b.get("rating")).intValue() - ((Number) a.get("rating")).intValue();
                case "Đánh giá thấp":
                    return ((Number) a.get("rating")).intValue() - ((Number) b.get("rating")).intValue();
                default:
                    return 0;
            }
        });

        txtResultsHeader.setText("Kết quả: " + displayReviews.size() + " đánh giá");
        adapter.notifyDataSetChanged();
    }

    private class ReviewAdapter extends RecyclerView.Adapter<ReviewAdapter.ReviewViewHolder> {
        private List<Map<String, Object>> reviews;

        ReviewAdapter(List<Map<String, Object>> reviews) {
            this.reviews = reviews;
        }

        @NonNull
        @Override
        public ReviewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_artist_review, parent, false);
            return new ReviewViewHolder(v);
        }

        @Override
        public void onBindViewHolder(@NonNull ReviewViewHolder holder, int position) {
            Map<String, Object> review = reviews.get(position);
            Map<String, Object> user = (Map<String, Object>) review.get("user");
            
            if (user != null) {
                holder.txtUserName.setText((String) user.get("username"));
                String avatar = (String) user.get("avatar_url");
                Glide.with(ArtistReviewsActivity.this)
                        .load(ImageUrlUtils.fixUrl(avatar))
                        .placeholder(R.drawable.default_avatar)
                        .circleCrop()
                        .into(holder.imgAvatar);
            }

            holder.txtContent.setText((String) review.get("content"));
            holder.txtSongTitle.setText("Bài hát: " + review.get("songTitle"));
            
            Object rating = review.get("rating");
            if (rating != null) {
                holder.txtRating.setText(String.valueOf(((Number) rating).intValue()));
            }

            Object date = review.get("createdAt");
            if (date != null) {
                String d = date.toString();
                if (d.contains("T")) d = d.replace("T", " ");
                if (d.length() > 16) d = d.substring(0, 16);
                holder.txtDate.setText(d);
            }
        }

        @Override
        public int getItemCount() {
            return reviews.size();
        }

        class ReviewViewHolder extends RecyclerView.ViewHolder {
            ImageView imgAvatar;
            TextView txtUserName, txtDate, txtRating, txtContent, txtSongTitle;

            ReviewViewHolder(View itemView) {
                super(itemView);
                imgAvatar = itemView.findViewById(R.id.imgReviewUserAvatar);
                txtUserName = itemView.findViewById(R.id.txtReviewUserName);
                txtDate = itemView.findViewById(R.id.txtReviewDate);
                txtRating = itemView.findViewById(R.id.txtReviewRating);
                txtContent = itemView.findViewById(R.id.txtReviewContent);
                txtSongTitle = itemView.findViewById(R.id.txtReviewSongTitle);
            }
        }
    }
}
