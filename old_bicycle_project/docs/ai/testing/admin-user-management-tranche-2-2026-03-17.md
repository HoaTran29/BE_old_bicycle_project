# Admin User Management Tranche 2 - 2026-03-17

## Scope

Target SRS item:

- `FR-ADM-001: User Management`

Implemented in this tranche:

- `PATCH /api/admin/users/{id}/password`
- `GET /api/admin/users/{id}/activity`

This tranche closes the remaining gaps from tranche 1:

- admin reset password
- user activity view

## What Changed

- Added admin password reset endpoint with the same password policy used by auth flows.
- Reused a shared `PasswordPolicyValidator` so register, reset-password, change-password, and admin reset all follow one rule.
- Revoked all existing refresh tokens after an admin resets a user's password.
- Added a user-activity summary endpoint for admin screens.
- Aggregated counts and recent items from products, orders, reports, notifications, wishlists, and conversations.

## Verification

Targeted tests passed:

- `AuthServiceTest`
- `AdminUserControllerTest`
- `AdminUserServiceImplTest`
- `SecurityConfigIntegrationTest`

Command used:

```powershell
./mvnw.cmd -q "-Dtest=AuthServiceTest,AdminUserControllerTest,AdminUserServiceImplTest,SecurityConfigIntegrationTest" test
```

## Notes

- A regression appeared after introducing `PasswordPolicyValidator` because `AuthServiceTest` still mocked the validator as a no-op. The fix was to change that dependency to a `@Spy` so the real password policy is exercised in unit tests.
- Runtime OpenAPI recheck was not completed in this tranche because `localhost:8080` was not responding at the time of the follow-up verification. The compile and behavior checks above passed.
