package org.hameed.hameedmoneycli.proxy;

import lombok.RequiredArgsConstructor;
import org.hameed.hameedmoneycli.config.HmcConfig.LlmConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class LlmProxy {

    private static final Logger log = LoggerFactory.getLogger(LlmProxy.class);

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    public String call(String prompt, LlmConfig llm) {
        if (llm.baseUrl() == null || llm.baseUrl().isBlank()) {
            throw new IllegalArgumentException("llm.baseUrl is required");
        }

        Object requestBody = buildRequestBody(prompt, llm);

        String response = restClient.post()
                .uri(llm.baseUrl())
                .headers(h -> {
                    if (llm.apiKey() != null && !llm.apiKey().isBlank()) {
                        applyAuth(h, llm);
                    }
                    h.setContentType(MediaType.APPLICATION_JSON);
                })
                .body(requestBody)
                .retrieve()
                .body(String.class);

        return extractContent(response, llm.provider());
    }

    private Object buildRequestBody(String prompt, LlmConfig llm) {
        return switch (llm.provider()) {
            case "ollama" -> Map.of(
                    "model", llm.model() != null ? llm.model() : "llama3",
                    "messages", List.of(Map.of("role", "user", "content", prompt)),
                    "stream", false
            );
            case "openai" -> Map.of(
                    "model", llm.model() != null ? llm.model() : "gpt-4o-mini",
                    "messages", List.of(Map.of("role", "user", "content", prompt))
            );
            case "claude" -> Map.of(
                    "model", llm.model() != null ? llm.model() : "claude-3-haiku-20240307",
                    "max_tokens", 4096,
                    "messages", List.of(Map.of("role", "user", "content", prompt))
            );
            case "gemini" -> Map.of(
                    "model", llm.model() != null ? llm.model() : "gemini-2.0-flash",
                    "input", prompt
            );
            default -> throw new IllegalArgumentException("Unsupported LLM provider: " + llm.provider());
        };
    }

    private void applyAuth(HttpHeaders headers, LlmConfig llm) {
        switch (llm.provider()) {
            case "openai" -> headers.setBearerAuth(llm.apiKey());
            case "claude" -> headers.set("x-api-key", llm.apiKey());
            case "gemini" -> headers.set("x-goog-api-key", llm.apiKey());
        }
    }

    private String extractContent(String responseBody, String provider) {
        if (responseBody == null) return null;
        try {
            var root = objectMapper.readTree(responseBody);
            return switch (provider) {
                case "ollama" -> root.path("message").path("content").asText();
                case "openai" -> root.path("choices").get(0).path("message").path("content").asText();
                case "claude" -> root.path("content").get(0).path("text").asText();
                case "gemini" -> extractGeminiContent(root);
                default -> null;
            };
        } catch (Exception e) {
            log.warn("Failed to extract LLM response content: {}", e.getMessage());
            return responseBody;
        }
    }

    private String extractGeminiContent(JsonNode root) {
        var steps = root.path("steps");
        if (!steps.isArray()) return null;
        for (var step : steps) {
            if (!"model_output".equals(step.path("type").asText())) continue;
            var content = step.path("content");
            if (content.isArray() && content.size() > 0) {
                return content.get(0).path("text").asText();
            }
        }
        return null;
    }
}
