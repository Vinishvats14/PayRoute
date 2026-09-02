package com.vinu.payroute.transaction.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "transactions",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_transaction_idempotency",
                        columnNames = "idempotency_key"
                )
        },
        indexes = {
                @Index(
                        name = "idx_transaction_sender",
                        columnList = "sender_wallet_id"
                ),
                @Index(
                        name = "idx_transaction_receiver",
                        columnList = "receiver_wallet_id"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(
            name = "sender_wallet_id",
            nullable = false
    )
    private Long senderWalletId;

    @Column(
            name = "receiver_wallet_id",
            nullable = false
    )
    private Long receiverWalletId;

    @Column(
            nullable = false,
            precision = 19,
            scale = 2
    )
    private BigDecimal amount;

    @Column(
            name = "idempotency_key",
            nullable = false,
            unique = true,
            length = 100
    )
    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TransactionStatus status;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {

        LocalDateTime now = LocalDateTime.now();

        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
///Why are wallet IDs stored here? sender_wallet_id
/// receiver_wallet_id , instead of creating a JPA relationship such as:
///@ManyToOne
/// private Wallet wallet;That's intentional.
/// Wallet belongs to Wallet Service.

/// Transaction belongs to Transaction Service.
/// We don't want:
///
/// Transaction Service
///        |
///        └── JPA relationship
///                 |
///                 v
///        Wallet Service DB
///
/// That's a bad microservice boundary.
///
/// Instead Transaction Service stores the identifiers:
///
/// senderWalletId = 10
/// receiverWalletId = 25
///and communicates with Wallet Service when it needs wallet information/mutation.