package com.vinu.payroute.auth.dto;

public record UserProfileResponse(
        Long id,
        String email,
        boolean transactionPinConfigured
) {
}
