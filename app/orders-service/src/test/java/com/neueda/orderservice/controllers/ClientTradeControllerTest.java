package com.neueda.orderservice.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import com.neueda.orderservice.config.SecurityConfig;
import com.neueda.orderservice.enums.OrderSide;
import com.neueda.orderservice.exceptions.InstrumentServiceUnavailableException;
import com.neueda.orderservice.models.ClientTrade;
import com.neueda.orderservice.models.ClientTradeFilter;
import com.neueda.orderservice.services.ClientTradeService;
import com.neueda.orderservice.services.InstrumentDirectory;

@WebMvcTest(ClientTradeController.class)
@Import(SecurityConfig.class)
class ClientTradeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ClientTradeService clientTradeService;

    @MockBean
    private InstrumentDirectory instrumentDirectory;

    @Test
    @DisplayName("GET /client-trades passes account, dates and side through and returns full records")
    void searchByAccountDatesAndSide() throws Exception {
        ClientTradeFilter expected = new ClientTradeFilter("ACC0001",
            LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31), null, OrderSide.BUY);
        when(clientTradeService.search(expected)).thenReturn(List.of(aaplBuy()));

        mockMvc.perform(get("/client-trades").with(jwt())
                .param("accountId", "ACC0001").param("from", "2026-01-01").param("to", "2026-01-31").param("side", "BUY"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].tradeId").value(7))
            .andExpect(jsonPath("$[0].accountId").value("ACC0001"))
            .andExpect(jsonPath("$[0].symbol").value("AAPL"))
            .andExpect(jsonPath("$[0].tradeType").value("BUY"))
            .andExpect(jsonPath("$[0].quantity").value(15))
            .andExpect(jsonPath("$[0].price").value(188.00))
            .andExpect(jsonPath("$[0].tradeDate").value("2026-01-08"))
            .andExpect(jsonPath("$[0].version").value(0))
            .andExpect(jsonPath("$[0].createdAt").exists())
            .andExpect(jsonPath("$[0].updatedBy").value("SYSTEM"));
        verifyNoInteractions(instrumentDirectory);
    }

    @Test
    @DisplayName("GET /client-trades?assetClass= filters to the asset class's symbols")
    void searchByAssetClass() throws Exception {
        when(instrumentDirectory.symbolsInAssetClass("ETF")).thenReturn(Set.of("SPY", "TLT"));
        when(clientTradeService.search(any())).thenReturn(List.of());

        mockMvc.perform(get("/client-trades").with(jwt()).param("assetClass", "ETF"))
            .andExpect(status().isOk());

        verify(clientTradeService).search(new ClientTradeFilter(null, null, null, Set.of("SPY", "TLT"), null));
    }

    @Test
    @DisplayName("A symbol outside the requested asset class matches nothing")
    void symbolOutsideAssetClass() throws Exception {
        when(instrumentDirectory.symbolsInAssetClass("ETF")).thenReturn(Set.of("SPY"));
        when(clientTradeService.search(any())).thenReturn(List.of());

        mockMvc.perform(get("/client-trades").with(jwt()).param("assetClass", "ETF").param("symbol", "AAPL"))
            .andExpect(status().isOk());

        verify(clientTradeService).search(new ClientTradeFilter(null, null, null, Set.of(), null));
    }

    @Test
    @DisplayName("Invalid filters are rejected with 422 VAL-422")
    void invalidFilters() throws Exception {
        when(clientTradeService.search(any())).thenThrow(new IllegalArgumentException("'from' date must be on or before 'to' date"));
        mockMvc.perform(get("/client-trades").with(jwt()).param("from", "2026-03-01").param("to", "2026-02-01"))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.errorCode").value("VAL-422"));

        mockMvc.perform(get("/client-trades").with(jwt()).param("from", "01/02/2026"))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.errorCode").value("VAL-422"));

        mockMvc.perform(get("/client-trades").with(jwt()).param("side", "HOLD"))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.errorCode").value("VAL-422"));
    }

    @Test
    @DisplayName("Instruments-service outage returns 503 INS-503")
    void instrumentsServiceDown() throws Exception {
        when(instrumentDirectory.symbolsInAssetClass("Equity"))
            .thenThrow(new InstrumentServiceUnavailableException("Instrument data is unavailable", null));

        mockMvc.perform(get("/client-trades").with(jwt()).param("assetClass", "Equity"))
            .andExpect(status().isServiceUnavailable())
            .andExpect(jsonPath("$.errorCode").value("INS-503"));
    }

    @Test
    @DisplayName("GET /client-trades/{tradeId} returns the trade, or 404 TRD-404")
    void getTradeById() throws Exception {
        when(clientTradeService.getTradeById(7)).thenReturn(Optional.of(aaplBuy()));
        when(clientTradeService.getTradeById(99)).thenReturn(Optional.empty());

        mockMvc.perform(get("/client-trades/7").with(jwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.tradeId").value(7));
        mockMvc.perform(get("/client-trades/99").with(jwt()))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.errorCode").value("TRD-404"));
    }

    @Test
    @DisplayName("GET /client-trades/account/{accountId} returns the account's history")
    void getTradesByAccount() throws Exception {
        when(clientTradeService.getAccountHistory("ACC0001")).thenReturn(List.of(aaplBuy()));

        mockMvc.perform(get("/client-trades/account/ACC0001").with(jwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].symbol").value("AAPL"));
    }

    @Test
    @DisplayName("Requests without a token are rejected with 401")
    void requiresAuthentication() throws Exception {
        mockMvc.perform(get("/client-trades")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/client-trades/7")).andExpect(status().isUnauthorized());
        verifyNoInteractions(clientTradeService);
    }

    private static ClientTrade aaplBuy() {
        ClientTrade trade = new ClientTrade("ACC0001", "AAPL", OrderSide.BUY,
            new BigDecimal("15.0000"), new BigDecimal("188.00"), LocalDate.of(2026, 1, 8));
        trade.setTradeId(7);
        return trade;
    }
}
