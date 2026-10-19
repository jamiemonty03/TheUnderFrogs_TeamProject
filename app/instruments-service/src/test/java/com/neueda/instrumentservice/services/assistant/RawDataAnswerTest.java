package com.neueda.instrumentservice.services.assistant;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;
import com.neueda.instrumentservice.dtos.ai.ToolResult;

class RawDataAnswerTest {

    @Test
    void listsThePlainFactsTheToolsFound() {
        List<ToolResult> results = List.of(
                new ToolResult("resolve_ticker", "{\"status\":\"MATCH\"}", List.of(), false),
                new ToolResult("get_price_change", """
                        {"symbol":"MSFT","price":412.37,"previousClose":421.05,"changePercent":-2.06,
                        "priceSource":"DAILY_CLOSE","asOf":"2026-10-16T20:00:00Z"}""", List.of(), false),
                new ToolResult("get_news", """
                        {"symbol":"MSFT","articles":[{"headline":"Azure growth slows","publisher":"benzinga",
                        "publishedAt":"2026-10-19T13:05:00Z"}]}""", List.of(), false),
                new ToolResult("get_trend", "{\"symbol\":\"MSFT\",\"change5DayPercent\":-0.63,\"movingAvg50\":null}",
                        List.of(), false),
                new ToolResult("get_fundamentals", "{\"error\":\"TOOL_FAILED\"}", List.of(), true));

        assertThat(RawDataAnswer.from(results)).isEqualTo("""
                I couldn't produce a checked AI summary just now, so here is the data I found:
                - MSFT: 412.37, -2.06% against the previous close of 421.05 (daily close, as of 2026-10-16T20:00:00Z)
                - MSFT news: "Azure growth slows" (benzinga, 2026-10-19T13:05:00Z)
                - MSFT trend: change5DayPercent -0.63""");
    }

    @Test
    void saysTheAssistantIsUnavailableWhenThereIsNoData() {
        assertThat(RawDataAnswer.from(List.of())).isEqualTo(RawDataAnswer.UNAVAILABLE);
    }
}
