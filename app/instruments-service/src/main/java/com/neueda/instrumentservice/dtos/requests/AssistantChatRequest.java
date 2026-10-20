package com.neueda.instrumentservice.dtos.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AssistantChatRequest(
        @NotBlank(message = "message must not be blank")
        @Size(max = AssistantChatRequest.MAX_LENGTH, message = "message must be at most 500 characters")
        String message) {

    public static final int MAX_LENGTH = 500;
}
