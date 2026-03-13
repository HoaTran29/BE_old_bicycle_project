# Auth Profile Password Reset - Phase 1

Date: 2026-03-13  
Scope: Complete missing `F-001` must-have account management features

## Summary

This pass completed the missing account-management pieces in the backend: password reset by email token, profile update, change password, and stronger password policy enforcement. The goal was to close the gap between the current auth implementation and the SRS must-have scope.

## Delivered Changes

### 1. Password reset flow

- Added `PasswordResetToken` entity and repository.
- Added `V6__password_reset_tokens.sql`.
- Added public endpoints:
  - `POST /api/auth/forgot-password`
  - `POST /api/auth/reset-password`
- Reset flow now:
  - creates a dedicated reset token
  - sends reset email
  - validates token expiry
  - updates password
  - revokes existing refresh tokens

### 2. Profile management flow

- Expanded `GET /api/auth/me` response with profile fields:
  - `phone`
  - `avatarUrl`
  - `defaultAddress`
- Added `PATCH /api/auth/profile`.
- Added `PATCH /api/auth/change-password`.

### 3. Password policy hardening

- Registration now requires:
  - minimum 8 characters
  - at least one uppercase letter
  - at least one number
- The same policy is enforced for:
  - reset password
  - change password

### 4. Security and routing

- Added `forgot-password` and `reset-password` to public auth endpoints in `SecurityConfig`.
- Kept profile update and change-password behind authenticated access.

### 5. Tests

- Added `AuthServiceTest`.
- Covered:
  - register with strong password
  - forgot password request
  - reset password
  - update profile
  - change password

## Verification

Command used:

```powershell
$env:JAVA_HOME='C:\Program Files\Eclipse Adoptium\jdk-21.0.9.10-hotspot'
$env:Path="$env:JAVA_HOME\bin;" + [System.Environment]::GetEnvironmentVariable('Path','Machine') + ';' + [System.Environment]::GetEnvironmentVariable('Path','User')
cmd /c ".\mvnw.cmd clean test"
```

Result:

- Passed on Java 21
- Test suite now includes:
  - `AuthServiceTest`: 5
  - `WebSocketAuthChannelInterceptorTest`: 3
  - `OrderServiceImplTest`: 1
  - `PaymentServiceImplTest`: 2
  - `RefundServiceImplTest`: 2
  - existing Spring context test: 1

## Known Limitations

- Password reset currently uses email token link, not SMS OTP.
- Auth API coverage is still service-level, not controller/integration-level.
- Email templates are functional but still basic.
- The shell default Java on this machine may still point to 17 unless `JAVA_HOME` is refreshed.

## Recommended Next Step

Continue the roadmap with the product and inspection business-rule cluster:

1. enforce `BR01-BR07`
2. expand filter/detail correctness
3. apply `V5` and `V6` in real dev/staging
