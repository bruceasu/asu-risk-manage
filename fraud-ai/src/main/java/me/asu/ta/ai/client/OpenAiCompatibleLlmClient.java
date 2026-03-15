package me.asu.ta.ai.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import me.asu.ta.ai.model.LlmRequest;
import me.asu.ta.ai.model.LlmResponse;
import org.springframework.stereotype.Component;

@Component
public class OpenAiCompatibleLlmClient implements LlmClient {
    private final HttpClient httpClient;
    private final LlmClientProperties properties;
    private final ObjectMapper objectMapper;

    public OpenAiCompatibleLlmClient(LlmClientProperties properties, ObjectMapper objectMapper) {
        this(HttpClient.newBuilder()
                .connectTimeout(properties.getTimeout())
                .build(), properties, objectMapper);
    }

    OpenAiCompatibleLlmClient(HttpClient httpClient, LlmClientProperties properties, ObjectMapper objectMapper) {
        this.httpClient = httpClient;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @Override
    public LlmResponse generate(LlmRequest request) {
        RuntimeException lastError = null;
        for (int attempt = 0; attempt <= properties.getMaxRetries(); attempt++) {
            try {
                HttpRequest httpRequest = HttpRequest.newBuilder()
                        .uri(URI.create(properties.getBaseUrl() + "/v1/chat/completions"))
                        .header("Authorization", "Bearer " + properties.getApiKey())
                        .header("Content-Type", "application/json")
                        .timeout(properties.getTimeout())
                        .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(buildRequestBody(request))))
                        .build();
                HttpResponse<String> httpResponse = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());
                if (httpResponse.statusCode() >= 400) {
                    throw new LlmClientException("LLM API returned status " + httpResponse.statusCode());
                }
                String rawResponse = httpResponse.body();
                if (rawResponse == null || rawResponse.isBlank()) {
                    throw new LlmClientException("Empty LLM response");
                }
                return new LlmResponse(request.modelName(), rawResponse, extractContent(rawResponse));
            } catch (IOException | InterruptedException ex) {
                if (ex instanceof InterruptedException) {
                    Thread.currentThread().interrupt();
                }
                lastError = new LlmClientException("Failed to call LLM API on attempt " + (attempt + 1), ex);
            }
        }
        throw lastError == null ? new LlmClientException("Unknown LLM API failure") : lastError;
    }

    Map<String, Object> buildRequestBody(LlmRequest request) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("model", request.modelName());
        payload.put("temperature", request.temperature());
        payload.put("messages", List.of(
                Map.of("role", "system", "content", request.systemPrompt()),
                Map.of("role", "user", "content", request.userPrompt())));
        payload.put("response_format", Map.of("type", "json_object"));
        return payload;
    }

    private String extractContent(String rawResponse) throws JsonProcessingException {
        JsonNode root = objectMapper.readTree(rawResponse);
        JsonNode contentNode = root.path("choices").path(0).path("message").path("content");
        if (contentNode.isMissingNode() || contentNode.isNull()) {
            throw new LlmClientException("LLM response missing message content");
        }
        return contentNode.asText();
    }
}
