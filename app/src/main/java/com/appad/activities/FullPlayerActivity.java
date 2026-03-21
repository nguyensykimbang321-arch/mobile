package com.appad.activities;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.appad.R;
import com.appad.models.Song;
import com.appad.utils.MusicPlayerManager;
import com.appad.utils.RetrofitClient;
import com.appad.utils.SessionManager;
import com.bumptech.glide.Glide;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.appad.utils.ImageUrlUtils;
import android.os.Handler;
import java.io.File;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.appad.components.PremiumAccessModal;
import com.appad.components.PurchaseConfirmationModal;
import com.appad.components.LyricsBottomSheetFragment;
import com.appad.components.CommentsBottomSheetFragment;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;


public class FullPlayerActivity extends AppCompatActivity {
    
    private MusicPlayerManager.OnPlayerStatusChangeListener statusChangeListener;
    private ImageButton btnBack, btnFavorite, btnShuffle, btnPrevious, btnNext, btnRepeat, btnLyrics, btnComments, btnSleepTimer, btnAddToPlaylist, btnReport, btnMoreActions, btnSendComment, btnDownload;
    private FloatingActionButton btnPlayPause;
    private TextView btnTabQueue, btnTabRelated;
    private android.widget.ProgressBar pbQueue;
    private String activeQueueTab = "queue"; // "queue" or "related"
    private List<Song> relatedSongs = new ArrayList<>();
    private android.widget.Button btnBuySong;
    private ImageView imgAlbumArt;
    private TextView textSongTitle, textArtistName, textGenre, textFullGenre, textSeparator, textCurrentTime, textTotalTime, textLyrics, txtNoComments;
    private android.widget.EditText editComment;
    private SeekBar seekBar;
    private android.view.View lyricsCard, commentsContainer, queueContainer, rootLayout;
    private androidx.recyclerview.widget.RecyclerView rvComments, rvQueue;

    private int selectedRating = 0;
    private ImageView[] ratingStars = new ImageView[5];
    private boolean isPlaying = false;
    private boolean isFavoriteCurrent = false;
    private boolean isLoadingRelated = false;
    private Handler progressHandler = new Handler();
    private Runnable progressRunnable;
    
    // Animation fields
    private android.view.View viewPulseEffect;
    private android.animation.ObjectAnimator scaleXBtn, scaleYBtn, scaleXC, scaleYC, alphaC;

    // Preview Mode variables
    private boolean isPreviewMode = false;
    private List<Song> pendingPlaylist = null;
    private int pendingIndex = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            // Immersive mode / Transparent Status Bar
            getWindow().getDecorView().setSystemUiVisibility(
                android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE | android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
            );
            getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
            
            setContentView(R.layout.activity_full_player);

            initViews();
            setupListeners();
            
            // Handle incoming formatted playlist
            if (getIntent().hasExtra("playlist_json")) {
                try {
                    String json = getIntent().getStringExtra("playlist_json");
                    int startIndex = getIntent().getIntExtra("start_index", 0);
                    boolean autoPlay = getIntent().getBooleanExtra("auto_play", true);
                    
                    java.lang.reflect.Type type = new TypeToken<List<Song>>(){}.getType();
                    List<Song> playlist = new Gson().fromJson(json, type);
                    
                    if (playlist != null && !playlist.isEmpty()) {
                         MusicPlayerManager.getInstance().setPlaylist(playlist, startIndex, autoPlay);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
            
            String title = getIntent().getStringExtra("title");
            String artist = getIntent().getStringExtra("artist");
            String cover = getIntent().getStringExtra("cover");
            String genre = getIntent().getStringExtra("genre");

            if (title != null) textSongTitle.setText(title);
            if (artist != null) textArtistName.setText(artist);
            if (textGenre != null && genre != null) textGenre.setText(genre);
            
             // Link textGenre to Generic Genre Detail (if possible) or keep as visual
            if (genre != null && !genre.isEmpty() && textGenre != null) {
                 textGenre.setOnClickListener(v -> {
                     Song current = MusicPlayerManager.getInstance().getCurrentSong();
                     if (current != null && current.getGenreId() != null) {
                         android.content.Intent i = new android.content.Intent(this, com.appad.activities.GenreDetailActivity.class);
                         i.putExtra("genreId", current.getGenreId());
                         i.putExtra("genreName", current.getGenreName());
                         startActivity(i);
                     }
                 });
            }
            
            android.util.Log.d("FullPlayer", "Checkpoint 1: Views initialized");
            setupPlayerSync();
            
            updateSongDetails(MusicPlayerManager.getInstance().getCurrentSong());

            // Trigger visual play effect immediately if auto-playing
            if (getIntent().getBooleanExtra("auto_play", true)) {
                if (statusChangeListener != null) {
                    statusChangeListener.onPlaybackStatusChanged(true);
                }
            }

            loadRelatedSongs();
            android.util.Log.d("FullPlayer", "Checkpoint 3: Sync calls done");
        } catch (Exception e) {
            e.printStackTrace();
            android.util.Log.e("FullPlayer", "Crash in onCreate", e);
            showErrorDialog(e);
        }
    }

    private void handleAutoPlay() {
        // Deprecated by onCreate logic, but kept for safety if used elsewhere
        boolean autoPlay = getIntent().getBooleanExtra("auto_play", true);
        if (!autoPlay) {
            if (MusicPlayerManager.getInstance().isPlaying()) {
                 MusicPlayerManager.getInstance().pause();
            }
        }
    }

    private void showErrorDialog(Exception e) {
        StringBuilder sb = new StringBuilder();
        sb.append("Chi tiết lỗi:\n");
        sb.append(e.toString()).append("\n\n");
        
        // Lấy 5 dòng đầu của StackTrace để biết vị trí lỗi
        StackTraceElement[] st = e.getStackTrace();
        for (int i = 0; i < Math.min(st.length, 5); i++) {
            sb.append("tại ").append(st[i].toString()).append("\n");
        }

        new android.app.AlertDialog.Builder(this)
            .setTitle("⚠️ Lỗi Trình Phát")
            .setMessage(sb.toString())
            .setPositiveButton("Đóng và quay lại", (dialog, which) -> finish())
            .setCancelable(false)
            .show();
    }

    private void updateSongDetails(Song song) {
        if (song == null) return;
        
        textSongTitle.setText(song.getTitle());
        textArtistName.setText(song.getArtistName());
        
        // Update Genre Headers and Body
        if (textGenre != null) textGenre.setText(song.getGenreName() != null ? song.getGenreName() : "Nhạc");
        
        if (textFullGenre != null) {
            String gName = song.getGenreName();
            if (gName == null) gName = "";
            
            if (gName.trim().equalsIgnoreCase("nhạc") || gName.trim().equalsIgnoreCase("music")) {
                gName = "";
            }

            String displayGenre = gName;
            if (!gName.isEmpty() && !gName.toLowerCase().startsWith("nhạc")) {
                displayGenre = "Nhạc " + gName;
            }
            textFullGenre.setText(displayGenre);
            textFullGenre.setVisibility(gName.isEmpty() ? View.GONE : View.VISIBLE);
            if (textSeparator != null) textSeparator.setVisibility(gName.isEmpty() ? View.GONE : View.VISIBLE);
            
            // Link to Genre Detail (Body)
            if (song.getGenreId() != null) {
                textFullGenre.setOnClickListener(v -> {
                     android.content.Intent i = new android.content.Intent(this, com.appad.activities.GenreDetailActivity.class);
                     i.putExtra("genreId", song.getGenreId());
                     i.putExtra("genreName", song.getGenreName());
                     startActivity(i);
                });
            }
        }
        
        // Link to Artist Detail
        if (song.getArtistId() != null) {
            textArtistName.setOnClickListener(v -> {
                 android.content.Intent i = new android.content.Intent(this, com.appad.activities.ArtistDetailActivity.class);
                 i.putExtra("artistId", song.getArtistId());
                 startActivity(i);
            });
        }

        // Update Cover and Background
        String coverUrl = ImageUrlUtils.fixUrl(song.getCoverUrl());
        Glide.with(this)
             .asBitmap()
             .load(coverUrl)
             .placeholder(R.drawable.ic_launcher_background)
             .into(new com.bumptech.glide.request.target.CustomTarget<android.graphics.Bitmap>() {
                 @Override
                 public void onResourceReady(@androidx.annotation.NonNull android.graphics.Bitmap resource, @androidx.annotation.Nullable com.bumptech.glide.request.transition.Transition<? super android.graphics.Bitmap> transition) {
                     imgAlbumArt.setImageBitmap(resource);
                     androidx.palette.graphics.Palette.from(resource).generate(palette -> {
                         if (palette != null) {
                              int dominantColor = palette.getDominantColor(0xFF121212);
                              int vibrantColor = palette.getVibrantColor(dominantColor);
                              int lightVibrantColor = palette.getLightVibrantColor(vibrantColor);
                              int darkMutedColor = palette.getDarkMutedColor(0xFF000000);
                              
                              // Create a more complex 4-color gradient for a premium "mesh" feel
                              android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable(
                                  android.graphics.drawable.GradientDrawable.Orientation.BR_TL,
                                  new int[] {darkMutedColor, dominantColor, vibrantColor, lightVibrantColor}
                              );
                              gd.setCornerRadius(0f);
                              gd.setGradientType(android.graphics.drawable.GradientDrawable.LINEAR_GRADIENT);
                              
                              if (rootLayout != null) {
                                  rootLayout.setBackground(gd);
                              }

                              // Sử dụng màu tím cố định cho seekBar để đồng bộ giao diện
                              if (seekBar != null) {
                                  int fixedPurple = androidx.core.content.ContextCompat.getColor(FullPlayerActivity.this, R.color.vibrant_purple);
                                  seekBar.setProgressTintList(android.content.res.ColorStateList.valueOf(fixedPurple));
                                  seekBar.setThumbTintList(android.content.res.ColorStateList.valueOf(fixedPurple));
                              }
                              
                              int iconColor = palette.getLightVibrantColor(Color.WHITE);
                              if (btnFavorite != null) btnFavorite.setColorFilter(iconColor);
                              
                              // Tints for Lyrics/Comments are handled separately in updateActionButtonTints()
                              updateActionButtonTints();
                         }
                     });
                 }
                 @Override public void onLoadCleared(@androidx.annotation.Nullable android.graphics.drawable.Drawable placeholder) {}
             });

        checkFavoriteStatus();
        checkPremiumAccess();
        checkDownloadAccess();
        loadLyrics();
        setupQueueList();
    }

    private void setupPlayerSync() {
        if (statusChangeListener == null) {
            statusChangeListener = new MusicPlayerManager.OnPlayerStatusChangeListener() {
                @Override
                public void onSongChanged(Song song) {
                    android.util.Log.d("FullPlayer", "onSongChanged: " + (song != null ? song.getTitle() : "null"));
                    if (song == null) return;
                    updateSongDetails(song);
                }

                @Override
                public void onPlaybackStatusChanged(boolean isPlayingStatus) {
                    isPlaying = isPlayingStatus;
                    btnPlayPause.setImageResource(isPlaying ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play);
                    
                    if (isPlaying) {
                        startProgressUpdate();

                        // Button Pulse
                        if (scaleXBtn == null) {
                            scaleXBtn = android.animation.ObjectAnimator.ofFloat(btnPlayPause, "scaleX", 1f, 1.1f);
                            scaleXBtn.setRepeatCount(android.animation.ValueAnimator.INFINITE);
                            scaleXBtn.setRepeatMode(android.animation.ValueAnimator.REVERSE);
                            scaleXBtn.setDuration(800);
                            
                            scaleYBtn = android.animation.ObjectAnimator.ofFloat(btnPlayPause, "scaleY", 1f, 1.1f);
                            scaleYBtn.setRepeatCount(android.animation.ValueAnimator.INFINITE);
                            scaleYBtn.setRepeatMode(android.animation.ValueAnimator.REVERSE);
                            scaleYBtn.setDuration(800);
                        }
                        if (!scaleXBtn.isStarted()) {
                            scaleXBtn.start();
                            scaleYBtn.start();
                        }

                        // Halo Effect
                        if (viewPulseEffect != null) {
                            if (scaleXC == null) {
                                scaleXC = android.animation.ObjectAnimator.ofFloat(viewPulseEffect, "scaleX", 1f, 2.2f);
                                scaleXC.setRepeatCount(android.animation.ValueAnimator.INFINITE);
                                scaleXC.setDuration(2200);
                                
                                scaleYC = android.animation.ObjectAnimator.ofFloat(viewPulseEffect, "scaleY", 1f, 2.2f);
                                scaleYC.setRepeatCount(android.animation.ValueAnimator.INFINITE);
                                scaleYC.setDuration(2200);
                                
                                alphaC = android.animation.ObjectAnimator.ofFloat(viewPulseEffect, "alpha", 0.5f, 0f);
                                alphaC.setRepeatCount(android.animation.ValueAnimator.INFINITE);
                                alphaC.setDuration(2200);
                            }
                            if (!scaleXC.isStarted()) {
                                scaleXC.start();
                                scaleYC.start();
                                alphaC.start();
                            }
                        }
                    } else {
                        stopProgressUpdate();
                        
                        if (scaleXBtn != null) {
                            scaleXBtn.cancel();
                            scaleYBtn.cancel();
                            btnPlayPause.setScaleX(1f);
                            btnPlayPause.setScaleY(1f);
                        }
                        
                        if (scaleXC != null) {
                            scaleXC.cancel();
                            scaleYC.cancel();
                            alphaC.cancel();
                            if (viewPulseEffect != null) {
                                viewPulseEffect.setScaleX(1f);
                                viewPulseEffect.setScaleY(1f);
                                viewPulseEffect.setAlpha(0f);
                            }
                        }
                    }
                }

                @Override
                public void onProgressUpdated(int position, int duration) {
                    // Could use this instead of internal progressHandler but keeping it for now
                }

                @Override
                public void onInactivityPause() {
                    com.appad.components.ContinueListeningModal fragment = com.appad.components.ContinueListeningModal.newInstance(() -> {
                         // On Continue
                         MusicPlayerManager.getInstance().togglePlayPause(); // This will resume and update interaction
                    });
                    fragment.show(getSupportFragmentManager(), "ContinueListeningModal");
                }
            };
        }
        MusicPlayerManager.getInstance().addStatusChangeListener(statusChangeListener);
    }

    private void startProgressUpdate() {
        progressRunnable = new Runnable() {
            @Override
            public void run() {
                if (MusicPlayerManager.getInstance().isPlaying()) {
                    int current = MusicPlayerManager.getInstance().getCurrentPosition();
                    int total = MusicPlayerManager.getInstance().getDuration();
                    seekBar.setMax(total);
                    seekBar.setProgress(current);
                    textCurrentTime.setText(formatTime(current));
                    textTotalTime.setText(formatTime(total));
                    progressHandler.postDelayed(this, 1000);
                }
            }
        };
        progressHandler.post(progressRunnable);
    }

    private void stopProgressUpdate() {
        progressHandler.removeCallbacks(progressRunnable);
    }

    private String formatTime(int ms) {
        int minutes = (ms / 1000) / 60;
        int seconds = (ms / 1000) % 60;
        return String.format("%d:%02d", minutes, seconds);
    }

    private void initViews() {
        btnBack = findViewById(R.id.btnBack);
        btnFavorite = findViewById(R.id.btnFavorite);
        btnShuffle = findViewById(R.id.btnShuffle);
        btnPrevious = findViewById(R.id.btnPrevious);
        btnNext = findViewById(R.id.btnNext);
        btnRepeat = findViewById(R.id.btnRepeat);
        viewPulseEffect = findViewById(R.id.viewPulseEffect);
        btnPlayPause = findViewById(R.id.btnPlayPause);
        imgAlbumArt = findViewById(R.id.imgFullAlbumArt);
        textSongTitle = findViewById(R.id.textFullSongTitle);
        textArtistName = findViewById(R.id.textFullArtistName);
        textFullGenre = findViewById(R.id.textFullGenre);
        textSeparator = findViewById(R.id.textSeparator);
        textGenre = findViewById(R.id.textGenre);
        textCurrentTime = findViewById(R.id.textCurrentTime);
        textTotalTime = findViewById(R.id.textTotalTime);
        seekBar = findViewById(R.id.playerSeekBar);
        btnBuySong = findViewById(R.id.btnBuySong);
        
        btnLyrics = findViewById(R.id.btnLyrics);
        btnComments = findViewById(R.id.btnComments);
        btnSleepTimer = findViewById(R.id.btnSleepTimer);
        btnAddToPlaylist = findViewById(R.id.btnAddToPlaylist);
        btnReport = findViewById(R.id.btnReport);
        btnMoreActions = findViewById(R.id.btnMoreActions);
        btnDownload = findViewById(R.id.btnDownload);
        
        lyricsCard = findViewById(R.id.lyricsCard);
        textLyrics = findViewById(R.id.textLyrics);
        commentsContainer = findViewById(R.id.commentsContainer);
        rvComments = findViewById(R.id.rvComments);
        txtNoComments = findViewById(R.id.txtNoComments);
        editComment = findViewById(R.id.editComment);
        btnSendComment = findViewById(R.id.btnSendComment);
        queueContainer = findViewById(R.id.queueContainer);
        rvQueue = findViewById(R.id.rvQueue);
        btnTabQueue = findViewById(R.id.btnTabQueue);
        btnTabRelated = findViewById(R.id.btnTabRelated);
        pbQueue = findViewById(R.id.pbQueue);
        
        rootLayout = findViewById(R.id.playerRootLayout);
        
        if (rvComments != null) rvComments.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this));
        if (rvQueue != null) rvQueue.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this));

        ratingStars[0] = findViewById(R.id.star1);
        ratingStars[1] = findViewById(R.id.star2);
        ratingStars[2] = findViewById(R.id.star3);
        ratingStars[3] = findViewById(R.id.star4);
        ratingStars[4] = findViewById(R.id.star5);
        updateRatingUI();
    }

    private void setupListeners() {
        btnBack.setOnClickListener(v -> finish());
        
        btnPlayPause.setOnClickListener(v -> MusicPlayerManager.getInstance().togglePlayPause());

        btnFavorite.setOnClickListener(v -> toggleFavorite());

        btnNext.setOnClickListener(v -> MusicPlayerManager.getInstance().playNext());
        btnPrevious.setOnClickListener(v -> MusicPlayerManager.getInstance().playPrevious());

        btnShuffle.setOnClickListener(v -> {
            MusicPlayerManager.getInstance().toggleShuffle();
            updateShuffleUI();
        });
        updateShuffleUI();

        btnRepeat.setOnClickListener(v -> {
            MusicPlayerManager.getInstance().toggleRepeat();
            updateRepeatUI();
        });
        updateRepeatUI();

        btnSleepTimer.setOnClickListener(v -> showSleepTimerDialog());

        btnAddToPlaylist.setOnClickListener(v -> {
            Song current = MusicPlayerManager.getInstance().getCurrentSong();
            if (current != null) {
                com.appad.components.AddToPlaylistFragment.newInstance(current)
                        .show(getSupportFragmentManager(), "AddToPlaylistSheet");
            }
        });

        btnReport.setOnClickListener(v -> {
            Song current = MusicPlayerManager.getInstance().getCurrentSong();
            if (current != null) {
                com.appad.components.ReportDialogFragment.newInstance(current)
                        .show(getSupportFragmentManager(), "ReportDialog");
            }
        });

        btnDownload.setOnClickListener(v -> {
            Song current = MusicPlayerManager.getInstance().getCurrentSong();
            if (current != null) {
                startDownload(current);
            }
        });

        btnLyrics.setOnClickListener(v -> {
            toggleContentSelection(lyricsCard);
        });

        btnComments.setOnClickListener(v -> {
            toggleContentSelection(commentsContainer);
            if (commentsContainer.getVisibility() == View.VISIBLE) {
                loadComments();
            }
        });

        btnMoreActions.setOnClickListener(v -> showMoreActionsMenu(v));

        btnTabQueue.setOnClickListener(v -> switchQueueTab("queue"));
        btnTabRelated.setOnClickListener(v -> switchQueueTab("related"));

        btnSendComment.setOnClickListener(v -> postComment());

        for (int i = 0; i < 5; i++) {
            final int rating = i + 1;
            ratingStars[i].setOnClickListener(v -> {
                selectedRating = rating;
                updateRatingUI();
            });
        }

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    textCurrentTime.setText(formatTime(progress));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                stopProgressUpdate();
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                MusicPlayerManager.getInstance().seekTo(seekBar.getProgress());
                startProgressUpdate();
            }
        });
    }

    private void showMoreActionsMenu(View view) {
        Song currentSong = MusicPlayerManager.getInstance().getCurrentSong();
        if (currentSong == null) return;

        boolean canDownload = btnDownload != null && btnDownload.getVisibility() == View.VISIBLE;

        com.appad.components.PlayerActionsBottomSheet sheet = com.appad.components.PlayerActionsBottomSheet.newInstance(
            currentSong, 
            isFavoriteCurrent, 
            canDownload,
            actionId -> {
                switch (actionId) {
                    case 1: // Favorite
                        toggleFavorite();
                        break;
                    case 2: // Lyrics
                        LyricsBottomSheetFragment.newInstance(currentSong)
                                .show(getSupportFragmentManager(), "LyricsSheet");
                        break;
                    case 3: // Comments
                        CommentsBottomSheetFragment.newInstance(currentSong)
                                .show(getSupportFragmentManager(), "CommentsSheet");
                        break;
                    case 4: // Queue
                        toggleContentSelection(queueContainer);
                        if (queueContainer.getVisibility() == View.VISIBLE) {
                            setupQueueList();
                        }
                        break;
                    case 5: // Sleep Timer
                        showSleepTimerDialog();
                        break;
                    case 6: // Add to Playlist
                        com.appad.components.AddToPlaylistFragment.newInstance(currentSong)
                                .show(getSupportFragmentManager(), "AddToPlaylistSheet");
                        break;
                    case 7: // Download
                        startDownload(currentSong);
                        break;
                    case 8: // Report
                        com.appad.components.ReportDialogFragment.newInstance(currentSong)
                                .show(getSupportFragmentManager(), "ReportDialog");
                        break;
                }
            }
        );
        sheet.show(getSupportFragmentManager(), "PlayerActionsSheet");
    }

    private void toggleContentSelection(View target) {
        boolean isNowVisible = target.getVisibility() == View.VISIBLE;
        
        // Hide all major containers
        if (lyricsCard != null) lyricsCard.setVisibility(View.GONE);
        if (commentsContainer != null) commentsContainer.setVisibility(View.GONE);
        if (queueContainer != null) queueContainer.setVisibility(View.GONE);
        

        if (!isNowVisible) {
            target.setVisibility(View.VISIBLE);
        } else {
            // Revert to Queue if toggling off something else
            if (target != queueContainer && queueContainer != null) {
                queueContainer.setVisibility(View.VISIBLE);
                setupQueueList();
            }
        }
        
        updateActionButtonTints();
    }

    private void updateActionButtonTints() {
        int activeColor = androidx.core.content.ContextCompat.getColor(this, R.color.vibrant_purple);
        int inactiveColor = android.graphics.Color.WHITE;
        
        if (btnLyrics != null) btnLyrics.setColorFilter(lyricsCard.getVisibility() == View.VISIBLE ? activeColor : inactiveColor);
        if (btnComments != null) btnComments.setColorFilter(commentsContainer.getVisibility() == View.VISIBLE ? activeColor : inactiveColor);
        if (btnMoreActions != null) btnMoreActions.setColorFilter(queueContainer.getVisibility() == View.VISIBLE ? activeColor : inactiveColor);
    }

    private void purchaseSong() {
        Song currentSong = MusicPlayerManager.getInstance().getCurrentSong();
        if (currentSong == null) return;
        
        PremiumAccessModal.newInstance(currentSong, false, () -> {
            checkDownloadAccess();
        }).show(getSupportFragmentManager(), "PremiumAccessModal");
    }

    private void toggleFavorite() {
        Song currentSong = MusicPlayerManager.getInstance().getCurrentSong();
        if (currentSong == null) return;

        Integer userId = SessionManager.getInstance(this).getUserId();
        if (userId == null) {
            Toast.makeText(this, "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show();
            return;
        }

        java.util.Map<String, Integer> payload = new java.util.HashMap<>();
        payload.put("userId", userId);
        payload.put("songId", currentSong.getSongId());

        RetrofitClient.getApiService().toggleFavorite(payload).enqueue(new retrofit2.Callback<java.util.Map<String, Object>>() {
            @Override
                public void onResponse(retrofit2.Call<java.util.Map<String, Object>> call, retrofit2.Response<java.util.Map<String, Object>> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        Object favObj = response.body().get("isFavorite");
                        boolean isFav = Boolean.TRUE.equals(favObj); // Fix casting error
                        android.util.Log.d("FullPlayer", "Favorite toggled: " + isFav + " for song ID: " + currentSong.getSongId());
                        updateFavoriteIcon(isFav);
                        Toast.makeText(FullPlayerActivity.this, isFav ? "Đã thêm vào yêu thích" : "Đã xóa khỏi yêu thích", Toast.LENGTH_SHORT).show();
                    } else {
                        android.util.Log.e("FullPlayer", "Failed to toggle favorite. Response code: " + response.code() + ", message: " + response.message());
                        Toast.makeText(FullPlayerActivity.this, "Không thể cập nhật yêu thích", Toast.LENGTH_SHORT).show();
                    }
                }
            @Override
            public void onFailure(retrofit2.Call<java.util.Map<String, Object>> call, Throwable t) {
                android.util.Log.e("FullPlayer", "API call to toggle favorite failed: " + t.getMessage(), t);
                Toast.makeText(FullPlayerActivity.this, "Lỗi kết nối khi cập nhật yêu thích", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateFavoriteIcon(boolean isFavorite) {
        isFavoriteCurrent = isFavorite;
        btnFavorite.setImageResource(isFavorite ? R.drawable.ic_heart_filled : R.drawable.ic_heart_outline);
        if (isFavorite) {
            btnFavorite.setColorFilter(android.graphics.Color.parseColor("#E91E63")); // Pink/Red heart
        } else {
            btnFavorite.clearColorFilter();
        }
    }

    private void checkPremiumAccess() {
        Song currentSong = MusicPlayerManager.getInstance().getCurrentSong();
        if (currentSong == null) return;

        com.appad.utils.AccessHelper.checkAccess(this, currentSong, true, new com.appad.utils.AccessHelper.AccessCallback() {
            @Override
            public void onAccessGranted() {
                if (btnBuySong != null) btnBuySong.setVisibility(View.GONE);
            }

            @Override
            public void onAccessDenied(Song song) {
                // Nếu đang phát mà không có quyền (do tự chuyển bài), thì dừng lại
                if (MusicPlayerManager.getInstance().isPlaying()) {
                    MusicPlayerManager.getInstance().togglePlayPause();
                }
                if (btnBuySong != null) btnBuySong.setVisibility(View.VISIBLE);
            }
        });
    }

    private void checkDownloadAccess() {
        if (btnDownload == null) return;
        Song currentSong = MusicPlayerManager.getInstance().getCurrentSong();
        if (currentSong == null) {
            btnDownload.setVisibility(View.GONE);
            return;
        }

        // We ALWAYS show the download button now as per request, unless already downloaded
        List<Song> downloads = com.appad.utils.DownloadHelper.getInstance(this).getDownloadedSongs();
        for (Song s : downloads) {
            if (s.getSongId().equals(currentSong.getSongId())) {
                btnDownload.setVisibility(View.GONE);
                return;
            }
        }
        
        btnDownload.setVisibility(View.VISIBLE);
        btnDownload.setAlpha(1.0f);
    }

    private void startDownload(Song song) {
        if (song == null || song.getFileUrl() == null) {
            Toast.makeText(this, "Không tìm thấy file để tải.", Toast.LENGTH_SHORT).show();
            return;
        }

        // 1. Check Access first before allowing download
        com.appad.utils.AccessHelper.checkDownloadAccess(this, song, new com.appad.utils.AccessHelper.AccessCallback() {
            @Override
            public void onAccessGranted() {
                com.appad.components.CustomAlertDialogFragment.newInstance(
                    com.appad.components.CustomAlertDialogFragment.AlertType.INFO,
                    "Tải nhạc",
                    "Bạn có muốn tải bài hát '" + song.getTitle() + "' về máy không?"
                ).setPrimaryButton("Tải về", () -> proceedWithDownload(song))
                 .setSecondaryButton("Hủy", null)
                 .show(getSupportFragmentManager(), "DownloadConfirmDialog");
            }

            @Override
            public void onAccessDenied(Song deniedSong) {
                // Always use PremiumAccessModal to show the appropriate choices
                PremiumAccessModal.newInstance(deniedSong, true, () -> {
                    // Refresh UI after successful purchase/upgrade
                    checkDownloadAccess();
                    checkPremiumAccess();
                }).show(getSupportFragmentManager(), "PremiumAccessModal");
            }
        });
    }

    private void proceedWithDownload(Song song) {
        // Check permission for older Android
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.Q) {
            if (androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE) 
                != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                androidx.core.app.ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, 100);
                Toast.makeText(this, "Vui lòng cấp quyền bộ nhớ để tải nhạc.", Toast.LENGTH_SHORT).show();
                return;
            }
        }

        String fileUrl = ImageUrlUtils.fixUrl(song.getFileUrl());
        String fileName = song.getTitle() + " - " + song.getArtistName() + ".mp3";

        try {
            android.app.DownloadManager.Request request = new android.app.DownloadManager.Request(android.net.Uri.parse(fileUrl));
            request.setTitle("Đang tải: " + song.getTitle());
            request.setDescription("Đang lưu bài hát về thiết bị...");
            request.setNotificationVisibility(android.app.DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            
            // Sử dụng thư mục riêng của App để được tính vào dung lượng App
            File appMusicDir = getExternalFilesDir(android.os.Environment.DIRECTORY_MUSIC);
            if (appMusicDir != null) {
                request.setDestinationInExternalFilesDir(this, android.os.Environment.DIRECTORY_MUSIC, fileName);
            } else {
                request.setDestinationInExternalPublicDir(android.os.Environment.DIRECTORY_MUSIC, fileName);
            }
            
            request.setAllowedOverMetered(true);
            request.setAllowedOverRoaming(true);

            android.app.DownloadManager downloadManager = (android.app.DownloadManager) getSystemService(DOWNLOAD_SERVICE);
            if (downloadManager != null) {
                downloadManager.enqueue(request);
                
                // Track download locally
                File targetFile = new File(appMusicDir != null ? appMusicDir : android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_MUSIC), fileName);
                String localPath = targetFile.getAbsolutePath();
                com.appad.utils.DownloadHelper.getInstance(this).saveDownload(song, localPath);
                
                Toast.makeText(this, "Đã bắt đầu tải bài hát.", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(this, "Lỗi khi bắt đầu tải: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void loadLyrics() {
        Song currentSong = MusicPlayerManager.getInstance().getCurrentSong();
        if (currentSong == null) return;

        textLyrics.setText("Đang tải lời bài hát...");
        RetrofitClient.getApiService().getSongById(currentSong.getSongId()).enqueue(new retrofit2.Callback<java.util.Map<String, Object>>() {
            @Override
            public void onResponse(retrofit2.Call<java.util.Map<String, Object>> call, retrofit2.Response<java.util.Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    java.util.Map<String, Object> data = (java.util.Map<String, Object>) response.body().get("data");
                    if (data != null) {
                        String lyrics = (String) data.get("lyrics");
                        if (lyrics != null && !lyrics.isEmpty()) {
                            textLyrics.setText(lyrics);
                        } else {
                            textLyrics.setText("Bài hát này chưa có lời.");
                        }
                        
                        // Update Missing Genre Info if needed
                        String gName = null;
                        if (data.containsKey("genre_name")) gName = (String) data.get("genre_name");
                        if (gName == null && data.containsKey("genreName")) gName = (String) data.get("genreName");

                        if (gName != null && (gName.trim().equalsIgnoreCase("nhạc") || gName.trim().equalsIgnoreCase("music"))) {
                            gName = null;
                        }

                        if (gName != null && !gName.isEmpty()) {
                                if (textGenre != null) textGenre.setText(gName);
                                if (textFullGenre != null) {
                                    String displayGenre = gName;
                                    if (!gName.toLowerCase().startsWith("nhạc")) {
                                        displayGenre = "Nhạc " + gName;
                                    }
                                    textFullGenre.setText(displayGenre);
                                    textFullGenre.setVisibility(View.VISIBLE);
                                    if (textSeparator != null) textSeparator.setVisibility(View.VISIBLE);
                                }
                                
                                // Update current song model locally
                                currentSong.setGenreName(gName);
                                if (data.containsKey("genre_id")) {
                                     Object gIdObj = data.get("genre_id");
                                     if (gIdObj instanceof Number) {
                                         int gId = ((Number) gIdObj).intValue();
                                         currentSong.setGenreId(gId);
                                         
                                         // Update click listener
                                         if (textFullGenre != null) {
                                             String finalGName = gName;
                                             textFullGenre.setOnClickListener(v -> {
                                                 android.content.Intent i = new android.content.Intent(FullPlayerActivity.this, com.appad.activities.GenreDetailActivity.class);
                                                 i.putExtra("genreId", gId);
                                                 i.putExtra("genreName", finalGName);
                                                 startActivity(i);
                                             });
                                         }
                                     }
                                }
                            }
                        }
                    }
                }


            @Override
            public void onFailure(retrofit2.Call<java.util.Map<String, Object>> call, Throwable t) {
                textLyrics.setText("Không thể tải lời bài hát.");
            }
        });
    }

    private void checkFavoriteStatus() {
        Song currentSong = MusicPlayerManager.getInstance().getCurrentSong();
        if (currentSong == null) return;

        Integer userId = SessionManager.getInstance(this).getUserId();
        if (userId == null) return;

        RetrofitClient.getApiService().checkFavorite(userId, currentSong.getSongId()).enqueue(new retrofit2.Callback<java.util.Map<String, Object>>() {
            @Override
                public void onResponse(retrofit2.Call<java.util.Map<String, Object>> call, retrofit2.Response<java.util.Map<String, Object>> response) {
                    if (response.isSuccessful() && response.body() != null && response.body().containsKey("isFavorite")) {
                        Object favObj = response.body().get("isFavorite");
                        boolean isFav = Boolean.TRUE.equals(favObj);
                        updateFavoriteIcon(isFav);
                    }
                }
            @Override
            public void onFailure(retrofit2.Call<java.util.Map<String, Object>> call, Throwable t) {}
        });
    }

    private void switchQueueTab(String tab) {
        activeQueueTab = tab;
        if (tab.equals("queue")) {
            btnTabQueue.setTextColor(android.graphics.Color.WHITE);
            btnTabQueue.setBackgroundResource(R.drawable.bg_tab_purple);
            btnTabRelated.setTextColor(android.graphics.Color.parseColor("#94A3B8"));
            btnTabRelated.setBackground(null);
        } else {
            btnTabRelated.setTextColor(android.graphics.Color.WHITE);
            btnTabRelated.setBackgroundResource(R.drawable.bg_tab_purple);
            btnTabQueue.setTextColor(android.graphics.Color.parseColor("#94A3B8"));
            btnTabQueue.setBackground(null);
            if (relatedSongs.isEmpty()) {
                loadRelatedSongs();
            }
        }
        setupQueueList();
    }

    private void setupQueueList() {
        if (rvQueue == null) return;
        
        if (activeQueueTab.equals("queue")) {
            List<Song> fullQueue = MusicPlayerManager.getInstance().getPlaylist();
            Song current = MusicPlayerManager.getInstance().getCurrentSong();
            
            // Fix NPE: Check if current song is null
            if (current == null) return;

            int currentIdx = -1;
            for (int i = 0; i < fullQueue.size(); i++) {
                Song queueSong = fullQueue.get(i);
                // Skip null elements in queue
                if (queueSong == null || queueSong.getSongId() == null) continue;
                
                if (queueSong.getSongId().equals(current.getSongId())) {
                    currentIdx = i;
                    break;
                }
            }
            
            List<Song> nextSongs = new ArrayList<>();
            if (currentIdx != -1) {
                // Find next 5 songs that user HAS access to
                int i = 1;
                while (nextSongs.size() < 5 && i < fullQueue.size()) {
                    int nextIdx = (currentIdx + i) % fullQueue.size();
                    Song nextSong = fullQueue.get(nextIdx);
                    if (nextSong != null && MusicPlayerManager.getInstance().hasAccess(nextSong)) {
                        nextSongs.add(nextSong);
                    }
                    i++;
                    if (nextIdx == currentIdx) break; // Wrapped around
                }
            }
            
            // Nếu không có bài tiếp theo (danh sách chỉ có 1 bài), hiển thị gợi ý
            if (nextSongs.isEmpty() && !relatedSongs.isEmpty()) {
                com.appad.adapters.SongAdapter adapter = new com.appad.adapters.SongAdapter(this, relatedSongs);
                rvQueue.setAdapter(adapter);
            } else {
                com.appad.adapters.SongAdapter adapter = new com.appad.adapters.SongAdapter(this, nextSongs);
                rvQueue.setAdapter(adapter);
            }
        } else {
            com.appad.adapters.SongAdapter adapter = new com.appad.adapters.SongAdapter(this, relatedSongs);
            rvQueue.setAdapter(adapter);
        }
    }

    private void loadRelatedSongs() {
        Song current = MusicPlayerManager.getInstance().getCurrentSong();
        if (current == null || pbQueue == null || isLoadingRelated) return;

        isLoadingRelated = true;
        pbQueue.setVisibility(View.VISIBLE);
        rvQueue.setVisibility(View.GONE);

        RetrofitClient.getApiService().getRecommendedSongs(5, 0).enqueue(new retrofit2.Callback<java.util.Map<String, Object>>() {
            @Override
            public void onResponse(retrofit2.Call<java.util.Map<String, Object>> call, retrofit2.Response<java.util.Map<String, Object>> response) {
                isLoadingRelated = false;
                pbQueue.setVisibility(View.GONE);
                rvQueue.setVisibility(View.VISIBLE);
                if (response.isSuccessful() && response.body() != null) {
                    Object data = response.body().get("data");
                    if (data instanceof java.util.List) {
                        String json = new Gson().toJson(data);
                        java.util.List<Song> songs = new Gson().fromJson(json, new TypeToken<List<Song>>(){}.getType());
                        if (songs != null) {
                            relatedSongs.clear();
                            // Exclude current song if it appears in recommendations
                            for (Song s : songs) {
                                if (s.getSongId() != null && !s.getSongId().equals(current.getSongId())) {
                                    relatedSongs.add(s);
                                }
                                if (relatedSongs.size() >= 5) break;
                            }
                            // Cập nhật list
                            setupQueueList();
                        }
                    }
                }
            }

            @Override
            public void onFailure(retrofit2.Call<java.util.Map<String, Object>> call, Throwable t) {
                isLoadingRelated = false;
                pbQueue.setVisibility(View.GONE);
                rvQueue.setVisibility(View.VISIBLE);
            }
        });
    }

    private void updateQueueListSelection() {
        // Highlighting logic now handled by SongAdapter which we updated earlier
        if (rvQueue == null || rvQueue.getAdapter() == null) return;
        rvQueue.getAdapter().notifyDataSetChanged();
    }

    private void loadComments() {
        Song current = MusicPlayerManager.getInstance().getCurrentSong();
        if (current == null) return;

        RetrofitClient.getApiService().getComments("song", current.getSongId()).enqueue(new retrofit2.Callback<java.util.Map<String, Object>>() {
            @Override
            public void onResponse(retrofit2.Call<java.util.Map<String, Object>> call, retrofit2.Response<java.util.Map<String, Object>> response) {
                android.util.Log.d("FullPlayer", "Comment API Response: " + new Gson().toJson(response.body()));
                if (response.isSuccessful() && response.body() != null) {
                    java.util.Map<String, Object> body = response.body();
                    Object dataObj = body.get("data");
                    Object commentsObj = null;
                    
                    if (dataObj instanceof java.util.Map) {
                        commentsObj = ((java.util.Map) dataObj).get("comments");
                    }
                    
                    java.util.List<java.util.Map<String, Object>> comments = null;
                    
                    if (commentsObj instanceof java.util.List) {
                        String json = new Gson().toJson(commentsObj);
                        comments = new Gson().fromJson(json, new TypeToken<List<java.util.Map<String, Object>>>(){}.getType());
                    }

                    if (comments != null && !comments.isEmpty()) {
                        android.util.Log.d("FullPlayer", "Loaded " + comments.size() + " comments");
                        Integer currentUserId = SessionManager.getInstance(FullPlayerActivity.this).getUserId();
                        
                        // Check if current user has a comment to enable "Edit" mode locally
                        if (currentUserId != null) {
                            boolean foundOwn = false;
                            for (Map<String, Object> c : comments) {
                                Map<String, Object> u = (Map<String, Object>) c.get("user");
                                if (u != null) {
                                    Object uId = u.get("userId") != null ? u.get("userId") : u.get("user_id");
                                    if (uId instanceof Number && ((Number) uId).intValue() == currentUserId) {
                                        // Found current user's comment
                                        editComment.setText((String) c.get("content"));
                                        Object r = c.get("rating");
                                        if (r instanceof Number) {
                                            selectedRating = ((Number) r).intValue();
                                            updateRatingUI();
                                        }
                                        foundOwn = true;
                                        break;
                                    }
                                }
                            }
                            if (!foundOwn) {
                                editComment.setText("");
                                selectedRating = 0;
                                updateRatingUI();
                            }
                        }

                        com.appad.adapters.CommentsAdapter adapter = new com.appad.adapters.CommentsAdapter(
                            FullPlayerActivity.this, 
                            comments, 
                            currentUserId,
                            this::deleteCommentFromServer
                        );
                        rvComments.setAdapter(adapter);
                        rvComments.setVisibility(View.VISIBLE);
                        if (txtNoComments != null) txtNoComments.setVisibility(View.GONE);
                    } else {
                        rvComments.setVisibility(View.GONE);
                        if (txtNoComments != null) txtNoComments.setVisibility(View.VISIBLE);
                    }
                } else {
                    android.util.Log.e("FullPlayer", "Failed to load comments: " + response.code());
                    if (txtNoComments != null) txtNoComments.setVisibility(View.VISIBLE);
                }
            }

            private void deleteCommentFromServer(long commentId) {
                RetrofitClient.getApiService().deleteComment(commentId).enqueue(new retrofit2.Callback<java.util.Map<String, Object>>() {
                    @Override
                    public void onResponse(retrofit2.Call<java.util.Map<String, Object>> call, retrofit2.Response<java.util.Map<String, Object>> response) {
                        if (response.isSuccessful()) {
                            Toast.makeText(FullPlayerActivity.this, "Đã xóa bình luận", Toast.LENGTH_SHORT).show();
                            loadComments(); // Refresh
                        }
                    }
                    @Override
                    public void onFailure(retrofit2.Call<java.util.Map<String, Object>> call, Throwable t) {}
                });
            }

            @Override
            public void onFailure(retrofit2.Call<java.util.Map<String, Object>> call, Throwable t) {
                android.util.Log.e("FullPlayer", "Failed to load comments", t);
                if (txtNoComments != null) txtNoComments.setVisibility(View.VISIBLE);
            }
        });
    }

    private void postComment() {
        Song current = MusicPlayerManager.getInstance().getCurrentSong();
        Integer userId = SessionManager.getInstance(this).getUserId();
        String content = editComment.getText().toString().trim();

        if (current == null) return;
        if (userId == null) {
            Toast.makeText(this, "Vui lòng đăng nhập để bình luận", Toast.LENGTH_SHORT).show();
            return;
        }
        if (content.isEmpty()) return;

        Map<String, Object> payload = new java.util.HashMap<>();
        payload.put("userId", userId);
        payload.put("targetId", current.getSongId());
        payload.put("type", "song");
        payload.put("content", content);
        if (selectedRating > 0) {
            payload.put("rating", selectedRating);
        }

        RetrofitClient.getApiService().addComment(payload).enqueue(new retrofit2.Callback<java.util.Map<String, Object>>() {
            @Override
            public void onResponse(retrofit2.Call<java.util.Map<String, Object>> call, retrofit2.Response<java.util.Map<String, Object>> response) {
                if (response.isSuccessful()) {
                    editComment.setText("");
                    selectedRating = 0;
                    updateRatingUI();
                    loadComments(); // Refresh comments
                }
            }

            @Override
            public void onFailure(retrofit2.Call<java.util.Map<String, Object>> call, Throwable t) {}
        });
    }

    // showPlaylistDialog removed - using AddToPlaylistFragment BottomSheet instead

    private void addSongToPlaylist(Integer playlistId, Integer songId) {
        java.util.Map<String, Long> payload = new java.util.HashMap<>();
        payload.put("song_id", songId.longValue());

        RetrofitClient.getApiService().addSongToPlaylist(playlistId, payload).enqueue(new retrofit2.Callback<java.util.Map<String, Object>>() {
            @Override
            public void onResponse(retrofit2.Call<java.util.Map<String, Object>> call, retrofit2.Response<java.util.Map<String, Object>> response) {
                if (response.isSuccessful()) {
                    Toast.makeText(FullPlayerActivity.this, "Đã thêm vào danh sách!", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(retrofit2.Call<java.util.Map<String, Object>> call, Throwable t) {}
        });
    }

    private void updateShuffleUI() {
        boolean shuffleOn = MusicPlayerManager.getInstance().isShuffle();
        if (shuffleOn) {
            btnShuffle.setImageResource(R.drawable.ic_shuffle_modern);
            btnShuffle.setBackgroundResource(R.drawable.bg_circle_purple);
            btnShuffle.setColorFilter(android.graphics.Color.WHITE);
        } else {
            btnShuffle.setImageResource(R.drawable.ic_shuffle_modern);
            btnShuffle.setBackgroundResource(R.drawable.bg_back_button_fixed);
            btnShuffle.setColorFilter(android.graphics.Color.WHITE);
        }   
    }

    private void updateRepeatUI() {
        int mode = MusicPlayerManager.getInstance().getRepeatMode();
        switch (mode) {
            case 0: // Off
                btnRepeat.setImageResource(R.drawable.ic_repeat_modern);
                btnRepeat.setBackgroundResource(R.drawable.bg_back_button_fixed);
                btnRepeat.setColorFilter(android.graphics.Color.WHITE);
                break;
            case 1: // Repeat All
                btnRepeat.setImageResource(R.drawable.ic_repeat_modern);
                btnRepeat.setBackgroundResource(R.drawable.bg_circle_purple);
                btnRepeat.setColorFilter(android.graphics.Color.WHITE);
                break;
            case 2: // Repeat One
                btnRepeat.setImageResource(R.drawable.ic_repeat_one_modern);
                btnRepeat.setBackgroundResource(R.drawable.bg_circle_purple);
                btnRepeat.setColorFilter(android.graphics.Color.WHITE);
                break;
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

    private void showSleepTimerDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_sleep_timer, null);
        builder.setView(dialogView);
        
        android.app.AlertDialog dialog = builder.create();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }
        
        // Find views
        android.widget.SeekBar timerSeekBar = dialogView.findViewById(R.id.timerSeekBar);
        android.widget.TextView textSliderValue = dialogView.findViewById(R.id.textSliderValue);
        android.widget.Button btnSetTimer = dialogView.findViewById(R.id.btnSetTimer);
        android.widget.Button btnCancelTimer = dialogView.findViewById(R.id.btnCancelTimer);
        android.widget.Button btnEndSongPlay = dialogView.findViewById(R.id.btnEndSongPlay);
        android.widget.TextView btnCloseDialog = dialogView.findViewById(R.id.btnCloseDialog);
        View activeTimerContainer = dialogView.findViewById(R.id.activeTimerContainer);
        View infiniteStatusContainer = dialogView.findViewById(R.id.infiniteStatusContainer);
        android.widget.TextView textNoTimer = dialogView.findViewById(R.id.textNoTimer);
        android.widget.TextView textTargetTime = dialogView.findViewById(R.id.textTargetTime);
        android.widget.TextView textRemainingTime = dialogView.findViewById(R.id.textRemainingTime);
        View sliderSection = dialogView.findViewById(R.id.sliderSection);
        
        // Set min to 1 minute (SeekBar starts at 0, so we add 1 offset)
        timerSeekBar.setMax(299); // 0-299 represents 1-300 minutes
        timerSeekBar.setProgress(29); // Default 30 minutes (30-1=29)
        
        // Update text display
        final Runnable updateSliderText = () -> {
            int minutes = timerSeekBar.getProgress() + 1; // +1 because min is 1
            if (minutes >= 60) {
                int hours = minutes / 60;
                int mins = minutes % 60;
                if (mins == 0) {
                    textSliderValue.setText(hours + " tiếng");
                } else {
                    textSliderValue.setText(hours + " tiếng " + mins + " phút");
                }
            } else {
                textSliderValue.setText(minutes + " phút");
            }
        };
        updateSliderText.run();
        
        timerSeekBar.setOnSeekBarChangeListener(new android.widget.SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(android.widget.SeekBar seekBar, int progress, boolean fromUser) {
                updateSliderText.run();
            }
            @Override
            public void onStartTrackingTouch(android.widget.SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(android.widget.SeekBar seekBar) {}
        });
        
        // Check current timer status
        boolean isEndSongMode = MusicPlayerManager.getInstance().isStopAfterCurrent();
        long sleepTarget = MusicPlayerManager.getInstance().getSleepTimerTarget();
        
        if (isEndSongMode) {
            infiniteStatusContainer.setVisibility(View.VISIBLE);
            activeTimerContainer.setVisibility(View.GONE);
            textNoTimer.setVisibility(View.GONE);
            btnCancelTimer.setVisibility(View.VISIBLE);
        } else if (sleepTarget > 0) {
            long remaining = sleepTarget - System.currentTimeMillis();
            if (remaining > 0) {
                // Timer is active
                activeTimerContainer.setVisibility(View.VISIBLE);
                infiniteStatusContainer.setVisibility(View.GONE);
                textNoTimer.setVisibility(View.GONE);
                btnCancelTimer.setVisibility(View.VISIBLE);
                
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault());
                textTargetTime.setText(sdf.format(new java.util.Date(sleepTarget)));
                
                int remainingMins = (int) (remaining / 60000);
                if (remainingMins >= 60) {
                    int h = remainingMins / 60;
                    int m = remainingMins % 60;
                    textRemainingTime.setText("(Còn khoảng " + h + " tiếng " + m + " phút)");
                } else {
                    textRemainingTime.setText("(Còn khoảng " + remainingMins + " phút)");
                }
            }
        } else {
            activeTimerContainer.setVisibility(View.GONE);
            infiniteStatusContainer.setVisibility(View.GONE);
            textNoTimer.setVisibility(View.VISIBLE);
            btnCancelTimer.setVisibility(View.GONE);
        }
        
        // Button actions
        btnSetTimer.setOnClickListener(v -> {
            int minutes = timerSeekBar.getProgress() + 1;
            MusicPlayerManager.getInstance().startSleepTimer(minutes);
            MusicPlayerManager.getInstance().setStopAfterCurrent(false);
            
            String msg;
            if (minutes >= 60) {
                int h = minutes / 60;
                int m = minutes % 60;
                if (m == 0) {
                    msg = "Sẽ tắt nhạc sau " + h + " tiếng";
                } else {
                    msg = "Sẽ tắt nhạc sau " + h + " tiếng " + m + " phút";
                }
            } else {
                msg = "Sẽ tắt nhạc sau " + minutes + " phút";
            }
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });
        
        btnCancelTimer.setOnClickListener(v -> {
            MusicPlayerManager.getInstance().cancelSleepTimer();
            MusicPlayerManager.getInstance().setStopAfterCurrent(false);
            Toast.makeText(this, "Đã hủy hẹn giờ", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });
        
        btnEndSongPlay.setOnClickListener(v -> {
            MusicPlayerManager.getInstance().setStopAfterCurrent(true);
            Toast.makeText(this, "Nhạc sẽ tắt sau bài hát này", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });
        
        btnCloseDialog.setOnClickListener(v -> dialog.dismiss());
        
        dialog.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (statusChangeListener != null) {
            MusicPlayerManager.getInstance().removeStatusChangeListener(statusChangeListener);
        }
        stopProgressUpdate();
    }
}
