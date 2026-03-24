# FE API Integration Work Split - 2026-03-17

## Mục tiêu

Chia toàn bộ API hiện có cho 3 FE dev sao cho:

- không chồng chéo quá nhiều
- mỗi người có một cụm màn hình rõ ràng
- người chịu phần khó nhất là **bạn**
- nhóm có thể gắn đủ phần lớn API BE hiện tại vào FE

## Quy ước chung

### Response wrapper

Hầu hết REST API trả:

```json
{
  "code": 1000,
  "message": "optional",
  "result": {}
}
```

FE nên unwrap tại tầng API client, không để component phải tự đọc `result` mỗi lần.

### Auth

- REST protected endpoint: `Authorization: Bearer <accessToken>`
- WebSocket STOMP:
  - gửi `Authorization: Bearer <accessToken>` ở frame `CONNECT`
  - backend chấp nhận cả `Authorization` và `authorization`

### Pagination

Các API dùng Spring `Page<T>` sẽ trả trong `result`:

- `content`
- `number`
- `size`
- `totalElements`
- `totalPages`
- `first`
- `last`

### Enum/Status cần nhớ

- `ProductStatus`: `pending`, `active`, `hidden`, `sold`, `pending_inspection`, `inspected_passed`, `inspected_failed`
- `UserStatus`: `active`, `unactive`, `banned`
- `PaymentOption`: `partial`, `full`
- `PaymentMethod`: `transfer`, `cash`, `online`
- `OrderStatus`: `pending`, `deposited`, `completed`, `cancelled`
- `ReportStatus`: `pending`, `reviewed`, `resolved`

## Phân công

### FE Dev 1 - Bạn - phần khó nhất

Phụ trách:

- transaction flow
- payment/refund
- chat realtime
- admin product moderation

Lý do:

- đây là cụm nhiều state nhất
- có WebSocket + payment side effects + admin transition
- cần hiểu sâu business flow hơn phần CRUD thông thường

#### A. Admin product moderation

| API | Auth | Request | Result |
| --- | --- | --- | --- |
| `GET /api/admin/products?status=&sellerId=&keyword=&page=&size=` | `ADMIN` | query filter | `Page<ProductResponse>` |
| `PATCH /api/admin/products/{id}/approve` | `ADMIN` | none | `ProductResponse` |
| `PATCH /api/admin/products/{id}/hide` | `ADMIN` | none | `ProductResponse` |
| `PATCH /api/admin/products/{id}/status?status=active|hidden|pending` | `ADMIN` | query `status` | `ProductResponse` |

`ProductResponse.result` key fields:

- `id`
- `title`
- `description`
- `price`
- `originalPrice`
- `condition`
- `status`
- `province`
- `district`
- `frameSize`
- `wheelSize`
- `groupset`
- `createdAt`
- `expiresAt`
- `brandName`
- `categoryName`
- `brakeTypeName`
- `frameMaterialName`
- `isVerified`
- `seller`
  - `id`
  - `firstName`
  - `lastName`
  - `avatarUrl`
  - `phone`
- `images[]`
  - `id`
  - `url`
  - `isPrimary`
  - `displayOrder`
- `inspection`
  - `id`
  - `overallScore`
  - `passed`
  - `reportFileUrl`
  - `validUntil`
  - `createdAt`

Lưu ý:

- `approve` chỉ là shortcut của `status=active`
- `hide` chỉ là shortcut của `status=hidden`
- đừng assume có `rejected` enum riêng

#### B. Orders

| API | Auth | Request | Result |
| --- | --- | --- | --- |
| `POST /api/orders` | `BUYER` | `OrderCreateRequestDTO` | `OrderResponseDTO` |
| `GET /api/orders/me` | logged-in | none | `List<OrderResponseDTO>` |
| `PATCH /api/orders/{orderId}/accept` | `SELLER`/`ADMIN` | none | `OrderResponseDTO` |
| `PATCH /api/orders/{orderId}/confirm-deposit` | `SELLER`/`ADMIN` | none | `OrderResponseDTO` |
| `PATCH /api/orders/{orderId}/complete` | `SELLER`/`ADMIN` | none | `OrderResponseDTO` |
| `PATCH /api/orders/{orderId}/cancel` | `BUYER`/`SELLER`/`ADMIN` | none | `OrderResponseDTO` |

`OrderCreateRequestDTO`:

- `productId: UUID`
- `upfrontAmount?: BigDecimal`
- `depositAmount?: BigDecimal`
- `serviceFee?: BigDecimal`
- `paymentOption?: partial|full`
- `paymentMethod: transfer|cash|online`

`OrderResponseDTO`:

- `id`
- `productId`
- `productTitle`
- `buyerId`
- `buyerName`
- `sellerId`
- `sellerName`
- `totalAmount`
- `depositAmount`
- `requiredUpfrontAmount`
- `paidAmount`
- `remainingAmount`
- `serviceFee`
- `paymentOption`
- `status`
- `fundingStatus`
- `paymentMethod`
- `acceptedAt`
- `paymentDeadline`
- `createdAt`
- `updatedAt`

Lưu ý:

- `GET /api/orders/me` là mixed list cho user hiện tại, không tách buyer/seller
- transaction timeline nên render theo `status + fundingStatus + paymentMethod`

#### C. Payments and refunds

| API | Auth | Request | Result |
| --- | --- | --- | --- |
| `POST /api/payments/orders/{orderId}/request` | `BUYER` | none | `PaymentRequestResponseDTO` |
| `GET /api/payments/orders/{orderId}` | logged-in | none | `List<PaymentResponseDTO>` |
| `POST /api/orders/{orderId}/refunds` | `BUYER` | `RefundCreateRequestDTO` | `RefundResponseDTO` |
| `PATCH /api/admin/refunds/{refundId}/review` | `ADMIN` | `RefundReviewRequestDTO` | `RefundResponseDTO` |

`PaymentRequestResponseDTO`:

- `paymentId`
- `orderId`
- `gateway`
- `phase`
- `status`
- `amount`
- `gatewayOrderCode`
- `checkoutUrl`
- `qrCodeUrl`
- `transferContent`
- `bankBin`
- `bankAccountNumber`
- `bankAccountName`
- `mockMode`
- `instructions`
- `expiresAt`

`PaymentResponseDTO`:

- `id`
- `orderId`
- `amount`
- `gateway`
- `method`
- `phase`
- `status`
- `gatewayOrderCode`
- `transactionReference`
- `checkoutUrl`
- `qrCodeUrl`
- `paymentDate`
- `createdAt`

`RefundCreateRequestDTO`:

- `amount`
- `reason`
- `evidenceNote`

`RefundReviewRequestDTO`:

- `status`
- `adminNote`
- `refundReference`

`RefundResponseDTO`:

- `id`
- `orderId`
- `paymentId`
- `requesterId`
- `requesterName`
- `amount`
- `reason`
- `evidenceNote`
- `status`
- `adminNote`
- `refundReference`
- `reviewedBy`
- `reviewedByName`
- `reviewedAt`
- `processedAt`
- `createdAt`

Lưu ý:

- FE không gọi webhook. FE chỉ:
  - tạo payment request
  - hiển thị QR/instructions
  - polling lại `GET /api/payments/orders/{orderId}` hoặc refresh order screen
- hệ thống hiện là **webhook-only** cho callback backend
- bank transfer flow không có checkout gateway phức tạp ở FE

#### D. Chat REST + realtime

| API | Auth | Request | Result |
| --- | --- | --- | --- |
| `POST /api/conversations?productId=` | logged-in | query `productId` | `ConversationResponseDTO` |
| `GET /api/conversations/me` | logged-in | none | `List<ConversationResponseDTO>` |
| `GET /api/conversations/{conversationId}/messages?page=&size=` | logged-in | query page/size | `Page<MessageResponseDTO>` |
| `PUT /api/conversations/{conversationId}/read` | logged-in | none | no `result` |

`ConversationResponseDTO`:

- `id`
- `productId`
- `productTitle`
- `buyerId`
- `buyerName`
- `sellerId`
- `sellerName`
- `lastMessage`
- `updatedAt`

`MessageResponseDTO`:

- `id`
- `conversationId`
- `senderId`
- `senderName`
- `content`
- `imageUrl`
- `isRead`
- `createdAt`

WebSocket:

- endpoint: `/ws`
- broker topics:
  - subscribe conversation: `/topic/conversation/{conversationId}`
  - subscribe personal queue: `/user/queue/messages`
- send:
  - destination: `/app/chat.sendMessage`
  - body:

```json
{
  "conversationId": "uuid",
  "content": "hello",
  "imageUrl": null
}
```

Lưu ý:

- `senderId` trong `MessageRequestDTO` không cần FE gửi; backend dùng principal từ JWT
- hãy làm reconnect + resubscribe rõ ràng
- unread badge nên cập nhật từ `/user/queue/messages` + REST refresh

### FE Dev 2 - Marketplace + seller listing + reference data

Phụ trách:

- public marketplace
- seller listing CRUD
- reference data
- wishlist
- reviews
- inspection view/request

#### A. Public marketplace

| API | Auth | Request | Result |
| --- | --- | --- | --- |
| `GET /api/products?...` | public | `ProductFilterRequest` + page/size | `Page<ProductResponse>` |
| `GET /api/products/{id}` | public | none | `ProductResponse` |

`ProductFilterRequest` query fields:

- `keyword`
- `brandId`
- `categoryId`
- `brakeTypeId`
- `frameMaterialId`
- `condition`
- `frameSize`
- `wheelSize`
- `groupset`
- `minPrice`
- `maxPrice`
- `province`
- `hasInspection`
- `sortBy`

Lưu ý:

- list page chỉ nên render `result.content`
- `hasInspection=true` map từ verified inspection state, không phải boolean field thô trong entity

#### B. Seller product CRUD

| API | Auth | Request | Result |
| --- | --- | --- | --- |
| `POST /api/products` | `SELLER` | `multipart/form-data` | `ProductResponse` |
| `PUT /api/products/{id}` | `SELLER` | `multipart/form-data` | `ProductResponse` |
| `DELETE /api/products/{id}` | `SELLER` | none | success string |
| `GET /api/products/my?page=&size=` | `SELLER` | page/size | `Page<ProductResponse>` |
| `PATCH /api/products/{id}/hide` | `SELLER` | none | `ProductResponse` |
| `PATCH /api/products/{id}/show` | `SELLER` | none | `ProductResponse` |

`ProductCreateRequest` form fields:

- `title`
- `description`
- `price`
- `originalPrice`
- `brakeTypeId`
- `frameMaterialId`
- `brandId`
- `categoryId`
- `frameSize`
- `wheelSize`
- `groupset`
- `condition`
- `province`
- `district`
- `images[]` as files

`ProductUpdateRequest` form fields:

- same shape as create, but optional/partial
- `images[]` when replacing image set

Lưu ý:

- create/update đều là `multipart/form-data`, không phải JSON
- `show` không đưa listing về `active`; nó quay lại `pending`
- FE seller dashboard cần hiển thị trạng thái moderation lại sau `show`

#### C. Reference data

Public:

| API | Auth | Result |
| --- | --- | --- |
| `GET /api/brands` | public | `List<BrandResponseDTO>` |
| `GET /api/categories` | public | `List<CategoryResponseDTO>` |
| `GET /api/brake-types` | public | `List<ReferenceValueResponseDTO>` |
| `GET /api/frame-materials` | public | `List<ReferenceValueResponseDTO>` |

Admin:

| API | Auth | Request | Result |
| --- | --- | --- | --- |
| `POST /api/admin/brands` | `ADMIN` | `{name, logoUrl}` | `BrandResponseDTO` |
| `PUT /api/admin/brands/{id}` | `ADMIN` | `{name, logoUrl}` | `BrandResponseDTO` |
| `DELETE /api/admin/brands/{id}` | `ADMIN` | none | success string |
| `POST /api/admin/categories` | `ADMIN` | `{name, slug, parentId}` | `CategoryResponseDTO` |
| `PUT /api/admin/categories/{id}` | `ADMIN` | `{name, slug, parentId}` | `CategoryResponseDTO` |
| `DELETE /api/admin/categories/{id}` | `ADMIN` | none | success string |
| `POST /api/admin/brake-types` | `ADMIN` | `{name, description}` | `ReferenceValueResponseDTO` |
| `PUT /api/admin/brake-types/{id}` | `ADMIN` | `{name, description}` | `ReferenceValueResponseDTO` |
| `DELETE /api/admin/brake-types/{id}` | `ADMIN` | none | success string |
| `POST /api/admin/frame-materials` | `ADMIN` | `{name, description}` | `ReferenceValueResponseDTO` |
| `PUT /api/admin/frame-materials/{id}` | `ADMIN` | `{name, description}` | `ReferenceValueResponseDTO` |
| `DELETE /api/admin/frame-materials/{id}` | `ADMIN` | none | success string |

Response DTO fields:

- `BrandResponseDTO`: `id`, `name`, `logoUrl`, `createdAt`
- `CategoryResponseDTO`: `id`, `name`, `slug`, `parentId`, `parentName`, `createdAt`
- `ReferenceValueResponseDTO`: `id`, `name`, `description`, `createdAt`

Lưu ý:

- delete có thể fail nếu BE trả `REFERENCE_DATA_IN_USE`
- category tree có `parentId`, `parentName`; FE admin nên hỗ trợ parent selector

#### D. Wishlist, review, inspection

| API | Auth | Request | Result |
| --- | --- | --- | --- |
| `GET /api/wishlist` | logged-in | none | `List<WishlistItemResponseDTO>` |
| `POST /api/wishlist/{productId}` | logged-in | none | `WishlistItemResponseDTO` |
| `DELETE /api/wishlist/{productId}` | logged-in | none | no `result` |
| `POST /api/reviews/{orderId}` | `BUYER` | `{rating, comment}` | `ReviewResponseDTO` |
| `GET /api/users/{sellerId}/reviews?page=&size=` | public | page/size | `Page<ReviewResponseDTO>` |
| `POST /api/inspections/request/{productId}` | `SELLER`/`ADMIN` | none | `InspectionResponseDTO` |
| `GET /api/inspections/product/{productId}` | public | none | `InspectionResponseDTO` |

`WishlistItemResponseDTO`:

- `productId`
- `title`
- `price`
- `status`
- `sellerId`
- `sellerName`
- `primaryImageUrl`
- `addedAt`

`ReviewResponseDTO`:

- `id`
- `orderId`
- `reviewerId`
- `reviewerName`
- `revieweeId`
- `revieweeName`
- `rating`
- `comment`
- `createdAt`

`InspectionResponseDTO`:

- `id`
- `productId`
- `inspectorId`
- `overallScore`
- component scores
- `wearPercentage`
- `expertNotes`
- `passed`
- `reportFileUrl`
- `validUntil`
- `createdAt`

Lưu ý:

- inspector evaluation UI có thể giao lại sang FE Dev 3 nếu nhóm muốn gom role-based admin/inspector tooling về một người
- review hiện chỉ có buyer submit và public fetch; chưa có seller reply API

### FE Dev 3 - Auth + notifications + reports + admin users/dashboard

Phụ trách:

- auth/profile/password/email flows
- notifications center
- report module
- admin users
- admin dashboard
- có thể nhận thêm inspector evaluation screen nếu nhóm muốn gom role-based admin tooling về một người

#### A. Auth

| API | Auth | Request | Result |
| --- | --- | --- | --- |
| `POST /api/auth/register` | public | `RegisterRequest` | success string |
| `POST /api/auth/login` | public | `LoginRequest` | `AuthResponse` |
| `POST /api/auth/refresh` | public | `{refreshToken}` | `AuthResponse` |
| `POST /api/auth/forgot-password` | public | `{email}` | success string |
| `POST /api/auth/reset-password` | public | `{token, newPassword}` | success string |
| `POST /api/auth/logout` | logged-in | none | success string |
| `GET /api/auth/verify-email?token=` | public | query token | success string |
| `GET /api/auth/me` | logged-in | none | `AuthResponse.UserInfo` |
| `PATCH /api/auth/profile` | logged-in | `ProfileUpdateRequest` | `AuthResponse.UserInfo` |
| `PATCH /api/auth/change-password` | logged-in | `ChangePasswordRequest` | success string |

`RegisterRequest`:

- `email`
- `password`
- `firstName`
- `lastName`
- `phone`
- `role`

`LoginRequest`:

- `email`
- `password`

`ProfileUpdateRequest`:

- `firstName`
- `lastName`
- `phone`
- `avatarUrl`
- `defaultAddress`

`ChangePasswordRequest`:

- `currentPassword`
- `newPassword`

`AuthResponse`:

- `accessToken`
- `refreshToken`
- `tokenType`
- `expiresIn`
- `user`
  - `id`
  - `email`
  - `firstName`
  - `lastName`
  - `phone`
  - `avatarUrl`
  - `defaultAddress`
  - `role`
  - `status`
  - `isVerified`

Lưu ý:

- login và refresh hiện chặn user chưa verify email
- FE phải chuẩn bị screen/email-state rõ cho `isVerified=false`

#### B. Notifications

| API | Auth | Request | Result |
| --- | --- | --- | --- |
| `GET /api/notifications/me?page=&size=` | logged-in | page/size | `Page<NotificationResponseDTO>` |
| `GET /api/notifications/me/unread-count` | logged-in | none | `Long` |
| `PUT /api/notifications/{notificationId}/read` | logged-in | none | no `result` |
| `PUT /api/notifications/me/read-all` | logged-in | none | no `result` |

`NotificationResponseDTO`:

- `id`
- `userId`
- `title`
- `content`
- `type`
- `isRead`
- `metadata`
- `createdAt`

#### C. Reports

| API | Auth | Request | Result |
| --- | --- | --- | --- |
| `POST /api/reports` | logged-in | `ReportRequestDTO` | `ReportResponseDTO` |
| `GET /api/reports/me?page=&size=` | logged-in | page/size | `Page<ReportResponseDTO>` |
| `GET /api/admin/reports?status=&targetType=&page=&size=` | `ADMIN` | query filter | `Page<ReportResponseDTO>` |
| `PUT /api/admin/reports/{reportId}/process` | `ADMIN` | `ReportProcessDTO` | `ReportResponseDTO` |

`ReportRequestDTO`:

- `targetId`
- `targetType`
- `reason`
- `description`

`ReportProcessDTO`:

- `status`
- `adminNote`

`ReportResponseDTO`:

- `id`
- `reporterId`
- `reporterName`
- `targetId`
- `targetType`
- `reason`
- `description`
- `status`
- `adminNote`
- `processedById`
- `processedByName`
- `createdAt`
- `processedAt`

#### D. Admin users and dashboard

| API | Auth | Request | Result |
| --- | --- | --- | --- |
| `GET /api/admin/users?keyword=&role=&status=&verified=&page=&size=` | `ADMIN` | query filter | `Page<AdminUserResponseDTO>` |
| `GET /api/admin/users/{id}` | `ADMIN` | none | `AdminUserResponseDTO` |
| `PATCH /api/admin/users/{id}/status` | `ADMIN` | `{status}` | `AdminUserResponseDTO` |
| `PATCH /api/admin/users/{id}/password` | `ADMIN` | `{newPassword}` | success string |
| `GET /api/admin/users/{id}/activity` | `ADMIN` | none | `AdminUserActivityResponseDTO` |
| `GET /api/admin/dashboard/stats` | `ADMIN` | none | `DashboardStatsDTO` |

`AdminUserResponseDTO`:

- `id`
- `email`
- `firstName`
- `lastName`
- `fullName`
- `phone`
- `avatarUrl`
- `defaultAddress`
- `role`
- `status`
- `isVerified`
- `averageRating`
- `totalReviews`
- `createdAt`
- `updatedAt`

`AdminUserActivityResponseDTO`:

- summary:
  - `userId`
  - `email`
  - `role`
  - `status`
  - `verified`
  - `createdAt`
  - `updatedAt`
  - `totalProducts`
  - `totalOrdersAsBuyer`
  - `totalOrdersAsSeller`
  - `totalReportsSubmitted`
  - `totalWishlistItems`
  - `totalConversations`
  - `unreadNotifications`
- recent lists:
  - `recentProducts[]`
  - `recentOrders[]`
  - `recentReports[]`
  - `recentNotifications[]`
  - `recentWishlistItems[]`

`DashboardStatsDTO`:

- `totalUsers`
- `totalProducts`
- `totalOrders`
- `totalRevenue`
- `totalInspections`
- `passedInspections`
- `failedInspections`
- `monthlyRevenue`
- `monthlyOrders`

Lưu ý:

- admin không được tự đổi status chính mình
- admin reset password dùng chung password policy với auth flow

## Cấu trúc FE nên dùng

### Đề xuất shared folders

```text
src/
  api/
    auth.api.ts
    products.api.ts
    orders.api.ts
    payments.api.ts
    refunds.api.ts
    chat.api.ts
    notifications.api.ts
    reports.api.ts
    admin-users.api.ts
    reference-data.api.ts
  sockets/
    chat.stomp.ts
  types/
    api-response.ts
    auth.ts
    product.ts
    order.ts
    payment.ts
    report.ts
    notification.ts
```

### Shared tasks cho cả 3 người

- thống nhất 1 API client unwrap `ApiResponse<T>`
- thống nhất 1 auth store chứa:
  - `accessToken`
  - `refreshToken`
  - `user`
- thống nhất enum mapping ở FE để tránh string hardcode rải rác
- thống nhất formatter cho page response

## Lưu ý tích hợp quan trọng

1. Product create/update là `multipart/form-data`, không phải JSON.
2. Seller `show` trả listing về `pending`, không hiển thị ngay.
3. Admin moderation hiện không có `rejected` enum riêng.
4. Payment callback là webhook-only ở backend, FE không xử lý callback.
5. Chat realtime bắt buộc gửi JWT ở STOMP `CONNECT`.
6. Nhiều endpoint trả `Page<T>` trong `result`, đừng map nhầm thành `T[]`.
7. Một số message text vẫn chưa đồng nhất hoàn toàn giữa tiếng Việt và tiếng Anh; FE nên dựa vào `code` và status hơn là hardcode toàn bộ message.
