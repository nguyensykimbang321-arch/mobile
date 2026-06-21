package com.appad.services;

import com.appad.models.User;
import java.util.Map;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.http.*;

public interface ApiService {
    @POST("api/auth/register")
    Call<Map<String, Object>> register(@Body Map<String, Object> registerRequest);

    @POST("api/auth/login")
    Call<Map<String, Object>> login(@Body Map<String, String> loginRequest);

    @GET("api/songs")
    Call<Map<String, Object>> getAllSongs(@Query("limit") int limit, @Query("offset") int offset);

    @GET("api/songs/latest")
    Call<Map<String, Object>> getLatestSongs(@Query("limit") int limit, @Query("offset") int offset);

    @GET("api/songs/search")
    Call<Map<String, Object>> searchSongs(
            @Query("q") String query,
            @Query("limit") int limit,
            @Query("offset") int offset,
            @Query("sort") String sort);

    @GET("api/search")
    Call<Map<String, Object>> searchAll(@Query("query") String query);

    @GET("api/songs/{id}")
    Call<Map<String, Object>> getSongById(@Path("id") Integer id);

    @DELETE("api/songs/{id}")
    Call<Map<String, Object>> deleteSong(@Path("id") Integer id);

    @GET("api/artists/{id}")
    Call<Map<String, Object>> getArtistById(@Path("id") Integer id);

    @GET("api/artists/user/{userId}")
    Call<Map<String, Object>> getArtistByUserId(@Path("userId") Integer userId);

    @GET("api/songs/artist/{artistId}")
    Call<Map<String, Object>> getArtistSongs(@Path("artistId") Integer artistId);

    @GET("api/albums/artist/{artistId}")
    Call<Map<String, Object>> getArtistAlbums(@Path("artistId") Integer artistId);

    @GET("api/artists/{artistId}/dashboard")
    Call<Map<String, Object>> getArtistDashboard(@Path("artistId") Integer artistId);

    @GET("api/albums")
    Call<Map<String, Object>> getAllAlbums(@Query("limit") int limit, @Query("offset") int offset);

    @GET("api/albums/{id}")
    Call<Map<String, Object>> getAlbumById(@Path("id") Integer id);

    @GET("api/songs/album/{albumId}")
    Call<Map<String, Object>> getSongsByAlbum(@Path("albumId") Integer albumId);

    // Favorites
    @GET("api/favorites/user/{userId}")
    Call<Map<String, Object>> getFavorites(@Path("userId") Integer userId);

    @POST("api/favorites/toggle")
    Call<Map<String, Object>> toggleFavorite(@Body Map<String, Integer> payload);

    @GET("api/favorites/check")
    Call<Map<String, Object>> checkFavorite(@Query("userId") Integer userId, @Query("songId") Integer songId);

    // Playlists
    @GET("api/playlists/my-playlists")
    Call<Map<String, Object>> getUserPlaylists();

    @POST("api/playlists")
    Call<Map<String, Object>> createPlaylist(@Body Map<String, Object> payload);

    @GET("api/playlists/{id}")
    Call<Map<String, Object>> getPlaylistById(@Path("id") Integer playlistId);

    @GET("api/playlists/{id}/songs")
    Call<Map<String, Object>> getPlaylistSongs(@Path("id") Integer playlistId);

    @POST("api/playlists/{playlistId}/songs")
    Call<Map<String, Object>> addSongToPlaylist(@Path("playlistId") Integer playlistId, @Body Map<String, Long> payload);

    @PUT("api/playlists/{playlistId}/reorder")
    Call<Map<String, Object>> reorderPlaylistSongs(@Path("playlistId") Integer playlistId, @Body Map<String, Object> payload);

    @DELETE("api/playlists/{playlistId}/songs/{songId}")
    Call<Map<String, Object>> removeSongFromPlaylist(@Path("playlistId") Integer playlistId, @Path("songId") Integer songId);

    @Multipart
    @PUT("api/playlists/{playlistId}/cover")
    Call<Map<String, Object>> updatePlaylistCover(@Path("playlistId") Integer playlistId, @Part okhttp3.MultipartBody.Part cover);

    @GET("api/playlists/{id}/share-code")
    Call<Map<String, Object>> getShareCode(@Path("id") Integer playlistId);

    @GET("api/playlists/share/{code}")
    Call<Map<String, Object>> getPlaylistByShareCode(@Path("code") String code);

    @POST("api/playlists/import")
    Call<Map<String, Object>> importPlaylist(@Body Map<String, String> payload);


    // Wallet & Premium
    @POST("api/wallet/topup")
    Call<Map<String, Object>> deposit(@Body Map<String, Object> payload);

    @GET("api/wallet/balance")
    Call<Map<String, Object>> getBalance();

    @POST("api/wallet/confirm")
    Call<Map<String, Object>> confirmDeposit(@Body Map<String, Object> payload);

    @POST("api/wallet/purchase/song")
    Call<Map<String, Object>> purchaseSong(@Body Map<String, Integer> payload);

    @POST("api/wallet/purchase/album")
    Call<Map<String, Object>> purchaseAlbum(@Body Map<String, Integer> payload);

    @GET("api/songs/trending")
    Call<Map<String, Object>> getTrendingSongs(@Query("limit") int limit, @Query("offset") int offset);

    @GET("api/songs/recommended")
    Call<Map<String, Object>> getRecommendedSongs(@Query("limit") int limit, @Query("offset") int offset);

    @GET("api/wallet/purchased-songs")
    Call<Map<String, Object>> getPurchasedSongs();

    @GET("api/wallet/purchased-albums")
    Call<Map<String, Object>> getPurchasedAlbums();

    @GET("api/wallet/check-purchase")
    Call<Map<String, Object>> checkPurchase(@Query("targetId") Integer targetId, @Query("type") String type);

    // Artists
    @GET("api/artists")
    Call<Map<String, Object>> getAllArtists();

    @GET("api/follows/my-artists")
    Call<Map<String, Object>> getFollowedArtists(@Query("userId") Integer userId);

    @POST("api/follows/toggle")
    Call<Map<String, Object>> toggleFollow(@Body Map<String, Integer> payload);

    @GET("api/follows/check")
    Call<Map<String, Object>> checkFollow(@Query("userId") Integer userId, @Query("artistId") Integer artistId);

    // Comments
    @GET("api/comments/{type}/{targetId}")
    Call<java.util.Map<String, Object>> getComments(@Path("type") String type, @Path("targetId") Integer targetId);

    @GET("api/comments/artist/{artistId}")
    Call<Map<String, Object>> getArtistReviews(@Path("artistId") Integer artistId);

    @GET("api/comments/admin/all")
    Call<Map<String, Object>> getAllReviews();

    @DELETE("api/comments/{commentId}")
    Call<Map<String, Object>> deleteComment(@Path("commentId") Long commentId);

    @POST("api/comments")
    Call<Map<String, Object>> addComment(@Body Map<String, Object> payload);

    @POST("api/history/record")
    Call<Map<String, Object>> recordListen(@Body Map<String, Object> payload);

    @POST("api/songs/{id}/play")
    Call<Map<String, Object>> playSong(@Path("id") Integer id, @Body Map<String, Object> payload);

    @GET("api/history/user/{userId}")
    Call<Map<String, Object>> getUserHistory(@Path("userId") Integer userId);

    @GET("api/history/by-day")
    Call<Map<String, Object>> getHistoryByDay(@Query("userId") Integer userId);

    @GET("api/notifications")
    Call<Map<String, Object>> getNotifications();

    @POST("api/notifications/mark-read/{id}")
    Call<Map<String, Object>> markAsRead(@Path("id") Long id);

    @POST("api/notifications/mark-all-read")
    Call<Map<String, Object>> markAllRead();

    @DELETE("api/notifications/{id}")
    Call<Map<String, Object>> deleteNotification(@Path("id") Long id);

    @DELETE("api/notifications/delete-all")
    Call<Map<String, Object>> deleteAllNotifications();

    @POST("api/notifications/broadcast")
    Call<Map<String, Object>> broadcastNotification(@Body Map<String, String> payload);

    @POST("api/wallet/premium/subscribe")
    Call<Map<String, Object>> subscribePremium(@Body Map<String, Object> payload);

    @GET("api/wallet/premium/cancel-preview")
    Call<Map<String, Object>> cancelPremiumPreview();

    @POST("api/wallet/premium/cancel")
    Call<Map<String, Object>> cancelPremium();

    // User Profile
    @GET("api/users/profile")
    Call<Map<String, Object>> getProfile();

    @PUT("api/users/profile")
    Call<Map<String, Object>> updateProfile(@Body Map<String, Object> payload);

    @Multipart
    @POST("api/users/upload-avatar")
    Call<Map<String, Object>> uploadAvatar(@Part okhttp3.MultipartBody.Part avatar);

    @GET("api/users/profile/{userId}/stats")
    Call<Map<String, Object>> getUserStats(@Path("userId") Integer userId);

    @POST("api/auth/change-password")
    Call<Map<String, Object>> changePassword(@Body Map<String, Object> payload);

    @GET("api/wallet/history")
    Call<Map<String, Object>> getTransactionHistory();

    @POST("api/reports/submit")
    Call<Map<String, Object>> submitReport(@Body Map<String, Object> payload);

    @POST("api/memberships/subscribe")
    Call<Map<String, Object>> subscribeArtist(@Body Map<String, Object> payload);

    @GET("api/memberships/check")
    Call<Map<String, Object>> checkArtistMembership(@Query("userId") Integer userId, @Query("artistId") Integer artistId);

    @GET("api/memberships/user/{userId}")
    Call<Map<String, Object>> getUserMemberships(@Path("userId") Integer userId);

    // Admin
    @GET("api/admin/stats")
    Call<Map<String, Object>> getAdminStats();

    @GET("api/admin/users")
    Call<java.util.List<Map<String, Object>>> getAdminUsers();

    @PUT("api/admin/users/{id}/ban")
    Call<Map<String, Object>> banUser(@Path("id") Integer id);

    @PUT("api/admin/users/{id}/unban")
    Call<Map<String, Object>> unbanUser(@Path("id") Integer id);

    @PUT("api/admin/users/{id}/role")
    Call<Map<String, Object>> updateUserRole(@Path("id") Integer id, @Body Map<String, Object> body);

    @GET("api/admin/withdrawals")
    Call<Map<String, Object>> getAllWithdrawals(@Query("status") String status, @Query("limit") Integer limit);

    @POST("api/admin/withdrawals/{id}/approve")
    Call<Map<String, Object>> approveWithdrawal(@Path("id") Long id, @Body Map<String, Object> payload);

    @POST("api/admin/withdrawals/{id}/reject")
    Call<Map<String, Object>> rejectWithdrawal(@Path("id") Long id, @Body Map<String, Object> payload);

    @GET("api/admin/transactions")
    Call<Map<String, Object>> getAllTransactions(@Query("type") String type, @Query("status") String status, @Query("limit") Integer limit);

    @GET("api/admin/transactions/pending-deposits/count")
    Call<Map<String, Object>> getPendingDepositsCount();

    @POST("api/admin/transactions/{id}/approve")
    Call<Map<String, Object>> approveDeposit(@Path("id") Integer id);

    @POST("api/admin/transactions/{id}/reject")
    Call<Map<String, Object>> rejectDeposit(@Path("id") Integer id, @Body Map<String, Object> payload);
    @Multipart
    @POST("api/studio/upload")
    Call<Map<String, Object>> uploadSong(
            @Part("title") RequestBody title,
            @Part("artistId") RequestBody artistId,
            @Part("genreId") RequestBody genreId,
            @Part("albumId") RequestBody albumId, 
            @Part("price") RequestBody price,
            @Part("isPremium") RequestBody isPremium,
            @Part("lyrics") RequestBody lyrics,
            @Part("status") RequestBody status,
            @Part("releaseDate") RequestBody releaseDate,
            @Part okhttp3.MultipartBody.Part musicFile,
            @Part okhttp3.MultipartBody.Part coverFile
    );

    @GET("api/genres")
    Call<Map<String, Object>> getGenres();

    @Multipart
    @PUT("api/studio/{songId}")
    Call<Map<String, Object>> updateSong(
            @Path("songId") Integer songId,
            @Part("title") RequestBody title,
            @Part("price") RequestBody price,
            @Part("isPremium") RequestBody isPremium,
            @Part("lyrics") RequestBody lyrics,
            @Part("genreId") RequestBody genreId,
            @Part("albumId") RequestBody albumId,
            @Part("status") RequestBody status,
            @Part("releaseDate") RequestBody releaseDate,
            @Part okhttp3.MultipartBody.Part coverFile,
            @Part okhttp3.MultipartBody.Part musicFile
    );

    @Multipart
    @POST("api/admin/upload/song")
    Call<Map<String, Object>> uploadAdminSongFile(@Part okhttp3.MultipartBody.Part file);

    @Multipart
    @POST("api/admin/upload/cover")
    Call<Map<String, Object>> uploadAdminCoverFile(@Part okhttp3.MultipartBody.Part file);

    @POST("api/admin/songs/create")
    Call<Map<String, Object>> createSongAdmin(@Body Map<String, Object> body);

    @PUT("api/admin/songs/{id}")
    Call<Map<String, Object>> updateSongAdmin(@Path("id") Integer id, @Body Map<String, Object> body);

    @GET("api/admin/artists/all")
    Call<Map<String, Object>> getAllArtistsAdmin();

    @GET("api/admin/albums/all")
    Call<Map<String, Object>> getAllAlbumsSimpleAdmin();

    @POST("api/admin/albums/create")
    Call<Map<String, Object>> createAlbumAdmin(@Body Map<String, Object> body);

    @PUT("api/admin/albums/{id}")
    Call<Map<String, Object>> updateAlbumAdmin(@Path("id") Integer id, @Body Map<String, Object> body);

    @GET("api/admin/songs/album/{id}")
    Call<Map<String, Object>> getSongsByAlbumAdmin(@Path("id") Integer albumId);

    @Multipart
    @POST("api/studio/album")
    Call<Map<String, Object>> createAlbum(
            @Part("title") RequestBody title,
            @Part("artistId") RequestBody artistId,
            @Part("price") RequestBody price,
            @Part("isPremium") RequestBody isPremium,
            @Part("releaseDate") RequestBody releaseDate,
            @Part okhttp3.MultipartBody.Part coverFile
    );

    @Multipart
    @PUT("api/studio/album/{albumId}")
    Call<Map<String, Object>> updateAlbum(
            @Path("albumId") Integer albumId,
            @Part("title") String title,
            @Part("price") Double price,
            @Part("isPremium") Integer isPremium,
            @Part("releaseDate") String releaseDate,
            @Part okhttp3.MultipartBody.Part coverFile
    );

    @DELETE("api/studio/album/{albumId}")
    Call<Map<String, Object>> deleteAlbum(@Path("albumId") Integer albumId);

    @PUT("api/artists/{id}/membership")
    Call<Map<String, Object>> updateArtistMembershipSettings(@Path("id") Integer artistId, @Body Map<String, Object> payload);

    // New Endpoints
    @POST("api/users/apply-artist")
    Call<Map<String, Object>> applyArtist();

    @GET("api/admin/artists/pending")
    Call<java.util.List<User>> getPendingArtists();

    @POST("api/admin/artists/{id}/approve")
    Call<Map<String, Object>> approveArtist(@Path("id") Integer userId);

    @POST("api/admin/artists/{id}/reject")
    Call<Map<String, Object>> rejectArtist(@Path("id") Integer userId);

    @POST("api/admin/broadcast")
    Call<Map<String, Object>> broadcast(@Body Map<String, String> payload);

    @GET("api/admin/songs")
    Call<java.util.List<com.appad.models.Song>> getAllSongsAdmin(@Query("limit") Integer limit, @Query("search") String search);

    @DELETE("api/admin/songs/{id}")
    Call<Map<String, Object>> deleteSongAdmin(@Path("id") Integer songId);

    @GET("api/admin/albums")
    Call<java.util.List<Map<String, Object>>> getAllAlbumsAdmin(@Query("limit") Integer limit);

    @DELETE("api/admin/albums/{id}")
    Call<Map<String, Object>> deleteAlbumAdmin(@Path("id") Integer albumId);

    @GET("api/admin/memberships")
    Call<Map<String, Object>> getAllMemberships(@Query("limit") Integer limit, @Query("status") String status, @Query("search") String search);

    @GET("api/admin/memberships/stats")
    Call<Map<String, Object>> getMembershipStats();

    @GET("api/genres/{id}")
    Call<Map<String, Object>> getGenreById(@Path("id") Integer genreId);

    @GET("api/songs/genre/{id}")
    Call<Map<String, Object>> getSongsByGenre(@Path("id") Integer genreId);

    @GET("api/wallet/stats")
    Call<Map<String, Object>> getWalletStats();

    @GET("api/admin/analytics")
    Call<Map<String, Object>> getAnalytics(@Query("period") String period);

    @GET("api/admin/analytics/users")
    Call<Map<String, Object>> getUserAnalytics();

    @GET("api/admin/analytics/songs")
    Call<Map<String, Object>> getSongAnalytics();

    // Revenue & Payout
    @POST("api/revenue/calculate-monthly")
    Call<Map<String, Object>> calculatePremiumPayout(@Body Map<String, Object> payload);

    @POST("api/revenue/apply-monthly")
    Call<Map<String, Object>> applyPremiumPayout(@Body Map<String, Object> payload);

    @GET("api/revenue/payout-history")
    Call<Map<String, Object>> getPayoutHistory();

    @GET("api/revenue/payout-batch")
    Call<Map<String, Object>> getPayoutBatchDetails(@Query("batch_time") String batchTime);

    @POST("api/withdrawals/request")
    Call<Map<String, Object>> requestWithdrawal(@Body Map<String, Object> payload);
}
