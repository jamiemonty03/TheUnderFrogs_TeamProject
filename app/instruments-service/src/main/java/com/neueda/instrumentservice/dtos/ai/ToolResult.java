package com.neueda.instrumentservice.dtos.ai;

import java.util.List;

public record ToolResult(String toolName, String content, List<AssistantSource> sources, boolean failed) {}
