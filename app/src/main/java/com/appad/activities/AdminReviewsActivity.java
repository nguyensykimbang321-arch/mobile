package com.appad.activities;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
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

public class AdminReviewsActivity extends AppCompatActivity {

    private RecyclerView rvReviews;
    private ReviewAdapter adapter;
    private List<Map<String, Object>> allReviews = new ArrayList<>();
    private List<Map<String, Object>> displayReviews = new ArrayList<>();

    private TextView txtAvgRating, txtTotalReviews, txtTopSong, txtResultsHeader;
    private RatingBar ratingBarSmall;
    private ChipGroup chipGroupFilters;
    private Spinner spinnerSort;

    private int currentFilterRating = 0; 
    private String currentSort = "Mới nhất";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_artist_reviews); // Reuse layout

        initViews();
        loadReviews();
    }

    private void initViews() {
        Toolbar toolbar = findViewById(R.id.toolbarReviews);
        setSupportActionBar(toolbar);
        getSupportActionBar().setTitle("Quản trị Đánh giá");
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

        chipGroupFilters.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.chipAll) currentFilterRating = 0;
            else if (checkedId == R.id.chip5) currentFilterRating = 5;
            else if (checkedId == R.id.chip4) currentFilterRating = 4;
            else if (checkedId == R.id.chip3) currentFilterRating = 3;
            else if (checkedId == R.id.chipLower) currentFilterRating = 2;
            applyFilterAndSort();
        });
    }

    private void loadReviews() {
        RetrofitClient.getApiService().getAllReviews().enqueue(new Callback<Map<String, Object>>() {
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
                }
            }
            @Override public void onFailure(Call<Map<String, Object>> call, Throwable t) {}
        });
    }

    private void calculateStats() {
        if (allReviews.isEmpty()) return;
        double sum = 0;
        int count = 0;
        for (Map<String, Object> r : allReviews) {
            Object ratingObj = r.get("rating");
            if (ratingObj != null) {
                sum += ((Number) ratingObj).doubleValue();
                count++;
            }
        }
        double avg = count > 0 ? sum / count : 0;
        txtAvgRating.setText(String.format("%.1f", avg));
        ratingBarSmall.setRating((float) avg);
        txtTotalReviews.setText(allReviews.size() + " đánh giá hệ thống");
        txtTopSong.setText("Tổng quan đánh giá toàn sàn");
    }

    private void applyFilterAndSort() {
        displayReviews.clear();
        for (Map<String, Object> r : allReviews) {
            Object ratingObj = r.get("rating");
            int rating = ratingObj != null ? ((Number) ratingObj).intValue() : 0;
            if (currentFilterRating == 0) displayReviews.add(r);
            else if (currentFilterRating == 2) { if (rating < 3) displayReviews.add(r); }
            else { if (rating == currentFilterRating) displayReviews.add(r); }
        }

        Collections.sort(displayReviews, (a, b) -> {
            switch (currentSort) {
                case "Mới nhất": return String.valueOf(b.get("createdAt")).compareTo(String.valueOf(a.get("createdAt")));
                case "Cũ nhất": return String.valueOf(a.get("createdAt")).compareTo(String.valueOf(b.get("createdAt")));
                case "Đánh giá cao": return ((Number) b.get("rating")).intValue() - ((Number) a.get("rating")).intValue();
                case "Đánh giá thấp": return ((Number) a.get("rating")).intValue() - ((Number) b.get("rating")).intValue();
                default: return 0;
            }
        });
        txtResultsHeader.setText("Kết quả: " + displayReviews.size());
        adapter.notifyDataSetChanged();
    }

    private void deleteComment(Long commentId, int position) {
        new android.app.AlertDialog.Builder(this)
            .setTitle("Xóa đánh giá?")
            .setMessage("Bạn có chắc muốn xóa đánh giá của người dùng này?")
            .setPositiveButton("Xóa", (dialog, which) -> {
                RetrofitClient.getApiService().deleteComment(commentId).enqueue(new Callback<Map<String, Object>>() {
                    @Override
                    public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                        if (response.isSuccessful()) {
                            Toast.makeText(AdminReviewsActivity.this, "Đã xóa đánh giá", Toast.LENGTH_SHORT).show();
                            loadReviews(); // Refresh
                        }
                    }
                    @Override public void onFailure(Call<Map<String, Object>> call, Throwable t) {}
                });
            })
            .setNegativeButton("Hủy", null)
            .show();
    }

    private class ReviewAdapter extends RecyclerView.Adapter<ReviewAdapter.ReviewViewHolder> {
        private List<Map<String, Object>> reviews;
        ReviewAdapter(List<Map<String, Object>> reviews) { this.reviews = reviews; }

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
                Glide.with(AdminReviewsActivity.this).load(ImageUrlUtils.fixUrl(avatar))
                        .placeholder(R.drawable.default_avatar).circleCrop().into(holder.imgAvatar);
            }

            holder.txtContent.setText((String) review.get("content"));
            holder.txtSongTitle.setText("Bài hát: " + review.get("songTitle"));
            
            Object rating = review.get("rating");
            if (rating != null) holder.txtRating.setText(String.valueOf(((Number) rating).intValue()));

            Object date = review.get("createdAt");
            if (date != null) {
                String d = date.toString().replace("T", " ");
                holder.txtDate.setText(d.length() > 16 ? d.substring(0, 16) : d);
            }

            // Show delete button for admin
            holder.btnDelete.setVisibility(View.VISIBLE);
            holder.btnDelete.setOnClickListener(v -> {
                Long cid = ((Number) review.get("commentId")).longValue();
                deleteComment(cid, position);
            });
        }

        @Override public int getItemCount() { return reviews.size(); }

        class ReviewViewHolder extends RecyclerView.ViewHolder {
            ImageView imgAvatar;
            TextView txtUserName, txtDate, txtRating, txtContent, txtSongTitle;
            ImageButton btnDelete;

            ReviewViewHolder(View itemView) {
                super(itemView);
                imgAvatar = itemView.findViewById(R.id.imgReviewUserAvatar);
                txtUserName = itemView.findViewById(R.id.txtReviewUserName);
                txtDate = itemView.findViewById(R.id.txtReviewDate);
                txtRating = itemView.findViewById(R.id.txtReviewRating);
                txtContent = itemView.findViewById(R.id.txtReviewContent);
                txtSongTitle = itemView.findViewById(R.id.txtReviewSongTitle);
                btnDelete = itemView.findViewById(R.id.btnDeleteReview);
            }
        }
    }
}
