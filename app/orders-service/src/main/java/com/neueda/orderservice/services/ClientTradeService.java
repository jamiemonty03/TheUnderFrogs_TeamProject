package com.neueda.orderservice.services;

import com.neueda.orderservice.models.ClientTrade;
import com.neueda.orderservice.repositories.ClientTradeRepository;
import com.neueda.orderservice.enums.OrderSide;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

/**
 * Service for managing client trade historical data.
 * 
 * Provides business logic for querying and calculating metrics from
 * historical trade records stored in the client_trades table.
 * Used for reconciliation and weighted average cost calculations.
 */
public class ClientTradeService {

    private final ClientTradeRepository clientTradeRepository;

    public ClientTradeService(ClientTradeRepository clientTradeRepository) {
        if (clientTradeRepository == null) {
            throw new IllegalArgumentException("ClientTradeRepository cannot be null");
        }
        this.clientTradeRepository = clientTradeRepository;
    }

    public List<ClientTrade> getAccountHistory(String accountId) {
        if (accountId == null || accountId.trim().isEmpty()) {
            throw new IllegalArgumentException("AccountId cannot be null or blank");
        }
        return clientTradeRepository.findByAccountId(accountId);
    }


    public List<ClientTrade> getSymbolHistory(String accountId, String symbol) {
        if (accountId == null || accountId.trim().isEmpty()) {
            throw new IllegalArgumentException("AccountId cannot be null or blank");
        }
        if (symbol == null || symbol.trim().isEmpty()) {
            throw new IllegalArgumentException("Symbol cannot be null or blank");
        }
        return clientTradeRepository.findByAccountAndSymbol(accountId, symbol);
    }


    public BigDecimal getHistoricalBuyQuantity(String accountId, String symbol) {
        List<ClientTrade> trades = getSymbolHistory(accountId, symbol);
        return trades.stream()
            .filter(trade -> trade.getTradeType() == OrderSide.BUY)
            .map(ClientTrade::getQuantity)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }


    public BigDecimal getHistoricalSellQuantity(String accountId, String symbol) {
        List<ClientTrade> trades = getSymbolHistory(accountId, symbol);
        return trades.stream()
            .filter(trade -> trade.getTradeType() == OrderSide.SELL)
            .map(ClientTrade::getQuantity)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public BigDecimal getWeightedAverageCost(String accountId, String symbol) {
        List<ClientTrade> trades = getSymbolHistory(accountId, symbol);
        
        List<ClientTrade> buyTrades = trades.stream()
            .filter(trade -> trade.getTradeType() == OrderSide.BUY)
            .toList();

        if (buyTrades.isEmpty()) {
            return BigDecimal.ZERO;
        }

        BigDecimal totalCost = buyTrades.stream()
            .map(trade -> trade.getQuantity().multiply(trade.getPrice()))
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalQuantity = buyTrades.stream()
            .map(ClientTrade::getQuantity)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalQuantity.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        return totalCost.divide(totalQuantity, 2, java.math.RoundingMode.HALF_UP);
    }

    public boolean reconcileWithPosition(String accountId, String symbol, BigDecimal currentPositionQuantity) {
        if (currentPositionQuantity == null) {
            throw new IllegalArgumentException("Current position quantity cannot be null");
        }

        BigDecimal totalBought = getHistoricalBuyQuantity(accountId, symbol);
        BigDecimal totalSold = getHistoricalSellQuantity(accountId, symbol);
        BigDecimal expectedQuantity = totalBought.subtract(totalSold);

        return expectedQuantity.compareTo(currentPositionQuantity) == 0;
    }

    public ClientTrade saveTrade(ClientTrade trade) {
        if (trade == null) {
            throw new IllegalArgumentException("ClientTrade cannot be null");
        }
        return clientTradeRepository.save(trade);
    }

    public Optional<ClientTrade> getTradeById(String tradeId) {
        if (tradeId == null || tradeId.trim().isEmpty()) {
            throw new IllegalArgumentException("TradeId cannot be null or blank");
        }
        return clientTradeRepository.findById(tradeId);
    }

    public boolean deleteTrade(String tradeId) {
        if (tradeId == null || tradeId.trim().isEmpty()) {
            throw new IllegalArgumentException("TradeId cannot be null or blank");
        }
        return clientTradeRepository.delete(tradeId);
    }
}
