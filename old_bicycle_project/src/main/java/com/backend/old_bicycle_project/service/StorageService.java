package com.backend.old_bicycle_project.service;

import com.backend.old_bicycle_project.exception.AppException;
import com.backend.old_bicycle_project.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Service upload file lên Supabase Storage qua REST API.
 * Supabase Storage endpoint: https://<project-id>.supabase.co/storage/v1/object/<bucket>/<path>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StorageService {

    private static final Pattern UNSAFE_FILENAME_CHARACTERS = Pattern.compile("[^A-Za-z0-9._-]");

    @Value("${supabase.url}")
    private String supabaseUrl;

    @Value("${supabase.anon-key}")
    private String supabaseAnonKey;

    @Value("${supabase.service-role-key:}")
    private String supabaseServiceRoleKey;

    @Value("${supabase.storage.bucket:product-images}")
    private String bucket;

    private final RestTemplate restTemplate;

    /**
     * Upload file lên bucket mặc định.
     */
    public String uploadFile(MultipartFile file, String folder) {
        return uploadFile(file, folder, bucket);
    }

    /**
     * Upload file lên bucket chỉ định và trả về public URL.
     */
    public String uploadFile(MultipartFile file, String folder, String bucketName) {
        if (file == null || file.isEmpty()) {
            throw new AppException(ErrorCode.INVALID_REQUEST_BODY);
        }

        String normalizedBucket = normalizeBucket(bucketName);
        String normalizedFolder = normalizeFolder(folder);
        String filename = UUID.randomUUID() + "_" + sanitizeFilename(file.getOriginalFilename());
        String path = normalizedFolder + "/" + filename;

        HttpHeaders headers = new HttpHeaders();
        applyStorageAuthorization(headers);
        headers.setContentType(resolveContentType(file.getContentType()));

        try {
            HttpEntity<byte[]> requestEntity = new HttpEntity<>(file.getBytes(), headers);
            ResponseEntity<String> response = restTemplate.exchange(
                    buildObjectUrl(normalizedBucket, path),
                    HttpMethod.POST,
                    requestEntity,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                return buildPublicUrl(normalizedBucket, path);
            }
            throw new RuntimeException("Upload file thất bại: " + response.getStatusCode());
        } catch (IOException exception) {
            throw new RuntimeException("Không thể đọc file: " + exception.getMessage(), exception);
        }
    }

    /**
     * Xóa file khỏi Supabase Storage dựa trên public URL.
     */
    public void deleteFile(String fileUrl) {
        StorageObjectLocation storageObjectLocation = resolveStorageObjectLocation(fileUrl);
        if (storageObjectLocation == null) {
            return;
        }

        HttpHeaders headers = new HttpHeaders();
        applyStorageAuthorization(headers);

        try {
            restTemplate.exchange(
                    buildObjectUrl(storageObjectLocation.bucket(), storageObjectLocation.path()),
                    HttpMethod.DELETE,
                    new HttpEntity<>(headers),
                    String.class
            );
            log.info("Đã xóa file: {}/{}", storageObjectLocation.bucket(), storageObjectLocation.path());
        } catch (Exception exception) {
            log.warn(
                    "Không thể xóa file {}/{}: {}",
                    storageObjectLocation.bucket(),
                    storageObjectLocation.path(),
                    exception.getMessage()
            );
        }
    }

    private void applyStorageAuthorization(HttpHeaders headers) {
        String apiKey = resolveStorageApiKey();
        headers.set("apikey", apiKey);

        if (isJwtStyleKey(apiKey)) {
            headers.setBearerAuth(apiKey);
        }
    }

    private String resolveStorageApiKey() {
        if (supabaseServiceRoleKey != null && !supabaseServiceRoleKey.isBlank()) {
            return supabaseServiceRoleKey;
        }
        return supabaseAnonKey;
    }

    private boolean isJwtStyleKey(String apiKey) {
        return apiKey != null && apiKey.chars().filter(ch -> ch == '.').count() == 2;
    }

    private String normalizeBucket(String bucketName) {
        if (bucketName == null || bucketName.isBlank()) {
            throw new AppException(ErrorCode.INVALID_REQUEST_BODY);
        }

        String normalizedBucket = bucketName.trim();
        if (normalizedBucket.contains("/") || normalizedBucket.contains("\\")) {
            throw new AppException(ErrorCode.INVALID_REQUEST_BODY);
        }

        return normalizedBucket;
    }

    private String normalizeFolder(String folder) {
        if (folder == null || folder.isBlank()) {
            throw new AppException(ErrorCode.INVALID_REQUEST_BODY);
        }

        String normalizedFolder = folder.trim()
                .replace('\\', '/')
                .replaceAll("/+", "/");

        if (normalizedFolder.startsWith("/")) {
            normalizedFolder = normalizedFolder.substring(1);
        }
        if (normalizedFolder.endsWith("/")) {
            normalizedFolder = normalizedFolder.substring(0, normalizedFolder.length() - 1);
        }
        if (normalizedFolder.isBlank() || normalizedFolder.contains("..")) {
            throw new AppException(ErrorCode.INVALID_REQUEST_BODY);
        }

        return normalizedFolder;
    }

    private String sanitizeFilename(String originalFilename) {
        String cleanedPath = StringUtils.cleanPath(originalFilename != null ? originalFilename : "");
        String filename = cleanedPath.replace('\\', '/');
        int lastSlashIndex = filename.lastIndexOf('/');
        if (lastSlashIndex >= 0) {
            filename = filename.substring(lastSlashIndex + 1);
        }

        String sanitizedFilename = UNSAFE_FILENAME_CHARACTERS
                .matcher(filename.trim().replace(' ', '_'))
                .replaceAll("_")
                .replaceAll("^\\.+", "");

        if (sanitizedFilename.isBlank()) {
            return "file";
        }

        return sanitizedFilename.length() <= 120
                ? sanitizedFilename
                : sanitizedFilename.substring(sanitizedFilename.length() - 120);
    }

    private MediaType resolveContentType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }

        try {
            return MediaType.parseMediaType(contentType);
        } catch (InvalidMediaTypeException exception) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }

    private String buildObjectUrl(String bucketName, String path) {
        return supabaseUrl + "/storage/v1/object/" + bucketName + "/" + path;
    }

    private String buildPublicUrl(String bucketName, String path) {
        return supabaseUrl + "/storage/v1/object/public/" + bucketName + "/" + path;
    }

    private StorageObjectLocation resolveStorageObjectLocation(String fileUrl) {
        String publicPrefix = supabaseUrl + "/storage/v1/object/public/";
        if (fileUrl == null || !fileUrl.startsWith(publicPrefix)) {
            return null;
        }

        String relativePath = fileUrl.substring(publicPrefix.length());
        int firstSlashIndex = relativePath.indexOf('/');
        if (firstSlashIndex <= 0 || firstSlashIndex == relativePath.length() - 1) {
            return null;
        }

        return new StorageObjectLocation(
                relativePath.substring(0, firstSlashIndex),
                relativePath.substring(firstSlashIndex + 1)
        );
    }

    private record StorageObjectLocation(String bucket, String path) {
    }
}
