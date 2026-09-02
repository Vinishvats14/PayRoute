package com.vinu.payroute.wallet.dto;

import com.vinu.payroute.wallet.entity.Wallet;
import com.vinu.payroute.wallet.entity.WalletStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record WalletResponse(
        Long id ,
        Long userId,
        BigDecimal balance,
        WalletStatus status,
        LocalDateTime createdAt
) {
    public static WalletResponse from(Wallet wallet) {

        return new WalletResponse(
                wallet.getId(),
                wallet.getUserId(),
                wallet.getBalance(),
                wallet.getStatus(),
                wallet.getCreatedAt()
        );
    }
}
