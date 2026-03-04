package com.backend.old_bicycle_project.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.UUID;

/**
 * Service upload ảnh lên Supabase Storage qua REST API.
 * Supabase Storage endpoint: https://<project-id>.supabase.co/storage/v1/object/<bucket>/<path>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StorageService {

    @Value("${supabase.url}")
    private String supabaseUrl;

    @Value("${supabase.anon-key}")
    private String supabaseAnonKey;

    @Value("${supabase.storage.bucket:product-images}")
    private String bucket;

    private final RestTemplate restTemplate;

    /**
     * Upload file lên Supabase Storage, trả về public URL.
     *
     * @param file      file ảnh từ multipart request
     * @param folder    folder trong bucket, ví dụ "products/{productId}"
     * @return public URL của file đã upload
     */
    public String uploadFile(MultipartFile file, String folder) {
        String filename = UUID.randomUUID() + "_" + file.getOriginalFilename();
        String path = folder + "/" + filename;
        String uploadUrl = supabaseUrl + "/storage/v1/object/" + bucket + "/" + path;

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + supabaseAnonKey);
        headers.setContentType(MediaType.parseMediaType(
                file.getContentType() != null ? file.getContentType() : "application/octet-stream"
        ));

        try {
            HttpEntity<byte[]> requestEntity = new HttpEntity<>(file.getBytes(), headers);
            ResponseEntity<String> response = restTemplate.exchange(
                    uploadUrl, HttpMethod.POST, requestEntity, String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                // Trả về public URL
                return supabaseUrl + "/storage/v1/object/public/" + bucket + "/" + path;
            } else {
                throw new RuntimeException("Upload ảnh thất bại: " + response.getStatusCode());
            }
        } catch (IOException e) {
            throw new RuntimeException("Không thể đọc file: " + e.getMessage(), e);
        }
    }

    /**
     * Xóa file khỏi Supabase Storage.
     *
     * @param fileUrl public URL của file cần xóa
     */
    public void deleteFile(String fileUrl) {
        // Lấy path từ URL
        String prefix = supabaseUrl + "/storage/v1/object/public/" + bucket + "/";
        if (!fileUrl.startsWith(prefix)) return;

        String path = fileUrl.substring(prefix.length());
        String deleteUrl = supabaseUrl + "/storage/v1/object/" + bucket + "/" + path;

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + supabaseAnonKey);

        try {
            restTemplate.exchange(deleteUrl, HttpMethod.DELETE, new HttpEntity<>(headers), String.class);
            log.info("Đã xóa file: {}", path);
        } catch (Exception e) {
            log.warn("Không thể xóa file {}: {}", path, e.getMessage());
        }
    }
}
