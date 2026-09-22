package com.neueda.positionservice.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.neueda.positionservice.models.Position;
import com.neueda.positionservice.services.PositionService;
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
}
