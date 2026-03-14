# Product, Storage, Order, Payment E2E Smoke Test - 2026-03-14

## Mục tiêu

Xác nhận luồng thật sau đã chạy được trên môi trường hiện tại:

1. seller tạo product qua API với ảnh thật
2. ảnh được upload lên Supabase Storage
3. product được chuyển sang `active`
4. buyer tạo order từ product đó
5. seller accept order
6. buyer tạo payment request
7. webhook mock xác nhận thanh toán
8. order chuyển sang `deposited` và `held`

Ngoài ra còn kiểm tra:

9. `DELETE /api/products/{id}` có dọn object storage
10. `PUT /api/products/{id}` thay bộ ảnh mới có xóa ảnh cũ

## Môi trường

- Backend local: `http://localhost:8080`
- Database: Supabase project `SWP391`
- Storage bucket: `product-images`
- `SEPAY_MOCK_MODE=true`
- `.env` có cả `SUPABASE_ANON_KEY` và key phía server cho storage

## Kết quả chính

### 1. Create product qua API

`POST /api/products`

Kết quả:

- pass
- product được tạo với 3 ảnh thật
- backend trả về URL public trong Supabase Storage

Ví dụ product smoke test:

- `09e561a8-4b57-4e22-9427-060a67c6f477`

### 2. Delete product cleanup

`DELETE /api/products/{id}`

Kết quả:

- product bị soft-delete:
  - `status = hidden`
  - `deleted_at != null`
- object trong `storage.objects` của product đã bị xóa sạch
- URL ảnh cũ không còn serve được

Ví dụ product delete-cleanup test:

- `d2efb8d9-e588-460c-9a5c-029db6ebdf6c`

### 3. Update product với ảnh mới

`PUT /api/products/{id}`

Kết quả:

- ảnh cũ bị xóa khỏi bucket
- ảnh mới được upload thành công
- URL ảnh cũ trả `400`
- URL ảnh mới trả `200`

Ví dụ product update-cleanup test:

- `ae6ec945-5de0-48d5-a778-54df6fbd1a63`

### 4. Order + payment full flow

Product tạo từ API:

- `09e561a8-4b57-4e22-9427-060a67c6f477`

Đã set `status = active` trên DB để cho phép đặt order.

Luồng đã chạy:

- `POST /api/orders`
- `PATCH /api/orders/{id}/accept`
- `POST /api/payments/orders/{id}/request`
- `POST /api/payments/sepay/webhook`

Order kết quả:

- `orderId = 5063b558-291a-4dee-9e2f-89e3d6c8dd78`
- `status = deposited`
- `fundingStatus = held`
- `paidAmount = 3000000`
- `remainingAmount = 14500000`

Payment kết quả:

- `paymentId = 76c7a226-b7db-4aeb-8ddb-276f7aa0208a`
- `status = success`
- `transactionReference = REF-E2E-2002`

## Bug phát hiện trong quá trình test

### 1. Key mới của Supabase không phải JWT

Lần đầu sau khi thêm key phía server, Storage API trả:

- `Invalid Compact JWS`

Nguyên nhân:

- key đang dùng là dạng `sb_secret_...`
- code cũ lại đối xử nó như JWT bearer cũ

Đã sửa [StorageService.java](/e:/Old_bicycle_system/BE_old_bicycle_project/old_bicycle_project/src/main/java/com/backend/old_bicycle_project/service/StorageService.java) để:

- luôn gửi `apikey`
- chỉ gửi `Authorization: Bearer ...` khi key là JWT kiểu cũ

### 2. Update product bị lỗi collection orphan removal

Khi thay bộ ảnh mới, backend ném:

- `A collection with orphan deletion was no longer referenced`

Nguyên nhân:

- `ProductService.update(...)` thay hẳn reference của `product.images`

Đã sửa [ProductService.java](/e:/Old_bicycle_system/BE_old_bicycle_project/old_bicycle_project/src/main/java/com/backend/old_bicycle_project/service/ProductService.java) bằng cách:

- giữ collection cũ
- dùng `clear()` và `addAll()`

## Kết luận

Luồng E2E trọng yếu hiện đã chạy được:

1. tạo product thật với ảnh thật
2. upload storage thật
3. tạo order thật
4. accept order
5. tạo payment request
6. webhook mock
7. xác nhận order và payment đổi trạng thái đúng

Ngoài ra, cleanup storage khi:

- update ảnh
- delete product

cũng đã được xác nhận.
