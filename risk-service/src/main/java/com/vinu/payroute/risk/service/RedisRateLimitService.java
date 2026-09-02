package com.vinu.payroute.risk.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@Service
@RequiredArgsConstructor
public class RedisRateLimitService {

    private static final int MAX_TRANSFER_PER_MINUTE = 5;

    private static final Duration VELOCITY_WINDOW =
            Duration.ofMinutes(1);

    private static final Duration FAILED_WINDOW =
            Duration.ofMinutes(2);

    private static final Duration LOCK_WINDOW =
            Duration.ofMinutes(5);

    private final StringRedisTemplate redisTemplate;


    // =========================================================
    // VELOCITY
    // =========================================================

    public boolean isTransferAllowed(Long userId) {

        String key =
                "risk:transfer:user:" + userId + ":1min";

        Long count =
                redisTemplate.opsForValue().increment(key);

        if (count == null) {
            return false;
        }

        if (count == 1) {
            redisTemplate.expire(
                    key,
                    VELOCITY_WINDOW
            );
        }

        return count <= MAX_TRANSFER_PER_MINUTE;
    }


    public Long getCurrentVelocityCount(Long userId) {

        String key =
                "risk:transfer:user:" + userId + ":1min";

        String value =
                redisTemplate.opsForValue().get(key);

        return value == null
                ? 0L
                : Long.parseLong(value);
    }


    // =========================================================
    // DAILY TRANSACTION LIMIT
    // =========================================================

    public BigDecimal getDailyAmount(Long userId) {

        String key =
                "risk:daily:user:" + userId;

        String value =
                redisTemplate.opsForValue().get(key);

        return value == null
                ? BigDecimal.ZERO
                : new BigDecimal(value);
    }


    public boolean isDailyLimitExceeded(
            Long userId,
            BigDecimal amount,
            BigDecimal dailyLimit
    ) {

        BigDecimal current =
                getDailyAmount(userId);

        return current
                .add(amount)
                .compareTo(dailyLimit) > 0;
    }


    /**
     * Call this ONLY after a transaction
     * has been successfully completed.
     */
    public BigDecimal addDailyAmount(
            Long userId,
            BigDecimal amount
    ) {

        String key =
                "risk:daily:user:" + userId;

        BigDecimal current =
                getDailyAmount(userId);

        BigDecimal updated =
                current.add(amount);

        redisTemplate.opsForValue().set(
                key,
                updated.toPlainString(),
                getTimeUntilMidnight()
        );

        return updated;
    }


    private Duration getTimeUntilMidnight() {

        ZonedDateTime now =
                ZonedDateTime.now(
                        ZoneId.systemDefault()
                );

        ZonedDateTime midnight =
                now.toLocalDate()
                        .plusDays(1)
                        .atStartOfDay(now.getZone());

        return Duration.between(
                now,
                midnight
        );
    }


    // =========================================================
    // FAILED ATTEMPTS
    // =========================================================

    public Long incrementFailedAttempts(Long userId) {

        String key =
                "risk:failed:user:" + userId + ":2min";

        Long attempts =
                redisTemplate.opsForValue().increment(key);

        if (attempts != null && attempts == 1L) {

            redisTemplate.expire(
                    key,
                    FAILED_WINDOW
            );
        }

        return attempts == null
                ? 0L
                : attempts;
    }


    public Long getFailedAttemptCount(Long userId) {

        String key =
                "risk:failed:user:" + userId + ":2min";

        String value =
                redisTemplate.opsForValue().get(key);

        return value == null
                ? 0L
                : Long.parseLong(value);
    }


    // =========================================================
    // TEMPORARY LOCK
    // =========================================================

    public boolean hasActiveLock(Long userId) {

        String key =
                "risk:lock:user:" + userId;

        return Boolean.TRUE.equals(
                redisTemplate.hasKey(key)
        );
    }


    public void setActiveLock(Long userId) {

        String key =
                "risk:lock:user:" + userId;

        redisTemplate.opsForValue().set(
                key,
                "LOCKED",
                LOCK_WINDOW
        );
    }


    public void clearLock(Long userId) {

        redisTemplate.delete(
                "risk:lock:user:" + userId
        );
    }


    // =========================================================
    // AVERAGE TRANSACTION AMOUNT
    // =========================================================

    public BigDecimal getAverageTransactionAmount(
            Long userId
    ) {

        String key =
                "risk:avg:user:" + userId;

        String value =
                redisTemplate.opsForValue().get(key);

        return value == null
                ? BigDecimal.ZERO
                : new BigDecimal(value);
    }


    public void setAverageTransactionAmount(
            Long userId,
            BigDecimal average
    ) {

        redisTemplate.opsForValue().set(
                "risk:avg:user:" + userId,
                average.toPlainString()
        );
    }


    // =========================================================
    // RESET - DEVELOPMENT / TESTING
    // =========================================================

    public void reset(Long userId) {

        redisTemplate.delete(
                "risk:transfer:user:" + userId + ":1min"
        );

        redisTemplate.delete(
                "risk:daily:user:" + userId
        );

        redisTemplate.delete(
                "risk:failed:user:" + userId + ":2min"
        );

        redisTemplate.delete(
                "risk:lock:user:" + userId
        );

        redisTemplate.delete(
                "risk:avg:user:" + userId
        );
    }
}