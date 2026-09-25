package com.neueda.instrumentservice.mappers;

import java.util.List;
import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;
import com.neueda.instrumentservice.models.Stock;

@Mapper
public interface StockMapper {
    List<Stock> findAll();
    Optional<Stock> findBySymbol(String symbol);
}
