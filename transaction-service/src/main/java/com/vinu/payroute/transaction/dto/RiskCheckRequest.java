package com.vinu.payroute.transaction.dto;


import java.math.BigDecimal;

public record RiskCheckRequest(
        Long userId,
        Long senderWalletId,
        Long receiverWalletId,
        BigDecimal amount
) {

}
