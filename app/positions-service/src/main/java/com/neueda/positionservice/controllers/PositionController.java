package com.neueda.positionservice.controllers;

import com.neueda.positionservice.models.Position;
import com.neueda.positionservice.services.PositionService;
import com.neueda.positionservice.dtos.requests.BuyRequest;
import com.neueda.positionservice.dtos.requests.CreatePositionRequest;
import com.neueda.positionservice.dtos.requests.ReplacePositionRequest;
import com.neueda.positionservice.dtos.requests.SellRequest;
import com.neueda.positionservice.dtos.requests.UpdatePositionRequest;
import com.neueda.positionservice.exceptions.AccountAuthorizationException;
import com.neueda.positionservice.exceptions.InsufficientHoldingsException;
import com.neueda.positionservice.exceptions.PositionNotFoundException;
import com.neueda.positionservice.dtos.requests.ReversalRequest;
import com.neueda.positionservice.utils.AuthorizationUtils;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/positions")
public class PositionController {

    private final PositionService positionService;

    public PositionController(PositionService positionService) {
        this.positionService = positionService;
    }

    @GetMapping("/{accountId}")
    public List<Position> getPositionsByAccount(@PathVariable String accountId,
            @RequestParam(required = false) String portfolioId) throws AccountAuthorizationException {
        AuthorizationUtils.verifyAccountAccess(accountId);

        return positionService.getPositionsByAccountId(accountId, portfolioId);
    }

    @GetMapping("/{accountId}/{symbol}")
    public Position getPosition(@PathVariable String accountId, @PathVariable String symbol,
            @RequestParam(required = false) String portfolioId) throws AccountAuthorizationException {
        AuthorizationUtils.verifyAccountAccess(accountId);

        return positionService.getPosition(accountId, portfolioId, symbol)
            .orElseThrow(() -> new PositionNotFoundException(accountId, symbol));
    }

    @PostMapping
    public Position createPosition(@RequestBody @Valid CreatePositionRequest request) throws AccountAuthorizationException {
        AuthorizationUtils.verifyAccountAccess(request.accountId());

        return positionService.createPosition(request);
    }

    @DeleteMapping("/{accountId}/{symbol}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePosition(@PathVariable String accountId, @PathVariable String symbol,
            @RequestParam(required = false) String portfolioId) throws AccountAuthorizationException {
        AuthorizationUtils.verifyAccountAccess(accountId);

        positionService.deletePosition(accountId, portfolioId, symbol);
    }

    @PutMapping("/{accountId}/{symbol}")
    public Position updatePosition(@PathVariable String accountId, @PathVariable String symbol,
            @RequestParam(required = false) String portfolioId,
            @RequestBody @Valid ReplacePositionRequest request) throws AccountAuthorizationException {
        AuthorizationUtils.verifyAccountAccess(accountId);

        return positionService.updatePosition(accountId, portfolioId, symbol, request);
    }

    @PatchMapping("/{accountId}/{symbol}")
    public Position partiallyUpdatePosition(@PathVariable String accountId, @PathVariable String symbol,
            @RequestParam(required = false) String portfolioId,
            @RequestBody @Valid UpdatePositionRequest request) throws AccountAuthorizationException {
        AuthorizationUtils.verifyAccountAccess(accountId);

        return positionService.patchPosition(accountId, portfolioId, symbol, request);
    }

    @PostMapping("/{accountId}/{symbol}/buy")
    public Position buyPosition(@PathVariable String accountId, @PathVariable String symbol, @RequestBody @Valid BuyRequest request) throws AccountAuthorizationException {
        AuthorizationUtils.verifyAccountAccess(accountId);

        return positionService.updatePositionAfterBuy(accountId, request.portfolioId(), symbol,
            request.quantity(), request.price(), request.orderId());
    }

    @PostMapping("/{accountId}/{symbol}/sell")
    public Position sellPosition(@PathVariable String accountId, @PathVariable String symbol, @RequestBody @Valid SellRequest request) throws InsufficientHoldingsException, AccountAuthorizationException {
        AuthorizationUtils.verifyAccountAccess(accountId);

        return positionService.updatePositionAfterSell(accountId, request.portfolioId(), symbol,
            request.quantity(), request.orderId());
    }

    @PostMapping("/{accountId}/{symbol}/reversal")
    public Position reversePosition(@PathVariable String accountId, @PathVariable String symbol, @RequestBody @Valid ReversalRequest request) throws InsufficientHoldingsException, AccountAuthorizationException {
        AuthorizationUtils.verifyAccountAccess(accountId);

        return positionService.reverse(accountId, symbol, request.orderId());
    }

}
