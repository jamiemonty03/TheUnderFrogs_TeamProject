package com.neueda.instrumentservice.controllers;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import com.neueda.instrumentservice.dtos.responses.StockResponse;
import com.neueda.instrumentservice.exceptions.InstrumentNotFoundException;
import com.neueda.instrumentservice.services.StockService;

@WebMvcTest(StockController.class)
public class StockControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StockService stockService;

    private StockResponse newStockResponse() {
        return new StockResponse("AAPL", "Apple Inc.", new BigDecimal("150.25"), LocalDateTime.now(),
                LocalDateTime.now(), LocalDateTime.now(), "Technology", "Consumer Electronics", "USA",
                150000, new BigDecimal("1.25"), new BigDecimal("28.5"), new BigDecimal("25.0"),
                new BigDecimal("5.5"), new BigDecimal("0.92"), new BigDecimal("0.15"),
                new BigDecimal("35.2"), new BigDecimal("1.45"), new BigDecimal("2500000000000"),
                new BigDecimal("15500000000"), new BigDecimal("383000000000"), "https://apple.com");
    }

    @Test
    void getStocks_returnsListOfStocks() throws Exception {
        when(stockService.getAllStocks()).thenReturn(List.of(newStockResponse()));

        mockMvc.perform(get("/stocks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].symbol").value("AAPL"));
    }

    @Test
    void getStock_returnsMatchingStock() throws Exception {
        when(stockService.getStockBySymbol("AAPL")).thenReturn(newStockResponse());

        mockMvc.perform(get("/stocks/AAPL"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.symbol").value("AAPL"))
                .andExpect(jsonPath("$.sector").value("Technology"));
    }

    @Test
    void getStock_returns404WhenNotFound() throws Exception {
        when(stockService.getStockBySymbol("ZZZZ")).thenThrow(new InstrumentNotFoundException("ZZZZ"));

        mockMvc.perform(get("/stocks/ZZZZ"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("INS-404"));
    }
}
