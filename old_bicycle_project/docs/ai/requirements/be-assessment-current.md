# Backend Assessment - Current State

Date: 2026-03-17  
Scope: `BE_old_bicycle_project/old_bicycle_project` backend compared against `../SRS-Old-Bicycles-Marketplace (1).md`

## Executive Summary

The old backend assessment is no longer a reliable baseline. The current repository is broader than the old report suggested, and the backend has now been validated not only at service-test level but also against the real Supabase-backed runtime: schema sync, storage-backed product creation, storage cleanup on update/delete, and both mock and non-mock product-to-order-to-payment smoke paths now pass. The transaction layer has moved one step further again: non-mock SePay integration hooks now exist, the MBBank case now falls back cleanly to direct-transfer QR while still using real webhook confirmation, report processing now records admin audit data, and regression coverage is better around auth/chat/report flows. The system is still behind the SRS in several must-have areas, but the backend is no longer missing a usable transaction path.

### **Fixed backend progress assessment: 74%**

This number reflects SRS-aligned backend readiness, not just file count or module breadth.

### MVP scope note

- Product video upload, video filtering, and video playback remain part of the full SRS scope.
- The current delivery plan now explicitly defers video/media depth out of MVP so the team can focus on payment readiness, regression coverage, and admin/report workflow depth.

### Why the score is not lower

- The backend already has real modules for auth, products, chat, inspection, review, report, notification, dashboard, and reference data.
- Flyway, Spring Security, Swagger, WebSocket chat, and a global exception handler are already present.
- The repo has moved beyond the old report's "early skeleton" stage.

### Why the score is not higher

- Several implemented modules are still only `Partial` because business rules, payment flow depth, or admin workflow depth are missing.
- The payment flow now supports a real SePay API path in non-mock mode, but it is still phase-1: no remaining-payment phase, no payout automation, and no automated refund execution.
- Delivery confidence is better because `V1-V8` has now been reconciled on the active Supabase environment, but repeatability across environments still depends on proper Flyway application in each runtime.
- Test coverage is better than before, but automated integration coverage is still far from full regression protection.

## Repository Snapshot

- 14 controllers
- 20 entities
- Spring Boot 3.4.3
- Java 21
- PostgreSQL + Flyway
- JWT + refresh token flow
- Google OAuth2 configuration
- WebSocket chat infrastructure
- Swagger UI

## Scoring Method

Weighted feature score:

- `Must` = 5
- `Should` = 3
- `Could` = 1
- `Done` = 1.0
- `Partial` = 0.5
- `Missing` = 0.0

Raw feature score from the SRS matrix below: **69%**

Readiness adjustment: **+5 points**

Reason for adjustment:

- A first payment/refund flow now exists, targeted service tests cover the main service layer, non-mock SePay order creation is now implemented behind real config flags, and the backend has passed real runtime smoke on Supabase for product creation with storage, product image replacement, storage cleanup on delete, order acceptance, payment request creation, and webhook confirmation.
- The non-mock SePay path has now been verified against a real SePay account and a public ngrok-routed callback: for the current MBBank account, the backend correctly falls back from BIDV-only VA order creation to direct-transfer QR plus real IPN/webhook confirmation.
- The system still lacks automated end-to-end integration tests and payment depth beyond the phase-1 upfront path.
- Admin user management now covers list/filter/detail/status reset-password/activity, seller listing management now includes self-service hide/show, admin product moderation is deeper, and admin CRUD now exists for core reference data used by FE forms and filters.

Final assessed backend progress: **74%**

## SRS Matrix

| SRS ID | Module | Priority | Status | Current BE | What blocks `Done` |
| --- | --- | --- | --- | --- | --- |
| `F-001` | User Authentication | Must | `Done` | Register, login, refresh, logout, email verification, forgot/reset password, `/me`, profile update, and change-password flows now exist. JWT and refresh-token flow are present, and password policy now enforces min 8 chars + uppercase + number. | Base must-have authentication scope is covered. |
| `F-002` | Bike Listing | Must | `Partial` | Product create now enforces required technical fields and minimum image count, sets `expiresAt`, uses seller-scoped listing queries, applies soft delete, uploads real images to Supabase Storage, and now cleans old images on update/delete. Seller self-service `hide/show` now exists and relisting correctly returns the listing to `pending` for moderation. | Video/media depth is intentionally deferred from MVP but still missing against the full SRS. |
| `F-003` | Search & Filter | Must | `Done` | Public search endpoint with pagination and core filter fields is already usable. | Basic search/filter is covered. Remaining gaps belong to `F-004`, not this base feature. |
| `F-004` | Advanced Filter | Must | `Partial` | Technical filters now cover brand, category, brake, frame material, condition, price, province, frame size, wheel size, groupset, and verified status. | The MVP no longer plans `hasVideo` filtering, but that field is still absent compared against the full SRS. |
| `F-005` | Bike Detail View | Must | `Partial` | Product detail now returns listing data with images, real verified badge state, and a public inspection summary/report block when inspection data exists. | Seller trust depth and some remaining SRS detail fields are still incomplete. Video playback/media expansion is deferred from MVP but not delivered in the full SRS sense. |
| `F-006` | Messaging System | Must | `Partial` | Conversations, messages, REST endpoints, and WebSocket push are implemented. REST read/list flows derive user identity from Spring Security, the invalid latest-message JPQL was removed, STOMP `CONNECT/SEND/SUBSCRIBE` frames are protected by a JWT-based inbound channel interceptor, and controller/service regression tests now cover message routing, lowercase auth header handling, unknown-user rejection, unauthenticated subscribe rejection, and unread-marking guard rails. | Real-time integration coverage is still missing, and the chat module still needs broader reconnect/delivery persistence coverage end-to-end. |
| `F-007` | Wishlist | Should | `Partial` | Authenticated wishlist add, remove, and list endpoints now exist with repository/service/controller flow. Focused service tests now cover ownership guard, mapping, and delete behavior. | Richer product-state and notification behavior from the SRS is still thin. |
| `F-008` | Deposit & Order | Must | `Partial` | Buyers can create orders with `partial/full` upfront intent, sellers can accept them, and authorized completion/cancellation paths now respect fund-hold states. Product status is updated to `sold` on completion. | Remaining payment phase, escrow-grade release policy, richer dispute handling, and integration-level tests are still missing. |
| `F-009` | Seller Rating | Must | `Partial` | Review endpoints and service exist. User aggregate rating fields are present, and review submission is now tied to the authenticated user with a real order lifecycle behind it. | The broader order/payment flow is still incomplete, and there is no automated coverage for review eligibility. |
| `F-010` | Inspection System | Should | `Partial` | Inspection request, evaluation, and fetch flows exist. Re-request now resets the existing inspection record, validity remains 7 days, and product verified state is derived from valid passed inspections. | The inspection/report model still shares too much state with product status, and deeper dispute/report workflow remains thin. |
| `F-011` | Admin Dashboard | Must | `Partial` | Dashboard stats endpoint exists. Admin user management now covers list/filter/detail/status change/reset password/activity, product moderation now has filter + approve/hide shortcuts, and admin CRUD now exists for brands, categories, brake types, and frame materials. Report processing also records which admin handled a case plus when it was processed. | Dispute resolution breadth, richer analytics, and the remaining technical master-data modules such as groupset/size chart are still missing. |
| `F-012` | Report System | Must | `Partial` | Report submission, reporter-side listing, admin listing with filter hooks, and process endpoints exist. Report processing now stores admin note, processor, processed time, and can notify the reporter/affected side after sanctions. Duplicate open reports from the same reporter to the same target are now blocked. | Moderation depth is better, but richer sanctions workflow, appeal/dispute loops, and integration tests are still missing. |
| `F-013` | Notification System | Must | `Partial` | Notification center endpoints and service are implemented. User-facing endpoints now use the authenticated user, single-notification read verifies ownership, and focused service tests now cover push/send, ownership rejection, and unread counting. | Event coverage is still limited and there are still no integration-level tests. |
| `F-014` | Chatbot Support | Could | `Missing` | No backend module. | Entire feature is absent. |
| `F-015` | Logistics Integration | Could | `Missing` | No backend module. | Entire feature is absent. |
| `F-016` | Online Payment | Could | `Partial` | A usable phase-1 flow now exists: create payment request, generate transfer instructions and QR, receive SePay-style webhook confirmation, persist payment records, and support refund request/admin review. Non-mock SePay order creation is now implemented via configurable API integration, webhook validation accepts both secret-header and legacy API-key style callbacks, and the current MBBank environment has now been live-verified through ngrok-routed IPN using the direct-transfer fallback path. Webhook-only regression now also covers malformed JSON, missing code, and snake_case payload variants. | Remaining-payment phase, payout/release automation, full refund automation, and richer bank/gateway-specific payment depth are still missing. |

## Readiness Gaps By Layer

| Layer | Status | Assessment |
| --- | --- | --- |
| Database schema and migrations | `Partial` | Runtime enum naming now matches lowercase PostgreSQL enum values, `V1-V8` has been reconciled on the active Supabase environment, and backend startup now validates against the real schema. Remaining risk is future environment drift, not the current primary environment. |
| Authorization and ownership | `Partial` | Notification, inspection, review, report, REST chat, and STOMP chat flows now derive identity from authenticated context instead of caller-supplied IDs. A few deeper business edges still need hardening. |
| Business-rule enforcement | `Partial` | Transaction rules remain stronger, and `BR01-BR07` is materially improved: required technical fields, minimum images, listing expiry, soft delete, verified derivation, and inspection invalidation now exist in backend logic. Remaining MVP gaps are richer moderation/admin flow and deeper order/admin rules. Video support is now a deliberate post-MVP item, though still a gap versus the full SRS. |
| Automated testing | `Partial` | The suite now includes focused service tests for `OrderServiceImpl`, `PaymentServiceImpl`, `RefundServiceImpl`, `AuthService`, `WishlistServiceImpl`, `NotificationServiceImpl`, `MessageServiceImpl`, and `ReportServiceImpl`, plus controller-level tests for `AuthController`, `ChatController`, `AdminProductController`, `ReferenceDataController`, and admin-user flows, in addition to the context-load test. Real smoke testing has improved confidence further, but automated integration coverage is still thin. |
| API and DX foundations | `Partial` | Swagger, API wrapper, and exception handling exist, but the API surface is still inconsistent in a few newer modules. |

## Main Findings

1. The old report understated the repository breadth. The backend is not a tiny skeleton anymore.
2. The old report also understated the amount of unfinished work that still blocks SRS-ready delivery.
3. The transaction layer has improved materially: `Order`, `Payment`, and `Refund` now form a usable phase-1 business flow instead of isolated entities.
4. The schema drift problem is materially better: the active Supabase environment is now reconciled through `V8`, and runtime startup has been verified against it.
5. Storage-backed product flows are stronger now that create, update-image replacement, and delete cleanup have all been smoke-tested against Supabase Storage.
6. Report/admin depth is better now that report processing stores admin audit data and blocks duplicate open reports from the same reporter to the same target.
7. Admin user tooling and reference-data breadth are materially better: FE now has real admin APIs for users, product moderation, brands, categories, brake types, and frame materials.
8. The largest remaining MVP quality gaps are now broader integration coverage, review/reply depth, groupset/size-chart management, and richer payment depth beyond the initial SePay live path.
9. Video/media support remains a full-SRS gap, but it is no longer a near-term MVP milestone.

## Recommended Next Milestones

### Milestone 1 - Reach 76%

- Add seller reply-to-review and fill the remaining seller-facing listing workflow gaps.
- Add groupset and size-chart reference-data management if FE admin screens need the full SRS breadth.
- Add broader regression tests for search filtering and notification/report integration.

### Milestone 2 - Reach 79%

- Add remaining-payment and payout-release rules if the product direction still wants staged payments.
- Expand report appeal/dispute handling and richer sanctions workflows.

### Milestone 3 - Reach 82%+

- Introduce repeatable integration testing and CI verification for WebSocket + payment.
- Revisit deferred full-SRS items such as video/media depth where product scope requires them.

## Deferred From MVP

- Product video upload/storage
- `hasVideo` filtering
- Video playback/detail-media expansion
