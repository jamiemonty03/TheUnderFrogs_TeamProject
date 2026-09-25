package com.neueda.accountservice.controllers;

import com.neueda.accountservice.dtos.AuthResponse;
import com.neueda.accountservice.dtos.LoginRequest;
import com.neueda.accountservice.dtos.RegisterRequest;
import com.neueda.accountservice.models.User;
import com.neueda.accountservice.services.UserService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    @Operation(summary = "Register a new user", security = {})
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        User user = userService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(toAuthResponse(user));
    }

    @PostMapping("/login")
    @Operation(summary = "Login with username and password", security = {})
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        User user = userService.login(request);
        return ResponseEntity.ok(toAuthResponse(user));
    }

    private AuthResponse toAuthResponse(User user) {
        return new AuthResponse(user.getToken(), user.getUsername(), user.getEmail(), user.getFullName());
    }
}
