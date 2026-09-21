package com.neueda.positionservice.repositories;

import java.util.Optional;
import java.util.List;
import com.neueda.positionservice.models.Position;

public interface PositionRepository {
    
    Position save(Position position);
    Optional<Position> findByAccountAndSymbol(String accountId, String symbol);
    List<Position> findByAccountId(String accountId);
    boolean delete(String accountId, String symbol);
    boolean exists(String accountId, String symbol);
}
