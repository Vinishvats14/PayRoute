package com.vinu.payroute.transaction.dto;

import com.vinu.payroute.transaction.entity.Transaction;
import com.vinu.payroute.transaction.entity.TransactionStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record TransactionResponse(
        Long id,
        Long senderWalletId,
        Long receiverWalletId,
        BigDecimal amount,
        String idempotencyKey,
        TransactionStatus status,
        LocalDateTime createdAt
) {

    public static TransactionResponse from(
            Transaction transaction
    ) {

        return new TransactionResponse(
                transaction.getId(),
                transaction.getSenderWalletId(),
                transaction.getReceiverWalletId(),
                transaction.getAmount(),
                transaction.getIdempotencyKey(),
                transaction.getStatus(),
                transaction.getCreatedAt()
        );
    }
}
