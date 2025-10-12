package com.linglevel.api.common.ratelimit.config;

import com.linglevel.api.common.ratelimit.annotation.RateLimit;
import lombok.Builder;
import lombok.Getter;

/**
 * Rate limit configuration DTO.
 * Used to represent rate limit settings from either annotation or global properties.
 */
@Getter
@Builder
public class RateLimitConfig {

    private final int capacity;
    private final long refillMinutes;
    private final RateLimit.KeyType keyType;

    /**
     * Creates a configuration from the global properties.
     */
    public static RateLimitConfig fromProperties(RateLimitProperties properties) {
        return RateLimitConfig.builder()
                .capacity(properties.getCapacity())
                .refillMinutes(properties.getRefill().getDuration().getMinutes())
                .keyType(RateLimit.KeyType.AUTO)
                .build();
    }

    /**
     * Creates a configuration from the annotation.
     */
    public static RateLimitConfig fromAnnotation(RateLimit annotation) {
        return RateLimitConfig.builder()
                .capacity(annotation.capacity())
                .refillMinutes(annotation.refillMinutes())
                .keyType(annotation.keyType())
                .build();
    }
}
