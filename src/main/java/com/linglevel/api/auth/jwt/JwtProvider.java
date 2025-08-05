package com.linglevel.api.auth.jwt;

import com.linglevel.api.auth.exception.AuthErrorCode;
import com.linglevel.api.auth.exception.AuthException;
import com.linglevel.api.users.entity.User;
import com.linglevel.api.users.entity.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
@Slf4j
public class JwtProvider {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.access-token-expiration}")
    private long accessTokenExpiration;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpiration;

    public String createToken(User user) {
        Claims claims = Jwts.claims()
                .add("username", user.getUsername())
                .add("email", user.getEmail())
                .add("role", user.getRole().name())
                .add("provider", user.getProvider())
                .add("display_name", user.getDisplayName())
                .build();

        SecretKey key = getSecretKey();

        return Jwts.builder()
                .claims(claims)
                .subject(user.getId())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + accessTokenExpiration))
                .signWith(key)
                .compact();
    }

    public JwtClaims parseTokenToJwtClaims(String token) {
        SecretKey key = getSecretKey();

        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        return JwtClaims.builder()
                .id(claims.getSubject())
                .username(claims.get("username", String.class))
                .email(claims.get("email", String.class))
                .role(UserRole.valueOf(claims.get("role", String.class)))
                .provider(claims.get("provider", String.class))
                .displayName(claims.get("display_name", String.class))
                .issuedAt(claims.getIssuedAt())
                .expiresAt(claims.getExpiration())
                .build();
    }

    public String extractTokenFromRequest(HttpServletRequest request) {
        String authorizationHeader = request.getHeader("Authorization");

        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            return authorizationHeader.substring(7);
        }

        return null;
    }

    public boolean isExpired(String token) {
        Date expiredDate = parseTokenToJwtClaims(token).getExpiresAt();
        return expiredDate.before(new Date());
    }

    private SecretKey getSecretKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
