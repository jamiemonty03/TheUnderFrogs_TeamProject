package com.neueda.instrumentservice.clients;

import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.web.util.HtmlUtils;
import com.neueda.instrumentservice.dtos.responses.NewsResponse;
import com.neueda.instrumentservice.exceptions.AlpacaNewsException;

@Component
public class AlpacaNewsClient {

    static final String DEFAULT_NEWS_URL = "https://data.alpaca.markets/v1beta1/news";
    static final int MAX_LIMIT = 25;

    private final RestTemplate restTemplate;
    private final String newsUrl;
    private final boolean configured;

    public AlpacaNewsClient(RestTemplateBuilder builder,
            @Value("${alpaca.key-id}") String keyId,
            @Value("${alpaca.secret-key}") String secretKey,
            @Value("${alpaca.news-url:" + DEFAULT_NEWS_URL + "}") String newsUrl) {
        this.restTemplate = builder
                .setConnectTimeout(Duration.ofSeconds(3))
                .setReadTimeout(Duration.ofSeconds(5))
                .defaultHeader("APCA-API-KEY-ID", keyId)
                .defaultHeader("APCA-API-SECRET-KEY", secretKey)
                .build();
        this.newsUrl = newsUrl;
        this.configured = !keyId.isBlank() && !secretKey.isBlank();
    }

    public List<NewsResponse> getNews(int limit) {
        return getNews(limit, List.of());
    }

    public List<NewsResponse> getNews(int limit, List<String> symbols) {
        if (!configured) {
            throw unavailable("NEWS-503", "News provider is not configured");
        }

        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString(newsUrl)
                .queryParam("limit", limit)
                .queryParam("sort", "desc");
        if (!symbols.isEmpty()) {
            uriBuilder.queryParam("symbols", symbols.stream().collect(Collectors.joining(",")));
        }
        URI uri = uriBuilder.build().toUri();
        try {
            AlpacaNewsResponse response = restTemplate.getForObject(uri, AlpacaNewsResponse.class);
            if (response == null || response.news() == null) {
                return List.of();
            }
            return response.news().stream().map(AlpacaNewsClient::toNewsResponse).toList();
        } catch (RestClientResponseException ex) {
            if (ex.getStatusCode().value() == 429) {
                throw new AlpacaNewsException(HttpStatus.TOO_MANY_REQUESTS, "NEWS-429",
                        "News provider rate limit reached");
            }
            throw unavailable("NEWS-503", "News provider is temporarily unavailable");
        } catch (RestClientException ex) {
            throw unavailable("NEWS-503", "News provider is temporarily unavailable");
        }
    }

    private static NewsResponse toNewsResponse(AlpacaNewsArticle article) {
        return new NewsResponse(HtmlUtils.htmlUnescape(article.headline()), article.url(), article.publishedAt(),
                article.updatedAt(), article.symbols() == null ? List.of() : article.symbols());
    }

    private static AlpacaNewsException unavailable(String code, String message) {
        return new AlpacaNewsException(HttpStatus.SERVICE_UNAVAILABLE, code, message);
    }

    record AlpacaNewsResponse(List<AlpacaNewsArticle> news) {}

    record AlpacaNewsArticle(
            String headline,
            String url,
            @JsonProperty("created_at") Instant publishedAt,
            @JsonProperty("updated_at") Instant updatedAt,
            List<String> symbols) {}
}