package com.neueda.positionservice.repositories;

import java.util.Optional;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import com.neueda.positionservice.models.Position;

@Mapper
public interface PositionRepository {
    
    void save(Position position);
    Optional<Position> findByAccountAndSymbol(@Param("accountId") String accountId, @Param("symbol") String symbol);
    List<Position> findByAccountId(@Param("accountId") String accountId);
    void update(Position position);
    boolean delete(@Param("accountId") String accountId, @Param("symbol") String symbol);
    boolean exists(@Param("accountId") String accountId, @Param("symbol") String symbol);
    
}
