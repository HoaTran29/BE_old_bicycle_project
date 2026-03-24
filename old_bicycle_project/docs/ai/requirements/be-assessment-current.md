# Backend Assessment - Current State

Date: 2026-03-24  
Scope: `BE_old_bicycle_project/old_bicycle_project` backend compared against `../SRS-Old-Bicycles-Marketplace (1).md`

## Executive Summary

Backend hiện đã vượt khá xa giai đoạn “có entity và controller nhưng chưa thành flow”. Các trục nghiệp vụ chính đã có đường đi thực tế qua database và runtime local/Supabase:

- auth với verify email, reset password, refresh token, chặn account unactive/banned
- listing -> admin moderation -> mandatory inspection -> public visibility
- order -> upfront payment -> webhook confirmation -> refund/payout manual có audit
- chat realtime với STOMP + unread handling
- buyer review + seller reply
- order evidence cho seller handover / buyer receipt
- groupset master data + size chart theo category

Điểm backend vẫn chưa lên mức “full SRS” là phần payment chiều sâu, chatbot, logistics, payout automation, video/media depth, và thêm một vòng stabilization cho timeout/expiry.

### Current backend readiness: **79%**

Con số này là ước lượng readiness theo SRS và business flow hiện tại, không phải chỉ dựa vào số file hay số module.

## Assessment Method

Weighted feature score:

- `Must` = 5
- `Should` = 3
- `Could` = 1
- `Done` = 1.0
- `Partial` = 0.5
- `Missing` = 0.0

Estimated raw feature score from the matrix below: **73%**

Readiness adjustment: **+6 points**

Reason for adjustment:

- nhiều module đã được smoke-test bằng runtime thật, không còn chỉ là unit/service coverage
- order, refund, payout, review, inspection, and size-chart/groupset flows đã nối được end-to-end
- schema và SRS đã được sync lại liên tục theo từng tranche lớn

Final assessed backend progress: **79%**

## Repository Snapshot

- Spring Boot 3.4.3
- Java 21
- PostgreSQL + Flyway
- JWT + refresh token
- WebSocket chat
- Supabase PostgreSQL + Storage
- SePay inbound payment integration

## SRS Matrix

| SRS ID | Module | Priority | Status | Current BE | What blocks `Done` |
| --- | --- | --- | --- | --- | --- |
| `F-001` | User Authentication | Must | `Done` | Register, login, refresh, logout, email verification, forgot/reset password, `/me`, profile update, change password, Google OAuth config, inactive/banned guard, and clearer auth error mapping đều đã có. | Core auth scope đã usable. |
| `F-002` | Bike Listing | Must | `Partial` | Product create/update/delete/hide/show/relist đã đi qua storage thật, required technical fields đã được siết, groupset và frame specs đã được chuẩn hóa tốt hơn. | Video/media depth vẫn chưa có theo full SRS. |
| `F-003` | Search & Filter | Must | `Done` | Public search với pagination và filter cơ bản đã usable. | Scope cốt lõi đã đủ. |
| `F-004` | Advanced Filter | Must | `Partial` | Đã có filter theo brand, category, brake, frame material, province, frame size, wheel size, groupset, verified. | `hasVideo` và một vài chiều sâu filter theo media chưa có. |
| `F-005` | Bike Detail View | Must | `Partial` | Product detail đã có inspection summary/report, seller review aggregate, groupset chuẩn hóa, và `categoryId` để FE render size guidance. | Video/media depth và một số trust detail nâng cao chưa đủ full SRS. |
| `F-006` | Messaging System | Must | `Done` | REST conversation/message, unread handling, STOMP auth, subscribe/send guard, ownership enforcement đã có. | Có thể tăng integration coverage, nhưng core flow đã usable. |
| `F-007` | Wishlist | Should | `Done` | Add/remove/list wishlist đã hoàn chỉnh ở service + API. | Scope hiện tại đã đủ. |
| `F-008` | Deposit & Order | Must | `Partial` | Order create/accept/payment request/confirm deposit/complete/confirm-received/refund request/manual payout đều đã có. Evidence upload cũng đã nối vào order flow. | Chưa có timeout/expiry/auto-cancel, late-payment handling, remaining-payment phase, ledger sâu hơn. |
| `F-009` | Seller Rating | Must | `Done` | Buyer chỉ review sau order hợp lệ, seller reply một lần cho review, aggregate rating đã có. | Core review/reply scope đã usable. |
| `F-010` | Inspection System | Must | `Done` | Mandatory inspection before public đã chạy thật: admin send-to-inspection, inspector evaluate, report upload, validity window, seller notification, public visibility guard. | Có thể mở rộng assignment/appeal sau, nhưng core SRS hiện tại đã usable. |
| `F-011` | Admin Dashboard | Must | `Partial` | Admin đã có user management, product moderation, refund/dispute review, reference-data CRUD, groupset CRUD, size chart CRUD, report processing audit. | Analytics depth và vài admin workflows nâng cao vẫn còn mỏng. |
| `F-012` | Report System | Must | `Partial` | Report submit/list/process đã có, có duplicate guard và admin audit fields. | Appeal/escalation/richer sanctions workflow chưa đủ rộng. |
| `F-013` | Notification System | Must | `Done` | Notification center API, unread count, mark read, mark all read, event publish cho các flow chính đã usable. | Core scope đủ. |
| `F-014` | Chatbot Support | Could | `Missing` | Chưa có backend assistant module. | Toàn bộ feature còn thiếu. |
| `F-015` | Logistics Integration | Could | `Missing` | Chưa có logistics module. | Toàn bộ feature còn thiếu. |
| `F-016` | Online Payment | Could | `Partial` | Phase-1 payment đã usable: payment request, QR/instructions, SePay webhook, refund request, manual payout, payout profile, TPBank/static fallback, order evidence cho dispute context. | Chưa có payout automation, timeout/expiry flow, late-payment policy, full VA-first path. |

## Readiness By Layer

| Layer | Status | Assessment |
| --- | --- | --- |
| Database schema and migrations | `Partial` | Migrations đang khá kỷ luật và bám feature slices tốt hơn nhiều; tuy vậy schema payment vẫn chưa có lớp timeout/expiry mới. |
| Authorization and ownership | `Partial` | Ownership ở auth/chat/review/notification/report/order khá tốt. Vẫn cần rà tiếp edge cases khi thêm chatbot và payment expiry. |
| Business-rule enforcement | `Partial` | Listing moderation, inspection bắt buộc, payout/refund manual, review/reply, evidence flow đã được siết rõ. Gaps lớn còn lại là payment timeout và deeper transaction policy. |
| Automated testing | `Partial` | Service/controller tests đã phủ được nhiều hơn trước và có smoke runtime cho nhiều flow. Nhưng chưa có integration suite thật sự rộng cho toàn bộ payment/chat/order matrix. |
| API and DX foundations | `Partial` | API surface dùng được và có Swagger/global error handling. Cần thêm một đợt consistency polish cho payment/assistant sắp tới. |

## Main Findings

1. Backend hiện đã có đủ lõi nghiệp vụ để demo một marketplace cũ có moderation, inspection, payment, dispute, review, và payout manual.
2. Mandatory inspection before public là thay đổi lớn nhất về trust layer và hiện đã đi qua runtime thật.
3. Payment không còn là “mock-only”; hệ thống đã có inbound webhook, manual payout/refund audit, nhưng vẫn chưa có timeout/expiry và không nên xem là full financial engine.
4. Review/reply và order evidence đã đóng được nhiều khoảng trống trong dispute/audit flow.
5. Groupset và size chart đã đưa technical master-data đi đúng hướng, giảm text tự do trong listing/filter/detail.
6. Khoảng trống lớn nhất còn lại cho backend hiện không phải breadth của module nữa mà là chiều sâu của transaction state machine và quality/stabilization.

## Recommended Next Milestones

### Milestone 1 - Stabilization

- Cập nhật assessment và rà lại quality gaps còn sót.
- Dọn mojibake/copy inconsistency ở các file legacy còn dính.
- Chạy smoke end-to-end cuối cho listing, inspection, payment, refund/payout, review, evidence, groupset, size chart.

### Milestone 2 - Payment Timeout / Expiry

- Thêm `PaymentStatus.expired`
- Thêm `payments.expires_at`, `orders.cancel_reason`, `orders.cancelled_at`
- Thêm scheduler auto-cancel order quá hạn thanh toán
- Thêm late-payment handling thay vì tự revive order

### Milestone 3 - Chatbot Level 2

- Spring Boot gọi Vercel AI Gateway qua server-side HTTP
- `POST /api/assistant/chat`
- context-aware answers dựa trên listing/order/inspection/refund/payout của user

## Deferred From Current MVP

- Video upload/playback/filtering depth
- Logistics integration
- Payout automation thật
- VA-first payment path toàn diện
- Chatbot beyond level 2
