package com.backend.old_bicycle_project.dto.product;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ProductResponseJsonTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void serializesFrontendCompatibleBooleanKeys() throws Exception {
        ProductResponse response = ProductResponse.builder()
                .isVerified(true)
                .images(java.util.List.of(ProductResponse.ImageInfo.builder()
                        .id(java.util.UUID.randomUUID())
                        .url("https://cdn.example.com/bike.jpg")
                        .isPrimary(true)
                        .displayOrder(0)
                        .build()))
                .lockedForTransaction(false)
                .sellerActionLocked(true)
                .build();

        String json = objectMapper.writeValueAsString(response);

        assertThat(json).contains("\"isVerified\":true");
        assertThat(json).contains("\"isPrimary\":true");
        assertThat(json).contains("\"sellerActionLocked\":true");
        assertThat(json).doesNotContain("\"verified\":true");
        assertThat(json).doesNotContain("\"primary\":true");
    }
}
