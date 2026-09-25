package com.neueda.instrumentservice.services;

import java.util.List;
import org.springframework.stereotype.Service;
import com.neueda.instrumentservice.dtos.responses.EtfResponse;
import com.neueda.instrumentservice.exceptions.InstrumentNotFoundException;
import com.neueda.instrumentservice.models.Etf;
import com.neueda.instrumentservice.mappers.EtfMapper;

@Service
public class EtfService {

    private final EtfMapper etfMapper;

    public EtfService(EtfMapper etfMapper) {
        this.etfMapper = etfMapper;
    }

    public List<EtfResponse> getAllEtfs() {
        return etfMapper.findAll().stream()
                .map(EtfService::toResponse)
                .toList();
    }

    public EtfResponse getEtfBySymbol(String symbol) throws InstrumentNotFoundException {
        Etf etf = etfMapper.findBySymbol(symbol)
                .orElseThrow(() -> new InstrumentNotFoundException(symbol));
        return toResponse(etf);
    }

    private static EtfResponse toResponse(Etf etf) {
        return new EtfResponse(
                etf.getSymbol(),
                etf.getName(),
                etf.getPrice(),
                etf.getTradeDate(),
                etf.getCreatedAt(),
                etf.getLastUpdated(),
                etf.getCategory(),
                etf.getFundFamily(),
                etf.getLegalType(),
                etf.getNetExpenseRatio(),
                etf.getNavPrice(),
                etf.getTotalAssets(),
                etf.getNetAssets(),
                etf.getYtdReturn(),
                etf.getThreeYearAvgReturn(),
                etf.getFiveYearAvgReturn(),
                etf.getBeta3Year(),
                etf.getDistributionYield()
        );
    }
}
