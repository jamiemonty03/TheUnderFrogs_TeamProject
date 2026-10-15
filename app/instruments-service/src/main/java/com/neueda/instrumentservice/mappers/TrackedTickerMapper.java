package com.neueda.instrumentservice.mappers;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TrackedTickerMapper {
    List<String> findActiveSymbols();
    int deactivate(String symbol);
}
