package com.linglevel.api.common.ratelimit.filter;

import com.linglevel.api.auth.jwt.JwtClaims;
import com.linglevel.api.common.ratelimit.annotation.RateLimit;
import com.linglevel.api.common.ratelimit.config.RateLimitConfig;
import com.linglevel.api.common.ratelimit.config.RateLimitProperties;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.Refill;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.time.Duration;
import java.util.function.Supplier;

@Slf4j
@Component
@RequiredArgsConstructor
public class RateLimitFilter implements Filter {

    private final ProxyManager<String> proxyManager;
    private final RateLimitProperties rateLimitProperties;
    private final RateLimitResolver rateLimitResolver;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        // 1. Check for @RateLimit annotation first (higher priority)
        RateLimit rateLimitAnnotation = rateLimitResolver.resolveRateLimit(httpRequest);
        RateLimitConfig config;

        if (rateLimitAnnotation != null) {
            // Use annotation-based configuration
            config = RateLimitConfig.fromAnnotation(rateLimitAnnotation);
            log.debug("Using annotation-based rate limit config: capacity={}, refillMinutes={}",
                    config.getCapacity(), config.getRefillMinutes());
        } else {
            // Fallback to global configuration
            config = RateLimitConfig.fromProperties(rateLimitProperties);
            log.debug("Using global rate limit config: capacity={}, refillMinutes={}",
                    config.getCapacity(), config.getRefillMinutes());
        }

        String bucketKey = getBucketKey(httpRequest, config.getKeyType());

        var bucket = proxyManager.builder().build(bucketKey, getBucketConfiguration(config));

        if (bucket.tryConsume(1)) {
            chain.doFilter(request, response);
        } else {
            log.warn("Rate limit exceeded for key: {}", bucketKey);
            httpResponse.setStatus(429);
            httpResponse.setContentType("application/json");
            httpResponse.getWriter().write("{\"error\":\"Too many requests. Please try again later.\"}");
        }
    }

    private Supplier<BucketConfiguration> getBucketConfiguration(RateLimitConfig config) {
        return () -> {
            int capacity = config.getCapacity();
            Duration refillDuration = Duration.ofMinutes(config.getRefillMinutes());

            Bandwidth limit = Bandwidth.classic(capacity, Refill.intervally(capacity, refillDuration));
            return BucketConfiguration.builder()
                    .addLimit(limit)
                    .build();
        };
    }

    /**
     * Generates bucket key based on the key type strategy.
     *
     * @param request HTTP request
     * @param keyType Key type strategy (AUTO, USER, IP)
     * @return Bucket key string
     */
    private String getBucketKey(HttpServletRequest request, RateLimit.KeyType keyType) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean isAuthenticated = authentication != null
                && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof JwtClaims;

        switch (keyType) {
            case USER:
                // Force user-based key (requires authentication)
                if (!isAuthenticated) {
                    log.warn("USER key type requested but user is not authenticated, falling back to IP");
                    return "rate_limit:ip:" + getClientIP(request);
                }
                JwtClaims userClaims = (JwtClaims) authentication.getPrincipal();
                return "rate_limit:user:" + userClaims.getId();

            case IP:
                // Force IP-based key
                return "rate_limit:ip:" + getClientIP(request);

            case AUTO:
            default:
                // Auto: use user ID if authenticated, otherwise use IP
                if (isAuthenticated) {
                    JwtClaims claims = (JwtClaims) authentication.getPrincipal();
                    return "rate_limit:user:" + claims.getId();
                }
                return "rate_limit:ip:" + getClientIP(request);
        }
    }

    private String getClientIP(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null || xfHeader.isEmpty()) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0].trim();
    }
}
