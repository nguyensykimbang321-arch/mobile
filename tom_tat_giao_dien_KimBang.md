# 📝 TÓM TẮT THÀNH PHẦN GIAO DIỆN PHÂN KHU NGUYỄN SỸ KIM BẰNG

Tài liệu này tóm tắt toàn bộ các thành phần giao diện (UI), bố cục (Layouts), mã màu sắc (Colors), kiểu dáng (Styles), hiệu ứng chuyển động (Animations) và các Adapter điều khiển danh sách mà **Nguyễn Sỹ Kim Bằng** phụ trách trong ứng dụng nghe nhạc **APPAD**.

---

## 1. 📂 Các File Giao Diện Phụ Trách

### Giao diện chính & Trang chủ
*   [activity_main.xml](file:///e:/tam/Android/mobile/app/src/main/res/layout/activity_main.xml): Layout tổng thể của ứng dụng (Bottom Navigation + Container Fragment).
*   [fragment_home.xml](file:///e:/tam/Android/mobile/app/src/main/res/layout/fragment_home.xml): Layout giao diện Trang chủ (Trending, Artists, Albums, Danh sách Tab).

### Trình phát nhạc (Players)
*   [layout_mini_player.xml](file:///e:/tam/Android/mobile/app/src/main/res/layout/layout_mini_player.xml): Giao diện trình phát nhạc thu nhỏ (Mini Player) neo ở đáy màn hình.
*   [activity_full_player.xml](file:///e:/tam/Android/mobile/app/src/main/res/layout/activity_full_player.xml): Giao diện trình phát nhạc toàn màn hình (Full Player).

### Các Dialog & Bottom Sheets
*   [bottom_sheet_player_actions.xml](file:///e:/tam/Android/mobile/app/src/main/res/layout/bottom_sheet_player_actions.xml): Menu hành động phụ của bài hát.
*   [layout_lyrics_sheet.xml](file:///e:/tam/Android/mobile/app/src/main/res/layout/layout_lyrics_sheet.xml): Giao diện hiển thị lời bài hát (Lyrics).
*   [layout_queue_sheet.xml](file:///e:/tam/Android/mobile/app/src/main/res/layout/layout_queue_sheet.xml): Giao diện hàng đợi phát nhạc (Queue).
*   [dialog_sleep_timer.xml](file:///e:/tam/Android/mobile/app/src/main/res/layout/dialog_sleep_timer.xml): Dialog hẹn giờ tắt nhạc.

### Các Item Adapter (ListView / RecyclerView Items)
*   [item_card.xml](file:///e:/tam/Android/mobile/app/src/main/res/layout/item_card.xml): Card hiển thị bài hát/album nổi bật (hình vuông bo góc).
*   [item_artist_circle.xml](file:///e:/tam/Android/mobile/app/src/main/res/layout/item_artist_circle.xml): Item hiển thị nghệ sĩ (hình tròn).
*   [item_genre.xml](file:///e:/tam/Android/mobile/app/src/main/res/layout/item_genre.xml): Item thể loại nhạc theo hàng ngang.
*   [item_genre_card.xml](file:///e:/tam/Android/mobile/app/src/main/res/layout/item_genre_card.xml): Card thể loại dạng ô lưới.
*   [item_queue_song.xml](file:///e:/tam/Android/mobile/app/src/main/res/layout/item_queue_song.xml): Dòng bài hát hiển thị trong hàng đợi.
*   [item_song_draggable.xml](file:///e:/tam/Android/mobile/app/src/main/res/layout/item_song_draggable.xml): Dòng bài hát hỗ trợ kéo thả sắp xếp thứ tự.

---

## 2. 🧱 Các View & Widget Cơ Bản Đã Sử Dụng

### Cấu trúc Bố cục (Layouts & ViewGroups)
*   **`SwipeRefreshLayout`**: Sử dụng ở Trang chủ để hỗ trợ thao tác vuốt từ trên xuống để làm mới (refresh) dữ liệu.
*   **`NestedScrollView`**: Dùng làm vùng chứa cuộn ở Trang chủ và Full Player để cho phép cuộn mượt mà các thành phần có kích thước linh hoạt mà không gây xung đột cuộn.
*   **`DrawerLayout`**: ViewGroup ngoài cùng của Activity chính.
*   **`ConstraintLayout`**: Bố cục ràng buộc linh hoạt dùng cho các khu vực điều khiển phức tạp (Header của Full Player, Mini Player, Dòng bài hát kéo thả).
*   **`LinearLayout` & `RelativeLayout`**: Sử dụng phân chia các khối dọc/ngang đơn giản hoặc đè các lớp View chồng lên nhau.
*   **`FrameLayout`**: Dùng làm Container chứa Fragment hoặc bọc các button có hiệu ứng xung động (pulse effect).

### Thành phần Giao diện & Danh sách (Widgets)
*   **`RecyclerView`**: Được sử dụng thay thế hoàn toàn cho `ListView` truyền thống để hiển thị danh sách bài hát, nghệ sĩ, album, bình luận và hàng đợi phát nhạc hiệu năng cao.
*   **`TabLayout`**: Thanh chuyển tab tại Trang chủ ("Mới nhất", "Gợi ý", "Nhạc tủ") và tại hàng đợi Full Player ("Tiếp theo", "Gợi ý").
*   **`MaterialCardView` & `CardView`**: Tạo khung bo góc tròn cực kỳ thẩm mỹ cho ảnh bìa album, các item và dialog.
*   **`ShapeableImageView`**: ImageView chuyên dụng bo góc tròn (Rounded) hoặc tròn hẳn (Circle) mà không cần tạo drawable background.
*   **`SeekBar`**: Thanh trượt tiến trình phát nhạc và thanh trượt chọn thời gian hẹn giờ tắt nhạc.
*   **`FloatingActionButton`**: Nút Play/Pause chính cỡ lớn ở Full Player.
*   **`ImageButton` & `ImageView`**: Nút bấm icon điều khiển (Shuffle, Repeat, Next, Prev, Back, Favorite, v.v.).
*   **`ProgressBar`**: Vòng quay tải dữ liệu (loading indicator) khi đang gọi API.
*   **`TextView` & `EditText`**: Hiển thị nhãn, thông tin bài hát và hộp nhập bình luận.

---

## 3. 🎨 Hệ Thống Màu Sắc & Hiệu Ứng Trực Quan (Colors & Styles)

### Mã màu sắc chủ đạo ([colors.xml](file:///e:/tam/Android/mobile/app/src/main/res/values/colors.xml))
*   `primary` (`#8b5cf6`), `primaryDark` (`#7c3aed`), `primaryLight` (`#a78bfa`): Tông màu tím Indigo trẻ trung, hiện đại.
*   `secondary` / `vibrant_pink` (`#ec4899`): Màu hồng dùng làm điểm nhấn hoặc icon yêu thích.
*   `accent` (`#10b981`) & `green_500` (`#22c55e`): Tông xanh lá cây dùng cho thanh Refresh hoặc trạng thái hoạt động.
*   `background` (`#0a0a0a`) & `backgroundSecondary` (`#121212`): Tông màu đen gần như tuyệt đối (AMOLED Dark Mode) mang lại cảm giác cao cấp.
*   `surface` (`#1e1e1e`) & `surfaceVariant` (`#2a2a2a`): Tông màu xám tối làm nền cho các Card hoặc Dialog.
*   `glass_white` (`#20FFFFFF`) & `glass_white_heavy` (`#40FFFFFF`): Màu trắng mờ translucent tạo hiệu ứng kính.

### Hiệu ứng nền & Phong cách Thiết kế (Drawables)
*   **Mesh Gradient nền đắm chìm (Immersive Palette)**: Ở [FullPlayerActivity.java](file:///e:/tam/Android/mobile/app/src/main/java/com/appad/activities/FullPlayerActivity.java), khi tải bài hát, app sẽ trích xuất 4 màu sắc chủ đạo của ảnh bìa album thông qua **Palette API**, sau đó tự động tạo một gradient xéo `GradientDrawable` làm nền cho màn hình phát nhạc. Hình nền sẽ chuyển động màu sắc theo từng bài hát đang phát.
*   **Hiệu ứng Kính mờ (Glassmorphism)**: Sử dụng các drawable như [bg_glass_card.xml](file:///e:/tam/Android/mobile/app/src/main/res/drawable/bg_glass_card.xml) (`#0DFFFFFF` solid + `#1AFFFFFF` stroke) để tạo hiệu ứng mờ đục trong suốt thời thượng cho thanh Action Bar của Full Player và Header của Trang chủ.
*   **Gradient Giao diện Trang chủ**: Nền phía trên của Trang chủ sử dụng gradient nằm ngang cực kỳ rực rỡ từ Indigo -> Purple -> Fuchsia ([home_top_gradient.xml](file:///e:/tam/Android/mobile/app/src/main/res/drawable/home_top_gradient.xml)) kết hợp với một lớp mờ tối đè lên ([glass_overlay.xml](file:///e:/tam/Android/mobile/app/src/main/res/drawable/glass_overlay.xml)).
*   **Bo góc đồng bộ (Rounded Corners)**: Toàn bộ thẻ card và ảnh bìa được bo góc nhất quán theo các style được định nghĩa sẵn trong [styles.xml](file:///e:/tam/Android/mobile/app/src/main/res/values/styles.xml) như: `RoundedImage` (8dp), `RoundedImageLarge` (20dp), `RoundedCornerImage24` (24dp), `CircleImage` (50% - Tròn tuyệt đối).

---

## 4. 🎬 Hiệu Ứng Chuyển Động (Animations)

*   **Hiệu ứng Nhịp đập (Pulse/Heartbeat Animation) của nút Play**: Khi nhạc đang phát, một `ObjectAnimator` liên tục co giãn nhẹ nút Play/Pause (`btnPlayPause`) scaleX/scaleY từ 1.0f sang 1.1f rồi ngược lại tạo hiệu ứng nhịp đập theo nhạc.
*   **Hiệu ứng Quầng sáng tỏa lan (Halo Effect)**: Phía sau nút Play/Pause là một View mờ hình tròn `viewPulseEffect`. Khi đang phát nhạc, quầng sáng này sẽ scale từ 1.0f lên 2.2f đồng thời giảm độ mờ `alpha` từ 0.5f về 0.0f liên tục, tạo hiệu ứng phát sáng lan tỏa cực đẹp mắt.
*   **Hiệu ứng bay lơ lửng khi Hover (Float Hover)**: Ở [CardAdapter.java](file:///e:/tam/Android/mobile/app/src/main/java/com/appad/adapters/CardAdapter.java), khi di chuột (hover) vào card bài hát, card sẽ kích hoạt animation `@anim/hover_float` để nhấc nhẹ lên trên và phóng to nhẹ `1.03f`, tạo hiệu ứng phản hồi xúc giác cao cấp.
*   **Hiệu ứng chuyển đổi trạng thái Mini Player**: Hỗ trợ phóng to Mini Player (Expanded) sang thu nhỏ thành một chấm tròn nhỏ nằm góc dưới bên phải màn hình (Collapsed) cực kỳ gọn gàng.

---

## 5. ⚙️ Logic Giao Diện Nâng Cao trong Adapter & Fragment

### Kéo thả sắp xếp danh sách (Drag & Drop)
*   Được triển khai trên danh sách bài hát Trang chủ ([HomeFragment.java](file:///e:/tam/Android/mobile/app/src/main/java/com/appad/fragments/HomeFragment.java)) và danh sách Playlist ([DraggableSongAdapter.java](file:///e:/tam/Android/mobile/app/src/main/java/com/appad/adapters/DraggableSongAdapter.java)).
*   Sử dụng `ItemTouchHelper` để lắng nghe thao tác kéo thả dọc.
*   Trong lúc kéo, item sẽ đổi độ mờ `alpha = 0.8f` và phóng to `1.02f` để làm nổi bật.
*   Khi thả ra, thứ tự danh sách bài hát đang phát (`MusicPlayerManager`) sẽ được tự động đồng bộ lại ngay lập tức.

### Chỉ báo bài hát đang phát (Active Item Highlighting)
*   Trong cả [CardAdapter.java](file:///e:/tam/Android/mobile/app/src/main/java/com/appad/adapters/CardAdapter.java) và [DraggableSongAdapter.java](file:///e:/tam/Android/mobile/app/src/main/java/com/appad/adapters/DraggableSongAdapter.java), mỗi khi danh sách cập nhật, adapter sẽ liên hệ với `MusicPlayerManager` để xem bài hát nào đang phát.
*   Nếu đúng là bài hát đang phát, adapter sẽ đổi viền card thành màu tím (`#8B5CF6`) đậm lên `4px` và hiển thị một lớp mờ phủ `playingOverlay` cùng icon Play chỉ báo đang chạy.

### Cuộn tự động mượt mà (Horizontal Auto Scroll)
*   Trang chủ hỗ trợ tự động cuộn (Auto Scroll) các danh sách chiều ngang (Trending, Albums, Artists) sau mỗi khoảng thời gian nhất định (4-6 giây).
*   Sử dụng `LinearSmoothScroller` tùy chỉnh tốc độ cuộn chậm lại khi quay về đầu danh sách, mang lại hiệu ứng mượt mà, không bị giật hay cuộn quá nhanh.

### Phân trang vô hạn & Tự động nối danh sách (Infinite Scrolling & Auto-Pagination)
*   `HomeFragment` lắng nghe sự kiện cuộn của `NestedScrollView`. Khi cuộn đến đáy, nó sẽ tự động nạp thêm bài hát từ API.
*   Thiết lập sự kiện lắng nghe khi hết nhạc (`OnPlaylistEndListener`). Khi danh sách hiện tại phát hết, ứng dụng sẽ tự động tải tiếp trang dữ liệu tiếp theo để nhạc không bao giờ dừng lại.
