package com.backend.old_bicycle_project.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    UNCATEGORIZED_EXCEPTION(9999, "Uncategorized error", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_KEY(1001, "Invalid key", HttpStatus.BAD_REQUEST),
    USER_EXISTED(1002, "User existed", HttpStatus.BAD_REQUEST),
    USERNAME_INVALID(1003, "Username must be at least 3 characters", HttpStatus.BAD_REQUEST),
    INVALID_PASSWORD(1004, "Password must be at least 8 characters and include an uppercase letter and a number", HttpStatus.BAD_REQUEST),
    USER_NOT_EXISTED(1005, "User not existed", HttpStatus.NOT_FOUND),
    UNAUTHENTICATED(1006, "Unauthenticated", HttpStatus.UNAUTHORIZED),
    UNAUTHORIZED(1007, "You do not have permission", HttpStatus.FORBIDDEN),
    RESOURCE_NOT_FOUND(1008, "Resource not found", HttpStatus.NOT_FOUND),
    PRODUCT_NOT_FOUND(1009, "Product not found", HttpStatus.NOT_FOUND),
    FORBIDDEN(1010, "Forbidden action", HttpStatus.FORBIDDEN),
    INVALID_STATUS(1011, "Invalid product status for this operation", HttpStatus.BAD_REQUEST),
    RECORD_ALREADY_EXISTS(1012, "Record already exists", HttpStatus.CONFLICT),
    RECORD_NOT_EXISTS(1013, "Record does not exist", HttpStatus.NOT_FOUND),
    PAYMENT_NOT_READY(1014, "Payment is not ready for this order", HttpStatus.BAD_REQUEST),
    PAYMENT_VALIDATION_FAILED(1015, "Payment validation failed", HttpStatus.BAD_REQUEST),
    REFUND_NOT_ALLOWED(1016, "Refund is not allowed for this order", HttpStatus.BAD_REQUEST),
    PAYMENT_METHOD_NOT_SUPPORTED(1017, "Selected payment method is not supported for this action", HttpStatus.BAD_REQUEST),
    INVALID_RESET_TOKEN(1018, "Password reset token is invalid or expired", HttpStatus.BAD_REQUEST),
    CURRENT_PASSWORD_INVALID(1019, "Current password is incorrect", HttpStatus.BAD_REQUEST),
    PRODUCT_TECHNICAL_FIELDS_REQUIRED(1020, "Frame size and wheel size are required", HttpStatus.BAD_REQUEST),
    PRODUCT_MINIMUM_IMAGES_REQUIRED(1021, "At least 3 product images are required", HttpStatus.BAD_REQUEST),
    PAYMENT_GATEWAY_ERROR(1022, "Payment gateway request failed", HttpStatus.BAD_GATEWAY),
    INVALID_REQUEST_BODY(1023, "Request body khong hop le", HttpStatus.BAD_REQUEST),
    EMAIL_NOT_VERIFIED(1024, "Please verify your email before logging in", HttpStatus.FORBIDDEN),
    SELF_STATUS_CHANGE_NOT_ALLOWED(1025, "Admin cannot change their own status", HttpStatus.BAD_REQUEST),
    REFERENCE_DATA_IN_USE(1026, "Reference data is being used and cannot be deleted", HttpStatus.BAD_REQUEST),
    CATEGORY_HIERARCHY_INVALID(1027, "Invalid category hierarchy", HttpStatus.BAD_REQUEST),
    PAYOUT_NOT_READY(1028, "Payout is not ready for this action", HttpStatus.BAD_REQUEST),
    PAYOUT_REFERENCE_REQUIRED(1029, "Bank reference is required to complete this payout", HttpStatus.BAD_REQUEST),
    INVALID_CREDENTIALS(1030, "Email or password is incorrect", HttpStatus.UNAUTHORIZED),
    ACCOUNT_INACTIVE(1031, "Your account is inactive. Please contact admin.", HttpStatus.FORBIDDEN),
    ACCOUNT_BANNED(1032, "Your account has been banned. Please contact admin.", HttpStatus.FORBIDDEN),
    ORDER_EVIDENCE_REQUIRED(1033, "At least 1 order evidence image is required", HttpStatus.BAD_REQUEST),
    ORDER_EVIDENCE_LIMIT_EXCEEDED(1034, "You can upload up to 3 evidence images", HttpStatus.BAD_REQUEST),
    ORDER_EVIDENCE_IMAGE_ONLY(1035, "Only image files are allowed for order evidence", HttpStatus.BAD_REQUEST);

    ErrorCode(int code, String message, HttpStatus statusCode) {
        this.code = code;
        this.message = message;
        this.statusCode = statusCode;
    }

    private final int code;
    private final String message;
    private final HttpStatus statusCode;
}
