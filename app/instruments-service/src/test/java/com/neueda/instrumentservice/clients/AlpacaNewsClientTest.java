package com.neueda.instrumentservice.clients;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.client.MockServerRestTemplateCustomizer;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import com.neueda.instrumentservice.dtos.responses.NewsResponse;

class AlpacaNewsClientTest {

    @Test
    void mapsAlpacaNewsAndKeepsCredentialsUpstream() {
        MockServerRestTemplateCustomizer customizer = new MockServerRestTemplateCustomizer();
        AlpacaNewsClient client = new AlpacaNewsClient(new RestTemplateBuilder(customizer),
                "PKTESTKEY", "test-secret", AlpacaNewsClient.DEFAULT_NEWS_URL);
        MockRestServiceServer alpaca = customizer.getServer();

        alpaca.expect(requestTo(AlpacaNewsClient.DEFAULT_NEWS_URL + "?limit=5&sort=desc"))
                .andExpect(header("APCA-API-KEY-ID", "PKTESTKEY"))
                .andExpect(header("APCA-API-SECRET-KEY", "test-secret"))
                .andRespond(withSuccess("""
                        {"news":[{"headline":"Markets rally","url":"https://example.com/article",
                        "created_at":"2026-10-14T08:00:00Z","updated_at":"2026-10-14T08:05:00Z",
                        "symbols":["AAPL"]}]}
                        """, MediaType.APPLICATION_JSON));

        List<NewsResponse> news = client.getNews(5);

        assertEquals(1, news.size());
        assertEquals("Markets rally", news.get(0).headline());
        assertEquals("https://example.com/article", news.get(0).articleUrl());
        assertEquals(Instant.parse("2026-10-14T08:00:00Z"), news.get(0).publishedAt());
        assertEquals(List.of("AAPL"), news.get(0).symbols());
        alpaca.verify();
    }

    @Test
    void decodesHtmlInHeadlines() {
        MockServerRestTemplateCustomizer customizer = new MockServerRestTemplateCustomizer();
        AlpacaNewsClient client = new AlpacaNewsClient(new RestTemplateBuilder(customizer),
                "PKTESTKEY", "test-secret", AlpacaNewsClient.DEFAULT_NEWS_URL);
        MockRestServiceServer alpaca = customizer.getServer();

        alpaca.expect(requestTo(AlpacaNewsClient.DEFAULT_NEWS_URL + "?limit=5&sort=desc"))
                .andRespond(withSuccess("""
                        {"news":[{"headline":"Storage &amp; Peripherals: what&#39;s next","url":"https://example.com/a",
                        "created_at":"2026-10-14T08:00:00Z","updated_at":"2026-10-14T08:05:00Z","symbols":[]}]}
                        """, MediaType.APPLICATION_JSON));

        assertEquals("Storage & Peripherals: what's next", client.getNews(5).get(0).headline());
    }

    @Test
    void filtersNewsByTrackedSymbols() {
        MockServerRestTemplateCustomizer customizer = new MockServerRestTemplateCustomizer();
        AlpacaNewsClient client = new AlpacaNewsClient(new RestTemplateBuilder(customizer),
                "PKTESTKEY", "test-secret", AlpacaNewsClient.DEFAULT_NEWS_URL);
        MockRestServiceServer alpaca = customizer.getServer();

        alpaca.expect(requestTo(AlpacaNewsClient.DEFAULT_NEWS_URL
                + "?limit=5&sort=desc&symbols=AAPL,SPY"))
                .andRespond(withSuccess("{\"news\":[]}", MediaType.APPLICATION_JSON));

        assertEquals(List.of(), client.getNews(5, List.of("AAPL", "SPY")));
        alpaca.verify();
    }
}