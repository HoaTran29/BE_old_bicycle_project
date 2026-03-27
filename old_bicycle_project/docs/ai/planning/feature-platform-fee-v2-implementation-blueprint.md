---
phase: planning
title: Platform Fee V2 Implementation Blueprint
description: Planning blueprint for implementing Policy V2 with fee base on total order value, buyer/seller split fee, buyer-fee refund on valid refunds, and revenue recognition after final completion
---

# Platform Fee V2 Implementation Blueprint

## Milestones

- [x] Milestone 1: Lock data model and business rules for `platform fee V2`
- [x] Milestone 2: Implement backend fee calculation, payment, payout, refund, and dashboard changes
- [ ] Milestone 3: Finish the last seller/E2E regression gaps and close the remaining UI polish items

## Task Breakdown

### Phase 1: Policy to Data Model
- [x] Add order-level fee snapshot fields: `fee_base_amount`, `platform_fee_rate`, `platform_fee_total`, `buyer_fee_amount`, `seller_fee_amount`, `buyer_charge_amount`, `seller_gross_payout_amount`, `seller_net_payout_amount`, `platform_fee_status`, `platform_fee_recognized_at`, `platform_fee_reversed_at`
- [x] Add payment-level trace fields for `protected_amount` and `buyer_fee_amount`, while keeping `amount` as the compatibility charge field
- [x] Add payout-level trace fields for `gross_amount`, `fee_deduction_amount`, and `net_amount`
- [x] Introduce a small immutable finance audit table `financial_transactions`
- [x] Define enum/status migration strategy so existing rows remain backward-compatible

### Phase 2: Backend Business Logic
- [x] Remove public trust in client-provided `serviceFee`; backend must calculate fee snapshot itself
- [x] Create a dedicated fee calculation service using `fee_base_amount = total_amount`
- [x] Enforce guard rule for `partial`: `required_upfront_amount >= seller_fee_amount`
- [x] Change payment request flow so buyer pays `required_upfront_amount + buyer_fee_amount` for `partial`, and `total_amount + buyer_fee_amount` for `full`
- [x] Persist payment breakdown so webhook reconciliation can distinguish `protected_amount` from `buyer_fee_amount`
- [x] Change seller payout flow to use `seller_net_payout_amount` instead of raw held amount
- [x] Change refund flow so valid refunds return `buyer_fee_amount` to buyer and reverse pending fee revenue
- [x] Add fee revenue recognition only when seller payout is truly completed and the order reaches the final settled state

### Phase 3: Admin Reporting and FE Contract
- [x] Replace ambiguous backend revenue metrics with `GMV`, `pending_platform_fee`, `recognized_platform_revenue`, and `reversed_platform_fee`, while keeping `totalRevenue` and `monthlyRevenue` as temporary GMV aliases for FE compatibility
- [x] Update buyer order creation and payment UI to show clear price breakdown
- [x] Update seller/admin payout UI to show `gross payout`, `seller fee deduction`, and `net payout`
- [x] Update dashboard wording so `doanh thu sàn` is not confused with `GMV`
- [x] Remove or hide legacy `serviceFee` request handling in public FE forms

### Phase 4: Verification and Documentation
- [x] Add focused backend tests for fee calculation, partial upfront validation, payment breakdown, seller payout deduction, refund reversal, and dashboard aggregation
- [x] Add FE tests for payout display helpers and dashboard wording/metrics
- [x] Add page-level FE regression coverage for the buyer order pricing dialog and buyer payment breakdown flow
- [x] Add a mocked cross-page FE buyer-flow integration regression for `product detail -> order -> payment request -> refund`
- [x] Extend refund request flow with multipart evidence upload support and admin dispute rendering for buyer-uploaded refund images
- [x] Mount real report entry points on `BikeDetailPage` so users can report `product` or `seller` without relying on hidden admin-only screens
- [x] Extend report flow with multipart image evidence upload support and render the evidence back in admin/my-report FE screens
- [x] Sync SRS sections for orders, payments, payouts, dashboard, and business rules
- [x] Keep the knowledge note for Policy V2 aligned with the current implementation slice

## Current Backend Status

Phase 1 completed on `2026-03-25`:

- additive Flyway migration `V19__platform_fee_v2_schema_foundation.sql`
- new backend enum/entity foundation for `PlatformFeeStatus` and `FinancialTransaction`
- updated response DTOs for order/payment/payout breakdown fields
- compatibility mapping so the current runtime can expose baseline `protected_amount`, `gross_amount`, and `net_amount`
- SRS sync for Policy V2 terminology, BR09, analytics metrics, and database schema

Phase 2 backend core completed on `2026-03-25`:

- new `PlatformFeeService` calculates Policy V2 fee snapshot on the server
- `OrderServiceImpl` no longer trusts client-provided `serviceFee`
- partial orders are blocked when `required_upfront_amount < seller_fee_amount`
- `PaymentServiceImpl` now charges `buyer_charge_amount` and reconciles `protected_amount` separately from buyer fee
- `PayoutServiceImpl` now uses `gross / fee deduction / net` and recognizes or reverses platform fee status at completion time
- `RefundServiceImpl` now refunds the buyer based on the real charged payment amount
- finance audit entries are now written for buyer charge receipt, fee recognition, and fee reversal

Phase 3 backend reporting completed on `2026-03-25`:

- `DashboardServiceImpl` now separates `totalGmv`, `pendingPlatformFee`, `recognizedPlatformRevenue`, and `reversedPlatformFee`
- `monthlyRecognizedPlatformRevenue` is now grouped by `platform_fee_recognized_at`
- `monthlyGmv` and `monthlyOrders` now use a completion proxy timestamp `COALESCE(platform_fee_recognized_at, updated_at, created_at)` instead of raw `created_at`
- legacy `totalRevenue` and `monthlyRevenue` are still returned as GMV aliases so the current FE does not break during migration
- focused backend test coverage was added for dashboard aggregation

Phase 3 frontend admin dashboard completed on `2026-03-25`:

- `AdminDashboardPage` now labels `GMV` separately from `doanh thu sàn đã ghi nhận`
- the FE no longer sums every month into a fake "tháng này" number
- the page reads explicit fields such as `totalGmv`, `pendingPlatformFee`, and `recognizedPlatformRevenue`, with fallback to legacy aliases during migration
- a focused FE test now protects the new dashboard wording and current-month aggregation behavior

Phase 3 frontend order and payout breakdown completed on `2026-03-25`:

- `BikeDetailPage` now previews Policy V2 fee split before order creation, including buyer fee, seller fee, buyer charge, and the partial-order minimum upfront guard
- `BuyerOrdersView` now renders buyer-side fee breakdown, payment request breakdown (`protectedAmount` vs `buyerFeeAmount`), and uses `buyerChargeAmount` for refund requests instead of legacy `paidAmount`
- `SellerOrdersPage` now shows seller-side fee deduction, gross payout, and expected net payout on order cards
- `AdminPayoutsPage` now surfaces `gross / fee deduction / net` in the table and payout completion dialog
- FE helper/test coverage now exists for `platform-fee-preview`, fee-aware order display helpers, admin payout rendering, and the buyer-side page flows in `BikeDetailPage` plus `BuyerOrdersView`

Phase 4 buyer-flow regression completed on `2026-03-25`:

- FE now has a mocked cross-page integration regression for the buyer journey from `BikeDetailPage` order creation to `BuyerOrdersView` payment instructions and refund request
- this regression does not replace a real browser E2E stack, but it closes the main FE contract gap for the Policy V2 buyer path with the current Vitest-based infrastructure

Follow-up dispute/report UX hardening completed on `2026-03-26`:

- refund requests now support multipart image uploads via `/api/orders/{orderId}/refunds`
- admin dispute detail now renders buyer-uploaded refund evidence images in addition to seller handover / buyer receipt order evidence
- `BikeDetailPage` now mounts real report actions for both product and seller targets instead of leaving report submit flow unreachable from normal buyer UI
- report submit now supports multipart image uploads via `/api/reports`
- admin report detail and `MyReportsPage` now render user-uploaded report evidence images
- focused backend + FE regression coverage was added for refund evidence upload forwarding, report evidence upload, and the live report entrypoint

Still intentionally pending:

- page-level FE regression coverage for the seller payout summary card
- browser-level E2E coverage beyond the current mocked FE integration regression

## Concrete Schema Proposal

This section locks the first implementation target for the data model so later code changes do not drift.

Principle:

- keep migrations additive first
- avoid destructive renames in the first rollout
- keep legacy columns alive during the transition
- let new code read and write the new fields while old fields remain as compatibility mirrors where needed

### Orders

Current order model already has:

- `total_amount`
- `deposit_amount`
- `required_upfront_amount`
- `paid_amount`
- `remaining_amount`
- `service_fee`

V2 adds these columns to `orders`:

- `fee_base_amount NUMERIC NOT NULL DEFAULT 0`
- `platform_fee_rate NUMERIC(5,4) NOT NULL DEFAULT 0`
- `platform_fee_total NUMERIC NOT NULL DEFAULT 0`
- `buyer_fee_amount NUMERIC NOT NULL DEFAULT 0`
- `seller_fee_amount NUMERIC NOT NULL DEFAULT 0`
- `buyer_charge_amount NUMERIC NOT NULL DEFAULT 0`
- `seller_gross_payout_amount NUMERIC NOT NULL DEFAULT 0`
- `seller_net_payout_amount NUMERIC NOT NULL DEFAULT 0`
- `platform_fee_status platform_fee_status NOT NULL DEFAULT 'not_applicable'`
- `platform_fee_recognized_at TIMESTAMP NULL`
- `platform_fee_reversed_at TIMESTAMP NULL`

Order-level meaning:

- `fee_base_amount`: amount used to calculate platform fee, which is `total_amount` in Policy V2
- `platform_fee_rate`: snapshot of the fee rate used when the order was created
- `platform_fee_total`: total platform fee for the whole trade
- `buyer_fee_amount`: buyer side of the platform fee
- `seller_fee_amount`: seller side of the platform fee
- `buyer_charge_amount`: what the buyer actually needs to transfer in the current payment flow
- `seller_gross_payout_amount`: payout before seller fee deduction
- `seller_net_payout_amount`: payout after seller fee deduction
- `platform_fee_status`: fee accounting state for dashboard and reconciliation

Recommended values for `platform_fee_status`:

- `not_applicable`
- `pending`
- `recognized`
- `reversed`

Compatibility note:

- keep `service_fee` for one migration cycle as a legacy column
- do not use `service_fee` as the source of truth in V2 logic
- only mirror `platform_fee_total` into `service_fee` temporarily if compatibility is needed

### Payments

Current payment model already has:

- `amount`
- `gateway`
- `method`
- `phase`
- `status`

V2 adds these columns to `payments`:

- `protected_amount NUMERIC NOT NULL DEFAULT 0`
- `buyer_fee_amount NUMERIC NOT NULL DEFAULT 0`

Payment-level meaning:

- `amount`: keep this column as the total charge amount for backward compatibility
- `protected_amount`: portion of the payment that counts toward the order amount being protected by the platform
- `buyer_fee_amount`: buyer side fee included in the charge

Recommended mapping in V2 runtime:

- `payments.amount = buyer_charge_amount`
- `payments.protected_amount = required_upfront_amount` for `partial`
- `payments.protected_amount = total_amount` for `full`
- `payments.buyer_fee_amount = orders.buyer_fee_amount`

### Payouts

Current payout model already has:

- `amount`
- `type`
- `status`
- bank transfer metadata

V2 adds these columns to `payouts`:

- `gross_amount NUMERIC NOT NULL DEFAULT 0`
- `fee_deduction_amount NUMERIC NOT NULL DEFAULT 0`
- `net_amount NUMERIC NOT NULL DEFAULT 0`

Payout-level meaning:

- `gross_amount`: payout before seller fee deduction
- `fee_deduction_amount`: seller-side platform fee taken from payout
- `net_amount`: actual transfer amount to recipient

Recommended mapping in V2 runtime:

- seller release payout:
  - `gross_amount = seller_gross_payout_amount`
  - `fee_deduction_amount = seller_fee_amount`
  - `net_amount = seller_net_payout_amount`
  - keep `amount` as a compatibility mirror of `net_amount`
- refund payout:
  - `gross_amount = buyer refund amount including buyer fee`
  - `fee_deduction_amount = 0`
  - `net_amount = gross_amount`
  - keep `amount` as a compatibility mirror of `net_amount`

### Financial Audit Table

Add a small immutable audit table instead of a full accounting subsystem in the first rollout:

- `financial_transactions`

Recommended columns:

- `id UUID PRIMARY KEY`
- `order_id UUID NULL`
- `payment_id UUID NULL`
- `payout_id UUID NULL`
- `refund_request_id UUID NULL`
- `entry_type VARCHAR(50) NOT NULL`
- `amount NUMERIC NOT NULL`
- `currency VARCHAR(3) NOT NULL DEFAULT 'VND'`
- `note TEXT NULL`
- `metadata JSONB NULL`
- `created_at TIMESTAMP NOT NULL DEFAULT now()`

Recommended `entry_type` values:

- `buyer_charge_received`
- `buyer_fee_refund_completed`
- `seller_release_payout_completed`
- `platform_fee_recognized`
- `platform_fee_reversed`

Scope note:

- this is an audit log, not double-entry accounting
- keep it append-only
- use it to explain how money moved without overbuilding the finance system

## Migration Strategy

### Step 1: Additive migration only

The first migration should only:

- add new columns
- add indexes if needed
- backfill safe defaults

It should not:

- drop `service_fee`
- rename `payments.amount`
- rename `payouts.amount`

### Step 2: Conservative backfill

Backfill rules for existing rows should be conservative:

- legacy rows default to `platform_fee_status = not_applicable`
- do not try to reconstruct historical platform fee unless the old row is unambiguous
- prefer explicit legacy exclusion over fake historical accuracy

### Step 3: Compatibility mirrors during transition

During the first implementation slice:

- `orders.service_fee` stays as a legacy compatibility field
- `payments.amount` remains the public amount field and now means total charge amount
- `payouts.amount` remains the public amount field and now mirrors `net_amount`

### Step 4: Cleanup later

Only after backend, FE, tests, and SRS are stable:

- remove legacy reads of `service_fee`
- decide whether `service_fee` should be dropped
- decide whether `payments.amount` and `payouts.amount` should remain mirrors or be renamed in a breaking change

## DTO Impact

The first V2 DTO update should expose enough breakdown to make FE truthful without breaking old consumers unnecessarily.

### Order response

Add to `OrderResponseDTO`:

- `feeBaseAmount`
- `platformFeeRate`
- `platformFeeTotal`
- `buyerFeeAmount`
- `sellerFeeAmount`
- `buyerChargeAmount`
- `sellerGrossPayoutAmount`
- `sellerNetPayoutAmount`
- `platformFeeStatus`
- `platformFeeRecognizedAt`
- `platformFeeReversedAt`

### Payment request/response

Add to `PaymentRequestResponseDTO` and `PaymentResponseDTO`:

- `protectedAmount`
- `buyerFeeAmount`

Keep:

- `amount` as the total charge amount

### Admin payout response

Add to `AdminPayoutResponseDTO`:

- `grossAmount`
- `feeDeductionAmount`
- `netAmount`

Keep:

- `amount` as a compatibility mirror of `netAmount`

### Dashboard response

Expose these explicit fields for admin reporting:

- `totalGmv`
- `pendingPlatformFee`
- `recognizedPlatformRevenue`
- `reversedPlatformFee`
- `monthlyGmv`
- `monthlyRecognizedPlatformRevenue`

Compatibility note:

- keep `totalRevenue` as a temporary alias of `totalGmv`
- keep `monthlyRevenue` as a temporary alias of `monthlyGmv`
- remove those aliases only after FE switches wording and contract usage

## Dependencies

- current order/payment/refund/payout architecture
- existing `PaymentPhase.upfront` runtime behavior
- existing manual payout flow with bank reference completion
- existing admin dashboard and FE payout/order detail screens
- Flyway migration discipline and enum compatibility

## Outcome

When this blueprint is completed, the system should have:

- a real platform fee model instead of a passive `service_fee` field
- buyer/seller split fee snapshots stored per order
- payment and payout records that explain where the money went
- dashboard metrics that distinguish `GMV` from `recognized platform revenue`
- refund handling that returns buyer fee correctly and reverses pending revenue

## Risks & Mitigation

- Risk: partial upfront values may be too small to cover seller fee deduction
  - Mitigation: enforce `required_upfront_amount >= seller_fee_amount` and prefer a minimum deposit ratio
- Risk: existing FE and tests assume `payment amount == held amount`
  - Mitigation: introduce explicit payment breakdown fields and migrate UI wording carefully
- Risk: dashboard numbers will look "smaller" after moving from `GMV` to real platform revenue
  - Mitigation: expose both metrics side by side instead of replacing one with the other silently
- Risk: monthly GMV currently has no dedicated `completed_at` column
  - Mitigation: use `COALESCE(platform_fee_recognized_at, updated_at, created_at)` as a completion proxy until a dedicated completion timestamp exists
- Risk: legacy rows created before V2 may not have fee snapshot data
  - Mitigation: treat old rows as legacy records with nullable/default fee fields and exclude them from strict V2 accounting where needed

## Representative File Areas

- Backend orders: `src/main/java/com/backend/old_bicycle_project/service/impl/OrderServiceImpl.java`
- Backend payments: `src/main/java/com/backend/old_bicycle_project/service/impl/PaymentServiceImpl.java`
- Backend payouts: `src/main/java/com/backend/old_bicycle_project/service/impl/PayoutServiceImpl.java`
- Backend refunds: `src/main/java/com/backend/old_bicycle_project/service/impl/RefundServiceImpl.java`
- Backend dashboard: `src/main/java/com/backend/old_bicycle_project/service/impl/DashboardServiceImpl.java`
- Frontend buyer order/payment UI: `old-bicycles-project/fe/src/pages/BikeDetailPage.tsx`
- Frontend admin dashboard: `old-bicycles-project/fe/src/pages/admin/AdminDashboardPage.tsx`
- Frontend admin payouts: `old-bicycles-project/fe/src/pages/admin/AdminPayoutsPage.tsx`
