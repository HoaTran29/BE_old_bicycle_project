package com.backend.old_bicycle_project.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "assistant.gateway")
public class AssistantGatewayProperties {
    private boolean enabled = false;
    private String apiBaseUrl = "https://ai-gateway.vercel.sh/v1/chat/completions";
    private String apiKey;
    private String model = "google/gemini-2.5-flash-lite";
    private double temperature = 0.2;
    private int maxTokens = 600;
}
