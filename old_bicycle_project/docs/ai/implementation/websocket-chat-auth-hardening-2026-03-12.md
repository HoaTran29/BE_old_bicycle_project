# WebSocket Chat Auth Hardening

Date: 2026-03-12  
Scope: Secure STOMP chat identity binding for `old_bicycle_project`

## Summary

This pass hardened the chat WebSocket flow so the sender identity no longer comes from the client payload. The server now authenticates STOMP sessions with JWT during `CONNECT`, assigns a user principal based on the authenticated user's UUID, and uses that principal for message sending and private queue routing.

## Delivered Changes

### 1. STOMP authentication binding

- Added `WebSocketAuthChannelInterceptor`.
- `CONNECT` now requires a `Bearer` token in STOMP native headers.
- The interceptor validates the JWT, loads the user, and assigns a WebSocket principal whose name is the user's UUID.

### 2. Private queue routing alignment

- Added `StompUserPrincipal`.
- The principal name is the user ID string, which aligns with the existing `convertAndSendToUser(userId.toString(), ...)` usage in chat and notifications.

### 3. Chat sender hardening

- `ChatController` now resolves sender identity from the authenticated WebSocket principal.
- `MessageService` and `MessageServiceImpl` now receive `senderId` from server-side auth context instead of trusting `MessageRequestDTO.senderId`.
- `MessageRequestDTO.senderId` remains in the payload model temporarily for backward compatibility, but it is no longer the source of truth.

### 4. Tests

- Added `WebSocketAuthChannelInterceptorTest`.
- Covered:
  - valid `CONNECT` with bearer token
  - rejected `CONNECT` without token
  - rejected `SEND` without authenticated user

## Verification

Commands used:

```powershell
java -version
cmd /c ".\mvnw.cmd -version"
```

Current shell state before override:

- terminal default Java: 17
- Maven wrapper default Java: 17

Explicit verification for this task:

```powershell
$env:JAVA_HOME='C:\Program Files\Eclipse Adoptium\jdk-21.0.9.10-hotspot'
$env:Path="$env:JAVA_HOME\bin;" + [System.Environment]::GetEnvironmentVariable('Path','Machine') + ';' + [System.Environment]::GetEnvironmentVariable('Path','User')
cmd /c ".\mvnw.cmd clean test"
```

Result:

- Passed on Java 21
- Spring context log confirmed runtime `Java 21.0.9`

## Client Impact

STOMP clients must now send the access token during `CONNECT`, for example:

```javascript
client.activate({
  connectHeaders: {
    Authorization: `Bearer ${accessToken}`
  }
});
```

The client should stop treating `senderId` in the outgoing message as authoritative. The backend now derives sender identity from the authenticated WebSocket session.

## Remaining Gaps

- No end-to-end real-time integration test yet.
- Chat unread badge and delivery semantics still need broader regression coverage.
- Default shell environment on this machine still points to Java 17, so future commands must either use a refreshed shell or set `JAVA_HOME` explicitly.
