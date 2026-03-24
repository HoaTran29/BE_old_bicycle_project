# Regression trọng tâm cho Auth, Chat, Payment: giải thích rõ cho người mới học

## 1. Regression trọng tâm là gì?

`Regression test` là kiểm tra lại những phần hệ thống dễ bị vỡ sau khi mình vừa sửa code.

Trong đợt này, ba cụm cần khóa chất lượng là:

- `auth`
- `chat`
- `payment`

Lý do:

- `auth` liên quan đăng nhập và phiên người dùng
- `chat` liên quan quyền truy cập conversation và unread flow
- `payment` vừa đổi sang `SePay webhook-only`, nên phải kiểm tra lại rất kỹ

## 2. Luồng refresh token trong Auth

Các file chính:

- `src/main/java/com/backend/old_bicycle_project/controller/AuthController.java`
- `src/main/java/com/backend/old_bicycle_project/service/AuthService.java`
- `src/main/java/com/backend/old_bicycle_project/service/RefreshTokenService.java`
- `src/main/java/com/backend/old_bicycle_project/repository/RefreshTokenRepository.java`

```mermaid
sequenceDiagram
    participant Client
    participant AuthController
    participant AuthService
    participant RefreshTokenService
    participant RefreshTokenRepository
    participant Database
    participant JwtTokenProvider

    Client->>AuthController: POST /api/auth/refresh
    AuthController->>AuthService: refreshToken(request)
    AuthService->>RefreshTokenService: findByToken(refreshToken)
    RefreshTokenService->>RefreshTokenRepository: findByToken(token)
    RefreshTokenRepository->>Database: SELECT refresh_tokens
    Database-->>RefreshTokenRepository: refresh token row
    RefreshTokenRepository-->>RefreshTokenService: RefreshToken
    RefreshTokenService-->>AuthService: RefreshToken

    alt Refresh token hết hạn
        AuthService->>RefreshTokenService: deleteAllByUser(user)
        RefreshTokenService->>RefreshTokenRepository: deleteAllByUser(user)
        RefreshTokenRepository->>Database: DELETE refresh_tokens
        AuthService-->>AuthController: throw UNAUTHENTICATED
        AuthController-->>Client: 401/4xx
    else Refresh token còn hạn
        AuthService->>JwtTokenProvider: generateAccessToken(user)
        JwtTokenProvider-->>AuthService: access token mới
        AuthService-->>AuthController: AuthResponse
        AuthController-->>Client: access token mới
    end
```

### Giải thích

- Client không gọi database trực tiếp.
- `AuthController` chỉ nhận request và gọi service.
- `AuthService` là nơi quyết định refresh token còn hạn hay không.
- `RefreshTokenRepository` chỉ lo đọc và xóa dữ liệu trong bảng `refresh_tokens`.

Regression batch này khóa lại hai rule quan trọng:

1. token còn hạn thì phải trả `access token` mới
2. token hết hạn thì phải revoke session cũ, không được tiếp tục dùng

## 3. Luồng gửi tin nhắn trong Chat

Các file chính:

- `src/main/java/com/backend/old_bicycle_project/controller/ChatController.java`
- `src/main/java/com/backend/old_bicycle_project/service/impl/MessageServiceImpl.java`
- `src/main/java/com/backend/old_bicycle_project/repository/ConversationRepository.java`
- `src/main/java/com/backend/old_bicycle_project/repository/MessageRepository.java`

```mermaid
sequenceDiagram
    participant Client as STOMP Client
    participant ChatController
    participant MessageServiceImpl
    participant ConversationRepository
    participant MessageRepository
    participant Database
    participant Broker as WebSocket Broker

    Client->>ChatController: /app/chat.sendMessage
    ChatController->>ChatController: resolveSenderId(principal)
    ChatController->>MessageServiceImpl: sendMessage(request, senderId)
    MessageServiceImpl->>ConversationRepository: findById(conversationId)
    ConversationRepository->>Database: SELECT conversation
    Database-->>ConversationRepository: conversation
    MessageServiceImpl->>MessageServiceImpl: validateParticipant(...)
    MessageServiceImpl->>MessageRepository: save(message)
    MessageRepository->>Database: INSERT message
    MessageServiceImpl->>ConversationRepository: save(updated conversation)
    ConversationRepository->>Database: UPDATE conversations.updated_at
    MessageServiceImpl-->>ChatController: MessageResponseDTO
    ChatController->>Broker: /topic/conversation/{id}
    ChatController->>Broker: /user/{recipientId}/queue/messages
```

### Giải thích

- `principal` từ WebSocket phải map được về `UUID` người gửi.
- Nếu principal lỗi hoặc không phải UUID hợp lệ, controller phải chặn sớm.
- Service phải kiểm tra người gửi có thật sự là buyer hoặc seller của conversation không.
- Sau khi lưu message, hệ thống mới broadcast lại lên WebSocket broker.

Regression batch này khóa lại ba rule quan trọng:

1. principal không hợp lệ phải trả `UNAUTHENTICATED`
2. người ngoài conversation không được gửi tin nhắn
3. gửi thành công thì phải lưu message, cập nhật conversation, và publish notification

## 4. Luồng SePay WebHook trong Payment

Các file chính:

- `src/main/java/com/backend/old_bicycle_project/controller/PaymentController.java`
- `src/main/java/com/backend/old_bicycle_project/service/impl/PaymentServiceImpl.java`
- `src/main/java/com/backend/old_bicycle_project/repository/PaymentRepository.java`
- `src/main/java/com/backend/old_bicycle_project/repository/OrderRepository.java`

```mermaid
sequenceDiagram
    participant SePay
    participant PaymentController
    participant PaymentServiceImpl
    participant PaymentRepository
    participant OrderRepository
    participant Database

    SePay->>PaymentController: POST /api/payments/sepay/webhook
    Note over SePay,PaymentController: Authorization: Apikey <webhook key>
    Note over SePay,PaymentController: Body: { code, transferType, transferAmount, ... }

    PaymentController->>PaymentServiceImpl: handleSepayWebhook(rawPayload, authorizationHeader)
    PaymentServiceImpl->>PaymentServiceImpl: validateWebhookAuthorization(...)
    PaymentServiceImpl->>PaymentServiceImpl: resolveWebhookPayload(...)
    PaymentServiceImpl->>PaymentRepository: findByGatewayOrderCode(code)
    PaymentRepository->>Database: SELECT payment
    Database-->>PaymentRepository: payment
    PaymentServiceImpl->>PaymentServiceImpl: confirmSuccessfulPayment(...)
    PaymentServiceImpl->>PaymentRepository: save(payment success)
    PaymentRepository->>Database: UPDATE payments
    PaymentServiceImpl->>OrderRepository: save(order deposited/held)
    OrderRepository->>Database: UPDATE orders
    PaymentServiceImpl-->>PaymentController: done
    PaymentController-->>SePay: 200 OK
```

### Giải thích

Luồng `webhook-only` hiện tại làm ba việc cốt lõi:

1. xác thực request bằng `Authorization`
2. đọc `gatewayOrderCode` từ `code`
3. chỉ cập nhật payment/order khi giao dịch là `tiền vào` và số tiền đủ

`validateWebhookAuthorization(...)` lấy key kỳ vọng từ config local:

- `application.properties`
- bind sang `SepayProperties`
- rồi `PaymentServiceImpl` gọi `sepayProperties.getWebhookApiKey()`

Nếu header không khớp:

- request bị reject
- payment không đổi trạng thái
- order không bị đánh dấu thanh toán thành công giả

Regression batch này khóa lại bốn rule quan trọng:

1. auth sai thì reject
2. thiếu tiền thì reject
3. giao dịch `transferType = out` thì bỏ qua
4. webhook lặp lại không được làm hỏng trạng thái đã `success`

## 5. Regression đang bảo vệ điều gì?

Nếu không có regression test, các lỗi kiểu này rất dễ quay lại:

- refresh token hết hạn nhưng vẫn lấy được access token mới
- WebSocket principal lỗi làm nổ controller bằng exception thô
- người ngoài conversation vẫn gửi được message
- webhook SePay với key giả vẫn update order thành `deposited`
- webhook gửi lặp lại làm order/payment đổi trạng thái sai

## 6. Batch này kiểm tra những file test nào?

- `src/test/java/com/backend/old_bicycle_project/service/AuthServiceTest.java`
- `src/test/java/com/backend/old_bicycle_project/controller/ChatControllerTest.java`
- `src/test/java/com/backend/old_bicycle_project/service/impl/MessageServiceImplTest.java`
- `src/test/java/com/backend/old_bicycle_project/service/impl/PaymentServiceImplTest.java`

## 7. Chốt ngắn

Batch regression trọng tâm này không nhằm “có thêm tính năng”.

Nó nhằm đảm bảo:

- `auth` không hỏng phiên đăng nhập
- `chat` không hỏng ownership và unread flow cơ bản
- `payment` không ghi nhận thanh toán giả sau khi chuyển sang `webhook-only`
