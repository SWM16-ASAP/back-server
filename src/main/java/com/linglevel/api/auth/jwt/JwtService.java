package com.linglevel.api.auth.jwt;

import com.linglevel.api.auth.exception.AuthErrorCode;
import com.linglevel.api.auth.exception.AuthException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class JwtService {

    private final JwtProvider jwtProvider;

    public JwtClaims extractJwtClaimsFromRequest(HttpServletRequest request) {
        String token = extractTokenFromRequest(request);
        return jwtProvider.parseTokenToJwtClaims(token);
    }

    public String extractTokenFromRequest(HttpServletRequest request) {
        String authorizationHeader = request.getHeader("Authorization");

        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            return authorizationHeader.substring(7);
        }

        throw new AuthException(AuthErrorCode.INVALID_ACCESS_TOKEN);
    }
}