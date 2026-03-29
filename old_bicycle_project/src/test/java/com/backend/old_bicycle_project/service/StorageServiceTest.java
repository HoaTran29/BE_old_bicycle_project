package com.backend.old_bicycle_project.service;

import com.backend.old_bicycle_project.exception.AppException;
import com.backend.old_bicycle_project.exception.ErrorCode;
import com.backend.old_bicycle_project.support.TestMultipartFiles;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StorageServiceTest {

    @Mock
    private org.springframework.web.client.RestTemplate restTemplate;

    private StorageService storageService;

    @BeforeEach
    void setUp() {
        storageService = new StorageService(restTemplate);
        ReflectionTestUtils.setField(storageService, "supabaseUrl", "https://example.supabase.co");
        ReflectionTestUtils.setField(storageService, "supabaseAnonKey", "anon-key");
        ReflectionTestUtils.setField(storageService, "bucket", "product-images");
    }

    @Test
    void uploadFileSanitizesFolderAndFilenameBeforeCallingStorage() {
        when(restTemplate.exchange(any(String.class), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok("ok"));

        String publicUrl = storageService.uploadFile(
                TestMultipartFiles.image("images", "../bike shot?.png"),
                "/products//abc/"
        );

        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);
        verify(restTemplate).exchange(urlCaptor.capture(), eq(HttpMethod.POST), any(HttpEntity.class), eq(String.class));
        assertThat(urlCaptor.getValue()).contains("/storage/v1/object/product-images/products/abc/");
        assertThat(urlCaptor.getValue()).doesNotContain("..");
        assertThat(urlCaptor.getValue()).doesNotContain(" ");
        assertThat(publicUrl).contains("/storage/v1/object/public/product-images/products/abc/");
    }

    @Test
    void uploadFileRejectsTraversalFolder() {
        assertThatThrownBy(() -> storageService.uploadFile(
                TestMultipartFiles.image("images", "bike.png"),
                "../escape"
        ))
                .isInstanceOf(AppException.class)
                .extracting(ex -> ((AppException) ex).getErrorCode())
                .isEqualTo(ErrorCode.INVALID_REQUEST_BODY);
    }
}
