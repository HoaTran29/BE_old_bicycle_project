# WebHook-Only ngrok Swagger Smoke Test - 2026-03-15

## Mục tiêu

Xác nhận sau khi chuyển sang `SePay WebHook-only`, backend có thể:

1. chạy local ở `:8080`
2. được expose public qua `ngrok`
3. mở được Swagger/OpenAPI qua URL public
4. nhận `SePay WebHook` qua endpoint public

## Runtime dùng trong lượt test

- Backend local:
  - `http://localhost:8080`
- Public tunnel:
  - `https://extrajudicial-fleta-fallalishly.ngrok-free.dev`
- WebHook endpoint:
  - `https://extrajudicial-fleta-fallalishly.ngrok-free.dev/api/payments/sepay/webhook`

## Những gì đã verify

### 1. Local OpenAPI

Request:

```text
GET http://localhost:8080/v3/api-docs
```

Kết quả:

- `200 OK`

### 2. Public OpenAPI qua ngrok

Request:

```text
GET https://extrajudicial-fleta-fallalishly.ngrok-free.dev/v3/api-docs
```

Kết quả:

- với request thường: bị chèn trang cảnh báo `ERR_NGROK_6024`
- với header `ngrok-skip-browser-warning: true`: trả JSON OpenAPI hợp lệ

### 3. Public Swagger UI qua ngrok

Request:

```text
GET https://extrajudicial-fleta-fallalishly.ngrok-free.dev/swagger-ui/index.html
```

Kết quả:

- resource Swagger UI tải được
- với client tự động nên thêm header `ngrok-skip-browser-warning: true`

### 4. Login qua public URL

Đã verify:

- `POST /api/auth/login` qua URL `ngrok` thành công với user smoke test

Điều này chứng minh:

- tunnel không chỉ mở được tài liệu
- mà còn gọi được API protected thật

### 5. WebHook-only callback qua public URL

Đã gửi payload WebHook kiểu:

- `code`
- `transferType`
- `transferAmount`
- `referenceCode`
- `transactionDate`

Kèm header:

```text
Authorization: Apikey <SEPAY_WEBHOOK_API_KEY>
```

Kết quả:

- backend trả:

```json
{
  "code": 1000,
  "message": "Webhook processed successfully"
}
```

Request này được gửi tới một payment/order đã tồn tại từ smoke test trước, nên đây là một replay test theo hướng idempotent.

## Kết luận

Sau khi chuyển sang `webhook-only`:

- backend local chạy ổn
- `ngrok` expose public thành công
- Swagger/OpenAPI public khả dụng
- endpoint WebHook public hoạt động

## Lưu ý vận hành

1. `ngrok free` có thể chèn trang cảnh báo `ERR_NGROK_6024` cho request kiểu browser.
2. Với API client/script, nên thêm header:

```text
ngrok-skip-browser-warning: true
```

3. Với người dùng mở Swagger bằng trình duyệt, có thể phải đi qua màn hình cảnh báo của `ngrok` trước khi vào Swagger UI.
4. URL `ngrok` free có thể đổi sau mỗi lần restart tunnel.
