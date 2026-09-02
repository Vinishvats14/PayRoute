package com.vinu.payroute.auth.dto;

public record AuthResponse(
        String accessToken,
        String tokenType
) {
}
