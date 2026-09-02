package com.vinu.payroute.wallet.service;

import com.vinu.payroute.wallet.dto.WalletResponse;
import com.vinu.payroute.wallet.entity.Wallet;
import com.vinu.payroute.wallet.entity.WalletStatus;
import com.vinu.payroute.wallet.repository.WalletRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@RequiredArgsConstructor
@Service
public class WalletService {
    private final WalletRepository walletRepository;

    @Transactional
    public WalletResponse createWallet(Long userId){
        if(walletRepository.existsByUserId(userId)){
            throw new IllegalArgumentException("Wallet already exists for user: " + userId);
        }

        Wallet wallet = Wallet.builder()
                .userId(userId)
                .balance(BigDecimal.ZERO)
                .status(WalletStatus.ACTIVE)
                .build();

        return WalletResponse.from(
                walletRepository.save(wallet)
        );
    }

    @Transactional(readOnly = true)
    public WalletResponse getWallet(Long userId){
        Wallet wallet = walletRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Wallet Not Found"
                ));
        return WalletResponse.from(wallet);
    }

    @Transactional
    public WalletResponse deposit(
            Long userId,
            BigDecimal amount
    ) {

        Wallet wallet = walletRepository
                .findByUserIdForUpdate(userId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Wallet not found"
                        ));

        validateWallet(wallet);

        wallet.setBalance(
                wallet.getBalance().add(amount)
        );

        return WalletResponse.from(wallet);
    }
    @Transactional
    public WalletResponse withdraw(
            Long userId,
            BigDecimal amount
    ) {

        Wallet wallet = walletRepository
                .findByUserIdForUpdate(userId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Wallet not found"
                        ));

        validateWallet(wallet);

        if (wallet.getBalance().compareTo(amount) < 0) {
            throw new IllegalArgumentException(
                    "Insufficient balance"
            );
        }

        wallet.setBalance(
                wallet.getBalance().subtract(amount)
        );

        return WalletResponse.from(wallet);
    }

    private Long getWalletId(Long userId){
        return walletRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Wallet not found"
                ))
                .getId();
    }
    private void validateWallet(Wallet wallet){
        if(wallet.getStatus() != WalletStatus.ACTIVE){
            throw new IllegalArgumentException(
                    "Wallet is not active"
            );
        }
    }

}
