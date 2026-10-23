package com.neueda.instrumentservice.clients;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.times;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.jsonPath;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withUnauthorizedRequest;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.client.MockServerRestTemplateCustomizer;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import com.neueda.instrumentservice.dtos.ai.ChatMessage;
import com.neueda.instrumentservice.dtos.ai.ModelReply;
import com.neueda.instrumentservice.dtos.ai.ToolDefinition;
import com.neueda.instrumentservice.exceptions.AssistantUnavailableException;

class AssistantModelClientTest {

    private static final String BASE_URL = "https://ai.example.com/openai/v1";
    private static final String CHAT_URL = BASE_URL + "/chat/completions";
    private static final List<ChatMessage> MESSAGES = List.of(
            ChatMessage.system("Only use tool data."), ChatMessage.user("Why is Microsoft down today?"));
    private static final ToolDefinition PRICE_TOOL = ToolDefinition.function("get_price_change",
            "Latest price change for one symbol",
            Map.of("type", "object", "properties", Map.of("symbol", Map.of("type", "string")),
                    "required", List.of("symbol")));

    private static final String TOOL_CALL_REPLY = """
            {"choices":[{"message":{"role":"assistant","content":null,"reasoning":"ignored",
            "tool_calls":[{"id":"call_1","type":"function",
            "function":{"name":"get_price_change","arguments":"{\\"symbol\\":\\"MSFT\\"}"}}]}}]}
            """;
    private static final String TEXT_REPLY = """
            {"choices":[{"message":{"role":"assistant","content":"MSFT is down 2.06%."}}]}
            """;

    private final MockServerRestTemplateCustomizer customizer = new MockServerRestTemplateCustomizer();

    private AssistantModelClient client(String apiKey, String fallbackModel) {
        return new AssistantModelClient(new RestTemplateBuilder(customizer), BASE_URL + "/", apiKey,
                "openai/gpt-oss-120b", fallbackModel, 0);
    }

    private MockRestServiceServer server() {
        return customizer.getServer();
    }

    @Test
    void sendsTheConversationAndToolsAndReadsBackAToolCall() {
        AssistantModelClient client = client("gsk_test", "openai/gpt-oss-20b");
        server().expect(requestTo(CHAT_URL))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer gsk_test"))
                .andExpect(header("User-Agent", AssistantModelClient.USER_AGENT))
                .andExpect(jsonPath("$.model").value("openai/gpt-oss-120b"))
                .andExpect(jsonPath("$.messages[1].content").value("Why is Microsoft down today?"))
                .andExpect(jsonPath("$.messages[0].tool_calls").doesNotExist())
                .andExpect(jsonPath("$.tools[0].function.name").value("get_price_change"))
                .andRespond(withSuccess(TOOL_CALL_REPLY, MediaType.APPLICATION_JSON));

        ModelReply reply = client.chat(MESSAGES, List.of(PRICE_TOOL));

        assertThat(reply.model()).isEqualTo("openai/gpt-oss-120b");
        assertThat(reply.message().hasToolCalls()).isTrue();
        assertThat(reply.message().toolCalls().get(0).id()).isEqualTo("call_1");
        assertThat(reply.message().toolCalls().get(0).function().arguments()).isEqualTo("{\"symbol\":\"MSFT\"}");
        server().verify();
    }

    @Test
    void leavesToolsOutOfTheRequestWhenThereAreNone() {
        AssistantModelClient client = client("gsk_test", "");
        server().expect(requestTo(CHAT_URL))
                .andExpect(content().string(Matchers.not(Matchers.containsString("\"tools\""))))
                .andRespond(withSuccess(TEXT_REPLY, MediaType.APPLICATION_JSON));

        assertThat(client.chat(MESSAGES, List.of()).message().content()).isEqualTo("MSFT is down 2.06%.");
    }

    @Test
    void retriesTheSameModelAfterARateLimit() {
        AssistantModelClient client = client("gsk_test", "openai/gpt-oss-20b");
        server().expect(requestTo(CHAT_URL))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));
        server().expect(requestTo(CHAT_URL))
                .andExpect(jsonPath("$.model").value("openai/gpt-oss-120b"))
                .andRespond(withSuccess(TEXT_REPLY, MediaType.APPLICATION_JSON));

        assertThat(client.chat(MESSAGES, List.of()).model()).isEqualTo("openai/gpt-oss-120b");
        server().verify();
    }

    @Test
    void switchesToTheFallbackModelWhenTheMainModelKeepsFailing() {
        AssistantModelClient client = client("gsk_test", "openai/gpt-oss-20b");
        server().expect(times(AssistantModelClient.ATTEMPTS_PER_MODEL), requestTo(CHAT_URL))
                .andExpect(jsonPath("$.model").value("openai/gpt-oss-120b"))
                .andRespond(withServerError());
        server().expect(requestTo(CHAT_URL))
                .andExpect(jsonPath("$.model").value("openai/gpt-oss-20b"))
                .andRespond(withSuccess(TEXT_REPLY, MediaType.APPLICATION_JSON));

        assertThat(client.chat(MESSAGES, List.of()).model()).isEqualTo("openai/gpt-oss-20b");
        server().verify();
    }

    @Test
    void retriesWhenTheProviderCannotBeReached() {
        AssistantModelClient client = client("gsk_test", "");
        server().expect(requestTo(CHAT_URL))
                .andRespond(request -> { throw new IOException("connection reset"); });
        server().expect(requestTo(CHAT_URL))
                .andRespond(withSuccess(TEXT_REPLY, MediaType.APPLICATION_JSON));

        assertThat(client.chat(MESSAGES, List.of()).message().content()).isEqualTo("MSFT is down 2.06%.");
    }

    @Test
    void reportsUnavailableWhenEveryModelFails() {
        AssistantModelClient client = client("gsk_test", "openai/gpt-oss-20b");
        server().expect(times(2 * AssistantModelClient.ATTEMPTS_PER_MODEL), requestTo(CHAT_URL))
                .andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        assertThatThrownBy(() -> client.chat(MESSAGES, List.of()))
                .isInstanceOf(AssistantUnavailableException.class);
        server().verify();
    }

    @Test
    void doesNotRetryOrFallBackWhenTheKeyIsRejected() {
        AssistantModelClient client = client("gsk_wrong", "openai/gpt-oss-20b");
        server().expect(requestTo(CHAT_URL)).andRespond(withUnauthorizedRequest());

        assertThatThrownBy(() -> client.chat(MESSAGES, List.of()))
                .isInstanceOf(AssistantUnavailableException.class);
        server().verify();
    }

    @Test
    void fallsBackWhenTheReplyHasNoMessage() {
        AssistantModelClient client = client("gsk_test", "openai/gpt-oss-20b");
        server().expect(requestTo(CHAT_URL))
                .andRespond(withSuccess("{\"choices\":[]}", MediaType.APPLICATION_JSON));
        server().expect(requestTo(CHAT_URL))
                .andExpect(jsonPath("$.model").value("openai/gpt-oss-20b"))
                .andRespond(withSuccess(TEXT_REPLY, MediaType.APPLICATION_JSON));

        assertThat(client.chat(MESSAGES, List.of()).model()).isEqualTo("openai/gpt-oss-20b");
    }

    @Test
    void refusesToCallTheProviderWithoutAKey() {
        AssistantModelClient client = client("", "openai/gpt-oss-20b");

        assertThat(client.isConfigured()).isFalse();
        assertThatThrownBy(() -> client.chat(MESSAGES, List.of()))
                .isInstanceOf(AssistantUnavailableException.class)
                .hasMessageContaining("not configured");
        server().verify();
    }
}
