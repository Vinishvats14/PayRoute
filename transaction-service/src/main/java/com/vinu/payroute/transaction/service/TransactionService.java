package com.vinu.payroute.transaction.service;

import com.vinu.payroute.transaction.client.RiskClient;
import com.vinu.payroute.transaction.dto.RiskCheckRequest;
import com.vinu.payroute.transaction.dto.RiskCheckResponse;
import com.vinu.payroute.transaction.dto.TransactionResponse;
import com.vinu.payroute.transaction.dto.TransferRequest;
import com.vinu.payroute.transaction.entity.*;
import com.vinu.payroute.transaction.repository.LedgerEntryRepository;
import com.vinu.payroute.transaction.repository.TransactionRepository;
import com.vinu.payroute.transaction.repository.WalletRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final LedgerEntryRepository ledgerEntryRepository;
    private final WalletRepository walletRepository;
    private final RiskClient riskClient;
    private final WebClient webClient;

    @Value("${auth.service.base-url:http://localhost:8081}")
    private String authServiceBaseUrl;

    @Value("${auth.service.internal-token:change-me}")
    private String authServiceInternalToken;

    @Transactional
    public TransactionResponse transfer(
            Long userId,
            TransferRequest request,
            String idempotencyKey
    ) {
        /*
         * Step 1:
         * Check if this request was already processed.
         */
        var existingTransaction =
                transactionRepository
                        .findByIdempotencyKey(idempotencyKey);

        if (existingTransaction.isPresent()) {

            return TransactionResponse.from(
                    existingTransaction.get()
            );
        }
        Long senderWalletId =
                walletRepository.findByUserIdForUpdate(userId)
                        .map(Wallet::getId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Sender wallet not found for user: " + userId
                                ));
        /*
         * Step 2:
         * Basic validation.
         */
        if (senderWalletId.equals(
                request.receiverWalletId()
        )) {
            throw new IllegalArgumentException(
                    "Cannot transfer to the same wallet"
            );
        }

        java.time.LocalDateTime startOfDay = java.time.LocalDate.now().atStartOfDay();
        java.math.BigDecimal dailySent = transactionRepository.sumDailyTransactionsBySenderWalletId(senderWalletId, startOfDay);
        if (dailySent.add(request.amount()).compareTo(new java.math.BigDecimal("500000")) > 0) {
            throw new IllegalArgumentException("Max total cumulative daily transaction limit of 500000 exceeded");
        }

        RiskCheckResponse riskResult = riskClient.check(
                new RiskCheckRequest(
                        userId,
                        senderWalletId,
                        request.receiverWalletId(),
                        request.amount()
                )
        );

        if (!riskResult.approved()) {
            if (riskResult.requiresPinVerification()) {
                Transaction pendingVerification = Transaction.builder()
                        .senderWalletId(senderWalletId)
                        .receiverWalletId(request.receiverWalletId())
                        .amount(request.amount())
                        .idempotencyKey(idempotencyKey)
                        .status(TransactionStatus.PENDING_VERIFICATION)
                        .build();

                return TransactionResponse.from(
                        transactionRepository.save(pendingVerification)
                );
            }

            throw new IllegalArgumentException(
                    riskResult.reason()
            );
        }

        /*
         * Step 3:
         * Lock wallets.
         */
        Long receiverWalletId = request.receiverWalletId();
        Wallet sender;
        Wallet receiver;

        if (senderWalletId < receiverWalletId) {

            sender = walletRepository
                    .findByIdForUpdate(senderWalletId)
                    .orElseThrow(() ->
                            new IllegalArgumentException(
                                    "Sender wallet not found"
                            ));

            receiver = walletRepository
                    .findByIdForUpdate(receiverWalletId)
                    .orElseThrow(() ->
                            new IllegalArgumentException(
                                    "Receiver wallet not found"
                            ));

        } else {

            receiver = walletRepository
                    .findByIdForUpdate(receiverWalletId)
                    .orElseThrow(() ->
                            new IllegalArgumentException(
                                    "Receiver wallet not found"
                            ));

            sender = walletRepository
                    .findByIdForUpdate(senderWalletId)
                    .orElseThrow(() ->
                            new IllegalArgumentException(
                                    "Sender wallet not found"
                            ));
        }
        /*
         * Step 4:
         * Validate wallet status.
         */
        validateWallet(sender);
        validateWallet(receiver);
        if (sender.getStatus() != WalletStatus.ACTIVE) {
            throw new IllegalArgumentException(
                    "Sender wallet is blocked"
            );
        }

        if (receiver.getStatus() != WalletStatus.ACTIVE) {
            throw new IllegalArgumentException(
                    "Receiver wallet is blocked"
            );
        }
        /*
         * Step 5:
         * Check balance.
         */
        if (sender.getBalance()
                .compareTo(request.amount()) < 0) {

            throw new IllegalArgumentException(
                    "Insufficient balance"
            );
        }

        /*
         * Step 6:
         * Create transaction.
         */
        Transaction transaction =
                Transaction.builder()
                        .senderWalletId(sender.getId())
                        .receiverWalletId(receiver.getId())
                        .amount(request.amount())
                        .idempotencyKey(idempotencyKey)
                        .status(TransactionStatus.PENDING)
                        .build();

        transaction =
                transactionRepository.save(transaction);

        /*
         * Step 7:
         * Move money.
         */
        sender.setBalance(
                sender.getBalance()
                        .subtract(request.amount())
        );

        receiver.setBalance(
                receiver.getBalance()
                        .add(request.amount())
        );

        /*
         * Step 8:
         * Create debit ledger entry.
         */
        LedgerEntry debit =
                LedgerEntry.builder()
                        .transactionId(transaction.getId())
                        .walletId(sender.getId())
                        .type(LedgerEntryType.DEBIT)
                        .amount(request.amount())
                        .build();

        /*
         * Step 9:
         * Create credit ledger entry.
         */
        LedgerEntry credit =
                LedgerEntry.builder()
                        .transactionId(transaction.getId())
                        .walletId(receiver.getId())
                        .type(LedgerEntryType.CREDIT)
                        .amount(request.amount())
                        .build();

        ledgerEntryRepository.save(debit);
        ledgerEntryRepository.save(credit);

        riskClient.recordDailyTransfer(
                sender.getUserId(),
                request.amount()
        );

        /*
         * Step 10:
         * Mark transaction completed.
         */
        transaction.setStatus(
                TransactionStatus.COMPLETED
        );

        transaction =
                transactionRepository.save(transaction);

        return TransactionResponse.from(transaction);
    }

    @Transactional
    public TransactionResponse verifyPendingTransfer(Long userId, String idempotencyKey, String pin) {
        Transaction pending = transactionRepository.findByIdempotencyKey(idempotencyKey)
                .orElseThrow(() -> new IllegalArgumentException("No pending transfer found for this request"));

        if (pending.getStatus() == TransactionStatus.COMPLETED) {
            return TransactionResponse.from(pending);
        }

        if (pending.getStatus() != TransactionStatus.PENDING_VERIFICATION) {
            throw new IllegalArgumentException("This transfer is not waiting for PIN verification");
        }

        Wallet senderWallet = walletRepository.findByUserIdForUpdate(userId)
                .orElseThrow(() -> new IllegalArgumentException("Sender wallet not found for user: " + userId));

        if (!pending.getSenderWalletId().equals(senderWallet.getId())) {
            throw new IllegalArgumentException("This transfer does not belong to the authenticated user");
        }

        boolean pinValid;
        try {
            pinValid = webClient.post()
                    .uri(authServiceBaseUrl + "/auth/internal/transaction-pin/verify")
                    .header("X-Service-Token", authServiceInternalToken)
                    .bodyValue(java.util.Map.of("userId", userId, "pin", pin))
                    .retrieve()
                    .bodyToMono(Boolean.class)
                    .block();
        } catch (Exception ex) {
            throw new IllegalArgumentException("Could not verify transaction PIN", ex);
        }

        if (!Boolean.TRUE.equals(pinValid)) {
            throw new IllegalArgumentException("Invalid transaction PIN");
        }

        RiskCheckResponse completionCheck = riskClient.validateTransferCompletion(
                new RiskCheckRequest(
                        userId,
                        pending.getSenderWalletId(),
                        pending.getReceiverWalletId(),
                        pending.getAmount()
                )
        );

        if (!completionCheck.approved()) {
            throw new IllegalArgumentException(
                    completionCheck.reason()
            );
        }

        pending.setStatus(TransactionStatus.PENDING);
        pending = transactionRepository.save(pending);

        return processTransferForPendingTransaction(pending);
    }

    @Transactional
    protected TransactionResponse processTransferForPendingTransaction(Transaction pending) {
        Long senderWalletId = pending.getSenderWalletId();
        Long receiverWalletId = pending.getReceiverWalletId();
        BigDecimal amount = pending.getAmount();

        Wallet sender = walletRepository.findByIdForUpdate(senderWalletId)
                .orElseThrow(() -> new IllegalArgumentException("Sender wallet not found"));
        Wallet receiver = walletRepository.findByIdForUpdate(receiverWalletId)
                .orElseThrow(() -> new IllegalArgumentException("Receiver wallet not found"));

        validateWallet(sender);
        validateWallet(receiver);

        if (sender.getBalance().compareTo(amount) < 0) {
            throw new IllegalArgumentException("Insufficient balance");
        }

        sender.setBalance(sender.getBalance().subtract(amount));
        receiver.setBalance(receiver.getBalance().add(amount));

        LedgerEntry debit = LedgerEntry.builder()
                .transactionId(pending.getId())
                .walletId(sender.getId())
                .type(LedgerEntryType.DEBIT)
                .amount(amount)
                .build();

        LedgerEntry credit = LedgerEntry.builder()
                .transactionId(pending.getId())
                .walletId(receiver.getId())
                .type(LedgerEntryType.CREDIT)
                .amount(amount)
                .build();

        ledgerEntryRepository.save(debit);
        ledgerEntryRepository.save(credit);

        riskClient.recordDailyTransfer(
                sender.getUserId(),
                amount
        );

        pending.setStatus(TransactionStatus.COMPLETED);
        pending = transactionRepository.save(pending);

        return TransactionResponse.from(pending);
    }

    private void validateWallet(Wallet wallet) {

        if (wallet.getStatus() != WalletStatus.ACTIVE) {

            throw new IllegalArgumentException(
                    "Wallet is not active"
            );
        }
    }
}