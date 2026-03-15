# SePay Webhook-Only Phase 1 - 2026-03-15

## Phạm vi

Lượt này chốt kiến trúc callback của backend theo hướng:

- giữ `SePay WebHook` làm kênh callback chính
- bỏ nhánh parse `IPN gateway` trong code
- giữ luồng `QR/chuyển khoản trực tiếp + webhook xác nhận`
- không đổi endpoint công khai để tránh gãy tích hợp

## Các thay đổi chính

### 1. Payment callback chuyển sang webhook-only

Đã cập nhật:

- [PaymentController.java](/e:/Old_bicycle_system/BE_old_bicycle_project/old_bicycle_project/src/main/java/com/backend/old_bicycle_project/controller/PaymentController.java)
- [PaymentService.java](/e:/Old_bicycle_system/BE_old_bicycle_project/old_bicycle_project/src/main/java/com/backend/old_bicycle_project/service/PaymentService.java)
- [PaymentServiceImpl.java](/e:/Old_bicycle_system/BE_old_bicycle_project/old_bicycle_project/src/main/java/com/backend/old_bicycle_project/service/impl/PaymentServiceImpl.java)

Kết quả:

- `POST /api/payments/sepay/webhook` vẫn được giữ nguyên
- service chỉ còn nhận `Authorization` header cho WebHook
- bỏ nhánh đọc payload `notification_type/order/transaction` của IPN gateway
- chỉ giữ payload kiểu WebHook:
  - `code`
  - `transferType`
  - `transferAmount`
  - `referenceCode`
  - `transactionDate`

### 2. Webhook authorization được đơn giản hóa

Đã đổi logic xác thực:

- trước đây chấp nhận cả `X-Secret-Key` lẫn `Authorization`
- bây giờ chỉ dùng `SEPAY_WEBHOOK_API_KEY` để so với:
  - `Authorization: <key>`
  - hoặc `Authorization: Apikey <key>`

Điều này làm flow rõ hơn:

- SePay WebHook -> `Authorization`
- backend -> đối chiếu với `payment.sepay.webhook-api-key`

### 3. Test được cập nhật theo webhook-only

Đã cập nhật:

- [PaymentServiceImplTest.java](/e:/Old_bicycle_system/BE_old_bicycle_project/old_bicycle_project/src/test/java/com/backend/old_bicycle_project/service/impl/PaymentServiceImplTest.java)

Các test chính:

- webhook hợp lệ với `Authorization: Apikey ...`
- webhook hợp lệ với raw authorization key
- reject nếu live mode nhưng thiếu `SEPAY_WEBHOOK_API_KEY`

## Vì sao chọn hướng này

Backend hiện tại phù hợp nhất với:

- buyer tạo payment request
- hệ thống trả về QR/chuyển khoản trực tiếp
- SePay WebHook báo tiền vào
- backend xác nhận order đã được cọc

Hướng này thực dụng hơn IPN cho bài toán hiện tại vì:

- tài khoản live đang là `MBBank`
- nhánh `BIDV VA order API` không phải đường chính cho tài khoản hiện tại
- business flow của dự án đang thiên về `upfront payment`

## Tác động tới SePay dashboard

Từ sau thay đổi này, với dự án old bicycle:

- nên cấu hình `WebHook` cho dự án này
- không nên tiếp tục xem `IPN` là callback chính của old bicycle nữa

Nếu vẫn để IPN ngoài SePay, backend hiện sẽ không còn parse payload đó như callback chính.

## Kiểm tra sau sửa

Nên test lại theo thứ tự:

1. `POST /api/orders/{id}/request`
2. lấy `gatewayOrderCode`
3. gửi WebHook payload có `code = gatewayOrderCode`
4. kiểm tra:
   - `payment.status = success`
   - `order.status = deposited`
   - `fundingStatus = held`

## Ghi chú

Lượt này chưa đổi tên endpoint `/api/payments/sepay/webhook` để tránh ảnh hưởng cấu hình SePay hiện có. Thay đổi nằm ở logic xử lý phía trong.
