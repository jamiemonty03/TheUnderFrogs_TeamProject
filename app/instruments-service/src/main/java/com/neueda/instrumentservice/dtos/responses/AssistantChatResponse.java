package com.neueda.instrumentservice.dtos.responses;

import java.util.List;
import com.neueda.instrumentservice.dtos.ai.AssistantAnswer;
import com.neueda.instrumentservice.dtos.ai.AssistantSource;

public record AssistantChatResponse(
        String reply,
        boolean aiSummary,
        boolean checksPassed,
        String model,
        List<AssistantSource> sources,
        String disclaimer) {

    public static final String DISCLAIMER = "Information only, not financial advice. "
            + "Figures come from the Alpaca and Yahoo Finance data held by The UnderFrogs.";

    public static AssistantChatResponse from(AssistantAnswer answer) {
        return new AssistantChatResponse(answer.reply(), answer.aiSummary(), answer.checksPassed(), answer.model(),
                answer.sources(), DISCLAIMER);
    }
}
