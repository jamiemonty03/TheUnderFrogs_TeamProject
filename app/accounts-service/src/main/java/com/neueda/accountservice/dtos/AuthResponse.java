package com.neueda.accountservice.dtos;

public record AuthResponse(
    String token,
    String username,
    String email,
    String fullName
) {}
