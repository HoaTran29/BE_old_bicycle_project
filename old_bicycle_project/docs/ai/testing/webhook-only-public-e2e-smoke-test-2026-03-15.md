# WebHook-Only Public E2E Smoke Test - 2026-03-15

## Mục tiêu

Chạy lại một luồng đầy đủ qua chính URL public `ngrok` sau khi backend đã chuyển sang `webhook-only`:

1. login buyer/seller qua public URL
2. tạo order mới
3. seller accept order
4. buyer request payment
5. gửi SePay WebHook callback qua public URL
6. verify order/payment đã cập nhật đúng

## Runtime

- Backend local: `http://localhost:8080`
- Public tunnel: `https://extrajudicial-fleta-fallalishly.ngrok-free.dev`

## Dữ liệu smoke test mới

- Product mới:
  - `4d8f1b50-57b5-4d4c-a872-46ceabb513f8`
  - `Webhook only smoke product 20260315`
- Order mới:
  - `bb2e1c1e-d22b-48a4-9225-cc8f3a9c97e9`
- Gateway order code:
  - `OB-BB2E1C1ED22B-111421`

## Các bước đã chạy

### 1. Login qua public URL

Đã gọi:

- `POST /api/auth/login` cho buyer
- `POST /api/auth/login` cho seller

Qua chính URL public `ngrok`.

### 2. Tạo order mới

Đã gọi:

- `POST /api/orders`

Payload chính:

```json
{
  "productId": "4d8f1b50-57b5-4d4c-a872-46ceabb513f8",
  "upfrontAmount": 2500000,
  "depositAmount": 2500000,
  "serviceFee": 0,
  "paymentOption": "partial",
  "paymentMethod": "transfer"
}
```

### 3. Seller accept order

Đã gọi:

- `PATCH /api/orders/bb2e1c1e-d22b-48a4-9225-cc8f3a9c97e9/accept`

### 4. Buyer request payment

Đã gọi:

- `POST /api/payments/orders/bb2e1c1e-d22b-48a4-9225-cc8f3a9c97e9/request`

Kết quả nhận được:

- `gatewayOrderCode = OB-BB2E1C1ED22B-111421`
- QR/chuyển khoản hợp lệ

### 5. Gửi WebHook-only callback

Đã gửi tới:

- `POST /api/payments/sepay/webhook`

Header:

```text
Authorization: Apikey <SEPAY_WEBHOOK_API_KEY>
ngrok-skip-browser-warning: true
```

Payload:

```json
{
  "id": 910001,
  "gateway": "sepay",
  "transactionDate": "2026-03-15 11:30:00",
  "accountNumber": "...",
  "transferType": "in",
  "transferAmount": 2500000,
  "code": "OB-BB2E1C1ED22B-111421",
  "referenceCode": "TRX-WEBHOOK-ONLY-NEW-001",
  "description": "Webhook only end-to-end smoke"
}
```

Kết quả:

```json
{
  "message": "Webhook processed successfully"
}
```

### 6. Verify trạng thái cuối

Kết quả cuối cùng:

- `payment.status = success`
- `order.status = deposited`
- `fundingStatus = held`
- `paidAmount = 2500000`
- `remainingAmount = 10700000`

## Ý nghĩa

Lượt test này xác nhận:

- `webhook-only` không chỉ compile được
- mà đã chạy được end-to-end qua đúng public URL
- không còn phụ thuộc vào `localhost` để test callback
- có thể dùng `ngrok + Swagger + JWT` để demo/test API từ bên ngoài

## Ghi chú vận hành

Khi gọi API qua URL public `ngrok`, script/client nên thêm:

```text
ngrok-skip-browser-warning: true
```

để tránh trang cảnh báo `ERR_NGROK_6024` của `ngrok free`.
