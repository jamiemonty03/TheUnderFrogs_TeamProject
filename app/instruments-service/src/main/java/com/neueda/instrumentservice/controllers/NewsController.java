package com.neueda.instrumentservice.controllers;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.neueda.instrumentservice.clients.AlpacaNewsClient;
import com.neueda.instrumentservice.dtos.responses.NewsResponse;
import com.neueda.instrumentservice.mappers.TrackedTickerMapper;

@RestController
@RequestMapping("/news")
public class NewsController {

    private static final int MAX_LIMIT = 25;

    private final AlpacaNewsClient newsClient;
    private final TrackedTickerMapper trackedTickerMapper;

    public NewsController(AlpacaNewsClient newsClient, TrackedTickerMapper trackedTickerMapper) {
        this.newsClient = newsClient;
        this.trackedTickerMapper = trackedTickerMapper;
    }

    @GetMapping
    public ResponseEntity<List<NewsResponse>> getNews(
            @RequestParam(defaultValue = "5") int limit,
            @RequestParam(required = false) String symbol) {
        if (limit < 1 || limit > MAX_LIMIT) {
            throw new IllegalArgumentException("limit must be between 1 and " + MAX_LIMIT);
        }
        List<String> symbols = trackedTickerMapper.findActiveSymbols();
        if (symbol != null && !symbol.isBlank()) {
            String wanted = symbol.trim().toUpperCase();
            symbols = symbols.contains(wanted) ? List.of(wanted) : List.of();
        }
        if (symbols.isEmpty()) {
            return ResponseEntity.ok(List.of());
        }
        return ResponseEntity.ok(newsClient.getNews(limit, symbols));
    }
}