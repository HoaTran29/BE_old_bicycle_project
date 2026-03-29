package com.backend.old_bicycle_project.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    UNCATEGORIZED_EXCEPTION(9999, "Lỗi không xác định", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_KEY(1001, "Key không hợp lệ", HttpStatus.BAD_REQUEST),
    USER_EXISTED(1002, "User đã tồn tại", HttpStatus.BAD_REQUEST),
    USERNAME_INVALID(1003, "Username phải có ít nhất 3 ký tự", HttpStatus.BAD_REQUEST),
    INVALID_PASSWORD(1004, "Password phải có ít nhất 8 ký tự và bao gồm chữ hoa và số", HttpStatus.BAD_REQUEST),
    USER_NOT_EXISTED(1005, "User không tồn tại", HttpStatus.NOT_FOUND),
    UNAUTHENTICATED(1006, "Chưa xác thực", HttpStatus.UNAUTHORIZED),
    UNAUTHORIZED(1007, "Bạn không có quyền", HttpStatus.FORBIDDEN),
    RESOURCE_NOT_FOUND(1008, "Resource không tồn tại", HttpStatus.NOT_FOUND),
    PRODUCT_NOT_FOUND(1009, "Sản phẩm không tồn tại", HttpStatus.NOT_FOUND),
    FORBIDDEN(1010, "Bạn không có quyền thực hiện hành động này", HttpStatus.FORBIDDEN),
    INVALID_STATUS(1011, "Trạng thái không hợp lệ", HttpStatus.BAD_REQUEST),
    RECORD_ALREADY_EXISTS(1012, "Bản ghi đã tồn tại", HttpStatus.CONFLICT),
    RECORD_NOT_EXISTS(1013, "Bản ghi không tồn tại", HttpStatus.NOT_FOUND),
    PAYMENT_NOT_READY(1014, "Thanh toán chưa sẵn sàng cho đơn hàng này", HttpStatus.BAD_REQUEST),
    PAYMENT_VALIDATION_FAILED(1015, "Thanh toán không hợp lệ", HttpStatus.BAD_REQUEST),
    REFUND_NOT_ALLOWED(1016, "Refund không được phép cho đơn hàng này", HttpStatus.BAD_REQUEST),
    PAYMENT_METHOD_NOT_SUPPORTED(1017, "Phương thức thanh toán được chọn không được hỗ trợ cho hành động này",
            HttpStatus.BAD_REQUEST),
    INVALID_RESET_TOKEN(1018, "Token reset password không hợp lệ hoặc đã hết hạn", HttpStatus.BAD_REQUEST),
    CURRENT_PASSWORD_INVALID(1019, "Mật khẩu hiện tại không chính xác", HttpStatus.BAD_REQUEST),
    PRODUCT_TECHNICAL_FIELDS_REQUIRED(1020, "Kích thước khung và kích thước bánh xe là bắt buộc",
            HttpStatus.BAD_REQUEST),
    PRODUCT_MINIMUM_IMAGES_REQUIRED(1021, "Cần ít nhất 3 ảnh sản phẩm", HttpStatus.BAD_REQUEST),
    PAYMENT_GATEWAY_ERROR(1022, "Yêu cầu cổng thanh toán thất bại", HttpStatus.BAD_GATEWAY),
    INVALID_REQUEST_BODY(1023, "Request body không hợp lệ", HttpStatus.BAD_REQUEST),
    EMAIL_NOT_VERIFIED(1024, "Vui lòng xác minh email trước khi đăng nhập", HttpStatus.FORBIDDEN),
    SELF_STATUS_CHANGE_NOT_ALLOWED(1025, "Admin không thể thay đổi trạng thái của chính mình", HttpStatus.BAD_REQUEST),
    REFERENCE_DATA_IN_USE(1026, "Dữ liệu tham chiếu đang được sử dụng và không thể xóa", HttpStatus.BAD_REQUEST),
    CATEGORY_HIERARCHY_INVALID(1027, "Hệ thống phân cấp danh mục không hợp lệ", HttpStatus.BAD_REQUEST),
    PAYOUT_NOT_READY(1028, "Payout chưa sẵn sàng", HttpStatus.BAD_REQUEST),
    PAYOUT_REFERENCE_REQUIRED(1029, "Bank ref là bắt buộc để hoàn tất thanh toán này", HttpStatus.BAD_REQUEST),
    INVALID_CREDENTIALS(1030, "Email hoặc mật khẩu không chính xác", HttpStatus.UNAUTHORIZED),
    ACCOUNT_INACTIVE(1031, "Tài khoản của bạn không hoạt động. Vui lòng liên hệ admin.", HttpStatus.FORBIDDEN),
    ACCOUNT_BANNED(1032, "Tài khoản của bạn đã bị cấm. Vui lòng liên hệ admin.", HttpStatus.FORBIDDEN),
    ORDER_EVIDENCE_REQUIRED(1033, "Cần ít nhất 1 ảnh bằng chứng đơn hàng", HttpStatus.BAD_REQUEST),
    ORDER_EVIDENCE_LIMIT_EXCEEDED(1034, "Bạn có thể tải lên tối đa 3 ảnh bằng chứng", HttpStatus.BAD_REQUEST),
    ORDER_EVIDENCE_IMAGE_ONLY(1035, "Chỉ chấp nhận file ảnh cho bằng chứng đơn hàng", HttpStatus.BAD_REQUEST),
    SIZE_CHART_ALREADY_EXISTS(1036, "Danh mục này đã có biểu đồ kích thước", HttpStatus.CONFLICT),
    SIZE_CHART_INVALID(1037, "Dữ liệu biểu đồ kích thước không hợp lệ", HttpStatus.BAD_REQUEST),
    PAYMENT_EXPIRED(1038, "Thời hạn thanh toán cho đơn hàng này đã hết hạn", HttpStatus.BAD_REQUEST),
    ASSISTANT_NOT_CONFIGURED(1039, "AI assistant không được cấu hình cho môi trường này",
            HttpStatus.SERVICE_UNAVAILABLE),
    ASSISTANT_REQUEST_FAILED(1040, "AI assistant yêu cầu thất bại", HttpStatus.BAD_GATEWAY),
    PAYOUT_PROFILE_REQUIRED(1041, "Cần có hồ sơ thanh toán trước khi thực hiện hành động này", HttpStatus.BAD_REQUEST),
    UPFRONT_AMOUNT_TOO_LOW(1042, "Số tiền trả trước quá thấp so với chính sách phí nền tảng hiện tại",
            HttpStatus.BAD_REQUEST),
    REFUND_EVIDENCE_LIMIT_EXCEEDED(1043, "Bạn có thể tải lên tối đa 3 ảnh bằng chứng hoàn tiền",
            HttpStatus.BAD_REQUEST),
    REFUND_EVIDENCE_IMAGE_ONLY(1044, "Chỉ chấp nhận file ảnh cho bằng chứng hoàn tiền", HttpStatus.BAD_REQUEST),
    REPORT_EVIDENCE_LIMIT_EXCEEDED(1045, "Bạn có thể tải lên tối đa 3 ảnh bằng chứng báo cáo", HttpStatus.BAD_REQUEST),
    REPORT_EVIDENCE_IMAGE_ONLY(1046, "Chỉ chấp nhận file ảnh cho bằng chứng báo cáo", HttpStatus.BAD_REQUEST),
    INVALID_REPORT_STATUS_TRANSITION(1047, "Chuyển đổi trạng thái báo cáo không hợp lệ", HttpStatus.BAD_REQUEST),
    INVALID_PAGINATION(1048, "Phân trang không hợp lệ. Page phải >= 0 và size trong khoảng 1-100",
            HttpStatus.BAD_REQUEST),
    INVALID_PRICE_RANGE(1049, "Khoảng giá không hợp lệ", HttpStatus.BAD_REQUEST),
    PRODUCT_IMAGE_INVALID(1050, "Chỉ chấp nhận file ảnh hợp lệ cho sản phẩm", HttpStatus.BAD_REQUEST),
    INSPECTION_REPORT_INVALID(1051, "Chỉ chấp nhận file PDF hợp lệ cho báo cáo kiểm định", HttpStatus.BAD_REQUEST);

    ErrorCode(int code, String message, HttpStatus statusCode) {
        this.code = code;
        this.message = message;
        this.statusCode = statusCode;
    }

    private final int code;
    private final String message;
    private final HttpStatus statusCode;
}
