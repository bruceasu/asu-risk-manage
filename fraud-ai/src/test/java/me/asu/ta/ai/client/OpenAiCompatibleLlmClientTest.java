package me.asu.ta.ai.client;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.http.HttpClient;
import java.util.List;
import java.util.Map;
import me.asu.ta.ai.AiTestSupport;
import me.asu.ta.ai.model.LlmRequest;
import org.junit.jupiter.api.Test;

class OpenAiCompatibleLlmClientTest {
    @Test
    void shouldBuildOpenAiCompatibleRequestBody() {
        OpenAiCompatibleLlmClient client = new OpenAiCompatibleLlmClient(
                HttpClient.newHttpClient(),
                AiTestSupport.llmProperties(),
                new ObjectMapper());

        Map<String, Object> payload = client.buildRequestBody(new LlmRequest(
                "test-model",
                "system prompt",
                "user prompt",
                0.1d));

        assertEquals("test-model", payload.get("model"));
        assertEquals(0.1d, payload.get("temperature"));
        assertEquals(Map.of("type", "json_object"), payload.get("response_format"));
        @SuppressWarnings("unchecked")
        List<Map<String, String>> messages = (List<Map<String, String>>) payload.get("messages");
        assertEquals("system", messages.getFirst().get("role"));
        assertEquals("user", messages.get(1).get("role"));
    }
}
