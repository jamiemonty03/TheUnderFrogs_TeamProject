package com.neueda.tradeexecutor.clients;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.headerDoesNotExist;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.web.client.MockServerRestTemplateCustomizer;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import com.neueda.tradeexecutor.dtos.AlpacaQuote;

class AlpacaMarketDataClientTest {

    @Test
    void fetchesAllSymbolsInOneRequestWithOnlyAlpacaHeaders() {
        MockServerRestTemplateCustomizer customizer = new MockServerRestTemplateCustomizer();
        AlpacaMarketDataClient client = new AlpacaMarketDataClient(
                new RestTemplateBuilder(customizer), "PKTESTKEY", "test-secret");
        MockRestServiceServer alpaca = customizer.getServer();

        alpaca.expect(requestTo(AlpacaMarketDataClient.DATA_URL + "/stocks/quotes/latest?symbols=AAPL,MSFT&feed=iex"))
                .andExpect(header("APCA-API-KEY-ID", "PKTESTKEY"))
                .andExpect(header("APCA-API-SECRET-KEY", "test-secret"))
                .andExpect(headerDoesNotExist(HttpHeaders.AUTHORIZATION))
                .andRespond(withSuccess("""
                        {"quotes":{
                          "AAPL":{"ap":150.10,"as":40,"ax":"V","bp":150.00,"bs":40,"bx":"V","c":["R"],"t":"2026-09-30T14:59:59.41915217Z","z":"C"},
                          "MSFT":{"ap":420.20,"as":1,"ax":"V","bp":420.00,"bs":2,"bx":"V","c":["R"],"t":"2026-09-30T14:59:58Z","z":"C"}
                        }}
                        """, MediaType.APPLICATION_JSON));

        Map<String, AlpacaQuote> quotes = client.latestQuotes(List.of("AAPL", "MSFT"));

        assertEquals(2, quotes.size());
        assertEquals(0, new BigDecimal("150.00").compareTo(quotes.get("AAPL").bp()));
        assertEquals(0, new BigDecimal("150.10").compareTo(quotes.get("AAPL").ap()));
        assertEquals(Instant.parse("2026-09-30T14:59:59.41915217Z"), quotes.get("AAPL").t());
        assertTrue(client.isConfigured());
        alpaca.verify();
    }

    @Test
    void isNotConfiguredWithoutKeys() {
        AlpacaMarketDataClient client = new AlpacaMarketDataClient(new RestTemplateBuilder(), "", "");

        assertFalse(client.isConfigured());
    }
}
