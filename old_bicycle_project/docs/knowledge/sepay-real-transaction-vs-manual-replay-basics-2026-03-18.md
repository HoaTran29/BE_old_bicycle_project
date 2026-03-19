# SePay Giao Dịch Thật Và Replay Thủ Công: giải thích cho người mới học

## 1. Bài toán dễ gây hiểu nhầm

Có lúc bạn sẽ thấy:

- giao dịch thật từ SePay trả `400 Payment validation failed`
- nhưng khi replay thủ công vào webhook thì order lại xác nhận thành công

Nghe có vẻ mâu thuẫn, nhưng thực ra đây là dấu hiệu debug rất có ích.

## 2. Nếu replay thủ công pass thì điều đó chứng minh gì?

Nếu backend nhận một payload thủ công như:

- `transferType = in`
- `transferAmount` đủ lớn
- `content` chứa đúng mã `OB-...`

và order chuyển sang:

- `deposited`
- `held`

thì điều đó chứng minh:

- webhook URL đúng
- API key đúng
- backend parser đúng
- backend có thể map payment đúng nếu nhận được payload hợp lệ

Nói ngắn gọn:

- **hệ thống webhook không chết**

## 3. Vậy vì sao giao dịch thật vẫn fail?

Khi giao dịch thật vẫn trả:

- `Payment validation failed`

thì thường nghĩa là **payload thật SePay gửi lên không giống payload mà backend đang chờ**.

Trong project này, backend chỉ xác nhận nếu:

1. tìm được đúng payment theo mã `OB-...`
2. `transferAmount >= payment.amount`

Nếu một trong hai điều kiện này sai, backend sẽ trả `1015`.

## 4. Những khả năng phổ biến nhất

### Trường hợp 1: mất mã `OB-...`

Nội dung giao dịch thật có thể:

- bị người dùng sửa tay
- app ngân hàng không giữ nguyên `addInfo`
- không chuyển bằng QR vừa sinh ra
- bị SePay nhận được `content/description` khác dự kiến

Nếu backend không còn thấy đúng chuỗi `OB-...`, nó không thể tìm đúng payment.

### Trường hợp 2: số tiền nhận vào nhỏ hơn số tiền ứng trước

Ví dụ backend đang chờ:

- `2000`

nhưng webhook thật gửi:

- `1999`
- hoặc `null`

thì backend cũng sẽ từ chối.

## 5. Cách kiểm tra đúng

Trong SePay logs, mở phần `Request` của giao dịch fail rồi xem 4 field:

- `code`
- `content`
- `description`
- `transferAmount`

Đây là 4 field quan trọng nhất để biết backend có đủ dữ liệu xác nhận hay không.

## 6. Kết luận thực dụng

Nếu replay thủ công pass nhưng giao dịch thật fail thì:

- đừng sửa webhook URL vội
- đừng đổi API key vội

Vì lúc đó vấn đề thường không nằm ở cấu hình endpoint nữa.

Vấn đề nằm ở:

- dữ liệu transaction thật mà SePay gửi lên

Tức là phải debug **payload thật**, không phải chỉ debug cấu hình webhook.
