package com.neueda.orderservice.repositories;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import com.neueda.orderservice.models.Position;

public class InMemoryPositionRepository implements PositionRepository {
    private final Map<String, Position> positions = new HashMap<>();

    @Override
    public Position save(Position position) {
        String key = position.getAccountId() + "-" + position.getSymbol();
        positions.put(key, position);
        return position;
    }

    @Override
    public Optional<Position> findByAccountIdAndSymbol(String accountId, String symbol) {
        String key = accountId + "-" + symbol;
        return Optional.ofNullable(positions.get(key));
    }

    @Override
    public Optional<Position> findById(String id) {
        return Optional.ofNullable(positions.get(id));
    }
}
