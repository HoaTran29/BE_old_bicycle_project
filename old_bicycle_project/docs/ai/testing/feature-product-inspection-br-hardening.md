---
phase: testing
title: Product + Inspection Business Rules Hardening Testing
description: Test strategy for Product and Inspection rule alignment
---

# Product + Inspection Business Rules Hardening Testing

## Test Coverage Goals

- Unit-test all new or changed Product and Inspection business rules.
- Cover public search/detail behavior, ownership-sensitive seller flows, and badge/report derivation.

## Unit Tests

### ProductService
- [x] Reject create when fewer than 3 images are provided
- [x] Set `expiresAt` to 30 days on create
- [x] Soft delete listing instead of hard delete
- [x] Return only seller-owned listings for `getMyProducts`
- [x] Derive verified badge and inspection summary correctly
- [x] Reset listing status and invalidate prior inspection on update

### Inspection rule helpers
- [x] Treat only passed and non-expired inspections as verified through ProductService mapping
- [x] Ignore invalid inspection state after update by expiring/invalidation logic

## Integration Tests

- [ ] Repository/specification behavior for public search vs deleted listings
- [ ] Filter behavior for verified and technical fields if unit tests are insufficient

## End-to-End Tests

- [ ] Seller creates valid listing
- [ ] Buyer views product detail with valid inspection report
- [ ] Seller soft deletes listing and it disappears from public results

## Test Data

- Mock product images and storage uploads
- Use explicit seller/product/inspection fixtures

## Test Reporting & Coverage

- Ran `.\mvnw.cmd -Dtest=ProductServiceTest test` with Java 21
- Ran full `.\mvnw.cmd test` with Java 21
- Added `ProductServiceTest` with 6 focused service-level tests
- Remaining gap: no repository/integration test yet for `ProductSpecification` and no runtime coverage for future video support

## Manual Testing

- Check multipart create/update requests
- Check public search after soft delete
- Check verified badge visibility before and after inspection expiry
