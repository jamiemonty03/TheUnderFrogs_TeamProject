package com.neueda.instrumentservice.config;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import com.neueda.instrumentservice.controllers.BondController;
import com.neueda.instrumentservice.controllers.EtfController;
import com.neueda.instrumentservice.controllers.InstrumentController;
import com.neueda.instrumentservice.controllers.StockController;
import com.neueda.instrumentservice.services.BondService;
import com.neueda.instrumentservice.services.EtfService;
import com.neueda.instrumentservice.services.InstrumentService;
import com.neueda.instrumentservice.services.StockService;

@WebMvcTest({StockController.class, EtfController.class, BondController.class, InstrumentController.class})
@Import(SecurityConfig.class)
class PublicPricesSecurityTest {

    @DynamicPropertySource
    static void auth(DynamicPropertyRegistry registry) {
        TestJwtIssuer.registerProperties(registry);
    }

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StockService stockService;

    @MockBean
    private EtfService etfService;

    @MockBean
    private BondService bondService;

    @MockBean
    private InstrumentService instrumentService;

    @Test
    @DisplayName("The stock, ETF and bond price lists can be read without a token")
    void priceListsArePublic() throws Exception {
        when(stockService.getAllStocks()).thenReturn(List.of());
        when(etfService.getAllEtfs()).thenReturn(List.of());
        when(bondService.getAllBonds()).thenReturn(List.of());

        mockMvc.perform(get("/stocks")).andExpect(status().isOk());
        mockMvc.perform(get("/etfs")).andExpect(status().isOk());
        mockMvc.perform(get("/bonds")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Everything else still needs a token")
    void everythingElseNeedsAToken() throws Exception {
        mockMvc.perform(get("/stocks/AAPL")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/etfs/SPY")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/bonds/AGG")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/instruments")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/instruments/live-prices")).andExpect(status().isUnauthorized());
        mockMvc.perform(delete("/instruments/AAPL")).andExpect(status().isUnauthorized());
    }
}
