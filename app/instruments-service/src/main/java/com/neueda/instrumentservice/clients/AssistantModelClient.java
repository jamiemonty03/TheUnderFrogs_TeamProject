package com.neueda.instrumentservice.clients;

import java.time.Duration;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.neueda.instrumentservice.dtos.ai.ChatMessage;
import com.neueda.instrumentservice.dtos.ai.ModelReply;
import com.neueda.instrumentservice.dtos.ai.ToolDefinition;
import com.neueda.instrumentservice.exceptions.AssistantUnavailableException;

@Component
public class AssistantModelClient {

    static final String DEFAULT_BASE_URL = "https://api.groq.com/openai/v1";
    static final String USER_AGENT = "underfrogs-instruments-service/1.0";
    static final int ATTEMPTS_PER_MODEL = 3;
    static final double TEMPERATURE = 0.2;

    private static final Logger log = LoggerFactory.getLogger(AssistantModelClient.class);

    private final RestTemplate restTemplate;
    private final String chatUrl;
    private final List<String> models;
    private final long retryBackoffMs;
    private final boolean configured;

    public AssistantModelClient(RestTemplateBuilder builder,
            @Value("${ai.base-url:" + DEFAULT_BASE_URL + "}") String baseUrl,
            @Value("${ai.api-key:}") String apiKey,
            @Value("${ai.model:openai/gpt-oss-120b}") String model,
            @Value("${ai.fallback-model:}") String fallbackModel,
            @Value("${ai.retry-backoff-ms:2000}") long retryBackoffMs) {
        this.restTemplate = builder
                .setConnectTimeout(Duration.ofSeconds(5))
                .setReadTimeout(Duration.ofSeconds(45))
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .defaultHeader(HttpHeaders.USER_AGENT, USER_AGENT)
                .build();
        this.chatUrl = baseUrl.replaceAll("/+$", "") + "/chat/completions";
        this.models = fallbackModel.isBlank() || fallbackModel.equals(model)
                ? List.of(model)
                : List.of(model, fallbackModel);
        this.retryBackoffMs = retryBackoffMs;
        this.configured = !apiKey.isBlank();
    }

    public boolean isConfigured() {
        return configured;
    }

    public ModelReply chat(List<ChatMessage> messages, List<ToolDefinition> tools) {
        if (!configured) {
            throw new AssistantUnavailableException("The AI assistant is not configured");
        }
        for (String model : models) {
            ChatRequest request = new ChatRequest(model, messages, tools.isEmpty() ? null : tools, TEMPERATURE);
            for (int attempt = 1; attempt <= ATTEMPTS_PER_MODEL; attempt++) {
                try {
                    ChatResponse response = restTemplate.postForObject(chatUrl, request, ChatResponse.class);
                    if (response == null || response.choices() == null || response.choices().isEmpty()
                            || response.choices().get(0).message() == null) {
                        log.warn("AI model {} returned no message", model);
                        break;
                    }
                    return new ModelReply(model, response.choices().get(0).message());
                } catch (RestClientResponseException ex) {
                    int status = ex.getStatusCode().value();
                    if (!isRetryable(status)) {
                        log.error("AI provider rejected the request with HTTP {}", status);
                        throw new AssistantUnavailableException("The AI assistant is temporarily unavailable");
                    }
                    log.warn("AI model {} returned HTTP {} on attempt {}", model, status, attempt);
                } catch (RestClientException ex) {
                    log.warn("AI model {} could not be reached on attempt {}: {}", model, attempt,
                            ex.getClass().getSimpleName());
                }
                if (attempt < ATTEMPTS_PER_MODEL) {
                    pause(attempt);
                }
            }
            log.warn("Giving up on AI model {}", model);
        }
        throw new AssistantUnavailableException("The AI assistant is temporarily unavailable");
    }

    private static boolean isRetryable(int status) {
        return status == 429 || status >= 500;
    }

    private void pause(int attempt) {
        try {
            Thread.sleep(retryBackoffMs * (1L << (attempt - 1)));
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new AssistantUnavailableException("The AI assistant request was interrupted");
        }
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    record ChatRequest(String model, List<ChatMessage> messages, List<ToolDefinition> tools, double temperature) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ChatResponse(List<Choice> choices) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Choice(ChatMessage message) {}
}
