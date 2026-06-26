package com.qianyi.core.model.task;

import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Field;

/**
 * 超时、重试、熔断策略
 *
 * @author TianJunQi
 * @since 2026-06-23
 */
@Data
public class Policy {
    @Field("timeout_ms")
    private long timeoutMs;

    @Field("retry_max_attempts")
    private int retryMaxAttempts;

    @Field("retry_backoff_ms")
    private long retryBackoffMs;

    @Field("cb_failure_threshold")
    private int cbFailureThreshold;

    @Field("cb_recovery_ms")
    private long cbRecoveryMs;
}
