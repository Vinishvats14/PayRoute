package com.vinu.payroute.transaction.repository;

import com.vinu.payroute.transaction.entity.Wallet;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface WalletRepository extends JpaRepository<Wallet, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
           SELECT w
           FROM Wallet w
           WHERE w.id = :walletId
           """)
    Optional<Wallet> findByIdForUpdate(
            @Param("walletId") Long walletId
    );

        Optional<Wallet> findByUserId(Long userId);

        @Lock(LockModeType.PESSIMISTIC_WRITE)
        @Query("""
           SELECT w
           FROM Wallet w
           WHERE w.userId = :userId
           """)
        Optional<Wallet> findByUserIdForUpdate(
            @Param("userId") Long userId
        );
}
