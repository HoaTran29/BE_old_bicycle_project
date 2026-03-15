---
phase: implementation
title: MVP Scope De-scope + Regression Hardening
description: Notes for deferring video from MVP and strengthening wishlist/notification/payment test guards
---

# MVP Scope De-scope + Regression Hardening

## Mục tiêu

- Chốt lại phạm vi MVP: không đưa video/media vào sprint gần.
- Giữ assessment và roadmap khớp với quyết định scope mới.
- Tăng độ an toàn cho các flow đang thuộc MVP bằng regression tests và payment config hardening.

## Những gì đã thay đổi

### 1. Chốt lại scope MVP

- Tài liệu assessment và các note Product/Inspection giờ ghi rõ:
  - video/media vẫn là yêu cầu của full SRS
  - nhưng đã được defer khỏi MVP hiện tại
- Các milestone gần được đổi trọng tâm sang:
  - SePay non-mock readiness
  - regression tests
  - moderation/admin/report depth

### 2. Siết config SePay cho live mode

- `payment.sepay.mock-mode` không còn hard-code `true` trong `application.properties`.
- Giá trị này giờ đọc từ biến môi trường `SEPAY_MOCK_MODE`, mặc định là `true`.
- `PaymentServiceImpl` giờ chặn `createUpfrontPaymentRequest(...)` nếu:
  - đang ở live mode
  - nhưng thiếu `bankBin`, `accountNumber`, hoặc `webhookApiKey`
- `handleSepayWebhook(...)` cũng không còn cho phép live mode chạy với webhook key rỗng.

### 3. Tăng regression coverage cho MVP flows

- Thêm `WishlistServiceImplTest`
  - cover add thành công
  - chặn seller thêm sản phẩm của chính mình
  - chặn user không tồn tại
  - kiểm tra remove flow
- Thêm `NotificationServiceImplTest`
  - cover send + push notification
  - chặn read notification không thuộc user
  - kiểm tra unread count
- Mở rộng `PaymentServiceImplTest`
  - thêm guard cho live mode thiếu webhook key
  - thêm guard cho webhook live mode không có key

## Các file chính

- Config:
  - `src/main/resources/application.properties`
  - `src/main/java/com/backend/old_bicycle_project/service/impl/PaymentServiceImpl.java`
- Tests:
  - `src/test/java/com/backend/old_bicycle_project/service/impl/PaymentServiceImplTest.java`
  - `src/test/java/com/backend/old_bicycle_project/service/impl/WishlistServiceImplTest.java`
  - `src/test/java/com/backend/old_bicycle_project/service/impl/NotificationServiceImplTest.java`
- Docs:
  - `docs/ai/requirements/be-assessment-current.md`
  - `docs/ai/planning/feature-product-inspection-br-hardening.md`
  - `docs/ai/design/feature-product-inspection-br-hardening.md`
  - `docs/ai/implementation/feature-product-inspection-br-hardening.md`
  - `docs/ai/testing/feature-product-inspection-br-hardening.md`

## Verification

- Chạy targeted tests:
  - `.\mvnw.cmd "-Dtest=PaymentServiceImplTest,WishlistServiceImplTest,NotificationServiceImplTest" test`
- Chạy full suite:
  - `.\mvnw.cmd test`
- Kết quả:
  - pass `29` tests trên Java 21

## Còn lại

- Payment vẫn chưa gọi outbound SePay API thật.
- Non-mock readiness hiện mới được siết ở mức config guard + webhook authorization guard.
- Chat realtime vẫn còn thiếu integration-level coverage.
- Admin moderation/report/dispute vẫn là cụm nên làm tiếp theo sau slice này.
