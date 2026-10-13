package com.neueda.positionservice.controllers;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import com.neueda.positionservice.dtos.requests.CreatePortfolioRequest;
import com.neueda.positionservice.dtos.requests.UpdatePortfolioRequest;
import com.neueda.positionservice.exceptions.AccountAuthorizationException;
import com.neueda.positionservice.models.Portfolio;
import com.neueda.positionservice.services.PortfolioService;
import com.neueda.positionservice.utils.AuthorizationUtils;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/portfolios")
public class PortfolioController {

    private final PortfolioService portfolioService;

    public PortfolioController(PortfolioService portfolioService) {
        this.portfolioService = portfolioService;
    }

    @GetMapping("/{accountId}")
    public List<Portfolio> getPortfolios(@PathVariable String accountId) throws AccountAuthorizationException {
        AuthorizationUtils.verifyAccountAccess(accountId);

        return portfolioService.getPortfolios(accountId);
    }

    @GetMapping("/{accountId}/{portfolioId}")
    public Portfolio getPortfolio(@PathVariable String accountId, @PathVariable String portfolioId)
            throws AccountAuthorizationException {
        AuthorizationUtils.verifyAccountAccess(accountId);

        return portfolioService.getPortfolio(accountId, portfolioId);
    }

    @PostMapping("/{accountId}")
    @ResponseStatus(HttpStatus.CREATED)
    public Portfolio createPortfolio(@PathVariable String accountId, @RequestBody @Valid CreatePortfolioRequest request)
            throws AccountAuthorizationException {
        AuthorizationUtils.verifyAccountAccess(accountId);

        return portfolioService.createPortfolio(accountId, request);
    }

    @PatchMapping("/{accountId}/{portfolioId}")
    public Portfolio updatePortfolio(@PathVariable String accountId, @PathVariable String portfolioId,
            @RequestBody @Valid UpdatePortfolioRequest request) throws AccountAuthorizationException {
        AuthorizationUtils.verifyAccountAccess(accountId);

        return portfolioService.updatePortfolio(accountId, portfolioId, request);
    }

    @DeleteMapping("/{accountId}/{portfolioId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePortfolio(@PathVariable String accountId, @PathVariable String portfolioId)
            throws AccountAuthorizationException {
        AuthorizationUtils.verifyAccountAccess(accountId);

        portfolioService.deletePortfolio(accountId, portfolioId);
    }
}
