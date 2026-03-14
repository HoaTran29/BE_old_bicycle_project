# SePay Live Non-BIDV ngrok Smoke Test

Date: 2026-03-14
Scope: `SePay non-mock + ngrok callback + MBBank account`

## Mục tiêu

Xác nhận backend có thể đi hết một luồng thanh toán gần production trong bối cảnh:

- backend chạy local
- callback public đi qua `ngrok`
- tài khoản SePay hiện tại là `MBBank`, không phải `BIDV`

## Phát hiện chính

Ban đầu live path bị lỗi `502 Bad Gateway`.

Nguyên nhân thực tế:

- backend hardcode gọi `POST /userapi/bidv/{bank_account_id}/orders`
- tài khoản SePay hiện tại trả về từ `bankaccounts/list` là:
  - `bank_code = MB`
  - `bank_short_name = MBBank`
  - `bank_bin = 970422`
- với tài khoản này, gọi nhánh `bidv/.../orders` trả về `404`

## Thay đổi đã thực hiện

- Sửa [PaymentServiceImpl.java](/e:/Old_bicycle_system/BE_old_bicycle_project/old_bicycle_project/src/main/java/com/backend/old_bicycle_project/service/impl/PaymentServiceImpl.java)
  - đọc `bankaccounts/list`
  - resolve chi tiết tài khoản ngân hàng đang cấu hình
  - chỉ gọi VA order API khi tài khoản là `BIDV`
  - nếu không phải `BIDV`, fallback sang:
    - QR chuyển khoản trực tiếp
    - nội dung chuyển khoản theo `gatewayOrderCode`
    - xác nhận hoàn tất vẫn đi bằng webhook/IPN thật

- Bổ sung test ở [PaymentServiceImplTest.java](/e:/Old_bicycle_system/BE_old_bicycle_project/old_bicycle_project/src/test/java/com/backend/old_bicycle_project/service/impl/PaymentServiceImplTest.java)
  - `BIDV -> VA order API`
  - `MBBank -> static transfer fallback`

## Cấu hình runtime dùng trong smoke test

- `SEPAY_MOCK_MODE=false`
- `SEPAY_API_TOKEN` có giá trị thật
- `SEPAY_WEBHOOK_API_KEY` có giá trị thật
- callback public:
  - `https://extrajudicial-fleta-fallalishly.ngrok-free.dev/api/payments/sepay/webhook`

## Dữ liệu smoke test

- Buyer test: `buyer.live.20260314222958@example.com`
- Seller test: `seller.live.20260314222958@example.com`
- Product test: `e26bf0a5-1c2d-42b5-aee1-2ddc6af7d355`
- Order test: `425850ae-1e56-4374-8470-86cd37bb8689`

## Các bước đã verify

1. Đăng nhập buyer/seller test
2. Buyer tạo order
3. Seller accept order
4. Buyer gọi `POST /api/payments/orders/{orderId}/request`
5. Backend gọi `bankaccounts/list` thật từ SePay
6. Backend phát hiện tài khoản hiện tại là `MBBank`
7. Backend không gọi nhánh `bidv/.../orders` nữa
8. Backend trả về:
   - `qrCodeUrl`
   - `bankBin=970422`
   - `bankAccountNumber=0363565884`
   - `gatewayOrderCode`
9. Gửi IPN mô phỏng qua chính URL `ngrok` public với `X-Secret-Key`
10. Backend xử lý callback thành công và đổi trạng thái order/payment

## Kết quả

- `payment request`: pass
- `public IPN callback via ngrok`: pass
- `payment.status`: `success`
- `order.status`: `deposited`
- `order.fundingStatus`: `held`

## Ý nghĩa

Milestone này chứng minh:

- non-mock payment path không còn chỉ là code chưa chạy
- callback public đã đi qua được backend local
- integration hiện tại dùng được với ngân hàng không phải `BIDV` theo hướng:
  - chuyển khoản trực tiếp
  - xác nhận trạng thái bằng webhook/IPN thật

## Việc còn lại

- nếu muốn dùng live order/VA API sâu hơn, cần làm rõ ngân hàng nào SePay hỗ trợ cho nhánh này ngoài `BIDV`
- thêm automated integration test cho payment callback
- cân nhắc lưu `bank_account_id` chính xác trong `.env` để giảm phụ thuộc vào bước auto-resolve
