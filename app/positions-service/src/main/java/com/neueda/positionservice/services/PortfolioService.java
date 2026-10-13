package com.neueda.positionservice.services;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.neueda.positionservice.dtos.requests.CreatePortfolioRequest;
import com.neueda.positionservice.dtos.requests.UpdatePortfolioRequest;
import com.neueda.positionservice.exceptions.PortfolioConflictException;
import com.neueda.positionservice.exceptions.PortfolioNotFoundException;
import com.neueda.positionservice.models.Portfolio;
import com.neueda.positionservice.models.Position;
import com.neueda.positionservice.models.PositionId;
import com.neueda.positionservice.repositories.PortfolioRepository;
import com.neueda.positionservice.repositories.PositionRepository;

@Service
public class PortfolioService {

    private static final int DECIMAL_PLACES = 4;

    private final PortfolioRepository portfolioRepository;
    private final PositionRepository positionRepository;

    public PortfolioService(PortfolioRepository portfolioRepository, PositionRepository positionRepository) {
        this.portfolioRepository = portfolioRepository;
        this.positionRepository = positionRepository;
    }

    @Transactional
    public List<Portfolio> getPortfolios(String accountId) {
        getDefaultPortfolio(accountId);
        return portfolioRepository.findByAccountIdOrderByDefaultPortfolioDescNameAsc(accountId);
    }

    @Transactional
    public Portfolio getPortfolio(String accountId, String portfolioId) {
        return resolve(accountId, portfolioId);
    }

    @Transactional
    public Portfolio resolve(String accountId, String portfolioId) {
        if (portfolioId == null || portfolioId.isBlank()) {
            return getDefaultPortfolio(accountId);
        }
        return portfolioRepository.findByPortfolioIdAndAccountId(portfolioId, accountId)
            .orElseThrow(() -> new PortfolioNotFoundException(accountId, portfolioId));
    }

    @Transactional
    public Portfolio getDefaultPortfolio(String accountId) {
        return portfolioRepository.findByAccountIdAndDefaultPortfolioTrue(accountId).orElseGet(() -> {
            portfolioRepository.insertDefaultIfAbsent(accountId);
            return portfolioRepository.findByAccountIdAndDefaultPortfolioTrue(accountId)
                .orElseThrow(() -> new IllegalStateException("Main portfolio missing for account " + accountId));
        });
    }

    @Transactional
    public Portfolio createPortfolio(String accountId, CreatePortfolioRequest request) {
        getDefaultPortfolio(accountId);
        String name = request.name().trim();
        requireUniqueName(accountId, name);
        Portfolio portfolio = new Portfolio(portfolioRepository.nextPortfolioId(), accountId, name,
            request.risk(), blankToNull(request.benchmark()));
        return portfolioRepository.save(portfolio);
    }

    @Transactional
    public Portfolio updatePortfolio(String accountId, String portfolioId, UpdatePortfolioRequest request) {
        Portfolio portfolio = resolve(accountId, portfolioId);
        if (request.name() != null) {
            String name = request.name().trim();
            if (!name.equalsIgnoreCase(portfolio.getName())) {
                requireUniqueName(accountId, name);
            }
            portfolio.setName(name);
        }
        if (request.risk() != null) {
            portfolio.setRisk(request.risk());
        }
        if (request.benchmark() != null) {
            portfolio.setBenchmark(blankToNull(request.benchmark()));
        }
        return portfolioRepository.save(portfolio);
    }


    @Transactional
    public void deletePortfolio(String accountId, String portfolioId) {
        Portfolio portfolio = resolve(accountId, portfolioId);
        if (portfolio.isDefaultPortfolio()) {
            throw new PortfolioConflictException("The Main portfolio can't be deleted");
        }
        Portfolio main = getDefaultPortfolio(accountId);

        for (Position moving : positionRepository.findByPortfolioIdOrderBySymbol(portfolio.getPortfolioId())) {
            positionRepository.findById(new PositionId(main.getPortfolioId(), moving.getSymbol()))
                .ifPresentOrElse(
                    existing -> combine(existing, moving),
                    () -> positionRepository.save(new Position(accountId, main.getPortfolioId(),
                        moving.getSymbol(), moving.getQuantity(), moving.getAverageCost())));
            positionRepository.delete(moving);
        }
        portfolioRepository.delete(portfolio);
    }

    private void combine(Position into, Position from) {
        BigDecimal quantity = into.getQuantity().add(from.getQuantity());
        if (quantity.signum() > 0) {
            BigDecimal cost = into.getTotalCostBasis().add(from.getTotalCostBasis());
            into.setAverageCost(cost.divide(quantity, DECIMAL_PLACES, RoundingMode.HALF_UP));
        }
        into.setQuantity(quantity);
        positionRepository.save(into);
    }

    private void requireUniqueName(String accountId, String name) {
        if (portfolioRepository.existsByAccountIdAndNameIgnoreCase(accountId, name)) {
            throw new PortfolioConflictException("There's already a portfolio called " + name);
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
