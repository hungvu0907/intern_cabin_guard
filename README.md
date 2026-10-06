# CabinGuard — Sprint 2

Ứng dụng Android mô phỏng cảm biến cabin, ghi log bằng Room và cảnh báo theo ngưỡng DataStore. Module `cli` chạy thử engine trên JVM.

## Settings và màn hình

- Chọn **Chỉnh ngưỡng** để mở Settings. Nhiệt độ hợp lệ **30–60°C**, CO₂ **500–3000 ppm**; mặc định **38°C / 1000 ppm**. Chỉ cảnh báo khi giá trị đo lớn hơn ngưỡng.
- Ngưỡng áp dụng ngay khi lưu. Engine chờ DataStore trước khi phát mẫu đã đánh giá; đổi ngưỡng không tạo thêm log cho mẫu cũ.
- Điện thoại hỗ trợ dọc/ngang. Thiết bị có `FEATURE_AUTOMOTIVE` dùng landscape. Màn hình nhỏ có bố cục cuộn dọc.
- Tạm dừng chỉ giữ số đo trên UI; foreground service vẫn ghi log.
- Dashboard chỉ nạp **200 mẫu gần nhất**, tránh giữ toàn bộ lịch sử nhiều ngày trong RAM. Khi chưa có mẫu sẽ hiển thị trạng thái chờ; lỗi đọc dữ liệu có thông báo và retry sau 5 giây. Mẫu hiển thị có giờ đo để phân biệt dữ liệu cũ.
- Quay lại từ Settings bỏ bản nháp chưa lưu; xoay màn hình giữ bản nháp. Lỗi hiển thị sát trường nhập; Next chuyển sang CO₂, Done lưu khi có thay đổi. Lỗi cập nhật widget không làm báo sai kết quả lưu DataStore.
- Các ngưỡng cũ không hợp lệ trong dải mới được đọc thành ngưỡng mặc định. Dải phát số đo mô phỏng vẫn là 25–45°C / 400–1200 ppm, nên ngưỡng cao hơn dải này không kích hoạt cảnh báo mô phỏng.

## Sync giả lập và giữ dữ liệu

**Sync hiện là mock lưu bền trên thiết bị, không phải cloud hoặc backup ngoài thiết bị.** Database `mock_cloud.db` tách khỏi `cabin_guard.db`, upsert theo ID Room; dữ liệu tồn tại sau khi process đóng và gửi lại không tạo bản trùng. Xóa dữ liệu ứng dụng hoặc gỡ ứng dụng sẽ xóa cả hai database.

WorkManager chạy sync mỗi 15 phút khi có mạng; chỉ đánh dấu `isSynced` sau khi mock ghi thành công. Batch không tiến triển sẽ báo lỗi và retry. Migration Room 1→2 giữ log cũ và đặt `isSynced = 0`.

Database chính hiện dùng **version 3**: migration 2→3 thêm index `(timestamp, id)` để truy vấn lịch sử và xuất CSV theo batch. Mock dùng **version 2**, có migration index tương ứng 1→2. Cả hai giữ nguyên bản ghi, không dùng destructive migration. Foreground service báo trạng thái chờ và retry sau 5 giây nếu luồng đọc/ghi thất bại; cancellation khi service bị hủy được truyền nguyên trạng.

Cleanup định kỳ mỗi 24 giờ, khi pin không yếu:

- Xóa log đã sync cũ hơn 24 giờ.
- Xóa mọi log cũ hơn 7 ngày, **kể cả chưa sync**; số dòng chưa sync bị xóa được ghi vào logcat.
- Các mốc là điều kiện xóa khi worker chạy, không phải cam kết xóa đúng tại thời điểm hết hạn. WorkManager có thể trì hoãn tác vụ.

Mock giữ các bản ghi đã nhận để không làm mất dữ liệu sau cleanup; dung lượng mock có thể tăng theo thời gian. Đây là kho mô phỏng phục vụ assignment. Tích hợp remote thật bằng `TelemetryRemoteDataSource` và thay binding trong `RemoteModule` khi có backend.

## CSV và widget US-08

CSV có UTF-8 BOM, header cố định, số thập phân theo `Locale.US`, thời gian ISO có offset và tên file duy nhất. File nằm trong cache `exports/`, chia sẻ qua FileProvider. Chỉ dọn CSV cũ hơn 24 giờ khi xuất lần tiếp theo; cache vẫn có thể được hệ điều hành thu hồi.

CSV nạp tối đa **1000 dòng/batch**, theo `(timestamp, id)`, giới hạn bởi ID lớn nhất tại thời điểm bắt đầu nên mẫu mới không kéo dài lần xuất. Cleanup đồng thời có thể loại bỏ dòng chưa được đọc; đây không phải snapshot transaction của toàn bộ database. Khi bị hủy hoặc ghi lỗi, file dang dở được xóa. Share sheet chỉ mở khi Activity ở trạng thái resumed; không có ứng dụng nhận file hoặc lỗi quyền sẽ hiển thị thông báo.

UI dùng chung palette với theme và widget; cảnh báo/số đo đổi ngay, không chạy animation lặp lại. Nút dùng phản hồi chạm/focus của Material. Layout chuyển sang dạng cuộn khi chiều rộng/chiều cao không đủ hoặc font scale lớn hơn 1.3; Settings giới hạn chiều rộng 640dp và tránh bàn phím che nội dung.

Thêm widget **CabinGuard** từ launcher: hiển thị nhiệt độ, CO₂, cảnh báo và giờ đo; chạm để mở app. Khi service chạy, cập nhật số đo tối đa mỗi 30 giây và ngay khi đổi cảnh báo/ngưỡng. Khi không có service, widget hiển thị số đo cuối cùng với giờ đo; launcher yêu cầu cập nhật định kỳ khoảng 30 phút. Lỗi cập nhật widget không dừng ghi telemetry.

## Kiểm tra

Chạy bằng Gradle wrapper trên Windows:

```powershell
.\gradlew.bat :app:testDebugUnitTest :cli:test :app:assembleDebug :app:lintDebug :app:assembleDebugAndroidTest
.\gradlew.bat :app:connectedDebugAndroidTest
```

Unit test kiểm tra biên ngưỡng, parsing, engine với thời gian giả lập, sync batch/retry/cancellation, Settings lưu thành công/thất bại, CSV và throttle widget. Instrumentation test kiểm tra migration thật, cleanup, mock mở lại database, không ghi mẫu trùng và truy cập Settings/export trên layout nhỏ dọc/ngang.

`connectedDebugAndroidTest` cần thiết bị hoặc emulator. Kiểm tra trực quan bổ sung trên thiết bị: xoay Settings khi đang nhập; lưu và khởi động lại process; thêm/resize widget; đổi ngưỡng để kiểm tra màu cảnh báo; xuất hai CSV liên tiếp và đọc URI của file đầu.
