package com.vinu.payroute.transaction.repository;

import com.vinu.payroute.transaction.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<Transaction,Long> {
    Optional<Transaction> findByIdempotencyKey(
            String idempotencyKey
    );

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.senderWalletId = :senderWalletId AND t.status IN ('COMPLETED', 'PENDING', 'PENDING_VERIFICATION') AND t.createdAt >= :startOfDay")
    BigDecimal sumDailyTransactionsBySenderWalletId(@Param("senderWalletId") Long senderWalletId, @Param("startOfDay") LocalDateTime startOfDay);
}
