package com.neueda.instrumentservice.services.assistant;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.neueda.instrumentservice.clients.AlpacaNewsClient;
import com.neueda.instrumentservice.dtos.responses.NewsResponse;

@Component
public class NewsCache {

    private record Key(String symbol, int limit) {}

    private record Entry(List<NewsResponse> news, Instant fetchedAt) {}

    private final AlpacaNewsClient newsClient;
    private final Duration timeToLive;
    private final Clock clock;
    private final Map<Key, Entry> entries = new ConcurrentHashMap<>();

    @Autowired
    public NewsCache(AlpacaNewsClient newsClient, @Value("${ai.news-cache-seconds:300}") long timeToLiveSeconds) {
        this(newsClient, Duration.ofSeconds(timeToLiveSeconds), Clock.systemUTC());
    }

    NewsCache(AlpacaNewsClient newsClient, Duration timeToLive, Clock clock) {
        this.newsClient = newsClient;
        this.timeToLive = timeToLive;
        this.clock = clock;
    }

    public List<NewsResponse> getNews(String symbol, int limit) {
        Key key = new Key(symbol, limit);
        Instant now = clock.instant();
        Entry cached = entries.get(key);
        if (cached != null && cached.fetchedAt().plus(timeToLive).isAfter(now)) {
            return cached.news();
        }
        List<NewsResponse> news = newsClient.getNews(limit, List.of(symbol));
        entries.put(key, new Entry(news, now));
        return news;
    }
}
