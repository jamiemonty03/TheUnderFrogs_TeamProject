package com.neueda.instrumentservice.services;

import java.util.List;
import org.springframework.stereotype.Service;
import com.neueda.instrumentservice.dtos.responses.PricePointResponse;
import com.neueda.instrumentservice.exceptions.InstrumentNotFoundException;
import com.neueda.instrumentservice.mappers.InstrumentMapper;
import com.neueda.instrumentservice.mappers.PriceHistoryMapper;

@Service
public class PriceHistoryService {

    public static final int DEFAULT_DAYS = 252;
    static final int MAX_DAYS = 1260;

    private final PriceHistoryMapper priceHistoryMapper;
    private final InstrumentMapper instrumentMapper;

    public PriceHistoryService(PriceHistoryMapper priceHistoryMapper, InstrumentMapper instrumentMapper) {
        this.priceHistoryMapper = priceHistoryMapper;
        this.instrumentMapper = instrumentMapper;
    }

    public List<PricePointResponse> getHistory(String symbol, int days) throws InstrumentNotFoundException {
        if (!instrumentMapper.exists(symbol)) {
            throw new InstrumentNotFoundException(symbol);
        }
        return priceHistoryMapper.findRecent(symbol, Math.max(1, Math.min(days, MAX_DAYS)));
    }
}
