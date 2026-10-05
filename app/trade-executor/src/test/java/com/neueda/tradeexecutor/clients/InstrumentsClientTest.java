package com.neueda.tradeexecutor.clients;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withResourceNotFound;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import com.neueda.tradeexecutor.dtos.InstrumentDto;

class InstrumentsClientTest {

    private static final String INSTRUMENTS_URL = "http://instruments-service/api/instruments";

    private MockRestServiceServer instrumentsService;
    private InstrumentsClient instrumentsClient;

    @BeforeEach
    void setUp() {
        RestTemplate restTemplate = new RestTemplate();
        instrumentsService = MockRestServiceServer.bindTo(restTemplate).build();
        instrumentsClient = new InstrumentsClient(restTemplate, INSTRUMENTS_URL);
    }

    private static String instrumentJson(boolean tradable) {
        return """
            {
              "symbol": "AAPL",
              "name": "Apple Inc.",
              "assetClass": "Equity",
              "currency": "USD",
              "exchange": "NasdaqGS",
              "tradable": %s
            }
            """.formatted(tradable);
    }

    @Test
    void getInstrumentReturnsTradableInstrument() {
        instrumentsService.expect(requestTo(INSTRUMENTS_URL + "/AAPL"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(instrumentJson(true), MediaType.APPLICATION_JSON));

        Optional<InstrumentDto> instrument = instrumentsClient.getInstrument("AAPL");

        assertTrue(instrument.isPresent());
        assertEquals("AAPL", instrument.get().symbol());
        assertEquals("Equity", instrument.get().assetClass());
        assertTrue(instrument.get().tradable());
        instrumentsService.verify();
    }

    @Test
    void getInstrumentReturnsNonTradableInstrument() {
        instrumentsService.expect(requestTo(INSTRUMENTS_URL + "/AAPL"))
                .andRespond(withSuccess(instrumentJson(false), MediaType.APPLICATION_JSON));

        Optional<InstrumentDto> instrument = instrumentsClient.getInstrument("AAPL");

        assertTrue(instrument.isPresent());
        assertFalse(instrument.get().tradable());
    }

    @Test
    void getInstrumentReturnsEmptyWhenSymbolIsUnknown() {
        instrumentsService.expect(requestTo(INSTRUMENTS_URL + "/NOPE"))
                .andRespond(withResourceNotFound());

        Optional<InstrumentDto> instrument = instrumentsClient.getInstrument("NOPE");

        assertTrue(instrument.isEmpty());
    }

    @Test
    void getInstrumentThrowsWhenServiceFails() {
        instrumentsService.expect(requestTo(INSTRUMENTS_URL + "/AAPL"))
                .andRespond(withServerError());

        assertThrows(HttpServerErrorException.class, () -> instrumentsClient.getInstrument("AAPL"));
    }

    @Test
    void getTradableSymbolsReturnsOnlyTradableInstruments() {
        instrumentsService.expect(requestTo(INSTRUMENTS_URL))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess("""
                        [
                          {"symbol":"AAPL","name":"Apple Inc.","assetClass":"Equity","currency":"USD","exchange":"NasdaqGS","tradable":true},
                          {"symbol":"OLD","name":"Delisted Co","assetClass":"Equity","currency":"USD","exchange":"NYSE","tradable":false},
                          {"symbol":"SPY","name":"SPDR S&P 500","assetClass":"ETF","currency":"USD","exchange":"NYSEArca","tradable":true}
                        ]
                        """, MediaType.APPLICATION_JSON));

        assertEquals(List.of("AAPL", "SPY"), instrumentsClient.getTradableSymbols());
    }
}
