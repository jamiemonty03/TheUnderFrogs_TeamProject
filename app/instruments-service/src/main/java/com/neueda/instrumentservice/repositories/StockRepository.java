package com.neueda.instrumentservice.repositories;

import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;
import com.neueda.instrumentservice.models.Stock;

@Mapper
public interface StockRepository {
    List<Stock> findAll();
    Optional<Stock> findBySymbol(String symbol);
}
