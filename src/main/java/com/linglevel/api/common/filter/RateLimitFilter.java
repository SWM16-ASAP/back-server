package com.linglevel.api.common.filter;

import com.linglevel.api.common.config.RateLimitProperties;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.BucketConfiguration;
import io.github.bucket4j.Refill;
import io.github.bucket4j.distributed.proxy.ProxyManager;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String clientIp = getClientIP(httpRequest);
        String bucketKey = "rate_limit:" + clientIp;

        var bucket = proxyManager.builder().build(bucketKey, getBucketConfiguration());

        if (bucket.tryConsume(1)) {
            chain.doFilter(request, response);
        } else {
            log.warn("Rate limit exceeded for IP: {}", clientIp);
            httpResponse.setStatus(429);
            httpResponse.setContentType("application/json");
            httpResponse.getWriter().write("{\"error\":\"Too many requests. Please try again later.\"}");
        }
    }

    private Supplier<BucketConfiguration> getBucketConfiguration() {
        return () -> {
            int capacity = rateLimitProperties.getCapacity();
            long minutes = rateLimitProperties.getRefill().getDuration().getMinutes();
            Duration refillDuration = Duration.ofMinutes(minutes);

            Bandwidth limit = Bandwidth.classic(capacity, Refill.intervally(capacity, refillDuration));
            return BucketConfiguration.builder()
                    .addLimit(limit)
                    .build();
        };
    }

    private String getClientIP(HttpServletRequest request) {
        String xfHeader = request.getHeader("X-Forwarded-For");
        if (xfHeader == null || xfHeader.isEmpty()) {
            return request.getRemoteAddr();
        }
        return xfHeader.split(",")[0].trim();
    }
}
