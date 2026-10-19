package com.neueda.instrumentservice.services.assistant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import com.neueda.instrumentservice.clients.AlpacaNewsClient;
import com.neueda.instrumentservice.dtos.responses.NewsResponse;
import com.neueda.instrumentservice.exceptions.AlpacaNewsException;

class NewsCacheTest {

    private static final Instant START = Instant.parse("2026-10-19T14:00:00Z");
    private static final List<NewsResponse> NEWS = List.of(new NewsResponse("Azure growth slows",
            "https://news.example.com/azure", START, null, List.of("MSFT"), null, null));

    private final AlpacaNewsClient newsClient = mock(AlpacaNewsClient.class);
    private final Clock clock = mock(Clock.class);
    private final NewsCache cache = new NewsCache(newsClient, Duration.ofMinutes(5), clock);

    @Test
    void reusesNewsFetchedInTheLastFiveMinutes() {
        when(clock.instant()).thenReturn(START, START.plusSeconds(299));
        when(newsClient.getNews(5, List.of("MSFT"))).thenReturn(NEWS);

        assertThat(cache.getNews("MSFT", 5)).isEqualTo(NEWS);
        assertThat(cache.getNews("MSFT", 5)).isEqualTo(NEWS);

        verify(newsClient, times(1)).getNews(5, List.of("MSFT"));
    }

    @Test
    void fetchesAgainOnceTheNewsIsFiveMinutesOld() {
        when(clock.instant()).thenReturn(START, START.plusSeconds(300));
        when(newsClient.getNews(5, List.of("MSFT"))).thenReturn(NEWS);

        cache.getNews("MSFT", 5);
        cache.getNews("MSFT", 5);

        verify(newsClient, times(2)).getNews(5, List.of("MSFT"));
    }

    @Test
    void keepsSymbolsAndLimitsApart() {
        when(clock.instant()).thenReturn(START);
        when(newsClient.getNews(5, List.of("MSFT"))).thenReturn(NEWS);
        when(newsClient.getNews(10, List.of("MSFT"))).thenReturn(List.of());
        when(newsClient.getNews(5, List.of("AAPL"))).thenReturn(List.of());

        assertThat(cache.getNews("MSFT", 5)).isEqualTo(NEWS);
        assertThat(cache.getNews("MSFT", 10)).isEmpty();
        assertThat(cache.getNews("AAPL", 5)).isEmpty();
    }

    @Test
    void doesNotCacheAFailure() {
        when(clock.instant()).thenReturn(START);
        when(newsClient.getNews(5, List.of("MSFT")))
                .thenThrow(new AlpacaNewsException(HttpStatus.SERVICE_UNAVAILABLE, "NEWS-503", "down"))
                .thenReturn(NEWS);

        assertThatThrownBy(() -> cache.getNews("MSFT", 5)).isInstanceOf(AlpacaNewsException.class);
        assertThat(cache.getNews("MSFT", 5)).isEqualTo(NEWS);
    }
}
