package com.neueda.instrumentservice.controllers;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import org.springframework.context.annotation.Import;
import com.neueda.instrumentservice.clients.AlpacaNewsClient;
import com.neueda.instrumentservice.config.SecurityConfig;
import com.neueda.instrumentservice.config.TestJwtIssuer;
import com.neueda.instrumentservice.dtos.responses.NewsResponse;
import com.neueda.instrumentservice.mappers.TrackedTickerMapper;

@WebMvcTest(NewsController.class)
@Import(SecurityConfig.class)
@WithMockUser
class NewsControllerTest {

    @DynamicPropertySource
    static void auth(DynamicPropertyRegistry registry) {
        TestJwtIssuer.registerProperties(registry);
    }

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AlpacaNewsClient newsClient;

    @MockBean
    private TrackedTickerMapper trackedTickerMapper;

    @Test
    void returnsNewsWithDefaultLimit() throws Exception {
        when(trackedTickerMapper.findActiveSymbols()).thenReturn(List.of("AAPL", "SPY"));
        when(newsClient.getNews(5, List.of("AAPL", "SPY"))).thenReturn(List.of(new NewsResponse(
                "Markets rally", "https://example.com/article",
                Instant.parse("2026-10-14T08:00:00Z"), null, List.of("AAPL"))));

        mockMvc.perform(get("/news"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].headline").value("Markets rally"))
                .andExpect(jsonPath("$[0].articleUrl").value("https://example.com/article"))
                .andExpect(jsonPath("$[0].symbols[0]").value("AAPL"));

        verify(newsClient).getNews(5, List.of("AAPL", "SPY"));
    }

    @Test
    void rejectsLimitOutsideAllowedRange() throws Exception {
        mockMvc.perform(get("/news").param("limit", "26"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.errorCode").value("VAL-422"));

        verifyNoInteractions(newsClient, trackedTickerMapper);
    }

    @Test
    void returnsEmptyNewsWhenNoSymbolsAreActive() throws Exception {
        when(trackedTickerMapper.findActiveSymbols()).thenReturn(List.of());

        mockMvc.perform(get("/news"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());

        verifyNoInteractions(newsClient);
    }
}