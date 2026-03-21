# 📋 PHÂN CÔNG CÔNG VIỆC ĐỒ ÁN - ỨNG DỤNG NGHE NHẠC (APPAD)

## 👑 Nguyễn Sỹ Kim Bằng

### 1. Thiết kế giao diện & Logic trang chủ (Home)
| Hạng mục | File liên quan |
|----------|----------------|
| Layout trang chủ | `fragment_home.xml` |
| Logic hiển thị trang chủ (Bài hát nổi bật, Nghệ sĩ, Album gợi ý, Thể loại) | `HomeFragment.java` |
| Card item hiển thị bài hát/album | `item_card.xml`, `CardAdapter.java` |
| Item hiển thị nghệ sĩ dạng tròn | `item_artist_circle.xml`, `ArtistAdapter.java` |
| Item hiển thị thể loại | `item_genre.xml`, `item_genre_card.xml`, `GenreAdapter.java` |

### 2. MiniPlayer & Player toàn màn hình
| Hạng mục | File liên quan |
|----------|----------------|
| Giao diện MiniPlayer | `layout_mini_player.xml` |
| Logic MiniPlayer | `MiniPlayerHelper.java` |
| Giao diện Full Player | `activity_full_player.xml` |
| Logic Full Player (play/pause, seek, repeat, shuffle, lyrics, queue, comments) | `FullPlayerActivity.java` |
| Music Service (xử lý phát nhạc nền) | `MusicService.java` |
| Music Player Manager (quản lý trạng thái phát) | `MusicPlayerManager.java` |
| Bottom sheet hành động Player | `bottom_sheet_player_actions.xml`, `PlayerActionsBottomSheet.java` |
| Hiển thị lời bài hát | `layout_lyrics_sheet.xml`, `LyricsBottomSheetFragment.java` |
| Hàng đợi phát nhạc | `layout_queue_sheet.xml`, `QueueFragment.java` |
| Item hàng đợi | `item_queue_song.xml` |
| Kéo thả bài hát trong queue | `DraggableSongAdapter.java`, `SongDragCallback.java`, `item_song_draggable.xml` |
| Hẹn giờ tắt nhạc | `dialog_sleep_timer.xml` |

### 3. Thiết kế giao diện chung & Activity chính
| Hạng mục | File liên quan |
|----------|----------------|
| Layout chính (Bottom Navigation + Fragment container) | `activity_main.xml` |
| Logic điều hướng chính | `MainActivity.java` |
| Application class (khởi tạo app) | `MusicApplication.java` |

### 4. Backend — Thể loại & Bài hát (API cơ bản)
| Hạng mục | File liên quan |
|----------|----------------|
| API Controller thể loại | `GenreController.java` |
| Service thể loại | `GenreService.java` |
| Repository thể loại | `GenreRepository.java` |
| Model thể loại | `Genre.java` (Backend) |
| API Controller bài hát | `SongController.java` |
| Service bài hát | `SongService.java` |
| Repository bài hát | `SongRepository.java` |
| Model bài hát | `Song.java` (Backend) |

---

## 🔵 THÀNH VIÊN B — THÀNH VIÊN 1

### 1. Xác thực & Quản lý người dùng
| Hạng mục | File liên quan |
|----------|----------------|
| Giao diện đăng nhập | `activity_login.xml` |
| Logic đăng nhập | `LoginActivity.java` |
| Giao diện đăng ký | `activity_register.xml` |
| Logic đăng ký | `RegisterActivity.java` |
| Giao diện đổi mật khẩu | `activity_change_password.xml` |
| Logic đổi mật khẩu | `ChangePasswordActivity.java` |
| Giao diện sửa hồ sơ | `activity_edit_profile.xml` |
| Logic sửa hồ sơ | `EditProfileActivity.java` |
| Quản lý phiên đăng nhập | `SessionManager.java` |

### 2. Trang Profile & Menu
| Hạng mục | File liên quan |
|----------|----------------|
| Layout Profile Fragment | `fragment_profile.xml` |
| Logic Profile | `ProfileFragment.java` |
| Item menu Profile | `layout_profile_menu_item.xml`, `ProfileMenuItemView.java` |

### 3. Backend — Xác thực & Người dùng
| Hạng mục | File liên quan |
|----------|----------------|
| API Controller xác thực | `AuthController.java` |
| Service xác thực | `AuthService.java` |
| API Controller người dùng | `UserController.java` |
| JWT Filter & Config bảo mật | `JwtFilter.java`, `SecurityConfig.java` |
| JWT Utilities | `JwtUtils.java` |
| Model người dùng | `User.java` (Backend) |
| Repository người dùng | `UserRepository.java` |
| Model người dùng (Mobile) | `User.java` (Mobile) |

### 4. Backend — Thông báo
| Hạng mục | File liên quan |
|----------|----------------|
| API Controller thông báo | `NotificationController.java` |
| Service thông báo | `NotificationService.java` |
| Repository thông báo | `NotificationRepository.java` |
| Model thông báo | `Notification.java` (Backend) |

### 5. Mobile — Thông báo
| Hạng mục | File liên quan |
|----------|----------------|
| Giao diện thông báo | `activity_notification.xml` |
| Logic thông báo | `NotificationActivity.java` |
| Adapter thông báo | `NotificationAdapter.java` |
| Item thông báo | `item_notification.xml` |
| Model thông báo (Mobile) | `Notification.java` (Mobile) |
| Dialog tạo thông báo | `dialog_create_notification.xml` |

### 6. Tải xuống & Các chức năng phụ
| Hạng mục | File liên quan |
|----------|----------------|
| Logic tải bài hát xuống | `DownloadHelper.java` |
| Item bài hát tải về | `item_download_row.xml` |
| Modal "Bạn muốn nghe tiếp?" (Continue Listening) | `layout_continue_listening_modal.xml`, `ContinueListeningModal.java` |
| Dialog thông báo tùy chỉnh | `dialog_custom_alert.xml`, `CustomAlertDialogFragment.java` |
| Dialog thành công | `dialog_success.xml`, `SuccessDialogFragment.java` |
| Dialog báo cáo vi phạm | `dialog_report.xml`, `ReportDialogFragment.java` |
| Base Bottom Sheet | `BaseBottomSheetFragment.java`, `layout_bottom_sheet.xml` |

---

## 🟢 THÀNH VIÊN C — THÀNH VIÊN 2

### 1. Tìm kiếm & Thư viện
| Hạng mục | File liên quan |
|----------|----------------|
| Layout tìm kiếm | `fragment_search.xml` |
| Logic tìm kiếm (bài hát, nghệ sĩ, album, thể loại) | `SearchFragment.java` |
| Item kết quả tìm kiếm bài hát | `item_search_result.xml` |
| Item kết quả tìm kiếm album | `item_search_album.xml` |
| Item kết quả tìm kiếm nghệ sĩ | `item_search_artist.xml` |
| Layout thư viện | `fragment_library.xml`, `activity_library.xml` |
| Logic thư viện | `LibraryFragment.java`, `LibraryActivity.java` |
| Item hàng thư viện | `item_library_row.xml` |
| Chi tiết thể loại | `activity_genre_detail.xml`, `GenreDetailActivity.java` |

### 2. Playlist & Chia sẻ Playlist
| Hạng mục | File liên quan |
|----------|----------------|
| Giao diện chi tiết Playlist | `activity_playlist_detail.xml` |
| Logic chi tiết Playlist | `PlaylistDetailActivity.java` |
| Thêm vào Playlist (Bottom Sheet) | `layout_add_to_playlist_sheet.xml`, `AddToPlaylistFragment.java` |
| Item Playlist nhỏ | `item_playlist_small.xml` |
| Item bài hát trong Playlist | `item_song_playlist.xml`, `item_playlist_song_inline.xml` |
| Layout item Playlist | `layout_playlist_item.xml` |
| Chia sẻ Playlist | `dialog_share_playlist.xml`, `activity_shared_playlist.xml`, `SharedPlaylistActivity.java` |
| Import Playlist | `dialog_import_playlist.xml` |

### 3. Lịch sử nghe & Bình luận
| Hạng mục | File liên quan |
|----------|----------------|
| Adapter lịch sử nghe | `HistoryAdapter.java` |
| Item lịch sử (header + song) | `item_history_header.xml`, `item_history_song.xml` |
| Bình luận Bottom Sheet | `layout_comments_sheet.xml`, `CommentsBottomSheetFragment.java` |
| Adapter bình luận | `CommentsAdapter.java` |
| Item bình luận | `item_comment.xml` |
| More Actions | `layout_more_actions_sheet.xml`, `MoreActionsFragment.java` |

### 4. Backend — Tìm kiếm, Playlist, Lịch sử, Bình luận, Follow, Yêu thích
| Hạng mục | File liên quan |
|----------|----------------|
| API Controller tìm kiếm | `SearchController.java` |
| API Controller playlist | `PlaylistController.java` |
| Repository playlist | `PlaylistRepository.java`, `PlaylistSongRepository.java` |
| Model playlist | `Playlist.java`, `PlaylistSong.java` |
| Tiện ích chia sẻ playlist | `PlaylistShareUtils.java` |
| API Controller lịch sử nghe | `HistoryController.java` |
| Service lịch sử | `HistoryService.java` |
| Repository lịch sử | `ListeningHistoryRepository.java` |
| Model lịch sử | `ListeningHistory.java` |
| API Controller bình luận | `CommentController.java` |
| Repository bình luận | `CommentRepository.java` |
| Model bình luận | `Comment.java` |
| API Controller follow | `FollowController.java` |
| Repository follow | `FollowRepository.java` |
| Model follow | `Follow.java` |
| API Controller yêu thích | `FavoriteController.java` |
| Repository yêu thích | `FavoriteRepository.java` |
| Model yêu thích | `Favorite.java` |

---

## 🟡 THÀNH VIÊN D — THÀNH VIÊN 3

### 1. Hệ thống Premium & Ví điện tử
| Hạng mục | File liên quan |
|----------|----------------|
| Giao diện Premium | `activity_premium.xml` |
| Logic Premium | `PremiumActivity.java` |
| Modal truy cập Premium | `layout_premium_access_modal.xml`, `PremiumAccessModal.java` |
| Premium Badge | `PremiumBadgeView.java` |
| Access Badge | `AccessBadgeView.java` |
| Access Helper (kiểm tra quyền truy cập) | `AccessHelper.java` |
| Giao diện Ví | `activity_wallet.xml` |
| Logic Ví | `WalletActivity.java` |
| Giao diện Nạp tiền | `activity_deposit.xml` |
| Logic Nạp tiền | `DepositActivity.java` |
| Giao diện Rút tiền | `activity_withdraw.xml` |
| Logic Rút tiền | `WithdrawActivity.java` |
| Lịch sử giao dịch | `activity_transaction_history.xml`, `TransactionHistoryActivity.java` |
| Adapter giao dịch | `TransactionAdapter.java` |
| Item giao dịch | `item_transaction.xml` |
| Mua bài hát | `dialog_purchase_confirmation.xml`, `layout_purchase_confirmation_modal.xml`, `PurchaseConfirmationModal.java`, `PurchaseDialogFragment.java` |
| Bài hát đã mua | `activity_purchased_songs.xml`, `PurchasedSongsActivity.java` |
| Model giao dịch (Mobile) | `Transaction.java` (Mobile) |

### 2. Nghệ sĩ Studio & Album
| Hạng mục | File liên quan |
|----------|----------------|
| Chi tiết nghệ sĩ | `activity_artist_detail.xml`, `ArtistDetailActivity.java` |
| Dashboard nghệ sĩ | `activity_artist_dashboard.xml`, `ArtistDashboardActivity.java` |
| Đánh giá nghệ sĩ | `activity_artist_reviews.xml`, `ArtistReviewsActivity.java` |
| Upload bài hát | `activity_upload.xml`, `UploadActivity.java` |
| Quản lý bài hát | `activity_manage_songs.xml`, `ManageSongsActivity.java` |
| Chỉnh sửa bài hát | `activity_edit_song.xml`, `EditSongActivity.java` |
| Item quản lý bài hát | `item_manage_song.xml` |
| Chi tiết Album | `activity_album_detail.xml`, `AlbumDetailActivity.java` |
| Quản lý album | `activity_manage_albums.xml`, `ManageAlbumsActivity.java` |
| Chỉnh sửa album | `activity_edit_album.xml`, `EditAlbumActivity.java` |
| Item quản lý album | `item_manage_album.xml` |
| Item album grid | `item_album_grid.xml`, `AlbumCardAdapter.java` |
| Hội viên nghệ sĩ | `activity_manage_membership.xml`, `ManageMembershipActivity.java` |
| Lịch sử hội viên | `activity_membership_history.xml`, `MembershipHistoryActivity.java` |
| Adapter hội viên | `MembershipAdapter.java` |
| Item hội viên | `item_membership.xml` |
| Model nghệ sĩ (Mobile) | `Artist.java`, `ArtistMembership.java` (Mobile) |
| Model album (Mobile) | `Album.java` (Mobile) |

### 3. Trang Admin (toàn bộ hệ thống quản trị)
| Hạng mục | File liên quan |
|----------|----------------|
| Admin Dashboard | `activity_admin.xml`, `activity_admin_dashboard.xml`, `AdminActivity.java` |
| Admin quản lý người dùng | `activity_admin_users.xml`, `AdminUsersActivity.java`, `AdminUserAdapter.java`, `item_admin_user.xml` |
| Admin quản lý bài hát | `activity_admin_songs.xml`, `AdminSongsActivity.java`, `AdminSongAdapter.java`, `item_admin_song.xml`, `item_admin_song_simple.xml` |
| Admin chỉnh sửa bài hát | `activity_admin_edit_song.xml`, `AdminEditSongActivity.java` |
| Admin quản lý album | `activity_admin_albums.xml`, `AdminAlbumsActivity.java`, `AdminAlbumAdapter.java`, `item_admin_album.xml` |
| Admin chỉnh sửa album | `activity_admin_edit_album.xml`, `AdminEditAlbumActivity.java` |
| Admin duyệt nghệ sĩ | `activity_admin_artist_approval.xml`, `AdminArtistApprovalActivity.java` |
| Admin đánh giá | `AdminReviewsActivity.java` |
| Admin hội viên | `activity_admin_membership.xml`, `AdminMembershipActivity.java`, `AdminMembershipAdapter.java`, `item_admin_membership.xml` |
| Admin giao dịch | `activity_admin_transactions.xml`, `AdminTransactionsActivity.java`, `AdminTransactionAdapter.java`, `item_admin_transaction.xml` |
| Admin rút tiền | `activity_admin_withdrawals.xml`, `AdminWithdrawalsActivity.java`, `AdminWithdrawalAdapter.java`, `item_admin_withdrawal.xml` |
| Admin thanh toán Premium | `activity_admin_premium_payout.xml`, `AdminPremiumPayoutActivity.java`, `ArtistPayoutAdapter.java`, `item_artist_payout.xml`, `PayoutHistoryAdapter.java`, `item_payout_history.xml` |
| Admin phát sóng thông báo | `activity_admin_broadcast.xml`, `AdminBroadcastActivity.java` |
| Admin thống kê / Analytics | `activity_admin_analytics.xml`, `AdminAnalyticsActivity.java`, `item_analytics_stat_box.xml`, `item_analytics_top_song.xml` |
| Admin danh sách chung | `activity_admin_list.xml` |
| Item menu admin | `item_admin_menu_card.xml`, `item_admin_stat_panel.xml` |

### 4. Backend — Premium, Ví, Nghệ sĩ, Album, Admin, Báo cáo, Report
| Hạng mục | File liên quan |
|----------|----------------|
| API Controller Premium | `PremiumController.java` |
| API Controller Ví | `WalletController.java` |
| Model giao dịch | `Transaction.java` (Backend) |
| Repository giao dịch | `TransactionRepository.java` |
| Model mua bài hát / album | `PurchasedSong.java`, `PurchasedAlbum.java` |
| Repository mua | `PurchasedSongRepository.java`, `PurchasedAlbumRepository.java` |
| API Controller nghệ sĩ | `ArtistController.java` |
| Service nghệ sĩ | `ArtistService.java` |
| Repository nghệ sĩ | `ArtistRepository.java` |
| Model nghệ sĩ | `Artist.java` (Backend) |
| API Controller hội viên nghệ sĩ | `ArtistMembershipController.java` |
| Repository hội viên | `ArtistMembershipRepository.java` |
| Model hội viên | `ArtistMembership.java` (Backend) |
| API Controller Artist Studio | `ArtistStudioController.java` |
| API Controller album | `AlbumController.java` |
| Service album | `AlbumService.java` |
| Repository album | `AlbumRepository.java` |
| Model album | `Album.java` (Backend) |
| API Controller Admin | `AdminController.java` |
| API Controller doanh thu | `RevenueController.java` |
| Service doanh thu | `RevenueService.java` |
| Model doanh thu | `RevenueSharing.java`, `PremiumListeningStats.java` |
| Repository doanh thu | `RevenueSharingRepository.java`, `PremiumListeningStatsRepository.java` |
| API Controller báo cáo vi phạm | `ReportController.java` |
| Repository báo cáo | `ReportRepository.java` |
| Model báo cáo | `Report.java` |
| Upload file (Cloudinary / Local) | `CloudinaryService.java`, `FileStorageService.java` |

---

## ⏰ CRON JOB / SCHEDULED TASKS

> **File:** `TaskSchedulerService.java`  
> **Người phụ trách:** Chia đều cho cả nhóm, mỗi người phụ trách task tương ứng chức năng mình quản lý.

| Cron Job | Mô tả | Cron Expression | Phụ trách |
|----------|--------|-----------------|-----------|
| `checkExpiredMemberships()` | Kiểm tra & cập nhật hội viên nghệ sĩ hết hạn | `0 */5 * * * *` (mỗi 5 phút) | **Thành viên D** |
| `monthlyRevenueReport()` | Tự động tổng kết doanh thu tháng trước | `0 */5 * * * *` (mỗi 5 phút) | **Thành viên D** |
| `checkExpiringPremium()` | Thông báo cho user Premium sắp hết hạn (trước 3 ngày) | `0 */5 * * * *` (mỗi 5 phút) | **Thành viên D** |

> **Ghi chú:** Các cron job hiện tại đều chạy mỗi 5 phút (dùng cho demo/test). Trong môi trường production, nên điều chỉnh:
> - `checkExpiredMemberships()` → `0 0 * * * *` (mỗi giờ)
> - `monthlyRevenueReport()` → `0 0 1 * * *` (ngày 1 mỗi tháng)
> - `checkExpiringPremium()` → `0 0 9 * * *` (9h sáng mỗi ngày)

---

## 🔧 PHẦN DÙNG CHUNG (Cả nhóm cùng sử dụng)

| Hạng mục | File liên quan | Ghi chú |
|----------|----------------|---------|
| ApiService (Retrofit API Interface) | `ApiService.java` | Tất cả thành viên cùng bổ sung endpoint |
| RetrofitClient | `RetrofitClient.java` | Cấu hình chung, cả nhóm sử dụng |
| FormatUtils | `FormatUtils.java` | Utility format dữ liệu dùng chung |
| ImageUrlUtils | `ImageUrlUtils.java` | Xử lý URL ảnh dùng chung |
| SongAdapter | `SongAdapter.java` | Adapter hiển thị bài hát dùng chung |
| Item bài hát | `item_song.xml` | Layout item bài hát dùng chung |
| Dropdown menu | `item_dropdown_menu.xml` | Item dropdown dùng chung |
| Item Dashboard Action | `item_dashboard_action.xml` | Item action dùng chung |
| Item Revenue Stat | `item_revenue_stat.xml` | Item thống kê doanh thu |
| Item Player Action | `item_player_action.xml` | Item action trong player |
| Middleware | `middleware/` | Xử lý middleware chung |
| Application Config | `AppadApplication.java` (Backend) | Config ứng dụng backend |
