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

        String requestUri = request.getRequestURI();
        
        // admin API 경로인 경우에만 처리
        if (requestUri.startsWith("/api/v1/admin/")) {
            String apiKey = request.getHeader("X-API-Key");
            
            if (importApiKey.equals(apiKey)) {
                SimpleGrantedAuthority adminAuthority = new SimpleGrantedAuthority(UserRole.ADMIN.getSecurityRole());
                UsernamePasswordAuthenticationToken authentication = 
                    new UsernamePasswordAuthenticationToken("admin", null, List.of(adminAuthority));
                
                SecurityContextHolder.getContext().setAuthentication(authentication);
                log.debug("Admin authentication successful for URI: {}", requestUri);
            }
        }
        
        filterChain.doFilter(request, response);
    }
}