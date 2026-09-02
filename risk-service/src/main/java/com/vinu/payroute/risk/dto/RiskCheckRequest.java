package com.vinu.payroute.risk.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record RiskCheckRequest(

        @NotNull
        Long userId,

        @NotNull
        Long senderWalletId,

        @NotNull
        Long receiverWalletId,

        @NotNull
        @DecimalMin("0.01")
        BigDecimal amount
) {
}