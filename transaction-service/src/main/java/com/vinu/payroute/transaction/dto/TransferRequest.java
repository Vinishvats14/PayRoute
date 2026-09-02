package com.vinu.payroute.transaction.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record TransferRequest(

        @NotNull(message = "Receiver wallet is required")
        Long receiverWalletId,

        @NotNull(message = "Amount is required")
        @DecimalMin(
                value = "0.01",
                message = "Amount must be greater than zero"
        )
        BigDecimal amount
) {
}
///we don't accept: senderWalletId from the request.
/// Why? Because the authenticated user determines the sender.Otherwise a malicious client could send:
/// {
///   "senderWalletId": 999,
///   "receiverWalletId": 10,
///   "amount": 10000
/// }
/// We should derive sender identity from JWT.