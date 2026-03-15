package me.asu.ta.ai.client;

import me.asu.ta.ai.model.LlmRequest;
import me.asu.ta.ai.model.LlmResponse;

public interface LlmClient {
    LlmResponse generate(LlmRequest request);
}
