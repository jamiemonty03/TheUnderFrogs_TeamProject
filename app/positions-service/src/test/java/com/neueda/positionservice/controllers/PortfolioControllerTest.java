package com.neueda.positionservice.controllers;

import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import com.neueda.positionservice.config.SecurityConfig;
import com.neueda.positionservice.dtos.requests.CreatePortfolioRequest;
import com.neueda.positionservice.dtos.requests.UpdatePortfolioRequest;
import com.neueda.positionservice.enums.PortfolioRisk;
import com.neueda.positionservice.exceptions.PortfolioConflictException;
import com.neueda.positionservice.exceptions.PortfolioNotFoundException;
import com.neueda.positionservice.models.Portfolio;
import com.neueda.positionservice.services.PortfolioService;

@WebMvcTest(PortfolioController.class)
@Import(SecurityConfig.class)
@DisplayName("PortfolioController")
class PortfolioControllerTest {

    private static final String ACCOUNT = "ACC0011";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PortfolioService portfolioService;

    /** A signed-in trader whose token says they own ACCOUNT. */
    private static RequestPostProcessor trader() {
        return jwt().jwt(token -> token.claim("accountId", ACCOUNT));
    }

    private static Portfolio growth() {
        return new Portfolio("PF0042", ACCOUNT, "Growth", PortfolioRisk.HIGH, "Nasdaq-100");
    }

    @Test
    @DisplayName("GET /portfolios/{accountId}: lists the signed-in user's portfolios")
    void listPortfolios() throws Exception {
        when(portfolioService.getPortfolios(ACCOUNT)).thenReturn(List.of(growth()));

        mockMvc.perform(get("/portfolios/" + ACCOUNT).with(trader()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].portfolioId", equalTo("PF0042")))
            .andExpect(jsonPath("$[0].name", equalTo("Growth")))
            .andExpect(jsonPath("$[0].risk", equalTo("HIGH")))
            .andExpect(jsonPath("$[0].defaultPortfolio", equalTo(false)));
    }

    @Test
    @DisplayName("GET /portfolios/{accountId}: another user's account is forbidden")
    void listAnotherAccountsPortfolios() throws Exception {
        mockMvc.perform(get("/portfolios/ACC0001").with(trader()))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.errorCode", equalTo("AUTH-403")));

        verify(portfolioService, never()).getPortfolios(anyString());
    }

    @Test
    @DisplayName("GET /portfolios/{accountId}: needs a token")
    void listWithoutToken() throws Exception {
        mockMvc.perform(get("/portfolios/" + ACCOUNT)).andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /portfolios/{accountId}/{portfolioId}: unknown portfolio is 404 PFL-404")
    void getUnknownPortfolio() throws Exception {
        when(portfolioService.getPortfolio(ACCOUNT, "PF9999"))
            .thenThrow(new PortfolioNotFoundException(ACCOUNT, "PF9999"));

        mockMvc.perform(get("/portfolios/" + ACCOUNT + "/PF9999").with(trader()))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.errorCode", equalTo("PFL-404")));
    }

    @Test
    @DisplayName("POST /portfolios/{accountId}: creates a portfolio and returns 201")
    void createPortfolio() throws Exception {
        when(portfolioService.createPortfolio(eq(ACCOUNT), any(CreatePortfolioRequest.class))).thenReturn(growth());

        mockMvc.perform(post("/portfolios/" + ACCOUNT).with(trader())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Growth\",\"risk\":\"HIGH\",\"benchmark\":\"Nasdaq-100\"}"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.portfolioId", equalTo("PF0042")));
    }

    @Test
    @DisplayName("POST /portfolios/{accountId}: a missing name is 422")
    void createPortfolioWithoutName() throws Exception {
        mockMvc.perform(post("/portfolios/" + ACCOUNT).with(trader())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"risk\":\"HIGH\"}"))
            .andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.message", equalTo("Portfolio name is required")));

        verify(portfolioService, never()).createPortfolio(anyString(), any());
    }

    @Test
    @DisplayName("POST /portfolios/{accountId}: an unknown risk value is 400")
    void createPortfolioWithUnknownRisk() throws Exception {
        mockMvc.perform(post("/portfolios/" + ACCOUNT).with(trader())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Growth\",\"risk\":\"EXTREME\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /portfolios/{accountId}: a duplicate name is 409 PFL-409")
    void createDuplicatePortfolio() throws Exception {
        when(portfolioService.createPortfolio(eq(ACCOUNT), any(CreatePortfolioRequest.class)))
            .thenThrow(new PortfolioConflictException("There's already a portfolio called Growth"));

        mockMvc.perform(post("/portfolios/" + ACCOUNT).with(trader())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Growth\",\"risk\":\"HIGH\"}"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.errorCode", equalTo("PFL-409")));
    }

    @Test
    @DisplayName("PATCH /portfolios/{accountId}/{portfolioId}: updates the portfolio")
    void updatePortfolio() throws Exception {
        when(portfolioService.updatePortfolio(eq(ACCOUNT), eq("PF0042"), any(UpdatePortfolioRequest.class)))
            .thenReturn(growth());

        mockMvc.perform(patch("/portfolios/" + ACCOUNT + "/PF0042").with(trader())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"risk\":\"HIGH\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.risk", equalTo("HIGH")));
    }

    @Test
    @DisplayName("PATCH /portfolios/{accountId}/{portfolioId}: a blank name is 422")
    void updatePortfolioBlankName() throws Exception {
        mockMvc.perform(patch("/portfolios/" + ACCOUNT + "/PF0042").with(trader())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"   \"}"))
            .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @DisplayName("DELETE /portfolios/{accountId}/{portfolioId}: deletes and returns 204")
    void deletePortfolio() throws Exception {
        mockMvc.perform(delete("/portfolios/" + ACCOUNT + "/PF0042").with(trader()))
            .andExpect(status().isNoContent());

        verify(portfolioService).deletePortfolio(ACCOUNT, "PF0042");
    }

    @Test
    @DisplayName("DELETE /portfolios/{accountId}/{portfolioId}: deleting Main is 409")
    void deleteMainPortfolio() throws Exception {
        doThrow(new PortfolioConflictException("The Main portfolio can't be deleted"))
            .when(portfolioService).deletePortfolio(ACCOUNT, "PF0011");

        mockMvc.perform(delete("/portfolios/" + ACCOUNT + "/PF0011").with(trader()))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.message", equalTo("The Main portfolio can't be deleted")));
    }
}
