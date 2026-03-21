package com.appad.utils;

import android.content.Context;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.FragmentActivity;
import com.appad.components.PremiumAccessModal;
import com.appad.models.Song;
import com.appad.models.User;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AccessHelper {

    public interface AccessCallback {
        void onAccessGranted();
        default void onAccessDenied(Song song) {}
    }

    /**
     * Checks if user has access to a song.
     * Logic: Release Date -> Artist Owner -> Free -> Premium -> Bought Song -> Bought Album -> Artist Member
     */
    public static void checkAccess(Context context, Song song, boolean showModal, AccessCallback callback) {
        android.util.Log.d("AccessHelper", "checkAccess called for song: " + (song != null ? song.getTitle() : "null"));
        
        if (song == null) {
            android.util.Log.e("AccessHelper", "Song is null, returning");
            return;
        }

        // 1. Check Release Date
        if (song.getAlbumReleaseDate() != null && !song.getAlbumReleaseDate().isEmpty()) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault());
                Date releaseDate = sdf.parse(song.getAlbumReleaseDate());
                if (releaseDate != null && releaseDate.after(new Date())) {
                    showReleaseDateWarning(context, song);
                    callback.onAccessDenied(song);
                    return;
                }
            } catch (Exception e) {
                // Ignore parse error or try another format if needed
            }
        }

        // 2. Get User and local checks
        SessionManager sm = SessionManager.getInstance(context);
        User user = (sm != null) ? sm.getUser() : null;

        // Artist Owner check - Artist can always play their own songs
        // Use isArtistOwner field which is pre-calculated by backend (compares artist.userId with current user)
        if (Boolean.TRUE.equals(song.getIsArtistOwner())) {
            android.util.Log.d("AccessHelper", "Access GRANTED: User is artist owner (isArtistOwner=true)");
            callback.onAccessGranted();
            return;
        }

        // 3. Free check
        boolean isPremium = (song.getIsPremium() != null && song.getIsPremium() == 1);
        boolean isAlbumPremium = (song.getIsAlbumPremium() != null && song.getIsAlbumPremium() == 1);
        android.util.Log.d("AccessHelper", "Premium check: isPremium=" + isPremium + ", isAlbumPremium=" + isAlbumPremium);
        
        if (!isPremium && !isAlbumPremium) {
            android.util.Log.d("AccessHelper", "Access GRANTED: Song is FREE");
            callback.onAccessGranted();
            return;
        }

        // 4. Local session checks
        // Global Premium
        if (user != null && user.getIsPremium() != null && user.getIsPremium() == 1) {
            android.util.Log.d("AccessHelper", "Access GRANTED: User has GLOBAL Premium");
            callback.onAccessGranted();
            return;
        }

        // Bought single song (cached in song object)
        if (Boolean.TRUE.equals(song.getBought())) {
            android.util.Log.d("AccessHelper", "Access GRANTED: Song marked as BOUGHT");
            callback.onAccessGranted();
            return;
        }
        
        android.util.Log.d("AccessHelper", "Starting REMOTE checks for premium song...");

        // 5. Remote checks - cascading
        RetrofitClient.getApiService().checkPurchase(song.getSongId(), "song").enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null && Boolean.TRUE.equals(response.body().get("purchased"))) {
                    callback.onAccessGranted();
                } else if (song.getAlbumId() != null) {
                    checkAlbumPurchase(context, user, song, showModal, callback);
                } else {
                    checkArtistMembership(context, user, song, showModal, callback);
                }
            }
            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                // Fallback to next check even if API fails, as it might be a transient error
                if (song.getAlbumId() != null) {
                    checkAlbumPurchase(context, user, song, showModal, callback);
                } else {
                    checkArtistMembership(context, user, song, showModal, callback);
                }
            }
        });
    }

    private static void checkAlbumPurchase(Context context, User user, Song song, boolean showModal, AccessCallback callback) {
        RetrofitClient.getApiService().checkPurchase(song.getAlbumId(), "album").enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null && Boolean.TRUE.equals(response.body().get("purchased"))) {
                    callback.onAccessGranted();
                } else {
                    checkArtistMembership(context, user, song, showModal, callback);
                }
            }
            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                checkArtistMembership(context, user, song, showModal, callback);
            }
        });
    }

    private static void checkArtistMembership(Context context, User user, Song song, boolean showModal, AccessCallback callback) {
        if (user == null || song.getArtistId() == null) {
            if (showModal) showPremiumModal(context, song, callback);
            else callback.onAccessDenied(song);
            return;
        }

        RetrofitClient.getApiService().checkArtistMembership(user.getUserId(), song.getArtistId()).enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                boolean hasAccess = false;
                if (response.isSuccessful() && response.body() != null) {
                    Object activeObj = response.body().get("active");
                    if (Boolean.TRUE.equals(activeObj)) {
                        hasAccess = true;
                    }
                }

                if (hasAccess) {
                    callback.onAccessGranted();
                } else {
                    if (showModal) showPremiumModal(context, song, callback);
                    else callback.onAccessDenied(song);
                }
            }

            @Override
            public void onFailure(Call<Map<String, Object>> call, Throwable t) {
                if (showModal) showPremiumModal(context, song, callback);
                else callback.onAccessDenied(song);
            }
        });
    }

    private static void showPremiumModal(Context context, Song song, AccessCallback callback) {
        if (context instanceof FragmentActivity) {
            PremiumAccessModal.newInstance(song, () -> {
                // When modal closes with success, grant access
                callback.onAccessGranted();
            }).show(((FragmentActivity) context).getSupportFragmentManager(), "PremiumAccessModal");
            
            // Still call onAccessDenied to notify the player system that access is NOT YET granted
            callback.onAccessDenied(song);
        } else {
            callback.onAccessDenied(song);
        }
    }

    public static void checkDownloadAccess(Context context, Song song, AccessCallback callback) {
        if (song == null) return;

        SessionManager sm = SessionManager.getInstance(context);
        User user = (sm != null) ? sm.getUser() : null;

        // 1. User is artist owner -> Always allow
        if (Boolean.TRUE.equals(song.getIsArtistOwner())) {
            callback.onAccessGranted();
            return;
        }

        // 2. Local purchase check
        if (Boolean.TRUE.equals(song.getBought()) || Boolean.TRUE.equals(song.getAlbumBought())) {
            callback.onAccessGranted();
            return;
        }

        // 3. Check if song is Free or Premium
        boolean isSongPremium = (song.getIsPremium() != null && song.getIsPremium() == 1) 
                            || (song.getIsAlbumPremium() != null && song.getIsAlbumPremium() == 1);

        if (!isSongPremium) {
            // Free music: Only for Premium users (not for normal users)
            if (user != null && user.getIsPremium() != null && user.getIsPremium() == 1) {
                callback.onAccessGranted();
            } else {
                callback.onAccessDenied(song);
            }
            return;
        }

        // 4. Premium Music: Must check remote purchase (Premium membership is not enough for download)
        RetrofitClient.getApiService().checkPurchase(song.getSongId(), "song").enqueue(new Callback<Map<String, Object>>() {
            @Override
            public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                if (response.isSuccessful() && response.body() != null && Boolean.TRUE.equals(response.body().get("purchased"))) {
                    callback.onAccessGranted();
                } else if (song.getAlbumId() != null) {
                    RetrofitClient.getApiService().checkPurchase(song.getAlbumId(), "album").enqueue(new Callback<Map<String, Object>>() {
                        @Override
                        public void onResponse(Call<Map<String, Object>> call, Response<Map<String, Object>> response) {
                            if (response.isSuccessful() && response.body() != null && Boolean.TRUE.equals(response.body().get("purchased"))) {
                                callback.onAccessGranted();
                            } else {
                                callback.onAccessDenied(song);
                            }
                        }
                        @Override public void onFailure(Call<Map<String, Object>> call, Throwable t) { callback.onAccessDenied(song); }
                    });
                } else {
                    callback.onAccessDenied(song);
                }
            }
            @Override public void onFailure(Call<Map<String, Object>> call, Throwable t) { callback.onAccessDenied(song); }
        });
    }

    private static void showReleaseDateWarning(Context context, Song song) {
        new AlertDialog.Builder(context)
                .setTitle("🎵 Sắp ra mắt")
                .setMessage("Bài hát \"" + song.getTitle() + "\" thuộc album chưa phát hành. Vui lòng quay lại sau!")
                .setPositiveButton("Đã hiểu", null)
                .show();
    }
}
