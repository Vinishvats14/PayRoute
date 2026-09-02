package com.vinu.payroute.risk.service;

import com.vinu.payroute.risk.dto.RiskCheckRequest;
import com.vinu.payroute.risk.dto.RiskCheckResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RiskServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    private RedisRateLimitService redisRateLimitService;

    private RiskService riskService;

    @BeforeEach
    void setUp() {
        redisRateLimitService = org.mockito.Mockito.spy(new RedisRateLimitService(redisTemplate));
        riskService = new RiskService(redisRateLimitService);
    }

    @Test
    void shouldRejectWhenAmountExceedsHardLimit() {
        RiskCheckRequest request = new RiskCheckRequest(1L, 10L, 20L, new BigDecimal("140000"));

        RiskCheckResponse response = riskService.check(request);

        assertFalse(response.approved());
        assertEquals("BLOCK", response.decision());
        assertTrue(response.reason().contains("hard limit"));
    }

    @Test
    void shouldAllowTransferCompletionWhenLimitsAreOk() {
        doReturn(false).when(redisRateLimitService).isDailyLimitExceeded(anyLong(), any(BigDecimal.class), any(BigDecimal.class));
        doReturn(false).when(redisRateLimitService).hasActiveLock(anyLong());

        RiskCheckRequest request = new RiskCheckRequest(1L, 10L, 20L, new BigDecimal("10000"));

        RiskCheckResponse response = riskService.validateTransferCompletion(request);

        assertTrue(response.approved());
        assertEquals("ALLOW", response.decision());
        assertEquals("LOW", response.riskLevel());
    }

    @Test
    void shouldRequirePinForTransfersAboveTwoLakhs() {
        doReturn(false).when(redisRateLimitService).isDailyLimitExceeded(anyLong(), any(BigDecimal.class), any(BigDecimal.class));
        doReturn(true).when(redisRateLimitService).isTransferAllowed(anyLong());
        doReturn(false).when(redisRateLimitService).hasActiveLock(anyLong());
        doReturn(new BigDecimal("5000")).when(redisRateLimitService).getAverageTransactionAmount(anyLong());
        doReturn(0L).when(redisRateLimitService).getFailedAttemptCount(anyLong());

        RiskCheckRequest request = new RiskCheckRequest(1L, 10L, 20L, new BigDecimal("200001"));

        RiskCheckResponse response = riskService.check(request);

        assertFalse(response.approved());
        assertEquals("REQUIRE_TRANSACTION_PIN", response.decision());
        assertEquals("MEDIUM", response.riskLevel());
    }

    @Test
    void shouldBlockWhenRiskScoreIsHigh() {
        doReturn(false).when(redisRateLimitService).isDailyLimitExceeded(anyLong(), any(BigDecimal.class), any(BigDecimal.class));
        doReturn(false).when(redisRateLimitService).isTransferAllowed(anyLong());
        doReturn(false).when(redisRateLimitService).hasActiveLock(anyLong());
        doReturn(new BigDecimal("5000")).when(redisRateLimitService).getAverageTransactionAmount(anyLong());
        doReturn(6L).when(redisRateLimitService).getFailedAttemptCount(anyLong());

        RiskCheckRequest request = new RiskCheckRequest(1L, 10L, 20L, new BigDecimal("140000"));

        RiskCheckResponse response = riskService.check(request);

        assertFalse(response.approved());
        assertEquals("BLOCK", response.decision());
        assertEquals("HIGH", response.riskLevel());
    }

    @Test
    void shouldAllowWhenRiskScoreIsLow() {
        doReturn(false).when(redisRateLimitService).isDailyLimitExceeded(anyLong(), any(BigDecimal.class), any(BigDecimal.class));
        doReturn(true).when(redisRateLimitService).isTransferAllowed(anyLong());
        doReturn(false).when(redisRateLimitService).hasActiveLock(anyLong());
        doReturn(new BigDecimal("5000")).when(redisRateLimitService).getAverageTransactionAmount(anyLong());
        doReturn(0L).when(redisRateLimitService).getFailedAttemptCount(anyLong());

        RiskCheckRequest request = new RiskCheckRequest(1L, 10L, 20L, new BigDecimal("20000"));

        RiskCheckResponse response = riskService.check(request);

        assertTrue(response.approved());
        assertEquals("ALLOW", response.decision());
        assertEquals("LOW", response.riskLevel());
    }

    @Test
    void shouldRecordCompletedTransferWhenWithinDailyLimit() {
        doReturn(false).when(redisRateLimitService).isDailyLimitExceeded(1L, new BigDecimal("10000"), new BigDecimal("500000"));
        doReturn(new BigDecimal("10000")).when(redisRateLimitService).addDailyAmount(1L, new BigDecimal("10000"));

        BigDecimal updated = riskService.recordCompletedTransfer(1L, new BigDecimal("10000"));

        assertEquals(new BigDecimal("10000"), updated);
        verify(redisRateLimitService).addDailyAmount(1L, new BigDecimal("10000"));
    }
}
