package com.neueda.instrumentservice.services;

import java.util.List;
import org.springframework.stereotype.Service;
import com.neueda.instrumentservice.dtos.responses.BondResponse;
import com.neueda.instrumentservice.exceptions.InstrumentNotFoundException;
import com.neueda.instrumentservice.models.Bond;
import com.neueda.instrumentservice.mappers.BondMapper;

@Service
public class BondService {

    private final BondMapper bondMapper;

    public BondService(BondMapper bondMapper) {
        this.bondMapper = bondMapper;
    }

    public List<BondResponse> getAllBonds() {
        return bondMapper.findAll().stream()
                .map(BondService::toResponse)
                .toList();
    }

    public BondResponse getBondBySymbol(String symbol) throws InstrumentNotFoundException {
        Bond bond = bondMapper.findBySymbol(symbol)
                .orElseThrow(() -> new InstrumentNotFoundException(symbol));
        return toResponse(bond);
    }

    private static BondResponse toResponse(Bond bond) {
        return new BondResponse(
                bond.getSymbol(),
                bond.getName(),
                bond.getPrice(),
                bond.getTradeDate(),
                bond.getCreatedAt(),
                bond.getLastUpdated(),
                bond.getCategory(),
                bond.getFundFamily(),
                bond.getLegalType(),
                bond.getNetExpenseRatio(),
                bond.getNavPrice(),
                bond.getTotalAssets(),
                bond.getNetAssets(),
                bond.getYieldToMaturity(),
                bond.getCouponRate(),
                bond.getDuration(),
                bond.getBeta3Year(),
                bond.getYtdReturn(),
                bond.getThreeYearAvgReturn(),
                bond.getFiveYearAvgReturn(),
                bond.getDistributionYield()
        );
    }
}
