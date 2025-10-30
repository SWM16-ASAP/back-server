package com.linglevel.api.auth.jwt;

import com.linglevel.api.auth.dto.RefreshTokenResponse;
import com.linglevel.api.auth.exception.AuthException;
import com.linglevel.api.auth.exception.AuthErrorCode;
import com.linglevel.api.auth.repository.RefreshTokenRepository;
import com.linglevel.api.user.entity.User;
import com.linglevel.api.user.repository.UserRepository;
import com.linglevel.api.user.exception.UsersException;
import com.linglevel.api.user.exception.UsersErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenService {
    
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;
    
    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpirationMs;
    
    @Transactional
    public String createRefreshToken(String userId) {
        String tokenId = UUID.randomUUID().toString();
        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(refreshTokenExpirationMs / 1000);

        RefreshToken refreshToken = RefreshToken.builder()
                .tokenId(tokenId)
                .userId(userId)
                .expiresAt(expiresAt)
                .build();

        refreshTokenRepository.save(refreshToken);
        log.info("Refresh token created for user: {}", userId);

        return tokenId;
    }
    
    @Transactional
    public RefreshTokenResponse refreshAccessToken(String refreshTokenId) {
        RefreshToken storedToken = refreshTokenRepository.findByTokenId(refreshTokenId)
                .orElseThrow(() -> new AuthException(AuthErrorCode.INVALID_REFRESH_TOKEN));
        
        if (storedToken.isExpired()) {
            refreshTokenRepository.delete(storedToken);
            throw new AuthException(AuthErrorCode.EXPIRED_REFRESH_TOKEN);
        }
        
        User user = userRepository.findById(storedToken.getUserId())
                .orElseThrow(() -> new UsersException(UsersErrorCode.USER_NOT_FOUND));
        
        if (user.getDeleted() != null && user.getDeleted()) {
            refreshTokenRepository.delete(storedToken);
            throw new UsersException(UsersErrorCode.USER_ACCOUNT_DELETED);
        }
        
        String newAccessToken = jwtProvider.createToken(user);
        String newRefreshToken = createRefreshToken(user.getId());
        
        log.info("Access token refreshed for user: {}", user.getId());
        
        return RefreshTokenResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .build();
    }
    
    @Transactional
    public void deleteRefreshToken(String tokenId) {
        refreshTokenRepository.findByTokenId(tokenId)
                .ifPresent(token -> {
                    refreshTokenRepository.delete(token);
                    log.info("Refresh token deleted: {} for user: {}", tokenId, token.getUserId());
                });
    }

    @Transactional
    public void deleteAllRefreshTokens(String userId) {
        refreshTokenRepository.deleteByUserId(userId);
        log.info("All refresh tokens deleted for user: {}", userId);
    }
}