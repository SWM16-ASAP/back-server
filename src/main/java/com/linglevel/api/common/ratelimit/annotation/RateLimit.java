package com.linglevel.api.common.ratelimit.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Rate limiting annotation for API endpoints.
 * When applied to a controller method, it overrides the global rate limit configuration.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {

    /**
     * Maximum number of requests allowed within the refill period.
     */
    int capacity();

    /**
     * Refill period in minutes.
     */
    long refillMinutes();

    /**
     * Key type for rate limiting.
     */
    KeyType keyType() default KeyType.AUTO;

    /**
     * Key types for rate limiting bucket identification.
     */
    enum KeyType {
        /**
         * Automatically determines key type based on authentication:
         * - Authenticated users: user ID
         * - Unauthenticated users: IP address
         */
        AUTO,

        /**
         * Always use user ID (requires authentication).
         */
        USER,

        /**
         * Always use IP address.
         */
        IP
    }
}
