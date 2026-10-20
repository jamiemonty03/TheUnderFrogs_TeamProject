package com.neueda.instrumentservice.dtos.ai;

import java.util.Map;

public record ToolDefinition(String type, Function function) {

    public record Function(String name, String description, Map<String, Object> parameters) {}

    public static ToolDefinition function(String name, String description, Map<String, Object> parameters) {
        return new ToolDefinition("function", new Function(name, description, parameters));
    }
}
