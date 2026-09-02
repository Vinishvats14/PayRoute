package com.vinu.payroute.transaction.controller;

import com.vinu.payroute.transaction.dto.TransactionResponse;
import com.vinu.payroute.transaction.dto.TransferRequest;
import com.vinu.payroute.transaction.dto.VerifyTransactionPinRequest;
import com.vinu.payroute.transaction.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/transactions")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping("/transfer")
    public ResponseEntity<TransactionResponse> transfer(
            Authentication authentication,
            @RequestHeader("Idempotency-Key")
            String idempotencyKey,
            @Valid @RequestBody TransferRequest request
    ) {

        Long userId = (Long) authentication.getPrincipal();

        return ResponseEntity.ok(
                transactionService.transfer(
                        userId,
                        request,
                        idempotencyKey
                )
        );
    }

    @PostMapping("/verify-pin")
    public ResponseEntity<TransactionResponse> verifyPin(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody VerifyTransactionPinRequest request
    ) {
        return ResponseEntity.ok(
                transactionService.verifyPendingTransfer(
                        userId,
                        request.idempotencyKey(),
                        request.pin()
                )
        );
    }
}