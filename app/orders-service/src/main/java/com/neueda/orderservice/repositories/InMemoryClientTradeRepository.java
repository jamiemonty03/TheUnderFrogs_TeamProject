package com.neueda.orderservice.repositories;

import com.neueda.orderservice.models.ClientTrade;
import java.util.Optional;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * In-memory implementation of ClientTradeRepository for testing.
 * Uses ConcurrentHashMap to store trades by tradeId.
 */
public class InMemoryClientTradeRepository implements ClientTradeRepository {

    private final ConcurrentHashMap<String, ClientTrade> trades = new ConcurrentHashMap<>();

    @Override
    public ClientTrade save(ClientTrade trade) {
        if (trade == null) {
            throw new IllegalArgumentException("Trade cannot be null");
        }
        if (trade.getTradeId() == null || trade.getTradeId().trim().isEmpty()) {
            throw new IllegalArgumentException("Trade ID cannot be null or empty");
        }
        trades.put(trade.getTradeId(), trade);
        return trade;
    }

    @Override
    public Optional<ClientTrade> findById(String tradeId) {
        if (tradeId == null || tradeId.trim().isEmpty()) {
            throw new IllegalArgumentException("Trade ID cannot be null or empty");
        }
        return Optional.ofNullable(trades.get(tradeId));
    }

    @Override
    public List<ClientTrade> findByAccountId(String accountId) {
        if (accountId == null || accountId.trim().isEmpty()) {
            throw new IllegalArgumentException("Account ID cannot be null or empty");
        }
        return trades.values().stream()
            .filter(trade -> trade.getAccountId().equals(accountId))
            .collect(Collectors.toList());
    }

    @Override
    public List<ClientTrade> findByAccountAndSymbol(String accountId, String symbol) {
        if (accountId == null || accountId.trim().isEmpty()) {
            throw new IllegalArgumentException("Account ID cannot be null or empty");
        }
        if (symbol == null || symbol.trim().isEmpty()) {
            throw new IllegalArgumentException("Symbol cannot be null or empty");
        }
        return trades.values().stream()
            .filter(trade -> trade.getAccountId().equals(accountId) && trade.getSymbol().equals(symbol))
            .collect(Collectors.toList());
    }

    @Override
    public boolean delete(String tradeId) {
        if (tradeId == null || tradeId.trim().isEmpty()) {
            throw new IllegalArgumentException("Trade ID cannot be null or empty");
        }
        return trades.remove(tradeId) != null;
    }

    @Override
    public boolean exists(String tradeId) {
        if (tradeId == null || tradeId.trim().isEmpty()) {
            throw new IllegalArgumentException("Trade ID cannot be null or empty");
        }
        return trades.containsKey(tradeId);
    }

    public void clear() {
        trades.clear();
    }

    public int size() {
        return trades.size();
    }
}
