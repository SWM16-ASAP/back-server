package com.linglevel.api.auth.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.AllArgsConstructor;

import javax.crypto.SecretKey;
import java.util.Date;

@AllArgsConstructor
public class JwtTokenProvider {

    private final String jwtSecret;
    private final long accessTokenExpiration;
    private final long refreshTokenExpiration;

    public String createToken(String username) {
        Claims claims = Jwts.claims()
                .add("username", username)
                .build();

        SecretKey key = secretToKey(jwtSecret);

        return Jwts.builder()
                .claims(claims)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + accessTokenExpiration))
                .signWith(key)
                .compact();
    }

    public static String getUsername(String token, String secret) {
        return extractClaims(token, secret).get("username").toString();
    }

    public static boolean isExpired(String token, String secret) {
        Date expiredDate = extractClaims(token, secret).getExpiration();
        return expiredDate.before(new Date());
    }

    private static Claims extractClaims(String token, String secret) {
        SecretKey key = secretToKey(secret);
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    }

    private static SecretKey secretToKey(String secret) {
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        return Keys.hmacShaKeyFor(keyBytes);
    }

}
