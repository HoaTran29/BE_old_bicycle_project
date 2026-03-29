# **Software Requirements Specification**
## **Old Bicycles Marketplace**


## **Document Information**

|**Field**|**Value**|
| :- | :- |
|**Project Name**|Old Bicycles Marketplace|
|**Document Version**|4\.4|
|**Date**|2026-03-29|
|**Author**|Development Team|
|**Status**|Final|
## **Revision History**

|**Version**|**Date**|**Author**|**Description**|
| :- | :- | :- | :- |
|1\.0|2026-01-21|Dev Team|Initial draft|
|2\.0|2026-01-21|Dev Team|Added Business Rules, Guest actor, detailed Use Cases, updated DB schema|
|3\.0|2026-01-28|Dev Team|Added Business Rules, updated DB schema|
|4\.0|2026-03-20|Dev Team|Synced mandatory inspection before public, manual payout/refund flow, seller reply review, and order evidence uploads|
|4\.1|2026-03-24|Dev Team|Synced groupset, size chart, payment timeout/expiry, and AI assistant mức 2 qua Spring Boot + Vercel AI Gateway|
|4\.2|2026-03-24|Dev Team|Final audit sync with shipped scope, actual integrations, auth flow, messaging scope, admin payout management, and runtime database schema|
|4\.3|2026-03-28|Dev Team|Hardened inspection detail access: public users read inspection summary via product detail, while raw inspection detail endpoint is restricted to admin/inspector tooling|
|4\.4|2026-03-29|Dev Team|Hardened backend validation: real image verification for multipart uploads, PDF-only inspection reports, sanitized storage paths, bounded pagination, stricter price-range checks, and normalized auth input|


# **1. Introduction**
## **1.1 Purpose**
Tài liệu Đặc tả Yêu cầu Phần mềm (SRS) này mô tả các yêu cầu chức năng và phi chức năng cho hệ thống **Old Bicycles Marketplace** - một nền tảng mua bán xe đạp cũ trực tuyến. Tài liệu này dành cho:



- Đội ngũ phát triển (Frontend, Backend, Mobile)
- Đội ngũ QA/Testing
- Product Owner và Stakeholders
- System Architects
- Đội ngũ vận hành và hỗ trợ


## **1.2 Scope**
### **1.2.1 Product Name**
**Old Bicycles Marketplace** (Sàn Xe Đạp Cũ)


### **1.2.2 Product Description**
Nền tảng thương mại điện tử chuyên về mua bán xe đạp cũ, kết nối người bán (Seller), người mua (Buyer), đơn vị kiểm định (Inspector), và quản trị viên (Admin). Hệ thống hỗ trợ đăng tin, tìm kiếm, chat, đặt cọc, kiểm định xe và quản lý giao dịch.


### **1.2.3 Objectives**
- Tạo nền tảng an toàn, minh bạch cho việc mua bán xe đạp cũ
- Tăng tính tin cậy thông qua hệ thống kiểm định và đánh giá
- Đơn giản hóa quy trình giao dịch với chat, đặt cọc tích hợp
- Cung cấp công cụ quản lý hiệu quả cho admin


### **1.2.4 Benefits**
- **Người bán**: Tiếp cận nhiều khách hàng, quản lý tin đăng dễ dàng
- **Người mua**: Tìm kiếm xe phù hợp, an tâm với xe đã kiểm định
- **Nền tảng**: Thu phí dịch vụ, xây dựng cộng đồng xe đạp


## **1.3 Definitions, Acronyms, and Abbreviations**

|**Term**|**Definition**|
| :- | :- |
|**Guest**|Người dùng chưa đăng nhập, chỉ xem thông tin cơ bản|
|**Buyer**|Người dùng mua xe đạp trên hệ thống|
|**Seller**|Người dùng đăng bán xe đạp trên hệ thống|
|**Inspector**|Đơn vị/cá nhân kiểm định tình trạng xe|
|**Admin**|Quản trị viên hệ thống|
|**Listing/Product**|Tin đăng bán xe|
|**Deposit**|Đặt cọc giữ chỗ xe|
|**Escrow**|Cơ chế giữ tiền trung gian|
|**Wishlist**|Danh sách xe yêu thích|
|**Verified Badge**|Nhãn xe đã kiểm định|
|**Groupset**|Bộ truyền động xe đạp|
|**Frame Size**|Kích thước khung xe|
|**SRS**|Software Requirements Specification|
|**FR**|Functional Requirement|
|**NFR**|Non-Functional Requirement|
|**BR**|Business Rule|
|**UC**|Use Case|
## **1.4 References**

|**Reference**|**Title**|**Version**|**Date**|
| :- | :- | :- | :- |
|IEEE 830-1998|IEEE Recommended Practice for SRS|1998|1998-06|
|Project Brief|Old Bicycles Marketplace Brief|1\.0|2026-01|
## **1.5 Overview**
Tài liệu được tổ chức như sau:

- **Section 1**: Giới thiệu và tổng quan
- **Section 2**: Mô tả tổng thể sản phẩm
- **Section 3**: Yêu cầu chi tiết (chức năng và phi chức năng)
- **Section 4**: Business Rules
- **Appendices**: Glossary, Models, Traceability Matrix




# **2. Overall Description**
## **2.1 Product Perspective**
### **2.1.1 System Context**
` `![old-bicycles-system-context](Aspose.Words.8bb30c8c-e38e-4947-8d97-66f6764702b5.001.png)
### **2.1.2 System Interfaces**

|**Interface**|**System**|**Description**|**Protocol**|
| :- | :- | :- | :- |
|INT-001|SePay Payment Gateway|Tạo hướng dẫn chuyển khoản/QR, nhận webhook xác nhận thanh toán, và đối soát payment phase|REST API / HTTPS|
|INT-002|Email Delivery Service|Gửi email xác thực tài khoản và reset mật khẩu|SMTP / HTTPS|
|INT-003|Chatbot AI|Hỗ trợ chăm sóc khách hàng tự động theo context user qua Spring Boot backend và Vercel AI Gateway|REST API / HTTPS|
|INT-004|Supabase Storage|Lưu trữ ảnh sản phẩm, ảnh chứng cứ và file báo cáo kiểm định|Storage API / HTTPS|
### **2.1.3 User Interfaces**
- **Responsive Web Application**: Hỗ trợ Desktop, Tablet, Mobile
- **Screen resolution**: Minimum 320px (mobile) đến 1920px+ (desktop)
- **Supported browsers**: Chrome 90+, Firefox 85+, Safari 14+, Edge 90+
- **Accessibility**: WCAG 2.1 Level AA compliant
- **Language**: Vietnamese (primary), English (future)


### **2.1.4 Hardware Interfaces**
Không yêu cầu hardware interface đặc biệt. Hệ thống hoạt động trên trình duyệt web chuẩn.


### **2.1.5 Software Interfaces**

|**Interface**|**Software**|**Version**|**Purpose**|
| :- | :- | :- | :- |
|SW-001|PostgreSQL (Supabase-hosted)|15+|Primary database|
|SW-002|Supabase Storage|Latest|Media and file storage|
|SW-003|SePay|Latest|Inbound transfer and webhook integration|
|SW-004|Vercel AI Gateway|Latest|Server-side AI assistant integration|
### **2.1.6 Communications Interfaces**
- **Protocols**: HTTPS (TLS 1.3), WebSocket/STOMP over SockJS (real-time chat)
- **Data formats**: JSON (API), WebP/JPEG/PNG (images), PDF (inspection reports)
- **Security**: JWT authentication, refresh token, email/password login, email verification, password reset via email token


## **2.2 Product Functions**
### **High-Level Features**

|**Feature ID**|**Feature Name**|**Description**|**Priority**|
| :- | :- | :- | :- |
|F-001|User Authentication|Đăng ký, đăng nhập, quản lý tài khoản|Must|
|F-002|Bike Listing|Đăng tin bán xe với ảnh, mô tả và thông số kỹ thuật; bắt buộc qua admin moderation và inspection trước khi public|Must|
|F-003|Search & Filter|Tìm kiếm và lọc xe theo nhiều tiêu chí|Must|
|F-004|Advanced Filter|Lọc theo thông số kỹ thuật (size, groupset, phanh)|Must|
|F-005|Bike Detail View|Xem chi tiết xe, ảnh, lịch sử, báo cáo kiểm định và gợi ý size theo danh mục|Must|
|F-006|Messaging System|Chat real-time giữa buyer và seller|Must|
|F-007|Wishlist|Lưu xe yêu thích|Should|
|F-008|Deposit & Order|Đặt cọc, quản lý đơn hàng, timeout/expiry, refund và payout thủ công có đối soát|Must|
|F-009|Seller Rating|Đánh giá uy tín người bán|Must|
|F-010|Inspection System|Kiểm định bắt buộc trước khi public và gắn nhãn xe|Must|
|F-011|Admin Dashboard|Quản lý toàn bộ hệ thống|Must|
|F-012|Report System|Báo cáo tin đăng/user vi phạm|Must|
|F-013|Notification System|Thông báo real-time|Must|
|F-014|Chatbot Support|Trợ lý AI mức 2 giải thích order, listing, inspection, refund và payout theo context thật của user đang đăng nhập|Could|
|F-016|Transfer Payment Integration|Tạo hướng dẫn chuyển khoản/QR, nhận webhook SePay và theo dõi payment phase của đơn hàng|Must|
## **2.3 User Characteristics**
### **2.3.1 User Classes (5 Actors)**

|**User Class**|**Description**|**Technical Level**|**Frequency**|
| :- | :- | :- | :- |
|**Guest**|Người dùng chưa đăng nhập, chỉ xem thông tin cơ bản|Basic|Casual|
|**Buyer**|Người tìm mua xe đạp cũ, đa dạng độ tuổi 18-55|Basic - Intermediate|Daily/Weekly|
|**Seller**|Cá nhân hoặc cửa hàng bán xe đạp|Basic - Intermediate|Daily|
|**Inspector**|Chuyên gia kiểm định xe đạp|Intermediate|On-demand|
|**Admin**|Nhân viên quản trị hệ thống|Advanced|Daily|
### **2.3.2 User Personas**
**Persona 1: Minh - The Buyer**

- **Role**: Nhân viên văn phòng, 28 tuổi
- **Goals**: Tìm xe đạp thể thao giá tốt để đi làm
- **Pain Points**: Lo ngại mua phải xe kém chất lượng, bị lừa
- **Technical Skills**: Sử dụng smartphone thành thạo



**Persona 2: Anh Tuấn - The Seller**

- **Role**: Chủ cửa hàng xe đạp nhỏ, 40 tuổi
- **Goals**: Mở rộng kênh bán hàng, quản lý tin đăng hiệu quả
- **Pain Points**: Mất thời gian trả lời tin nhắn, quản lý đơn hàng
- **Technical Skills**: Cơ bản, cần giao diện đơn giản



**Persona 3: Thảo - The Inspector**

- **Role**: Kỹ thuật viên xe đạp, 35 tuổi
- **Goals**: Cung cấp dịch vụ kiểm định, xây dựng uy tín
- **Pain Points**: Cần công cụ upload báo cáo chuyên nghiệp
- **Technical Skills**: Trung bình


## **2.4 Constraints**
### **2.4.1 Regulatory Requirements**
- Tuân thủ Luật Thương mại điện tử Việt Nam
- Bảo vệ dữ liệu cá nhân theo PDPD (Personal Data Protection Decree)
- Chính sách thanh toán theo quy định Ngân hàng Nhà nước


### **2.4.2 Technical Constraints**
- Web-first approach (không phát triển mobile app trong phase 1)
- Response time < 3 giây cho mọi operations
- Hệ thống hỗ trợ upload ảnh sản phẩm, ảnh chứng cứ và file báo cáo kiểm định; video chưa nằm trong phạm vi release hiện tại
- Các giới hạn số lượng/tệp được kiểm soát bởi validation của từng form FE/BE và chính sách storage đang dùng
- Ảnh upload phải là file ảnh hợp lệ ở phía backend; file báo cáo kiểm định dùng định dạng PDF và đường dẫn storage được chuẩn hóa/sanitize trước khi publish
- Các API phân trang công khai/nội bộ dùng giới hạn page >= 0 và size trong khoảng 1-100; filter giá âm hoặc minPrice > maxPrice bị từ chối ở server-side


### **2.4.3 Business Constraints**
- **Timeline**: Phase 1 hoàn thành trong 4 tháng
- **Budget**: Theo ngân sách được phê duyệt
- **Team**: 3 Frontend, 2 Backend, 1 DevOps, 1 QA


## **2.5 Assumptions and Dependencies**
### **Assumptions**

|**ID**|**Assumption**|**Impact if False**|
| :- | :- | :- |
|A-001|Người dùng có kết nối internet ổn định|Cần offline mode|
|A-002|Người dùng sử dụng trình duyệt hiện đại|Cần polyfills|
|A-003|Payment gateway sẵn sàng tích hợp|Delay phase thanh toán|
|A-004|Có đủ Inspector đăng ký sử dụng|Tính năng kiểm định không hiệu quả|
### **Dependencies**

|**ID**|**Dependency**|**Type**|**Impact**|
| :- | :- | :- | :- |
|D-001|Payment Gateway API|External|Block online payment|
|D-002|Cloud Storage Service|External|Block image upload|
|D-003|Email Provider|External|Block verify/reset email flows|
|D-004|Vercel AI Gateway|External|Block AI assistant feature|


# **3. Specific Requirements**
## **3.1 Use Cases by Actor**
### **3.1.1 Guest (Khách vãng lai - Chưa đăng nhập)**

|**UC ID**|**Use Case**|**Description**|
| :- | :- | :- |
|UC01|Đăng ký tài khoản|Người dùng chọn role Buyer hoặc Seller|
|UC02|Đăng nhập / Quên mật khẩu|Xác thực và khôi phục tài khoản|
|UC03|Tìm kiếm xe (Search basic)|Tìm kiếm theo từ khóa|
|UC04|Xem danh sách xe|Filter & Sort cơ bản|
|UC05|Xem chi tiết tin đăng|Xem thông tin xe, không xem liên hệ seller|
### **3.1.2 Buyer (Người mua)**
*Bao gồm tất cả chức năng của Guest*



|**UC ID**|**Use Case**|**Description**|
| :- | :- | :- |
|UC06|Quản lý hồ sơ cá nhân|Cập nhật profile, avatar, địa chỉ|
|UC07|Lọc nâng cao|Filter: size khung, groupset, loại phanh, chất liệu|
|UC08|Chat/Nhắn tin với người bán|Real-time messaging|
|UC09|Tạo đơn đặt mua / Đặt cọc|Booking/Deposit|
|UC10|Quản lý danh sách yêu thích|Wishlist management|
|UC11|Đánh giá người bán|Rating & Review sau giao dịch|
|UC12|Báo cáo tin đăng vi phạm|Report Ad|
### **3.1.3 Seller (Người bán)**
*Bao gồm tất cả chức năng của Guest*



|**UC ID**|**Use Case**|**Description**|
| :- | :- | :- |
|UC13|Đăng tin bán xe|Post Ad với đầy đủ thông tin kỹ thuật|
|UC14|Quản lý tin đăng|Sửa, Ẩn, Xóa, Đánh dấu đã bán|
|UC15|Theo dõi trạng thái kiểm định|Nhận kết quả kiểm định, chỉnh sửa tin khi fail và chờ admin chuyển kiểm định lại|
|UC16|Quản lý đơn hàng|Chấp nhận/Từ chối yêu cầu mua/cọc|
|UC17|Phản hồi đánh giá|Reply Review từ buyer|
### **3.1.4 Inspector (Người kiểm định)**

|**UC ID**|**Use Case**|**Description**|
| :- | :- | :- |
|UC18|Tiếp nhận yêu cầu kiểm định|Nhận các tin do admin chuyển sang hàng chờ kiểm định|
|UC19|Cập nhật Checklist kiểm tra|Khung, phuộc, truyền động, phanh, bánh|
|UC20|Upload báo cáo kiểm định|Báo cáo + Hình ảnh thực tế|
|UC21|Gắn nhãn "Verified"|Xác nhận xe đã kiểm định|
|UC22|Tham gia giải quyết tranh chấp|Cung cấp bằng chứng kỹ thuật|
### **3.1.5 Admin (Quản trị viên)**

|**UC ID**|**Use Case**|**Description**|
| :- | :- | :- |
|UC23|Quản lý người dùng|Khóa/Mở khóa tài khoản|
|UC24|Duyệt tin đăng|Reject hoặc chuyển tin sang inspection trước khi public|
|UC25|Quản lý danh mục kỹ thuật|Hãng xe, loại groupset, size chart|
|UC26|Giải quyết khiếu nại/Tranh chấp|Dựa trên báo cáo Inspector|
|UC27|Xem báo cáo thống kê|Doanh thu, user, listing, inspection|
|UC28|Quản lý payout queue|Hoàn tiền/giải ngân thủ công và nhắc user cập nhật payout profile|


## **3.2 External Interface Requirements**
### **3.2.1 User Interfaces**

|**UI-ID**|**Screen/Component**|**Description**|
| :- | :- | :- |
|UI-001|Homepage|Landing page với featured bikes, search|
|UI-002|Bike Listing Page|Danh sách xe với filters, pagination|
|UI-003|Bike Detail Page|Chi tiết xe, gallery, seller info, báo cáo kiểm định, gợi ý size theo category|
|UI-004|Login/Register|Authentication forms|
|UI-005|Seller Dashboard|Quản lý tin đăng, đơn hàng, tin nhắn|
|UI-006|Create Listing|Form đăng tin với upload ảnh và thông tin kỹ thuật|
|UI-007|Chat Interface|Real-time messaging|
|UI-008|Profile Page|Thông tin cá nhân, đánh giá, wishlist|
|UI-009|Inspector Dashboard|Quản lý kiểm định|
|UI-010|Admin Dashboard|Quản trị toàn hệ thống|
|UI-011|Report Form|Form báo cáo vi phạm|
|UI-012|Notification Center|Trung tâm thông báo|
|UI-013|Assistant Page|Trang trò chuyện với trợ lý AI theo context tài khoản|
|UI-014|Admin Payouts Page|Hàng chờ hoàn tiền/giải ngân thủ công và nhắc cập nhật payout profile|
### **3.2.2 API Interfaces**

|**API-ID**|**Endpoint Group**|**Description**|
| :- | :- | :- |
|API-001|/auth/\*|Authentication, email verification, profile, refresh token|
|API-002|/products/\*|Bike listing CRUD, public search/filter, seller listing management|
|API-003|/conversations/\*|Messaging system and message history|
|API-004|/orders/\*|Order/deposit management, delivery evidence, confirm received|
|API-005|/payments/\*|Payment request, payment instructions, SePay webhook|
|API-006|/orders/{orderId}/refunds & /admin/refunds/\*|Refund request and admin review|
|API-007|/inspections/\*|Inspection queue, internal inspection detail, evaluate, report upload, dashboard/history; public inspection summary is exposed via product detail instead of raw inspection detail endpoint|
|API-008|/notifications/\*|Notification list, unread count, mark read/read all|
|API-009|/reviews/\* & /users/{sellerId}/reviews|Buyer review and seller reply|
|API-010|/reports/\*|Report submit, my reports, admin report processing|
|API-011|/wishlist/\*|Wishlist add/remove/list|
|API-012|Reference data APIs|/brands, /categories, /brake-types, /frame-materials, /groupsets, /size-charts|
|API-013|/admin/users/\*|Admin user management|
|API-014|/admin/products/\* & /admin/dashboard/\*|Listing moderation and admin statistics|
|API-015|/payout-profiles/\*|Buyer/Seller payout profile management|
|API-016|/admin/payouts/\*|Admin manual payout completion và reminder khi thiếu payout profile|
|API-017|/assistant/\*|AI assistant chat via backend proxy|


## **3.3 Functional Requirements**
### **3.3.1 Authentication & User Management**
#### **FR-AUTH-001: User Registration**

|**Attribute**|**Value**|
| :- | :- |
|**ID**|FR-AUTH-001|
|**Description**|Hệ thống shall cho phép người dùng đăng ký tài khoản mới bằng email, mật khẩu, thông tin cá nhân cơ bản và chọn role Buyer hoặc Seller|
|**Priority**|Must|

**Inputs:**

- Email
- Mật khẩu (min 8 ký tự, có chữ hoa, số)
- Họ tên
- Số điện thoại (optional)
- Role (Buyer/Seller)



**Processing:**

1\. Validate input format

2\. Kiểm tra email chưa tồn tại

3\. Hash password với bcrypt

4\. Tạo user với `is_verified = false`, `status = active`

5\. Gửi email verification chứa token xác thực



**Outputs:**

- Success: Hiển thị trạng thái thành công và nhắc người dùng kiểm tra email để xác thực trước khi đăng nhập
- Error: Hiển thị lỗi cụ thể




#### **FR-AUTH-002: User Login**

|**Attribute**|**Value**|
| :- | :- |
|**ID**|FR-AUTH-002|
|**Description**|Hệ thống shall cho phép người dùng đăng nhập bằng email và mật khẩu|
|**Priority**|Must|


#### **FR-AUTH-003: Email/Password Login UI**

|**Attribute**|**Value**|
| :- | :- |
|**ID**|FR-AUTH-003|
|**Description**|Màn hình đăng nhập web shall chỉ hiển thị form email/mật khẩu; không hiển thị nút đăng nhập Google hoặc Facebook|
|**Priority**|Should|


#### **FR-AUTH-004: Password Reset**

|**Attribute**|**Value**|
| :- | :- |
|**ID**|FR-AUTH-004|
|**Description**|Hệ thống shall cho phép yêu cầu reset mật khẩu qua email token và đặt lại mật khẩu bằng link/token hợp lệ|
|**Priority**|Must|


#### **FR-AUTH-005: Profile Management**

|**Attribute**|**Value**|
| :- | :- |
|**ID**|FR-AUTH-005|
|**Description**|User shall có thể cập nhật thông tin cá nhân: họ tên, avatar, địa chỉ mặc định, số điện thoại và tài khoản ngân hàng dùng cho refund/payout thủ công|
|**Priority**|Must|


### **3.3.2 Bike Listing Management (Seller)**
#### **FR-SELL-001: Create Bike Listing**

|**Attribute**|**Value**|
| :- | :- |
|**ID**|FR-SELL-001|
|**Description**|Seller shall có thể đăng tin bán xe với đầy đủ thông tin kỹ thuật|
|**Priority**|Must|

**Inputs (Bắt buộc theo BR01):**

- Tiêu đề tin đăng
- Loại xe (Road, Mountain, City, Touring, BMX)
- Thương hiệu (Brand)
- **Kích thước khung** (S/M/L hoặc cm) ⭐
- **Kích thước bánh** (26, 27.5, 29, 700c) ⭐
- **Loại phanh** (Đĩa/V-brake) ⭐
- **Chất liệu khung** (Nhôm/Carbon/Thép) ⭐
- Bộ truyền động (Groupset - VD: Shimano 105)
- Tình trạng (Mới 90%, Đã qua sử dụng, Cần sửa chữa)
- Giá bán, Giá gốc (optional)
- Mô tả chi tiết
- Địa chỉ bán (Tỉnh/Thành, Quận/Huyện)



**Ảnh bắt buộc (theo BR02):**

- Ảnh toàn thân xe ⭐
- Ảnh bộ truyền động (groupset) ⭐
- Ảnh số khung (serial number) ⭐
- Ảnh bổ sung (tối đa 17 ảnh)
- Video (0-2, max 100MB)



**Processing:**

1\. Validate all required fields

2\. Validate image types

3\. Upload và compress media

4\. Tạo listing với status "Pending"

5\. Notify admin để duyệt nội dung và chuyển kiểm định

6\. Set expires\_at = created\_at + 30 ngày




#### **FR-SELL-002: Edit Bike Listing**

|**Attribute**|**Value**|
| :- | :- |
|**ID**|FR-SELL-002|
|**Description**|Seller shall có thể chỉnh sửa tin đăng đã tạo|
|**Priority**|Must|

**Constraints:**

- Sau khi sửa, tin cần được duyệt lại (status = Pending)
- Không thể sửa tin đang trong giao dịch




#### **FR-SELL-003: Hide/Show Listing**

|**Attribute**|**Value**|
| :- | :- |
|**ID**|FR-SELL-003|
|**Description**|Seller shall có thể ẩn tạm thời hoặc hiện lại tin đăng|
|**Priority**|Must|


#### **FR-SELL-004: Delete Listing**

|**Attribute**|**Value**|
| :- | :- |
|**ID**|FR-SELL-004|
|**Description**|Seller shall có thể xóa tin đăng (soft delete)|
|**Priority**|Must|

**Constraints:**

- Không thể xóa tin đang có đặt cọc active




#### **FR-SELL-005: Mark as Sold**

|**Attribute**|**Value**|
| :- | :- |
|**ID**|FR-SELL-005|
|**Description**|Seller shall có thể đánh dấu xe đã bán|
|**Priority**|Must|


#### **FR-SELL-006: Track Inspection Status**

|**Attribute**|**Value**|
| :- | :- |
|**ID**|FR-SELL-006|
|**Description**|Seller shall có thể theo dõi trạng thái kiểm định của tin đăng và chỉnh sửa/gửi lại tin khi inspection failed|
|**Priority**|Should|

**Access Note:**

- Seller và Buyer xem trạng thái/summary kiểm định qua product detail
- Endpoint raw inspection detail chỉ dành cho Admin/Inspector để tránh lộ ghi chú kỹ thuật nội bộ và metadata kiểm định


#### **FR-SELL-007: Reply to Review**

|**Attribute**|**Value**|
| :- | :- |
|**ID**|FR-SELL-007|
|**Description**|Seller shall có thể phản hồi một lần cho mỗi đánh giá từ Buyer|
|**Priority**|Should|


### **3.3.3 Search & Browse (Buyer)**
#### **FR-BUY-001: Search Bikes**

|**Attribute**|**Value**|
| :- | :- |
|**ID**|FR-BUY-001|
|**Description**|User shall có thể tìm kiếm xe theo từ khóa|
|**Priority**|Must|


#### **FR-BUY-002: Basic Filter**

|**Attribute**|**Value**|
| :- | :- |
|**ID**|FR-BUY-002|
|**Description**|User shall có thể lọc xe theo tiêu chí cơ bản|
|**Priority**|Must|

**Filter Options:**

- Loại xe (Road/MTB/City...)
- Thương hiệu
- Khoảng giá
- Địa điểm (Province/City)
- Tình trạng




#### **FR-BUY-003: Advanced Filter (NEW)**

|**Attribute**|**Value**|
| :- | :- |
|**ID**|FR-BUY-003|
|**Description**|User shall có thể lọc xe theo thông số kỹ thuật chi tiết|
|**Priority**|Must|

**Advanced Filter Options:**

- Kích thước khung (S/M/L hoặc range cm)
- Kích thước bánh (26, 27.5, 29, 700c)
- Loại phanh (Đĩa/V-brake)
- Chất liệu khung (Nhôm/Carbon/Thép)
- Groupset (Shimano, SRAM, Campagnolo)
- Có kiểm định (Verified/All)




#### **FR-BUY-004: View Bike Detail**

|**Attribute**|**Value**|
| :- | :- |
|**ID**|FR-BUY-004|
|**Description**|User shall có thể xem chi tiết xe bao gồm ảnh, mô tả, thông số, báo cáo kiểm định|
|**Priority**|Must|

**Display Information:**

- Image gallery với zoom
- Chi tiết specs (tất cả thông số kỹ thuật)
- Seller profile và rating
- Báo cáo kiểm định công khai (nếu có) - theo BR07
- Gợi ý size theo category và frame size (nếu category có size chart)
- Nút liên hệ/chat (chỉ hiện khi đã đăng nhập)
- Nút đặt cọc




#### **FR-BUY-005: Add to Wishlist**

|**Attribute**|**Value**|
| :- | :- |
|**ID**|FR-BUY-005|
|**Description**|Buyer shall có thể lưu xe vào danh sách yêu thích|
|**Priority**|Should|


#### **FR-BUY-006: Place Deposit**

|**Attribute**|**Value**|
| :- | :- |
|**ID**|FR-BUY-006|
|**Description**|Buyer shall có thể tạo đơn đặt cọc giữ xe với cơ chế Escrow qua chuyển khoản trong flow công khai hiện tại|
|**Priority**|Must|

**Business Rules (BR08):**

- Tiền cọc giữ trên hệ thống trung gian
- Nếu Seller giao đúng: hệ thống tạo seller payout pending, admin/kế toán hoàn tất chuyển tiền thủ công và lưu bankRef
- Nếu Seller hủy/xe sai mô tả: admin duyệt refund và hoàn tiền Buyer theo luồng payout thủ công có đối soát
- Buyer vẫn có thể gửi yêu cầu hoàn tiền dù chưa khai payout profile, nhưng FE phải cảnh báo và backend sẽ chặn ở bước chuyển tiền cho đến khi buyer cập nhật payout profile




#### **FR-BUY-007: Rate Seller**

|**Attribute**|**Value**|
| :- | :- |
|**ID**|FR-BUY-007|
|**Description**|Buyer shall có thể đánh giá seller CHỈ sau khi đơn hàng "Hoàn tất" và mỗi đơn chỉ được gửi một review|
|**Priority**|Must|

**Constraint (BR11):**

- Đánh giá chỉ được phép khi order.status = "Completed"
- Seller có thể gửi đúng một phản hồi cho review đó




#### **FR-BUY-008: Report Listing (NEW)**

|**Attribute**|**Value**|
| :- | :- |
|**ID**|FR-BUY-008|
|**Description**|User shall có thể báo cáo tin đăng vi phạm|
|**Priority**|Must|

**Inputs:**

- Loại vi phạm (Lừa đảo, Hàng giả, Sai mô tả, Spam)
- Mô tả chi tiết
- Bằng chứng (ảnh chụp - optional)




### **3.3.4 Messaging System**
#### **FR-MSG-001: Start Conversation**

|**Attribute**|**Value**|
| :- | :- |
|**ID**|FR-MSG-001|
|**Description**|User shall có thể bắt đầu cuộc hội thoại với user khác về một sản phẩm cụ thể|
|**Priority**|Must|


#### **FR-MSG-002: Real-time Messaging**

|**Attribute**|**Value**|
| :- | :- |
|**ID**|FR-MSG-002|
|**Description**|Hệ thống shall hỗ trợ chat real-time qua WebSocket|
|**Priority**|Must|

**Features:**

- Gửi/nhận tin nhắn text
- Gửi ảnh
- Badge chưa đọc và cập nhật realtime khi có tin mới
- Notification in-app để điều hướng lại vào cuộc trò chuyện khi cần




### **3.3.5 Inspection System (Inspector)**
#### **FR-INS-001: Receive Inspection Request**

|**Attribute**|**Value**|
| :- | :- |
|**ID**|FR-INS-001|
|**Description**|Inspector shall có thể tiếp nhận các tin đăng do admin chuyển sang hàng chờ kiểm định|
|**Priority**|Should|


#### **FR-INS-002: Submit Inspection Report**

|**Attribute**|**Value**|
| :- | :- |
|**ID**|FR-INS-002|
|**Description**|Inspector shall có thể điền checklist, upload ảnh thực tế và file báo cáo kiểm định PDF|
|**Priority**|Should|

**Checklist Items:**

- Tình trạng khung (Frame) - Score 1-5
- Tình trạng phuộc (Fork) - Score 1-5
- Hệ thống phanh (Brakes) - Score 1-5
- Hệ thống truyền động (Drivetrain) - Score 1-5
- Bánh xe (Wheels) - Score 1-5
- % mòn xích líp
- Điểm tổng thể
- Passed/Failed
- Ghi chú chuyên gia
- Ảnh minh chứng
- File báo cáo PDF




#### **FR-INS-003: Apply Verified Badge**

|**Attribute**|**Value**|
| :- | :- |
|**ID**|FR-INS-003|
|**Description**|Hệ thống shall chỉ public tin đăng khi Inspector pass; khi đó hệ thống tự động gắn nhãn "Verified" và chuyển tin sang active|
|**Priority**|Should|

**Constraint (BR05):**

- CHỈ Inspector được phép gắn nhãn, Seller không thể tự gắn



**Constraint (BR06):**

- Nhãn có hiệu lực 7 ngày hoặc cho đến khi xe được bán
- Nếu xe thay đổi linh kiện, nhãn bị hủy




#### **FR-INS-004: Dispute Support**

|**Attribute**|**Value**|
| :- | :- |
|**ID**|FR-INS-004|
|**Description**|Inspector shall có thể cung cấp bằng chứng kỹ thuật để hỗ trợ giải quyết tranh chấp|
|**Priority**|Should|


### **3.3.6 Order & Transaction Management**
#### **FR-ORD-001: Create Order**

|**Attribute**|**Value**|
| :- | :- |
|**ID**|FR-ORD-001|
|**Description**|Hệ thống shall tạo đơn hàng khi Buyer đặt cọc qua chuyển khoản hoặc thanh toán toàn bộ qua chuyển khoản trong flow công khai hiện tại|
|**Priority**|Must|

**Order Data:**

- buyer\_id, product\_id
- total\_amount, deposit\_amount, required\_upfront\_amount
- service\_fee (theo BR09)
- payment\_method (public buyer flow hiện hỗ trợ transfer; cash chỉ còn là nhánh legacy/manual nội bộ)
- status: Pending → Deposited → Awaiting Buyer Confirmation → Completed / Cancelled
- funding\_status: Unpaid → Awaiting Payment → Held → Seller Payout Pending / Refund Pending Transfer → Released / Refunded
- Khi seller chấp nhận đơn, hệ thống tạo `payment_deadline` cho khoản thanh toán ứng trước
- Nếu buyer không thanh toán đúng hạn, scheduler backend tự đổi đơn sang `cancelled` với `cancel_reason = payment_expired`
- Nếu webhook nhận tiền đến sau khi đơn đã hết hạn/hủy, hệ thống không khôi phục đơn; thay vào đó tự tạo nhánh `refund_pending_transfer` để hoàn tiền thủ công cho buyer




#### **FR-ORD-002: Seller Accept/Reject Order**

|**Attribute**|**Value**|
| :- | :- |
|**ID**|FR-ORD-002|
|**Description**|Seller shall có thể chấp nhận hoặc từ chối đơn đặt cọc, nhưng phải hoàn tất payout profile trước khi chấp nhận đơn mới|
|**Priority**|Must|


#### **FR-ORD-003: Complete Transaction**

|**Attribute**|**Value**|
| :- | :- |
|**ID**|FR-ORD-003|
|**Description**|Hệ thống shall ghi nhận seller bàn giao xe kèm ảnh chứng cứ, cho buyer xác nhận đã nhận xe, rồi tạo payout pending để admin/kế toán giải ngân tiền cọc thủ công cho seller|
|**Priority**|Must|

**Flow chính:**

- Seller báo đã giao xe phải đính kèm ít nhất 1 ảnh bàn giao
- Buyer có thể đính kèm ảnh đã nhận xe khi xác nhận
- Sau khi buyer xác nhận, đơn hoàn tất nhưng tiền chỉ chuyển trạng thái sang `seller_payout_pending`
- Chỉ khi admin hoàn tất chuyển tiền thật và nhập `bankRef`, payout mới được xem là released
- Nếu seller chưa có payout profile, hệ thống không cho chấp nhận đơn mới để tránh kẹt giải ngân ở cuối flow


#### **FR-ORD-004: Transaction Logging**

|**Attribute**|**Value**|
| :- | :- |
|**ID**|FR-ORD-004|
|**Description**|Hệ thống shall ghi log chi tiết cho payment vào, refund review, payout record, bankRef và evidence gắn với đơn hàng|
|**Priority**|Must|


### **3.3.7 Notification System (NEW)**
#### **FR-NOTIF-001: Send Notifications**

|**Attribute**|**Value**|
| :- | :- |
|**ID**|FR-NOTIF-001|
|**Description**|Hệ thống shall gửi thông báo khi có sự kiện quan trọng|
|**Priority**|Must|

**Notification Types:**

- Tin nhắn mới
- Trạng thái đơn hàng thay đổi
- Tin đăng được duyệt/từ chối
- Xe đã được kiểm định xong
- Có đánh giá mới




#### **FR-NOTIF-002: Notification Center**

|**Attribute**|**Value**|
| :- | :- |
|**ID**|FR-NOTIF-002|
|**Description**|User shall có thể xem tất cả thông báo và đánh dấu đã đọc|
|**Priority**|Must|

**Behavior:**

- Chuông thông báo trên header shall mở một dropdown nhỏ có scrollbar để preview nhanh các thông báo gần nhất.
- Dropdown shall cho phép đánh dấu đã đọc từng thông báo hoặc đọc hết ngay trong panel.
- Route `/notifications` vẫn tồn tại như trang lịch sử đầy đủ khi user bấm "Xem tất cả thông báo".

### **3.3.8 Chatbot Support (NEW)**
#### **FR-AI-001: Context-Aware Assistant**

|**Attribute**|**Value**|
| :- | :- |
|**ID**|FR-AI-001|
|**Description**|User đã đăng nhập shall có thể hỏi trợ lý AI về trạng thái đơn hàng, listing, inspection, refund hoặc payout dựa trên context thật của tài khoản hiện tại|
|**Priority**|Could|

**Behavior:**

- FE gửi lịch sử hội thoại ngắn lên backend qua `/api/assistant/chat`
- Backend lấy context thật của user hiện tại: role, order gần đây, listing gần đây, inspection summary, refund/payout summary và unread notifications
- Backend gọi Vercel AI Gateway theo kiểu server-side; không lộ API key ở browser
- Assistant chỉ giải thích và hướng dẫn theo dữ liệu hiện có, không tự thực hiện action thay user
- Nếu context không đủ hoặc AI Gateway chưa cấu hình, hệ thống phải trả thông báo rõ ràng thay vì bịa dữ liệu
- CTA hỗ trợ trên trang hướng dẫn shall điều hướng trực tiếp sang route `/assistant` thay vì mở một kênh chat hỗ trợ riêng


### **3.3.9 Admin Management**
#### **FR-ADM-001: User Management**

|**Attribute**|**Value**|
| :- | :- |
|**ID**|FR-ADM-001|
|**Description**|Admin shall có thể quản lý tất cả user accounts|
|**Priority**|Must|

**Functions:**

- View/Search/Filter users
- Ban/Unban user
- Reset password
- View user activity




#### **FR-ADM-002: Listing Moderation**

|**Attribute**|**Value**|
| :- | :- |
|**ID**|FR-ADM-002|
|**Description**|Admin shall kiểm duyệt nội dung tin đăng, từ chối hoặc chuyển tin sang inspection; không đưa thẳng public khi chưa inspection pass|
|**Priority**|Must|


#### **FR-ADM-003: Report Management**

|**Attribute**|**Value**|
| :- | :- |
|**ID**|FR-ADM-003|
|**Description**|Admin shall có thể xử lý báo cáo vi phạm|
|**Priority**|Must|


#### **FR-ADM-004: Category & Brand Management**

|**Attribute**|**Value**|
| :- | :- |
|**ID**|FR-ADM-004|
|**Description**|Admin shall có thể quản lý danh mục, thương hiệu, groupset, size chart|
|**Priority**|Must|


#### **FR-ADM-005: Dispute Resolution**

|**Attribute**|**Value**|
| :- | :- |
|**ID**|FR-ADM-005|
|**Description**|Admin shall có thể giải quyết tranh chấp dựa trên báo cáo Inspector, ảnh seller bàn giao, ảnh buyer đã nhận và hoàn tất refund/payout thủ công bằng bankRef|
|**Priority**|Must|


#### **FR-ADM-006: Analytics Dashboard**

|**Attribute**|**Value**|
| :- | :- |
|**ID**|FR-ADM-006|
|**Description**|Admin shall có thể xem thống kê và báo cáo hệ thống|
|**Priority**|Must|

**Reports:**

- Users: Total users
- Listings: Total products và trạng thái listing
- Orders: Total orders, monthly orders
- Revenue: Total revenue, monthly revenue
- Inspection: Total inspections, passed, failed


#### **FR-ADM-007: Payout Queue Management**

|**Attribute**|**Value**|
| :- | :- |
|**ID**|FR-ADM-007|
|**Description**|Admin shall có thể xem hàng chờ refund/payout thủ công, đánh dấu hoàn tất chuyển tiền bằng bankRef và nhắc buyer/seller cập nhật payout profile khi payout đang bị kẹt ở trạng thái `profile_required`|
|**Priority**|Must|




## **3.4 Non-Functional Requirements**
### **3.4.1 Performance Requirements**

|**ID**|**Description**|**Target**|
| :- | :- | :- |
|NFR-PERF-001|Page Load Time|< 3 giây (TTI)|
|NFR-PERF-002|API Response Time|< 500ms (read), < 1000ms (write)|
|NFR-PERF-003|Search Performance|< 1 giây cho 100K listings|
|NFR-PERF-004|Concurrent Users|5,000 users|
### **3.4.2 Security Requirements**

|**ID**|**Description**|**Implementation**|
| :- | :- | :- |
|NFR-SEC-001|Data Encryption|HTTPS (TLS 1.3), secret management, password hashing|
|NFR-SEC-002|Authentication|JWT, refresh tokens, email verification|
|NFR-SEC-003|Password Policy|Min 8 chars, uppercase, number, bcrypt|
|NFR-SEC-004|Input Validation|Server-side validation, XSS/SQL prevention|
|NFR-SEC-005|File Upload|Type validation, storage path control, backend ownership checks|
### **3.4.3 Reliability Requirements**

|**ID**|**Description**|**Target**|
| :- | :- | :- |
|NFR-REL-001|Availability|99\.5% uptime|
|NFR-REL-002|Data Backup|Daily, RPO 24h, RTO 4h|
|NFR-REL-003|Error Handling|Graceful degradation, logging|
### **3.4.4 Usability Requirements**

|**ID**|**Description**|**Target**|
| :- | :- | :- |
|NFR-USA-001|Mobile Responsiveness|Responsive trên mobile, tablet và desktop hiện đại|
|NFR-USA-002|Accessibility|WCAG 2.1 Level AA|
|NFR-USA-003|Navigation|90% task completion without help|


# **4. Business Rules**
## **4.1 Listing Rules (Quy tắc Tin đăng)**

|**BR ID**|**Rule**|**Description**|
| :- | :- | :- |
|**BR01**|Tiêu chuẩn thông tin kỹ thuật|Khi đăng tin, Seller PHẢI nhập: Size khung, Kích thước bánh, Loại phanh, Chất liệu khung|
|**BR02**|Hình ảnh thực tế|Tối thiểu 3 ảnh: Ảnh toàn thân, ảnh groupset, ảnh số khung (serial)|
|**BR03**|Thời hạn tin đăng|Tin có hiệu lực 30 ngày, sau đó phải gia hạn|
|**BR04**|Trạng thái tin mới|Tin mới = "Pending". Admin phải duyệt nội dung trước, sau đó chuyển sang inspection. Chỉ tin inspection pass mới được public|
## **4.2 Inspection Rules (Quy tắc Kiểm định)**

|**BR ID**|**Rule**|**Description**|
| :- | :- | :- |
|**BR05**|Inspection bắt buộc trước public|Mọi tin đăng muốn public đều phải được admin chuyển sang inspection và được inspector pass|
|**BR06**|Quy trình gắn nhãn|Nhãn "Verified" CHỈ được cấp bởi Inspector, Seller không thể tự gắn|
|**BR07**|Hiệu lực và minh bạch kiểm định|Báo cáo có giá trị 7 ngày hoặc đến khi bán. Thay đổi linh kiện hoặc relist sau khi ẩn = phải kiểm định lại. Báo cáo hợp lệ phải công khai trên trang chi tiết sản phẩm|
## **4.3 Transaction Rules (Quy tắc Giao dịch)**

|**BR ID**|**Rule**|**Description**|
| :- | :- | :- |
|**BR08**|Cơ chế Escrow|Tiền cọc giữ trung gian. Giao đúng = tạo seller payout pending. Hủy/sai = tạo refund pending transfer cho Buyer. Refund hoàn tất = listing bị ẩn và seller muốn bán lại phải qua moderation/inspection lại|
|**BR09**|Phí dịch vụ|Đơn hàng hỗ trợ lưu `service_fee` để theo dõi phí dịch vụ nếu nghiệp vụ áp dụng ở từng giao dịch|
## **4.4 Trust & Safety Rules (Quy tắc Tin cậy)**

|**BR ID**|**Rule**|**Description**|
| :- | :- | :- |
|**BR11**|Điều kiện đánh giá|Buyer CHỈ được đánh giá sau khi đơn hàng "Hoàn tất". Mỗi đơn chỉ có một review và seller chỉ được reply một lần|
|**BR12**|Xử lý tranh chấp|Kết quả từ Inspector là căn cứ ưu tiên. Admin cũng phải xem evidence bàn giao/nhận xe trước khi quyết định refund hoặc payout. Nếu payout bị kẹt vì thiếu payout profile, admin có thể nhắc buyer/seller cập nhật từ màn admin payouts|


# **Appendix A: Glossary**

|**Term**|**Definition**|
| :- | :- |
|**Listing/Product**|Tin đăng bán xe trên hệ thống|
|**Deposit**|Khoản tiền đặt cọc để giữ xe|
|**Escrow**|Cơ chế giữ tiền trung gian giữa buyer và seller|
|**Inspection**|Quá trình kiểm định tình trạng xe bởi Inspector|
|**Verified Badge**|Nhãn hiển thị xe đã được kiểm định|
|**Wishlist**|Danh sách xe yêu thích của Buyer|
|**Rating**|Điểm đánh giá uy tín (1-5 sao)|
|**Moderation**|Quá trình Admin duyệt/từ chối nội dung|
|**Groupset**|Bộ truyền động xe đạp (líp, đùi đĩa, tay đề...)|
|**Frame Size**|Kích thước khung xe (S/M/L/XL hoặc cm)|
|**Size Chart**|Bảng tham chiếu theo danh mục xe để gợi ý chiều cao phù hợp với từng frame size|


# **Appendix B: Database Schema**
## **B.1 Core Tables**
### **users**

|**Column Name**|**Data Type**|**Constraints**|**Description**|
| :- | :- | :- | :- |
|**id**|UUID|PK, Not Null|Khóa chính, định danh duy nhất cho user.|
|**email**|VARCHAR|Unique, Not Null|Địa chỉ email đăng nhập.|
|**password\_hash**|VARCHAR|Not Null|Mật khẩu đã được mã hóa (Hashed).|
|**first\_name**|VARCHAR|Nullable|Tên người dùng.|
|**last\_name**|VARCHAR|Nullable|Họ người dùng.|
|**phone**|VARCHAR|Nullable|Số điện thoại liên hệ.|
|**avatar\_url**|TEXT|Nullable|Đường dẫn ảnh đại diện.|
|**default\_address**|TEXT|Nullable|Địa chỉ mặc định để giao hàng/nhận hàng.|
|**role**|ENUM|Default: 'buyer'|Vai trò: guest, buyer, seller, inspector, admin.|
|**is\_verified**|BOOLEAN|Default: false|Trạng thái xác thực email của tài khoản.|
|**status**|ENUM|Default: 'active'|Trạng thái: active, unactive, banned.|
|**created\_at**|TIMESTAMP|Default: Now()|Thời gian tạo tài khoản.|
|**updated\_at**|TIMESTAMP|Default: Now()|Thời gian cập nhật thông tin lần cuối.|

### **refresh_tokens**

|**Column Name**|**Data Type**|**Constraints**|**Description**|
| :- | :- | :- | :- |
|**id**|UUID|PK, Not Null|Khóa chính refresh token.|
|**user\_id**|UUID|FK -> Users.id, Not Null|User sở hữu token.|
|**token**|VARCHAR|Unique, Not Null|Chuỗi refresh token lưu trong DB để revoke và rotate.|
|**expires\_at**|TIMESTAMP|Not Null|Thời điểm refresh token hết hạn.|
|**created\_at**|TIMESTAMP|Default: Now()|Thời gian tạo token.|

### **email_verifications**

|**Column Name**|**Data Type**|**Constraints**|**Description**|
| :- | :- | :- | :- |
|**id**|UUID|PK, Not Null|Khóa chính verification token.|
|**user\_id**|UUID|FK -> Users.id, Not Null|User cần xác thực email.|
|**token**|VARCHAR|Unique, Not Null|Token xác thực email được gửi qua email.|
|**expires\_at**|TIMESTAMP|Not Null|Thời điểm token hết hạn.|
|**created\_at**|TIMESTAMP|Default: Now()|Thời gian tạo token.|

### **password_reset_tokens**

|**Column Name**|**Data Type**|**Constraints**|**Description**|
| :- | :- | :- | :- |
|**id**|UUID|PK, Not Null|Khóa chính reset token.|
|**user\_id**|UUID|FK -> Users.id, Not Null|User đang yêu cầu đặt lại mật khẩu.|
|**token**|VARCHAR|Unique, Not Null|Token đặt lại mật khẩu được gửi qua email.|
|**expires\_at**|TIMESTAMP|Not Null|Thời điểm reset token hết hạn.|
|**created\_at**|TIMESTAMP|Default: Now()|Thời gian tạo token.|


### **products**

|**Column Name**|**Data Type**|**Constraints**|**Description**|
| :- | :- | :- | :- |
|**id**|UUID|PK, Not Null|Khóa chính sản phẩm.|
|**seller\_id**|UUID|FK -> Users.id|ID người bán.|
|**brand\_id**|UUID|FK -> Brands.id|ID thương hiệu xe.|
|**category\_id**|UUID|FK -> Categories.id|ID danh mục xe.|
|**brake\_type\_id**|UUID|FK -> Brake\_types.id, Not Null|ID loại phanh xe.|
|**frame\_material\_id**|UUID|FK -> Frame\_materials.id, Not Null|ID chất liệu khung xe.|
|**groupset\_id**|UUID|FK -> Groupsets.id|ID groupset chuẩn hóa để filter và quản trị master data.|
|**title**|VARCHAR|Not Null|Tiêu đề bài đăng.|
|**description**|TEXT|Nullable|Mô tả chi tiết sản phẩm.|
|**price**|NUMERIC|Not Null|Giá bán hiện tại.|
|**original\_price**|NUMERIC|Nullable|Giá gốc (lúc mua mới).|
|**frame\_size**|VARCHAR|Nullable|Kích thước khung (S, M, L, 52cm...).|
|**wheel\_size**|VARCHAR|Nullable|Kích thước bánh (29", 700c...).|
|**groupset**|VARCHAR|Nullable|Tên groupset dạng text để giữ tương thích dữ liệu cũ và hiển thị fallback.|
|**condition**|ENUM|Default: 'used'|Tình trạng: new\_90, used, needs\_repair.|
|**province**|VARCHAR|Nullable|Tỉnh/Thành phố nơi bán.|
|**district**|VARCHAR|Nullable|Quận/Huyện nơi bán.|
|**status**|ENUM|Default: 'pending'|Trạng thái tin: pending, pending_inspection, inspected_failed, active, hidden, sold.|
|**expires\_at**|TIMESTAMP|Nullable|Thời gian tin đăng hết hạn.|
|**created\_at**|TIMESTAMP|Default: Now()|Thời gian tạo tin.|
|**updated\_at**|TIMESTAMP|Default: Now()|Thời gian cập nhật tin gần nhất.|
|**deleted\_at**|TIMESTAMP|Nullable|Soft delete marker khi tin bị xóa logic.|


### **product\_images**

|**Column Name**|**Data Type**|**Constraints**|**Description**|
| :- | :- | :- | :- |
|**id**|UUID|PK, Not Null|Khóa chính hình ảnh.|
|**product\_id**|UUID|FK -> Products.id|ID sản phẩm sở hữu ảnh.|
|**url**|TEXT|Not Null|Đường dẫn file ảnh (CDN/Cloud storage).|
|**is\_primary**|BOOLEAN|Default: false|Đánh dấu ảnh đại diện (thumbnail).|
|**display\_order**|INT|Default: 0|Thứ tự hiển thị trong slider.|
|**created\_at**|TIMESTAMP|Default: Now()|Thời gian upload.|


### **Brands**

|**Column Name**|**Data Type**|**Constraints**|**Description**|
| :- | :- | :- | :- |
|**id**|UUID|PK, Not Null|Khóa chính thương hiệu.|
|**name**|VARCHAR|Unique, Not Null|Tên thương hiệu (Không được trùng).|
|**logo\_url**|TEXT|Nullable|Đường dẫn logo thương hiệu (nếu có).|
|**created\_at**|TIMESTAMP|Default: Now()|Thời gian tạo.|
### ** 
### **Categories**

|**Column Name**|**Data Type**|**Constraints**|**Description**|
| :- | :- | :- | :- |
|**id**|UUID|PK, Not Null|Khóa chính danh mục.|
|**name**|VARCHAR|Not Null|Tên hiển thị của danh mục.|
|**slug**|VARCHAR|Unique, Not Null|Tên định danh dùng cho URL (SEO friendly). VD: xe-dap-dua.|
|**parent\_id**|UUID|FK -> Categories.id|ID danh mục cha. Nếu NULL thì là danh mục gốc (Cấp 1).|
|**created\_at**|TIMESTAMP|Default: Now()|Thời gian tạo.|

### **Brake\_types**

|**Column Name**|**Data Type**|**Constraints**|**Description**|
| :- | :- | :- | :- |
|**id**|UUID|PK, Not Null|Khóa chính loại phanh.|
|**name**|VARCHAR|Unique, Not Null|Tên loại phanh (VD: Phanh đĩa dầu, Phanh V...).|
|**description**|TEXT|Nullable|Mô tả kỹ thuật ngắn gọn (nếu cần).|
|**created\_at**|TIMESTAMP|Default: Now()|Thời gian tạo.|


### **Frame\_materials**

|**Column Name**|**Data Type**|**Constraints**|**Description**|
| :- | :- | :- | :- |
|**id**|UUID|PK, Not Null|Khóa chính loại vật liệu.|
|**name**|VARCHAR|Unique, Not Null|Tên vật liệu (VD: Nhôm 6061, Carbon Nano, Titan...).|
|**description**|TEXT|Nullable|Mô tả đặc tính (VD: Nhẹ, bền, chống rỉ...).|
|**created\_at**|TIMESTAMP|Default: Now()|Thời gian tạo.|

### **Groupsets**

|**Column Name**|**Data Type**|**Constraints**|**Description**|
| :- | :- | :- | :- |
|**id**|UUID|PK, Not Null|Khóa chính groupset.|
|**name**|VARCHAR|Unique, Not Null|Tên groupset chuẩn hóa (VD: Shimano 105, SRAM Rival).|
|**description**|TEXT|Nullable|Mô tả ngắn về groupset, số speed hoặc dòng sử dụng.|
|**created\_at**|TIMESTAMP|Default: Now()|Thời gian tạo master data groupset.|

### **size_charts**

|**Column Name**|**Data Type**|**Constraints**|**Description**|
| :- | :- | :- | :- |
|**id**|UUID|PK, Not Null|Khóa chính size chart.|
|**category\_id**|UUID|FK -> Categories.id, Unique, Not Null|Mỗi category có tối đa một bảng size chart đang hoạt động.|
|**name**|VARCHAR|Not Null|Tên hiển thị của bảng hướng dẫn size.|
|**description**|TEXT|Nullable|Mô tả ngắn về phạm vi áp dụng của bảng size chart.|
|**created\_at**|TIMESTAMP|Default: Now()|Thời gian tạo size chart.|
|**updated\_at**|TIMESTAMP|Default: Now()|Thời gian cập nhật gần nhất.|

### **size_chart_rows**

|**Column Name**|**Data Type**|**Constraints**|**Description**|
| :- | :- | :- | :- |
|**id**|UUID|PK, Not Null|Khóa chính dòng size chart.|
|**size\_chart\_id**|UUID|FK -> size\_charts.id, Not Null|ID bảng size chart mà dòng này thuộc về.|
|**frame\_size**|VARCHAR|Not Null|Frame size tương ứng, ví dụ S, M, 52, 54.|
|**height\_min\_cm**|INT|Not Null|Chiều cao tối thiểu gợi ý cho frame size này.|
|**height\_max\_cm**|INT|Not Null|Chiều cao tối đa gợi ý cho frame size này.|
|**note**|TEXT|Nullable|Ghi chú bổ sung cho dòng size chart.|
|**display\_order**|INT|Default: 0|Thứ tự hiển thị các dòng trong bảng.|
|**created\_at**|TIMESTAMP|Default: Now()|Thời gian tạo dòng size chart.|

## **B.2 Inspection System**
### **inspections**

|**Column Name**|**Data Type**|**Constraints**|**Description**|
| :- | :- | :- | :- |
|**id**|UUID|PK, Not Null|Khóa chính phiếu kiểm định.|
|**product\_id**|UUID|FK -> Products.id|ID sản phẩm được kiểm tra.|
|**inspector\_id**|UUID|FK -> Users.id|ID thợ kiểm định (role = inspector).|
|**overall\_score**|NUMERIC(3,1)|Nullable|Điểm tổng kết hiển thị theo thang 1-5.|
|**frame\_score**|INT|Nullable|Điểm hạng mục khung xe theo thang 1-5.|
|**fork\_score**|INT|Nullable|Điểm hạng mục phuộc theo thang 1-5.|
|**brakes\_score**|INT|Nullable|Điểm hạng mục phanh theo thang 1-5.|
|**drivetrain\_score**|INT|Nullable|Điểm hạng mục truyền động theo thang 1-5.|
|**wheels\_score**|INT|Nullable|Điểm hạng mục bánh xe theo thang 1-5.|
|**wear\_percentage**|INT|Nullable|Tỷ lệ hao mòn ước tính, ví dụ chain/cassette wear.|
|**passed**|BOOLEAN|Default: false|Kết quả: Đạt chuẩn hay không.|
|**report\_file\_url**|TEXT|Nullable|Link file PDF báo cáo chi tiết.|
|**expert\_notes**|TEXT|Nullable|Ghi chú tổng hợp của inspector.|
|**valid\_until**|TIMESTAMP|Nullable|Thời hạn hiệu lực của kết quả kiểm định.|
|**created\_at**|TIMESTAMP|Default: Now()|Thời gian lập báo cáo.|
|**updated\_at**|TIMESTAMP|Default: Now()|Thời gian cập nhật báo cáo gần nhất.|


## **B.3 Messaging System**
### **conversations & messages**
### ` `**Conversation**

|**Column Name**|**Data Type**|**Constraints**|**Description**|
| :- | :- | :- | :- |
|**id**|UUID|PK, Not Null|Khóa chính cuộc hội thoại.|
|**product\_id**|UUID|FK -> Products.id|ID sản phẩm đang được thảo luận.|
|**buyer\_id**|UUID|FK -> Users.id|ID người muốn mua (Buyer).|
|**seller\_id**|UUID|FK -> Users.id|ID người bán (Seller).|
|**created\_at**|TIMESTAMP|Default: Now()|Thời gian bắt đầu hội thoại.|
|**updated\_at**|TIMESTAMP|Default: Now()|Thời gian có tin nhắn mới nhất (Dùng để sort list chat).|


### **Message**

|**Column Name**|**Data Type**|**Constraints**|**Description**|
| :- | :- | :- | :- |
|**id**|UUID|PK, Not Null|Khóa chính tin nhắn.|
|**conversation\_id**|UUID|FK -> Conversations.id|ID cuộc hội thoại chứa tin nhắn này.|
|**sender\_id**|UUID|FK -> Users.id|ID người gửi tin nhắn.|
|**content**|TEXT|Nullable|Nội dung văn bản của tin nhắn.|
|**image\_url**|TEXT|Nullable|Đường dẫn ảnh đính kèm (nếu gửi ảnh).|
|**is\_read**|BOOLEAN|Default: false|Trạng thái đã xem (Dùng để hiển thị "Đã xem" hoặc Badge chưa đọc).|
|**created\_at**|TIMESTAMP|Default: Now()|Thời gian gửi tin nhắn.|

## **B.4 Transaction System**
### **orders, refund_requests & order evidences**

|**Column Name**|**Data Type**|**Constraints**|**Description**|
| :- | :- | :- | :- |
|**id**|UUID|PK, Not Null|Khóa chính đơn hàng.|
|**buyer\_id**|UUID|FK -> Users.id|ID người mua.|
|**seller\_id**|UUID|FK -> Users.id|ID người bán.|
|**product\_id**|UUID|FK -> Products.id|ID sản phẩm được mua.|
|**total\_amount**|NUMERIC|Not Null|Tổng giá trị đơn hàng.|
|**deposit\_amount**|NUMERIC|Nullable|Số tiền đặt cọc (nếu có).|
|**required\_upfront\_amount**|NUMERIC|Not Null|Số tiền buyer bắt buộc phải trả trước ở bước hiện tại.|
|**paid\_amount**|NUMERIC|Default: 0|Số tiền đã ghi nhận vào hệ thống.|
|**remaining\_amount**|NUMERIC|Default: 0|Số tiền còn lại chưa thanh toán trên lý thuyết đơn hàng.|
|**service\_fee**|NUMERIC|Nullable|Phí dịch vụ sàn thu.|
|**payment\_option**|ENUM|Default: 'partial'|Lựa chọn thanh toán: partial hoặc full.|
|**status**|ENUM|Default: 'pending'|Trạng thái: pending, deposited, awaiting_buyer_confirmation, completed, cancelled.|
|**funding\_status**|ENUM|Default: 'unpaid'|Trạng thái tiền: unpaid, awaiting_payment, held, seller_payout_pending, released, refund_pending, refund_pending_transfer, refunded.|
|**payment\_method**|ENUM|Nullable|Phương thức: transfer, cash, online. Public buyer flow hiện dùng transfer; các giá trị còn lại là legacy/internal.|
|**accepted\_at**|TIMESTAMP|Nullable|Thời điểm seller chấp nhận đơn.|
|**payment\_deadline**|TIMESTAMP|Nullable|Hạn cuối buyer phải hoàn tất khoản trả trước.|
|**cancel\_reason**|ENUM|Nullable|Lý do hủy đơn: buyer\_cancelled, seller\_cancelled, admin\_cancelled, payment\_expired.|
|**cancelled\_at**|TIMESTAMP|Nullable|Thời điểm đơn hàng bị hủy.|
|**created\_at**|TIMESTAMP|Default: Now()|Thời gian tạo đơn.|
|**updated\_at**|TIMESTAMP|Default: Now()|Thời gian cập nhật đơn gần nhất.|

### **refund_requests**

|**Column Name**|**Data Type**|**Constraints**|**Description**|
| :- | :- | :- | :- |
|**id**|UUID|PK, Not Null|Khóa chính yêu cầu hoàn tiền.|
|**order\_id**|UUID|FK -> Orders.id, Not Null|Đơn hàng bị yêu cầu hoàn tiền.|
|**payment\_id**|UUID|FK -> Payments.id, Not Null|Giao dịch thanh toán liên quan.|
|**requester\_id**|UUID|FK -> Users.id, Not Null|Người gửi yêu cầu hoàn tiền, thường là buyer.|
|**amount**|NUMERIC|Not Null|Số tiền cần hoàn.|
|**reason**|TEXT|Not Null|Lý do hoàn tiền.|
|**evidence\_note**|TEXT|Nullable|Ghi chú bằng chứng do requester cung cấp.|
|**status**|ENUM|Default: 'pending'|Trạng thái: pending, approved, rejected, completed.|
|**admin\_note**|TEXT|Nullable|Kết luận hoặc ghi chú của admin.|
|**refund\_reference**|VARCHAR|Nullable|Mã tham chiếu ngân hàng khi hoàn tiền thật.|
|**reviewed\_by**|UUID|FK -> Users.id|Admin đã xử lý yêu cầu.|
|**reviewed\_at**|TIMESTAMP|Nullable|Thời điểm admin review.|
|**processed\_at**|TIMESTAMP|Nullable|Thời điểm hoàn tiền thật được xác nhận xong.|
|**created\_at**|TIMESTAMP|Default: Now()|Thời gian tạo yêu cầu.|

### **order_evidence_submissions**

|**Column Name**|**Data Type**|**Constraints**|**Description**|
| :- | :- | :- | :- |
|**id**|UUID|PK, Not Null|Khóa chính của một lần nộp chứng cứ.|
|**order\_id**|UUID|FK -> Orders.id, Not Null|Đơn hàng liên quan.|
|**submitted\_by\_user\_id**|UUID|FK -> Users.id, Not Null|Người đã gửi chứng cứ.|
|**submitted\_by\_role**|ENUM|Not Null|Vai trò của người gửi: buyer, seller, admin...|
|**evidence\_type**|ENUM|Not Null|Loại chứng cứ: seller_handover, buyer_receipt.|
|**note**|TEXT|Nullable|Ghi chú kèm bộ ảnh chứng cứ.|
|**created\_at**|TIMESTAMP|Default: Now()|Thời gian tạo submission.|

### **order_evidence_files**

|**Column Name**|**Data Type**|**Constraints**|**Description**|
| :- | :- | :- | :- |
|**id**|UUID|PK, Not Null|Khóa chính file chứng cứ.|
|**submission\_id**|UUID|FK -> order_evidence_submissions.id, Not Null|Submission mà file thuộc về.|
|**file\_url**|TEXT|Not Null|Đường dẫn file ảnh trên storage.|
|**file\_name**|VARCHAR|Nullable|Tên file gốc để hiển thị lại trên UI.|
|**content\_type**|VARCHAR|Nullable|Kiểu MIME của file, ví dụ image/jpeg.|
|**sort\_order**|INT|Default: 0|Thứ tự hiển thị của ảnh trong submission.|
|**created\_at**|TIMESTAMP|Default: Now()|Thời gian upload file.|

## **B.5 Trust & Safety**
### **reviews, reports, notifications**
### **Review**

|**Column Name**|**Data Type**|**Constraints**|**Description**|
| :- | :- | :- | :- |
|**id**|UUID|PK, Not Null|Khóa chính của đánh giá.|
|**order\_id**|UUID|FK -> Orders.id|ID đơn hàng liên quan (Đánh giá phải gắn liền với giao dịch thật).|
|**reviewer\_id**|UUID|FK -> Users.id|ID người viết đánh giá.|
|**reviewee\_id**|UUID|FK -> Users.id|ID người nhận đánh giá (Target User).|
|**rating**|INT|Check (1-5)|Điểm số đánh giá (Sao). Thường từ 1 đến 5.|
|**comment**|TEXT|Nullable|Nội dung nhận xét chi tiết.|
|**seller\_reply**|TEXT|Nullable|Phản hồi của seller cho review này.|
|**seller\_replied\_at**|TIMESTAMP|Nullable|Thời điểm seller gửi phản hồi.|
|**created\_at**|TIMESTAMP|Default: Now()|Thời gian gửi đánh giá.|


### **Report**

|**Column Name**|**Data Type**|**Constraints**|**Description**|
| :- | :- | :- | :- |
|**id**|UUID|PK, Not Null|Khóa chính phiếu báo cáo.|
|**reporter\_id**|UUID|FK -> Users.id|ID người báo cáo.|
|**target\_id**|UUID|Not Null|ID của đối tượng bị báo cáo (có thể là UserID hoặc ProductID).|
|**target\_type**|VARCHAR|Not Null|Loại đối tượng: user hoặc product (Polymorphic relationship).|
|**reason**|ENUM|Not Null|Lý do: fraud, fake, wrong\_description, spam, other.|
|**description**|TEXT|Nullable|Mô tả chi tiết sự việc vi phạm.|
|**admin\_note**|TEXT|Nullable|Ghi chú của admin khi xử lý report.|
|**status**|ENUM|Default: 'pending'|Trạng thái xử lý: pending (chờ), reviewed (đang xem), resolved (xong).|
|**processed\_by**|UUID|FK -> Users.id|Admin đã xử lý report.|
|**created\_at**|TIMESTAMP|Default: Now()|Thời gian gửi báo cáo.|
|**processed\_at**|TIMESTAMP|Nullable|Thời điểm report được xử lý xong.|


### **Notification**

|**Column Name**|**Data Type**|**Constraints**|**Description**|
| :- | :- | :- | :- |
|**id**|UUID|PK, Not Null|Khóa chính thông báo.|
|**user\_id**|UUID|FK -> Users.id, Not Null|ID người nhận.|
|**title**|VARCHAR|Not Null|Tiêu đề thông báo.|
|**content**|TEXT|Not Null|Nội dung thông báo.|
|**type**|ENUM|Not Null|Loại: order, chat, system, inspection, promotion.|
|**is\_read**|BOOLEAN|Default: false|Trạng thái đã xem chưa.|
|**metadata**|JSONB|Nullable|Dữ liệu điều hướng (VD: {order\_id: 123}).|
|**created\_at**|TIMESTAMP|Default: Now()|Thời gian gửi.|


## **B.6 Payment & Payout** 

### **Payment**

|**Column Name**|**Data Type**|**Constraints**|**Description**|
| :- | :- | :- | :- |
|**id**|UUID|PK, Not Null|Khóa chính giao dịch thanh toán.|
|**order\_id**|UUID|FK -> Orders.id, Not Null|ID đơn hàng liên quan.|
|**amount**|NUMERIC|Not Null|Số tiền thanh toán thực tế.|
|**gateway**|ENUM|Not Null|Cổng thanh toán: manual, sepay...|
|**method**|ENUM|Not Null|Phương thức thanh toán (online, transfer...).|
|**phase**|ENUM|Not Null|Giai đoạn thanh toán: upfront hoặc remaining.|
|**status**|ENUM|Default: 'pending'|pending, processing, success, failed, expired, refunded.|
|**gateway\_order\_code**|VARCHAR|Nullable|Mã nội bộ dùng để đối chiếu webhook từ cổng thanh toán.|
|**transaction\_reference**|VARCHAR|Unique|Mã giao dịch/bank reference do provider hoặc webhook trả về để đối soát.|
|**checkout\_url**|TEXT|Nullable|Link checkout do gateway trả về nếu có.|
|**qr\_code\_url**|TEXT|Nullable|Link QR thanh toán hiện cho buyer.|
|**gateway\_response**|JSONB|Nullable|Dữ liệu phản hồi nguyên bản từ cổng thanh toán.|
|**payment\_date**|TIMESTAMP|Nullable|Thời gian thanh toán thành công.|
|**expires\_at**|TIMESTAMP|Nullable|Thời điểm yêu cầu thanh toán hết hiệu lực ở phía hệ thống/gateway.|
|**created\_at**|TIMESTAMP|Default: Now()|Thời gian tạo yêu cầu thanh toán.|
|**updated\_at**|TIMESTAMP|Default: Now()|Thời gian cập nhật payment gần nhất.|

### **payout_profiles**

|**Column Name**|**Data Type**|**Constraints**|**Description**|
| :- | :- | :- | :- |
|**id**|UUID|PK, Not Null|Khóa chính payout profile.|
|**user\_id**|UUID|FK -> Users.id, Unique, Not Null|Người sở hữu tài khoản nhận tiền.|
|**bank\_code**|VARCHAR|Not Null|Tên/mã ngân hàng nhận tiền hiển thị cho user và admin.|
|**bank\_bin**|VARCHAR|Not Null|Mã BIN ngân hàng dùng để sinh VietQR.|
|**account\_number**|VARCHAR|Not Null|Số tài khoản nhận tiền.|
|**account\_name**|VARCHAR|Not Null|Tên chủ tài khoản nhận tiền.|
|**created\_at**|TIMESTAMP|Default: Now()|Thời gian tạo profile.|
|**updated\_at**|TIMESTAMP|Default: Now()|Thời gian cập nhật gần nhất.|

### **payouts**

|**Column Name**|**Data Type**|**Constraints**|**Description**|
| :- | :- | :- | :- |
|**id**|UUID|PK, Not Null|Khóa chính payout.|
|**recipient\_id**|UUID|FK -> Users.id, Not Null|Người nhận tiền thật.|
|**order\_id**|UUID|FK -> Orders.id|Đơn hàng liên quan nếu là payout cho seller.|
|**refund\_request\_id**|UUID|FK -> refund_requests.id|Yêu cầu refund liên quan nếu là payout cho buyer.|
|**type**|ENUM|Not Null|Loại payout: seller\_release hoặc refund.|
|**provider**|ENUM|Not Null|Provider payout thủ công, hiện tại là vietqr\_manual.|
|**amount**|NUMERIC|Not Null|Số tiền cần chuyển.|
|**status**|ENUM|Default: 'profile\_required'|Trạng thái: profile\_required, pending\_transfer, completed, cancelled.|
|**bank\_code**|VARCHAR|Nullable|Tên/mã ngân hàng nhận tiền.|
|**bank\_bin**|VARCHAR|Nullable|BIN dùng để tạo VietQR.|
|**account\_number**|VARCHAR|Nullable|Số tài khoản nhận tiền tại thời điểm payout.|
|**account\_name**|VARCHAR|Nullable|Tên chủ tài khoản tại thời điểm payout.|
|**transfer\_content**|VARCHAR|Nullable|Nội dung chuyển khoản/VietQR gợi ý cho kế toán.|
|**qr\_code\_url**|VARCHAR|Nullable|Link ảnh VietQR thủ công để admin/kế toán chuyển khoản.|
|**bank\_reference**|VARCHAR|Nullable|Mã giao dịch thật sau khi công ty chuyển tiền thành công.|
|**admin\_note**|TEXT|Nullable|Ghi chú nội bộ khi xử lý payout.|
|**completed\_by**|UUID|FK -> Users.id|Admin đã đánh dấu complete payout.|
|**completed\_at**|TIMESTAMP|Nullable|Thời điểm payout thật hoàn tất.|
|**created\_at**|TIMESTAMP|Default: Now()|Thời gian tạo payout record.|
|**updated\_at**|TIMESTAMP|Default: Now()|Thời gian cập nhật payout gần nhất.|


## **B.7 Wishlist**

### **Wishlist**

|**Column Name**|**Data Type**|**Constraints**|**Description**|
| :- | :- | :- | :- |
|**user\_id**|UUID|PK, FK -> Users.id|ID người dùng.|
|**product\_id**|UUID|PK, FK -> Products.id|ID sản phẩm yêu thích.|
|**created\_at**|TIMESTAMP|Default: Now()|Thời gian thêm vào wishlist.|
|*(Lưu ý: Bảng này sử dụng Composite Primary Key gồm user\_id và product\_id)*||||





# **Appendix C: Requirements Traceability Matrix**
## **C.1 Use Case to Functional Requirement Mapping**

|**UC ID**|**Use Case**|**FR IDs**|
| :- | :- | :- |
|UC01-02|Authentication|FR-AUTH-001 to 004|
|UC03-05|Browse/Search|FR-BUY-001, 002, 004|
|UC06|Profile|FR-AUTH-005|
|UC07|Advanced Filter|FR-BUY-003|
|UC08|Messaging|FR-MSG-001, 002|
|UC09|Deposit/Order|FR-BUY-006, FR-ORD-001 to 003|
|UC10|Wishlist|FR-BUY-005|
|UC11|Rating|FR-BUY-007|
|UC12|Report|FR-BUY-008|
|UC13-17|Seller Features|FR-SELL-001 to 007|
|UC18-22|Inspector Features|FR-INS-001 to 004|
|UC23-28|Admin Features|FR-ADM-001 to 007|
## **C.2 Business Rule Enforcement**

|**BR ID**|**Enforced By**|
| :- | :- |
|BR01|FR-SELL-001 (required fields)|
|BR02|FR-SELL-001 (image validation)|
|BR03|FR-SELL-001 (expires\_at logic)|
|BR04|FR-SELL-001, FR-ADM-002|
|BR05-07|FR-INS-001 to 003|
|BR08-09|FR-ORD-001 to 004|
|BR11|FR-BUY-007|
|BR12|FR-ADM-005, FR-ADM-007, FR-INS-004|


# **Appendix D: Sign-Off**
## **D.1 Stakeholder Sign-Off**

|**Stakeholder**|**Role**|**Signature**|**Date**|
| :- | :- | :- | :- |
||Product Owner|\_\_\_\_\_\_\_\_\_||
||Tech Lead|\_\_\_\_\_\_\_\_\_||
||QA Lead|\_\_\_\_\_\_\_\_\_||
||Project Manager|\_\_\_\_\_\_\_\_\_||



*End of Software Requirements Specification v2.0*
