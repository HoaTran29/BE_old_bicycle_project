---
phase: planning
title: Product + Inspection Business Rules Hardening Plan
description: Implementation plan for closing Product and Inspection rule gaps
---

# Product + Inspection Business Rules Hardening Plan

## Milestones

- [x] Milestone 1: Lock requirement and design scope for `FR-SELL-001/002`, `FR-BUY-003/004`, `BR01-BR07`
- [x] Milestone 2: Implement Product and Inspection rule fixes with additive migration
- [x] Milestone 3: Add regression tests and update implementation knowledge docs

## Task Breakdown

### Phase 1: Product Rule Enforcement
- [x] Add request/service validation for required technical fields, minimum image count, and `expiresAt`
- [x] Fix seller-owned listing flow for `GET /api/products/my`
- [x] Replace hard delete with soft delete

### Phase 2: Inspection Integration
- [x] Derive verified badge from valid passed inspections
- [x] Expose public inspection summary/report on product detail
- [x] Expand filter support for frame size, wheel size, groupset, and verified
- [x] Record that video/media remains deferred from MVP because the backend still has no video storage/model and the team is intentionally reducing scope

### Phase 3: Persistence & Verification
- [x] Add Flyway migration for soft delete and any supporting indexes/columns
- [x] Add focused tests for ProductService and related rule behavior
- [x] Update implementation/testing notes and beginner knowledge note

## Dependencies

- Existing `Product`, `Inspection`, and storage flows
- Existing `product_status` enum values from `V4`
- Current branch `feat/ImplementingPayment` must remain compatible

## Timeline & Estimates

- Product rule enforcement: medium
- Inspection integration: medium
- Migration and tests: medium

## Outcome

- Code updated across controller, service, repository, DTO, specification, entity, migration, and tests.
- `.\mvnw.cmd test` passed on Java 21 after the slice was completed.
- Remaining MVP gap in this feature area: repository/integration coverage for search filtering and tighter moderation-state handling.
- Explicitly deferred from MVP: actual video upload/storage and therefore true `hasVideo` filtering.

## Risks & Mitigation

- Risk: Search behavior changes may hide more records than expected
  - Mitigation: keep filtering rules explicit and test seller/admin/public paths separately
- Risk: Soft delete may affect existing repository queries
  - Mitigation: centralize public filtering and seller listing queries
- Risk: Inspection validity could be misread after updates
  - Mitigation: encapsulate validity logic in a dedicated helper

## Resources Needed

- SRS sections for `FR-SELL`, `FR-BUY`, and `FR-INS`
- Existing Product/Inspection code
- Maven test suite with Java 21
