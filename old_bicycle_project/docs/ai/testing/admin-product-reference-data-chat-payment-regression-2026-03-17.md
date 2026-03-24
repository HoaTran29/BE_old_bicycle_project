# Admin Product Moderation, Reference Data, And Edge Regression - 2026-03-17

## Scope

- Repository: `BE_old_bicycle_project/old_bicycle_project`
- SRS areas touched:
  - `UC24` Duyệt tin đăng
  - `UC25` Quản lý danh mục kỹ thuật
  - `FR-ADM-001` Quản lý người dùng
  - `FR-ADM-004` Quản lý danh mục kỹ thuật
  - chat realtime guard rails
  - SePay webhook-only edge cases

## Implementation Summary

### 1. Admin product moderation depth

Files:

- `src/main/java/com/backend/old_bicycle_project/controller/AdminProductController.java`
- `src/main/java/com/backend/old_bicycle_project/service/ProductService.java`
- `src/main/java/com/backend/old_bicycle_project/specification/ProductSpecification.java`
- `src/main/java/com/backend/old_bicycle_project/controller/ProductController.java`

Delivered:

- `GET /api/admin/products` now supports:
  - `status`
  - `sellerId`
  - `keyword`
  - `page`
  - `size`
- Added explicit admin endpoints:
  - `PATCH /api/admin/products/{id}/approve`
  - `PATCH /api/admin/products/{id}/hide`
- Added seller self-service endpoints:
  - `PATCH /api/products/{id}/hide`
  - `PATCH /api/products/{id}/show`
- Seller `show` does not bypass moderation:
  - current status must be `hidden`
  - relisting moves listing back to `pending`
  - `expiresAt` is renewed
- Admin moderation is blocked on transaction-sensitive product states:
  - `sold`
  - `pending_inspection`
  - `inspected_passed`
  - `inspected_failed`

### 2. Reference-data admin CRUD

Files:

- `src/main/java/com/backend/old_bicycle_project/controller/ReferenceDataController.java`
- `src/main/java/com/backend/old_bicycle_project/service/BrandService.java`
- `src/main/java/com/backend/old_bicycle_project/service/CategoryService.java`
- `src/main/java/com/backend/old_bicycle_project/service/BrakeTypeService.java`
- `src/main/java/com/backend/old_bicycle_project/service/FrameMaterialService.java`
- repositories for brand/category/brake/frame/product

Delivered:

- Public read endpoints return DTOs:
  - `GET /api/brands`
  - `GET /api/categories`
  - `GET /api/brake-types`
  - `GET /api/frame-materials`
- Admin CRUD endpoints now exist for:
  - brands
  - categories
  - brake types
  - frame materials
- Delete rules block removing reference data still used by products
- Category update/delete now enforces:
  - unique slug
  - no self-parent
  - no parent cycle
  - no delete while child categories or active products still depend on it

### 3. Chat realtime regression

Files:

- `src/test/java/com/backend/old_bicycle_project/controller/ChatControllerTest.java`
- `src/test/java/com/backend/old_bicycle_project/service/impl/MessageServiceImplTest.java`
- `src/test/java/com/backend/old_bicycle_project/security/WebSocketAuthChannelInterceptorTest.java`

Delivered:

- seller message routes unread push to buyer queue correctly
- `markMessagesAsRead` rejects users outside the conversation
- websocket auth now has coverage for:
  - lowercase `authorization`
  - invalid JWT
  - unknown user
  - unauthenticated `SUBSCRIBE`

### 4. Payment webhook edge regression

Files:

- `src/test/java/com/backend/old_bicycle_project/service/impl/PaymentServiceImplTest.java`

Delivered:

- malformed JSON is rejected cleanly
- payload without `code` is rejected
- snake_case webhook payload is parsed correctly
- numeric `id` fallback can become transaction reference when explicit reference is absent

## Verification

### Full suite

```powershell
./mvnw.cmd -q test
```

Result:

- pass
- known warnings remain around:
  - H2 + PostgreSQL enum/table DDL in context-load test
  - Mockito inline mock agent warning on JDK 21
- no failing tests in Maven exit code

### Security and controller coverage added

- anonymous user cannot call admin moderation APIs
- anonymous user cannot call seller hide/show APIs
- admin-only access is enforced for reference-data admin CRUD

## Runtime/Integration Notes

- No schema migration was needed for this tranche.
- Seller relist currently maps back to `pending`, not `active`.
- Admin moderation still uses the existing enum set:
  - `pending`
  - `active`
  - `hidden`
- The backend remains webhook-only for SePay callback handling. FE does not consume webhook directly.

## Residual Gaps After This Tranche

- seller reply-to-review API
- groupset and size-chart reference-data management
- deeper live integration coverage for WebSocket delivery and reconnect behavior
- payment depth beyond upfront phase
