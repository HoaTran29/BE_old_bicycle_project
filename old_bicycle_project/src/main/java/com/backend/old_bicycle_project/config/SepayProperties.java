package com.backend.old_bicycle_project.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "payment.sepay")
public class SepayProperties {
    private boolean mockMode = true;
    private String apiToken;
    private String webhookApiKey;
    private String bankBin;
    private String accountNumber;
    private String accountName;
}
