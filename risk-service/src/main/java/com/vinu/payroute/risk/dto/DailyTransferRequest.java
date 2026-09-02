package com.vinu.payroute.risk.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record DailyTransferRequest(
        @NotNull Long userId,
        @NotNull BigDecimal amount
) {
}