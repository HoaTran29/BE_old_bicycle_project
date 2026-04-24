package com.backend.old_bicycle_project.validation;

import com.backend.old_bicycle_project.exception.AppException;
import com.backend.old_bicycle_project.exception.ErrorCode;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

public final class MultipartFileValidationUtils {

    private static final byte[] PDF_SIGNATURE = "%PDF-".getBytes(StandardCharsets.US_ASCII);

    private MultipartFileValidationUtils() {
    }

    public static List<MultipartFile> normalizeFiles(List<MultipartFile> files) {
        if (files == null || files.isEmpty()) {
            return List.of();
        }

        return files.stream()
                .filter(file -> file != null && !file.isEmpty())
                .toList();
    }

    public static void validateRequiredImages(
            List<MultipartFile> files,
            int minimumFiles,
            ErrorCode minimumFilesError,
            ErrorCode invalidImageError) {
        if (files.size() < minimumFiles) {
            throw new AppException(minimumFilesError);
        }

        validateImageFiles(files, invalidImageError);
    }

    public static void validateImageFiles(
            List<MultipartFile> files,
            int maxFiles,
            ErrorCode limitExceededError,
            ErrorCode invalidImageError) {
        if (files.size() > maxFiles) {
            throw new AppException(limitExceededError);
        }

        validateImageFiles(files, invalidImageError);
    }

    public static void validatePdfReport(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new AppException(ErrorCode.INVALID_REQUEST_BODY);
        }

        if (!isPdfFile(file)) {
            throw new AppException(ErrorCode.INSPECTION_REPORT_INVALID);
        }
    }

    private static void validateImageFiles(List<MultipartFile> files, ErrorCode invalidImageError) {
        for (MultipartFile file : files) {
            if (!isImageFile(file)) {
                throw new AppException(invalidImageError);
            }
        }
    }

    private static boolean isImageFile(MultipartFile file) {
        String contentType = normalizeContentType(file.getContentType());
        if (contentType == null || !contentType.startsWith("image/")) {
            return false;
        }

        try (InputStream inputStream = file.getInputStream()) {
            return ImageIO.read(inputStream) != null;
        } catch (IOException exception) {
            return false;
        }
    }

    private static boolean isPdfFile(MultipartFile file) {
        String contentType = normalizeContentType(file.getContentType());
        boolean pdfContentType = "application/pdf".equals(contentType);
        boolean pdfExtension = hasPdfExtension(file.getOriginalFilename());

        if (!pdfContentType && !pdfExtension) {
            return false;
        }

        try (InputStream inputStream = file.getInputStream()) {
            byte[] header = inputStream.readNBytes(PDF_SIGNATURE.length);
            return Arrays.equals(PDF_SIGNATURE, header);
        } catch (IOException exception) {
            return false;
        }
    }

    private static boolean hasPdfExtension(String originalFilename) {
        if (originalFilename == null) {
            return false;
        }

        return originalFilename.trim().toLowerCase().endsWith(".pdf");
    }

    private static String normalizeContentType(String contentType) {
        if (contentType == null) {
            return null;
        }

        String normalizedContentType = contentType.trim().toLowerCase();
        return normalizedContentType.isEmpty() ? null : normalizedContentType;
    }
}
