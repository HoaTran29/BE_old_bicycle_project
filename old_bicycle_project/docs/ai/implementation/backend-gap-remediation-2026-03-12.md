# Backend Gap Remediation Summary - 2026-03-12

## Scope completed

This document records the work completed for the backend remediation pass requested on 2026-03-12.

## 1. Documentation and agent rules

- Updated `AGENTS.md` to require agents to explicitly tell the user when they use an agent, skill, or workflow.
- Added a new `Knowledge Capture` section in `AGENTS.md`.
- Required new beginner-friendly Vietnamese knowledge notes under `docs/knowledge/` after substantial engineering tasks.
- Added the rule that existing knowledge notes must be checked before creating duplicates.

## 2. Assessment and report alignment

- Updated `docs/ai/requirements/be-assessment-current.md`.
- Re-scored backend progress from `43%` to `54%` after the implemented remediation work.
- Replaced the old external report at:
  - `C:\Users\LapHub\.gemini\antigravity\brain\4a8ad914-2054-4fb8-9d2b-42ff52b13134\be_assessment.md.resolved`
  with the new assessment content.

## 3. Gap fixes implemented

### 3.1 Authorization and ownership hardening

- Reworked notification endpoints to use `@AuthenticationPrincipal User currentUser`.
- Reworked inspection request/evaluation to use the authenticated user instead of request-supplied IDs.
- Reworked review submission to use the authenticated reviewer.
- Reworked report submission to use the authenticated reporter.
- Reworked REST chat endpoints to use authenticated identity for listing conversations, reading messages, and marking messages read.
- Added conversation participant validation in `MessageServiceImpl`.
- Updated `SecurityConfig` so inspection route protection matches real routes.

### 3.2 Messaging runtime fix

- Removed the invalid JPQL query using `LIMIT 1`.
- Replaced it with a Spring Data derived query:
  - `findFirstByConversationIdOrderByCreatedAtDesc`

### 3.3 Wishlist usable flow

- Added `WishlistRepository`
- Added `WishlistService`
- Added `WishlistServiceImpl`
- Added `WishlistController`
- Added `WishlistItemResponseDTO`

Supported operations:

- list my wishlist
- add product to wishlist
- remove product from wishlist

### 3.4 Order and deposit usable flow

- Added `OrderCreateRequestDTO`
- Added `OrderResponseDTO`
- Added `OrderService`
- Added `OrderServiceImpl`
- Added `OrderController`
- Extended `OrderRepository`

Supported operations:

- create order
- list my orders
- confirm deposit
- complete order
- cancel order

Business effects added:

- seller/admin confirmation required for deposit state transition
- product status changes to `sold` when the order is completed

### 3.5 Schema drift reduction

- Normalized Java enum names to lowercase to match PostgreSQL enum values.
- Added `V4__align_runtime_schema.sql`.
- Added missing `product_status` values:
  - `pending_inspection`
  - `inspected_passed`
  - `inspected_failed`
- Added missing `users` columns:
  - `average_rating`
  - `total_reviews`
- Updated inspection validity from `6 months` to `7 days` to better align with the SRS.

## 4. Verification result

Attempted verification command:

```powershell
.\mvnw.cmd test
```

Result:

- Standard project verification with `.\mvnw.cmd test` is blocked because the current environment is running Java 17 while the project targets Java 21.
- Maven failed in that mode with: `release version 21 not supported`
- Compatibility verification succeeded with:

```powershell
cmd /c ".\mvnw.cmd clean -Dmaven.compiler.release=17 test"
```

- That compatibility run compiled the code and passed the single existing `contextLoads` test.

This means:

- the remediation changes do not show obvious compile/wiring issues in the current environment
- final verification on a real JDK 21 toolchain is still recommended

## 5. Remaining notable gaps

- Payment workflow is still missing.
- WebSocket chat sender identity is still not fully bound to authenticated sessions.
- Product and inspection business rules from the SRS are still incomplete.
- Automated tests remain extremely thin.

## 6. Knowledge note created

- `docs/knowledge/backend-gap-remediation-basics-2026-03-12.md`
