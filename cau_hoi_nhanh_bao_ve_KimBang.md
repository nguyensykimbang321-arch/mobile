# 📋 CÂU HỎI NHANH BẢO VỆ ĐỒ ÁN (PHẦN NGUYỄN SỸ KIM BẰNG)

Tài liệu này tổng hợp các câu hỏi hỏi nhanh - đáp gọn của Giảng viên khi demo đồ án và chỉ rõ **tên file + dòng code** để bạn mở trực tiếp cho Thầy xem.

---

## 📺 PHẦN 1: TRANG CHỦ & DANH SÁCH (RECYCLERVIEW & ADAPTER)

### Câu 1
*   **Thầy:** Giao diện danh sách trên trang chủ dùng widget gì?
*   **Tôi:** Dùng **RecyclerView** để tối ưu bộ nhớ.
*   **File cần mở:** [fragment_home.xml](file:///e:/tam/Android/mobile/app/src/main/res/layout/fragment_home.xml) (Thẻ `<androidx.recyclerview.widget.RecyclerView>`).

### Câu 2
*   **Thầy:** Tại sao RecyclerView lại tối ưu hơn ListView?
*   **Tôi:** RecyclerView tái sử dụng lại các View Holder đã cuộn khỏi màn hình thông qua cơ chế Recycle Bin, tránh việc tạo mới View liên tục giúp app mượt hơn.

### Câu 3
*   **Thầy:** Để tạo một RecyclerView hoàn chỉnh cần mấy bước?
*   **Tôi:** Cần **3 bước**:
    1. Khai báo thẻ `<RecyclerView>` trong file layout XML.
    2. Tạo file XML cho item (ví dụ: [item_card.xml](file:///e:/tam/Android/mobile/app/src/main/res/layout/item_card.xml)).
    3. Viết lớp **Adapter** (extends `RecyclerView.Adapter`) để kết nối dữ liệu.
*   **File cần mở:** [CardAdapter.java](file:///e:/tam/Android/mobile/app/src/main/java/com/appad/adapters/CardAdapter.java) (Adapter cho bài hát nổi bật).

### Câu 4
*   **Thầy:** Adapter của RecyclerView có những phương thức bắt buộc nào cần override?
*   **Tôi:** Có **3 phương thức**:
    1. `onCreateViewHolder()` (Tạo View).
    2. `onBindViewHolder()` (Đổ dữ liệu).
    3. `getItemCount()` (Trả về số lượng).
*   **File cần mở:** [CardAdapter.java](file:///e:/tam/Android/mobile/app/src/main/java/com/appad/adapters/CardAdapter.java) (Chỉ vào 3 hàm này trong code).

### Câu 5
*   **Thầy:** Kéo thả sắp xếp danh sách (Drag & Drop) hàng đợi bạn làm thế nào?
*   **Tôi:** Dùng lớp **`ItemTouchHelper`** kết hợp với `Callback` để di chuyển item và cập nhật dữ liệu.
*   **File cần mở:** 
    *   [SongDragCallback.java](file:///e:/tam/Android/mobile/app/src/main/java/com/appad/utils/SongDragCallback.java) (Logic bắt sự kiện kéo thả).
    *   [DraggableSongAdapter.java](file:///e:/tam/Android/mobile/app/src/main/java/com/appad/adapters/DraggableSongAdapter.java) (Adapter cập nhật lại vị trí bài hát).

### Câu 6
*   **Thầy:** Trên trang chủ, làm sao để khi vuốt từ trên xuống thì danh sách được cập nhật lại?
*   **Tôi:** Dùng widget **`SwipeRefreshLayout`** bọc ngoài RecyclerView, lắng nghe sự kiện `onRefresh()`.
*   **File cần mở:**
    *   [fragment_home.xml](file:///e:/tam/Android/mobile/app/src/main/res/layout/fragment_home.xml) (Thẻ `<androidx.swiperefreshlayout.widget.SwipeRefreshLayout>`).
    *   [HomeFragment.java](file:///e:/tam/Android/mobile/app/src/main/java/com/appad/fragments/HomeFragment.java) (Tìm đến hàm `setupSwipeRefresh()`).

### Câu 7
*   **Thầy:** Trang chủ có cơ chế tự động cuộn (Auto Scroll) các danh sách ngang, bạn xử lý thế nào?
*   **Tôi:** Dùng một **`Handler` + `Runnable`** để chạy lặp lại sau mỗi 4-6 giây và gọi `smoothScrollToPosition()`.
*   **File cần mở:** [HomeFragment.java](file:///e:/tam/Android/mobile/app/src/main/java/com/appad/fragments/HomeFragment.java) (Tìm đến Runnable `autoScrollRunnableTrending` ở đầu file).

### Câu 8
*   **Thầy:** Load ảnh bìa bài hát từ link mạng về ImageView dùng thư viện nào?
*   **Tôi:** Dùng thư viện **`Glide`**.
*   **File cần mở:** [CardAdapter.java](file:///e:/tam/Android/mobile/app/src/main/java/com/appad/adapters/CardAdapter.java) (Tìm dòng `Glide.with(context).load(...)`).

---

## 🎨 PHẦN 2: DANH SÁCH NGHỆ SĨ & THỂ LOẠI (ARTISTS & GENRES)

### Câu 9
*   **Thầy:** Ảnh đại diện của nghệ sĩ hiển thị dạng hình tròn trên trang chủ làm bằng cách nào?
*   **Tôi:** Dùng widget **`ShapeableImageView`** và cấu hình thuộc tính bo góc tròn 50% trong style XML.
*   **File cần mở:** [item_artist_circle.xml](file:///e:/tam/Android/mobile/app/src/main/res/layout/item_artist_circle.xml) (Tìm thẻ `<com.google.android.material.imageview.ShapeableImageView>`).

### Câu 10
*   **Thầy:** Nhấp vào một nghệ sĩ trên trang chủ thì chuyển sang màn hình chi tiết nghệ sĩ bằng cách nào?
*   **Tôi:** Viết sự kiện onClick trong ViewHolder của Adapter, dùng **`Intent`** để truyền `artistId` và khởi chạy `ArtistDetailActivity`.
*   **File cần mở:** [ArtistAdapter.java](file:///e:/tam/Android/mobile/app/src/main/java/com/appad/adapters/ArtistAdapter.java) (Tìm hàm `onBindViewHolder` hoặc onClick trong `ViewHolder`).

### Câu 11
*   **Thầy:** Giao diện Thể loại nhạc (Genres) có cấu trúc như thế nào?
*   **Tôi:** Giao diện thể loại gồm các ô lưới (Grid) hiển thị màu sắc và tên thể loại. Dữ liệu nạp qua `GenreAdapter`.
*   **File cần mở:** [item_genre_card.xml](file:///e:/tam/Android/mobile/app/src/main/res/layout/item_genre_card.xml) (Giao diện thẻ ô lưới thể loại).

---

## 🎧 PHẦN 3: TRÌNH PHÁT NHẠC TOÀN MÀN HÌNH (FULL PLAYER)

### Câu 12
*   **Thầy:** Làm sao để màu nền của trang Full Player tự động thay đổi theo tông màu ảnh bìa bài hát đang phát?
*   **Tôi:** Dùng thư viện **`Palette API`** phân tích ảnh bìa thành Bitmap, lấy ra các màu chính rồi dùng `GradientDrawable` để làm nền.
*   **File cần mở:** [FullPlayerActivity.java](file:///e:/tam/Android/mobile/app/src/main/java/com/appad/activities/FullPlayerActivity.java) (Tìm hàm `updateGradientBackground()`).

### Câu 13
*   **Thầy:** Hiệu ứng nhịp đập (pulse) và quầng sáng lan tỏa (halo) của nút Play/Pause hoạt động thế nào?
*   **Tôi:** Dùng **`ObjectAnimator`** tác động liên tục vào thuộc tính `scaleX`, `scaleY` và `alpha` của View.
*   **File cần mở:** [FullPlayerActivity.java](file:///e:/tam/Android/mobile/app/src/main/java/com/appad/activities/FullPlayerActivity.java) (Tìm hàm `startPulseEffect()`).

### Câu 14
*   **Thầy:** Lời bài hát (Lyrics) và Hàng đợi (Queue) hiển thị dưới dạng gì?
*   **Tôi:** Hiển thị dưới dạng **`BottomSheetDialogFragment`** trượt từ dưới lên để không gây gián đoạn màn hình phát nhạc.
*   **File cần mở:** 
    *   [LyricsBottomSheetFragment.java](file:///e:/tam/Android/mobile/app/src/main/java/com/appad/fragments/LyricsBottomSheetFragment.java) (Phần lời bài hát).
    *   [QueueFragment.java](file:///e:/tam/Android/mobile/app/src/main/java/com/appad/fragments/QueueFragment.java) (Phần danh sách chờ).

---

## ⚡ PHẦN 4: TRÌNH PHÁT THU NHỎ (MINI PLAYER)

### Câu 15
*   **Thầy:** Mini Player hoạt động như thế nào và nằm ở đâu?
*   **Tôi:** Mini Player là một Layout thu nhỏ nằm neo ở đáy màn hình chính (`MainActivity`), được quản lý bởi `MiniPlayerHelper`.
*   **File cần mở:** [layout_mini_player.xml](file:///e:/tam/Android/mobile/app/src/main/res/layout/layout_mini_player.xml).

### Câu 16
*   **Thầy:** Làm sao để Mini Player đồng bộ trạng thái (Play/Pause, tên bài hát) y hệt như Full Player?
*   **Tôi:** Cả hai đều cùng lắng nghe (đăng ký làm Listener) sự thay đổi trạng thái từ **`MusicPlayerManager`** (Singleton). Khi bài hát thay đổi, Manager sẽ notify để cả hai cập nhật cùng lúc.
*   **File cần mở:** [MiniPlayerHelper.java](file:///e:/tam/Android/mobile/app/src/main/java/com/appad/utils/MiniPlayerHelper.java) (Tìm dòng `MusicPlayerManager.getInstance().addStatusChangeListener(...)` trong hàm `setupStatusListener`).

---

## 🧩 PHẦN 5: FRAGMENT & ACTIVITY (ĐIỀU HƯỚNG CHÍNH)

### Câu 17
*   **Thầy:** 1 Fragment có mấy file?
*   **Tôi:** Cần **2 file**: File logic **`.java`** và file giao diện **`.xml`**.
*   **File cần mở:** [HomeFragment.java](file:///e:/tam/Android/mobile/app/src/main/java/com/appad/fragments/HomeFragment.java) và [fragment_home.xml](file:///e:/tam/Android/mobile/app/src/main/res/layout/fragment_home.xml).

### Câu 18
*   **Thầy:** Sự khác nhau cơ bản giữa Activity và Fragment là gì?
*   **Tôi:** Activity là một màn hình độc lập có vòng đời riêng do hệ thống quản lý. Fragment là một phần giao diện phụ thuộc và phải gắn vào một Activity cha.

### Câu 19
*   **Thầy:** MainActivity quản lý việc chuyển đổi giữa các Fragment ở Bottom Navigation như thế nào?
*   **Tôi:** Dùng cơ chế **Add/Show/Hide** của `FragmentManager` thay vì `replace` để giữ lại trạng thái Fragment cũ (không phải load lại từ API).
*   **File cần mở:** [MainActivity.java](file:///e:/tam/Android/mobile/app/src/main/activities/MainActivity.java) (Tìm hàm `loadFragment()` hoặc sự kiện click `bottomNavigation`).

---

## 🎵 PHẦN 6: SERVICE & CORE MUSIC PLAYING

### Câu 20
*   **Thầy:** Thư viện nào được dùng để phát nhạc trong app?
*   **Tôi:** Dùng thư viện **`ExoPlayer`** của Google.
*   **File cần mở:** [MusicService.java](file:///e:/tam/Android/mobile/app/src/main/java/com/appad/services/MusicService.java) (Tìm dòng khởi tạo `new ExoPlayer.Builder(this)`).

### Câu 21
*   **Thầy:** Làm sao tắt màn hình hoặc chuyển sang ứng dụng khác mà nhạc vẫn phát?
*   **Tôi:** Dùng **Foreground Service** (`MusicService`) kết hợp hiển thị Notification để hệ thống không kill tiến trình.
*   **File cần mở:** [MusicService.java](file:///e:/tam/Android/mobile/app/src/main/java/com/appad/services/MusicService.java) (Tìm dòng `startForeground(NOTIFICATION_ID, notification)` trong hàm `updateNotification`).

### Câu 22
*   **Thầy:** Tại sao phải dùng Foreground Service mà không dùng Service thường (Background Service)?
*   **Tôi:** Vì từ Android 8.0+, hệ thống sẽ tự động kill các Service chạy ngầm (Background) sau khoảng 1 phút khi app xuống nền. Foreground Service có thông báo hiển thị cho user biết nên được hệ thống ưu tiên giữ lại.

### Câu 23
*   **Thầy:** Làm thế nào để Activity liên kết và điều khiển được Service phát nhạc?
*   **Tôi:** Dùng cơ chế **Binder Pattern** và **ServiceConnection**. Activity gọi `bindService()`, Service trả về một `IBinder` để Activity điều khiển trực tiếp các hàm trong Service.
*   **File cần mở:** 
    *   [MusicService.java](file:///e:/tam/Android/mobile/app/src/main/java/com/appad/services/MusicService.java) (Tìm lớp `MusicBinder`).
    *   [MusicPlayerManager.java](file:///e:/tam/Android/mobile/app/src/main/java/com/appad/utils/MusicPlayerManager.java) (Tìm lớp `ServiceConnection` ở đầu file).

### Câu 24
*   **Thầy:** `MusicPlayerManager` dùng Design Pattern nào? Tại sao?
*   **Tôi:** Dùng **Singleton Pattern** để đảm bảo chỉ có duy nhất một trình quản lý trạng thái phát nhạc trên toàn app (không bị phát chồng chéo nhạc).
*   **File cần mở:** [MusicPlayerManager.java](file:///e:/tam/Android/mobile/app/src/main/java/com/appad/utils/MusicPlayerManager.java) (Tìm hàm `getInstance()`).

### Câu 25
*   **Thầy:** App có hẹn giờ tắt nhạc không? Bạn dùng gì để hẹn giờ?
*   **Tôi:** Có. Dùng **`Handler.postDelayed()`** để chạy lệnh pause sau số phút người dùng chọn.
*   **File cần mở:** [MusicPlayerManager.java](file:///e:/tam/Android/mobile/app/src/main/java/com/appad/utils/MusicPlayerManager.java) (Tìm hàm `startSleepTimer()`).

### Câu 26
*   **Thầy:** Quyền hiển thị thông báo nhạc trên Android 13+ xin ở đâu?
*   **Tôi:** Xin quyền runtime **`POST_NOTIFICATIONS`** ở MainActivity.
*   **File cần mở:** [MainActivity.java](file:///e:/tam/Android/mobile/app/src/main/java/com/appad/activities/MainActivity.java) (Tìm đoạn request permission `Manifest.permission.POST_NOTIFICATIONS`).

---

## 🔧 PHẦN 7: BACKEND (API BÀI HÁT & THỂ LOẠI)

### Câu 27
*   **Thầy:** Bạn phụ trách những API nào ở Backend?
*   **Tôi:** API quản lý Bài hát (lấy bài mới nhất, bài thịnh hành, lượt phát) và API Thể loại nhạc.
*   **File cần mở:** [SongController.java](file:///e:/tam/Android/backend/src/main/java/com/appad/controllers/SongController.java) (Các Route `/api/songs/*`).
