package com.backend.old_bicycle_project.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class AppConfig {

    /**
     * RestTemplate dùng cho StorageService (Supabase) và VNPayService
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
