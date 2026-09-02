package com.vinu.payroute.transaction.dto;

public record RiskCheckResponse(
        boolean approved,
        String decision,
        String riskLevel,
        int riskScore,
        String reason
) {

    public boolean requiresPinVerification() {
        return "REQUIRE_TRANSACTION_PIN".equalsIgnoreCase(decision);
    }
}
