---
phase: deployment
title: SWP391 Schema Sync
description: Applied additive schema updates to the Supabase SWP391 project and verified the resulting state
---

# SWP391 Schema Sync

## Project

- Supabase project: `SWP391`
- Project id: `kfkzxghznwgbbarfsqre`

## What was checked first

- `flyway_schema_history` showed only versions `1` through `4`
- `payments` was still missing V5 columns such as:
  - `gateway`
  - `phase`
  - `gateway_order_code`
  - `checkout_url`
  - `qr_code_url`
- `products` was still missing:
  - `deleted_at`
- `refund_requests` was not present

## What was applied

- `V5__payment_refund_upgrade.sql`
- `V6__password_reset_tokens.sql`
- `V7__product_inspection_br_hardening.sql`

Applied via Supabase MCP migration execution against the target project.

## Verification

Verified after apply:

- `payments` now includes:
  - `gateway`
  - `phase`
  - `gateway_order_code`
  - `checkout_url`
  - `qr_code_url`
- `products` now includes:
  - `deleted_at`
- both tables now exist:
  - `refund_requests`
  - `password_reset_tokens`

## Runtime reconciliation completed

After the initial MCP apply, the schema had moved forward but `public.flyway_schema_history` was still only at `V4`.

This was later reconciled by running the backend against the same `SWP391` database and fixing the migration history issue that blocked Flyway validation.

Verified result:

- `flyway_schema_history` now contains `V1` through `V7`
- `V5__payment_refund_upgrade.sql` is recorded successfully
- `V6__password_reset_tokens.sql` is recorded successfully
- `V7__product_inspection_br_hardening.sql` is recorded successfully

This means the database schema and Flyway bookkeeping are now aligned again for the current repo state.

## Flyway issue found during runtime check

When the backend was started against `SWP391`, Flyway validation failed before `V5` to `V7` could be recorded.

The reason was:

- version `3` in `flyway_schema_history` still pointed to an older migration identity
- the current repo file for version `3` is `V3__update_schema_for_srs.sql`
- the database row for version `3` still described a different migration name/checksum

This was not repaired blindly.

Before reconciling the history row:

- the `inspections` columns expected by the current `V3` were verified to already exist
- the missing `products` indexes from the current `V3` were created first

Only after that was the Flyway history row for version `3` updated to match the repo, which allowed Flyway validation to pass and the later migrations to be recorded normally.

## Remaining issues observed

- Supabase security advisors still report many public tables with RLS disabled
- token-bearing tables such as `refresh_tokens`, `email_verifications`, and `password_reset_tokens` are still exposed without RLS from Supabase's perspective

## Scope clarification

- For this project, Supabase is currently treated as a managed Postgres host and storage provider only.
- The current backend architecture does not rely on Supabase Auth, Edge Functions, or direct client-side data access through Supabase APIs.
- Because of that, the RLS/security advisor findings are informative, but they are not part of the immediate delivery scope unless the project later decides to expose Supabase APIs directly.

## Suggested next step

1. Keep treating Supabase as managed Postgres only, unless the product direction explicitly decides to use Supabase APIs directly
2. Move to smoke testing the runtime flow:
   buyer login -> create order -> seller accept -> buyer request payment -> mock webhook
