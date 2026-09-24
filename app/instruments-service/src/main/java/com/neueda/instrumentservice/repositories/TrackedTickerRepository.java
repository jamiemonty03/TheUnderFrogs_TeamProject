package com.neueda.instrumentservice.repositories;

import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TrackedTickerRepository {
    int deactivate(String symbol);
}
