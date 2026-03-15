# Kiến Thức Nền Tảng Từ Lần Sửa Backend Ngày 2026-03-12

## Mục tiêu của tài liệu này

Tài liệu này giải thích những kiến thức quan trọng đã được dùng trong lần sửa backend gần đây của dự án Old Bicycles Marketplace.

Cách viết của tài liệu này hướng tới:

- sinh viên năm nhất mới học lập trình
- người đã biết rất cơ bản về biến, hàm, class, API, request, response
- người muốn hiểu không chỉ "đã sửa gì" mà còn "vì sao phải sửa như vậy"

Tài liệu này không cố gắng dạy hết backend. Mục tiêu của nó là giúp bạn hiểu rõ những ý quan trọng vừa xuất hiện trong project.

## Cách đọc tài liệu này

Mỗi phần sẽ cố gắng đi theo cùng một thứ tự:

1. Bối cảnh hoặc vấn đề
2. Định nghĩa
3. Ví dụ đơn giản
4. Cách áp dụng trong project
5. Lỗi hiểu sai thường gặp

Nếu bạn mới học, hãy đọc chậm và ưu tiên hiểu từng khái niệm một.

---

## 1. Vì sao không nên nhận `userId` trực tiếp từ request

### Bối cảnh

Trước khi sửa, một số API nhận các giá trị như:

- `userId`
- `sellerId`
- `inspectorId`
- `reviewerId`
- `reporterId`

trực tiếp từ URL, query param, hoặc body.

Điều này nhìn qua có vẻ tiện, nhưng thật ra rất nguy hiểm.

### Định nghĩa

`request` là dữ liệu do phía client gửi lên server.

Ví dụ:

- URL
- query string
- JSON body
- form data

Nếu backend tin hoàn toàn vào dữ liệu do client gửi lên, thì client có thể giả mạo dữ liệu đó.

### Ví dụ đơn giản

Giả sử có API:

```http
GET /api/notifications/user/123
```

Nếu backend chỉ nhìn số `123` và trả dữ liệu tương ứng, thì người dùng có thể thử đổi thành:

```http
GET /api/notifications/user/999
```

Nếu `999` là ID của người khác, dữ liệu của người khác có thể bị lộ.

### Cách làm đúng hơn

Nếu hệ thống đã có đăng nhập bằng JWT hoặc session, thì backend nên lấy người dùng hiện tại từ thông tin xác thực đã được kiểm tra sẵn.

Trong Spring Security, điều này thường được làm bằng:

```java
@AuthenticationPrincipal User currentUser
```

Ý nghĩa rất đơn giản:

- client gửi token
- Spring Security kiểm tra token
- nếu token hợp lệ, Spring biết người đang đăng nhập là ai
- controller chỉ cần lấy `currentUser`

Sau đó backend dùng:

```java
currentUser.getId()
```

thay vì tin vào ID mà client tự gửi.

### Đã áp dụng trong project này như thế nào

Trong lần sửa này, các API sau đã được đổi sang lấy user hiện tại từ `@AuthenticationPrincipal`:

- `NotificationController`
- `InspectionController`
- `ReviewController`
- `ReportController`
- các REST endpoint trong `ChatController`

Nói ngắn gọn: backend bớt tin vào dữ liệu tự khai của client, và tin vào hệ thống xác thực hơn.

### Lỗi hiểu sai thường gặp

Hiểu sai 1:
"Client của mình là app do mình viết, nên chắc nó sẽ không gửi sai."

Sai vì:

- client luôn có thể bị sửa
- request luôn có thể bị giả lập bằng Postman, curl, script
- backend phải tự bảo vệ mình

Hiểu sai 2:
"Có đăng nhập rồi thì nhận thêm `userId` cũng không sao."

Sai vì:

- nếu đã biết người dùng hiện tại là ai, thì nhận thêm `userId` thường chỉ làm tăng rủi ro

---

## 2. Ownership check là gì

### Bối cảnh

Ngay cả khi đã biết người dùng hiện tại là ai, backend vẫn cần kiểm tra xem người đó có quyền thao tác lên dữ liệu cụ thể hay không.

Ví dụ:

- bạn đã đăng nhập, nhưng không có nghĩa là bạn được sửa đơn hàng của người khác
- bạn đã đăng nhập, nhưng không có nghĩa là bạn được đọc tin nhắn trong cuộc trò chuyện không phải của mình

### Định nghĩa

`ownership check` có thể hiểu đơn giản là:

"Kiểm tra xem tài nguyên này có thuộc về người đang thao tác hay không."

`resource` là tài nguyên trong hệ thống, ví dụ:

- notification
- product
- order
- conversation
- review

### Ví dụ đơn giản

Giả sử có notification với ID `A1`.

Backend cần kiểm tra:

- notification `A1` có thuộc về user hiện tại không?

Nếu có thì cho phép đánh dấu là đã đọc.
Nếu không thì từ chối.

### Ví dụ ngoài đời thường

Ownership check giống như việc kiểm tra:

- đây có phải chìa khóa xe của bạn không?
- đây có phải tài khoản ngân hàng của bạn không?

Không phải cứ là "người đã vào được tòa nhà" thì muốn mở phòng nào cũng được.

### Đã áp dụng trong project này như thế nào

Trong lần sửa này:

- Notification: chỉ người nhận notification mới được đánh dấu notification đó là đã đọc
- Review: chỉ buyer của order mới được gửi review cho order đó
- Chat: chỉ buyer hoặc seller trong conversation mới được đọc và đánh dấu tin nhắn là đã đọc
- Order: chỉ seller hoặc admin mới được xác nhận đặt cọc và hoàn tất đơn

### Lỗi hiểu sai thường gặp

Hiểu sai:
"Chỉ cần kiểm tra role là đủ."

Không đủ.

Ví dụ:

- hai người đều có role `SELLER`
- nhưng seller A không được phép xác nhận order của seller B

Nghĩa là:

- `role check` kiểm tra loại quyền
- `ownership check` kiểm tra quyền trên tài nguyên cụ thể

Hai thứ này thường phải đi cùng nhau.

---

## 3. Enum là gì và vì sao enum Java phải khớp với enum trong database

### Định nghĩa

`enum` là một kiểu dữ liệu chỉ cho phép một số giá trị cố định.

Ví dụ:

```java
public enum OrderStatus {
    pending,
    deposited,
    completed,
    cancelled
}
```

Nghĩa là trạng thái đơn hàng chỉ được nằm trong 4 giá trị đó.

Bạn không thể gán bừa kiểu:

- `hello`
- `123`
- `almost_done`

nếu những giá trị đó không nằm trong enum.

### Vì sao enum hữu ích

Enum giúp:

- code rõ nghĩa hơn
- tránh gõ sai chuỗi
- giảm lỗi logic
- dễ kiểm soát trạng thái của hệ thống

### Vấn đề đã gặp trong project

Trong database PostgreSQL, enum được khai báo theo dạng chữ thường:

- `pending`
- `deposited`
- `completed`

Nhưng trong Java trước đó lại dùng:

- `PENDING`
- `DEPOSITED`
- `COMPLETED`

Hai bên không giống nhau.

### Vì sao đây là vấn đề

Khi JPA map dữ liệu từ Java xuống database, nó cần biết giá trị nào sẽ được ghi.

Nếu Java ghi:

```java
PENDING
```

nhưng database chỉ chấp nhận:

```sql
'pending'
```

thì có thể phát sinh lỗi runtime hoặc map sai.

### Cách sửa đã dùng

Trong lần sửa này, các enum liên quan đã được đổi sang dạng lowercase để khớp với PostgreSQL.

Ví dụ:

```java
public enum OrderStatus {
    pending,
    deposited,
    completed,
    cancelled
}
```

### Đã áp dụng vào đâu

- `OrderStatus`
- `PaymentMethod`
- `PaymentStatus`
- `ReportReason`
- `ReportStatus`
- `NotificationType`

### Lỗi hiểu sai thường gặp

Hiểu sai:
"Chữ hoa hay chữ thường chỉ là chuyện style."

Sai.

Trong nhiều tình huống, đây không phải chuyện style mà là chuyện dữ liệu có map đúng hay không.

---

## 4. Flyway migration là gì

### Định nghĩa

`migration` là một bước thay đổi cấu trúc database có ghi lại lịch sử.

`Flyway` là công cụ giúp quản lý các bước thay đổi đó.

Bạn có thể hiểu đơn giản:

- code thay đổi
- database cũng phải thay đổi theo
- Flyway ghi lại những thay đổi đó thành từng file có thứ tự

### Ví dụ đơn giản

Ban đầu bảng `users` chưa có cột `average_rating`.

Sau này project cần lưu điểm trung bình của seller.

Ta không nên chỉ vào database và thêm tay một cột.

Ta nên tạo migration, ví dụ:

```sql
ALTER TABLE users
ADD COLUMN average_rating DOUBLE PRECISION DEFAULT 0.0;
```

Nhờ vậy:

- mọi môi trường đều có cùng thay đổi
- người khác kéo code về cũng biết database phải thay đổi thế nào
- lịch sử thay đổi được lưu lại rõ ràng

### Đã áp dụng trong project này như thế nào

Trong lần sửa này, đã thêm:

- `V4__align_runtime_schema.sql`

Migration này dùng để:

- thêm các giá trị mới cho `product_status`
- thêm `average_rating`
- thêm `total_reviews`

### Vì sao việc này quan trọng

Nếu entity trong code đã có field mới nhưng database chưa có cột tương ứng, hệ thống sẽ bị lệch.

Sự lệch này gọi là `schema drift`.

### Định nghĩa `schema drift`

`schema` là cấu trúc của database.

`drift` có thể hiểu là "trôi lệch".

`schema drift` nghĩa là:

- code nghĩ database đang có cấu trúc A
- nhưng database thật lại đang là cấu trúc B

### Lỗi hiểu sai thường gặp

Hiểu sai:
"Chỉ cần sửa entity Java là đủ."

Sai vì:

- entity chỉ là phần mô tả ở phía code
- database thật vẫn cần được cập nhật

---

## 5. Vì sao một feature có entity vẫn chưa thể coi là xong

### Bối cảnh

Đây là lỗi đánh giá tiến độ rất hay gặp khi mới học backend.

Bạn mở project ra và thấy:

- có `Order.java`
- có `Wishlist.java`
- có repository

rồi kết luận:

"Feature này chắc gần xong."

Thực tế thường không phải vậy.

### Định nghĩa

Một backend feature "dùng được" thường cần đủ nhiều lớp:

1. Controller
2. Service
3. Repository
4. Entity
5. Security
6. Migration
7. Test

### Giải thích từng lớp bằng ngôn ngữ đơn giản

`Controller`

- nhận request từ client
- trả response về client

`Service`

- chứa business logic
- quyết định hệ thống sẽ xử lý nghiệp vụ ra sao

`Repository`

- đọc và ghi dữ liệu từ database

`Entity`

- mô tả dữ liệu trong code

`Security`

- kiểm tra ai được phép làm gì

`Migration`

- cập nhật database cho đúng với code

`Test`

- kiểm tra xem tính năng có còn chạy đúng không sau khi sửa

### Ví dụ đơn giản

Nếu chỉ có `Wishlist.java` nhưng không có:

- API để thêm sản phẩm vào wishlist
- API để xóa
- API để xem danh sách wishlist

thì người dùng vẫn chưa sử dụng được feature đó.

### Đã áp dụng trong project này như thế nào

Trước đây:

- `Wishlist` và `Order` chủ yếu mới dừng ở dữ liệu và repository

Sau lần sửa này đã bổ sung:

- `WishlistController`, `WishlistService`, `WishlistRepository`
- `OrderController`, `OrderService`, `OrderRepository`

Nghĩa là hai feature này đã tiến thêm một bước lớn: từ "có cấu trúc dữ liệu" sang "có luồng xử lý backend thực tế".

### Lỗi hiểu sai thường gặp

Hiểu sai:
"Có entity là coi như đã 70%."

Sai vì:

- entity thường chỉ là phần đầu
- phần khó hơn thường nằm ở business rule, auth, ownership, test, migration

---

## 6. Business flow là gì và order flow trong lần sửa này hoạt động ra sao

### Định nghĩa

`business flow` là luồng nghiệp vụ.

Nói đơn giản hơn:

"Người dùng làm bước 1, hệ thống phản ứng ra sao, rồi tới bước 2, bước 3..."

### Ví dụ rất đơn giản

Trong một ứng dụng bán hàng, flow có thể là:

1. Người mua chọn sản phẩm
2. Người mua tạo đơn
3. Người bán xác nhận
4. Đơn hoàn tất

Đó chính là business flow.

### Order flow trong project này sau lần sửa

Flow hiện tại ở mức backend cơ bản nhưng đã usable hơn trước:

1. Buyer tạo order
2. Seller hoặc admin xác nhận đặt cọc
3. Seller hoặc admin hoàn tất order
4. Khi hoàn tất, product được chuyển sang trạng thái `sold`
5. Buyer, seller, hoặc admin có thể hủy order nếu đơn chưa hoàn tất

### Vì sao flow này quan trọng

Nếu chỉ lưu order vào database mà không có quy tắc chuyển trạng thái, hệ thống sẽ rất rối.

Ví dụ:

- không biết khi nào đơn được xem là hợp lệ
- không biết khi nào sản phẩm phải chuyển sang `sold`
- không biết ai được phép xác nhận đơn

### Điều gì vẫn còn thiếu

Flow hiện tại chưa phải là hệ thống thanh toán hoàn chỉnh. Vẫn còn thiếu:

- cổng thanh toán
- đối soát giao dịch
- hoàn tiền
- dispute/refund flow

Điều này có nghĩa là:

- feature đã tiến bộ
- nhưng vẫn chưa đạt mức `Done` theo SRS

### Lỗi hiểu sai thường gặp

Hiểu sai:
"Có API tạo order là xong order feature."

Sai vì:

- tạo order chỉ là một phần của flow
- còn cần trạng thái, phân quyền, chuyển trạng thái, và xử lý các trường hợp lỗi

---

## 7. Vì sao chat vẫn cần kiểm tra participant

### Định nghĩa

`participant` là người tham gia vào một conversation.

Trong project này, participant thường là:

- buyer
- seller

### Vấn đề

Ngay cả khi người dùng đã đăng nhập, backend vẫn phải kiểm tra:

- người này có thật sự nằm trong conversation này không?

Nếu không kiểm tra, ai biết `conversationId` cũng có thể thử đọc tin nhắn.

### Ví dụ đơn giản

Giả sử conversation `C1` là giữa:

- buyer A
- seller B

Nếu user C không liên quan gì nhưng vẫn gọi:

```http
GET /api/conversations/C1/messages
```

thì backend phải từ chối.

### Đã áp dụng trong project này như thế nào

Trong `MessageServiceImpl`, đã thêm kiểm tra:

- nếu user không phải buyer
- và cũng không phải seller
- thì ném lỗi `FORBIDDEN`

### Một lỗi kỹ thuật khác cũng đã được sửa

Trước đó repository dùng một JPQL query có `LIMIT 1`.

Điều này không chuẩn trong JPQL.

Đã thay bằng query method của Spring Data:

```java
findFirstByConversationIdOrderByCreatedAtDesc(...)
```

### Bài học rút ra

Không chỉ cần đúng về nghiệp vụ, mà còn phải đúng với công nghệ đang dùng.

---

## 8. Phân biệt lỗi code và lỗi môi trường build

### Bối cảnh

Khi chạy Maven, dự án từng báo:

```text
release version 21 not supported
```

Người mới học thường rất dễ kết luận ngay:

"Code bị sai."

Nhưng kết luận đó chưa chắc đúng.

### Định nghĩa

`lỗi code`

- lỗi xuất phát từ source code
- ví dụ sai cú pháp, sai import, sai logic, type không khớp

`lỗi môi trường`

- lỗi do máy đang chạy không đúng điều kiện mà project yêu cầu
- ví dụ sai version Java, thiếu database, thiếu biến môi trường

### Trường hợp trong project này

Project target Java 21.

Nhưng môi trường hiện tại lúc đó đang dùng Java 17.

Vì vậy Maven không verify theo cấu hình gốc được.

### Điều này dạy ta điều gì

Khi gặp lỗi build, cần hỏi:

1. Source code có sai không?
2. Hay máy đang dùng sai toolchain?

### Đã làm gì để kiểm tra thêm

Đã chạy một bước kiểm tra tương thích với `release=17` để xem:

- code có lỗi compile rõ ràng không
- context test cơ bản có lên được không

Kết quả:

- compile pass
- test `contextLoads` pass

Điều đó không chứng minh rằng mọi thứ đã hoàn hảo trên Java 21, nhưng nó cho thấy:

- thay đổi mới không có lỗi cú pháp/wiring quá rõ

### Lỗi hiểu sai thường gặp

Hiểu sai:
"Build fail nghĩa là sửa code chưa đúng."

Không phải lúc nào cũng vậy.

Đôi khi vấn đề nằm ở:

- JDK sai version
- cấu hình môi trường sai
- dependency chưa đúng

---

## 9. Cách tự đọc một backend feature cho đúng

Khi bạn muốn tự đánh giá một module như `wishlist`, `order`, `review`, hãy đi theo thứ tự sau:

1. `Controller`
2. `Service`
3. `Repository`
4. `Entity`
5. `Migration`
6. `Security`
7. `Test`

### Vì sao nên đọc theo thứ tự này

Vì thứ tự này gần giống luồng chạy thật của hệ thống:

- request đi vào controller
- controller gọi service
- service gọi repository
- repository làm việc với database

Sau đó bạn mới kiểm tra:

- dữ liệu có đúng schema không
- quyền có đúng không
- test có bảo vệ được không

### Một mẹo rất quan trọng

Nếu bạn chỉ nhìn:

- entity
- repository

thì rất dễ đánh giá nhầm là feature đã gần xong.

Hãy luôn tự hỏi:

- endpoint nào gọi feature này?
- business rule nằm ở đâu?
- user nào được phép gọi?
- database đã có migration chưa?
- đã có test chưa?

---

## 10. Tóm tắt ngắn gọn những điều quan trọng nhất

- Không nên tin `userId` do client tự gửi nếu hệ thống đã có auth.
- Đăng nhập rồi vẫn chưa đủ, còn phải kiểm tra ownership.
- Enum trong Java và enum trong PostgreSQL phải khớp nhau.
- Có entity không có nghĩa là feature đã hoàn thành.
- Flyway giúp database đi cùng với code.
- Khi build lỗi, phải tách rõ lỗi code và lỗi môi trường.
- Muốn hiểu một feature backend, đừng chỉ nhìn entity. Hãy đọc cả controller, service, security, migration, và test.

---

## Gợi ý đọc code sau khi đọc xong tài liệu này

Nếu bạn muốn quay lại code để nhìn những ý trên trong thực tế, hãy xem các file sau:

- `src/main/java/com/backend/old_bicycle_project/controller/NotificationController.java`
- `src/main/java/com/backend/old_bicycle_project/controller/InspectionController.java`
- `src/main/java/com/backend/old_bicycle_project/controller/OrderController.java`
- `src/main/java/com/backend/old_bicycle_project/controller/WishlistController.java`
- `src/main/java/com/backend/old_bicycle_project/service/impl/MessageServiceImpl.java`
- `src/main/java/com/backend/old_bicycle_project/service/impl/OrderServiceImpl.java`
- `src/main/resources/db/migration/V4__align_runtime_schema.sql`

Bạn có thể thử đọc theo cách sau:

1. Đọc controller để biết API nào đã được mở ra
2. Đọc service để biết logic thật sự nằm ở đâu
3. Đọc migration để biết database đã đổi gì
4. Tự trả lời câu hỏi: "Nếu bỏ đoạn kiểm tra này đi thì bug gì có thể xảy ra?"
