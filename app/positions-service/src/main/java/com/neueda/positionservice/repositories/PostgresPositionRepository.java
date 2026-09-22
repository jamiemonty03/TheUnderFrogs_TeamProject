package com.neueda.positionservice.repositories;

import com.neueda.positionservice.mappers.PositionMapper;
import com.neueda.positionservice.models.Position;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.List;

@Repository
public class PostgresPositionRepository implements PositionRepository {
    
    private final PositionMapper mapper;
    
    public PostgresPositionRepository(PositionMapper mapper) {
        this.mapper = mapper;
    }
    
    @Override
    public Position save(Position position) {
        if (exists(position.getAccountId(), position.getSymbol())) {
            mapper.updatePosition(position);
        } else {
            mapper.insertPosition(position);
        }
        return position;
    }
    
    @Override
    public Optional<Position> findByAccountAndSymbol(String accountId, String symbol) {
        Position position = mapper.selectByAccountAndSymbol(accountId, symbol);
        return Optional.ofNullable(position);
    }
    
    @Override
    public List<Position> findByAccountId(String accountId) {
        return mapper.selectByAccount(accountId);
    }
    
    @Override
    public boolean delete(String accountId, String symbol) {
        int rowsAffected = mapper.deleteByAccountAndSymbol(accountId, symbol);
        return rowsAffected > 0;
    }
    
    @Override
    public boolean exists(String accountId, String symbol) {
        Integer count = mapper.countByAccountAndSymbol(accountId, symbol);
        return count != null && count > 0;
    }
}