# Admin User Management Tranche 1 - 2026-03-17

## Scope

Target SRS item:

- `FR-ADM-001: User Management`

Implemented in this tranche:

- `GET /api/admin/users`
- `GET /api/admin/users/{id}`
- `PATCH /api/admin/users/{id}/status`

Deferred for a later tranche:

- reset password by admin
- view user activity

## What Changed

- Added admin user listing with keyword, role, status, and verified filters.
- Added admin user detail endpoint.
- Added admin status update endpoint for `active`, `unactive`, and `banned`.
- Added a self-protection rule so an admin cannot change their own account status.
- Extended repository support with `JpaSpecificationExecutor<User>`.

## Verification

Targeted tests passed:

- `AdminUserControllerTest`
- `AdminUserServiceImplTest`
- `SecurityConfigIntegrationTest`

Command used:

```powershell
./mvnw.cmd -q "-Dtest=AdminUserControllerTest,AdminUserServiceImplTest,SecurityConfigIntegrationTest" test
```

## Remaining Gaps To Reach Full FR-ADM-001

- admin password reset flow for a target user
- user activity view with an agreed definition of "activity"

## Risk Notes

- The new status endpoint is generic, so admin UIs must send the intended `UserStatus` value explicitly.
- Page responses still use Spring `Page` JSON directly, which is consistent with the current project but not the most stable public contract style.
