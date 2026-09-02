package com.vinu.payroute.transaction.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record VerifyTransactionPinRequest(
        @NotBlank(message = "Idempotency key is required")
        String idempotencyKey,

        @NotBlank(message = "PIN is required")
        @Pattern(regexp = "\\d{4,6}", message = "PIN must be 4 to 6 digits")
        String pin
) {
}
