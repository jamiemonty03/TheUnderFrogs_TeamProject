package com.neueda.instrumentservice.controllers;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.context.annotation.Import;
import com.neueda.instrumentservice.config.SecurityConfig;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import com.neueda.instrumentservice.dtos.responses.InstrumentResponse;
import com.neueda.instrumentservice.exceptions.InstrumentNotFoundException;
import com.neueda.instrumentservice.services.InstrumentService;

@WebMvcTest(InstrumentController.class)
@Import(SecurityConfig.class)
@WithMockUser
public class InstrumentControllerTest {

    private static final String TEST_SECRET = "instrument-test-secret-32-bytes-minimum";

    private String createJwt() throws Exception {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer("accounts-service")
                .subject("test-user")
                .issueTime(new Date())
                .expirationTime(Date.from(Instant.now().plusSeconds(60)))
                .build();
        SignedJWT jwt = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.HS256).keyID("key-1").build(), claims);
        jwt.sign(new MACSigner(TEST_SECRET.getBytes(StandardCharsets.UTF_8)));
        return jwt.serialize();
    }


    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private InstrumentService instrumentService;


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
                        .header("Authorization", "Bearer " + createJwt()))
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
