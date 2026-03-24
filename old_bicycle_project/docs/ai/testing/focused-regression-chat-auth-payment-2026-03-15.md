# Focused Regression Chat, Auth, Payment - 2026-03-15

## Mục tiêu

Khóa chất lượng cho ba cụm vừa thay đổi nhiều nhất trong các sprint gần đây:

1. `auth`
2. `chat`
3. `payment`

Mục tiêu của batch này không phải mở rộng feature mới, mà là kiểm tra lại các luồng chính và các edge case dễ vỡ sau các thay đổi về:

- refresh token
- WebSocket/chat ownership
- SePay `webhook-only`
- Swagger public qua `ngrok`

## Phạm vi regression

### 1. Auth

File test chính:

- `src/test/java/com/backend/old_bicycle_project/service/AuthServiceTest.java`

Case đã thêm hoặc siết lại:

- `register` mặc định về `buyer` nếu request không truyền role hợp lệ
- `register` chặn password yếu
- `refreshToken` trả access token mới khi refresh token còn hạn
- `refreshToken` revoke toàn bộ session khi refresh token đã hết hạn

### 2. Chat

File test chính:

- `src/test/java/com/backend/old_bicycle_project/controller/ChatControllerTest.java`
- `src/test/java/com/backend/old_bicycle_project/service/impl/MessageServiceImplTest.java`

Case đã thêm hoặc siết lại:

- `ChatController.sendMessage(...)` reject principal không có UUID hợp lệ
- `markAsRead(...)` delegate đúng xuống service
- `MessageServiceImpl.sendMessage(...)` chỉ cho participant gửi tin nhắn
- gửi tin nhắn thành công thì phải:
  - lưu message
  - cập nhật `conversation.updatedAt`
  - publish notification event

### 3. Payment

File test chính:

- `src/test/java/com/backend/old_bicycle_project/service/impl/PaymentServiceImplTest.java`

Case đã thêm hoặc siết lại:

- webhook reject `Authorization` sai
- webhook reject giao dịch thiếu tiền
- webhook bỏ qua giao dịch `transferType = out`
- webhook idempotent khi payment đã `success`

## Code runtime được sửa để hỗ trợ regression

### Chat

File:

- `src/main/java/com/backend/old_bicycle_project/controller/ChatController.java`

Thay đổi:

- `resolveSenderId(...)` giờ bắt `IllegalArgumentException` khi principal không phải UUID hợp lệ
- thay vì để văng lỗi kỹ thuật thô, controller chuẩn hóa về `UNAUTHENTICATED`

### Auth

File:

- `src/main/java/com/backend/old_bicycle_project/service/AuthService.java`

Batch này không đổi logic nghiệp vụ lớn, nhưng regression mới khóa lại các rule đã có:

- password policy
- default role
- revoke refresh token khi reset/change password
- refresh token hết hạn không được dùng lại

### Payment

File:

- `src/main/java/com/backend/old_bicycle_project/service/impl/PaymentServiceImpl.java`

Regression tập trung vào nhánh `webhook-only`:

- chỉ tin request có `Authorization` đúng
- chỉ xử lý giao dịch `tiền vào`
- chỉ cập nhật order/payment khi số tiền nhận đủ
- duplicate callback không được làm hỏng trạng thái

## Lệnh đã chạy

### Targeted suite

```powershell
cmd /c ".\mvnw.cmd -q -Dtest=AuthServiceTest,ChatControllerTest,MessageServiceImplTest,PaymentServiceImplTest test"
```

Kết quả:

- `pass`

### Full suite

```powershell
cmd /c ".\mvnw.cmd -q test"
```

Kết quả:

- `pass`

## Kết quả chốt

- batch regression trọng tâm cho `auth/chat/payment` đã xanh
- không phát hiện regression mới từ các thay đổi `webhook-only`
- không phát hiện regression mới từ thay đổi validate principal trong chat

## Rủi ro còn lại

- full suite vẫn có warning quen thuộc của Mockito về dynamic agent trên JDK 21
- `SpringBootTest` với H2 vẫn in warning liên quan enum/PostgreSQL custom type, nhưng suite hiện vẫn xanh
- phần realtime WebSocket ở mức broker/runtime thật vẫn chưa có integration test end-to-end

## Bước hợp lý tiếp theo

Sau batch regression này, roadmap nên chuyển sang một trong hai hướng:

1. đi sâu `admin/report/dispute`
2. hoặc tăng thêm integration coverage cho realtime chat và refund flow nếu muốn siết chất lượng trước
