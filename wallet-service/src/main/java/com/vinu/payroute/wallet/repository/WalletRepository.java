package com.vinu.payroute.wallet.repository;

import com.vinu.payroute.wallet.entity.Wallet;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface WalletRepository extends JpaRepository<Wallet,Long> {

    Optional<Wallet> findByUserId(Long userId);
    boolean existsByUserId(Long userId);
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
