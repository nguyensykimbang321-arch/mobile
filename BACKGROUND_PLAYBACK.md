# HƯỚNG DẪN VỀ CƠ CHẾ PHÁT NHẠC NỀN (BACKGROUND PLAYBACK) TRÊN APPAD

Tài liệu này giải thích các giải pháp kỹ thuật đã được triển khai để đảm bảo ứng dụng có thể phát nhạc liên tục khi tắt màn hình, đặc biệt là khắc phục lỗi nhạc bị dừng sau một vài bài hát.

## 1. Vấn đề: Tại sao nhạc bị dừng khi tắt màn hình?
Trên các phiên bản Android hiện đại, hệ thống có cơ chế **Doze Mode** và **App Standby**. Khi màn hình tắt và điện thoại không cắm sạc:
- **CPU** sẽ đi vào trạng thái ngủ (Deep Sleep) để tiết kiệm pin.
- **Network (Wi-Fi/Data)** bị giới hạn hoặc ngắt hoàn toàn.
- Các tiến trình chạy ngầm (Background Tasks) bị tạm dừng.

Điều này khiến code chuyển bài hát không thể yêu cầu CPU xử lý hoặc không thể tải dữ liệu bài hát mới, dẫn đến việc nhạc bị đứng cho đến khi người dùng bật màn hình.

## 2. Các giải pháp đã triển khai

### A. WakeLock (Giữ CPU tỉnh táo)
Trong `MusicService.java`, chúng ta sử dụng `WakeMode` của ExoPlayer:
```java
exoPlayer = new ExoPlayer.Builder(this)
    .setWakeMode(com.google.android.exoplayer2.C.WAKE_MODE_LOCAL)
    .build();
```
- **Cách thức:** Khi nhạc đang phát, hệ thống sẽ giữ CPU ở trạng thái hoạt động nhẹ (Partial WakeLock). Khi nhạc dừng hoặc tắt, khóa này tự động được giải phóng để tiết kiệm pin.

### B. WifiLock (Giữ kết nối mạng)
Khi phát nhạc trực tuyến (Streaming), Wi-Fi có thể bị ngắt khi ngủ sâu. Chúng ta sử dụng `WifiLock`:
```java
wifiLock = wm.createWifiLock(WifiManager.WIFI_MODE_FULL_HIGH_PERF, "appad:MusicWifiLock");
wifiLock.acquire();
```
- **Cách thức:** Khóa này báo cho Android rằng ứng dụng cần Wi-Fi ở hiệu suất cao nhất để tải nhạc, ngăn hệ thống ngắt kết nối mạng.

### C. Transition WakeLock (Cầu nối giữa các bài hát)
Đây là phần quan trọng nhất để sửa lỗi "đứng nhạc khi hết bài":
```java
private void acquireTransitionWakeLock() {
    transitionWakeLock.acquire(10000); // 10 giây
}
```
- **Cách thức:** Khi một bài hát kết thúc (`STATE_ENDED`), ứng dụng sẽ thực hiện một "cú hích" tỉnh táo trong 10 giây. Khoảng thời gian này đủ để ứng dụng thực hiện các tác vụ:
    1. Ghi lại lịch sử nghe bài cũ.
    2. Tìm bài tiếp theo trong danh sách.
    3. Tải dữ liệu ban đầu của bài mới.
    4. Bắt đầu phát bài mới.
- Sau khi bài mới bắt đầu, khóa này sẽ tự động giải phóng hoặc được giải phóng sau khi hết 10s.

### D. AudioAttributes (Đăng ký với hệ thống)
```java
AudioAttributes audioAttributes = new AudioAttributes.Builder()
    .setUsage(C.USAGE_MEDIA)
    .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
    .build();
```
- **Cách thức:** Báo cho hệ thống Android rằng đây là ứng dụng nghe nhạc chuyên nghiệp. Giúp hệ thống ưu tiên phân phối tài nguyên và xử lý âm thanh tốt hơn.

## 3. Tổng kết quy trình hoạt động
1. **Bắt đầu nghe:** App xin quyền giữ CPU và Network hoạt động.
2. **Trong khi nghe:** Nhạc phát mượt mà vì CPU và Wi-Fi luôn sẵn sàng.
3. **Khi hết bài:** App kích hoạt "khóa bảo vệ 10 giây" để đảm bảo dù hệ thống đang ngủ say thì code chuyển bài vẫn chạy được.
4. **Khi sang bài mới:** App tiếp tục chu kỳ mới.

## 4. Lưu ý cho người dùng (Quan trọng)
Dù code đã tối ưu, một số hãng điện thoại (Samsung, Xiaomi, Oppo...) có trình quản lý pin cực kỳ nghiêm ngặt (Aggressive Battery Management). Để đạt hiệu quả 100%, người dùng nên:
- Vào **Cài đặt > Ứng dụng > Appad**.
- Chọn **Pin** hoặc **Tối ưu hóa pin**.
- Chọn **Không tối ưu hóa** (Unrestricted/Don't optimize).

---
*Tài liệu này được soạn thảo để hỗ trợ kỹ thuật cho dự án Appad.*
