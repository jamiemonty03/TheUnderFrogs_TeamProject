package com.neueda.leap.repositories;

import java.util.Map;
import java.util.Optional;
import java.util.List;
import java.util.stream.Collectors;
import java.util.HashMap;
import com.neueda.leap.models.Position;

public class InMemoryPositionRepository implements PositionRepository {
    
    private final Map<String, Position> positions = new HashMap<>();

    private String generateKey(String accountId, String symbol) {
        return accountId + ":" + symbol;
    }

    @Override
    public Position save(Position position) {
        if (position == null) {
            throw new IllegalArgumentException("Position cannot be null");
        }
        if (position.getAccountId() == null || position.getAccountId().trim().isEmpty()) {
            throw new IllegalArgumentException("Account ID cannot be null or empty");
        }
        if (position.getSymbol() == null || position.getSymbol().trim().isEmpty()) {
            throw new IllegalArgumentException("Symbol cannot be null or empty");
        }
        
        String key = generateKey(position.getAccountId(), position.getSymbol());
        positions.put(key, position);
        return position;
    }

    @Override
    public Optional<Position> findByAccountAndSymbol(String accountId, String symbol) {
        if (accountId == null || accountId.trim().isEmpty() ||
            symbol == null || symbol.trim().isEmpty()) {
            return Optional.empty();
        }
        String key = generateKey(accountId, symbol);
        return Optional.ofNullable(positions.get(key));
    }

   @Override
    public List<Position> findByAccountId(String accountId) {
        if (accountId == null || accountId.trim().isEmpty()) {
            return List.of();
        }
        String prefix = accountId + ":";
        return positions.values().stream()
            .filter(position -> position.getAccountId().equals(accountId))
            .collect(Collectors.toList());
    }

    @Override
    public boolean delete(String accountId, String symbol) {
        if (accountId == null || accountId.trim().isEmpty() ||
            symbol == null || symbol.trim().isEmpty()) {
            return false;
        }
        String key = generateKey(accountId, symbol);
        return positions.remove(key) != null;
    }

    @Override
    public boolean exists(String accountId, String symbol) {
        if (accountId == null || accountId.trim().isEmpty() ||
            symbol == null || symbol.trim().isEmpty()) {
            return false;
        }
        String key = generateKey(accountId, symbol);
        return positions.containsKey(key);
    }

    public void clear() {
        positions.clear();
    }

    public int count() {
        return positions.size();
    }
}
