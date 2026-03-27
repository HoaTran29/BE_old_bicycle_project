# Report Status State Machine And Sanction Separation: giải thích cho người mới học

## 1. Bối cảnh

Trước bản sửa ngày `2026-03-27`, report flow của backend chỉ có 3 trạng thái:

- `pending`
- `reviewed`
- `resolved`

Vấn đề là:

- `reviewed` bị dùng nửa mở nửa đóng, nghĩa là admin đã xem nhưng case vẫn chưa thật sự kết thúc
- `resolved` vừa mang nghĩa "đã xử lý xong" vừa mang nghĩa "đã phạt"
- hệ thống không có trạng thái "đóng case nhưng không phạt"

Điều này làm moderation flow bị mơ hồ và dễ kẹt ở bước xử lý report.

## 2. Mục tiêu của bản sửa

Bản sửa này tách rõ 3 khái niệm:

- report đang mở
- report đã đóng
- có áp dụng chế tài hay không

Nói ngắn gọn: "đóng case" và "trừng phạt" không còn bị trộn làm một.

## 3. State machine mới

Report bây giờ có 4 trạng thái:

- `pending`
- `investigating`
- `resolved_upheld`
- `resolved_dismissed`

Ý nghĩa:

- `pending`: vừa được gửi lên, chưa có thao tác moderation nào
- `investigating`: admin đã nhận case và đang xem xét thêm
- `resolved_upheld`: admin kết luận report đúng và chấp nhận vi phạm
- `resolved_dismissed`: admin bác bỏ report và đóng case mà không phạt

## 4. Luật chuyển trạng thái

Backend chỉ cho phép các đường đi sau:

- `pending -> investigating`
- `pending -> resolved_upheld`
- `pending -> resolved_dismissed`
- `investigating -> resolved_upheld`
- `investigating -> resolved_dismissed`

Không cho phép:

- chuyển từ trạng thái đã đóng quay lại trạng thái mở
- giữ nguyên trạng thái rồi gọi process lại
- nhảy từ `investigating` về `pending`

Nếu gọi sai flow, service ném `INVALID_REPORT_STATUS_TRANSITION`.

## 5. Vì sao phải tách sanction khỏi status đóng case

Ở flow cũ, `resolved` đồng nghĩa với việc:

- report vào user => user bị `banned`
- report vào product => product bị `hidden`

Điều đó quá cứng. Thực tế moderation luôn có case:

- chưa đủ bằng chứng
- hiểu nhầm
- case không hợp lệ

Với flow mới:

- `resolved_upheld` mới là nhánh áp dụng chế tài
- `resolved_dismissed` chỉ đóng case, không động tới user hoặc product

Đây là lý do tên file note có chữ `sanction separation`.

## 6. Duplicate report được xử lý thế nào

Backend vẫn chặn report trùng cho cùng `reporter + target`, nhưng chỉ khi case cũ còn đang mở:

- `pending`
- `investigating`

Khi report cũ đã vào:

- `resolved_upheld`
- `resolved_dismissed`

thì reporter có thể tạo report mới nếu có tình tiết hoặc bằng chứng mới.

Đây là điểm rất quan trọng vì nó tránh việc case bị "kẹt vĩnh viễn" chỉ vì admin đã xem qua một lần.

## 7. Các file chính

- `src/main/java/com/backend/old_bicycle_project/entity/enums/ReportStatus.java`
- `src/main/java/com/backend/old_bicycle_project/service/impl/ReportServiceImpl.java`
- `src/main/java/com/backend/old_bicycle_project/exception/ErrorCode.java`
- `src/main/resources/db/migration/V22__report_status_state_machine_hardening.sql`
- `src/test/java/com/backend/old_bicycle_project/service/impl/ReportServiceImplTest.java`

## 8. Migration làm gì

Migration `V22` làm 3 việc:

1. đổi enum value `reviewed` thành `investigating`
2. đổi enum value `resolved` thành `resolved_upheld`
3. thêm enum value mới `resolved_dismissed`

Làm như vậy giúp dữ liệu cũ đi tiếp được mà không cần tạo lại toàn bộ bảng `reports`.

## 9. Chốt ngắn

Bản sửa này làm report flow của backend rõ nghĩa hơn:

- trạng thái mở và đóng được tách bạch
- admin có thể đóng case mà không buộc phải phạt
- chế tài chỉ chạy khi report được xác nhận là đúng
- duplicate report chỉ bị chặn trong khi case cũ còn thật sự mở

Đây là nền tảng tối thiểu để moderation flow không còn kẹt ở giữa đường.
