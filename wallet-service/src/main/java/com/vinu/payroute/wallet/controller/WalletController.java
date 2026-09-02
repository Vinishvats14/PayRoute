package com.vinu.payroute.wallet.controller;


import com.vinu.payroute.wallet.dto.MoneyRequest;
import com.vinu.payroute.wallet.dto.WalletResponse;
import com.vinu.payroute.wallet.service.WalletService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/wallets")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

    @PostMapping
    public ResponseEntity<WalletResponse> createWallet(
            Authentication authentication
    ){
        Long userId = Long.parseLong(
                authentication.getName()
        );
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(walletService.createWallet(userId));
    }

    @GetMapping
    public ResponseEntity<WalletResponse>getWallet(
            Authentication authentication
    ){
        Long userId = Long.parseLong(
                authentication.getName()
        );
        return ResponseEntity
                .status(HttpStatus.OK)
                .body(walletService.getWallet(userId));
    }

    @PostMapping("/deposit")
    public ResponseEntity<WalletResponse> deposit(
            Authentication authentication,
            @Valid @RequestBody MoneyRequest request
    ){
        Long userId = Long.parseLong(
                authentication.getName()
        );
        return ResponseEntity.ok(
                walletService.deposit(
                        userId,
                        request.amount()
                )
        );
    }
    @PostMapping("/withdraw")
    public ResponseEntity<WalletResponse> withdraw(
            Authentication authentication,
            @Valid @RequestBody MoneyRequest request
    ){
        Long userId = Long.parseLong(
                authentication.getName()
        );
        return ResponseEntity.ok(
                walletService.withdraw(
                        userId,
                        request.amount()
                )
        );
    }
}
