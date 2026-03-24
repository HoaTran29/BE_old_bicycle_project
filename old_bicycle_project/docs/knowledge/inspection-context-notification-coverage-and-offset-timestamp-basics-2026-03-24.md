# Inspection Context, Notification Coverage, and Offset Timestamps Basics

## What changed

- Added `GET /api/inspections/product-context/{productId}` for `INSPECTOR` and `ADMIN`.
- Inspection form can now load non-public products that are in `pending_inspection`.
- Notification DTO timestamps now return `OffsetDateTime` instead of timezone-naive `LocalDateTime`.
- Added admin/inspector notifications for intervention-required events:
  - seller listing enters `pending` moderation
  - admin sends listing to inspection
  - buyer creates refund request
  - user submits report
  - manual payout enters `pending_transfer`

## Why

- Inspector UI was calling the public product detail endpoint, which correctly hides non-public listings and caused `Product not found`.
- Naive notification timestamps caused browser-side timezone drift, especially on deployed environments running in UTC.
- Admin and inspector were missing several events that require direct action, so notification coverage was incomplete.

## Impact

- Inspectors can open the inspection form for queued listings without relying on marketplace visibility.
- Notification relative time on FE now uses the real server timestamp with offset.
- Admin and inspector inboxes receive the core moderation/review/transfer tasks they need to act on.
