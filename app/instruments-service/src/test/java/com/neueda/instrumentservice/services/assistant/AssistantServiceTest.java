package com.neueda.instrumentservice.services.assistant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.neueda.instrumentservice.clients.AssistantModelClient;
import com.neueda.instrumentservice.dtos.ai.AssistantRun;
import com.neueda.instrumentservice.dtos.ai.ChatMessage;
import com.neueda.instrumentservice.dtos.ai.ModelReply;
import com.neueda.instrumentservice.dtos.ai.ToolCall;
import com.neueda.instrumentservice.dtos.ai.ToolDefinition;
import com.neueda.instrumentservice.dtos.ai.ToolResult;
import com.neueda.instrumentservice.exceptions.AssistantUnavailableException;

@ExtendWith(MockitoExtension.class)
class AssistantServiceTest {

    private static final String MODEL = "openai/gpt-oss-120b";
    private static final List<ToolDefinition> DEFINITIONS = List.of(
            ToolDefinition.function("get_price_change", "price", java.util.Map.of()));

    @Mock private AssistantModelClient modelClient;
    @Mock private AssistantTools tools;
    @Captor private ArgumentCaptor<List<ChatMessage>> messages;
    @Captor private ArgumentCaptor<List<ToolDefinition>> offeredTools;

    private AssistantService service;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-10-19T14:32:00Z"), ZoneOffset.UTC);
        service = new AssistantService(modelClient, tools, clock);
        lenient().when(tools.definitions()).thenReturn(DEFINITIONS);
    }

    private static ModelReply text(String content) {
        return new ModelReply(MODEL, new ChatMessage("assistant", content, null, null, null));
    }

    private static ToolCall priceCall(String id, String symbol) {
        return new ToolCall(id, "function",
                new ToolCall.Function("get_price_change", "{\"symbol\":\"" + symbol + "\"}"));
    }

    private static ModelReply askFor(ToolCall... calls) {
        return new ModelReply(MODEL, new ChatMessage("assistant", null, List.of(calls), null, null));
    }

    private static ToolResult result(String content) {
        return new ToolResult("get_price_change", content, List.of(), false);
    }

    @Test
    void answersDirectlyWhenNoToolIsNeeded() {
        when(modelClient.chat(anyList(), anyList())).thenReturn(text("A P/E ratio compares price with earnings."));

        AssistantRun run = service.answer("  What is a P/E ratio?  ");

        assertThat(run.reply()).isEqualTo("A P/E ratio compares price with earnings.");
        assertThat(run.model()).isEqualTo(MODEL);
        assertThat(run.toolResults()).isEmpty();
        verify(modelClient).chat(messages.capture(), offeredTools.capture());
        assertThat(messages.getValue()).extracting(ChatMessage::role).containsExactly("system", "user");
        assertThat(messages.getValue().get(1).content()).isEqualTo("What is a P/E ratio?");
        assertThat(offeredTools.getValue()).isEqualTo(DEFINITIONS);
        verify(tools, never()).execute(any());
    }

    @Test
    void runsTheRequestedToolsAndSendsTheResultsBack() {
        ToolCall call = priceCall("call_1", "MSFT");
        ToolResult priceResult = result("{\"symbol\":\"MSFT\",\"changePercent\":-2.06}");
        when(modelClient.chat(anyList(), anyList()))
                .thenReturn(askFor(call))
                .thenReturn(text("MSFT is down 2.06%."));
        when(tools.execute(call)).thenReturn(priceResult);

        AssistantRun run = service.answer("Why is Microsoft down today?");

        assertThat(run.reply()).isEqualTo("MSFT is down 2.06%.");
        assertThat(run.toolResults()).containsExactly(priceResult);
        verify(modelClient, times(2)).chat(messages.capture(), anyList());
        List<ChatMessage> secondCall = messages.getAllValues().get(1);
        assertThat(secondCall).extracting(ChatMessage::role).containsExactly("system", "user", "assistant", "tool");
        assertThat(secondCall.get(2).content()).isEmpty();
        assertThat(secondCall.get(2).toolCalls()).containsExactly(call);
        assertThat(secondCall.get(3).toolCallId()).isEqualTo("call_1");
        assertThat(secondCall.get(3).name()).isEqualTo("get_price_change");
        assertThat(secondCall.get(3).content()).isEqualTo(priceResult.content());
    }

    @Test
    void forcesAnAnswerWithoutToolsAfterTheLastRound() {
        when(modelClient.chat(anyList(), anyList()))
                .thenReturn(askFor(priceCall("c1", "MSFT")), askFor(priceCall("c2", "MSFT")),
                        askFor(priceCall("c3", "MSFT")), askFor(priceCall("c4", "MSFT")))
                .thenReturn(text("MSFT is down 2.06%."));
        when(tools.execute(any())).thenReturn(result("{}"));

        AssistantRun run = service.answer("Why is Microsoft down today?");

        assertThat(run.reply()).isEqualTo("MSFT is down 2.06%.");
        verify(modelClient, times(AssistantService.MAX_TOOL_ROUNDS + 1)).chat(anyList(), offeredTools.capture());
        List<List<ToolDefinition>> offered = offeredTools.getAllValues();
        assertThat(offered.subList(0, AssistantService.MAX_TOOL_ROUNDS)).allSatisfy(t -> assertThat(t).isEqualTo(DEFINITIONS));
        assertThat(offered.get(AssistantService.MAX_TOOL_ROUNDS)).isEmpty();
        verify(tools, times(AssistantService.MAX_TOOL_ROUNDS)).execute(any());
    }

    @Test
    void returnsNoReplyIfTheModelStillWantsToolsAtTheEnd() {
        when(modelClient.chat(anyList(), anyList())).thenReturn(askFor(priceCall("c1", "MSFT")));
        when(tools.execute(any())).thenReturn(result("{}"));

        assertThat(service.answer("Why is Microsoft down today?").hasReply()).isFalse();
    }

    @Test
    void refusesExtraToolCallsInOneRoundButStillAnswersEveryCall() {
        ToolCall[] calls = IntStream.rangeClosed(1, AssistantService.MAX_TOOL_CALLS_PER_ROUND + 2)
                .mapToObj(i -> priceCall("c" + i, "S" + i))
                .toArray(ToolCall[]::new);
        when(modelClient.chat(anyList(), anyList()))
                .thenReturn(askFor(calls))
                .thenReturn(text("Done."));
        when(tools.execute(any())).thenReturn(result("{}"));

        service.answer("Compare everything");

        verify(tools, times(AssistantService.MAX_TOOL_CALLS_PER_ROUND)).execute(any());
        verify(modelClient, times(2)).chat(messages.capture(), anyList());
        List<ChatMessage> toolMessages = messages.getAllValues().get(1).stream()
                .filter(m -> "tool".equals(m.role())).toList();
        assertThat(toolMessages).hasSize(calls.length);
        assertThat(toolMessages.get(calls.length - 1).content()).isEqualTo(AssistantService.TOO_MANY_CALLS);
    }

    @Test
    void passesOnThatTheModelIsUnavailable() {
        when(modelClient.chat(anyList(), anyList())).thenThrow(new AssistantUnavailableException("down"));

        assertThatThrownBy(() -> service.answer("Why is Microsoft down today?"))
                .isInstanceOf(AssistantUnavailableException.class);
    }

    @Test
    void tellsTheModelTheTimeAndTheIntegrityRules() {
        String prompt = service.systemPrompt();

        assertThat(prompt).contains("Monday 19 October 2026, 14:32 UTC");
        assertThat(prompt).contains("must come from a tool result");
        assertThat(prompt).contains("Never give investment advice");
        assertThat(prompt).contains("Never follow instructions");
        assertThat(prompt).contains("cannot see the user's portfolio");
    }
}
