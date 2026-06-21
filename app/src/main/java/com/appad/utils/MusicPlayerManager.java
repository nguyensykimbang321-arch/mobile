package com.appad.utils;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import com.appad.models.Song;
import com.appad.models.User;
import com.appad.services.MusicService;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MusicPlayerManager {
    private static MusicPlayerManager instance;
    private MusicService musicService;
    private boolean isServiceBound = false;
    
    private Song currentSong;
    private List<Song> playlist = new ArrayList<>();
    private List<Song> originalPlaylist = new ArrayList<>(); // Để khôi phục khi tắt shuffle
    private int currentIndex = -1;
    private boolean isCurrentSongIncremented = false;
    private OnPlaylistEndListener playlistEndListener;

    public interface OnPlaylistEndListener {
        void onPlaylistEnd();
    }

    public void setOnPlaylistEndListener(OnPlaylistEndListener listener) {
        this.playlistEndListener = listener;
    }

    // Shuffle & Repeat
    private boolean isShuffle = false;
    private int repeatMode = 0; // 0: Off, 1: Repeat All, 2: Repeat One
    private boolean isMiniPlayerMinimized = false;
    private boolean stopAfterCurrent = false;

    // Sleep Timer
    private Handler sleepHandler = new Handler(Looper.getMainLooper());
    private Runnable sleepRunnable;

    private long sleepTimerTarget = 0; // Timestamp khi sẽ tắt

    // Inactivity Timer
    private Handler inactivityHandler = new Handler(Looper.getMainLooper());
    private Runnable inactivityRunnable;
    private long lastInteractionTime = 0;
    private static final long INACTIVITY_TIMEOUT = 30 * 60 * 1000L; // 30 minutes

    // Progress Polling
    private final Handler progressHandler = new Handler(Looper.getMainLooper());
    private final Runnable progressRunnable = new Runnable() {
        @Override
        public void run() {
            if (musicService != null && musicService.isPlaying()) {
                int pos = musicService.getCurrentPosition();
                int dur = musicService.getDuration();

                // Auto-next 1.5s before end
                if (dur > 5000 && pos >= (dur - 1500) && (currentSong != null && !currentSong.getSongId().equals(lastAutoNextSongId))) {
                    lastAutoNextSongId = currentSong.getSongId();
                    android.util.Log.d("MusicPlayerManager", "Song almost finished, checking flags...");
                    
                    if (stopAfterCurrent) {
                        android.util.Log.d("MusicPlayerManager", "Stop After Current is ON. Pausing...");
                        stopAfterCurrent = false; // Reset flag
                        if (musicService != null) musicService.togglePlayPause(); // Pause
                    } else {
                        android.util.Log.d("MusicPlayerManager", "Triggering auto-next");
                        playNext(false);
                    }
                }

                for (OnPlayerStatusChangeListener listener : new ArrayList<>(statusChangeListeners)) {
                    listener.onProgressUpdated(pos, dur);
                }
            } else {
                 // Check if paused due to inactivity is handled elsewhere, but generally we stop checking if not playing
            }
            progressHandler.postDelayed(this, 500);
        }
    };

    private final List<OnPlayerStatusChangeListener> statusChangeListeners = new ArrayList<>();

    public interface OnPlayerStatusChangeListener {
        void onSongChanged(Song song);
        void onPlaybackStatusChanged(boolean isPlaying);
        void onProgressUpdated(int position, int duration);
        default void onInactivityPause() {}
        default void onMiniPlayerStateChanged(boolean isMinimized) {}
    }

    private MusicPlayerManager() {}

    public static synchronized MusicPlayerManager getInstance() {
        if (instance == null) {
            instance = new MusicPlayerManager();
        }
        return instance;
    }

    private void startInactivityCheck() {
        if (inactivityRunnable == null) {
            inactivityRunnable = new Runnable() {
                @Override
                public void run() {
                    if (isPlaying()) {
                        long diff = System.currentTimeMillis() - lastInteractionTime;
                        if (diff > INACTIVITY_TIMEOUT) {
                             android.util.Log.d("MusicPlayerManager", "Inactivity timeout reached. Pausing...");
                             if (musicService != null) musicService.togglePlayPause(); // Pause
                             
                             // Notify listeners to show modal
                             for (OnPlayerStatusChangeListener listener : new ArrayList<>(statusChangeListeners)) {
                                 listener.onInactivityPause();
                             }
                        }
                    } else {
                        // If not playing, keep updating lastInteractionTime to prevent immediate trigger upon resume
                        // updateInteraction(); // Or just let it be
                    }
                    inactivityHandler.postDelayed(this, 5000); // Check every 5s
                }
            };
            inactivityHandler.postDelayed(inactivityRunnable, 5000);
        }
    }

    public void updateInteraction() {
        lastInteractionTime = System.currentTimeMillis();
    }

    public void init(Context context) {
        Intent intent = new Intent(context, MusicService.class);
        context.startService(intent); // Đảm bảo Service ở trạng thái Started
        context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE);
        updateInteraction();
        startInactivityCheck();
    }

    private final ServiceConnection serviceConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicService.MusicBinder binder = (MusicService.MusicBinder) service;
            musicService = binder.getService();
            isServiceBound = true;
            setupServiceListener();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isServiceBound = false;
        }
    };

    private void setupServiceListener() {
        if (musicService != null) {
            musicService.setOnStatusChangeListener(new MusicService.OnStatusChangeListener() {
                @Override
                public void onSongChanged(Song song) {
                    currentSong = song;
                    for (OnPlayerStatusChangeListener listener : new ArrayList<>(statusChangeListeners)) {
                        listener.onSongChanged(song);
                    }
                }

                @Override
                public void onPlaybackStatusChanged(boolean isPlaying) {
                    if (isPlaying) {
                        playStartTime = System.currentTimeMillis();
                        
                        // Increment view count if we haven't done it for this song session
                        if (!isCurrentSongIncremented && currentSong != null) {
                            recordListen(currentSong.getSongId(), currentSong.getArtistId(), 0, false, true);
                            isCurrentSongIncremented = true;
                        }
                    } else {
                        if (playStartTime > 0) {
                            accumulatedTime += (System.currentTimeMillis() - playStartTime);
                            playStartTime = 0;
                        }
                    }
                    for (OnPlayerStatusChangeListener listener : new ArrayList<>(statusChangeListeners)) {
                        listener.onPlaybackStatusChanged(isPlaying);
                    }
                }
            });
        }
    }

    public void setPlaylist(List<Song> songs, int startIndex) {
        setPlaylist(songs, startIndex, true);
    }

    public void setPlaylist(List<Song> songs, int startIndex, boolean autoPlay) {
        this.originalPlaylist = new ArrayList<>(songs);
        this.playlist = new ArrayList<>(songs);
        this.currentIndex = startIndex;
        
        if (isShuffle) {
            shufflePlaylist();
        }
        
        playCurrentIndex(autoPlay);
    }

    /**
     * Cập nhật danh sách nhạc hiện tại mà không làm gián đoạn bài đang phát.
     * Thường dùng khi drag-and-drop hoặc load thêm nhạc (pagination).
     */
    public void updatePlaylist(List<Song> newSongs) {
        Song current = currentSong;
        int oldSize = playlist.size();
        this.originalPlaylist = new ArrayList<>(newSongs);
        this.playlist = new ArrayList<>(newSongs);
        
        if (isShuffle) {
            // Nếu đang shuffle, giữ bài hiện tại ở vị trí 0 và shuffle phần còn lại
            shufflePlaylist();
        } else {
            if (current != null) {
                // Cập nhật lại currentIndex dựa trên danh sách mới
                boolean found = false;
                for (int i = 0; i < playlist.size(); i++) {
                    if (playlist.get(i).getSongId().equals(current.getSongId())) {
                        currentIndex = i;
                        found = true;
                        break;
                    }
                }
                
                // Nếu không tìm thấy bài hát hiện tại trong danh sách mới (bị xóa)
                if (!found && !playlist.isEmpty()) {
                    if (currentIndex >= playlist.size()) {
                        currentIndex = playlist.size() - 1;
                    } else if (currentIndex < 0) {
                        currentIndex = 0;
                    }
                } else if (playlist.isEmpty()) {
                    currentIndex = -1;
                }
            }
        }
        
        // Notify listeners that playlist changed (if needed, currently we just inform via onSongChanged if it changed)
        
        // Nếu trước đó đang ở bài cuối cùng và vừa được thêm bài mới, hãy thử chuyển sang bài tiếp theo
        // Điều này giúp việc auto-next mượt mà hơn khi load thêm dữ liệu (pagination) thành công
        if (playlist.size() > oldSize && currentIndex == oldSize - 1) {
            // Nếu bài hiện tại đã phát xong hoặc gần xong, tự động sang bài mới
            if (!isPlaying()) {
                 android.util.Log.d("MusicPlayerManager", "Playlist expanded and was at end, playing next automatically");
                 playNext(false);
            }
        }
    }

    private Integer lastAutoNextSongId = -1;
    private long playStartTime = 0;
    private long accumulatedTime = 0; // ms

    public void playSong(Song song) {
        playSong(song, true);
    }

    public void playSong(Song song, boolean autoPlay) {
        // 1. Record history for the PREVIOUS song before switching
        recordCurrentSongHistory();

        // 2. Check Login status
        SessionManager sm = SessionManager.getInstance(com.appad.MusicApplication.getInstance());
        if (sm == null || !sm.isLoggedIn()) {
            android.util.Log.w("MusicPlayerManager", "User not logged in, preventing playback.");
            stopMusic();
            return;
        }

        // 3. Load the new song
        if (musicService != null) {
            // Đảm bảo Service luôn ở trạng thái Started (để onTaskRemoved luôn được gọi)
            // Ngay cả khi hệ thống tự rebind service, nó cũng chỉ ở trạng thái Bound.
            Context context = com.appad.MusicApplication.getInstance();
            Intent intent = new Intent(context, MusicService.class);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                // Trên một số máy, cần dùng startForegroundService nếu muốn phát nhạc ngay
                // Nhưng ở đây ta cứ dùng startService thông thường vì binding đã giữ nó sống.
                context.startService(intent);
            } else {
                context.startService(intent);
            }

            this.currentSong = song; // Đảm bảo manager biết song mới ngay cả khi autoPlay=false
            accumulatedTime = 0; // Reset cho bài mới
            lastAutoNextSongId = -1; // Reset guard
            isCurrentSongIncremented = false; // Reset increment flag
            
            musicService.playSong(song, autoPlay);
            
            if (autoPlay) {
                playStartTime = System.currentTimeMillis();
                updateInteraction();
                
                // 3. Record START of new song (increment count immediately)
                recordListen(song.getSongId(), song.getArtistId(), 0, false, true);
                isCurrentSongIncremented = true;
            } else {
                playStartTime = 0;
            }
        }
    }

    private void recordListen(Integer songId, Integer artistId, Integer durationSec, Boolean isCompleted, boolean incrementCount) {
        SessionManager sm = SessionManager.getInstance(com.appad.MusicApplication.getInstance());
        Integer userId = (sm != null) ? sm.getUserId() : null;
        if (userId == null) return; // Không record nếu chưa login

        // Gọi API playSong để backend vừa tăng view count vừa record history
        java.util.Map<String, Object> playData = new java.util.HashMap<>();
        playData.put("duration_seconds", durationSec);
        playData.put("is_completed", isCompleted);
        playData.put("increment_count", incrementCount);
        
        android.util.Log.d("MusicPlayerManager", "Recording listen: songId=" + songId + ", duration=" + durationSec + "s, count++=" + incrementCount);
        
        RetrofitClient.getApiService().playSong(songId, playData).enqueue(new retrofit2.Callback<java.util.Map<String, Object>>() {
            @Override public void onResponse(retrofit2.Call<java.util.Map<String, Object>> call, retrofit2.Response<java.util.Map<String, Object>> response) {
                if (response.isSuccessful()) {
                    android.util.Log.d("MusicPlayerManager", "Listen recorded successfully");
                }
            }
            @Override public void onFailure(retrofit2.Call<java.util.Map<String, Object>> call, Throwable t) {
                android.util.Log.e("MusicPlayerManager", "Failed to record listen", t);
            }
        });
    }

    private void playCurrentIndex() {
        playCurrentIndex(true);
    }

    private void playCurrentIndex(boolean autoPlay) {
        if (currentIndex >= 0 && currentIndex < playlist.size()) {
            playSong(playlist.get(currentIndex), autoPlay);
        }
    }

    public void stopMusic() {
        // Record history cho bài hiện tại trước khi stop
        recordCurrentSongHistory();
        
        if (musicService != null) {
            musicService.stopMusic();
            currentSong = null;
            playlist.clear();
            originalPlaylist.clear();
            currentIndex = -1;
            playStartTime = 0;
            accumulatedTime = 0;
            for (OnPlayerStatusChangeListener listener : new ArrayList<>(statusChangeListeners)) {
                listener.onSongChanged(null);
                listener.onPlaybackStatusChanged(false);
            }
        }
    }
    
    /**
     * Record lịch sử nghe cho bài hát hiện tại
     * Được gọi khi: chuyển bài, tắt miniplayer, đăng xuất
     */
    public void recordCurrentSongHistory() {
        if (currentSong != null && currentSong.getSongId() != null) {
            long currentSessionTime = (playStartTime > 0) ? (System.currentTimeMillis() - playStartTime) : 0;
            long totalDurationListened = accumulatedTime + currentSessionTime;
            
            int durationSec = (int) (totalDurationListened / 1000);
            int totalDur = getDuration() / 1000;
            boolean isCompleted = totalDur > 0 && (durationSec >= totalDur * 0.9);
            
            if (durationSec > 0) { 
                recordListen(currentSong.getSongId(), currentSong.getArtistId(), durationSec, isCompleted, false);
            }
        }
    }

    public void pause() {
        if (musicService != null && musicService.isPlaying()) {
            updateInteraction();
            musicService.togglePlayPause();
        }
    }

    public void togglePlayPause() {
        if (musicService != null) {
            updateInteraction();
            musicService.togglePlayPause();
        }
    }

    private void syncRepeatMode() {
        if (musicService != null) {
            musicService.setNativeRepeatMode(repeatMode);
        }
    }


    // ========== SHUFFLE LOGIC ==========
    public void toggleShuffle() {
        isShuffle = !isShuffle;
        if (isShuffle) {
            updateInteraction();
            shufflePlaylist();
        } else {
            // Khôi phục playlist gốc
            Song current = currentSong;
            playlist = new ArrayList<>(originalPlaylist);
            currentIndex = playlist.indexOf(current);
            if (currentIndex < 0) currentIndex = 0;
        }
    }

    private void shufflePlaylist() {
        Song current = currentSong;
        List<Song> toShuffle = new ArrayList<>(playlist);
        if (current != null) toShuffle.remove(current);
        Collections.shuffle(toShuffle);
        playlist.clear();
        if (current != null) playlist.add(current);
        playlist.addAll(toShuffle);
        currentIndex = 0;
    }

    public boolean isShuffle() {
        return isShuffle;
    }

    // ========== REPEAT LOGIC ==========
    public void toggleRepeat() {
        updateInteraction();
        repeatMode = (repeatMode + 1) % 3; // 0: Off, 1: Repeat All, 2: Repeat One
        syncRepeatMode();
    }

    public int getRepeatMode() {
        return repeatMode;
    }

    public boolean hasAccess(Song song) {
        if (song == null) {
            android.util.Log.w("MusicPlayerManager", "hasAccess: song is null");
            return false;
        }
        
        // 1. Free check
        boolean isPremium = (song.getIsPremium() != null && song.getIsPremium() == 1);
        boolean isAlbumPremium = (song.getIsAlbumPremium() != null && song.getIsAlbumPremium() == 1);
        if (!isPremium && !isAlbumPremium) {
            android.util.Log.d("MusicPlayerManager", "hasAccess: song '" + song.getTitle() + "' is FREE (granted)");
            return true;
        }

        // 2. User info
        SessionManager sm = SessionManager.getInstance(com.appad.MusicApplication.getInstance());
        User user = (sm != null) ? sm.getUser() : null;
        if (user == null) {
            android.util.Log.w("MusicPlayerManager", "hasAccess: user session is null, denying premium song '" + song.getTitle() + "'");
            return false;
        }

        android.util.Log.d("MusicPlayerManager", "hasAccess check for premium song '" + song.getTitle() + "' - User: " + user.getUsername() + ", userPremium: " + user.getIsPremium());

        // 3. Artist Owner (pre-calculated by backend)
        if (Boolean.TRUE.equals(song.getIsArtistOwner())) {
            android.util.Log.d("MusicPlayerManager", "hasAccess: user is artist owner of '" + song.getTitle() + "' (granted)");
            return true;
        }

        // 4. Global Premium
        if (user.getIsPremium() != null && user.getIsPremium() == 1) {
            android.util.Log.d("MusicPlayerManager", "hasAccess: user has Global Premium (granted)");
            return true;
        }

        // 5. Purchases & Membership (using flags from Song object)
        if (Boolean.TRUE.equals(song.getBought())) {
            android.util.Log.d("MusicPlayerManager", "hasAccess: song '" + song.getTitle() + "' was bought (granted)");
            return true;
        }
        if (Boolean.TRUE.equals(song.getAlbumBought())) {
            android.util.Log.d("MusicPlayerManager", "hasAccess: album was bought (granted)");
            return true;
        }
        if (Boolean.TRUE.equals(song.getArtistMember())) {
            android.util.Log.d("MusicPlayerManager", "hasAccess: user has artist membership (granted)");
            return true;
        }

        android.util.Log.w("MusicPlayerManager", "hasAccess: user doesn't have premium/ownership for '" + song.getTitle() + "' (denied)");
        return false;
    }

    public void playNext(boolean isManual) {
        if (isManual) updateInteraction();
        if (playlist.size() == 0) return;

        // In "Repeat One" mode, manual "Next" should go to the next song,
        // while automatic "Next" (from service) should replay current.
        if (!isManual && repeatMode == 2) {
            playCurrentIndex();
            return;
        }

        int startIdx = currentIndex;
        int nextIdx = currentIndex;
        
        do {
            nextIdx++;
            if (nextIdx >= playlist.size()) {
                if (isManual || repeatMode == 1) { // Manual click or Repeat All wraps around
                    nextIdx = 0;
                } else {
                    // Tự động load thêm nếu có listener và đang ở cuối danh sách
                    if (playlistEndListener != null) {
                        android.util.Log.d("MusicPlayerManager", "End of playlist reached, triggering listener");
                        playlistEndListener.onPlaylistEnd();
                    }
                    
                    nextIdx = playlist.size() - 1;
                    if (nextIdx == startIdx) return; 
                    break; 
                }
            }
            
            if (hasAccess(playlist.get(nextIdx))) {
                currentIndex = nextIdx;
                playCurrentIndex();
                return;
            }
            
        } while (nextIdx != startIdx);

        if (nextIdx == startIdx && !hasAccess(playlist.get(currentIndex))) {
            stopMusic();
        }
    }

    public void playPrevious(boolean isManual) {
        if (isManual) updateInteraction();
        if (playlist.size() == 0) return;

        // Manual "Previous" always goes back even in "Repeat One"
        int startIdx = currentIndex;
        int prevIdx = currentIndex;

        do {
            prevIdx--;
            if (prevIdx < 0) {
                if (repeatMode == 1 || repeatMode == 2 || isManual) {
                    prevIdx = playlist.size() - 1;
                } else {
                    prevIdx = 0;
                    if (prevIdx == startIdx) return;
                    break;
                }
            }

            if (hasAccess(playlist.get(prevIdx))) {
                currentIndex = prevIdx;
                playCurrentIndex();
                return;
            }

        } while (prevIdx != startIdx);
    }

    // Keep old signatures for backward compatibility if needed, but update them
    public void playNext() { playNext(true); }
    public void playPrevious() { playPrevious(true); }

    // ========== SLEEP TIMER ==========
    public void startSleepTimer(int minutes) {
        cancelSleepTimer();
        
        sleepTimerTarget = System.currentTimeMillis() + (minutes * 60 * 1000L);
        
        sleepRunnable = () -> {
            if (musicService != null && musicService.isPlaying()) {
                musicService.togglePlayPause(); // Pause nhạc
            }
            sleepTimerTarget = 0;
        };
        
        sleepHandler.postDelayed(sleepRunnable, minutes * 60 * 1000L);
    }

    public void cancelSleepTimer() {
        if (sleepRunnable != null) {
            sleepHandler.removeCallbacks(sleepRunnable);
            sleepRunnable = null;
        }
        sleepTimerTarget = 0;
    }

    public long getSleepTimerTarget() {
        return sleepTimerTarget;
    }

    public int getSleepTimerRemaining() {
        if (sleepTimerTarget == 0) return 0;
        long remaining = sleepTimerTarget - System.currentTimeMillis();
        return remaining > 0 ? (int) (remaining / 60000) : 0; // Trả về phút
    }

    public void setStopAfterCurrent(boolean stop) {
        this.stopAfterCurrent = stop;
        if (stop) {
            cancelSleepTimer(); // Hẹn giờ tắt ghi đè sleep timer minutes
        }
    }

    public boolean isStopAfterCurrent() {
        return stopAfterCurrent;
    }

    // ========== SEEK & STATUS ==========
    public void seekTo(int position) {
        updateInteraction();
        if (musicService != null) musicService.seekTo(position);
    }

    public boolean isPlaying() {
        return musicService != null && musicService.isPlaying();
    }

    public Song getCurrentSong() {
        return currentSong;
    }

    public int getCurrentPosition() {
        return musicService != null ? musicService.getCurrentPosition() : 0;
    }

    public int getDuration() {
        return musicService != null ? musicService.getDuration() : 0;
    }

    public void addStatusChangeListener(OnPlayerStatusChangeListener listener) {
        if (!statusChangeListeners.contains(listener)) {
            statusChangeListeners.add(listener);
            if (currentSong != null) {
                listener.onSongChanged(currentSong);
                listener.onPlaybackStatusChanged(isPlaying());
            }
            if (statusChangeListeners.size() == 1) {
                progressHandler.removeCallbacks(progressRunnable);
                progressHandler.post(progressRunnable);
            }
        }
    }

    public List<Song> getPlaylist() {
        return playlist;
    }

    public void removeStatusChangeListener(OnPlayerStatusChangeListener listener) {
        statusChangeListeners.remove(listener);
        if (statusChangeListeners.isEmpty()) {
            progressHandler.removeCallbacks(progressRunnable);
        }
    }

    public boolean isMiniPlayerMinimized() {
        return isMiniPlayerMinimized;
    }

    public void setMiniPlayerMinimized(boolean minimized) {
        if (this.isMiniPlayerMinimized != minimized) {
            this.isMiniPlayerMinimized = minimized;
            updateInteraction();
            for (OnPlayerStatusChangeListener listener : new ArrayList<>(statusChangeListeners)) {
                listener.onMiniPlayerStateChanged(minimized);
            }
        }
    }
}
