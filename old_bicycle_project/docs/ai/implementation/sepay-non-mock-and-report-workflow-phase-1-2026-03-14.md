# SePay Non-Mock và Report Workflow Phase 1 - 2026-03-14

## Phạm vi

Lượt này tập trung vào 3 cụm có ROI cao:

1. SePay non-mock phase 1
2. regression tests cho auth/chat/payment/report
3. hardening workflow report/admin

## Những gì đã làm

### 1. SePay non-mock phase 1

Đã cập nhật:

- [PaymentServiceImpl.java](/e:/Old_bicycle_system/BE_old_bicycle_project/old_bicycle_project/src/main/java/com/backend/old_bicycle_project/service/impl/PaymentServiceImpl.java)
- [PaymentController.java](/e:/Old_bicycle_system/BE_old_bicycle_project/old_bicycle_project/src/main/java/com/backend/old_bicycle_project/controller/PaymentController.java)
- [SepayProperties.java](/e:/Old_bicycle_system/BE_old_bicycle_project/old_bicycle_project/src/main/java/com/backend/old_bicycle_project/config/SepayProperties.java)
- [PaymentService.java](/e:/Old_bicycle_system/BE_old_bicycle_project/old_bicycle_project/src/main/java/com/backend/old_bicycle_project/service/PaymentService.java)
- [application.properties](/e:/Old_bicycle_system/BE_old_bicycle_project/old_bicycle_project/src/main/resources/application.properties)
- [.env.example](/e:/Old_bicycle_system/BE_old_bicycle_project/old_bicycle_project/.env.example)

Kết quả:

- `mock mode` vẫn giữ nguyên cho dev local
- `non-mock mode` giờ có 2 nhánh:
  - nếu có `SEPAY_API_TOKEN` và đủ config thì tạo order thật qua SePay API
  - nếu chưa có token nhưng vẫn có webhook + bank transfer config thì dùng live transfer QR fallback
- webhook giờ nhận cả:
  - `X-Secret-Key`
  - `Authorization: Apikey ...`
- webhook parser giờ chịu được cả:
  - payload cũ kiểu transfer webhook
  - payload mới kiểu payment gateway / IPN

### 2. Regression tests

Đã thêm:

- [PaymentServiceImplTest.java](/e:/Old_bicycle_system/BE_old_bicycle_project/old_bicycle_project/src/test/java/com/backend/old_bicycle_project/service/impl/PaymentServiceImplTest.java)
- [AuthControllerTest.java](/e:/Old_bicycle_system/BE_old_bicycle_project/old_bicycle_project/src/test/java/com/backend/old_bicycle_project/controller/AuthControllerTest.java)
- [ChatControllerTest.java](/e:/Old_bicycle_system/BE_old_bicycle_project/old_bicycle_project/src/test/java/com/backend/old_bicycle_project/controller/ChatControllerTest.java)
- [MessageServiceImplTest.java](/e:/Old_bicycle_system/BE_old_bicycle_project/old_bicycle_project/src/test/java/com/backend/old_bicycle_project/service/impl/MessageServiceImplTest.java)
- [ReportServiceImplTest.java](/e:/Old_bicycle_system/BE_old_bicycle_project/old_bicycle_project/src/test/java/com/backend/old_bicycle_project/service/impl/ReportServiceImplTest.java)

Coverage mới tập trung vào:

- non-mock SePay create request
- legacy webhook và IPN webhook
- auth controller delegation
- chat realtime routing
- unread/read guard
- report duplicate guard
- report sanction + audit

### 3. Report/admin workflow

Đã cập nhật:

- [Report.java](/e:/Old_bicycle_system/BE_old_bicycle_project/old_bicycle_project/src/main/java/com/backend/old_bicycle_project/entity/Report.java)
- [ReportRepository.java](/e:/Old_bicycle_system/BE_old_bicycle_project/old_bicycle_project/src/main/java/com/backend/old_bicycle_project/repository/ReportRepository.java)
- [ReportService.java](/e:/Old_bicycle_system/BE_old_bicycle_project/old_bicycle_project/src/main/java/com/backend/old_bicycle_project/service/ReportService.java)
- [ReportServiceImpl.java](/e:/Old_bicycle_system/BE_old_bicycle_project/old_bicycle_project/src/main/java/com/backend/old_bicycle_project/service/impl/ReportServiceImpl.java)
- [ReportController.java](/e:/Old_bicycle_system/BE_old_bicycle_project/old_bicycle_project/src/main/java/com/backend/old_bicycle_project/controller/ReportController.java)
- [ReportResponseDTO.java](/e:/Old_bicycle_system/BE_old_bicycle_project/old_bicycle_project/src/main/java/com/backend/old_bicycle_project/dto/response/ReportResponseDTO.java)
- [V8__report_workflow_hardening.sql](/e:/Old_bicycle_system/BE_old_bicycle_project/old_bicycle_project/src/main/resources/db/migration/V8__report_workflow_hardening.sql)

Kết quả:

- report xử lý giờ lưu:
  - `admin_note`
  - `processed_at`
  - `processed_by`
- user có endpoint xem danh sách report của chính mình
- admin có filter `status`, `targetType`
- chặn duplicate open report cùng reporter và cùng target
- khi resolve report có thể:
  - ban user vi phạm
  - hide product vi phạm
- reporter và phía bị ảnh hưởng được gửi notification

## Migration / DB

Đã apply `V8` trực tiếp lên project Supabase `SWP391`.

Đã verify có các cột mới trong bảng `reports`:

- `admin_note`
- `processed_at`
- `processed_by`

## Verification

Đã chạy:

```powershell
.\mvnw.cmd -q "-Dtest=PaymentServiceImplTest,AuthControllerTest,ChatControllerTest,MessageServiceImplTest,ReportServiceImplTest" test
.\mvnw.cmd -q test
```

Kết quả:

- pass

## Giới hạn còn lại

### 1. SePay non-mock chưa live-verify được

Code path đã có, nhưng môi trường hiện tại chưa có:

- `SEPAY_API_TOKEN`
- `SEPAY_BANK_ACCOUNT_ID` hoặc mapping account tương ứng

Nên chưa thể xác nhận live transaction thật với SePay.

### 2. Report/dispute mới ở phase 1

Chưa có:

- appeal flow
- escalation flow
- dispute timeline đầy đủ
- integration test end-to-end với notification persistence + API

### 3. Chat regression vẫn thiên về unit/controller

Chưa có integration test STOMP end-to-end cho unread lifecycle.
