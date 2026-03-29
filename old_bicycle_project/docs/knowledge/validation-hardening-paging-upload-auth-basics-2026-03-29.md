# Validation Hardening Paging Upload Auth Basics (2026-03-29)

## Bối cảnh
- Trước thay đổi này, nhiều chỗ trong backend đang validate theo kiểu rải rác.
- Một số flow chỉ kiểm tra `contentType` do client gửi lên, nên file giả dạng ảnh vẫn có thể lọt vào storage.
- Nhiều API phân trang gọi `PageRequest.of(page, size)` trực tiếp, nên nếu FE gửi size quá lớn hoặc page âm thì backend không có một rule chung rõ ràng.
- Auth cũng chưa đồng nhất hoàn toàn ở chỗ normalize email và token đầu vào.

## Mục tiêu của đợt hardening này
- Dùng rule chung cho các API phân trang.
- Dùng rule chung cho multipart upload.
- Chặn file giả dạng ảnh trước khi upload lên storage.
- Chỉ cho phép báo cáo kiểm định ở dạng PDF hợp lệ.
- Chuẩn hóa thêm đầu vào auth để giảm lỗi khó đoán.

## Những gì đã thay đổi

### 1. Pagination dùng chung một validator
- Tạo `PaginationValidationUtils`.
- Rule đang dùng là:
  - `page >= 0`
  - `size` phải nằm trong khoảng `1..100`
- Các chỗ như notifications, reviews, reports, chat, product admin list, refund admin list, payout admin list, inspection history/request đều dùng chung helper này.

Ý nghĩa:
- Không còn tình trạng mỗi controller/service tự xử lý phân trang một kiểu.
- Khi cần đổi policy, chỉ sửa một chỗ.

### 2. Multipart upload dùng chung một validator
- Tạo `MultipartFileValidationUtils`.
- Helper này làm 3 việc chính:
  - loại bỏ file null/rỗng khỏi danh sách
  - kiểm tra ảnh upload có thật sự là ảnh hợp lệ
  - kiểm tra report kiểm định có đúng PDF hợp lệ hay không

Ảnh không chỉ nhìn vào `contentType`.
- Backend còn đọc nội dung file để chắc đó là ảnh thật.

PDF report cũng không chỉ nhìn đuôi file.
- Backend kiểm tra header `%PDF-` trước khi chấp nhận.

### 3. Product image được siết chặt hơn
- Flow tạo/cập nhật product giờ không chỉ kiểm tra “ít nhất 3 ảnh”.
- Nó còn kiểm tra 3 ảnh đó phải là ảnh hợp lệ.
- Nếu có file giả dạng ảnh, backend trả `PRODUCT_IMAGE_INVALID`.

Ý nghĩa:
- Tránh đẩy file rác hoặc file độc hại vào bucket public của product images.

### 4. Refund / report / order evidence dùng cùng rule ảnh
- Các flow evidence trước đây mỗi service tự kiểm tra `image/*`.
- Giờ các flow này dùng chung validator.

Lợi ích:
- Dễ bảo trì hơn.
- Giảm bug kiểu “chỗ này chặn, chỗ kia quên chặn”.

### 5. Inspection report là PDF-only
- `uploadInspectionReport(...)` giờ validate PDF thật trước khi upload.
- Nếu người dùng gửi file text hoặc file giả PDF thì request bị chặn sớm.

Ý nghĩa:
- Phù hợp hơn với vai trò “report tài liệu kiểm định”.
- Giảm khả năng bucket inspection chứa file linh tinh.

### 6. StorageService an toàn hơn
- `StorageService` giờ:
  - từ chối file null/rỗng
  - sanitize tên file
  - normalize folder path
  - chặn path có `..`
  - fallback `contentType` về `application/octet-stream` nếu client gửi sai

Ý nghĩa:
- Giảm rủi ro path traversal và tên file bẩn.
- Backend bớt phụ thuộc vào dữ liệu thô từ client.

### 7. Auth input đồng nhất hơn
- Login giờ normalize email giống register/reset:
  - trim
  - lowercase
- `verifyEmail(...)` chặn token rỗng từ sớm.
- `updateProfile` và `updateProduct` đã có `@Valid`.
- Một số DTO auth/product được thêm `@Size` / `@Positive` để trả lỗi sớm hơn.

Ý nghĩa:
- Giảm lỗi kiểu “email tồn tại nhưng login không được vì khác hoa thường”.
- Giúp API fail sớm, dễ đoán hơn.

### 8. Global exception mapping đúng semantics hơn
- Exception không mong muốn giờ trả `500` thay vì giả thành `400`.

Ý nghĩa:
- Log/monitoring phản ánh đúng lỗi server.
- FE và QA dễ phân biệt lỗi input với lỗi hệ thống.

## Cách đọc thay đổi này trong code
- `validation/PaginationValidationUtils.java`
- `validation/MultipartFileValidationUtils.java`
- `service/StorageService.java`
- `service/ProductService.java`
- `service/AuthService.java`
- `security/CustomUserDetailsService.java`
- `exception/GlobalExceptionHandler.java`

## Test nào chứng minh thay đổi
- `ProductServiceTest`: chặn fake image, chặn price range sai, chặn pagination quá lớn.
- `InspectionServiceImplTest`: chặn inspection report không phải PDF.
- `OrderEvidenceServiceImplTest`, `RefundServiceImplTest`, `ReportServiceImplTest`: chặn evidence không phải ảnh.
- `StorageServiceTest`: verify sanitize path/tên file.
- `PaginationValidationUtilsTest`: verify rule page/size dùng chung.
- `AuthServiceTest`: verify login normalize email và verify-email reject blank token.

## Điều vẫn nên nhớ
- Validation file hiện tại đã mạnh hơn nhiều, nhưng chưa phải antivirus hay content-scanning đầy đủ.
- H2 test profile của repo vẫn chưa mô phỏng PostgreSQL enum thật tốt; đó là một hướng hardening khác nên làm tiếp.
- Nếu sau này thêm API upload mới, nên tái sử dụng `MultipartFileValidationUtils` thay vì tự viết rule mới từ đầu.
