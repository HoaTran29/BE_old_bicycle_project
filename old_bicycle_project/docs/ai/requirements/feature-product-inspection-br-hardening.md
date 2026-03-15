---
phase: requirements
title: Product + Inspection Business Rules Hardening
description: Close the remaining Product and Inspection gaps against FR-SELL-001/002, FR-BUY-003/004, and BR01-BR07
---

# Product + Inspection Business Rules Hardening

## Problem Statement

The current backend already has `Product` and `Inspection` modules, but key marketplace rules from the SRS are still only partially enforced.

Main gaps confirmed in code:

- `FR-SELL-001`, `BR01`, `BR02`, and `BR03` are incomplete in product creation because required technical fields, minimum image count, and `expires_at` are not enforced.
- `FR-SELL-002` is incomplete because `getMyProducts()` does not return the seller's own listings, and listing updates are not paired with stricter re-validation.
- `FR-BUY-003` is incomplete because advanced filter fields like frame size, wheel size, groupset, verified status, and video are not fully supported.
- `FR-BUY-004` and `BR07` are incomplete because product detail still hard-codes `isVerified = false`, and the public inspection report is not surfaced through the product detail response.
- Soft delete is still missing for listings.

## Goals & Objectives

- Enforce `BR01` to `BR03` in the seller listing flow.
- Make product detail and filter behavior reflect actual inspection data.
- Keep inspection validity aligned with `BR05` to `BR07`.
- Add a safe additive migration instead of editing old migrations.
- Add targeted regression tests for the changed rules.

## Non-Goals

- Full admin moderation redesign
- Video upload implementation
- Inspection dispute workflow expansion
- Reworking payment flow in this slice

## User Stories & Use Cases

- As a seller, I want product creation to reject incomplete technical specs so that my listing matches marketplace rules.
- As a seller, I want my listings endpoint to show only my own listings, including soft-deleted filtering behavior.
- As a buyer, I want to filter products by technical specs and verified status so that I can find the right bicycle faster.
- As a buyer, I want product detail to show whether the bike is currently verified and expose the inspection report when available.
- As an inspector-driven system flow, I want verified status to expire automatically when the inspection is no longer valid.

## Success Criteria

- Product creation rejects missing `frameSize`, `wheelSize`, and fewer than 3 images.
- New listings automatically set `expiresAt = createdAt + 30 days`.
- Product deletion becomes soft delete and hidden from public search.
- `GET /api/products/my` returns only the authenticated seller's active non-deleted listings.
- Product search supports frame size, wheel size, groupset, and verified filtering without exposing deleted listings.
- Product detail returns real verified status based on a valid passed inspection and includes inspection summary/report data when available.
- New or changed business rules have automated tests.

## Constraints & Assumptions

- Keep the current Java 21 / Spring Boot / PostgreSQL stack.
- Use additive Flyway migration only.
- Avoid breaking existing order/payment work on the current branch.
- Keep API changes incremental so frontend integration can adapt without a full rewrite.

## Questions & Open Items

- `FR-BUY-003` mentions video filter, but the current backend has no video entity/storage flow. This slice will support the filter field contract only if it can be derived safely; otherwise it remains documented as deferred.
- `BR06` says changing components invalidates the badge. The current model has no audit trail for component changes, so this slice will invalidate on listing update by resetting status rather than detecting part-level drift.
