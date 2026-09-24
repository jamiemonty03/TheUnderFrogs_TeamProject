package com.neueda.positionservice.controllers;

import com.neueda.positionservice.models.Position;
import com.neueda.positionservice.services.PositionService;
import com.neueda.positionservice.dtos.requests.BuyRequest;
import com.neueda.positionservice.dtos.requests.SellRequest;
import com.neueda.positionservice.dtos.requests.UpdatePositionRequest;
import com.neueda.positionservice.exceptions.InsufficientHoldingsException;
import com.neueda.positionservice.exceptions.PositionNotFoundException;
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
        return positionService.getPositionsByAccountId(accountId);
    }
    
    @GetMapping("/{accountId}/{symbol}")
    public Position getPosition(@PathVariable String accountId, @PathVariable String symbol) {
        return positionService.getPosition(accountId, symbol)
            .orElseThrow(() -> new PositionNotFoundException(accountId, symbol));
    }
    
    @PostMapping
    public Position createPosition(@RequestBody @Valid Position position) {
        return positionService.savePosition(position);
    }
    
    @DeleteMapping("/{accountId}/{symbol}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deletePosition(@PathVariable String accountId, @PathVariable String symbol) {
        positionService.deletePosition(accountId, symbol);
    }

    @PutMapping("/{accountId}/{symbol}")
    public Position updatePosition(@PathVariable String accountId, @PathVariable String symbol, @RequestBody @Valid Position position) {
        return positionService.updatePosition(accountId, symbol, position);
    }

    @PatchMapping("/{accountId}/{symbol}")
    public Position partiallyUpdatePosition(@PathVariable String accountId, @PathVariable String symbol, @RequestBody UpdatePositionRequest request) {
        return positionService.patchPosition(accountId, symbol, request);
    }

    @PostMapping("/{accountId}/{symbol}/buy")
    public Position buyPosition(@PathVariable String accountId, @PathVariable String symbol, @RequestBody @Valid BuyRequest request) {
        return positionService.updatePositionAfterBuy(accountId, symbol, request.quantity(), request.price());
    }

    @PostMapping("/{accountId}/{symbol}/sell")
    public Position sellPosition(@PathVariable String accountId, @PathVariable String symbol, @RequestBody @Valid SellRequest request) throws InsufficientHoldingsException {
        return positionService.updatePositionAfterSell(accountId, symbol, request.quantity());
    }
}