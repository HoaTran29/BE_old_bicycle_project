# Backend Assessment - Current State

Date: 2026-03-12  
Scope: `BE_old_bicycle_project/old_bicycle_project` backend compared against `../SRS-Old-Bicycles-Marketplace (1).md`

## Executive Summary

The old backend assessment is no longer a reliable baseline. The current repository is broader than the old report suggested, and this pass closes four of the highest-value backend gaps, but the system is still materially behind the SRS in the areas that determine production readiness.

**Fixed backend progress assessment: 54%**

This number reflects SRS-aligned backend readiness, not just file count or module breadth.

### Why the score is not lower

- The backend already has real modules for auth, products, chat, inspection, review, report, notification, dashboard, and reference data.
- Flyway, Spring Security, Swagger, WebSocket chat, and a global exception handler are already present.
- The repo has moved beyond the old report's "early skeleton" stage.

### Why the score is not higher

- `Payment` is still not delivered as a usable backend flow.
- Several implemented modules are still only `Partial` because business rules, payment flow depth, WebSocket auth, or tests are missing.
- Schema drift is improved, but delivery confidence still depends on applying the new Flyway changes in the real environments.
- Automated verification is almost absent beyond a single context-load test.

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

Raw feature score from the SRS matrix below: **57%**

Readiness adjustment: **-3 points**

Reason for adjustment:

- Test coverage is still too thin to treat `Partial` modules as near-production.
- Order and messaging flows are now usable, but they still lack payment integration and automated regression checks.

Final assessed backend progress: **54%**

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
| `F-008` | Deposit & Order | Must | `Partial` | Buyers can now create orders, list their orders, and authorized sellers/admins can confirm deposit, complete, or cancel the order. Product status is updated to `sold` on completion. | No payment/escrow gateway integration, refund/dispute handling, or automated tests. |
| `F-009` | Seller Rating | Must | `Partial` | Review endpoints and service exist. User aggregate rating fields are present, and review submission is now tied to the authenticated user with a real order lifecycle behind it. | The broader order/payment flow is still incomplete, and there is no automated coverage for review eligibility. |
| `F-010` | Inspection System | Should | `Partial` | Inspection request, evaluation, and fetch flows already exist. | Verified badge lifecycle, report transparency, and validity rules do not match `BR05-BR07`. |
| `F-011` | Admin Dashboard | Must | `Partial` | Dashboard stats endpoint exists. Brand/category admin operations already started. | User management, listing moderation, dispute resolution, and richer admin analytics are still missing. |
| `F-012` | Report System | Must | `Partial` | Report submission, admin listing, and process endpoints exist. Submission is now tied to the authenticated reporter instead of caller-supplied user IDs. | Moderation depth, richer sanctions workflow, and tests are still missing. |
| `F-013` | Notification System | Must | `Partial` | Notification center endpoints and service are implemented. User-facing endpoints now use the authenticated user, and single-notification read now verifies ownership. | Event coverage is still limited and there are no automated tests. |
| `F-014` | Chatbot Support | Could | `Missing` | No backend module. | Entire feature is absent. |
| `F-015` | Logistics Integration | Could | `Missing` | No backend module. | Entire feature is absent. |
| `F-016` | Online Payment | Could | `Missing` | `Payment` entity exists. | No payment workflow, gateway integration, or usable API flow exists. |

## Readiness Gaps By Layer

| Layer | Status | Assessment |
| --- | --- | --- |
| Database schema and migrations | `Partial` | Runtime enum naming now matches lowercase PostgreSQL enum values, and a new Flyway migration aligns `product_status` plus seller rating columns. Confidence still depends on applying the migration in real environments. |
| Authorization and ownership | `Partial` | Notification, inspection, review, report, and REST chat flows now derive identity from Spring Security. WebSocket chat auth and a few deeper business edges still need hardening. |
| Business-rule enforcement | `Partial` | `BR01-BR07`, `BR11`, and parts of admin/order rules are not fully enforced in request validation or service logic. |
| Automated testing | `Missing` | Test suite currently provides only a context-load test, which is not enough for delivery confidence. |
| API and DX foundations | `Partial` | Swagger, API wrapper, and exception handling exist, but the API surface is still inconsistent in a few newer modules. |

## Main Findings

1. The old report understated the repository breadth. The backend is not a tiny skeleton anymore.
2. The old report also understated the amount of unfinished work that still blocks SRS-ready delivery.
3. The transaction layer has improved materially: `Wishlist` and `Order/Deposit` are no longer just entities, but `Payment` is still absent as a real backend flow.
4. The schema drift problem has been reduced, but it is not fully retired until the new migration is applied in actual environments.
5. The largest quality gap is missing automated tests.

## Recommended Next Milestones

### Milestone 1 - Reach 62%

- Add payment workflow or explicitly defer it with a documented non-goal.
- Add regression tests for order, wishlist, chat ownership, and notifications.
- Bind WebSocket chat sender identity to authenticated sessions instead of caller-provided payload IDs.

### Milestone 2 - Reach 70%

- Close `BR01-BR07` enforcement gaps in product and inspection flows.
- Complete password reset and profile management.

### Milestone 3 - Reach 78%+

- Expand admin moderation and dispute resolution coverage.
- Introduce repeatable integration testing and CI verification.
