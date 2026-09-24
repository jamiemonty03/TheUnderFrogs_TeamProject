package com.neueda.positionservice.repositories;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import com.neueda.positionservice.models.Position;
import com.neueda.positionservice.models.PositionId;

public interface PositionRepository extends JpaRepository<Position, PositionId> {

    List<Position> findByAccountIdOrderBySymbol(String accountId);
}
