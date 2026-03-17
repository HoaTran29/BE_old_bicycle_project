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
    INVALID_REQUEST_BODY(1023, "Request body khong hop le", HttpStatus.BAD_REQUEST);

    ErrorCode(int code, String message, HttpStatus statusCode) {
        this.code = code;
        this.message = message;
        this.statusCode = statusCode;
    }

    private final int code;
    private final String message;
    private final HttpStatus statusCode;
}
