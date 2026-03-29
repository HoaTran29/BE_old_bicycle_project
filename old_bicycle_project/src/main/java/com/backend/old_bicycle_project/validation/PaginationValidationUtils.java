package com.backend.old_bicycle_project.validation;

import com.backend.old_bicycle_project.exception.AppException;
import com.backend.old_bicycle_project.exception.ErrorCode;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

public final class PaginationValidationUtils {

    public static final int MAX_PAGE_SIZE = 100;

    private PaginationValidationUtils() {
    }

    public static PageRequest createPageRequest(int page, int size) {
        validate(page, size);
        return PageRequest.of(page, size);
    }

    public static PageRequest createPageRequest(int page, int size, Sort sort) {
        validate(page, size);
        return PageRequest.of(page, size, sort);
    }

    public static void validate(int page, int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new AppException(ErrorCode.INVALID_PAGINATION);
        }
    }
}
