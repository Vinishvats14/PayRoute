package com.vinu.payroute.transaction.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "ledger_entries",
        indexes = {
                @Index(
                        name = "idx_ledger_transaction",
                        columnList = "transaction_id"
                ),
                @Index(
                        name = "idx_ledger_wallet",
                        columnList = "wallet_id"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LedgerEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
            name = "transaction_id",
            nullable = false
    )
    private Long transactionId;

    @Column(
            name = "wallet_id",
            nullable = false
    )
    private Long walletId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private LedgerEntryType type;

    @Column(
            nullable = false,
            precision = 19,
            scale = 2
    )
    private BigDecimal amount;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
///Now our transfer creates:
///
/// Transaction #1001
///
/// ledger_entries
///
/// transaction_id | wallet | type    | amount
/// ------------------------------------------------
/// 1001           | 10     | DEBIT   | 1000
/// 1001           | 25     | CREDIT  | 1000
///
/// This is the core of PayRoute.