
package com.neueda.orderservice.controllers;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;

@RestController
@RequestMapping("/orders")
    public class OrderController {
    @GetMapping
    public String getOrders() {
        return "List of orders";
    }
}
