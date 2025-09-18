package com.linglevel.api.common.config;

import io.sentry.Sentry;
import io.sentry.protocol.User;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class SentryUserContext {

    /**
     * 현재 로그인한 사용자 정보를 Sentry에 설정
     */
    public static void setSentryUser() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            
            if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser")) {
                String username = auth.getName();
                
                User sentryUser = new User();
                sentryUser.setUsername(username);
                
                Sentry.setUser(sentryUser);
            }
        } catch (Exception e) {
            // 에러가 나더라도 무시
        }
    }
    
    /**
     * Sentry 사용자 정보 제거
     */
    public static void clearSentryUser() {
        Sentry.setUser(null);
    }
}