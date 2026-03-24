# Size Chart Theo Category: API Và Schema cho người mới

## 1. Bài toán

`frame_size` trên `products` chỉ cho biết chiếc xe đang bán có size gì.

Nó chưa trả lời được câu hỏi:

> người cao bao nhiêu thì hợp size đó?

Vì vậy hệ thống thêm `size_chart` để buyer xem được bảng gợi ý chiều cao theo từng danh mục xe.

## 2. Vì sao chart gắn theo category

Ở tranche này, size chart không gắn theo từng model riêng.

Nó gắn theo `category` vì đây là mức đơn giản và đủ thực dụng cho MVP:

- Road Bike có một bảng gợi ý riêng
- MTB có một bảng gợi ý riêng
- Gravel có thể có bảng riêng nếu admin tạo

Lý do:

- ít bảng hơn
- dễ cho admin quản lý
- đủ tốt để buyer có tham chiếu ban đầu

## 3. Hai bảng mới

### `size_charts`

Giữ metadata chung của một bảng size.

Ví dụ:

- bảng này thuộc category nào
- tên bảng là gì
- mô tả ngắn

### `size_chart_rows`

Giữ từng dòng cụ thể bên trong bảng đó.

Ví dụ:

- frame size `54`
- chiều cao `170-178 cm`
- ghi chú thêm nếu cần

Quan hệ là:

- một `size_chart`
- có nhiều `size_chart_rows`

## 4. API chính

### Public

- `GET /api/size-charts/category/{categoryId}`

Trả về size chart của category đó.

Nếu category chưa có chart thì trả `result = null`, không ném lỗi.

### Admin

- `GET /api/admin/size-charts`
- `POST /api/admin/size-charts`
- `PUT /api/admin/size-charts/{id}`
- `DELETE /api/admin/size-charts/{id}`

Admin là bên tạo và duy trì bảng size chart.

## 5. Validation quan trọng

Backend chặn các case sau:

- một category có nhiều hơn một size chart
- không có dòng nào trong chart
- trùng `frame_size` trong cùng một chart
- `height_min_cm > height_max_cm`
- thiếu `name`, `categoryId`, hoặc `frameSize`

Mục tiêu là tránh bảng size bị rác hoặc tự mâu thuẫn.

## 6. Product dùng dữ liệu này thế nào

`products` vẫn giữ:

- `category_id`
- `frame_size`

Khi buyer mở trang chi tiết xe:

1. lấy `category_id` của product
2. gọi size chart theo category
3. so `frame_size` của product với từng row
4. highlight dòng phù hợp

Tức là:

- `frame_size` là dữ liệu của chiếc xe thật
- `size_chart` là bảng tham khảo chung

## 7. Điểm giới hạn cố ý của tranche này

Chưa làm:

- size chart theo từng brand/model
- filter buyer theo chiều cao

Tranche này chỉ chốt:

- schema
- admin CRUD
- public API
- dữ liệu đủ để FE hiển thị gợi ý size ở bike detail

## 8. Chốt ngắn

`size_chart` giúp hệ thống chuyển từ:

- chỉ biết xe size gì

sang:

- biết size đó thường hợp với người cao khoảng nào

Đây là lớp tư vấn cho buyer, không thay thế việc fit xe thực tế.
