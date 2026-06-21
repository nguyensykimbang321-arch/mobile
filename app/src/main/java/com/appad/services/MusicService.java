package com.appad.services;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Binder;
import android.os.IBinder;
import androidx.core.app.NotificationCompat;
import com.appad.R;
import com.appad.activities.FullPlayerActivity;
import com.appad.models.Song;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.PlaybackException;
import com.google.android.exoplayer2.Player;

public class MusicService extends Service {

    private final IBinder binder = new MusicBinder();
    private ExoPlayer exoPlayer;
    private Song currentSong;
    private OnStatusChangeListener listener;
    private android.net.wifi.WifiManager.WifiLock wifiLock;
    private android.os.PowerManager.WakeLock transitionWakeLock;

    public interface OnStatusChangeListener {
        void onSongChanged(Song song);
        void onPlaybackStatusChanged(boolean isPlaying);
    }

    public class MusicBinder extends Binder {
        public MusicService getService() {
            return MusicService.this;
        }
    }

    private boolean isPrepared = false;

    @Override
    public void onCreate() {
        super.onCreate();
        initExoPlayer();
    }

    private void initExoPlayer() {
        com.google.android.exoplayer2.audio.AudioAttributes audioAttributes = new com.google.android.exoplayer2.audio.AudioAttributes.Builder()
                .setUsage(com.google.android.exoplayer2.C.USAGE_MEDIA)
                .setContentType(com.google.android.exoplayer2.C.AUDIO_CONTENT_TYPE_MUSIC)
                .build();

        exoPlayer = new ExoPlayer.Builder(this)
                .setWakeMode(com.google.android.exoplayer2.C.WAKE_MODE_LOCAL)
                .setAudioAttributes(audioAttributes, true)
                .build();
        
        exoPlayer.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int playbackState) {
                android.util.Log.d("MusicService", "ExoPlayer state changed: " + playbackState);
                
                if (playbackState == Player.STATE_READY) {
                    android.util.Log.d("MusicService", "ExoPlayer READY - Duration: " + exoPlayer.getDuration() + "ms");
                    isPrepared = true;
                    updateNotification();
                    if (listener != null) {
                        listener.onPlaybackStatusChanged(exoPlayer.isPlaying());
                    }
                } else if (playbackState == Player.STATE_ENDED) {
                    android.util.Log.d("MusicService", "ExoPlayer ENDED - playing next");
                    acquireTransitionWakeLock();
                    com.appad.utils.MusicPlayerManager.getInstance().playNext(false);
                } else if (playbackState == Player.STATE_BUFFERING) {
                    android.util.Log.d("MusicService", "ExoPlayer BUFFERING...");
                }
            }

            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                android.util.Log.d("MusicService", "ExoPlayer isPlaying changed: " + isPlaying);
                updateNotification();
                if (listener != null) {
                    listener.onPlaybackStatusChanged(isPlaying);
                }
            }

            @Override
            public void onPlayerError(PlaybackException error) {
                android.util.Log.e("MusicService", "ExoPlayer Error: " + error.getMessage(), error);
                isPrepared = false;
                
                String userMsg = "Lỗi phát nhạc: " + error.getMessage();
                android.widget.Toast.makeText(MusicService.this, userMsg, android.widget.Toast.LENGTH_SHORT).show();
                
                if (listener != null) {
                    listener.onPlaybackStatusChanged(false);
                }
            }
        });
    }

    public static final String ACTION_PAUSE = "action_pause";
    public static final String ACTION_PLAY = "action_play";
    public static final String ACTION_NEXT = "action_next";
    public static final String ACTION_PREV = "action_prev";

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getAction() != null) {
            String action = intent.getAction();
            switch (action) {
                case ACTION_PLAY:
                case ACTION_PAUSE:
                    togglePlayPause();
                    break;
                case ACTION_NEXT:
                    com.appad.utils.MusicPlayerManager.getInstance().playNext();
                    break;
                case ACTION_PREV:
                    com.appad.utils.MusicPlayerManager.getInstance().playPrevious();
                    break;
            }
        }
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public void setOnStatusChangeListener(OnStatusChangeListener listener) {
        this.listener = listener;
    }

    public void playSong(Song song) {
        playSong(song, true);
    }

    public void playSong(Song song, boolean autoPlay) {
        this.currentSong = song;
        // Notify UI immediately
        if (listener != null) {
            listener.onSongChanged(currentSong);
        }

        try {
            if (exoPlayer == null) {
                initExoPlayer();
            }
            
            isPrepared = false;

            // Get and validate URL
            String rawUrl = song.getFileUrl();
            android.util.Log.d("MusicService", "Raw fileUrl from song: " + rawUrl);
            
            String url = com.appad.utils.ImageUrlUtils.fixUrl(rawUrl);
            android.util.Log.d("MusicService", "Fixed URL: " + url);
            
            if (url == null || url.isEmpty()) {
                android.util.Log.e("MusicService", "ERROR: URL is null or empty! Cannot play song.");
                android.widget.Toast.makeText(this, "Không thể phát: URL bài hát trống", android.widget.Toast.LENGTH_SHORT).show();
                if (listener != null) {
                    listener.onPlaybackStatusChanged(false);
                }
                return;
            }

            android.util.Log.d("MusicService", "Creating MediaItem with URL: " + url);
            MediaItem mediaItem = MediaItem.fromUri(url);
            
            android.util.Log.d("MusicService", "Setting MediaItem and preparing... autoPlay=" + autoPlay);
            exoPlayer.setMediaItem(mediaItem);
            exoPlayer.prepare();
            exoPlayer.setPlayWhenReady(autoPlay);
            
            // Release transition lock after a short delay or once started
            if (transitionWakeLock != null && transitionWakeLock.isHeld()) {
                new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                    if (transitionWakeLock != null && transitionWakeLock.isHeld()) {
                        transitionWakeLock.release();
                    }
                }, 5000); 
            }
            
            android.util.Log.d("MusicService", "ExoPlayer updated with song: " + song.getTitle());
            
            // Acquire locks if streaming
            if (url.startsWith("http")) {
                acquireWifiLock();
            } else {
                releaseWifiLock();
            }
            
        } catch (Exception e) {
            android.util.Log.e("MusicService", "Error during playSong", e);
            android.widget.Toast.makeText(this, "Lỗi: " + e.getMessage(), android.widget.Toast.LENGTH_SHORT).show();
            isPrepared = false;
        }
    }

    public void stopMusic() {
        if (exoPlayer != null) {
            exoPlayer.stop();
            isPrepared = false;
        }
        releaseWifiLock();
        stopForeground(true);
        if (listener != null) {
            listener.onPlaybackStatusChanged(false);
        }
        // If we stop music from notification or manual action, we might keep the service for binding
    }

    public void togglePlayPause() {
        if (exoPlayer == null) return;

        if (exoPlayer.isPlaying()) {
            exoPlayer.pause();
        } else {
            exoPlayer.play();
        }
    }

    public void setNativeRepeatMode(int mode) {
        if (exoPlayer == null) return;
        switch (mode) {
            case 1: // Repeat All
                exoPlayer.setRepeatMode(Player.REPEAT_MODE_ALL);
                break;
            case 2: // Repeat One
                exoPlayer.setRepeatMode(Player.REPEAT_MODE_ONE);
                break;
            default:
                exoPlayer.setRepeatMode(Player.REPEAT_MODE_OFF);
                break;
        }
    }

    public boolean isPlaying() {
        return exoPlayer != null && exoPlayer.isPlaying();
    }

    public int getCurrentPosition() {
        if (exoPlayer != null) {
            return (int) exoPlayer.getCurrentPosition();
        }
        return 0;
    }

    public int getDuration() {
        if (exoPlayer != null && isPrepared) {
            return (int) exoPlayer.getDuration();
        }
        return 0;
    }

    public void seekTo(int pos) {
        if (exoPlayer != null) {
            exoPlayer.seekTo(pos);
        }
    }

    private PendingIntent getActionIntent(String action) {
        Intent intent = new Intent(this, MusicService.class);
        intent.setAction(action);
        return PendingIntent.getService(this, 0, intent, PendingIntent.FLAG_IMMUTABLE);
    }

    private void updateNotification() {
        if (currentSong == null) return;

        Intent notificationIntent = new Intent(this, FullPlayerActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE);

        boolean playing = exoPlayer != null && exoPlayer.isPlaying();
        
        Notification notification = new NotificationCompat.Builder(this, "music_channel")
                .setContentTitle(currentSong.getTitle())
                .setContentText(currentSong.getArtistName())
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setLargeIcon(BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher_background))
                .setContentIntent(pendingIntent)
                .setStyle(new androidx.media.app.NotificationCompat.MediaStyle()
                        .setShowActionsInCompactView(0, 1, 2))
                .addAction(android.R.drawable.ic_media_previous, "Previous", getActionIntent(ACTION_PREV))
                .addAction(playing ? android.R.drawable.ic_media_pause : android.R.drawable.ic_media_play, 
                        playing ? "Pause" : "Play", 
                        getActionIntent(playing ? ACTION_PAUSE : ACTION_PLAY))
                .addAction(android.R.drawable.ic_media_next, "Next", getActionIntent(ACTION_NEXT))
                .build();

        if (playing) {
            startForeground(1, notification);
        } else {
            // Khi pause, không bắt buộc chạy foreground để tiết kiệm tài nguyên và cho phép hệ thống giải phóng RAM
            // false có nghĩa là giữ lại notification nhưng cho phép service bị kill nếu thiếu RAM
            stopForeground(false);
            
            // Check permission for Android 13+ before notifying while not in foreground
            boolean canNotify = true;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                canNotify = androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) 
                        == android.content.pm.PackageManager.PERMISSION_GRANTED;
            }
            
            if (canNotify) {
                android.app.NotificationManager nm = (android.app.NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
                if (nm != null) nm.notify(1, notification);
            }
        }
    }

    @Override
    public void onTaskRemoved(Intent rootIntent) {
        // Called when user removes app from recent apps
        android.util.Log.d("MusicService", "App removed from recent apps - stopping everything");
        
        // 1. Tắt player ngay lập tức (không đợi ghi history vì có thể bị kill process)
        if (exoPlayer != null) {
            exoPlayer.stop();
            exoPlayer.release();
            exoPlayer = null;
        }
        
        // 2. Giải phóng locks
        releaseWifiLock();
        if (transitionWakeLock != null && transitionWakeLock.isHeld()) {
            transitionWakeLock.release();
        }
        
        // 3. Xóa notification và dừng service
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            stopForeground(STOP_FOREGROUND_REMOVE);
        } else {
            stopForeground(true);
        }
        stopSelf();
        
        super.onTaskRemoved(rootIntent);
        
        // 4. Force kill process để đảm bảo không còn thread nào chạy ngầm (tùy chọn nhưng hiệu quả)
        // android.os.Process.killProcess(android.os.Process.myPid());
    }

    @Override
    public void onDestroy() {
        com.appad.utils.MusicPlayerManager.getInstance().recordCurrentSongHistory();
        releaseWifiLock();
        if (transitionWakeLock != null && transitionWakeLock.isHeld()) {
            transitionWakeLock.release();
        }
        super.onDestroy();
        if (exoPlayer != null) {
            exoPlayer.release();
            exoPlayer = null;
        }
    }

    private void acquireTransitionWakeLock() {
        if (transitionWakeLock == null) {
            android.os.PowerManager pm = (android.os.PowerManager) getSystemService(Context.POWER_SERVICE);
            if (pm != null) {
                transitionWakeLock = pm.newWakeLock(android.os.PowerManager.PARTIAL_WAKE_LOCK, "appad:MusicTransitionLock");
            }
        }
        if (transitionWakeLock != null && !transitionWakeLock.isHeld()) {
            transitionWakeLock.acquire(10000); // Tự động release sau 10s nếu không code nào thả
        }
    }

    private void acquireWifiLock() {
        if (wifiLock == null) {
            android.net.wifi.WifiManager wm = (android.net.wifi.WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (wm != null) {
                wifiLock = wm.createWifiLock(android.net.wifi.WifiManager.WIFI_MODE_FULL_HIGH_PERF, "appad:MusicWifiLock");
            }
        }
        if (wifiLock != null && !wifiLock.isHeld()) {
            wifiLock.acquire();
        }
    }

    private void releaseWifiLock() {
        if (wifiLock != null && wifiLock.isHeld()) {
            wifiLock.release();
        }
    }
}
