# Product và Inspection Hardening Cơ Bản

## Bối cảnh

Trong đợt làm việc này, phần backend của dự án được sửa để cho module `Product` và `Inspection` bám sát SRS hơn.

Những điểm chính đã làm là:

- Bắt buộc đủ thông tin kỹ thuật khi đăng tin
- Bắt buộc tối thiểu 3 ảnh
- Tự đặt thời hạn tin đăng 30 ngày
- Xóa mềm thay vì xóa cứng
- Hiển thị `verified badge` dựa trên kết quả kiểm định thật
- Đưa thông tin báo cáo kiểm định vào response của sản phẩm

Tài liệu này giải thích các khái niệm cốt lõi ở mức dễ hiểu cho người mới học.

## 1. Validation là gì?

### Định nghĩa

`Validation` là bước kiểm tra dữ liệu đầu vào có hợp lệ hay không trước khi lưu vào hệ thống.

Nói đơn giản:

- người dùng gửi dữ liệu lên
- backend kiểm tra dữ liệu đó có đủ và đúng không
- nếu sai thì chặn lại ngay

### Vì sao quan trọng?

Nếu không có validation:

- dữ liệu xấu sẽ chui vào database
- về sau rất khó sửa
- giao diện có thể hiện thông tin thiếu hoặc sai

### Ví dụ nhỏ

Ví dụ người bán đăng xe nhưng quên nhập `frame size`.

Nếu backend không kiểm tra:

- tin vẫn được tạo
- buyer vào xem sẽ thấy thông số bị thiếu
- rule của SRS bị vi phạm

### Áp dụng trong dự án này

Ở slice này, backend đã kiểm tra thêm:

- `frameSize` bắt buộc
- `wheelSize` bắt buộc
- tối thiểu `3` ảnh

Điều này giúp `FR-SELL-001`, `BR01`, `BR02` được enforce tốt hơn.

### Lỗi hiểu sai thường gặp

Nhiều người mới nghĩ rằng chỉ cần frontend kiểm tra là đủ. Điều đó không đúng.

Frontend chỉ giúp người dùng thao tác thuận tiện hơn. Backend mới là lớp chốt cuối cùng để bảo vệ dữ liệu.

## 2. Soft Delete là gì?

### Định nghĩa

`Soft delete` nghĩa là không xóa hẳn dữ liệu khỏi database, mà chỉ đánh dấu là bản ghi đó đã bị xóa.

Thường cách làm là thêm một cột như:

- `deleted_at`

Nếu cột này có giá trị, hệ thống hiểu rằng bản ghi đó đã bị xóa mềm.

### Vì sao quan trọng?

Nếu xóa cứng (`hard delete`):

- dữ liệu biến mất hoàn toàn
- khó audit
- khó khôi phục
- khó truy vết xem trước đó chuyện gì đã xảy ra

### Ví dụ nhỏ

Giả sử một tin đăng bị người bán xóa.

`Hard delete`:

- dòng dữ liệu bị xóa khỏi bảng

`Soft delete`:

- dòng vẫn còn
- nhưng `deleted_at = thời điểm xóa`

### Áp dụng trong dự án này

Trước đây, backend gọi `productRepository.delete(product)`.

Bây giờ backend:

- gán `deletedAt`
- đổi trạng thái sang `hidden`
- khi search public thì loại các sản phẩm đã bị soft delete

### Lỗi hiểu sai thường gặp

Soft delete không chỉ là “thêm cột vào bảng”.

Nếu bạn thêm `deleted_at` nhưng quên sửa query, thì dữ liệu “đã xóa” vẫn có thể hiện ra ngoài.

Cho nên soft delete luôn đi kèm với việc sửa:

- repository
- specification
- service đọc dữ liệu

## 3. Verified Badge là gì?

### Định nghĩa

`Verified badge` là dấu xác thực cho biết sản phẩm đã được kiểm định và đang còn hiệu lực.

Ở đây cần tách 2 ý:

- đã từng được kiểm định
- đang còn hiệu lực xác minh

Hai ý này không hoàn toàn giống nhau.

### Vì sao quan trọng?

Một báo cáo kiểm định có thể:

- đã pass
- nhưng đã hết hạn

Nếu hệ thống vẫn hiện badge trong trường hợp đó, buyer sẽ bị hiểu sai.

### Ví dụ nhỏ

Một xe được kiểm định ngày 1.

Báo cáo chỉ có hiệu lực 7 ngày.

Đến ngày 10:

- báo cáo vẫn tồn tại
- nhưng badge `verified` không còn nên hiện `false`

### Áp dụng trong dự án này

Trước đây, `ProductResponse` đang hard-code `isVerified = false`.

Bây giờ backend tính `isVerified` từ dữ liệu kiểm định thật:

- inspection tồn tại
- `passed = true`
- `validUntil` chưa hết hạn
- sản phẩm chưa bị ẩn/xóa/bán

Đây là một ví dụ rất điển hình của việc **không hard-code trạng thái hiển thị**, mà suy ra nó từ dữ liệu nguồn.

### Lỗi hiểu sai thường gặp

Người mới hay lưu thêm một cột boolean `is_verified` rồi sửa tay ở nhiều nơi.

Cách đó dễ gây lệch dữ liệu:

- inspection hết hạn nhưng cột boolean chưa cập nhật
- sản phẩm bị sửa rồi nhưng badge vẫn còn

Khi có thể, nên suy ra trạng thái từ dữ liệu gốc thay vì nhân bản trạng thái ra nhiều nơi.

## 4. Invalidate Inspection nghĩa là gì?

### Định nghĩa

`Invalidate` nghĩa là làm cho một trạng thái cũ không còn hợp lệ nữa.

Trong bài toán này:

- nếu seller sửa lại thông tin sản phẩm
- thì kiểm định cũ không nên tiếp tục được tin là còn đúng

### Vì sao quan trọng?

SRS có rule: thay đổi linh kiện thì nhãn kiểm định phải bị hủy.

Nếu không invalidate:

- buyer sẽ thấy badge verified cũ
- nhưng thực tế sản phẩm đã thay đổi

### Ví dụ nhỏ

Xe ban đầu dùng groupset A và đã pass kiểm định.

Sau đó seller thay groupset B.

Nếu vẫn giữ nguyên badge cũ, buyer sẽ tưởng rằng xe hiện tại cũng đã được kiểm định với groupset B, trong khi điều đó không đúng.

### Áp dụng trong dự án này

Khi update sản phẩm, backend hiện:

- reset status về `pending`
- đánh dấu inspection cũ không còn hợp lệ

Điều này làm cho `isVerified` tự động trở về `false`.

### Lỗi hiểu sai thường gặp

Nhiều bạn chỉ reset status của product nhưng quên động vào inspection.

Kết quả là:

- status thì đổi
- nhưng response vẫn đọc inspection cũ và hiện verified sai

## 5. Vì sao phải sửa cả controller, service, repository, entity, migration và test?

### Định nghĩa ngắn

Một feature backend thường không nằm ở một file.

Nó đi qua nhiều lớp:

- `controller`: nhận request
- `service`: xử lý logic nghiệp vụ
- `repository`: lấy/lưu dữ liệu
- `entity`: mô tả dữ liệu
- `migration`: thay đổi cấu trúc database
- `test`: chứng minh logic đang đúng

### Ví dụ nhỏ

Muốn thêm `soft delete`:

- chỉ sửa `controller` là không đủ
- chỉ sửa `entity` cũng không đủ

Bạn phải:

1. thêm `deleted_at` vào database
2. thêm field vào entity
3. sửa query để loại dữ liệu đã xóa
4. sửa service để dùng xóa mềm
5. viết test để chứng minh nó hoạt động

### Áp dụng trong dự án này

Slice này là ví dụ rất rõ của “một thay đổi nghiệp vụ kéo theo nhiều lớp”.

Backend đã được sửa ở:

- `ProductController`
- `ProductService`
- `ProductSpecification`
- `InspectionServiceImpl`
- `ProductRepository`
- `InspectionRepository`
- `Product` entity
- migration `V7`
- `ProductServiceTest`

### Lỗi hiểu sai thường gặp

Người mới rất hay sửa một chỗ và nghĩ feature đã xong.

Ví dụ:

- thêm field trong entity
- nhưng không có migration

hoặc:

- sửa service
- nhưng không có test

Khi đó code nhìn có vẻ nhiều hơn, nhưng feature vẫn chưa “done” thật.

## 6. Bài học thực tế rút ra từ lần sửa này

### Điều quan trọng nhất

Khi làm backend theo SRS, đừng hỏi:

- “Repo đã có bao nhiêu controller?”

Hãy hỏi:

- “Rule nghiệp vụ đã được enforce ở đâu?”
- “Database có support rule đó không?”
- “Response trả ra có phản ánh đúng trạng thái thật không?”
- “Có test chứng minh chưa?”

### Một câu chốt dễ nhớ

Backend tốt không phải là backend có nhiều file.

Backend tốt là backend làm cho dữ liệu đúng, flow đúng, và trạng thái hiển thị đúng theo nghiệp vụ.
