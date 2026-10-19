package com.neueda.instrumentservice.dtos.ai;

import java.util.List;

public record AssistantRun(String reply, String model, List<ToolResult> toolResults, List<ChatMessage> conversation) {

    public boolean hasReply() {
        return reply != null && !reply.isBlank();
    }
}
