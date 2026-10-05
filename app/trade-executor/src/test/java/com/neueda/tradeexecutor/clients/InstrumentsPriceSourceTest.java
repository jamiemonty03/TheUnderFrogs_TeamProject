package com.neueda.tradeexecutor.clients;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withResourceNotFound;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import com.neueda.tradeexecutor.dtos.InstrumentDto;
import com.neueda.tradeexecutor.enums.OrderSide;

class InstrumentsPriceSourceTest {

    private static final String PRICES_URL = "http://instruments-service/api";

    private MockRestServiceServer instrumentsService;
    private InstrumentsPriceSource priceSource;

    @BeforeEach
    void setUp() {
        RestTemplate restTemplate = new RestTemplate();
        instrumentsService = MockRestServiceServer.bindTo(restTemplate).build();
        priceSource = new InstrumentsPriceSource(restTemplate, PRICES_URL);
    }

    private static InstrumentDto instrument(String symbol, String assetClass) {
        return new InstrumentDto(symbol, assetClass, true);
    }

    // The shape shared by StockResponse, EtfResponse and BondResponse, plus a field PriceDto ignores
    private static String priceJson(String symbol, String price) {
        return """
            {
              "symbol": "%s",
              "name": "Some instrument",
              "price": %s,
              "tradeDate": "2026-09-25T00:00:00"
            }
            """.formatted(symbol, price);
    }

    @Test
    void equityPriceComesFromStocks() {
        instrumentsService.expect(requestTo(PRICES_URL + "/stocks/AAPL"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(priceJson("AAPL", "148.50"), MediaType.APPLICATION_JSON));

        Optional<BigDecimal> price = priceSource.getPrice(instrument("AAPL", "Equity"), OrderSide.BUY);

        assertEquals(0, new BigDecimal("148.50").compareTo(price.orElseThrow()));
        instrumentsService.verify();
    }

    @Test
    void etfPriceComesFromEtfs() {
        instrumentsService.expect(requestTo(PRICES_URL + "/etfs/SPY"))
                .andRespond(withSuccess(priceJson("SPY", "520.10"), MediaType.APPLICATION_JSON));

        Optional<BigDecimal> price = priceSource.getPrice(instrument("SPY", "ETF"), OrderSide.BUY);

        assertEquals(0, new BigDecimal("520.10").compareTo(price.orElseThrow()));
        instrumentsService.verify();
    }

    @Test
    void bondPriceComesFromBonds() {
        instrumentsService.expect(requestTo(PRICES_URL + "/bonds/BND"))
                .andRespond(withSuccess(priceJson("BND", "72.35"), MediaType.APPLICATION_JSON));

        Optional<BigDecimal> price = priceSource.getPrice(instrument("BND", "Bond"), OrderSide.BUY);

        assertEquals(0, new BigDecimal("72.35").compareTo(price.orElseThrow()));
        instrumentsService.verify();
    }

    @Test
    void returnsEmptyWhenNoPriceExists() {
        instrumentsService.expect(requestTo(PRICES_URL + "/stocks/AAPL"))
                .andRespond(withResourceNotFound());

        Optional<BigDecimal> price = priceSource.getPrice(instrument("AAPL", "Equity"), OrderSide.BUY);

        assertTrue(price.isEmpty());
    }

    @Test
    void throwsWhenServiceFails() {
        instrumentsService.expect(requestTo(PRICES_URL + "/stocks/AAPL"))
                .andRespond(withServerError());

        assertThrows(HttpServerErrorException.class,
                () -> priceSource.getPrice(instrument("AAPL", "Equity"), OrderSide.BUY));
    }

    @Test
    void returnsEmptyWithoutCallingForUnknownAssetClass() {
        Optional<BigDecimal> price = priceSource.getPrice(instrument("GOLD", "Commodity"), OrderSide.BUY);

        assertTrue(price.isEmpty());
        // verify() fails if any request was made
        instrumentsService.verify();
    }
}
