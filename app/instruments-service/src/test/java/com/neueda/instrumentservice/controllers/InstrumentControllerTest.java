package com.neueda.instrumentservice.controllers;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import com.neueda.instrumentservice.dtos.responses.InstrumentResponse;
import com.neueda.instrumentservice.exceptions.InstrumentNotFoundException;
import com.neueda.instrumentservice.services.InstrumentService;

@WebMvcTest(InstrumentController.class)
public class InstrumentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private InstrumentService instrumentService;

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
}
