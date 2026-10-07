package com.neueda.positionservice.controllers;

import com.neueda.positionservice.models.Position;
import com.neueda.positionservice.services.PositionService;
import com.neueda.positionservice.dtos.requests.BuyRequest;
import com.neueda.positionservice.dtos.requests.CreatePositionRequest;
import com.neueda.positionservice.dtos.requests.ReplacePositionRequest;
import com.neueda.positionservice.dtos.requests.SellRequest;
import com.neueda.positionservice.dtos.requests.UpdatePositionRequest;
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
    public List<Position> getPositionsByAccount(@PathVariable String accountId) {
        AuthorizationUtils.verifyAccountAccess(accountId);
        
        return positionService.getPositionsByAccountId(accountId);
    }
    
    @GetMapping("/{accountId}/{symbol}")
    public Position getPosition(@PathVariable String accountId, @PathVariable String symbol) {
        AuthorizationUtils.verifyAccountAccess(accountId);
        
        return positionService.getPosition(accountId, symbol)
            .orElseThrow(() -> new PositionNotFoundException(accountId, symbol));
    }
    
    @PostMapping
    public Position createPosition(@RequestBody @Valid CreatePositionRequest request) {
        return positionService.createPosition(request);
    }
    
    @DeleteMapping("/{accountId}/{symbol}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePosition(@PathVariable String accountId, @PathVariable String symbol) {
        AuthorizationUtils.verifyAccountAccess(accountId);
        
        positionService.deletePosition(accountId, symbol);
    }

    @PutMapping("/{accountId}/{symbol}")
    public Position updatePosition(@PathVariable String accountId, @PathVariable String symbol, @RequestBody @Valid ReplacePositionRequest request) {
        AuthorizationUtils.verifyAccountAccess(accountId);
        
        return positionService.updatePosition(accountId, symbol, request);
    }

    @PatchMapping("/{accountId}/{symbol}")
    public Position partiallyUpdatePosition(@PathVariable String accountId, @PathVariable String symbol, @RequestBody @Valid UpdatePositionRequest request) {
        AuthorizationUtils.verifyAccountAccess(accountId);
        
        return positionService.patchPosition(accountId, symbol, request);
    }

    @PostMapping("/{accountId}/{symbol}/buy")
    public Position buyPosition(@PathVariable String accountId, @PathVariable String symbol, @RequestBody @Valid BuyRequest request) {
        AuthorizationUtils.verifyAccountAccess(accountId);
        
        return request.orderId() == null
            ? positionService.updatePositionAfterBuy(accountId, symbol, request.quantity(), request.price())
            : positionService.updatePositionAfterBuy(accountId, symbol, request.quantity(), request.price(), request.orderId());
    }

    @PostMapping("/{accountId}/{symbol}/sell")
    public Position sellPosition(@PathVariable String accountId, @PathVariable String symbol, @RequestBody @Valid SellRequest request) throws InsufficientHoldingsException {
        AuthorizationUtils.verifyAccountAccess(accountId);
        
        return request.orderId() == null
            ? positionService.updatePositionAfterSell(accountId, symbol, request.quantity())
            : positionService.updatePositionAfterSell(accountId, symbol, request.quantity(), request.orderId());
    }

    @PostMapping("/{accountId}/{symbol}/reversal")
    public Position reversePosition(@PathVariable String accountId, @PathVariable String symbol, @RequestBody @Valid ReversalRequest request) throws InsufficientHoldingsException {
        AuthorizationUtils.verifyAccountAccess(accountId);
        
        return positionService.reverse(accountId, symbol, request.orderId());
    }

}