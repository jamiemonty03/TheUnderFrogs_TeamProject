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

import com.neueda.instrumentservice.dtos.responses.EtfResponse;
import com.neueda.instrumentservice.exceptions.InstrumentNotFoundException;
import com.neueda.instrumentservice.services.EtfService;

@WebMvcTest(EtfController.class)
public class EtfControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private EtfService etfService;

    private EtfResponse newEtfResponse() {
        return new EtfResponse("SPY", "SPDR S&P 500 ETF", new BigDecimal("450.00"), LocalDateTime.now(),
                LocalDateTime.now(), LocalDateTime.now(), "Large Blend", "State Street", "ETF",
                new BigDecimal("0.09"), new BigDecimal("450.10"), new BigDecimal("400000000000"),
                new BigDecimal("399000000000"), new BigDecimal("18.5"), new BigDecimal("10.2"),
                new BigDecimal("11.8"), new BigDecimal("1.0"), new BigDecimal("1.3"));
    }

    @Test
    void getEtfs_returnsListOfEtfs() throws Exception {
        when(etfService.getAllEtfs()).thenReturn(List.of(newEtfResponse()));

        mockMvc.perform(get("/etfs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].symbol").value("SPY"));
    }

    @Test
    void getEtf_returnsMatchingEtf() throws Exception {
        when(etfService.getEtfBySymbol("SPY")).thenReturn(newEtfResponse());

        mockMvc.perform(get("/etfs/SPY"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.symbol").value("SPY"))
                .andExpect(jsonPath("$.category").value("Large Blend"));
    }

    @Test
    void getEtf_returns404WhenNotFound() throws Exception {
        when(etfService.getEtfBySymbol("ZZZZ")).thenThrow(new InstrumentNotFoundException("ZZZZ"));

        mockMvc.perform(get("/etfs/ZZZZ"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("INS-404"));
    }
}
