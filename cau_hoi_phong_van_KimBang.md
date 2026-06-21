# 📋 CÂU HỎI BẢO VỆ ĐỒ ÁN — Phần của Nguyễn Sỹ Kim Bằng

> Tổng hợp đầy đủ 38 câu hỏi kỹ thuật liên quan đến các phần mà **Nguyễn Sỹ Kim Bằng** đã phụ trách:
> 1. Trang chủ (Home)
> 2. MiniPlayer & Full Player
> 3. Music Service & Music Player Manager
> 4. Activity chính & Điều hướng
> 5. Backend — Song & Genre API

---

## 🎵 NHÓM 1: MUSIC SERVICE & PHÁT NHẠC NỀN

### Câu 1: Bạn làm sao để khi tắt màn hình mà nhạc vẫn phát? Sử dụng cái gì, cơ chế như thế nào?

**Trả lời:**
- Sử dụng **Android Foreground Service** (`MusicService extends Service`), khi nhạc đang phát sẽ gọi `startForeground(1, notification)` để chuyển Service sang trạng thái foreground, hệ thống sẽ không kill service.
- Bên trong `initExoPlayer()`, cấu hình `ExoPlayer.Builder` với `.setWakeMode(C.WAKE_MODE_LOCAL)` để giữ **WakeLock partial**, ngăn CPU bị sleep khi phát nhạc.
- Ngoài ra còn sử dụng `AudioAttributes` với `USAGE_MEDIA` và `AUDIO_CONTENT_TYPE_MUSIC` để hệ thống ưu tiên audio focus cho ứng dụng.
- Khi stream nhạc qua mạng, gọi `acquireWifiLock()` để giữ kết nối WiFi không bị ngắt khi màn hình tắt.

---

### Câu 2: Khi người dùng vuốt tắt ứng dụng từ Recent Apps, nhạc có dừng không? Xử lý thế nào?

**Trả lời:**
- Có, nhạc sẽ dừng lại ngay lập tức. Xử lý bằng cách override phương thức `onTaskRemoved(Intent rootIntent)` trong `MusicService`:
  1. **Dừng Player và giải phóng tài nguyên**: Gọi `exoPlayer.stop()` và `exoPlayer.release()` ngay lập tức. Ta bỏ qua việc gọi API async (như ghi lịch sử) ở đây để đảm bảo dọn dẹp xong trước khi hệ thống kill process.
  2. **Giải phóng các loại Lock**: WakeLock và WifiLock.
  3. **Xóa notification**: Sử dụng `stopForeground(STOP_FOREGROUND_REMOVE)` (đối với Android 13+) hoặc `stopForeground(true)`.
  4. **Kết thúc Service**: Gọi `stopSelf()`.
- Ngoài ra `onStartCommand` trả về `START_NOT_STICKY`, nghĩa là service sẽ không tự restart nếu bị hệ thống kill.

---

### Câu 3: ExoPlayer là gì? Tại sao bạn chọn ExoPlayer thay vì MediaPlayer mặc định của Android?

**Trả lời:**
- **ExoPlayer** là thư viện phát media mã nguồn mở của Google, được thiết kế để thay thế `MediaPlayer` mặc định.
- Lý do chọn ExoPlayer:
  - Hỗ trợ **streaming trực tuyến** tốt hơn (adaptive streaming, buffering linh hoạt).
  - Có thể **tùy chỉnh sâu** hơn (custom render, custom data source).
  - API hiện đại hơn với **Listener pattern** (`Player.Listener`) để xử lý state changes.
  - Hỗ trợ tích hợp tốt với `WakeMode`, `AudioAttributes` một cách tự nhiên.

---

### Câu 4: WakeLock và WifiLock là gì? Tại sao cần cả hai?

**Trả lời:**
- **WakeLock (`PARTIAL_WAKE_LOCK`)**: Giữ CPU hoạt động khi màn hình tắt, đảm bảo nhạc tiếp tục xử lý.
- **WifiLock (`WIFI_MODE_FULL_HIGH_PERF`)**: Giữ kết nối WiFi ổn định khi streaming nhạc qua mạng.
- Cần cả hai vì: WakeLock chỉ giữ CPU, WifiLock chỉ giữ WiFi. Phải có cả hai thì nhạc online mới không bị ngắt khi khóa màn hình.

---

### Câu 5: Notification nhạc hoạt động như thế nào? Có nút Previous/Play/Pause/Next không?

**Trả lời:**
- Sử dụng `NotificationCompat.Builder` với `MediaStyle` để hiển thị notification kiểu media player.
- Có đầy đủ các action button: Previous, Play/Pause, Next.
- Mỗi action sử dụng `PendingIntent.getService()` gửi intent với action tương ứng về `MusicService.onStartCommand()` để điều khiển nhạc.

---

### Câu 6: `onStartCommand` trả về `START_NOT_STICKY` có ý nghĩa gì?

**Trả lời:**
- Có nghĩa là nếu hệ thống kill Service (do thiếu RAM), Service sẽ **không tự khởi động lại**. Điều này tránh việc ứng dụng tự chạy ngầm khi không cần thiết, gây tốn pin và lỗi logic.

---

## 🎛️ NHÓM 2: MUSIC PLAYER MANAGER (Singleton Pattern)

### Câu 7: Tại sao sử dụng Singleton Pattern cho MusicPlayerManager? Cơ chế hoạt động ra sao?

**Trả lời:**
- Vì cần **duy nhất một trạng thái phát nhạc** xuyên suốt toàn bộ ứng dụng. 
- Mọi Activity/Fragment đều tham chiếu đến cùng một instance → đảm bảo đồng bộ: cùng một bài hát, cùng một playlist và trạng thái play/pause ở mọi nơi.

---

### Câu 8: Giải thích cơ chế Shuffle và Repeat trong ứng dụng?

**Trả lời:**
- **Shuffle**: Trộn ngẫu nhiên playlist hiện tại bằng `Collections.shuffle()`, nhưng giữ bài đang phát ở đầu.
- **Repeat**: Xoay vòng qua 3 chế độ (Off -> Repeat All -> Repeat One).

---

### Câu 9: Cơ chế chuyển bài tự động (Auto-Next) hoạt động như thế nào?

**Trả lời:**
- Kết hợp 2 cách:
  1. Lắng nghe sự kiện `STATE_ENDED` từ ExoPlayer.
  2. Dùng một `Handler` kiểm tra mỗi 500ms, nếu còn 1.5s cuối bài thì chủ động chuyển bài tiếp theo để tạo cảm giác mượt mà (seamless).

---

### Câu 10: Giải thích cơ chế Sleep Timer và "Tắt sau bài hiện tại"?

**Trả lời:**
- **Sleep Timer**: Sử dụng `Handler.postDelayed()` để pause nhạc sau một khoảng thời gian chọn trước.
- **Tắt sau bài hiện tại**: Bật cờ (flag), khi bài hát kết thúc sẽ gọi lệnh pause thay vì chuyển bài tiếp theo.

---

### Câu 11: Inactivity Timer là gì? Xử lý như thế nào?

**Trả lời:**
- Tự động pause nhạc nếu không có tương tác sau 30 phút. App sẽ hiện một Modal "Bạn còn nghe không?" để user xác nhận. Giúp tránh trường hợp user ngủ quên mà nhạc vẫn phát gây tốn pin/data.

---

### Câu 12: Cơ chế ghi lại lịch sử nghe (`recordListen`) hoạt động ra sao?

**Trả lời:**
- API được gọi 2 lần: 
  - Lần 1: Ngay khi phát để tăng View count.
  - Lần 2: Khi dừng/chuyển bài để ghi nhận thời gian nghe thực tế và xem bài hát đã "hoàn thành" (nghe trên 90%) hay chưa.

---

### Câu 13: Cơ chế kiểm tra quyền truy cập bài hát Premium (`hasAccess`) như thế nào?

**Trả lời:**
- Dựa trên các flag từ Backend trả về (isPremium, bought, artistMember...). Nếu user không có quyền, Manager sẽ tự động skip bài đó khi phát danh sách.

---

## 🏠 NHÓM 3: TRANG CHỦ (HOME FRAGMENT)

### Câu 14: Trang chủ hiển thị những gì? Dữ liệu được load như thế nào?

**Trả lời:**
- Hiển thị 4 section: Trending, Albums, Artists, và Main List (tabbed). Dữ liệu được load song song qua Retrofit từ nhiều endpoint.

---

### Câu 15: Auto Scroll trên trang chủ hoạt động thế nào?

**Trả lời:**
- Sử dụng `Handler + Runnable` để tự cuộn các RecyclerView ngang. Tự động chạy khi user đang ở màn hình Home và dừng lại khi user chuyển sang màn hình khác.

---

### Câu 16: Drag & Drop trên danh sách bài hát Main List hoạt động ra sao?

**Trả lời:**
- Sử dụng `ItemTouchHelper`. Khi người dùng kéo thả, thứ tự trong Adapter thay đổi và được đồng bộ ngay lập tức vào playlist của `MusicPlayerManager`.

---

### Câu 17: Pagination (phân trang) được xử lý như thế nào?

**Trả lời:**
- Lắng nghe sự kiện cuộn của `NestedScrollView`. Khi chạm đáy, app gọi API với `offset` mới để lấy thêm 10 bài tiếp theo.

---

## 🎧 NHÓM 4: FULL PLAYER & MINI PLAYER

### Câu 18: Full Player có những tính năng gì? Mô tả tổng quan.

**Trả lời:**
- Phát nhạc, lời bài hát (lyrics), bình luận, danh sách chờ (queue), hẹn giờ, tải nhạc, đổi màu nền động theo ảnh bìa (Palette API).

---

### Câu 19: Palette API là gì? Sử dụng nó như thế nào trong Full Player?

**Trả lời:**
- Thư viện phân tích ảnh bìa để lấy ra các màu chủ đạo. Dùng các màu này tạo Gradient nền giúp giao diện biến đổi linh hoạt theo từng bài hát.

---

### Câu 20: Animation pulse và halo effect trong Full Player hoạt động thế nào?

**Trả lời:**
- Dùng `ObjectAnimator` tạo hiệu ứng nhịp đập cho nút điều khiển và sóng lan tỏa xung quanh ảnh đĩa nhạc khi nhạc đang phát.

---

### Câu 21: MiniPlayer đồng bộ trạng thái với Full Player như thế nào?

**Trả lời:**
- Cả hai đều lắng nghe cùng một `MusicPlayerManager`. Khi trạng thái phát thay đổi ở bất kỳ đâu, cả hai UI đều được cập nhật qua cùng một callback.

---

### Câu 22: Sự khác nhau giữa MiniPlayerHelper và FullPlayerActivity?

**Trả lời:**
- MiniPlayerHelper là một tiện ích quản lý phần giao diện thu gọn nằm ở MainActivity. FullPlayerActivity là một màn hình riêng biệt với đầy đủ chức năng và animation phức tạp hơn.

---

## 🧭 NHÓM 5: MAIN ACTIVITY & ĐIỀU HƯỚNG

### Câu 23: MainActivity quản lý Fragment như thế nào?

**Trả lời:**
- Dùng cơ chế **Add/Show/Hide** để tránh việc phải load lại dữ liệu mỗi khi người dùng chuyển tab giữa Home, Search, Library.

---

### Câu 24: MusicApplication class làm gì? Tại sao cần Application class riêng?

**Trả lời:**
- Khởi tạo `MusicPlayerManager` ngay khi app vừa bật lên và tạo Notification Channel cho Android 8+. Đây là nơi đặt các cấu hình global cho toàn ứng dụng.

---

### Câu 25: Bạn xin quyền POST_NOTIFICATIONS ở đâu và tại sao?

**Trả lời:**
- Xin quyền runtime ở `MainActivity` cho các máy Android 13+. Nếu không xin quyền này, notification điều khiển nhạc sẽ không thể hiển thị.

---

## 🔧 NHÓM 6: BACKEND — SONG & GENRE API

### Câu 26: API Bài hát có những endpoint nào?

**Trả lời:**
- Latest, Trending, Recommended, Search, Detail, Play (ghi lịch sử).

---

### Câu 27: `populateAccessInfo` trong SongService làm gì? Tại sao tính ở Backend?

**Trả lời:**
- Kiểm tra quyền truy cập (đã mua, premium...). Tính ở backend để đảm bảo bảo mật và giúp Mobile xử lý nhanh hơn chỉ với các flag true/false.

---

### Câu 28: API ghi nhận lượt nghe xử lý gì ở Backend?

**Trả lời:**
- Tăng view count, ghi vào bảng `ListeningHistory` thời gian nghe và trạng thái hoàn thành bài hát của User.

---

### Câu 29: GenreController dùng để làm gì?

**Trả lời:**
- Quản lý danh mục thể loại nhạc. Mobile dùng API này để lọc bài hát theo thể loại mà người dùng chọn.

---

## 🔗 NHÓM 7: KIẾN TRÚC & PATTERN

### Câu 30: Mô tả kiến trúc tổng thể phần bạn phụ trách?

**Trả lời:**
- Mobile: UI -> Manager (Singleton) -> Service (Foreground + ExoPlayer). 
- Backend: Controller -> Service -> Repository (JPA).

---

### Câu 31: Bạn sử dụng Observer Pattern ở đâu?

**Trả lời:**
- Trong hệ thống Listener của `MusicPlayerManager`. Khi Service có biến động, Manager sẽ "thông báo" cho tất cả UI (Home, Player, MiniPlayer) cùng cập nhật.

---

### Câu 32: Retrofit được sử dụng như thế nào?

**Trả lời:**
- Dùng `RetrofitClient` để tạo instance duy nhất, định nghĩa interface API và thực hiện gọi hàm (async) để trao đổi dữ liệu JSON với Backend.

---

### Câu 33: ServiceConnection và Binder Pattern là gì?

**Trả lời:**
- Là cơ chế để Activity/Manager kết nối và điều khiển trực tiếp các hàm bên trong `MusicService`.

---

## 💡 NHÓM 8: CÂU HỎI TÌNH HUỐNG (ĐẶC BIỆT)

### Câu 34: Nếu người dùng đang nghe nhạc mà mất mạng, ứng dụng xử lý thế nào?

**Trả lời:**
- ExoPlayer sẽ rơi vào trạng thái Buffering, sau một thời gian không reconnect được sẽ báo lỗi qua Toast và dừng phát.

---

### Câu 35: Nếu 2 Activity cùng lúc đăng ký listener, có bị conflict không?

**Trả lời:**
- Không, vì Manager lưu listener trong một danh sách (`ArrayList`) và duyệt thông báo cho từng người một.

---

### Câu 36: Luồng đi từ lúc user bấm phát bài hát trên trang chủ đến khi nghe được nhạc?

**Trả lời:**
- Home -> Manager -> Service (Start & Bind) -> ExoPlayer (Prepare & Play) -> Update Notification & UI.

---

### Câu 37: Tại sao ứng dụng nhạc cần Foreground Service?

**Trả lời:**
- Để ngăn Android giết tiến trình nghe nhạc khi user tắt màn hình hoặc sang app khác.

---

### Câu 38: Tại sao khi vuốt tắt ứng dụng, có trường hợp nhạc đã tắt thành công nhưng lần sau mở lại app và phát nhạc thì vuốt app nhạc lại KHÔNG tắt? Cách xử lý?

**Trả lời:**
- **Nguyên nhân:** Do hệ thống tự động rebind Service ở trạng thái **Bound-only** sau khi ta gọi `stopSelf`. Trạng thái này không nhận được callback `onTaskRemoved`.
- **Giải pháp:** Gọi `context.startService(intent)` mỗi khi phát bài mới trong `playSong` để ép Service luôn ở trạng thái **Started**.

---

## 📱 NHÓM 9: CƠ CHẾ CHUYÊN SÂU ANDROID (PROCESS, LIFECYCLE STATE & BACKGROUND LIMITS)

### Câu 39: Android phân cấp tiến trình (Processes) như thế nào theo thứ tự ưu tiên? Khi bộ nhớ thấp, hệ thống dựa vào cơ chế nào để quyết định kill ứng dụng?

**Trả lời:**
Android chia các tiến trình thành các cấp độ ưu tiên khác nhau dựa trên mức độ tương tác của người dùng. Mức độ ưu tiên này được biểu diễn bằng chỉ số **OOM ADJ (Out of Memory Adjuster)** từ thấp đến cao (càng thấp càng an toàn, càng cao càng dễ bị kill khi thiếu RAM):

1. **Foreground Process (Tiến trình tiền cảnh - OOM ADJ: 0 đến 2)**:
   - Chứa Activity đang tương tác trực tiếp với người dùng (đã gọi `onResume`).
   - Chứa một **Foreground Service** đang hoạt động (ví dụ: `MusicService` đang phát nhạc và hiển thị notification).
   - Chứa BroadcastReceiver đang chạy `onReceive` hoặc Service đang thực thi vòng đời (`onCreate`, `onStart`, `onDestroy`).
2. **Visible Process (Tiến trình hiển thị - OOM ADJ: 2 đến 3)**:
   - Chứa Activity bị che khuất một phần (ví dụ: bị đè bởi một Dialog/Popup dạng Translucent) nhưng vẫn hiển thị trên màn hình (đang ở trạng thái `onStart` / `onPause`).
3. **Service Process (Tiến trình dịch vụ chạy ngầm - OOM ADJ: 5 đến 8)**:
   - Chứa một Service được khởi chạy bằng `startService` nhưng không chạy ở chế độ Foreground (ví dụ: tác vụ đồng bộ dữ liệu nhỏ gọn).
4. **Cached Process (Tiến trình lưu tạm - OOM ADJ: 9 đến 15)**:
   - Chứa các Activity đã dừng hẳn (`onStop`), người dùng đã nhấn nút Home để quay lại màn hình chính. 

**Cơ chế giải phóng RAM**: 
Khi RAM cạn kiệt, nhân Linux sẽ chạy trình **Low Memory Killer Daemon (LMKD)** để quét các tiến trình từ có OOM ADJ cao nhất (Cached Process) xuống thấp dần. Nhờ việc gọi `startForeground(1, notification)`, `MusicService` của ta nâng độ ưu tiên của tiến trình app từ *Cached* (OOM ADJ ~ 9-15) lên *Foreground* (OOM ADJ ~ 2), đảm bảo hệ thống không kill ứng dụng kể cả khi người dùng mở các ứng dụng nặng khác (game, mạng xã hội...).

---

### Câu 40: Có bao nhiêu trạng thái trong vòng đời (Lifecycle States) của một Activity và Fragment? Phân biệt sự khác nhau giữa "Trạng thái" (State) và "Sự kiện" (Event) trong Lifecycle Component.

**Trả lời:**
Theo thư viện `androidx.lifecycle`, vòng đời của một Activity hay Fragment có **5 Trạng thái (Lifecycle.State)** chính và **6 Sự kiện dịch chuyển (Lifecycle.Event)** tương ứng:

*   **5 Trạng thái (States - Trạng thái tĩnh)**:
    1.  `INITIALIZED`: Trạng thái ban đầu trước khi Activity được khởi tạo.
    2.  `CREATED`: Activity đã được tạo nhưng chưa hiển thị cho người dùng (sau `onCreate` và trước `onStart`, hoặc sau `onStop` trước `onDestroy`).
    3.  `STARTED`: Activity đã hiển thị một phần trên màn hình nhưng chưa có focus (sau `onStart` và trước `onPause`).
    4.  `RESUMED`: Activity nằm ở tiền cảnh (Foreground), hiển thị đầy đủ và có focus cao nhất để người dùng tương tác (sau `onResume`).
    5.  `DESTROYED`: Activity đã bị hủy hoàn toàn và được dọn dẹp khỏi bộ nhớ.

*   **Sự khác biệt giữa State và Event**:
    *   **Sự kiện (Event - Cạnh đồ thị)**: Là các điểm kích hoạt mà hệ điều hành gọi ra để chuyển đổi giữa các trạng thái (ví dụ: `ON_CREATE`, `ON_START`, `ON_RESUME`, `ON_PAUSE`, `ON_STOP`, `ON_DESTROY`).
    *   **Trạng thái (State - Nút đồ thị)**: Là trạng thái hiện hành của component tại một thời điểm. Ví dụ, sau khi sự kiện `ON_START` xảy ra, trạng thái chuyển từ `CREATED` sang `STARTED`.
    *   *Tình huống thực tế:* Khi một Dialog không thuộc Activity (ví dụ một Activity dạng Dialog của app khác) đè lên và che mất một phần giao diện app của bạn, Activity của bạn sẽ nhận sự kiện `ON_PAUSE` và rơi xuống trạng thái `STARTED` (vì nó vẫn hiển thị một phần nhưng mất tương tác trực tiếp).

---

### Câu 41: Foreground Service và Background Service khác nhau như thế nào? Từ Android 8.0 và đặc biệt là Android 14+, Google đã áp đặt các giới hạn chạy ngầm (Background Execution Limits) như thế nào?

**Trả lời:**

*   **Sự khác nhau cơ bản**:
    *   **Background Service**: Chạy hoàn toàn ẩn danh dưới nền, người dùng không nhận biết được. Độ ưu tiên rất thấp (OOM ADJ cao) nên dễ bị hệ thống kill.
    *   **Foreground Service**: Bắt buộc phải gắn liền với một Notification liên tục ở thanh trạng thái (không thể vuốt bỏ qua nếu service đang chạy). Hệ thống coi nó tương đương với việc người dùng đang tương tác nên cực kỳ khó bị kill.

*   **Giới hạn chạy ngầm qua các phiên bản Android**:
    *   **Từ Android 8.0 (API 26)**: Cấm khởi chạy Background Service khi ứng dụng đang ở dưới nền (Background). Nếu cố gọi `startService()` khi app ở background sẽ bị ném ra ngoại lệ `IllegalStateException`. Người dùng phải dùng `ContextCompat.startForegroundService()` và trong vòng 5 giây phải gọi `startForeground()` bên trong Service.
    *   **Từ Android 10 (API 29)**: Thêm yêu cầu khai báo quyền sử dụng các tính năng nhạy cảm chạy ngầm như Vị trí (`location`), Camera (`camera`), Microphone (`microphone`).
    *   **Từ Android 14 (API 34)**: Google bắt buộc nhà phát triển phải **khai báo loại (type) Foreground Service** cụ thể trong `AndroidManifest.xml` (thông qua thuộc tính `android:foregroundServiceType`). 
        *   Đối với ứng dụng nghe nhạc của chúng ta, ta phải khai báo loại là `mediaPlayback`:
            ```xml
            <service
                android:name=".service.MusicService"
                android:foregroundServiceType="mediaPlayback"
                android:exported="false" />
            ```
        *   Đồng thời phải xin quyền runtime tương ứng trong manifest: `<uses-permission android:name="android.permission.FOREGROUND_SERVICE_MEDIA_PLAYBACK" />`. Nếu khởi chạy Service mà không khai báo đúng loại hoặc thiếu quyền, ứng dụng sẽ crash ngay lập tức.

---

### Câu 42: Service trong Android có những trạng thái hoạt động nào? Giải thích sự khác biệt về vòng đời và cơ chế giải phóng giữa Started Service và Bound Service. Khi kết hợp cả hai (vừa Start vừa Bind) thì Service sẽ dừng lại khi nào?

**Trả lời:**
Service có hai trạng thái hoạt động độc lập hoặc có thể kết hợp với nhau:

1.  **Started Service (Chạy độc lập)**:
    *   Khởi chạy qua: `context.startService(Intent)`.
    *   Vòng đời bắt đầu bằng `onCreate() -> onStartCommand()`.
    *   **Cơ chế giải phóng**: Nó sẽ tiếp tục chạy vô hạn ngay cả khi Activity kích hoạt nó đã bị hủy. Chỉ dừng lại khi Service tự gọi `stopSelf()` hoặc client gọi `context.stopService()`.
2.  **Bound Service (Chạy liên kết)**:
    *   Khởi chạy qua: `context.bindService(Intent, ServiceConnection, flags)`.
    *   Vòng đời bắt đầu bằng `onCreate() -> onBind()`. Client nhận một `IBinder` để tương tác trực tiếp với Service qua các hàm public.
    *   **Cơ chế giải phóng**: Service tồn tại phụ thuộc vào vòng đời của Client gắn kết. Khi tất cả các Client hủy liên kết (`unbindService()`), Service sẽ tự động gọi `onUnbind() -> onDestroy()` để giải phóng tài nguyên.

*   **Khi kết hợp cả hai (Trường hợp ứng dụng nghe nhạc - vừa Start vừa Bind)**:
    *   *Tại sao cần kết hợp?* Ta cần `startService` để giữ cho nhạc tiếp tục phát khi người dùng thoát màn hình (Activity bị destroy nhưng Service vẫn sống). Đồng thời ta cần `bindService` khi người dùng mở màn hình Player để Activity có thể kết nối trực tiếp với Service và đồng bộ hóa tiến trình phát, lấy thông tin bài hát hiện tại.
    *   *Cơ chế dừng của Service kết hợp:* Khi kết hợp cả hai, hệ thống yêu cầu **phải thỏa mãn cả hai điều kiện dừng** thì Service mới bị hủy hoàn toàn:
        1. Tất cả các Client đã ngắt kết nối (gọi `unbindService()`).
        2. Có lệnh dừng rõ ràng được thực thi (Service gọi `stopSelf()` hoặc client gọi `stopService()`).
    *   Nếu chỉ unbind mà chưa gọi `stopSelf()`, Service vẫn tiếp tục chạy. Ngược lại, nếu gọi `stopSelf()` nhưng vẫn còn Client đang bind, Service vẫn sẽ sống cho đến khi Client đó unbind.

---

### Câu 43: Android quản lý tài nguyên và điện năng qua Doze Mode và App Standby như thế nào? Làm sao Foreground Service của bạn vượt qua được các chế độ này để phát nhạc liên tục khi màn hình tắt lâu?

**Trả lời:**
Hệ điều hành Android quản lý pin và tài nguyên thông qua hai cơ chế chính:

1.  **Doze Mode (Chế độ ngủ sâu - áp dụng toàn hệ thống)**:
    *   Khi thiết bị không cắm sạc, màn hình tắt và nằm yên không di chuyển trong một khoảng thời gian.
    *   Hệ thống sẽ hạn chế truy cập mạng, trì hoãn các tác vụ ngầm (JobScheduler, SyncAdapter) và chỉ mở một cửa sổ nhỏ để xử lý tác vụ (Maintenance Window) theo chu kỳ giãn cách xa dần.
2.  **App Standby (Chế độ chờ của từng app)**:
    *   Hệ thống dựa trên hành vi của người dùng để đưa ứng dụng vào các nhóm sử dụng (**App Standby Buckets**): *Active, Working Set, Frequent, Rare, Restricted*. Nhóm càng thấp thì tần suất được chạy ngầm và nhận thông báo đẩy càng ít.

*   **Cách ứng dụng nhạc của chúng ta vượt qua Doze Mode và App Standby**:
    *   **Vượt qua App Standby**: Khi ứng dụng của chúng ta khởi chạy một **Foreground Service** đang phát nhạc và hiển thị Notification có nút điều khiển, hệ thống sẽ tự động xếp ứng dụng vào nhóm **Active Bucket** (Nhóm có độ ưu tiên cao nhất, không bị hạn chế tài nguyên và network).
    *   **Vượt qua Doze Mode**:
        *   Foreground Service loại `mediaPlayback` được hệ thống đưa vào danh sách **miễn trừ (Exempted)** khỏi các hạn chế về kết nối mạng ngầm và giữ CPU hoạt động.
        *   Bằng việc cấu hình `exoPlayer.setWakeMode(C.WAKE_MODE_LOCAL)` (sử dụng **WakeLock** ngầm bảo vệ bởi ExoPlayer) và `WifiLock`, ta báo cho hệ điều hành biết CPU và Chip mạng cần duy trì nguồn điện tối thiểu để streaming nhạc liên tục, ngăn thiết bị rơi vào trạng thái ngủ sâu (Deep Sleep) gây ngắt nhạc.

---

> **Lưu ý:** Đây là bộ câu hỏi đầy đủ **43 câu** đã được cập nhật theo logic code mới nhất và bổ sung các kiến thức chuyên sâu về cơ chế hệ thống Android. Chúc bạn bảo vệ đồ án thành công! 🎉
