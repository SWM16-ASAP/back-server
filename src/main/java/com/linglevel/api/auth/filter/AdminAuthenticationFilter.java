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

        if (isValidApiKey(apiKey)) {
            SimpleGrantedAuthority adminAuthority = new SimpleGrantedAuthority(UserRole.ADMIN.getSecurityRole());
            UsernamePasswordAuthenticationToken authentication =
                new UsernamePasswordAuthenticationToken("admin", null, List.of(adminAuthority));

            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
        
        filterChain.doFilter(request, response);
    }


    // Timing Attack 방지
    private boolean isValidApiKey(String providedApiKey) {
        if (providedApiKey == null || importApiKey == null) {
            return false;
        }
        
        // Constant-time 비교를 위해 MessageDigest.isEqual() 사용
        byte[] expected = importApiKey.getBytes();
        byte[] provided = providedApiKey.getBytes();
        
        return MessageDigest.isEqual(expected, provided);
    }
}