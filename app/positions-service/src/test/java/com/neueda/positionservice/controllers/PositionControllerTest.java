package com.neueda.positionservice.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neueda.positionservice.models.Position;
import com.neueda.positionservice.services.PositionService;
import com.neueda.positionservice.dtos.requests.BuyRequest;
import com.neueda.positionservice.dtos.requests.SellRequest;
import com.neueda.positionservice.dtos.requests.UpdatePositionRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@WebMvcTest(PositionController.class)
@DisplayName("PositionController Integration Tests")
public class PositionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PositionService positionService;

    @Autowired
    private ObjectMapper objectMapper;

    private Position testPosition;

    @BeforeEach
    public void setUp() {
        testPosition = new Position("ACC001", "AAPL", new BigDecimal("100"), new BigDecimal("150.50"));
        testPosition.setVersion(1);
        testPosition.setLastUpdated(LocalDateTime.now());
        testPosition.setUpdatedBy("SYSTEM");
    }

    // ==================== CREATE POSITION TESTS ====================

    @Test
    @DisplayName("POST /positions: Successfully creates position and returns 200 OK")
    public void testCreatePositionSuccess() throws Exception {
        Position newPosition = new Position("ACC002", "MSFT", new BigDecimal("50"), new BigDecimal("350.00"));
        when(positionService.savePosition(any(Position.class))).thenReturn(newPosition);
        
        mockMvc.perform(post("/positions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(newPosition)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId", equalTo("ACC002")))
                .andExpect(jsonPath("$.symbol", equalTo("MSFT")))
                .andExpect(jsonPath("$.quantity", notNullValue()))
                .andExpect(jsonPath("$.averageCost", notNullValue()));

        verify(positionService).savePosition(any(Position.class));
    }

    @Test
    @DisplayName("POST /positions: Returns 422 when position body is invalid (missing fields)")
    public void testCreatePositionInvalidBody() throws Exception {
        mockMvc.perform(post("/positions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
                .andExpect(status().isUnprocessableEntity());

        verify(positionService, never()).savePosition(any());
    }

    @Test
    @DisplayName("POST /positions: Returns 422 when account ID is null")
    public void testCreatePositionNullAccountId() throws Exception {
        String invalidJson = "{\"symbol\":\"AAPL\",\"quantity\":100,\"averageCost\":150.50}";
        
        mockMvc.perform(post("/positions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
                .andExpect(status().isUnprocessableEntity());

        verify(positionService, never()).savePosition(any());
    }

    @Test
    @DisplayName("POST /positions: Returns 422 when symbol is null")
    public void testCreatePositionNullSymbol() throws Exception {
        String invalidJson = "{\"accountId\":\"ACC001\",\"quantity\":100,\"averageCost\":150.50}";
        
        mockMvc.perform(post("/positions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
                .andExpect(status().isUnprocessableEntity());

        verify(positionService, never()).savePosition(any());
    }

    // ==================== GET POSITIONS BY ACCOUNT TESTS ====================

    @Test
    @DisplayName("GET /positions/{accountId}: Successfully retrieves all positions for account")
    public void testGetPositionsByAccountSuccess() throws Exception {
        Position position2 = new Position("ACC001", "GOOGL", new BigDecimal("25"), new BigDecimal("2800.00"));
        List<Position> positions = Arrays.asList(testPosition, position2);
        
        when(positionService.getPositionsByAccountId("ACC001")).thenReturn(positions);

        mockMvc.perform(get("/positions/ACC001")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].accountId", equalTo("ACC001")))
                .andExpect(jsonPath("$[0].symbol", equalTo("AAPL")))
                .andExpect(jsonPath("$[1].symbol", equalTo("GOOGL")));

        verify(positionService).getPositionsByAccountId("ACC001");
    }

    @Test
    @DisplayName("GET /positions/{accountId}: Returns empty list when account has no positions")
    public void testGetPositionsByAccountEmpty() throws Exception {
        when(positionService.getPositionsByAccountId("ACC999")).thenReturn(Arrays.asList());

        mockMvc.perform(get("/positions/ACC999")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        verify(positionService).getPositionsByAccountId("ACC999");
    }

    // ==================== GET SINGLE POSITION TESTS ====================

    @Test
    @DisplayName("GET /positions/{accountId}/{symbol}: Successfully retrieves position by account and symbol")
    public void testGetPositionSuccess() throws Exception {
        when(positionService.getPosition("ACC001", "AAPL")).thenReturn(Optional.of(testPosition));

        mockMvc.perform(get("/positions/ACC001/AAPL")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accountId", equalTo("ACC001")))
                .andExpect(jsonPath("$.symbol", equalTo("AAPL")))
                .andExpect(jsonPath("$.quantity", notNullValue()))
                .andExpect(jsonPath("$.averageCost", notNullValue()))
                .andExpect(jsonPath("$.version", equalTo(1)));

        verify(positionService).getPosition("ACC001", "AAPL");
    }

    @Test
    @DisplayName("GET /positions/{accountId}/{symbol}: Returns 500 when position not found")
    public void testGetPositionNotFound() throws Exception {
        when(positionService.getPosition("ACC001", "INVALID")).thenReturn(Optional.empty());

        mockMvc.perform(get("/positions/ACC001/INVALID")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError());

        verify(positionService).getPosition("ACC001", "INVALID");
    }

    // ==================== DELETE POSITION TESTS ====================

    @Test
    @DisplayName("DELETE /positions/{accountId}/{symbol}: Successfully deletes position")
    public void testDeletePositionSuccess() throws Exception {
        when(positionService.deletePosition("ACC001", "AAPL")).thenReturn(true);

        mockMvc.perform(delete("/positions/ACC001/AAPL")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));

        verify(positionService).deletePosition("ACC001", "AAPL");
    }

    @Test
    @DisplayName("DELETE /positions/{accountId}/{symbol}: Returns false when position not found")
    public void testDeletePositionNotFound() throws Exception {
        when(positionService.deletePosition("ACC001", "INVALID")).thenReturn(false);

        mockMvc.perform(delete("/positions/ACC001/INVALID")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().string("false"));

        verify(positionService).deletePosition("ACC001", "INVALID");
    }

    // ==================== EDGE CASE TESTS ====================

    @Test
    @DisplayName("POST /positions: Successfully creates position with large quantity")
    public void testCreatePositionLargeQuantity() throws Exception {
        Position largePosition = new Position("ACC001", "SPY", new BigDecimal("10000"), new BigDecimal("450.00"));
        when(positionService.savePosition(any(Position.class))).thenReturn(largePosition);
        
        mockMvc.perform(post("/positions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(largePosition)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity", equalTo(10000)));

        verify(positionService).savePosition(any(Position.class));
    }

    @Test
    @DisplayName("POST /positions: Successfully creates position with decimal prices")
    public void testCreatePositionDecimalPrice() throws Exception {
        Position decimalPosition = new Position("ACC001", "BRK.A", new BigDecimal("1"), new BigDecimal("575123.4567"));
        when(positionService.savePosition(any(Position.class))).thenReturn(decimalPosition);
        
        mockMvc.perform(post("/positions")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(decimalPosition)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.averageCost", notNullValue()));

        verify(positionService).savePosition(any(Position.class));
    }

    // ==================== UPDATE POSITION (PUT) TESTS ====================

    @Test
    @DisplayName("PUT /positions/{accountId}/{symbol}: Successfully updates position")
    public void testUpdatePositionSuccess() throws Exception {
        Position updatedPosition = new Position("ACC001", "AAPL", new BigDecimal("150"), new BigDecimal("155.00"));
        updatedPosition.setVersion(2);
        when(positionService.updatePosition(eq("ACC001"), eq("AAPL"), any(Position.class))).thenReturn(updatedPosition);
        
        mockMvc.perform(put("/positions/ACC001/AAPL")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updatedPosition)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.symbol", equalTo("AAPL")))
                .andExpect(jsonPath("$.quantity", equalTo(150)))
                .andExpect(jsonPath("$.version", equalTo(2)));

        verify(positionService).updatePosition(eq("ACC001"), eq("AAPL"), any(Position.class));
    }

    @Test
    @DisplayName("PUT /positions/{accountId}/{symbol}: Returns 500 when position not found")
    public void testUpdatePositionNotFound() throws Exception {
        Position updateData = new Position("ACC001", "UNKNOWN", new BigDecimal("100"), new BigDecimal("150.00"));
        when(positionService.updatePosition(eq("ACC001"), eq("UNKNOWN"), any(Position.class)))
                .thenThrow(new RuntimeException("Position not found"));
        
        mockMvc.perform(put("/positions/ACC001/UNKNOWN")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateData)))
                .andExpect(status().isInternalServerError());

        verify(positionService).updatePosition(eq("ACC001"), eq("UNKNOWN"), any(Position.class));
    }

    @Test
    @DisplayName("PUT /positions/{accountId}/{symbol}: Returns 422 when quantity is invalid")
    public void testUpdatePositionInvalidQuantity() throws Exception {
        String invalidJson = "{\"quantity\":-50,\"averageCost\":155.00}";
        
        mockMvc.perform(put("/positions/ACC001/AAPL")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
                .andExpect(status().isUnprocessableEntity());

        verify(positionService, never()).updatePosition(anyString(), anyString(), any(Position.class));
    }

    @Test
    @DisplayName("PUT /positions/{accountId}/{symbol}: Returns 422 when averageCost is invalid")
    public void testUpdatePositionInvalidAverageCost() throws Exception {
        String invalidJson = "{\"quantity\":100,\"averageCost\":-50.00}";
        
        mockMvc.perform(put("/positions/ACC001/AAPL")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
                .andExpect(status().isUnprocessableEntity());

        verify(positionService, never()).updatePosition(anyString(), anyString(), any(Position.class));
    }

    @Test
    @DisplayName("PUT /positions/{accountId}/{symbol}: Returns 422 when quantity is null")
    public void testUpdatePositionNullQuantity() throws Exception {
        String invalidJson = "{\"averageCost\":155.00}";
        
        mockMvc.perform(put("/positions/ACC001/AAPL")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
                .andExpect(status().isUnprocessableEntity());

        verify(positionService, never()).updatePosition(anyString(), anyString(), any(Position.class));
    }

    @Test
    @DisplayName("PUT /positions/{accountId}/{symbol}: Returns 422 when averageCost is null")
    public void testUpdatePositionNullAverageCost() throws Exception {
        String invalidJson = "{\"quantity\":100}";
        
        mockMvc.perform(put("/positions/ACC001/AAPL")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
                .andExpect(status().isUnprocessableEntity());

        verify(positionService, never()).updatePosition(anyString(), anyString(), any(Position.class));
    }
    
    // ==================== PARTIAL UPDATE POSITION (PATCH) TESTS ====================

    @Test
    @DisplayName("PATCH /positions/{accountId}/{symbol}: Successfully patches position with quantity only")
    public void testPatchPositionQuantityOnly() throws Exception {
        Position patchedPosition = new Position("ACC001", "AAPL", new BigDecimal("120"), new BigDecimal("150.50"));
        patchedPosition.setVersion(2);
        when(positionService.updatePosition(eq("ACC001"), eq("AAPL"), any(Position.class))).thenReturn(patchedPosition);
        
        UpdatePositionRequest patchRequest = new UpdatePositionRequest(new BigDecimal("120"), null, null);
        mockMvc.perform(patch("/positions/ACC001/AAPL")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(patchRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity", equalTo(120)))
                .andExpect(jsonPath("$.version", equalTo(2)));

        verify(positionService).updatePosition(eq("ACC001"), eq("AAPL"), any(Position.class));
    }

    @Test
    @DisplayName("PATCH /positions/{accountId}/{symbol}: Successfully patches position with averageCost only")
    public void testPatchPositionAverageCostOnly() throws Exception {
        Position patchedPosition = new Position("ACC001", "AAPL", new BigDecimal("100"), new BigDecimal("160.00"));
        patchedPosition.setVersion(2);
        when(positionService.updatePosition(eq("ACC001"), eq("AAPL"), any(Position.class))).thenReturn(patchedPosition);
        
        UpdatePositionRequest patchRequest = new UpdatePositionRequest(null, new BigDecimal("160.00"), null);
        mockMvc.perform(patch("/positions/ACC001/AAPL")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(patchRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.averageCost", equalTo(160.00)))
                .andExpect(jsonPath("$.version", equalTo(2)));

        verify(positionService).updatePosition(eq("ACC001"), eq("AAPL"), any(Position.class));
    }

    @Test
    @DisplayName("PATCH /positions/{accountId}/{symbol}: Returns 500 when position not found")
    public void testPatchPositionNotFound() throws Exception {
        when(positionService.updatePosition(eq("ACC001"), eq("INVALID"), any(Position.class)))
                .thenThrow(new RuntimeException("Position not found"));
        
        UpdatePositionRequest patchRequest = new UpdatePositionRequest(new BigDecimal("100"), null, null);
        mockMvc.perform(patch("/positions/ACC001/INVALID")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(patchRequest)))
                .andExpect(status().isInternalServerError());

        verify(positionService).updatePosition(eq("ACC001"), eq("INVALID"), any(Position.class));
    }

    @Test
    @DisplayName("PATCH /positions/{accountId}/{symbol}: Successfully patches with empty request (no fields)")
    public void testPatchPositionEmptyRequest() throws Exception {
        Position patchedPosition = new Position("ACC001", "AAPL", new BigDecimal("100"), new BigDecimal("150.50"));
        patchedPosition.setVersion(2);
        when(positionService.updatePosition(eq("ACC001"), eq("AAPL"), any(Position.class))).thenReturn(patchedPosition);
        
        UpdatePositionRequest emptyRequest = new UpdatePositionRequest(null, null, null);
        mockMvc.perform(patch("/positions/ACC001/AAPL")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(emptyRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.version", equalTo(2)));

        verify(positionService).updatePosition(eq("ACC001"), eq("AAPL"), any(Position.class));
    }

    @Test
    @DisplayName("PATCH /positions/{accountId}/{symbol}: Successfully patches with updatedBy field")
    public void testPatchPositionWithUpdatedBy() throws Exception {
        Position patchedPosition = new Position("ACC001", "AAPL", new BigDecimal("100"), new BigDecimal("150.50"));
        patchedPosition.setVersion(2);
        patchedPosition.setUpdatedBy("USER123");
        when(positionService.updatePosition(eq("ACC001"), eq("AAPL"), any(Position.class))).thenReturn(patchedPosition);
        
        UpdatePositionRequest patchRequest = new UpdatePositionRequest(null, null, "USER123");
        mockMvc.perform(patch("/positions/ACC001/AAPL")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(patchRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.updatedBy", equalTo("USER123")));

        verify(positionService).updatePosition(eq("ACC001"), eq("AAPL"), any(Position.class));
    }

    @Test
    @DisplayName("PATCH /positions/{accountId}/{symbol}: Successfully patches all fields together")
    public void testPatchPositionAllFields() throws Exception {
        Position patchedPosition = new Position("ACC001", "AAPL", new BigDecimal("200"), new BigDecimal("160.00"));
        patchedPosition.setVersion(2);
        patchedPosition.setUpdatedBy("ADMIN");
        when(positionService.updatePosition(eq("ACC001"), eq("AAPL"), any(Position.class))).thenReturn(patchedPosition);
        
        UpdatePositionRequest patchRequest = new UpdatePositionRequest(new BigDecimal("200"), new BigDecimal("160.00"), "ADMIN");
        mockMvc.perform(patch("/positions/ACC001/AAPL")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(patchRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity", equalTo(200)))
                .andExpect(jsonPath("$.averageCost", equalTo(160.00)))
                .andExpect(jsonPath("$.updatedBy", equalTo("ADMIN")));

        verify(positionService).updatePosition(eq("ACC001"), eq("AAPL"), any(Position.class));
    }

    // ==================== BUY OPERATION TESTS ====================

    @Test
    @DisplayName("POST /positions/{accountId}/{symbol}/buy: Successfully executes buy operation")
    public void testBuyPositionSuccess() throws Exception {
        Position boughtPosition = new Position("ACC001", "AAPL", new BigDecimal("150"), new BigDecimal("152.00"));
        boughtPosition.setVersion(2);
        
        BuyRequest buyRequest = new BuyRequest(50, new BigDecimal("155.00"));
        when(positionService.getPosition("ACC001", "AAPL")).thenReturn(Optional.of(boughtPosition));
        
        mockMvc.perform(post("/positions/ACC001/AAPL/buy")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(buyRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.symbol", equalTo("AAPL")))
                .andExpect(jsonPath("$.quantity", notNullValue()));

        verify(positionService).updatePositionAfterBuy("ACC001", "AAPL", 50, new BigDecimal("155.00"));
    }

    @Test
    @DisplayName("POST /positions/{accountId}/{symbol}/buy: Returns 422 when quantity is null")
    public void testBuyPositionNullQuantity() throws Exception {
        String invalidJson = "{\"price\":155.00}";
        
        mockMvc.perform(post("/positions/ACC001/AAPL/buy")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
                .andExpect(status().isUnprocessableEntity());

        verify(positionService, never()).updatePositionAfterBuy(anyString(), anyString(), anyInt(), any(BigDecimal.class));
    }

    @Test
    @DisplayName("POST /positions/{accountId}/{symbol}/buy: Returns 422 when price is null")
    public void testBuyPositionNullPrice() throws Exception {
        String invalidJson = "{\"quantity\":50}";
        
        mockMvc.perform(post("/positions/ACC001/AAPL/buy")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
                .andExpect(status().isUnprocessableEntity());

        verify(positionService, never()).updatePositionAfterBuy(anyString(), anyString(), anyInt(), any(BigDecimal.class));
    }

    @Test
    @DisplayName("POST /positions/{accountId}/{symbol}/buy: Returns 422 when quantity is negative")
    public void testBuyPositionNegativeQuantity() throws Exception {
        String invalidJson = "{\"quantity\":-50,\"price\":155.00}";
        
        mockMvc.perform(post("/positions/ACC001/AAPL/buy")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
                .andExpect(status().isUnprocessableEntity());

        verify(positionService, never()).updatePositionAfterBuy(anyString(), anyString(), anyInt(), any(BigDecimal.class));
    }

    @Test
    @DisplayName("POST /positions/{accountId}/{symbol}/buy: Returns 422 when price is negative")
    public void testBuyPositionNegativePrice() throws Exception {
        String invalidJson = "{\"quantity\":50,\"price\":-155.00}";
        
        mockMvc.perform(post("/positions/ACC001/AAPL/buy")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
                .andExpect(status().isUnprocessableEntity());

        verify(positionService, never()).updatePositionAfterBuy(anyString(), anyString(), anyInt(), any(BigDecimal.class));
    }

    @Test
    @DisplayName("POST /positions/{accountId}/{symbol}/buy: Returns 422 when price is zero")
    public void testBuyPositionZeroPrice() throws Exception {
        String invalidJson = "{\"quantity\":50,\"price\":0.00}";
        
        mockMvc.perform(post("/positions/ACC001/AAPL/buy")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
                .andExpect(status().isUnprocessableEntity());

        verify(positionService, never()).updatePositionAfterBuy(anyString(), anyString(), anyInt(), any(BigDecimal.class));
    }

    @Test
    @DisplayName("POST /positions/{accountId}/{symbol}/buy: Returns 422 when quantity is zero")
    public void testBuyPositionZeroQuantity() throws Exception {
        String invalidJson = "{\"quantity\":0,\"price\":155.00}";
        
        mockMvc.perform(post("/positions/ACC001/AAPL/buy")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
                .andExpect(status().isUnprocessableEntity());

        verify(positionService, never()).updatePositionAfterBuy(anyString(), anyString(), anyInt(), any(BigDecimal.class));
    }

    @Test
    @DisplayName("POST /positions/{accountId}/{symbol}/buy: Successfully buys with large quantity")
    public void testBuyPositionLargeQuantity() throws Exception {
        Position boughtPosition = new Position("ACC001", "AAPL", new BigDecimal("10000"), new BigDecimal("152.00"));
        boughtPosition.setVersion(2);
        
        BuyRequest buyRequest = new BuyRequest(10000, new BigDecimal("155.00"));
        when(positionService.getPosition("ACC001", "AAPL")).thenReturn(Optional.of(boughtPosition));
        
        mockMvc.perform(post("/positions/ACC001/AAPL/buy")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(buyRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity", equalTo(10000)));

        verify(positionService).updatePositionAfterBuy("ACC001", "AAPL", 10000, new BigDecimal("155.00"));
    }

    @Test
    @DisplayName("POST /positions/{accountId}/{symbol}/buy: Successfully buys with decimal price")
    public void testBuyPositionDecimalPrice() throws Exception {
        Position boughtPosition = new Position("ACC001", "BRK.A", new BigDecimal("1"), new BigDecimal("575123.4567"));
        boughtPosition.setVersion(2);
        
        BuyRequest buyRequest = new BuyRequest(1, new BigDecimal("575123.4567"));
        when(positionService.getPosition("ACC001", "BRK.A")).thenReturn(Optional.of(boughtPosition));
        
        mockMvc.perform(post("/positions/ACC001/BRK.A/buy")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(buyRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.averageCost", notNullValue()));

        verify(positionService).updatePositionAfterBuy("ACC001", "BRK.A", 1, new BigDecimal("575123.4567"));
    }

    // ==================== SELL OPERATION TESTS ====================

    @Test
    @DisplayName("POST /positions/{accountId}/{symbol}/sell: Successfully executes sell operation")
    public void testSellPositionSuccess() throws Exception {
        Position soldPosition = new Position("ACC001", "AAPL", new BigDecimal("50"), new BigDecimal("150.50"));
        soldPosition.setVersion(2);
        
        SellRequest sellRequest = new SellRequest(50);
        when(positionService.getPosition("ACC001", "AAPL")).thenReturn(Optional.of(soldPosition));
        
        mockMvc.perform(post("/positions/ACC001/AAPL/sell")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sellRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.symbol", equalTo("AAPL")))
                .andExpect(jsonPath("$.quantity", notNullValue()));

        verify(positionService).updatePositionAfterSell("ACC001", "AAPL", 50);
    }

    @Test
    @DisplayName("POST /positions/{accountId}/{symbol}/sell: Returns 422 when quantity is null")
    public void testSellPositionNullQuantity() throws Exception {
        String invalidJson = "{}";
        
        mockMvc.perform(post("/positions/ACC001/AAPL/sell")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
                .andExpect(status().isUnprocessableEntity());

        verify(positionService, never()).updatePositionAfterSell(anyString(), anyString(), anyInt());
    }

    @Test
    @DisplayName("POST /positions/{accountId}/{symbol}/sell: Returns 422 when quantity is negative")
    public void testSellPositionNegativeQuantity() throws Exception {
        String invalidJson = "{\"quantity\":-50}";
        
        mockMvc.perform(post("/positions/ACC001/AAPL/sell")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
                .andExpect(status().isUnprocessableEntity());

        verify(positionService, never()).updatePositionAfterSell(anyString(), anyString(), anyInt());
    }

    @Test
    @DisplayName("POST /positions/{accountId}/{symbol}/sell: Returns 422 when quantity is zero")
    public void testSellPositionZeroQuantity() throws Exception {
        String invalidJson = "{\"quantity\":0}";
        
        mockMvc.perform(post("/positions/ACC001/AAPL/sell")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson))
                .andExpect(status().isUnprocessableEntity());

        verify(positionService, never()).updatePositionAfterSell(anyString(), anyString(), anyInt());
    }

    @Test
    @DisplayName("POST /positions/{accountId}/{symbol}/sell: Successfully sells with large quantity")
    public void testSellPositionLargeQuantity() throws Exception {
        Position soldPosition = new Position("ACC001", "AAPL", new BigDecimal("1000"), new BigDecimal("150.50"));
        soldPosition.setVersion(2);
        
        SellRequest sellRequest = new SellRequest(1000);
        when(positionService.getPosition("ACC001", "AAPL")).thenReturn(Optional.of(soldPosition));
        
        mockMvc.perform(post("/positions/ACC001/AAPL/sell")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sellRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity", notNullValue()));

        verify(positionService).updatePositionAfterSell("ACC001", "AAPL", 1000);
    }

    @Test
    @DisplayName("POST /positions/{accountId}/{symbol}/sell: Returns 500 when position not found")
    public void testSellPositionNotFound() throws Exception {
        SellRequest sellRequest = new SellRequest(50);
        doThrow(new RuntimeException("Position not found"))
                .when(positionService).updatePositionAfterSell("ACC001", "UNKNOWN", 50);
        
        mockMvc.perform(post("/positions/ACC001/UNKNOWN/sell")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sellRequest)))
                .andExpect(status().isInternalServerError());

        verify(positionService).updatePositionAfterSell("ACC001", "UNKNOWN", 50);
    }

    @Test
    @DisplayName("POST /positions/{accountId}/{symbol}/sell: Successfully sells all shares")
    public void testSellPositionAllShares() throws Exception {
        Position soldPosition = new Position("ACC001", "AAPL", new BigDecimal("0"), new BigDecimal("150.50"));
        soldPosition.setVersion(2);
        
        SellRequest sellRequest = new SellRequest(100);
        when(positionService.getPosition("ACC001", "AAPL")).thenReturn(Optional.of(soldPosition));
        
        mockMvc.perform(post("/positions/ACC001/AAPL/sell")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sellRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantity", equalTo(0)));

        verify(positionService).updatePositionAfterSell("ACC001", "AAPL", 100);
    }

    @Test
    @DisplayName("POST /positions/{accountId}/{symbol}/sell: Returns 500 when insufficient holdings")
    public void testSellPositionInsufficientHoldings() throws Exception {
        SellRequest sellRequest = new SellRequest(200);
        doThrow(new RuntimeException("Insufficient holdings"))
                .when(positionService).updatePositionAfterSell("ACC001", "AAPL", 200);
        
        mockMvc.perform(post("/positions/ACC001/AAPL/sell")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(sellRequest)))
                .andExpect(status().isInternalServerError());

        verify(positionService).updatePositionAfterSell("ACC001", "AAPL", 200);
    }
}
