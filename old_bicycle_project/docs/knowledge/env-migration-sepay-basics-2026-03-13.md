# Biến Môi Trường, Migration và SePay Cơ Bản

Ngày cập nhật: 2026-03-13  
Phạm vi: giải thích cho người mới học lập trình vì sao dự án cần `.env`, cần migration, và SePay cần cấu hình gì để chạy.

## 1. Biến môi trường là gì?

`Biến môi trường` là những giá trị cấu hình được đặt bên ngoài code.

Ví dụ:

- địa chỉ database
- mật khẩu database
- secret để ký JWT
- key của dịch vụ thanh toán

Ta không nên hard-code những giá trị này trực tiếp vào source code vì:

- dễ lộ secret
- khó đổi giữa local, staging, production
- không tiện khi nhiều người cùng làm

## 2. File `.env` là gì?

`.env` là file dùng để chứa các biến môi trường dưới dạng:

```env
DB_URL=jdbc:postgresql://db.kfkzxghznwgbbarfsqre.supabase.co:5432/postgres?sslmode=require
DB_USERNAME=postgres
DB_PASSWORD=your_database_password
```

Trong dự án này, dependency `spring-dotenv` giúp Spring Boot đọc file `.env` khi chạy app.

## 2.1. Supabase đang đóng vai trò gì trong dự án này?

Trong dự án này, Supabase được xem là:

- nơi host database PostgreSQL
- nơi chứa file media nếu backend cần upload ảnh

Điều quan trọng là:

- **Spring Boot vẫn là backend chính**
- business logic vẫn nằm trong controller, service, repository của dự án
- Supabase ở đây **không phải** là “backend thay thế” cho Spring Boot

Nói dễ hiểu:

- Spring Boot là “bộ não xử lý nghiệp vụ”
- Supabase là “nơi chứa dữ liệu”

Ví dụ:

- khi người dùng đăng nhập, backend Spring Boot xử lý JWT
- khi người dùng tạo đơn hàng, backend Spring Boot kiểm tra rule nghiệp vụ
- khi cần lưu sản phẩm, đơn hàng, refresh token, backend mới ghi dữ liệu xuống PostgreSQL đang được host trên Supabase

## 3. Migration là gì?

`Migration` là các file SQL dùng để thay đổi cấu trúc database theo thời gian.

Ví dụ:

- thêm cột mới
- thêm bảng mới
- thêm index
- cập nhật enum

Trong repo này, các migration nằm trong:

- `src/main/resources/db/migration`

Ví dụ:

- `V5__payment_refund_upgrade.sql`
- `V6__password_reset_tokens.sql`

## 3.1. Có phải dùng Supabase thì không cần JDBC nữa không?

Không.

Nếu backend của bạn là Java Spring Boot và bạn muốn truy cập database PostgreSQL trên Supabase, thì backend vẫn kết nối bằng cách quen thuộc:

- dùng PostgreSQL JDBC driver
- dùng `DB_URL`
- dùng `DB_USERNAME`
- dùng `DB_PASSWORD`

Điểm khác duy nhất là:

- database đó không nằm trên máy local
- nó nằm trên hạ tầng Supabase

Nói cách khác:

- **Supabase không làm mất đi cách kết nối JDBC**
- Supabase chỉ cung cấp cho bạn một địa chỉ Postgres được host sẵn

Ví dụ tư duy sai dễ gặp:

- “Dùng Supabase thì phải dùng supabase-js”

Điều này chỉ đúng khi bạn chọn đi theo hướng client SDK hoặc Data API.

Trong dự án này, hướng đúng là:

- frontend gọi Spring Boot backend
- Spring Boot backend dùng JDBC/JPA để nói chuyện với PostgreSQL trên Supabase

## 4. Vì sao migration phải được apply?

Vì code Java hiện tại đang giả định rằng database đã có các cột/bảng mới.

Nếu code đã mới nhưng DB vẫn cũ, sẽ xảy ra tình huống:

- code cần đọc cột `deleted_at`
- nhưng DB chưa có cột đó
- app lỗi hoặc feature chạy sai

Nói ngắn gọn:

- code và database phải đi cùng phiên bản logic

## 5. Luồng khi app khởi động với Flyway

```mermaid
sequenceDiagram
    participant Developer
    participant App as Spring Boot App
    participant Flyway
    participant Database
    participant Response as App Startup Result

    Developer->>App: chạy ứng dụng
    App->>Flyway: kiểm tra migration trong thư mục db/migration
    Flyway->>Database: đọc bảng lịch sử migration
    Database-->>Flyway: migration nào đã chạy / chưa chạy
    Flyway->>Database: chạy các file SQL còn thiếu
    Database-->>Flyway: apply thành công
    Flyway-->>App: schema đã cập nhật
    App->>Database: Hibernate validate schema
    Database-->>App: schema hợp lệ
    App-->>Response: ứng dụng khởi động thành công
```

## 6. Giải thích luồng trên theo cách dễ hiểu

### Bước 1: bạn chạy ứng dụng

Lúc này Spring Boot bắt đầu khởi động.

### Bước 2: Flyway kiểm tra migration

Flyway nhìn vào thư mục migration để xem repo có những file SQL nào.

### Bước 3: Flyway hỏi database

Flyway kiểm tra database xem:

- file nào đã chạy rồi
- file nào chưa chạy

### Bước 4: Flyway chạy phần còn thiếu

Nếu có migration chưa apply, Flyway sẽ chạy SQL đó vào database.

### Bước 5: Hibernate kiểm tra schema

Sau đó Hibernate dùng `ddl-auto=validate` để kiểm tra:

- schema trong DB có khớp với entity Java hay không

Nếu khớp thì app mới lên ổn.

## 6.1. Luồng kết nối từ backend tới Supabase Postgres

```mermaid
sequenceDiagram
    participant Client
    participant Backend as Spring Boot Backend
    participant Pool as JDBC/HikariCP
    participant SupabaseDB as Supabase Postgres
    participant Response

    Client->>Backend: HTTP request
    Backend->>Pool: lấy connection
    Pool->>SupabaseDB: SQL query
    SupabaseDB-->>Pool: result rows
    Pool-->>Backend: dữ liệu
    Backend-->>Response: JSON response
```

Giải thích đơn giản:

1. Người dùng gọi API vào backend Spring Boot.
2. Backend xử lý ở controller, service, repository.
3. Khi cần đọc hoặc ghi dữ liệu, repository dùng JDBC/JPA để lấy connection.
4. Connection đó trỏ tới PostgreSQL đang chạy trên Supabase.
5. Database trả dữ liệu về cho backend.
6. Backend biến dữ liệu thành JSON response rồi trả lại cho client.

Điểm cần nhớ:

- client **không** nói chuyện trực tiếp với database
- client nói chuyện với backend
- backend mới là nơi nói chuyện với Supabase Postgres

## 7. SePay trong dự án này đang hoạt động kiểu gì?

Hiện tại backend đang ở mức:

- tạo payment request
- sinh hướng dẫn chuyển khoản
- sinh QR VietQR
- nhận webhook để xác nhận thanh toán

Nó **chưa gọi outbound SePay API thật một cách đầy đủ**.

## 7.1. Backend kết nối tới Supabase Postgres như thế nào?

Backend kết nối bằng chuỗi kết nối PostgreSQL bình thường.

Ví dụ tổng quát:

```env
DB_URL=jdbc:postgresql://db.your_project_ref.supabase.co:5432/postgres?sslmode=require
DB_USERNAME=postgres
DB_PASSWORD=your_database_password
```

Sau đó Spring Boot đọc các biến này trong `application.properties`:

- `spring.datasource.url=${DB_URL}`
- `spring.datasource.username=${DB_USERNAME}`
- `spring.datasource.password=${DB_PASSWORD}`

Nghĩa là:

- bạn không viết truy vấn “đặc biệt cho Supabase”
- bạn vẫn dùng JPA, Hibernate, Repository như Postgres bình thường

## 7.3. Lỗi rất hay gặp: copy raw connection string của Supabase vào `DB_URL`

Đây là lỗi rất dễ gặp với người mới.

Supabase thường hiển thị connection string kiểu:

```txt
postgresql://postgres:[YOUR-PASSWORD]@db.your-project.supabase.co:5432/postgres
```

Nhưng trong Spring Boot với PostgreSQL JDBC driver, nếu bạn đang dùng:

- `spring.datasource.url=${DB_URL}`
- `spring.datasource.username=${DB_USERNAME}`
- `spring.datasource.password=${DB_PASSWORD}`

thì `DB_URL` nên có dạng:

```env
DB_URL=jdbc:postgresql://db.your-project.supabase.co:5432/postgres?sslmode=require
DB_USERNAME=postgres
DB_PASSWORD=your_database_password
```

Nói ngắn gọn:

- Supabase dashboard cho bạn một connection string kiểu Postgres chung
- Spring Boot JDBC lại muốn URL bắt đầu bằng `jdbc:postgresql://`
- username và password nên tách riêng ra để datasource đọc rõ ràng hơn

Nếu copy sai, bạn rất dễ gặp lỗi như:

- `Driver org.postgresql.Driver claims to not accept jdbcUrl`

Đây không phải lỗi database chết.

Đây là lỗi vì format của `DB_URL` chưa đúng kiểu JDBC.

## 7.2. Nên dùng connection string nào?

Theo tài liệu chính thức của Supabase:

- backend chạy lâu, ổn định, có IPv6: ưu tiên **direct connection**
- backend chạy lâu nhưng môi trường chỉ có IPv4: ưu tiên **session pooler**
- serverless, edge function, job ngắn hạn: dùng **transaction pooler**

Với dự án này là Spring Boot backend chạy kiểu persistent server, lựa chọn hợp lý là:

- **direct connection** nếu môi trường deploy hỗ trợ IPv6
- nếu không hỗ trợ IPv6 thì dùng **Supavisor session mode**

Điều này quan trọng vì transaction pooler phù hợp hơn cho kết nối ngắn hạn, còn Spring Boot backend thường giữ connection lâu hơn thông qua connection pool.

## 8. SePay cần cấu hình gì?

### Chế độ 1: Mock mode

Dùng khi dev local.

Bạn để:

```env
SEPAY_MOCK_MODE=true
```

Khi đó:

- bạn chưa cần cấu hình đủ SePay thật
- có thể mô phỏng thanh toán bằng webhook test

### Chế độ 2: Non-mock mode

Dùng khi muốn gần thật hơn.

Bạn cần tối thiểu:

```env
SEPAY_MOCK_MODE=false
SEPAY_WEBHOOK_API_KEY=...
SEPAY_BANK_BIN=...
SEPAY_ACCOUNT_NUMBER=...
SEPAY_ACCOUNT_NAME=...
```

Ý nghĩa:

- `SEPAY_WEBHOOK_API_KEY`: dùng để xác thực webhook
- `SEPAY_BANK_BIN`: mã ngân hàng để tạo QR
- `SEPAY_ACCOUNT_NUMBER`: số tài khoản nhận tiền
- `SEPAY_ACCOUNT_NAME`: tên tài khoản nhận tiền

## 9. Vì sao tôi chưa thể tự apply migration ngay lúc này?

Vì trong shell hiện tại không có các biến môi trường DB thật như:

- `DB_URL`
- `DB_USERNAME`
- `DB_PASSWORD`

Nói cách khác:

- tôi có code
- tôi có migration
- nhưng tôi chưa có “địa chỉ” và “chìa khóa” để vào đúng database thật của bạn

Nếu chạy bừa khi thiếu thông tin này thì không thể apply migration vào DB thật.

## 10. Một lưu ý rất quan trọng về Supabase MCP và Flyway

Khi dùng `Supabase MCP` để apply migration trực tiếp lên database, có một điều người mới rất dễ nhầm:

- **schema trong database có thể đã đổi**
- nhưng **bảng `flyway_schema_history` chưa chắc đã được cập nhật**

Điều đó có nghĩa là:

- database thật đã có cột/bảng mới
- nhưng Flyway của ứng dụng local vẫn có thể nghĩ rằng migration đó chưa chạy

Với repo này, điều đó chưa quá nguy hiểm vì các migration gần đây chủ yếu dùng:

- `IF NOT EXISTS`
- `CREATE TABLE IF NOT EXISTS`
- `CREATE INDEX IF NOT EXISTS`

nên nếu Flyway chạy lại sau đó thì thường vẫn an toàn hơn.

Nhưng về mặt quản lý trạng thái, bạn phải hiểu rõ:

- **apply bằng MCP**
- và **apply bằng chính Flyway của app**

là hai chuyện liên quan với nhau nhưng không hoàn toàn giống nhau.

Nói dễ hiểu:

- MCP giúp đổi schema
- Flyway history giúp ghi sổ xem migration nào đã được app chính thức ghi nhận

## 10.1. Flyway checksum mismatch là gì?

`Checksum mismatch` nghĩa là:

- database nói rằng version migration nào đó đã chạy rồi
- nhưng file migration cùng version trong repo hiện tại lại khác bản đã chạy trước đây

Ví dụ dễ hiểu:

1. tuần trước bạn có file `V3.sql`
2. database đã ghi nhận checksum của `V3.sql` cũ
3. hôm nay ai đó sửa nội dung file `V3.sql`
4. khi app chạy lại, Flyway nhìn vào DB và nói:
   - “version vẫn là 3”
   - “nhưng nội dung file bây giờ không còn giống bản cũ nữa”

Khi đó Flyway sẽ chặn lại để tránh trường hợp:

- bạn tưởng DB đã đúng
- nhưng thật ra lịch sử migration đang bị mâu thuẫn

## 10.2. Có phải thấy checksum mismatch là repair ngay không?

Không.

Đây là chỗ người mới rất dễ sửa sai.

Trước khi repair, phải trả lời được:

- DB đã có đủ thay đổi mà file migration hiện tại mô tả hay chưa?

Nếu chưa có mà vẫn repair, bạn đang tự ghi vào lịch sử rằng:

- “migration này đã được áp dụng rồi”

trong khi thực tế database vẫn còn thiếu một phần schema.

Đó là repair sai.

## 10.3. Cách xử lý đúng trong dự án này

Trong lần xử lý thực tế của dự án:

1. backend đã kết nối được tới Supabase Postgres
2. Flyway báo `V3` bị checksum mismatch
3. kiểm tra schema thật trên DB trước
4. phát hiện DB đã có các cột của `V3` mới, nhưng còn thiếu một số index
5. thêm đúng các index còn thiếu
6. sau đó mới repair bản ghi version `3` trong `flyway_schema_history`
7. chạy lại app để Flyway tiếp tục ghi nhận `V5`, `V6`, `V7`

Điểm quan trọng nhất cần nhớ:

- **repair không phải là “xóa lỗi”**
- **repair là bước đồng bộ lại lịch sử**
- chỉ nên làm khi bạn chắc chắn schema thật đã khớp với nội dung migration hiện tại

## 11. Ánh xạ sang file thật của dự án

- Config ứng dụng:
  - `src/main/resources/application.properties`
- Migration:
  - `src/main/resources/db/migration/`
- Payment controller:
  - `src/main/java/com/backend/old_bicycle_project/controller/PaymentController.java`
- Payment service:
  - `src/main/java/com/backend/old_bicycle_project/service/impl/PaymentServiceImpl.java`
- File mẫu biến môi trường:
  - `.env.example`

## 12. Câu chốt dễ nhớ

- `.env` giúp app biết phải kết nối đi đâu và dùng secret nào
- `migration` giúp database cập nhật theo code mới
- `SePay mock mode` dùng cho dev nhanh
- `SePay non-mock mode` cần cấu hình thật hơn và phải cẩn thận hơn
- Supabase trong dự án này chủ yếu là PostgreSQL được host sẵn, còn Spring Boot vẫn là backend chính
- với Spring Boot, `DB_URL` phải đúng chuẩn `jdbc:postgresql://...`, không nên dán raw URI của Supabase rồi dùng nguyên xi
- nếu Flyway báo checksum mismatch, phải kiểm tra schema thật trước khi repair
