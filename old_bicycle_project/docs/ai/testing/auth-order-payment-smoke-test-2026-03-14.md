# Auth, Order, Payment Smoke Test - 2026-03-14

## Mục tiêu

Xác nhận luồng tối thiểu sau đã chạy được trên môi trường thật:

1. đăng ký và đăng nhập `seller`
2. đăng ký và đăng nhập `buyer`
3. buyer tạo `order`
4. seller `accept order`
5. buyer tạo `payment request`
6. webhook SePay mock cập nhật thanh toán
7. kiểm tra lại `payment history`, `order status`, và `notification`

Môi trường:

- Backend Spring Boot chạy local trên `http://localhost:8080`
- Database thật là Supabase project `SWP391`
- `SEPAY_MOCK_MODE=true`

## Điều kiện trước khi test

- Flyway trên `SWP391` đã đồng bộ tới `V7`
- Kết nối JDBC tới Supabase Postgres đã chạy được
- Vì bucket Supabase Storage `product-images` chưa tồn tại, luồng tạo product qua API chưa đi hết được
- Để không chặn smoke test payment, một product tối thiểu đã được seed trực tiếp vào DB

## Dữ liệu smoke test

- Seller email: `smoke.seller.20260314112847@example.com`
- Buyer email: `smoke.buyer.20260314112847@example.com`
- Product id: `43025d31-57ec-4025-b264-0279be7bce4e`

## Kết quả từng bước

### 1. Auth

`POST /api/auth/register`

- seller: pass
- buyer: pass

`POST /api/auth/login`

- seller: pass
- buyer: pass

Kết quả:

- lấy được JWT cho cả seller và buyer
- role hoạt động đúng với `ROLE_SELLER` và `ROLE_BUYER`

## 2. Order

`POST /api/orders`

- buyer tạo order với:
  - `productId = 43025d31-57ec-4025-b264-0279be7bce4e`
  - `upfrontAmount = 2000000`
  - `paymentMethod = transfer`
  - `paymentOption = partial`

Kết quả:

- pass
- tạo ra order `39275eaa-429e-41c6-bc26-34cd7561d125`
- trạng thái ban đầu:
  - `status = pending`
  - `fundingStatus = unpaid`

`PATCH /api/orders/{orderId}/accept`

- seller accept order

Kết quả:

- pass
- order chuyển sang:
  - `status = pending`
  - `fundingStatus = awaiting_payment`
- đã có:
  - `acceptedAt`
  - `paymentDeadline`

## 3. Payment request

`POST /api/payments/orders/{orderId}/request`

Lần chạy đầu:

- fail
- nguyên nhân thật:
  - cột `payments.gateway_response` là `jsonb`
  - entity `Payment.gatewayResponse` đang bị gửi xuống Postgres như `varchar`

Lỗi đã thấy trong log:

- `ERROR: column "gateway_response" is of type jsonb but expression is of type character varying`

Trong cùng lúc, notification async cũng lộ lỗi tương tự:

- `notifications.metadata` là `jsonb`
- entity `Notification.metadata` cũng đang bị gửi như `varchar`

## 4. Runtime fix

Đã sửa 2 entity:

- [Payment.java](/e:/Old_bicycle_system/BE_old_bicycle_project/old_bicycle_project/src/main/java/com/backend/old_bicycle_project/entity/Payment.java)
- [Notification.java](/e:/Old_bicycle_system/BE_old_bicycle_project/old_bicycle_project/src/main/java/com/backend/old_bicycle_project/entity/Notification.java)

Cách sửa:

- thêm `@ColumnTransformer(read = \"...::text\", write = \"?::jsonb\")`

Mục tiêu:

- khi ghi dữ liệu, Hibernate ép parameter sang `jsonb`
- khi đọc dữ liệu, Postgres trả về dạng text dễ map lại vào `String`

## 5. Payment request sau khi fix

`POST /api/payments/orders/{orderId}/request`

Kết quả:

- pass
- tạo payment `542f25ca-90ae-4188-aafa-77a0904069f4`
- response trả về:
  - `gateway = sepay`
  - `phase = upfront`
  - `status = processing`
  - `gatewayOrderCode = OB-39275EAA429E-113137`
  - `mockMode = true`

## 6. Webhook SePay mock

`POST /api/payments/sepay/webhook`

Payload dùng để test:

- `transferType = in`
- `transferAmount = 2000000`
- `code = OB-39275EAA429E-113137`
- `referenceCode = REF-SMOKE-1001`

Kết quả:

- pass
- backend ghi nhận webhook thành công

## 7. Kiểm tra sau webhook

`GET /api/payments/orders/{orderId}`

Kết quả:

- payment chuyển sang:
  - `status = success`
  - `transactionReference = REF-SMOKE-1001`

`GET /api/orders/me`

Kết quả:

- order chuyển sang:
  - `status = deposited`
  - `fundingStatus = held`
  - `paidAmount = 2000000`
  - `remainingAmount = 13000000`

Kiểm tra DB `notifications`:

- buyer đã có notification `Thanh toan dat coc thanh cong`
- seller đã có notification `Order da duoc thanh toan tien dat coc`
- `metadata` được lưu thành công dưới dạng `jsonb`

## Kết luận

Luồng smoke test cốt lõi sau đã chạy thành công trên DB thật:

1. auth
2. create order
3. accept order
4. create payment request
5. webhook mock
6. verify payment history
7. verify order status
8. verify notification side-effect

## Vấn đề còn lại

### 1. Product create API chưa đi hết end-to-end

Hiện vẫn bị chặn bởi Supabase Storage:

- bucket `product-images` chưa tồn tại

Điều này không chặn luồng payment smoke test, nhưng chặn luồng tạo product qua API thật.

### 2. Smoke test hiện chưa cover non-mock SePay

Hiện mới xác nhận được:

- mock mode của payment flow

Chưa xác nhận:

- webhook key thật
- cấu hình ngân hàng nhận tiền thật
- luồng gần production hơn với SePay non-mock
