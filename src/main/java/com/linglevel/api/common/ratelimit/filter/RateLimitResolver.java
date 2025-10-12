package com.linglevel.api.common.ratelimit.filter;

import com.linglevel.api.common.ratelimit.annotation.RateLimit;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

/**
 * Resolves rate limit configuration from controller method annotations.
 */
@Slf4j
@Component
public class RateLimitResolver {

    private final RequestMappingHandlerMapping handlerMapping;

    public RateLimitResolver(RequestMappingHandlerMapping handlerMapping) {
        this.handlerMapping = handlerMapping;
    }

    /**
     * Extracts @RateLimit annotation from the handler method if present.
     *
     * @param request HTTP request
     * @return RateLimit annotation if found, null otherwise
     */
    public RateLimit resolveRateLimit(HttpServletRequest request) {
        try {
            HandlerExecutionChain handlerChain = handlerMapping.getHandler(request);
            if (handlerChain == null) {
                return null;
            }

            Object handler = handlerChain.getHandler();
            if (handler instanceof HandlerMethod) {
                HandlerMethod handlerMethod = (HandlerMethod) handler;
                return handlerMethod.getMethodAnnotation(RateLimit.class);
            }
        } catch (Exception e) {
            log.debug("Failed to resolve handler method for rate limit annotation", e);
        }
        return null;
    }
}
