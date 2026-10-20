package com.neueda.instrumentservice.dtos.ai;

import java.util.List;

public record AssistantAnswer(
        String reply,
        boolean aiSummary,
        boolean checksPassed,
        boolean revised,
        String model,
        List<String> problems,
        List<AssistantSource> sources,
        List<ToolResult> toolResults) {}
