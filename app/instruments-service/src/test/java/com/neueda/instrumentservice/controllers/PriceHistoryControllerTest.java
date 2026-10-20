package com.neueda.instrumentservice.controllers;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import com.neueda.instrumentservice.config.SecurityConfig;
import com.neueda.instrumentservice.config.TestJwtIssuer;
import com.neueda.instrumentservice.dtos.responses.PricePointResponse;
import com.neueda.instrumentservice.exceptions.InstrumentNotFoundException;
import com.neueda.instrumentservice.services.PriceHistoryService;

@WebMvcTest(PriceHistoryController.class)
@Import(SecurityConfig.class)
@WithMockUser
class PriceHistoryControllerTest {

    @DynamicPropertySource
    static void auth(DynamicPropertyRegistry registry) {
        TestJwtIssuer.registerProperties(registry);
    }

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PriceHistoryService priceHistoryService;

    @Test
    void returnsTheDailyClosesOldestFirst() throws Exception {
        when(priceHistoryService.getHistory("AAPL", 252)).thenReturn(List.of(
                new PricePointResponse(LocalDate.of(2026, 10, 13), new BigDecimal("344.67"), new BigDecimal("348.15"),
                        new BigDecimal("340.74"), new BigDecimal("341.27"), 38_983_800L, new BigDecimal("336.28"), new BigDecimal("323.77")),
                new PricePointResponse(LocalDate.of(2026, 10, 14), new BigDecimal("344.61"), new BigDecimal("347.09"),
                        new BigDecimal("341.72"), new BigDecimal("342.96"), 50_061_100L, new BigDecimal("336.81"), null)));

        mockMvc.perform(get("/instruments/AAPL/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].date").value("2026-10-13"))
                .andExpect(jsonPath("$[1].close").value(342.96))
                .andExpect(jsonPath("$[1].movingAvg50").doesNotExist());
    }

    @Test
    void includesEachDaysOpenHighLowAndVolumeForCandlesticks() throws Exception {
        when(priceHistoryService.getHistory("AAPL", 252)).thenReturn(List.of(
                new PricePointResponse(LocalDate.of(2026, 10, 13), new BigDecimal("344.67"), new BigDecimal("348.15"),
                        new BigDecimal("340.74"), new BigDecimal("341.27"), 38_983_800L, null, null)));

        mockMvc.perform(get("/instruments/AAPL/history"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].open").value(344.67))
                .andExpect(jsonPath("$[0].high").value(348.15))
                .andExpect(jsonPath("$[0].low").value(340.74))
                .andExpect(jsonPath("$[0].volume").value(38_983_800));
    }

    @Test
    void passesTheNumberOfDaysAsked() throws Exception {
        when(priceHistoryService.getHistory("AAPL", 21)).thenReturn(List.of());

        mockMvc.perform(get("/instruments/AAPL/history").param("days", "21")).andExpect(status().isOk());

        verify(priceHistoryService).getHistory("AAPL", 21);
    }

    @Test
    void unknownSymbolIs404() throws Exception {
        when(priceHistoryService.getHistory("NOPE", 252)).thenThrow(new InstrumentNotFoundException("NOPE"));

        mockMvc.perform(get("/instruments/NOPE/history")).andExpect(status().isNotFound());
    }

    @Test
    @WithAnonymousUser
    void needsAToken() throws Exception {
        mockMvc.perform(get("/instruments/AAPL/history")).andExpect(status().isUnauthorized());

        verifyNoInteractions(priceHistoryService);
    }
}
