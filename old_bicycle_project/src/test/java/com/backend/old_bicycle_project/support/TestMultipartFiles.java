package com.backend.old_bicycle_project.support;

import org.springframework.mock.web.MockMultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public final class TestMultipartFiles {

    private static final byte[] PNG_BYTES = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR42mP8/x8AAwMCAO7+XGkAAAAASUVORK5CYII="
    );
    private static final byte[] PDF_BYTES = "%PDF-1.4\n1 0 obj\n<<>>\nendobj\ntrailer\n<<>>\n%%EOF"
            .getBytes(StandardCharsets.US_ASCII);

    private TestMultipartFiles() {
    }

    public static MockMultipartFile image(String partName, String filename) {
        return new MockMultipartFile(partName, filename, "image/png", PNG_BYTES);
    }

    public static MockMultipartFile pdf(String partName, String filename) {
        return new MockMultipartFile(partName, filename, "application/pdf", PDF_BYTES);
    }

    public static MockMultipartFile text(String partName, String filename) {
        return new MockMultipartFile(partName, filename, "text/plain", "not-a-valid-upload".getBytes(StandardCharsets.UTF_8));
    }
}
