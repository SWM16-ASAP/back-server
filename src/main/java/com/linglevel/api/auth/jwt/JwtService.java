package com.linglevel.api.auth.jwt;

import com.linglevel.api.auth.exception.AuthErrorCode;
import com.linglevel.api.auth.exception.AuthException;
import com.linglevel.api.users.entity.UserRole;
import io.jsonwebtoken.Claims;
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
        String token = jwtProvider.extractTokenFromRequest(request);

        JwtClaims jwtClaims = jwtProvider.parseTokenToJwtClaims(token);

        if (jwtProvider.isExpired(token) || jwtClaims.getId() == null) {
            throw new AuthException(AuthErrorCode.INVALID_ACCESS_TOKEN);
        }

        return jwtClaims;
    }
}