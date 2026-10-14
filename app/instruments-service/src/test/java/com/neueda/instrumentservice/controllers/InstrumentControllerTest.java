package com.neueda.instrumentservice.controllers;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.context.annotation.Import;
import com.neueda.instrumentservice.config.SecurityConfig;

import com.neueda.instrumentservice.dtos.responses.InstrumentResponse;
import com.neueda.instrumentservice.dtos.responses.LivePriceResponse;
import com.neueda.instrumentservice.exceptions.InstrumentNotFoundException;
import com.neueda.instrumentservice.services.InstrumentService;
import com.neueda.instrumentservice.services.LivePriceCache;
import com.neueda.instrumentservice.config.TestJwtIssuer;

@WebMvcTest(InstrumentController.class)
@Import(SecurityConfig.class)
@WithMockUser
public class InstrumentControllerTest {

    @DynamicPropertySource
    static void auth(DynamicPropertyRegistry registry) {
        TestJwtIssuer.registerProperties(registry);
    }


    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private InstrumentService instrumentService;

    @MockBean
    private LivePriceCache livePriceCache;


    @Test
    @WithAnonymousUser
    void endpointsRequireBearerToken() throws Exception {
        mockMvc.perform(get("/instruments"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(instrumentService);
    }

    @Test
    @WithAnonymousUser
    void validBearerTokenAllowsAccess() throws Exception {
        when(instrumentService.getAllInstruments()).thenReturn(List.of());

        mockMvc.perform(get("/instruments")
                        .header("Authorization", "Bearer " + TestJwtIssuer.userToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void getInstruments_returnsListOfInstruments() throws Exception {
        InstrumentResponse apple = new InstrumentResponse("AAPL", "Apple Inc.", "EQUITY", "USD", "NASDAQ", true);
        InstrumentResponse tesla = new InstrumentResponse("TSLA", "Tesla Inc.", "EQUITY", "USD", "NASDAQ", false);
        when(instrumentService.getAllInstruments()).thenReturn(List.of(apple, tesla));

        mockMvc.perform(get("/instruments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].symbol").value("AAPL"))
                .andExpect(jsonPath("$[1].symbol").value("TSLA"));
    }

    @Test
    void getLivePrices_returnsCachedQuotesForRequestedSymbols() throws Exception {
        LivePriceResponse apple = new LivePriceResponse("AAPL", new BigDecimal("189.85"),
                new BigDecimal("189.80"), new BigDecimal("189.90"), Instant.parse("2026-10-13T14:30:00Z"));
        when(livePriceCache.get(List.of("AAPL", "TSLA"))).thenReturn(List.of(apple));

        mockMvc.perform(get("/instruments/live-prices").param("symbols", "AAPL,TSLA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].symbol").value("AAPL"))
                .andExpect(jsonPath("$[0].price").value(189.85))
                .andExpect(jsonPath("$[0].quoteTime").value("2026-10-13T14:30:00Z"));
        verifyNoInteractions(instrumentService);
    }

    @Test
    void getLivePrices_returnsEveryQuoteWhenNoSymbolsAreGiven() throws Exception {
        LivePriceResponse apple = new LivePriceResponse("AAPL", new BigDecimal("189.85"),
                new BigDecimal("189.80"), new BigDecimal("189.90"), Instant.parse("2026-10-13T14:30:00Z"));
        LivePriceResponse spy = new LivePriceResponse("SPY", new BigDecimal("571.32"),
                new BigDecimal("571.30"), new BigDecimal("571.34"), Instant.parse("2026-10-13T14:30:00Z"));
        when(livePriceCache.getAll()).thenReturn(List.of(apple, spy));

        mockMvc.perform(get("/instruments/live-prices"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[1].symbol").value("SPY"));
    }

    @Test
    void getInstrument_returnsMatchingInstrument() throws Exception {
        InstrumentResponse apple = new InstrumentResponse("AAPL", "Apple Inc.", "EQUITY", "USD", "NASDAQ", true);
        when(instrumentService.getInstrumentBySymbol("AAPL")).thenReturn(apple);

        mockMvc.perform(get("/instruments/AAPL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.symbol").value("AAPL"))
                .andExpect(jsonPath("$.name").value("Apple Inc."))
                .andExpect(jsonPath("$.assetClass").value("EQUITY"))
                .andExpect(jsonPath("$.tradable").value(true));
    }

    @Test
    void getInstrument_returns404WhenNotFound() throws Exception {
        when(instrumentService.getInstrumentBySymbol("ZZZZ"))
                .thenThrow(new InstrumentNotFoundException("ZZZZ"));

        mockMvc.perform(get("/instruments/ZZZZ"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("INS-404"))
                .andExpect(jsonPath("$.message").value("Instrument not found"));
    }

    @Test
    void deleteInstrument_returns204WhenDeleted() throws Exception {
        mockMvc.perform(delete("/instruments/AAPL"))
                .andExpect(status().isNoContent());

        // tracked_tickers deactivation is asserted in InstrumentServiceTest
        verify(instrumentService).deleteInstrument("AAPL");
    }

    @Test
    void deleteInstrument_returns404WhenNotFound() throws Exception {
        org.mockito.Mockito.doThrow(new InstrumentNotFoundException("ZZZZ"))
                .when(instrumentService).deleteInstrument("ZZZZ");

        mockMvc.perform(delete("/instruments/ZZZZ"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("INS-404"));
    }
}
