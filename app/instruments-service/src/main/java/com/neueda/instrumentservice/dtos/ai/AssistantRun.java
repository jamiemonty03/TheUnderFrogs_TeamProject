package com.neueda.instrumentservice.dtos.ai;

import java.util.List;

public record AssistantRun(String reply, String model, List<ToolResult> toolResults) {

    public boolean hasReply() {
        return reply != null && !reply.isBlank();
    }
}
