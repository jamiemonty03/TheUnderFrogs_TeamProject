package com.neueda.positionservice.controllers;

import com.neueda.positionservice.models.Position;
import com.neueda.positionservice.services.PositionService;
import com.neueda.positionservice.dtos.responses.PositionResponse;
import org.springframework.web.bind.annotation.*;
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
    public Position createPosition(@RequestBody Position position) {
        System.out.println("DEBUG: POST /api/positions called with position: " + position);
        return positionService.savePosition(position);
    }
    
    @DeleteMapping("/{accountId}/{symbol}")
    public boolean deletePosition(@PathVariable String accountId, @PathVariable String symbol) {
        return positionService.deletePosition(accountId, symbol);
    }
    
}