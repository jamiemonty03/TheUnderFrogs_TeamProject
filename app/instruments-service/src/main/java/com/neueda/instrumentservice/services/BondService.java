package com.neueda.instrumentservice.services;

import java.util.List;
import org.springframework.stereotype.Service;
import com.neueda.instrumentservice.dtos.responses.BondResponse;
import com.neueda.instrumentservice.exceptions.InstrumentNotFoundException;
import com.neueda.instrumentservice.models.Bond;
import com.neueda.instrumentservice.repositories.BondRepository;

@Service
public class BondService {

    private final BondRepository bondRepository;

    public BondService(BondRepository bondRepository) {
        this.bondRepository = bondRepository;
    }

    public List<BondResponse> getAllBonds() {
        return bondRepository.findAll().stream()
                .map(BondService::toResponse)
                .toList();
    }

    public BondResponse getBondBySymbol(String symbol) throws InstrumentNotFoundException {
        Bond bond = bondRepository.findBySymbol(symbol)
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
