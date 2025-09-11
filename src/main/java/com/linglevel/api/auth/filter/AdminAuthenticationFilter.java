package com.linglevel.api.auth.filter;

import com.linglevel.api.user.entity.UserRole;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.security.MessageDigest;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdminAuthenticationFilter extends OncePerRequestFilter {

    @Value("${import.api.key}")
    private String importApiKey;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String apiKey = request.getHeader("X-API-Key");
        String authHeader = request.getHeader("Authorization");

        if (isValidAdminCredentials(apiKey, authHeader)) {
            SimpleGrantedAuthority adminAuthority = new SimpleGrantedAuthority(UserRole.ADMIN.getSecurityRole());
            UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken("admin", null, List.of(adminAuthority));

            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
        
        filterChain.doFilter(request, response);
    }


     // Admin 인증을 위한 종합적인 검증 (X-API-Key 또는 Authorization 헤더)
    private boolean isValidAdminCredentials(String apiKey, String authHeader) {
        // X-API-Key 헤더로 인증 시도
        if (isValidApiKey(apiKey)) {
            return true;
        }
        
        // Authorization 헤더로 인증 시도 (Prometheus용)
        return isValidMonitoringToken(authHeader);
    }

     // X-API-Key 헤더 검증 (Timing Attack 방지)
    private boolean isValidApiKey(String providedApiKey) {
        if (providedApiKey == null || importApiKey == null) {
            return false;
        }
        
        // Constant-time 비교를 위해 MessageDigest.isEqual() 사용
        byte[] expected = importApiKey.getBytes();
        byte[] provided = providedApiKey.getBytes();
        
        return MessageDigest.isEqual(expected, provided);
    }

     // Authorization 헤더 검증 (Prometheus monitoring용)
    private boolean isValidMonitoringToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("llvk ")) {
            return false;
        }
        
        String token = authHeader.substring(5); // "llvk " 제거
        
        // Import API Key와 동일한 토큰 검증
        return isSecureEquals(importApiKey, token);
    }

     // Timing Attack을 방지하는 안전한 문자열 비교
    private boolean isSecureEquals(String expected, String provided) {
        if (expected == null || provided == null) {
            return false;
        }
        
        byte[] expectedBytes = expected.getBytes();
        byte[] providedBytes = provided.getBytes();
        
        return MessageDigest.isEqual(expectedBytes, providedBytes);
    }
}