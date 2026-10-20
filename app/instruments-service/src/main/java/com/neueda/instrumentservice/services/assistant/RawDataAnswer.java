package com.neueda.instrumentservice.services.assistant;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.neueda.instrumentservice.dtos.ai.ToolResult;

final class RawDataAnswer {

    static final String UNAVAILABLE = "The AI assistant is unavailable right now. Please try again in a minute.";
    static final String INTRO = "I couldn't produce a checked AI summary just now, so here is the data I found:";

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private RawDataAnswer() {}

    static String from(List<ToolResult> results) {
        List<String> lines = new ArrayList<>();
        for (ToolResult result : results) {
            if (result.failed() || AssistantTools.RESOLVE_TICKER.equals(result.toolName())) {
                continue;
            }
            JsonNode data;
            try {
                data = MAPPER.readTree(result.content());
            } catch (JsonProcessingException ex) {
                continue;
            }
            String symbol = data.path("symbol").asText("");
            switch (result.toolName()) {
                case AssistantTools.GET_PRICE_CHANGE -> lines.add("- %s: %s, %s%% against the previous close of %s (%s, as of %s)"
                        .formatted(symbol, text(data, "price"), text(data, "changePercent"),
                                text(data, "previousClose"),
                                "LIVE".equals(data.path("priceSource").asText()) ? "live price" : "daily close",
                                text(data, "asOf")));
                case AssistantTools.GET_NEWS -> data.path("articles").forEach(article -> lines.add(
                        "- %s news: \"%s\" (%s, %s)".formatted(symbol, text(article, "headline"),
                                text(article, "publisher"), text(article, "publishedAt"))));
                default -> lines.add("- %s %s: %s".formatted(symbol,
                        AssistantTools.GET_TREND.equals(result.toolName()) ? "trend" : "fundamentals",
                        keyValues(data)));
            }
        }
        return lines.isEmpty() ? UNAVAILABLE : INTRO + "\n" + String.join("\n", lines);
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isMissingNode() || value.isNull() ? "n/a" : value.asText();
    }

    private static String keyValues(JsonNode data) {
        List<String> parts = new ArrayList<>();
        Iterator<Map.Entry<String, JsonNode>> fields = data.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            if (!"symbol".equals(field.getKey()) && field.getValue().isValueNode() && !field.getValue().isNull()) {
                parts.add(field.getKey() + " " + field.getValue().asText());
            }
        }
        return String.join(", ", parts);
    }
}
