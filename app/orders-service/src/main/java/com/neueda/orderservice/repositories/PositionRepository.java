package com.neueda.orderservice.repositories;

import java.util.Optional;
import com.neueda.orderservice.models.Position;

public interface PositionRepository {
    Position save(Position position);
    Optional<Position> findByAccountIdAndSymbol(String accountId, String symbol);
    Optional<Position> findById(String id);
}
