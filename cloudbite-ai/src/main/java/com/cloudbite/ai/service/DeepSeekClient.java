package com.cloudbite.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Component
public class DeepSeekClient {
    private static final Logger log = LoggerFactory.getLogger(DeepSeekClient.class);

    private final String apiKey;
    private final String model;
    private final RestClient client;
    private final ObjectMapper mapper = new ObjectMapper();

    public DeepSeekClient(@Value("${cloudbite.ai.api-key:}") String apiKey,
                          @Value("${cloudbite.ai.base-url:https://api.deepseek.com}") String baseUrl,
                          @Value("${cloudbite.ai.model:deepseek-chat}") String model) {
        this.apiKey = apiKey;
        this.model = model;
        this.client = RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", "application/json")
                .build();
    }

    public boolean enabled() {
        return apiKey != null && !apiKey.isBlank();
    }

    public String chat(String system, String user) {
        if (!enabled()) {
            return null;
        }
        try {
            Map<String, Object> body = Map.of(
                    "model", model,
                    "messages", List.of(
                            Map.of("role", "system", "content", system),
                            Map.of("role", "user", "content", user)
                    ),
                    "temperature", 0.7,
                    "max_tokens", 800,
                    "stream", false
            );
            String raw = client.post()
                    .uri("/chat/completions")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(String.class);
            JsonNode node = mapper.readTree(raw);
            return node.path("choices").path(0).path("message").path("content").asText(null);
        } catch (Exception e) {
            log.warn("DeepSeek 调用失败: {}", e.getMessage());
            return null;
        }
    }
}