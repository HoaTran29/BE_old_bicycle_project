# Supabase Storage Bootstrap - 2026-03-14

## Mục tiêu

Hoàn thiện phần Storage tối thiểu để backend hiện tại có thể:

1. upload ảnh sản phẩm
2. thay ảnh sản phẩm khi update
3. xóa ảnh thật khỏi bucket khi xóa product
4. trả URL public cho frontend

Project áp dụng:

- Supabase project: `SWP391`
- Bucket: `product-images`

## Việc đã làm

### 1. Tạo bucket `product-images`

Đã tạo bucket trong `storage.buckets` với cấu hình:

- `public = true`
- `file_size_limit = 10485760` tương ứng `10MB`
- `allowed_mime_types = ['image/png', 'image/jpeg', 'image/webp']`

### 2. Giữ cả `anon key` và key quyền cao hơn

Theo quyết định hiện tại:

- không bỏ `SUPABASE_ANON_KEY`
- vẫn giữ key phía server để backend ưu tiên dùng khi có

Backend hiện được cấu hình theo thứ tự ưu tiên:

1. nếu có `SUPABASE_SERVICE_ROLE_KEY` thì ưu tiên key này
2. nếu không có thì fallback sang `SUPABASE_ANON_KEY`

Như vậy:

- `anon key` vẫn còn để phục vụ các use case đang cần
- backend server-side vẫn có thể đi theo hướng an toàn hơn khi đã có key quyền cao hơn

### 3. Cập nhật `StorageService`

Đã cập nhật [StorageService.java](/e:/Old_bicycle_system/BE_old_bicycle_project/old_bicycle_project/src/main/java/com/backend/old_bicycle_project/service/StorageService.java) để:

- ưu tiên `SUPABASE_SERVICE_ROLE_KEY`
- fallback sang `SUPABASE_ANON_KEY`
- luôn gửi header `apikey`
- chỉ gửi `Authorization: Bearer ...` khi key có dạng JWT cũ

Điểm này quan trọng vì key mới của Supabase dạng `sb_secret_...` không phải JWT. Nếu nhét nó vào `Bearer` như key cũ thì Storage API sẽ báo lỗi `Invalid Compact JWS`.

### 4. Cập nhật config mẫu

Đã thêm biến:

```env
SUPABASE_SERVICE_ROLE_KEY=
```

vào:

- [application.properties](/e:/Old_bicycle_system/BE_old_bicycle_project/old_bicycle_project/src/main/resources/application.properties)
- [.env.example](/e:/Old_bicycle_system/BE_old_bicycle_project/old_bicycle_project/.env.example)

## Smoke test đã chạy

### 1. `POST /api/products`

Đã chạy tạo product qua API với:

- seller JWT thật
- 3 file PNG nhỏ
- `brakeTypeId = 11111111-1111-1111-1111-111111111111`
- `frameMaterialId = 22222222-2222-2222-2222-222222222222`

Kết quả:

- request thành công
- product được tạo thành công
- backend trả về 3 URL ảnh public trong bucket `product-images`

### 2. `PUT /api/products/{id}`

Đã chạy update product bằng bộ ảnh mới.

Kết quả:

- ảnh mới upload thành công
- object ảnh cũ bị xóa khỏi `storage.objects`
- URL ảnh cũ không còn serve được
- URL ảnh mới serve `200 OK`

Trong lúc test đã phát hiện bug `orphanRemoval` ở collection `product.images`. Bug này đã được sửa trong [ProductService.java](/e:/Old_bicycle_system/BE_old_bicycle_project/old_bicycle_project/src/main/java/com/backend/old_bicycle_project/service/ProductService.java) bằng cách giữ collection cũ rồi dùng `clear()` + `addAll()` thay vì thay hẳn reference.

### 3. `DELETE /api/products/{id}`

Đã chạy delete product sau khi thêm cleanup storage.

Kết quả:

- product vẫn được soft-delete:
  - `status = hidden`
  - `deleted_at != null`
- ảnh thật trong Supabase Storage bị xóa
- metadata trong `product_images` bị dọn
- URL ảnh cũ không còn dùng được

## Kết luận

Phần Storage của MVP hiện đã đủ để:

1. tạo product qua API với ảnh thật
2. cập nhật product với bộ ảnh mới
3. xóa product và dọn ảnh thật khỏi bucket
4. lưu object lên Supabase Storage
5. nhận lại public URL để render ngoài frontend

## Việc còn lại

### 1. Policy `anon` vẫn đang là cấu hình chuyển tiếp

Hiện policy `anon` vẫn được giữ lại theo đúng scope đã chốt.

Điều này chấp nhận được cho MVP, nhưng về lâu dài bạn có thể:

- giữ nguyên nếu muốn đơn giản hóa vận hành
- hoặc siết dần sau khi chắc chắn mọi upload/delete đều đi qua backend bằng key phía server

### 2. Cần giữ đồng bộ giữa bucket policy và rule nghiệp vụ

Nếu sau này backend mở thêm:

- video
- signed upload
- private bucket
- moderation file

thì policy storage cũng phải cập nhật theo. Bucket tạo xong không có nghĩa là mọi use case mới sẽ tự chạy đúng.
