package com.neueda.instrumentservice.dtos.responses;

import java.time.Instant;
import java.util.List;

public record NewsResponse(
        String headline,
        String articleUrl,
        Instant publishedAt,
        Instant updatedAt,
        List<String> symbols,
        String summary,
        String source) {}
