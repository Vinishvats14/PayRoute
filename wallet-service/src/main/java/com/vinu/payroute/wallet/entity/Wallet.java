package com.vinu.payroute.wallet.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(
        name = "wallets",
        uniqueConstraints= {
                @UniqueConstraint(
                        name = "uk_wallet_user",
                        columnNames = "user_id"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Wallet {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(
            nullable= false ,
    precision= 19 ,
    scale=2)
    private BigDecimal balance;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false , length=20)
    private WalletStatus status;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt ;

    @Version
    private Long version;
///@PrePersist ka matlab hai: "Database me Record Save (INSERT) hone se JUST PEHLE."
/// Pre= phle ,Persist = Database me Permanently Save karna (INSERT query chalana)
    @PrePersist
    protected void onCreate() {

        createdAt = LocalDateTime.now();

        if (balance == null) {
            balance = BigDecimal.ZERO;
        }

        if (status == null) {
            status = WalletStatus.ACTIVE;
        }
    }
}
///Why not Double for money?
/// Floating-point numbers can introduce precision errors. Financial amounts require exact decimal arithmetic, so we use BigDecimal.

///why version
///Suppose two requests simultaneously try to update the same wallet:
/// Request A ──> balance = ₹1000
/// Request B ──> balance = ₹1000
/// Both read the same version.
///If A updates first:  version 1 → version 2
///B is still working with: version 1. When B tries to update, it will fail because the version has changed. This prevents overwriting A's changes and ensures data integrity.