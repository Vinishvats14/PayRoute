package com.vinu.payroute.risk.service;

import com.vinu.payroute.risk.dto.RiskCheckRequest;
import com.vinu.payroute.risk.dto.RiskCheckResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class RiskService {

    @Value("${risk.max-transaction-amount:200000}")
    private BigDecimal maxTransactionAmount = new BigDecimal("200000");

    @Value("${risk.daily-transaction-limit:500000}")
    private BigDecimal dailyTransactionLimit = new BigDecimal("500000");

    @Value("${risk.amount-spike-multiplier:5}")
    private int amountSpikeMultiplier = 5;

    @Value("${risk.medium-risk-threshold:30}")
    private int mediumRiskThreshold = 30;

    @Value("${risk.high-risk-threshold:60}")
    private int highRiskThreshold = 60;

    @Value("${risk.failed-attempts-threshold:5}")
    private long failedAttemptsThreshold = 5L;

    @Value("${risk.amount-above-50000-points:20}")
    private int amountAbove50000Points = 20;

    @Value("${risk.amount-spike-points:30}")
    private int amountSpikePoints = 30;

    @Value("${risk.velocity-points:30}")
    private int velocityPoints = 30;

    @Value("${risk.failed-attempts-points:25}")
    private int failedAttemptsPoints = 25;

    private final RedisRateLimitService rateLimitService;

    public RiskCheckResponse check(RiskCheckRequest request) {
        RiskCheckResponse limitResult = validateTransferLimits(request);

        if (limitResult != null) {
            return limitResult;
        }

        int score = 0;
        String reason = "Normal transaction";

        if (request.amount().compareTo(new BigDecimal("50000")) > 0) {
            score += amountAbove50000Points;
            reason = "Amount above ₹50,000";
        }

        BigDecimal average = rateLimitService.getAverageTransactionAmount(request.userId());

        if (average.compareTo(BigDecimal.ZERO) > 0
                && request.amount().compareTo(
                average.multiply(BigDecimal.valueOf(amountSpikeMultiplier))) > 0) {
            score += amountSpikePoints;
            reason = "Amount spike detected";
        }

        if (!rateLimitService.isTransferAllowed(request.userId())) {
            score += velocityPoints;
            reason = "Velocity violation detected";
        }

        Long failedAttempts = rateLimitService.getFailedAttemptCount(request.userId());

        if (failedAttempts >= failedAttemptsThreshold) {
            score += failedAttemptsPoints;
            reason = "Repeated failed transactions detected";
        }

        String riskLevel;

        if (score < mediumRiskThreshold) {
            riskLevel = "LOW";
        } else if (score < highRiskThreshold) {
            riskLevel = "MEDIUM";
        } else {
            riskLevel = "HIGH";
        }

        if (riskLevel.equals("LOW")) {
            return RiskCheckResponse.allow(reason, score);
        }

        if (riskLevel.equals("MEDIUM")) {
            return RiskCheckResponse.requirePin(
                    "Medium risk transaction requires transaction PIN verification.",
                    score
            );
        }

        return RiskCheckResponse.block(reason, riskLevel, score);
    }

    public RiskCheckResponse validateTransferCompletion(RiskCheckRequest request) {
        RiskCheckResponse limitResult = validateTransferLimits(request);

        if (limitResult != null) {
            return limitResult;
        }

        return RiskCheckResponse.allow("Transfer can proceed", 0);
    }

    public BigDecimal recordCompletedTransfer(Long userId, BigDecimal amount) {
        if (rateLimitService.isDailyLimitExceeded(userId, amount, dailyTransactionLimit)) {
            throw new IllegalArgumentException("Daily transaction limit exceeded.");
        }

        return rateLimitService.addDailyAmount(userId, amount);
    }

    private RiskCheckResponse validateTransferLimits(RiskCheckRequest request) {
        if (request.senderWalletId().equals(request.receiverWalletId())) {
            return RiskCheckResponse.block("Sender and receiver cannot be same", "HIGH", 100);
        }

        if (request.amount().compareTo(maxTransactionAmount) > 0) {
            return RiskCheckResponse.block(
                    "Transfer amount exceeds hard limit of ₹" + maxTransactionAmount,
                    "HIGH",
                    100
            );
        }

        if (rateLimitService.hasActiveLock(request.userId())) {
            return RiskCheckResponse.block(
                    "Transaction temporarily locked due to suspicious activity.",
                    "HIGH",
                    100
            );
        }

        if (rateLimitService.isDailyLimitExceeded(request.userId(), request.amount(), dailyTransactionLimit)) {
            return RiskCheckResponse.block("Daily transaction limit exceeded.", "HIGH", 100);
        }

        return null;
    }
}