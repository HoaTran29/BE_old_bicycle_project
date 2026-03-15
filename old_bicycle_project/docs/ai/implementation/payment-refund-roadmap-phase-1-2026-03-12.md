# Payment Refund Roadmap - Phase 1 Implementation

Date: 2026-03-12  
Scope: Transaction layer implementation aligned with the current backend roadmap

## Summary

This pass implemented a first usable payment and refund flow for the backend. The goal was not to build a full marketplace escrow engine, but to move the project from "order only" into a transaction model that can be upgraded later.

## Delivered Changes

### 1. Transaction domain upgrade

- Added order-level funding fields for `requiredUpfrontAmount`, `paidAmount`, `remainingAmount`, `paymentOption`, `fundingStatus`, `acceptedAt`, and `paymentDeadline`.
- Kept `depositAmount` for backward compatibility, but aligned it with the new upfront-payment model.
- Added `PaymentGateway`, `PaymentPhase`, `PaymentOption`, `OrderFundingStatus`, and `RefundStatus`.
- Added `RefundRequest` as a separate entity instead of overloading `Payment`.

### 2. Flyway migration

- Added `V5__payment_refund_upgrade.sql`.
- Extended `orders` and `payments`.
- Added `refund_requests`.
- Backfilled existing order financial fields.

### 3. Order flow hardening

- Added seller/admin `accept order`.
- Restricted direct manual `confirm-deposit` to `cash` only.
- Prevented direct cancellation of orders once funds are held or refund handling has started.
- Marked completion as settlement finished and release-ready at the order level.

### 4. Payment flow phase 1

- Added `PaymentController` and `PaymentService`.
- Added `POST /api/payments/orders/{orderId}/request`.
- Added `GET /api/payments/orders/{orderId}`.
- Added `POST /api/payments/sepay/webhook`.
- Payment request now returns:
  - order-linked payment record
  - transfer content
  - bank account info
  - VietQR image URL
  - expiry time
- Webhook confirmation now:
  - validates the gateway order code
  - updates payment status
  - updates order paid/remaining amounts
  - moves the order into `deposited` + `held`
  - emits buyer/seller notifications

### 5. Refund flow phase 1

- Added `RefundController` and `RefundService`.
- Added buyer refund request endpoint.
- Added admin refund review endpoint.
- Refund flow is intentionally `full refund only` in this phase.
- Refund completion now:
  - marks payment as `refunded`
  - cancels the order
  - marks funding as `refunded`

### 6. Tests

- Added focused service tests for:
  - `OrderServiceImpl`
  - `PaymentServiceImpl`
  - `RefundServiceImpl`
- This gives the project coverage for the most important state transitions in the new transaction layer.

## Verification

Command used:

```powershell
cmd /c ".\mvnw.cmd clean -Dmaven.compiler.release=17 test"
```

Result:

- Passed
- Test count confirmed:
  - `OrderServiceImplTest`: 1
  - `PaymentServiceImplTest`: 2
  - `RefundServiceImplTest`: 2
  - existing Spring context test: 1

## Known Limitations

- Real non-mock SePay outbound integration is not implemented yet.
- Remaining-payment phase is not implemented yet.
- Refund automation is still semi-manual at the business level.
- WebSocket chat sender identity still needs JWT/session binding.
- Product and inspection SRS business rules are still incomplete.

## Recommended Next Step

Follow the roadmap sequence already chosen:

1. Apply `V5` in real dev/staging.
2. Bind chat WebSocket identity to authenticated sessions.
3. Finish password reset and profile management.
4. Close `BR01-BR07` gaps in product and inspection modules.
