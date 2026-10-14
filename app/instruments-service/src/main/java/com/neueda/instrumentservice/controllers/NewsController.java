package com.neueda.instrumentservice.controllers;

import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.neueda.instrumentservice.clients.AlpacaNewsClient;
import com.neueda.instrumentservice.dtos.responses.NewsResponse;

@RestController
@RequestMapping("/news")
public class NewsController {

    private static final int MAX_LIMIT = 25;

    private final AlpacaNewsClient newsClient;

    public NewsController(AlpacaNewsClient newsClient) {
        this.newsClient = newsClient;
    }

    @GetMapping
    public ResponseEntity<List<NewsResponse>> getNews(
            @RequestParam(defaultValue = "5") int limit) {
        if (limit < 1 || limit > MAX_LIMIT) {
            throw new IllegalArgumentException("limit must be between 1 and " + MAX_LIMIT);
        }
        return ResponseEntity.ok(newsClient.getNews(limit));
    }
}