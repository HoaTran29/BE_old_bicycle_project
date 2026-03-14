---
phase: deployment
title: Environment and Migration Checklist
description: Checklist for .env setup, Flyway migration readiness, and SePay configuration
---

# Environment and Migration Checklist

## 1. Mục tiêu

Tài liệu này giúp chốt nhanh 3 việc:

- cần điền biến môi trường nào để backend chạy được
- khi nào cần apply migration
- SePay cần những thông tin gì để hoạt động ở `mock mode` và `non-mock mode`

## 1.1. Phạm vi dùng Supabase trong dự án này

- Supabase hiện được dùng như:
  - nơi host PostgreSQL
  - nơi chứa storage cho ảnh
- Supabase hiện không phải là backend chính của ứng dụng.
- Backend chính vẫn là Spring Boot.
- Vì vậy việc kết nối dữ liệu đi theo hướng:
  - Spring Boot kết nối Postgres của Supabase qua JDBC
  - Spring Boot xử lý business logic
  - không phụ thuộc vào Supabase Auth hay Supabase Edge Functions

## 2. File `.env`

Repo hiện đã có file mẫu:

- `.env.example`

Checklist:

- [ ] Copy `.env.example` thành `.env`
- [ ] Điền `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`
- [ ] Điền `JWT_SECRET`
- [ ] Điền `SUPABASE_URL`, `SUPABASE_ANON_KEY`
- [ ] Nếu dùng email thật: điền `MAIL_USERNAME`, `MAIL_PASSWORD`
- [ ] Nếu dùng Google login: điền `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`
- [ ] Nếu đang dev local: để `SEPAY_MOCK_MODE=true`
- [ ] Nếu muốn chạy SePay gần thật: đặt `SEPAY_MOCK_MODE=false` và điền đủ các biến SePay

## 3. Biến nào là bắt buộc?

### Bắt buộc để app backend chạy với DB thật

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`
- `JWT_SECRET`
- `SUPABASE_URL`
- `SUPABASE_ANON_KEY`

### Bắt buộc nếu dùng verify/reset password qua email

- `MAIL_USERNAME`
- `MAIL_PASSWORD`

### Bắt buộc nếu dùng Google OAuth

- `GOOGLE_CLIENT_ID`
- `GOOGLE_CLIENT_SECRET`

### Bắt buộc nếu chạy SePay non-mock

- `SEPAY_MOCK_MODE=false`
- `SEPAY_WEBHOOK_API_KEY`
- `SEPAY_BANK_BIN`
- `SEPAY_ACCOUNT_NUMBER`
- `SEPAY_ACCOUNT_NAME`

### Hiện có khai báo nhưng chưa dùng outbound thật

- `SEPAY_API_TOKEN`

Biến này đã có chỗ khai báo trong config, nhưng implementation hiện tại chưa gọi outbound SePay API thật. Nó hữu ích cho giai đoạn tích hợp sâu hơn sau này.

## 4. Khi nào cần apply migration?

Bạn cần apply migration nếu:

- DB hiện tại chưa có các thay đổi mới trong code
- hoặc bạn đang chuyển từ schema cũ sang code mới hơn

Với trạng thái repo hiện tại, cần đặc biệt lưu ý:

- `V4__align_runtime_schema.sql`
- `V5__payment_refund_upgrade.sql`
- `V6__password_reset_tokens.sql`
- `V7__product_inspection_br_hardening.sql`

Nếu các migration này chưa nằm trong DB thật, rất dễ gặp:

- app không lên được vì `ddl-auto=validate`
- feature mới lỗi do thiếu cột/bảng/index/trạng thái enum

## 5. Cách apply migration trong repo này

Repo đang dùng:

- `spring.flyway.enabled=true`
- `spring.jpa.hibernate.ddl-auto=validate`
- thư viện `spring-dotenv`

Nghĩa là cách đơn giản nhất là:

1. chuẩn bị file `.env`
2. đảm bảo DB thật đang reachable
3. khởi động application

Khi app khởi động, Flyway sẽ tự chạy migration chưa apply.

## 5.1. Chọn connection string nào cho Spring Boot backend?

Theo Supabase docs, với backend chạy lâu dài như Spring Boot:

- ưu tiên `direct connection` nếu môi trường hỗ trợ IPv6
- nếu môi trường chỉ có IPv4, ưu tiên `session pooler`
- `transaction pooler` phù hợp hơn cho các workload ngắn hạn hoặc serverless

Vì vậy, backend này không nên mặc định nghĩ rằng cứ Supabase là phải dùng transaction pooler.

## 6. Checklist trước khi apply migration

- [ ] Đã backup DB nếu là staging/prod
- [ ] `.env` có đúng `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`
- [ ] DB user có quyền `ALTER TABLE`, `CREATE TABLE`, `CREATE INDEX`, và thay đổi enum nếu cần
- [ ] Team đã xác nhận môi trường đang trỏ đúng DB
- [ ] Nếu là staging/prod: đã có plan rollback

## 7. Checklist riêng cho SePay

### Nếu chỉ cần dev local

- [ ] `SEPAY_MOCK_MODE=true`
- [ ] Có thể để trống các biến SePay còn lại

### Nếu muốn gần với môi trường thật hơn

- [ ] `SEPAY_MOCK_MODE=false`
- [ ] Điền `SEPAY_WEBHOOK_API_KEY`
- [ ] Điền `SEPAY_BANK_BIN`
- [ ] Điền `SEPAY_ACCOUNT_NUMBER`
- [ ] Điền `SEPAY_ACCOUNT_NAME`
- [ ] Cấu hình endpoint webhook public trỏ về `/api/payments/sepay/webhook`

## 8. Kết luận ngắn

- Dev local: dùng `mock mode` là đủ
- Staging/production: phải có DB env thật và apply migration đầy đủ
- SePay non-mock hiện mới ở mức inbound webhook + QR/instruction flow, chưa phải outbound gateway integration hoàn chỉnh

## 9. Ghi chú vận hành

- Nếu apply DDL trực tiếp qua Supabase MCP, schema sẽ đổi ngay trên database.
- Tuy nhiên, bảng `public.flyway_schema_history` của ứng dụng có thể không tự tăng version tương ứng.
- Vì vậy sau khi dùng MCP để sync schema, vẫn cần nhớ:
  - app local có thể còn chạy lại `V5`, `V6`, `V7` khi startup
  - may mắn là các migration hiện tại chủ yếu idempotent nên rủi ro thấp hơn
  - nhưng về mặt bookkeeping thì Flyway history và schema state chưa hoàn toàn đồng bộ
- Với scope hiện tại, không cần triển khai thêm RLS chỉ vì Supabase advisor cảnh báo, nếu hệ thống vẫn chỉ dùng Supabase như managed Postgres/storage qua backend Spring Boot
