# Backend Assessment - Current State

Date: 2026-03-12  
Scope: `BE_old_bicycle_project/old_bicycle_project` backend compared against `../SRS-Old-Bicycles-Marketplace (1).md`

## Executive Summary

The old backend assessment is no longer a reliable baseline. The current repository is broader than the old report suggested, and this pass moves the transaction layer from "order skeleton" into a usable first phase with acceptance, payment request, webhook confirmation, refund request, and admin review. The system is still behind the SRS in several must-have areas, but the backend is no longer missing a real payment path.

**Fixed backend progress assessment: 60%**

This number reflects SRS-aligned backend readiness, not just file count or module breadth.

### Why the score is not lower

- The backend already has real modules for auth, products, chat, inspection, review, report, notification, dashboard, and reference data.
- Flyway, Spring Security, Swagger, WebSocket chat, and a global exception handler are already present.
- The repo has moved beyond the old report's "early skeleton" stage.

### Why the score is not higher

- Several implemented modules are still only `Partial` because business rules, payment flow depth, WebSocket auth, or admin workflow depth are missing.
- The new payment flow is intentionally phase-1 simple: it supports upfront payment and refund handling, but not full gateway checkout orchestration, split payout, or automated refund execution.
- Schema drift is improved again, but delivery confidence still depends on applying the new Flyway changes in the real environments.
- Test coverage is better than before, but it is still service-level and far from full regression protection.

## Repository Snapshot

- 12 controllers
- 18 entities
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

Raw feature score from the SRS matrix below: **62%**

Readiness adjustment: **-2 points**

Reason for adjustment:

- A first payment/refund flow now exists, and targeted unit tests were added for order creation, payment request, webhook confirmation, and refund review.
- The system still lacks end-to-end integration tests, real gateway connectivity in non-mock mode, and deeper regression coverage.

Final assessed backend progress: **60%**

## SRS Matrix

| SRS ID | Module | Priority | Status | Current BE | What blocks `Done` |
| --- | --- | --- | --- | --- | --- |
| `F-001` | User Authentication | Must | `Partial` | Register, login, refresh, logout, email verification, and `/me` already exist. JWT and refresh-token flow are present. | Password reset and profile management are missing. Password policy is weaker than the SRS. |
| `F-002` | Bike Listing | Must | `Partial` | Product create, update, delete, search, and detail endpoints exist. Multipart image upload is already wired. | Soft delete, moderation flow, seller-only ownership paths, and `BR01-BR04` are not fully enforced. |
| `F-003` | Search & Filter | Must | `Done` | Public search endpoint with pagination and core filter fields is already usable. | Basic search/filter is covered. Remaining gaps belong to `F-004`, not this base feature. |
| `F-004` | Advanced Filter | Must | `Partial` | Technical filters exist for brand, category, brake, frame material, condition, price, and province. | Groupset, verified/video, frame-size, wheel-size, and full inspection-aware filtering are incomplete. |
| `F-005` | Bike Detail View | Must | `Partial` | Product detail endpoint exists and returns listing data with images. | Seller trust data, inspection transparency, and full SRS detail content are incomplete. |
| `F-006` | Messaging System | Must | `Partial` | Conversations, messages, REST endpoints, and WebSocket push are implemented. REST read/list flows now derive user identity from Spring Security and the invalid latest-message JPQL was removed. | WebSocket sender identity is still not bound to JWT/STOMP auth, and there is no regression coverage yet. |
| `F-007` | Wishlist | Should | `Partial` | Authenticated wishlist add, remove, and list endpoints now exist with repository/service/controller flow. | No tests yet, and richer product-state and notification behavior from the SRS is still thin. |
| `F-008` | Deposit & Order | Must | `Partial` | Buyers can create orders with `partial/full` upfront intent, sellers can accept them, and authorized completion/cancellation paths now respect fund-hold states. Product status is updated to `sold` on completion. | Remaining payment phase, escrow-grade release policy, richer dispute handling, and integration-level tests are still missing. |
| `F-009` | Seller Rating | Must | `Partial` | Review endpoints and service exist. User aggregate rating fields are present, and review submission is now tied to the authenticated user with a real order lifecycle behind it. | The broader order/payment flow is still incomplete, and there is no automated coverage for review eligibility. |
| `F-010` | Inspection System | Should | `Partial` | Inspection request, evaluation, and fetch flows already exist. | Verified badge lifecycle, report transparency, and validity rules do not match `BR05-BR07`. |
| `F-011` | Admin Dashboard | Must | `Partial` | Dashboard stats endpoint exists. Brand/category admin operations already started. | User management, listing moderation, dispute resolution, and richer admin analytics are still missing. |
| `F-012` | Report System | Must | `Partial` | Report submission, admin listing, and process endpoints exist. Submission is now tied to the authenticated reporter instead of caller-supplied user IDs. | Moderation depth, richer sanctions workflow, and tests are still missing. |
| `F-013` | Notification System | Must | `Partial` | Notification center endpoints and service are implemented. User-facing endpoints now use the authenticated user, and single-notification read now verifies ownership. | Event coverage is still limited and there are no automated tests. |
| `F-014` | Chatbot Support | Could | `Missing` | No backend module. | Entire feature is absent. |
| `F-015` | Logistics Integration | Could | `Missing` | No backend module. | Entire feature is absent. |
| `F-016` | Online Payment | Could | `Partial` | A usable phase-1 flow now exists: create payment request, generate transfer instructions and QR, receive SePay-style webhook confirmation, persist payment records, and support refund request/admin review. | Real outbound gateway integration, remaining-payment phase, payout/release automation, and full refund automation are still missing. |

## Readiness Gaps By Layer

| Layer | Status | Assessment |
| --- | --- | --- |
| Database schema and migrations | `Partial` | Runtime enum naming now matches lowercase PostgreSQL enum values, `V4` reduced previous schema drift, and `V5` adds order/payment/refund fields for the new transaction flow. Confidence still depends on applying the migrations in real environments. |
| Authorization and ownership | `Partial` | Notification, inspection, review, report, and REST chat flows now derive identity from Spring Security. WebSocket chat auth and a few deeper business edges still need hardening. |
| Business-rule enforcement | `Partial` | Transaction rules are stronger now: cash/manual and transfer/online paths are separated, held funds can no longer be cancelled directly, and refund flow is explicit. `BR01-BR07`, `BR11`, and deeper admin/order rules are still not fully enforced. |
| Automated testing | `Partial` | The suite now includes focused service tests for `OrderServiceImpl`, `PaymentServiceImpl`, and `RefundServiceImpl` in addition to the context-load test. Integration coverage is still thin. |
| API and DX foundations | `Partial` | Swagger, API wrapper, and exception handling exist, but the API surface is still inconsistent in a few newer modules. |

## Main Findings

1. The old report understated the repository breadth. The backend is not a tiny skeleton anymore.
2. The old report also understated the amount of unfinished work that still blocks SRS-ready delivery.
3. The transaction layer has improved materially: `Order`, `Payment`, and `Refund` now form a usable phase-1 business flow instead of isolated entities.
4. The schema drift problem has been reduced again, but it is not fully retired until `V4` and `V5` are applied in actual environments.
5. The largest remaining quality gaps are missing integration tests, incomplete SRS business rules in product/inspection flows, and missing WebSocket auth binding.

## Recommended Next Milestones

### Milestone 1 - Reach 66%

- Apply `V5__payment_refund_upgrade.sql` in dev/staging and wire the flow to real non-mock SePay configuration.
- Add regression tests for wishlist, chat ownership, notifications, and the new transaction endpoints.
- Bind WebSocket chat sender identity to authenticated sessions instead of caller-provided payload IDs.

### Milestone 2 - Reach 72%

- Close `BR01-BR07` enforcement gaps in product and inspection flows.
- Complete password reset and profile management.
- Add remaining-payment and payout-release rules if the product direction still wants staged payments.

### Milestone 3 - Reach 78%+

- Expand admin moderation and dispute resolution coverage.
- Introduce repeatable integration testing and CI verification.
