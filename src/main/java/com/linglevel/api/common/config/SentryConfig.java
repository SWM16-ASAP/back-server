package com.linglevel.api.common.config;

import io.sentry.SentryOptions.TracesSamplerCallback;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SentryConfig {
    
    @Bean
    public TracesSamplerCallback tracesSamplerCallback() {
        return (context) -> {
            HttpServletRequest request = (HttpServletRequest) context.getCustomSamplingContext().get("request");
            if (request != null) {
                String path = request.getRequestURI();
                if (path != null && path.startsWith("/actuator")) {
                    // actuator 경로의 트랜잭션은 샘플링하지 않음 (0%)
                    return 0.0;
                }
            }
            // 그 외의 경우는 application.properties의 traces-sample-rate 설정을 따름
            return null;
        };
    }
}