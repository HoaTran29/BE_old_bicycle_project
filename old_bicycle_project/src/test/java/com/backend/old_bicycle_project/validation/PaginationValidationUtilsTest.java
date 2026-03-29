package com.backend.old_bicycle_project.validation;

import com.backend.old_bicycle_project.exception.AppException;
import com.backend.old_bicycle_project.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaginationValidationUtilsTest {

    @Test
    void createPageRequestRejectsInvalidSize() {
        assertThatThrownBy(() -> PaginationValidationUtils.createPageRequest(0, 101))
                .isInstanceOf(AppException.class)
                .extracting(ex -> ((AppException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_PAGINATION);
    }

    @Test
    void createPageRequestRejectsNegativePage() {
        assertThatThrownBy(() -> PaginationValidationUtils.createPageRequest(-1, 10))
                .isInstanceOf(AppException.class)
                .extracting(ex -> ((AppException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_PAGINATION);
    }

    @Test
    void createPageRequestBuildsExpectedSortWhenInputIsValid() {
        var pageRequest = PaginationValidationUtils.createPageRequest(1, 20, Sort.by("createdAt").descending());

        assertThat(pageRequest.getPageNumber()).isEqualTo(1);
        assertThat(pageRequest.getPageSize()).isEqualTo(20);
        assertThat(pageRequest.getSort().getOrderFor("createdAt")).isNotNull();
        assertThat(pageRequest.getSort().getOrderFor("createdAt").isDescending()).isTrue();
    }
}
