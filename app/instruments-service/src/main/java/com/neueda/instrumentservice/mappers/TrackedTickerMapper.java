package com.neueda.instrumentservice.mappers;

import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TrackedTickerMapper {
    int deactivate(String symbol);
}
