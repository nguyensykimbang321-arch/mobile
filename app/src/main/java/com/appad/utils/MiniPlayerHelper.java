package com.appad.utils;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.content.Intent;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.graphics.ColorUtils;
import androidx.palette.graphics.Palette;
import com.appad.R;
import com.appad.activities.FullPlayerActivity;
import com.appad.models.Song;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;

public class MiniPlayerHelper {

    private Activity activity;
    private MusicPlayerManager.OnPlayerStatusChangeListener playerStatusChangeListener;

    public MiniPlayerHelper(Activity activity) {
        this.activity = activity;
    }

    public void setupMiniPlayer() {
        View miniContainer = activity.findViewById(R.id.miniPlayerContainer);
        if (miniContainer == null) return;

        ImageView imgMini = activity.findViewById(R.id.imgMiniAlbum);
        TextView txtTitle = activity.findViewById(R.id.textMiniSongTitle);
        TextView txtArtist = activity.findViewById(R.id.textMiniArtistName);
        ImageButton btnPlay = activity.findViewById(R.id.btnMiniPlayPause);
        ImageButton btnPrev = activity.findViewById(R.id.btnMiniPrevious);
        ImageButton btnNext = activity.findViewById(R.id.btnMiniNext);
        ImageButton btnClose = activity.findViewById(R.id.btnMiniClose);
        ImageButton btnMinimize = activity.findViewById(R.id.btnMiniMinimize);
        TextView txtCurrent = activity.findViewById(R.id.textMiniCurrentTime);
        TextView txtDuration = activity.findViewById(R.id.textMiniDuration);
        SeekBar seekBar = activity.findViewById(R.id.miniPlayerProgress);
        
        View miniExpanded = activity.findViewById(R.id.miniPlayerExpanded);
        View miniCollapsed = activity.findViewById(R.id.miniPlayerCollapsed);
        View pulseView = activity.findViewById(R.id.viewPulseEffect);

        if (miniExpanded != null) {
            miniExpanded.setOnClickListener(v -> {
                Song currentSong = MusicPlayerManager.getInstance().getCurrentSong();
                if (currentSong != null) {
                    Intent intent = new Intent(activity, FullPlayerActivity.class);
                    activity.startActivity(intent);
                }
            });
        }
        
        if (miniCollapsed != null) {
            miniCollapsed.setOnClickListener(v -> {
                MusicPlayerManager.getInstance().setMiniPlayerMinimized(false);
            });
        }

        if (playerStatusChangeListener == null) {
            playerStatusChangeListener = new MusicPlayerManager.OnPlayerStatusChangeListener() {
                private ObjectAnimator scaleXC, scaleYC, alphaC;
                private ObjectAnimator scaleXBtn, scaleYBtn;
                
                @Override
                public void onSongChanged(Song song) {
                    if (song == null) {
                        miniContainer.setVisibility(View.GONE);
                        return;
                    }
                    
                    // Hiển thị mini player ngay lập tức khi chọn bài
                    miniContainer.setVisibility(View.VISIBLE); 
                    
                    if (txtTitle != null) txtTitle.setText(song.getTitle());
                    if (txtArtist != null) txtArtist.setText(song.getArtistName());
                    
                    updateMiniPlayerVisibility(MusicPlayerManager.getInstance().isMiniPlayerMinimized());
                    
                    if (imgMini != null) {
                        Glide.with(activity)
                                .asBitmap()
                                .load(ImageUrlUtils.fixUrl(song.getCoverUrl()))
                                .into(new CustomTarget<Bitmap>() {
                                    @Override
                                    public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                                        imgMini.setImageBitmap(resource);
                                        Palette.from(resource).generate(palette -> {
                                            if (palette != null) {
                                                updateMiniPlayerBackground(palette);
                                            }
                                        });
                                    }

                                    @Override
                                    public void onLoadCleared(@Nullable Drawable placeholder) {
                                        imgMini.setImageResource(R.drawable.ic_launcher_background);
                                    }
                                });
                    }

                    // Fix button colors and visibility
                    if (btnClose != null) btnClose.setColorFilter(Color.parseColor("#E5E7EB"));
                    if (btnMinimize != null) btnMinimize.setColorFilter(Color.parseColor("#E5E7EB"));
                    if (btnPrev != null) btnPrev.setColorFilter(Color.WHITE);
                    if (btnNext != null) btnNext.setColorFilter(Color.WHITE);
                    if (btnPlay != null) btnPlay.setColorFilter(Color.WHITE);
                    
                    // Kích hoạt hiệu ứng visual ngay lập tức nếu đang trong mode auto-play
                    // Dùng check isPlaying() của manager để đảm bảo tính nhất quán
                    if (MusicPlayerManager.getInstance().isPlaying() || MusicPlayerManager.getInstance().getPlaylist().size() > 0) {
                         onPlaybackStatusChanged(true);
                    }
                }

                @Override
                public void onPlaybackStatusChanged(boolean isPlaying) {
                    if (isPlaying) {
                        miniContainer.setVisibility(View.VISIBLE);
                    } else {
                        // Nếu dừng hẳn (không có song) thì ẩn, còn nếu chỉ pause thì có thể giữ nguyên 
                        // nhưng theo yêu cầu "tránh chạy ngầm" thì có thể ẩn nếu muốn.
                        // Tuy nhiên, thông thường pause vẫn hiện để user nhấn play lại.
                        // Ở đây ta giữ logic: nếu đang chơi thì chắc chắn hiện.
                        if (MusicPlayerManager.getInstance().getCurrentSong() == null) {
                            miniContainer.setVisibility(View.GONE);
                        }
                    }

                    if (btnPlay != null) {
                        if (isPlaying) {
                            if (scaleXBtn == null) {
                                scaleXBtn = ObjectAnimator.ofFloat(btnPlay.getParent(), "scaleX", 1f, 1.1f);
                                scaleXBtn.setRepeatCount(ValueAnimator.INFINITE);
                                scaleXBtn.setRepeatMode(ValueAnimator.REVERSE);
                                scaleXBtn.setDuration(800);

                                scaleYBtn = ObjectAnimator.ofFloat(btnPlay.getParent(), "scaleY", 1f, 1.1f);
                                scaleYBtn.setRepeatCount(ValueAnimator.INFINITE);
                                scaleYBtn.setRepeatMode(ValueAnimator.REVERSE);
                                scaleYBtn.setDuration(800);
                            }
                            if (!scaleXBtn.isStarted()) {
                                scaleXBtn.start();
                                scaleYBtn.start();
                            }
                        } else {
                            if (scaleXBtn != null) {
                                scaleXBtn.cancel();
                                scaleYBtn.cancel();
                                ((View)btnPlay.getParent()).setScaleX(1f);
                                ((View)btnPlay.getParent()).setScaleY(1f);
                            }
                        }
                    }

                    if (pulseView != null) {
                        if (isPlaying) {
                            if (scaleXC == null) {
                                scaleXC = ObjectAnimator.ofFloat(pulseView, "scaleX", 1f, 2.2f);
                                scaleXC.setRepeatCount(ValueAnimator.INFINITE);
                                scaleXC.setDuration(2000);

                                scaleYC = ObjectAnimator.ofFloat(pulseView, "scaleY", 1f, 2.2f);
                                scaleYC.setRepeatCount(ValueAnimator.INFINITE);
                                scaleYC.setDuration(2000);

                                alphaC = ObjectAnimator.ofFloat(pulseView, "alpha", 0.5f, 0f);
                                alphaC.setRepeatCount(ValueAnimator.INFINITE);
                                alphaC.setDuration(2000);
                            }
                            if (!scaleXC.isStarted()) {
                                scaleXC.start();
                                scaleYC.start();
                                alphaC.start();
                            }
                        } else {
                            if (scaleXC != null) {
                                scaleXC.cancel();
                                scaleYC.cancel();
                                alphaC.cancel();
                                pulseView.setScaleX(1f);
                                pulseView.setScaleY(1f);
                                pulseView.setAlpha(0f);
                            }
                        }
                    }

                    if (btnPlay != null) {
                        btnPlay.setImageResource(isPlaying ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play);
                    }
                }

                @Override
                public void onProgressUpdated(int position, int duration) {
                    if (seekBar != null) {
                        seekBar.setMax(duration);
                        seekBar.setProgress(position);
                    }
                    if (txtCurrent != null) txtCurrent.setText(formatTime(position));
                    if (txtDuration != null) txtDuration.setText(formatTime(duration));
                }
                
                @Override
                public void onInactivityPause() {
                    // Handled by activity or a global listener if needed
                }

                @Override
                public void onMiniPlayerStateChanged(boolean isMinimized) {
                    updateMiniPlayerVisibility(isMinimized);
                }
            };
        }
        
        MusicPlayerManager.getInstance().addStatusChangeListener(playerStatusChangeListener);

        if (seekBar != null) {
            seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
                @Override public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                    if (fromUser && txtCurrent != null) txtCurrent.setText(formatTime(progress));
                }
                @Override public void onStartTrackingTouch(SeekBar seekBar) {}
                @Override public void onStopTrackingTouch(SeekBar seekBar) {
                    MusicPlayerManager.getInstance().seekTo(seekBar.getProgress());
                }
            });
        }

        if (btnPlay != null) btnPlay.setOnClickListener(v -> MusicPlayerManager.getInstance().togglePlayPause());
        if (btnPrev != null) btnPrev.setOnClickListener(v -> MusicPlayerManager.getInstance().playPrevious());
        if (btnNext != null) btnNext.setOnClickListener(v -> MusicPlayerManager.getInstance().playNext());

        if (btnClose != null) {
            btnClose.setOnClickListener(v -> {
                MusicPlayerManager.getInstance().stopMusic();
                miniContainer.setVisibility(View.GONE);
                if (miniExpanded != null) miniExpanded.setVisibility(View.VISIBLE);
                if (miniCollapsed != null) miniCollapsed.setVisibility(View.GONE);
            });
        }

        if (btnMinimize != null) {
            btnMinimize.setOnClickListener(v -> {
                MusicPlayerManager.getInstance().setMiniPlayerMinimized(true);
            });
        }

        // Initial update
        Song current = MusicPlayerManager.getInstance().getCurrentSong();
        if (current != null) {
            playerStatusChangeListener.onSongChanged(current);
            playerStatusChangeListener.onPlaybackStatusChanged(MusicPlayerManager.getInstance().isPlaying());
            updateMiniPlayerVisibility(MusicPlayerManager.getInstance().isMiniPlayerMinimized());
        } else {
            miniContainer.setVisibility(View.GONE);
        }
    }

    private void updateMiniPlayerVisibility(boolean isMinimized) {
        View miniExpanded = activity.findViewById(R.id.miniPlayerExpanded);
        View miniCollapsed = activity.findViewById(R.id.miniPlayerCollapsed);
        
        if (miniExpanded != null && miniCollapsed != null) {
            if (isMinimized) {
                // Thu nhỏ
                if (miniExpanded.getVisibility() == View.VISIBLE) {
                    miniExpanded.animate()
                        .alpha(0f)
                        .setDuration(200)
                        .withEndAction(() -> {
                            miniExpanded.setVisibility(View.GONE);
                            miniCollapsed.setVisibility(View.VISIBLE);
                            miniCollapsed.setAlpha(0f);
                            miniCollapsed.animate().alpha(1f).setDuration(200).start();
                        })
                        .start();
                } else {
                    miniExpanded.setVisibility(View.GONE);
                    miniCollapsed.setVisibility(View.VISIBLE);
                    miniCollapsed.setAlpha(1f);
                }
            } else {
                // Mở rộng
                if (miniCollapsed.getVisibility() == View.VISIBLE) {
                    miniCollapsed.animate()
                        .alpha(0f)
                        .setDuration(200)
                        .withEndAction(() -> {
                            miniCollapsed.setVisibility(View.GONE);
                            miniExpanded.setVisibility(View.VISIBLE);
                            miniExpanded.setAlpha(0f);
                            miniExpanded.animate().alpha(1f).setDuration(200).start();
                        })
                        .start();
                } else {
                    miniCollapsed.setVisibility(View.GONE);
                    miniExpanded.setVisibility(View.VISIBLE);
                    miniExpanded.setAlpha(1f);
                }
            }
        }
    }

    public void detach() {
        if (playerStatusChangeListener != null) {
            MusicPlayerManager.getInstance().removeStatusChangeListener(playerStatusChangeListener);
        }
    }

    private void updateMiniPlayerBackground(Palette palette) {
        View miniExpanded = activity.findViewById(R.id.miniPlayerExpanded);
        View miniCollapsed = activity.findViewById(R.id.miniPlayerCollapsed);
        
        float density = activity.getResources().getDisplayMetrics().density;
        
        // Trích xuất bộ màu phong phú hơn từ palette
        int dominantColor = palette.getDominantColor(0xFF333333);
        int vibrantColor = palette.getVibrantColor(dominantColor);
        int mutedColor = palette.getMutedColor(dominantColor);
        
        if (miniExpanded != null) {
            // Tạo 3 điểm dừng màu cho gradient (3-stop gradient)
            // Màu 1: Vibrant (làm sáng một chút)
            int color1 = ColorUtils.blendARGB(vibrantColor, Color.WHITE, 0.1f);
            color1 = ColorUtils.blendARGB(color1, Color.BLACK, 0.2f); // Đảm bảo không quá chói
            
            // Màu 2: Dominant (trộn nhẹ)
            int color2 = ColorUtils.blendARGB(dominantColor, Color.BLACK, 0.3f);
            
            // Màu 3: Muted (tối hơn một chút để tạo chiều sâu)
            int color3 = ColorUtils.blendARGB(mutedColor, Color.BLACK, 0.5f);

            GradientDrawable gd = new GradientDrawable(
                    GradientDrawable.Orientation.TL_BR, // Top-Left to Bottom-Right cho hiện đại
                    new int[]{color1, color2, color3}
            );
            gd.setCornerRadius(density * 24); 
            gd.setStroke((int) (density * 1.5), Color.parseColor("#55FFFFFF")); 
            miniExpanded.setBackground(gd);
        }

        if (miniCollapsed != null) {
            // Màu cho bản pill cũng dùng 2 màu gradient để đồng bộ
            int c1 = ColorUtils.blendARGB(vibrantColor, Color.BLACK, 0.2f);
            int c2 = ColorUtils.blendARGB(dominantColor, Color.BLACK, 0.4f);
            
            GradientDrawable cgd = new GradientDrawable(
                    GradientDrawable.Orientation.LEFT_RIGHT,
                    new int[]{c1, c2}
            );
            float r = density * 30;
            cgd.setCornerRadii(new float[]{r, r, 0, 0, 0, 0, r, r});
            cgd.setStroke((int) (density * 1), Color.parseColor("#44FFFFFF"));
            miniCollapsed.setBackground(cgd);
        }
    }

    private String formatTime(int ms) {
        int minutes = (ms / 1000) / 60;
        int seconds = (ms / 1000) % 60;
        return String.format("%d:%02d", minutes, seconds);
    }
}
