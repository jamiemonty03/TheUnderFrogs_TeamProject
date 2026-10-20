package com.neueda.instrumentservice.controllers;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.neueda.instrumentservice.dtos.ai.AssistantAnswer;
import com.neueda.instrumentservice.dtos.requests.AssistantChatRequest;
import com.neueda.instrumentservice.dtos.responses.AssistantChatResponse;
import com.neueda.instrumentservice.services.assistant.AssistantRateLimiter;
import com.neueda.instrumentservice.services.assistant.AssistantService;

@RestController
@RequestMapping("/assistant")
public class AssistantController {

    private final AssistantService assistantService;
    private final AssistantRateLimiter rateLimiter;

    public AssistantController(AssistantService assistantService, AssistantRateLimiter rateLimiter) {
        this.assistantService = assistantService;
        this.rateLimiter = rateLimiter;
    }

    @PostMapping("/chat")
    public ResponseEntity<AssistantChatResponse> chat(@Valid @RequestBody AssistantChatRequest request,
            Authentication authentication) {
        rateLimiter.check(authentication.getName());
        AssistantAnswer answer = assistantService.respond(request.message());
        return ResponseEntity.ok(AssistantChatResponse.from(answer));
    }
}
