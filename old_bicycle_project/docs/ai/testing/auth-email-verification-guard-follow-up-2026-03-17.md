# Auth Email Verification Guard Follow-up - 2026-03-17

## Scope

- Repository: `BE_old_bicycle_project/old_bicycle_project`
- Focus: close the `isVerified` authentication gap highlighted in the API/SRS audit

## Change

The backend now blocks authentication for accounts that have not completed email verification.

Applied behavior:

- `login` rejects an unverified user even if email and password are correct
- `refresh token` also rejects an unverified user
- stale refresh tokens for that user are revoked on rejection

Files changed:

- `src/main/java/com/backend/old_bicycle_project/service/AuthService.java`
- `src/main/java/com/backend/old_bicycle_project/exception/ErrorCode.java`
- `src/test/java/com/backend/old_bicycle_project/service/AuthServiceTest.java`

## Why this matters

Before this follow-up:

- register created `isVerified = false`
- email verification flow existed
- but login did not actually enforce the verification state

That meant email verification existed as a feature, but not as a real access rule.

## Verification

Targeted suites run:

- `./mvnw.cmd -q -Dtest=AuthServiceTest test`
- `./mvnw.cmd -q "-Dtest=AuthControllerTest,AuthServiceTest" test`

Result:

- both passed
- only the usual Mockito dynamic-agent warning appeared on JDK 21

## Outcome

This closes one of the P0 backend gaps from the audit:

- `Bắt login phải tôn trọng isVerified`

It does **not** yet solve the other P0 items:

- admin user management
- clearer listing moderation depth
- broader integration coverage for realtime chat and payment webhook edge cases
