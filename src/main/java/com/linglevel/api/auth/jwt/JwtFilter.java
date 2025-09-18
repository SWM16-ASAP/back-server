package com.linglevel.api.auth.jwt;

import com.linglevel.api.auth.exception.AuthErrorCode;
import com.linglevel.api.auth.exception.AuthException;
import com.linglevel.api.common.config.SentryUserContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class JwtFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws SecurityException, IOException, ServletException {

        try {
            JwtClaims claims = jwtService.extractJwtClaimsFromRequest(request);

            if (claims.isExpired()) {
                throw new AuthException(AuthErrorCode.EXPIRED_ACCESS_TOKEN);
            }

            // TODO: 성능 최적화를 위해 Principal로 username(String) 대신 JwtClaims 객체 전체를 저장하는 것을 고려해야 합니다.
            // 이렇게 하면 컨트롤러 레벨에서 DB 조회 없이 사용자 ID, 역할 등 추가 정보에 접근할 수 있습니다.
            // 예: new UsernamePasswordAuthenticationToken(claims, null, authorities);
            // 변경 시 컨트롤러의 @AuthenticationPrincipal 타입도 JwtClaims으로 수정해야 합니다.
            UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(
                    claims.getUsername(),
                    null,
                    List.of(new SimpleGrantedAuthority(claims.getRole().getSecurityRole()))
            );

            authenticationToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authenticationToken);
            
            // Sentry 사용자 컨텍스트 설정
            SentryUserContext.setSentryUser();

        } catch (Exception e) {
            filterChain.doFilter(request, response);
            return;
        }

        filterChain.doFilter(request, response);
    }
}
