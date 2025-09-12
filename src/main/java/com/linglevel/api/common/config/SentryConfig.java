package com.linglevel.api.common.config;

import com.linglevel.api.common.exception.CommonException;
import io.sentry.SentryOptions;
import io.sentry.SentryOptions.TracesSamplerCallback;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.servlet.resource.NoResourceFoundException;


@Configuration
public class SentryConfig {

    @Bean
    public TracesSamplerCallback tracesSamplerCallback() {
        return (context) -> {
            HttpServletRequest request = (HttpServletRequest) context.getCustomSamplingContext().get("request");
            if (request != null) {
                String path = request.getRequestURI();
                if (path != null && (path.startsWith("/actuator"))) {
                    return 0.0; // 샘플링 비율 0%
                }
            }
            // 그 외의 경우는 application.properties 설정을 따름
            return null;
        };
    }

    @Bean
    public SentryOptions.BeforeSendCallback beforeSendCallback() {
        return (event, hint) -> {
            // event 객체에서 직접 예외를 가져옵니다.
            Throwable throwable = event.getThrowable();
            if (throwable == null) {
                return event;
            }

            // 1. CommonException 및 하위 예외가 4xx 상태를 가지면 무시합니다.
            if (throwable instanceof CommonException) {
                if (((CommonException) throwable).getStatus().is4xxClientError()) {
                    return null;
                }
            }

            // 2. 그 외 표준적인 스프링 예외 중 4xx를 유발하는 것들을 무시합니다.
            if (throwable instanceof AuthenticationException ||
                throwable instanceof AccessDeniedException ||
                throwable instanceof MethodArgumentNotValidException ||
                throwable instanceof ConstraintViolationException ||
                throwable instanceof NoResourceFoundException ||
                throwable instanceof HttpMessageNotReadableException) {
                return null;
            }

            // 그 외 모든 예외(주로 5xx)는 Sentry로 전송합니다.
            return event;
        };
    }
}
