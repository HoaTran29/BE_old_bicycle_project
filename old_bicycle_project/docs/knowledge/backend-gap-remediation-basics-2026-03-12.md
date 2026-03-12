# Kiến Thức Nền Tảng Từ Lần Sửa Backend Ngày 2026-03-12

## Mục tiêu của tài liệu này

Tài liệu này giải thích những kiến thức quan trọng đã được áp dụng khi sửa backend của dự án Old Bicycles Marketplace. Nội dung được viết cho người mới học lập trình, ưu tiên dễ hiểu và bám sát đúng những gì vừa được làm trong code.

## 1. Vì sao không nên nhận `userId` trực tiếp từ request

Khi một API nhận `userId` từ query, path, hoặc body, client có thể cố tình thay bằng ID của người khác. Nếu backend tin vào giá trị đó, người dùng A có thể đọc hoặc sửa dữ liệu của người dùng B.

Ví dụ xấu:

```http
GET /api/notifications/user/{userId}
```

Nếu backend chỉ lấy `userId` từ URL, thì ai cũng có thể thử ID của người khác.

### Cách làm đúng hơn

Backend nên lấy người dùng hiện tại từ phiên đăng nhập hoặc JWT đã được Spring Security xác thực.

Trong dự án này, `User` đã implement `UserDetails`, nên controller có thể dùng:

```java
@AuthenticationPrincipal User currentUser
```

Sau đó truyền `currentUser.getId()` xuống service.

### Đã áp dụng vào đâu

- `NotificationController`
- `InspectionController`
- `ReviewController`
- `ReportController`
- REST endpoints trong `ChatController`

Kết quả là nhiều endpoint không còn tin vào `userId`, `sellerId`, `inspectorId`, `reviewerId`, `reporterId` do client tự gửi lên nữa.

## 2. Ownership check là gì

Ownership check là kiểm tra xem người đang gọi API có thực sự sở hữu tài nguyên hoặc có quyền thao tác với tài nguyên đó hay không.

Ví dụ:

- Chỉ seller của sản phẩm mới được xác nhận đơn.
- Chỉ buyer của order mới được review seller.
- Chỉ người nhận notification mới được đánh dấu notification đó là đã đọc.

### Đã áp dụng vào đâu

- Notification đọc 1 item: service kiểm tra `notificationId` có thuộc `currentUser` hay không.
- Chat messages: service kiểm tra người đang đọc tin nhắn có phải là buyer hoặc seller trong conversation hay không.
- Review: reviewer phải đúng là buyer của order.
- Order: chỉ seller hoặc admin mới được `confirm-deposit` và `complete`.

## 3. Vì sao phải đồng bộ enum Java với enum trong PostgreSQL

Trong dự án này, database dùng enum PostgreSQL như:

- `pending`
- `deposited`
- `completed`

Nhưng code Java trước đó lại dùng:

- `PENDING`
- `DEPOSITED`
- `COMPLETED`

Điều này dễ gây lỗi khi JPA ghi dữ liệu xuống database, vì giá trị Java và giá trị enum trong database không trùng nhau.

### Cách sửa đã dùng

Thay các enum Java sang dạng lowercase để khớp với database:

```java
public enum OrderStatus {
    pending,
    deposited,
    completed,
    cancelled
}
```

### Lợi ích

- Dễ đọc khi so với migration SQL.
- Giảm rủi ro runtime khi map enum.
- Ít phải dùng converter hoặc custom type phức tạp.

### Đã áp dụng vào đâu

- `OrderStatus`
- `PaymentMethod`
- `PaymentStatus`
- `ReportReason`
- `ReportStatus`
- `NotificationType`

## 4. Flyway migration dùng để làm gì

Flyway là công cụ quản lý thay đổi database theo từng version.

Thay vì sửa tay database mỗi lần code đổi, ta ghi lại thay đổi bằng file migration:

- `V1__...sql`
- `V2__...sql`
- `V3__...sql`
- `V4__...sql`

Mỗi file là một bước thay đổi rõ ràng.

### Trong lần sửa này đã làm gì với migration

Đã thêm `V4__align_runtime_schema.sql` để:

- thêm các giá trị còn thiếu cho `product_status`
- thêm `average_rating`
- thêm `total_reviews`

Điều này giúp database tiến gần hơn với entity hiện tại trong code.

## 5. Một feature chỉ có entity chưa đủ gọi là hoàn thành

Nhiều bạn mới học thường thấy đã có:

- entity
- repository

thì nghĩ feature gần xong.

Thực tế chưa đủ.

Một feature backend usable thường cần:

1. Controller nhận request
2. Service chứa business logic
3. Repository truy xuất dữ liệu
4. Security/ownership đúng
5. Migration/schema tương ứng
6. Test cơ bản

Nếu thiếu các phần này, feature thường mới ở mức `Partial`.

### Đã áp dụng vào đâu

Trước đây `Wishlist` và `Order` chủ yếu mới ở mức entity/repository. Trong lần sửa này đã thêm:

- `WishlistController`, `WishlistService`, `WishlistRepository`
- `OrderController`, `OrderService`, `OrderRepository`

Điều này biến chúng từ "có cấu trúc dữ liệu" thành "có API flow để dùng".

## 6. Business flow của `Order` trong lần sửa này

Flow hiện tại được làm theo hướng tối thiểu nhưng usable:

1. Buyer tạo order
2. Seller/Admin xác nhận đặt cọc
3. Seller/Admin hoàn tất order
4. Hệ thống đổi trạng thái product sang `sold`
5. Buyer/Seller/Admin có thể hủy order nếu chưa hoàn tất

### Điểm cần nhớ

Đây mới là flow backend cơ bản.

Nó chưa phải payment/escrow hoàn chỉnh vì còn thiếu:

- cổng thanh toán
- đối soát giao dịch
- hoàn tiền
- dispute/refund flow

Nghĩa là feature đã usable hơn trước, nhưng chưa đạt mức `Done` theo SRS.

## 7. Vì sao chat vẫn cần kiểm tra participant

Kể cả khi đã có conversation ID, backend vẫn phải kiểm tra người gọi có thuộc conversation đó hay không.

Nếu không kiểm tra, ai biết `conversationId` cũng có thể đọc tin nhắn.

### Trong lần sửa này

`MessageServiceImpl` đã thêm kiểm tra:

- nếu user không phải buyer hoặc seller của conversation
- thì ném `FORBIDDEN`

Ngoài ra cũng đã bỏ query JPQL không hợp lệ dạng `LIMIT 1` và thay bằng query method của Spring Data.

## 8. Vì sao build không chạy được dù code có thể đúng

Khi chạy Maven, dự án báo:

`release version 21 not supported`

Điều này không có nghĩa code chắc chắn sai. Nó có nghĩa môi trường hiện tại đang dùng JDK 17 trong khi project yêu cầu Java 21.

### Bài học quan trọng

Khi verify backend Java, cần tách rõ:

- lỗi code
- lỗi môi trường build

Trong lần này, môi trường đang là:

- Java 17

Trong khi project đang target:

- Java 21

Nên bước compile/test bị chặn từ trước khi kiểm tra hết source code.

## 9. Cách tự đọc một backend feature cho đúng

Khi gặp một module như `wishlist` hoặc `order`, hãy đọc theo thứ tự:

1. Controller
2. Service
3. Repository
4. Entity
5. Migration
6. Security
7. Test

Nếu chỉ nhìn entity hoặc repository, rất dễ đánh giá tiến độ sai.

## 10. Tóm tắt những gì nên nhớ sau lần sửa này

- Không tin `userId` do client tự gửi nếu đã có auth.
- Muốn an toàn thì phải kiểm tra ownership.
- Enum Java và enum PostgreSQL phải khớp nhau.
- Có entity chưa có nghĩa là feature đã usable.
- Flyway giúp database đi cùng với code.
- Khi build lỗi, phải kiểm tra xem đó là lỗi code hay lỗi môi trường.

## Liên hệ trực tiếp với lần sửa này

Nếu bạn muốn đọc code để hiểu lại toàn bộ thay đổi, hãy ưu tiên xem các file sau:

- `src/main/java/com/backend/old_bicycle_project/controller/NotificationController.java`
- `src/main/java/com/backend/old_bicycle_project/controller/InspectionController.java`
- `src/main/java/com/backend/old_bicycle_project/controller/OrderController.java`
- `src/main/java/com/backend/old_bicycle_project/controller/WishlistController.java`
- `src/main/java/com/backend/old_bicycle_project/service/impl/MessageServiceImpl.java`
- `src/main/java/com/backend/old_bicycle_project/service/impl/OrderServiceImpl.java`
- `src/main/resources/db/migration/V4__align_runtime_schema.sql`
