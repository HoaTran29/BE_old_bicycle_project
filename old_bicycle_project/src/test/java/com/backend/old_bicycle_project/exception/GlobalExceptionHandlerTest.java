package com.backend.old_bicycle_project.exception;

import com.backend.old_bicycle_project.dto.auth.RegisterRequest;
import com.backend.old_bicycle_project.dto.response.ApiResponse;
import jakarta.validation.Valid;
import org.junit.jupiter.api.Test;
import org.springframework.core.MethodParameter;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;

import java.lang.reflect.Method;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handlingValidationReturnsRawConstraintMessageWhenMessageIsNotErrorCodeEnum() throws Exception {
        MethodArgumentNotValidException exception = methodArgumentNotValidException(
                "email",
                "bad@@mail.com",
                "Email khong hop le"
        );

        ResponseEntity<ApiResponse<?>> response = handler.handlingValidation(exception);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(ErrorCode.INVALID_KEY.getCode());
        assertThat(response.getBody().getMessage()).isEqualTo("Email khong hop le");
    }

    @Test
    void handlingValidationResolvesKnownErrorCodeMessageWhenConstraintMessageMatchesEnumName() throws Exception {
        MethodArgumentNotValidException exception = methodArgumentNotValidException(
                "password",
                "123456",
                "INVALID_PASSWORD"
        );

        ResponseEntity<ApiResponse<?>> response = handler.handlingValidation(exception);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(ErrorCode.INVALID_PASSWORD.getCode());
        assertThat(response.getBody().getMessage()).isEqualTo(ErrorCode.INVALID_PASSWORD.getMessage());
    }

    @Test
    void handlingMalformedRequestReturnsReadableMessage() {
        HttpMessageNotReadableException exception = new HttpMessageNotReadableException("Malformed JSON");

        ResponseEntity<ApiResponse<?>> response = handler.handlingMalformedRequest(exception);

        assertThat(response.getStatusCode().value()).isEqualTo(400);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getCode()).isEqualTo(ErrorCode.INVALID_REQUEST_BODY.getCode());
        assertThat(response.getBody().getMessage()).isEqualTo(ErrorCode.INVALID_REQUEST_BODY.getMessage());
    }

    private MethodArgumentNotValidException methodArgumentNotValidException(
            String field,
            Object rejectedValue,
            String defaultMessage
    ) throws Exception {
        Method method = ValidationStubController.class.getDeclaredMethod("register", RegisterRequest.class);
        MethodParameter parameter = new MethodParameter(method, 0);
        BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new RegisterRequest(), "registerRequest");
        bindingResult.addError(new FieldError("registerRequest", field, rejectedValue, false, null, null, defaultMessage));
        return new MethodArgumentNotValidException(parameter, bindingResult);
    }

    @SuppressWarnings("unused")
    private static class ValidationStubController {
        public void register(@Valid RegisterRequest request) {
        }
    }
}
