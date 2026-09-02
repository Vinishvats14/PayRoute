package com.vinu.payroute.risk.dto;

public record RiskCheckResponse(
        boolean approved,
        String decision,
        String riskLevel,
        int riskScore,
        String reason
) {

    public static RiskCheckResponse allow(String reason, int riskScore) {
        return new RiskCheckResponse(true, "ALLOW", "LOW", riskScore, reason);
    }

    public static RiskCheckResponse requirePin(String reason, int riskScore) {
        return new RiskCheckResponse(false, "REQUIRE_TRANSACTION_PIN", "MEDIUM", riskScore, reason);
    }

    public static RiskCheckResponse block(String reason, String riskLevel, int riskScore) {
        return new RiskCheckResponse(false, "BLOCK", riskLevel, riskScore, reason);
    }
}