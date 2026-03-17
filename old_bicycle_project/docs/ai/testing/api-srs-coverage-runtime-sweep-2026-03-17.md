# API SRS Coverage And Runtime Sweep - 2026-03-17

## Scope

- Repository: `BE_old_bicycle_project/old_bicycle_project`
- SRS source: `../SRS-Old-Bicycles-Marketplace (1).md`
- Runtime target: local Spring Boot app on `localhost:8080` backed by Supabase `SWP391`
- Objective:
  - đối chiếu API hiện có với SRS
  - sweep runtime các API chính
  - ghi lại gap còn thiếu và mức ưu tiên

## Summary

Backend hiện không còn ở trạng thái "thiếu API nền tảng". Bề rộng API REST đã đủ lớn cho các flow chính: auth, product, inspection, chat REST, wishlist, order, payment, report, notification, admin dashboard, admin product moderation, refund review.

Kết quả sweep runtime cho thấy các flow business chính đã chạy được thật trên Supabase:

- auth login, `/me`, profile update
- product create, seller my-products, public product search/detail
- admin product moderation list + đổi trạng thái
- wishlist add/list/remove
- inspection request/evaluate/fetch
- chat REST create conversation, list conversation, list messages, mark read
- report submit, my reports, admin list/process
- notifications list/unread/mark read/read all
- cash order -> accept -> confirm deposit -> complete -> review
- transfer order -> accept -> payment request -> SePay webhook -> refund request -> admin approve -> admin complete
- seller reviews public fetch
- admin dashboard stats

## Runtime Sweep Results

### Passed directly in runtime

- `GET /api/products`
- `GET /api/products/{id}`
- `GET /api/brands`
- `GET /api/categories`
- `GET /api/brake-types`
- `GET /api/frame-materials`
- `POST /api/auth/register`
- `POST /api/auth/login`
- `GET /api/auth/me`
- `PATCH /api/auth/profile`
- `POST /api/auth/forgot-password`
- `GET /api/users/{sellerId}/reviews`
- `POST /api/products`
- `GET /api/products/my`
- `GET /api/admin/products`
- `PATCH /api/admin/products/{id}/status`
- `POST /api/inspections/request/{productId}`
- `POST /api/inspections/evaluate/{productId}`
- `GET /api/inspections/product/{productId}`
- `POST /api/conversations`
- `GET /api/conversations/me`
- `GET /api/conversations/{conversationId}/messages`
- `PUT /api/conversations/{conversationId}/read`
- `POST /api/reports`
- `GET /api/reports/me`
- `GET /api/admin/reports`
- `PUT /api/admin/reports/{reportId}/process`
- `GET /api/notifications/me`
- `GET /api/notifications/me/unread-count`
- `PUT /api/notifications/{notificationId}/read`
- `PUT /api/notifications/me/read-all`
- `POST /api/orders`
- `PATCH /api/orders/{orderId}/accept`
- `PATCH /api/orders/{orderId}/confirm-deposit`
- `PATCH /api/orders/{orderId}/complete`
- `GET /api/orders/me`
- `POST /api/payments/orders/{orderId}/request`
- `GET /api/payments/orders/{orderId}`
- `POST /api/payments/sepay/webhook`
- `POST /api/orders/{orderId}/refunds`
- `PATCH /api/admin/refunds/{refundId}/review`
- `GET /api/admin/dashboard/stats`

### Sweep notes

- Product moderation path hiện đã có API admin thật, nên không còn bắt buộc phải `UPDATE products SET status='active'` bằng SQL để test order/payment.
- Transfer flow đã được chạy lại trên product mới, không dùng lại product cũ bị `hidden` bởi report resolution.
- SePay callback path hiện đã được chốt theo `webhook-only`.

## Fixes Applied During Audit

### 1. Chặn lại auth endpoint lẽ ra phải protected

Files:

- `src/main/java/com/backend/old_bicycle_project/security/SecurityConfig.java`
- `src/test/java/com/backend/old_bicycle_project/security/SecurityConfigIntegrationTest.java`

Fix:

- `PATCH /api/auth/profile` không còn public
- `PATCH /api/auth/change-password` không còn public
- đã có regression test để giữ 2 endpoint này trả `401` khi anonymous

### 2. Mở public đúng seller review endpoint

File:

- `src/main/java/com/backend/old_bicycle_project/security/SecurityConfig.java`

Fix:

- `GET /api/users/{sellerId}/reviews` giờ public đúng với use case buyer/guest xem uy tín seller

### 3. Trả lỗi rõ hơn cho request body JSON sai

Files:

- `src/main/java/com/backend/old_bicycle_project/exception/ErrorCode.java`
- `src/main/java/com/backend/old_bicycle_project/exception/GlobalExceptionHandler.java`
- `src/test/java/com/backend/old_bicycle_project/exception/GlobalExceptionHandlerTest.java`

Fix:

- thêm `INVALID_REQUEST_BODY`
- malformed JSON không còn trả `Uncategorized error`
- hiện trả:
  - code `1023`
  - message `Request body khong hop le`

## SRS Coverage Assessment

### Covered well enough for current backend scope

- Auth nền tảng: register, login, refresh, logout, forgot/reset password, verify email, me/profile/change-password
- Public product browsing: search, filter cơ bản, detail
- Wishlist
- Inspection request/evaluate/view
- Messaging REST
- Order lifecycle cơ bản
- Review submission và public seller review
- Notifications center
- Report submit + admin process
- Payment webhook-only phase-1
- Refund request + admin review
- Admin dashboard stats

### Partial against full SRS

- Seller listing management:
  - có create/update/delete
  - chưa có self-service `hide/show` rõ ràng cho seller
- Product moderation:
  - đã có admin list + đổi status
  - nhưng business flow moderation vẫn còn khá mỏng
- Advanced filter:
  - đủ nhiều field kỹ thuật
  - chưa có `hasVideo`
- Bike detail:
  - đã có inspection summary và verified state
  - chưa có video/media depth
- Messaging:
  - REST ổn
  - WebSocket realtime chưa sweep live end-to-end
- Payment:
  - upfront flow chạy được
  - chưa có remaining-payment phase, payout, auto release, auto refund execution
- Social login:
  - có dấu vết Google OAuth2
  - chưa thấy Facebook

### Missing or clearly underbuilt

- Admin user management API:
  - [Done 2026-03-17] tranche 1: list/filter/detail/status change
  - cÃ²n thiáº¿u reset password vÃ  user activity view Ä‘á»ƒ chạm Ä‘áº§y Ä‘á»§ FR-ADM-001
- Seller reply-to-review API
- Category update/delete admin API
- Groupset / size chart reference-data management API
- Chatbot support
- Logistics integration
- Video upload / video filter / video playback APIs

## Follow-up Applied After Initial Audit

### 2026-03-17 - Auth verification guard

- Login now rejects users whose `isVerified` flag is still `false`.
- Refresh-token reuse also rejects unverified accounts, so old sessions from pre-guard behavior cannot continue silently.
- Covered by focused auth regression tests in `AuthServiceTest`.

### 2026-03-17 - Admin user management tranche 1

- Added `GET /api/admin/users` with keyword, role, status, and verified filters.
- Added `GET /api/admin/users/{id}` for admin detail lookup.
- Added `PATCH /api/admin/users/{id}/status` for active/unactive/banned management.
- Added a guard so admins cannot change their own account status and accidentally lock themselves out.
- Covered by focused service, controller, and security regression tests.

## Priority Backlog From This Audit

### P0 - nên làm sớm nhất

- [Done 2026-03-17] Bắt login phải tôn trọng `isVerified`
- Hoàn thiện admin user management
- Siết rõ moderation flow cho listing mới tạo
- Tăng integration coverage cho realtime chat và payment webhook duplicate/edge cases

### P1 - nên làm ngay sau đó

- Seller hide/show listing endpoint
- Seller reply review endpoint
- Category update/delete admin endpoint
- Refund admin list/history endpoint nếu admin UI cần thao tác thực tế dễ hơn

### P2 - có ích nhưng chưa gấp

- Groupset / size chart management
- Facebook social login
- Richer payment phases và payout automation

### P3 - full SRS nhưng có thể defer

- Video/media support
- Chatbot
- Logistics integration

## Key Conclusion

Kết luận sau lượt audit này:

- backend không còn thiếu các API cốt lõi cho MVP transaction flow
- runtime thực đã chứng minh được cả cash path lẫn transfer + webhook + refund path
- gap lớn nhất bây giờ không còn là "có API hay chưa", mà là:
  - moderation depth
  - admin user tooling
  - auth verification rule
  - deeper integration coverage
