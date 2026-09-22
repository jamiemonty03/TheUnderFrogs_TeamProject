package com.neueda.instrumentservice.services;

import java.util.List;
import org.springframework.stereotype.Service;
import com.neueda.instrumentservice.dtos.responses.StockResponse;
import com.neueda.instrumentservice.exceptions.InstrumentNotFoundException;
import com.neueda.instrumentservice.models.Stock;
import com.neueda.instrumentservice.repositories.StockRepository;

@Service
public class StockService {

    private final StockRepository stockRepository;

    public StockService(StockRepository stockRepository) {
        this.stockRepository = stockRepository;
    }

    public List<StockResponse> getAllStocks() {
        return stockRepository.findAll().stream()
                .map(StockService::toResponse)
                .toList();
    }

    public StockResponse getStockBySymbol(String symbol) throws InstrumentNotFoundException {
        Stock stock = stockRepository.findBySymbol(symbol)
                .orElseThrow(() -> new InstrumentNotFoundException(symbol));
        return toResponse(stock);
    }

    private static StockResponse toResponse(Stock stock) {
        return new StockResponse(
                stock.getSymbol(),
                stock.getName(),
                stock.getPrice(),
                stock.getTradeDate(),
                stock.getCreatedAt(),
                stock.getLastUpdated(),
                stock.getSector(),
                stock.getIndustry(),
                stock.getCountry(),
                stock.getFullTimeEmployees(),
                stock.getBeta(),
                stock.getTrailingPe(),
                stock.getForwardPe(),
                stock.getTrailingEps(),
                stock.getDividendRate(),
                stock.getPayoutRatio(),
                stock.getPriceToBook(),
                stock.getReturnOnEquity(),
                stock.getMarketCap(),
                stock.getSharesOutstanding(),
                stock.getTotalRevenue(),
                stock.getWebsite()
        );
    }
}
