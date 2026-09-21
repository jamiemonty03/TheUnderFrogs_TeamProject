package com.neueda.accountservice.controllers;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.GetMapping;

@RestController
@RequestMapping("/accounts")
    public class AccountController {
    @GetMapping
    public String getAccounts() {
        return "List of accounts";
    }
}
