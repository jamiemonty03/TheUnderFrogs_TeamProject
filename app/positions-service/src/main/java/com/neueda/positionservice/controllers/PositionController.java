package com.neueda.positionservice.controllers;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;

@RestController
@RequestMapping("/positions")
    public class PositionController {
    @GetMapping
    public String getPositions() {
        return "List of positions";
    }
}
