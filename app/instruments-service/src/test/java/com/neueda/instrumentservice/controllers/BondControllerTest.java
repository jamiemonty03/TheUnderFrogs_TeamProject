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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.context.annotation.Import;
import com.neueda.instrumentservice.config.SecurityConfig;

import com.neueda.instrumentservice.dtos.responses.BondResponse;
import com.neueda.instrumentservice.exceptions.InstrumentNotFoundException;
import com.neueda.instrumentservice.services.BondService;

@WebMvcTest(BondController.class)
@Import(SecurityConfig.class)
@WithMockUser
public class BondControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private BondService bondService;

    private BondResponse newBondResponse() {
        return new BondResponse("BND01", "US Treasury 10Y", new BigDecimal("98.50"), LocalDateTime.now(),
                LocalDateTime.now(), LocalDateTime.now(), "Government", "Vanguard", "Open-End Fund",
                new BigDecimal("0.05"), new BigDecimal("98.50"), new BigDecimal("50000000000"),
                new BigDecimal("49500000000"), new BigDecimal("4.25"), new BigDecimal("3.75"),
                new BigDecimal("6.5"), new BigDecimal("4.9"), new BigDecimal("2.1"),
                new BigDecimal("1.8"), new BigDecimal("2.0"), new BigDecimal("3.6"));
    }

    @Test
    void getBonds_returnsListOfBonds() throws Exception {
        when(bondService.getAllBonds()).thenReturn(List.of(newBondResponse()));

        mockMvc.perform(get("/bonds"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].symbol").value("BND01"));
    }

    @Test
    void getBond_returnsMatchingBond() throws Exception {
        when(bondService.getBondBySymbol("BND01")).thenReturn(newBondResponse());

        mockMvc.perform(get("/bonds/BND01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.symbol").value("BND01"))
                .andExpect(jsonPath("$.category").value("Government"));
    }

    @Test
    void getBond_returns404WhenNotFound() throws Exception {
        when(bondService.getBondBySymbol("ZZZZ")).thenThrow(new InstrumentNotFoundException("ZZZZ"));

        mockMvc.perform(get("/bonds/ZZZZ"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("INS-404"));
    }
}
