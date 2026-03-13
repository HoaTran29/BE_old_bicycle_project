---
phase: implementation
title: Product + Inspection Business Rules Hardening Implementation
description: Implementation notes for Product and Inspection rule alignment
---

# Product + Inspection Business Rules Hardening Implementation

## Development Setup

- Java 21
- Maven wrapper
- Spring Boot 3.4.x
- PostgreSQL + Flyway

## Code Structure

- Product API and service logic stay in the existing `controller`, `service`, `repository`, and `dto` packages.
- Inspection read-model integration is kept inside service mapping helpers instead of duplicating badge state across entities.

## Implementation Notes

### Core Features

- Product create now enforces required technical fields, minimum image count, and automatic `expiresAt` generation for 30 days.
- Product update now resets listing status to `pending`, revalidates technical fields, and invalidates any previous inspection badge.
- Product delete now performs soft delete with `deletedAt` instead of removing the row.
- Seller listing endpoint now uses seller-scoped repository queries instead of the old admin fallback.
- Product detail now derives `isVerified` from a real valid inspection and includes an inspection summary/report block.
- Product search now excludes soft-deleted rows and supports frame size, wheel size, groupset, and verified filtering.
- Inspection request now supports re-request flow by resetting the existing inspection record instead of blocking forever after the first request.

### Patterns & Best Practices

- Keep controllers thin.
- Keep validation close to request/service boundaries.
- Prefer additive migration and query helpers over editing legacy schema files.
- Derive read-model state such as verified badge from source data instead of storing duplicate mutable flags when possible.

## Integration Points

- Product writes depend on storage upload and product image persistence.
- Product reads depend on inspection validity lookup for public badge/report data.
- Inspection lifecycle now affects both product status and product detail read mapping.

## Error Handling

- Use existing application exception patterns where practical.
- Fail before upload or save when listing rules are violated.
- New explicit error codes were added for missing technical fields and insufficient images.

## Security Notes

- Seller-specific operations continue to derive identity from `AuthenticationPrincipal`.
- Public search/detail must not expose soft-deleted records.

## Deferred Items

- True video upload/storage is still missing, so `hasVideo` filtering is intentionally not claimed as complete.
- Product/inspection status modeling is still somewhat overloaded because moderation state and inspection state share one enum. This slice makes that behavior safer, but it does not redesign the whole state machine.
