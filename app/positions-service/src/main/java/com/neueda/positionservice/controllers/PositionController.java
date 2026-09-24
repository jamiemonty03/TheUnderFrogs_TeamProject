package com.neueda.positionservice.controllers;

import com.neueda.positionservice.models.Position;
import com.neueda.positionservice.services.PositionService;
import com.neueda.positionservice.dtos.responses.PositionResponse;
import com.neueda.positionservice.dtos.requests.BuyRequest;
import com.neueda.positionservice.dtos.requests.SellRequest;
import com.neueda.positionservice.dtos.requests.UpdatePositionRequest;
import com.neueda.positionservice.exceptions.InsufficientHoldingsException;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;
import java.util.List;
import java.math.BigDecimal;

@RestController
@RequestMapping("/positions")
public class PositionController {
    
    private final PositionService positionService;
    
    public PositionController(PositionService positionService) {
        this.positionService = positionService;
    }
    
    @GetMapping("/{accountId}")
    public List<Position> getPositionsByAccount(@PathVariable String accountId) {
        return positionService.getPositionsByAccountId(accountId);
    }
    
    @GetMapping("/{accountId}/{symbol}")
    public Position getPosition(@PathVariable String accountId, @PathVariable String symbol) {
        return positionService.getPosition(accountId, symbol)
            .orElseThrow(() -> new RuntimeException("Position not found"));
    }
    
    @PostMapping
    public Position createPosition(@RequestBody @Valid Position position) {
        return positionService.savePosition(position);
    }
    
    @DeleteMapping("/{accountId}/{symbol}")
    public boolean deletePosition(@PathVariable String accountId, @PathVariable String symbol) {
        return positionService.deletePosition(accountId, symbol);
    }

    @PutMapping("/{accountId}/{symbol}")
    public Position updatePosition(@PathVariable String accountId, @PathVariable String symbol, @RequestBody @Valid Position position) {
        return positionService.updatePosition(accountId, symbol, position);
    }

    @PatchMapping("/{accountId}/{symbol}")
    public Position partiallyUpdatePosition(@PathVariable String accountId, @PathVariable String symbol, @RequestBody UpdatePositionRequest request) {
        Position position = new Position(accountId, symbol, BigDecimal.ZERO, BigDecimal.ZERO);
        
        if (request.quantity() != null) {
            position.setQuantity(request.quantity());
        }
        if (request.averageCost() != null) {
            position.setAverageCost(request.averageCost());
        }
        if (request.updatedBy() != null) {
            position.setUpdatedBy(request.updatedBy());
        }
        
        return positionService.updatePosition(accountId, symbol, position);
    }

    @PostMapping("/{accountId}/{symbol}/buy")
    public Position buyPosition(@PathVariable String accountId, @PathVariable String symbol, @RequestBody @Valid BuyRequest request) {
        positionService.updatePositionAfterBuy(accountId, symbol, request.quantity(), request.price());
        return positionService.getPosition(accountId, symbol).orElseThrow(() -> new RuntimeException("Position not found"));
    }

    @PostMapping("/{accountId}/{symbol}/sell")
    public Position sellPosition(@PathVariable String accountId, @PathVariable String symbol, @RequestBody @Valid SellRequest request) throws InsufficientHoldingsException {
        positionService.updatePositionAfterSell(accountId, symbol, request.quantity());
        return positionService.getPosition(accountId, symbol).orElseThrow(() -> new RuntimeException("Position not found"));
    }
}