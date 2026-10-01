package com.neueda.orderservice.services;

import com.neueda.orderservice.models.ClientTrade;
import com.neueda.orderservice.models.PositionStep;
import com.neueda.orderservice.repositories.ClientTradeRepository;
import com.neueda.orderservice.enums.OrderSide;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("ClientTradeService Unit Tests")
public class ClientTradeServiceTest {

    private ClientTradeService clientTradeService;

    @Mock
    private ClientTradeRepository clientTradeRepository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        clientTradeService = new ClientTradeService(clientTradeRepository);
    }

    @Test
    @DisplayName("Constructor throws when repository is null")
    void testNullRepositoryThrows() {
        assertThrows(IllegalArgumentException.class, () ->
            new ClientTradeService(null)
        );
    }

    @Test
    @DisplayName("Get account history retrieves all trades for account")
    void testGetAccountHistory() {
        String accountId = "ACC001";
        List<ClientTrade> trades = Arrays.asList(
            new ClientTrade(accountId, "AAPL", OrderSide.BUY, new BigDecimal("100"), new BigDecimal("150"), LocalDate.now()),
            new ClientTrade(accountId, "MSFT", OrderSide.SELL, new BigDecimal("50"), new BigDecimal("300"), LocalDate.now())
        );

        when(clientTradeRepository.findByAccountIdOrderByTradeDateDescTradeIdDesc(accountId)).thenReturn(trades);

        List<ClientTrade> result = clientTradeService.getAccountHistory(accountId);

        assertEquals(2, result.size());
        assertEquals("AAPL", result.get(0).getSymbol());
        assertEquals("MSFT", result.get(1).getSymbol());
        verify(clientTradeRepository).findByAccountIdOrderByTradeDateDescTradeIdDesc(accountId);
    }

    @Test
    @DisplayName("Get account history throws for null accountId")
    void testGetAccountHistoryNullAccountId() {
        assertThrows(IllegalArgumentException.class, () ->
            clientTradeService.getAccountHistory(null)
        );
    }

    @Test
    @DisplayName("Get account history throws for blank accountId")
    void testGetAccountHistoryBlankAccountId() {
        assertThrows(IllegalArgumentException.class, () ->
            clientTradeService.getAccountHistory("  ")
        );
    }

    @Test
    @DisplayName("Get symbol history retrieves trades for account and symbol")
    void testGetSymbolHistory() {
        String accountId = "ACC001";
        String symbol = "AAPL";
        List<ClientTrade> trades = Arrays.asList(
            new ClientTrade(accountId, symbol, OrderSide.BUY, new BigDecimal("100"), new BigDecimal("150"), LocalDate.now()),
            new ClientTrade(accountId, symbol, OrderSide.BUY, new BigDecimal("50"), new BigDecimal("155"), LocalDate.now())
        );

        when(clientTradeRepository.findByAccountIdAndSymbolOrderByTradeDateDescTradeIdDesc(accountId, symbol)).thenReturn(trades);

        List<ClientTrade> result = clientTradeService.getSymbolHistory(accountId, symbol);

        assertEquals(2, result.size());
        verify(clientTradeRepository).findByAccountIdAndSymbolOrderByTradeDateDescTradeIdDesc(accountId, symbol);
    }

    @Test
    @DisplayName("Get symbol history throws for null accountId")
    void testGetSymbolHistoryNullAccountId() {
        assertThrows(IllegalArgumentException.class, () ->
            clientTradeService.getSymbolHistory(null, "AAPL")
        );
    }

    @Test
    @DisplayName("Get symbol history throws for null symbol")
    void testGetSymbolHistoryNullSymbol() {
        assertThrows(IllegalArgumentException.class, () ->
            clientTradeService.getSymbolHistory("ACC001", null)
        );
    }

    @Test
    @DisplayName("Calculate historical buy quantity sums all BUY trades")
    void testGetHistoricalBuyQuantity() {
        String accountId = "ACC001";
        String symbol = "AAPL";
        List<ClientTrade> trades = Arrays.asList(
            new ClientTrade(accountId, symbol, OrderSide.BUY, new BigDecimal("100"), new BigDecimal("150"), LocalDate.now()),
            new ClientTrade(accountId, symbol, OrderSide.BUY, new BigDecimal("50"), new BigDecimal("155"), LocalDate.now()),
            new ClientTrade(accountId, symbol, OrderSide.SELL, new BigDecimal("30"), new BigDecimal("160"), LocalDate.now())
        );

        when(clientTradeRepository.findByAccountIdAndSymbolOrderByTradeDateDescTradeIdDesc(accountId, symbol)).thenReturn(trades);

        BigDecimal result = clientTradeService.getHistoricalBuyQuantity(accountId, symbol);

        assertEquals(new BigDecimal("150"), result);
    }

    @Test
    @DisplayName("Calculate historical buy quantity returns zero when no buys")
    void testGetHistoricalBuyQuantityNoTrades() {
        String accountId = "ACC001";
        String symbol = "AAPL";
        List<ClientTrade> trades = Arrays.asList(
            new ClientTrade(accountId, symbol, OrderSide.SELL, new BigDecimal("100"), new BigDecimal("150"), LocalDate.now())
        );

        when(clientTradeRepository.findByAccountIdAndSymbolOrderByTradeDateDescTradeIdDesc(accountId, symbol)).thenReturn(trades);

        BigDecimal result = clientTradeService.getHistoricalBuyQuantity(accountId, symbol);

        assertEquals(BigDecimal.ZERO, result);
    }

    @Test
    @DisplayName("Calculate historical sell quantity sums all SELL trades")
    void testGetHistoricalSellQuantity() {
        String accountId = "ACC001";
        String symbol = "AAPL";
        List<ClientTrade> trades = Arrays.asList(
            new ClientTrade(accountId, symbol, OrderSide.BUY, new BigDecimal("100"), new BigDecimal("150"), LocalDate.now()),
            new ClientTrade(accountId, symbol, OrderSide.SELL, new BigDecimal("30"), new BigDecimal("160"), LocalDate.now()),
            new ClientTrade(accountId, symbol, OrderSide.SELL, new BigDecimal("20"), new BigDecimal("165"), LocalDate.now())
        );

        when(clientTradeRepository.findByAccountIdAndSymbolOrderByTradeDateDescTradeIdDesc(accountId, symbol)).thenReturn(trades);

        BigDecimal result = clientTradeService.getHistoricalSellQuantity(accountId, symbol);

        assertEquals(new BigDecimal("50"), result);
    }

    @Test
    @DisplayName("Calculate weighted average cost correctly")
    void testGetWeightedAverageCost() {
        String accountId = "ACC001";
        String symbol = "AAPL";
        List<ClientTrade> trades = Arrays.asList(
            new ClientTrade(accountId, symbol, OrderSide.BUY, new BigDecimal("100"), new BigDecimal("150"), LocalDate.now()),
            new ClientTrade(accountId, symbol, OrderSide.BUY, new BigDecimal("200"), new BigDecimal("160"), LocalDate.now()),
            new ClientTrade(accountId, symbol, OrderSide.SELL, new BigDecimal("50"), new BigDecimal("170"), LocalDate.now())
        );

        when(clientTradeRepository.findByAccountIdAndSymbolOrderByTradeDateDescTradeIdDesc(accountId, symbol)).thenReturn(trades);

        BigDecimal result = clientTradeService.getWeightedAverageCost(accountId, symbol);

        BigDecimal expected = new BigDecimal("100").multiply(new BigDecimal("150"))
            .add(new BigDecimal("200").multiply(new BigDecimal("160")))
            .divide(new BigDecimal("300"), 2, java.math.RoundingMode.HALF_UP);

        assertEquals(expected, result);
    }

    @Test
    @DisplayName("Weighted average cost returns zero when no BUY trades")
    void testGetWeightedAverageCostNoBuys() {
        String accountId = "ACC001";
        String symbol = "AAPL";
        List<ClientTrade> trades = Arrays.asList(
            new ClientTrade(accountId, symbol, OrderSide.SELL, new BigDecimal("50"), new BigDecimal("170"), LocalDate.now())
        );

        when(clientTradeRepository.findByAccountIdAndSymbolOrderByTradeDateDescTradeIdDesc(accountId, symbol)).thenReturn(trades);

        BigDecimal result = clientTradeService.getWeightedAverageCost(accountId, symbol);

        assertEquals(BigDecimal.ZERO, result);
    }

    @Test
    @DisplayName("Weighted average cost returns zero for empty history")
    void testGetWeightedAverageCostEmptyHistory() {
        String accountId = "ACC001";
        String symbol = "AAPL";

        when(clientTradeRepository.findByAccountIdAndSymbolOrderByTradeDateDescTradeIdDesc(accountId, symbol)).thenReturn(Collections.emptyList());

        BigDecimal result = clientTradeService.getWeightedAverageCost(accountId, symbol);

        assertEquals(BigDecimal.ZERO, result);
    }

    @Test
    @DisplayName("Reconcile with position passes when historical quantity matches position")
    void testReconcileWithPositionPass() {
        String accountId = "ACC001";
        String symbol = "AAPL";
        List<ClientTrade> trades = newestFirst(
            new ClientTrade(accountId, symbol, OrderSide.BUY, new BigDecimal("100"), new BigDecimal("150"), LocalDate.now()),
            new ClientTrade(accountId, symbol, OrderSide.SELL, new BigDecimal("30"), new BigDecimal("160"), LocalDate.now())
        );

        when(clientTradeRepository.findByAccountIdAndSymbolOrderByTradeDateDescTradeIdDesc(accountId, symbol)).thenReturn(trades);

        boolean result = clientTradeService.reconcileWithPosition(accountId, symbol, new BigDecimal("70"));

        assertTrue(result);
    }

    @Test
    @DisplayName("Reconcile with position fails when quantities don't match")
    void testReconcileWithPositionFail() {
        String accountId = "ACC001";
        String symbol = "AAPL";
        List<ClientTrade> trades = newestFirst(
            new ClientTrade(accountId, symbol, OrderSide.BUY, new BigDecimal("100"), new BigDecimal("150"), LocalDate.now()),
            new ClientTrade(accountId, symbol, OrderSide.SELL, new BigDecimal("30"), new BigDecimal("160"), LocalDate.now())
        );

        when(clientTradeRepository.findByAccountIdAndSymbolOrderByTradeDateDescTradeIdDesc(accountId, symbol)).thenReturn(trades);

        boolean result = clientTradeService.reconcileWithPosition(accountId, symbol, new BigDecimal("50"));

        assertFalse(result);
    }

    @Test
    @DisplayName("Reconcile throws for null position quantity")
    void testReconcileNullPositionQuantity() {
        assertThrows(IllegalArgumentException.class, () ->
            clientTradeService.reconcileWithPosition("ACC001", "AAPL", null)
        );
    }

    @Test
    @DisplayName("Save trade persists to repository")
    void testSaveTrade() {
        ClientTrade trade = new ClientTrade("ACC001", "AAPL", OrderSide.BUY, new BigDecimal("100"), new BigDecimal("150"), LocalDate.now());
        when(clientTradeRepository.save(trade)).thenReturn(trade);

        ClientTrade result = clientTradeService.saveTrade(trade);

        assertNotNull(result);
        assertEquals("AAPL", result.getSymbol());
        verify(clientTradeRepository).save(trade);
    }

    @Test
    @DisplayName("Save trade throws for null trade")
    void testSaveTradeNull() {
        assertThrows(IllegalArgumentException.class, () ->
            clientTradeService.saveTrade(null)
        );
    }

    @Test
    @DisplayName("Get trade by ID retrieves from repository")
    void testGetTradeById() {
        ClientTrade trade = new ClientTrade("ACC001", "AAPL", OrderSide.BUY, new BigDecimal("100"), new BigDecimal("150"), LocalDate.now());
        trade.setTradeId(1);
        when(clientTradeRepository.findById(1)).thenReturn(Optional.of(trade));

        Optional<ClientTrade> result = clientTradeService.getTradeById(1);

        assertTrue(result.isPresent());
        assertEquals(1, result.get().getTradeId());
        verify(clientTradeRepository).findById(1);
    }

    @Test
    @DisplayName("Get trade by ID throws for null ID")
    void testGetTradeByIdNull() {
        assertThrows(IllegalArgumentException.class, () ->
            clientTradeService.getTradeById(null)
        );
    }


    @Test
    @DisplayName("Delete trade removes from repository")
    void testDeleteTrade() {
        when(clientTradeRepository.existsById(1)).thenReturn(true);

        boolean result = clientTradeService.deleteTrade(1);

        assertTrue(result);
        verify(clientTradeRepository).deleteById(1);
    }

    @Test
    @DisplayName("Delete trade returns false and deletes nothing when trade is missing")
    void testDeleteTradeMissing() {
        when(clientTradeRepository.existsById(99)).thenReturn(false);

        boolean result = clientTradeService.deleteTrade(99);

        assertFalse(result);
        verify(clientTradeRepository, never()).deleteById(99);
    }

    @Test
    @DisplayName("Delete trade throws for null ID")
    void testDeleteTradeNull() {
        assertThrows(IllegalArgumentException.class, () ->
            clientTradeService.deleteTrade(null)
        );
    }


    @Test
    @DisplayName("Weighted average cost handles fractional results correctly")
    void testWeightedAverageCostRounding() {
        String accountId = "ACC001";
        String symbol = "AAPL";
        List<ClientTrade> trades = Arrays.asList(
            new ClientTrade(accountId, symbol, OrderSide.BUY, new BigDecimal("3"), new BigDecimal("100.00"), LocalDate.now()),
            new ClientTrade(accountId, symbol, OrderSide.BUY, new BigDecimal("1"), new BigDecimal("101.00"), LocalDate.now())
        );

        when(clientTradeRepository.findByAccountIdAndSymbolOrderByTradeDateDescTradeIdDesc(accountId, symbol)).thenReturn(trades);

        BigDecimal result = clientTradeService.getWeightedAverageCost(accountId, symbol);

        assertEquals(new BigDecimal("100.25"), result);
    }

    @Test
    @DisplayName("Get symbol history throws for blank symbol")
    void testGetSymbolHistoryBlankSymbol() {
        assertThrows(IllegalArgumentException.class, () ->
            clientTradeService.getSymbolHistory("ACC001", "  ")
        );
    }

    @Test
    @DisplayName("Get account history throws for blank accountId")
    void testGetAccountHistoryBlankId() {
        assertThrows(IllegalArgumentException.class, () ->
            clientTradeService.getAccountHistory("   ")
        );
    }

    private ClientTrade trade(OrderSide side, String quantity) {
        return new ClientTrade("ACC001", "AAPL", side, new BigDecimal(quantity), new BigDecimal("150"), LocalDate.now());
    }

    // Trades are given oldest first; the repository returns them newest first
    private void givenAaplHistory(ClientTrade... trades) {
        when(clientTradeRepository.findByAccountIdAndSymbolOrderByTradeDateDescTradeIdDesc("ACC001", "AAPL"))
            .thenReturn(newestFirst(trades));
    }

    private List<ClientTrade> newestFirst(ClientTrade... trades) {
        List<ClientTrade> list = new ArrayList<>(Arrays.asList(trades));
        Collections.reverse(list);
        return list;
    }

    private List<BigDecimal> quantitiesAfter(List<PositionStep> steps) {
        return steps.stream().map(PositionStep::quantityAfter).toList();
    }

    @Test
    @DisplayName("Position history: one buy of 10 reaches 10")
    void testPositionHistorySingleBuy() {
        givenAaplHistory(trade(OrderSide.BUY, "10"));

        List<PositionStep> steps = clientTradeService.getPositionHistory("ACC001", "AAPL");

        assertEquals(List.of(new BigDecimal("10")), quantitiesAfter(steps));
        assertTrue(clientTradeService.reconcileWithPosition("ACC001", "AAPL", new BigDecimal("10")));
    }

    @Test
    @DisplayName("Position history: buy 20, sell 10 reaches 10")
    void testPositionHistoryBuyThenSell() {
        givenAaplHistory(trade(OrderSide.BUY, "20"), trade(OrderSide.SELL, "10"));

        List<PositionStep> steps = clientTradeService.getPositionHistory("ACC001", "AAPL");

        assertEquals(List.of(new BigDecimal("20"), new BigDecimal("10")), quantitiesAfter(steps));
        assertTrue(clientTradeService.reconcileWithPosition("ACC001", "AAPL", new BigDecimal("10")));
    }

    @Test
    @DisplayName("Position history: buy 15, three sells of 5 reaches 0")
    void testPositionHistoryBuyThenThreeSells() {
        givenAaplHistory(trade(OrderSide.BUY, "15"), trade(OrderSide.SELL, "5"),
            trade(OrderSide.SELL, "5"), trade(OrderSide.SELL, "5"));

        List<PositionStep> steps = clientTradeService.getPositionHistory("ACC001", "AAPL");

        assertEquals(List.of(new BigDecimal("15"), new BigDecimal("10"), new BigDecimal("5"), new BigDecimal("0")),
            quantitiesAfter(steps));
        assertTrue(clientTradeService.reconcileWithPosition("ACC001", "AAPL", BigDecimal.ZERO));
        assertFalse(clientTradeService.reconcileWithPosition("ACC001", "AAPL", new BigDecimal("10")));
    }

    @Test
    @DisplayName("Position history is empty when there are no trades")
    void testPositionHistoryEmpty() {
        givenAaplHistory();

        assertTrue(clientTradeService.getPositionHistory("ACC001", "AAPL").isEmpty());
        assertTrue(clientTradeService.reconcileWithPosition("ACC001", "AAPL", BigDecimal.ZERO));
    }

    @Test
    @DisplayName("Reconcile fails when history sells shares not yet held, even if the net matches")
    void testReconcileFailsWhenHistoryGoesShort() {
        givenAaplHistory(trade(OrderSide.SELL, "10"), trade(OrderSide.BUY, "20"));

        List<PositionStep> steps = clientTradeService.getPositionHistory("ACC001", "AAPL");

        assertEquals(List.of(new BigDecimal("-10"), new BigDecimal("10")), quantitiesAfter(steps));
        assertFalse(clientTradeService.reconcileWithPosition("ACC001", "AAPL", new BigDecimal("10")));
    }
}
