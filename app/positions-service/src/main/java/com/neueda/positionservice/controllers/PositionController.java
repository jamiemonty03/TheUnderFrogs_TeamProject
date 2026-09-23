package com.neueda.positionservice.controllers;

import com.neueda.positionservice.models.Position;
import com.neueda.positionservice.services.PositionService;
import com.neueda.positionservice.dtos.responses.PositionResponse;
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
            .orElseThrow(() -> new RuntimeException("Position not found"));
    }
    
    @PostMapping
    public Position createPosition(@Valid @RequestBody Position position) {
        return positionService.savePosition(position);
    }
    
    @DeleteMapping("/{accountId}/{symbol}")
    public boolean deletePosition(@PathVariable String accountId, @PathVariable String symbol) {
        return positionService.deletePosition(accountId, symbol);
    }

    @PutMapping("/{accountId}/{symbol}")
    public Position updatePosition(@PathVariable String accountId, @PathVariable String symbol, @Valid @RequestBody Position position) {
        return positionService.updatePosition(accountId, symbol, position);
    }

    @PatchMapping("/{accountId}/{symbol}")
    public Position partiallyUpdatePosition(@PathVariable String accountId, @PathVariable String symbol, @Valid @RequestBody Position position) {
        return positionService.updatePosition(accountId, symbol, position);
    }
}