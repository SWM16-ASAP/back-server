package com.linglevel.api.fcm.service;

import com.linglevel.api.fcm.entity.FcmPlatform;
import com.linglevel.api.fcm.entity.FcmToken;
import com.linglevel.api.fcm.repository.FcmTokenRepository;
import com.linglevel.api.i18n.CountryCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * FCM 토큰 로그아웃/삭제 시 비활성화 테스트
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("FCM 토큰 비활성화 테스트")
class FcmTokenServiceTest {

    @Mock
    private FcmTokenRepository fcmTokenRepository;

    @InjectMocks
    private FcmTokenService fcmTokenService;

    @Test
    @DisplayName("디바이스별 토큰 비활성화")
    void testDeactivateTokenByDevice() {
        // Given
        String userId = "user123";
        String deviceId = "device456";
        FcmToken token = createToken(userId, deviceId, "token789");

        when(fcmTokenRepository.findByUserIdAndDeviceId(userId, deviceId))
                .thenReturn(Optional.of(token));

        // When
        fcmTokenService.deactivateTokenByDevice(userId, deviceId);

        // Then
        ArgumentCaptor<FcmToken> captor = ArgumentCaptor.forClass(FcmToken.class);
        verify(fcmTokenRepository).save(captor.capture());

        FcmToken savedToken = captor.getValue();
        assertThat(savedToken.getIsActive()).isFalse();
        assertThat(savedToken.getUpdatedAt()).isNotNull();
    }

    @Test
    @DisplayName("전체 토큰 비활성화 (로그아웃/계정삭제)")
    void testDeactivateAllTokens() {
        // Given
        String userId = "user123";
        List<FcmToken> tokens = List.of(
                createToken(userId, "device1", "token1"),
                createToken(userId, "device2", "token2"),
                createToken(userId, "device3", "token3")
        );

        when(fcmTokenRepository.findByUserIdAndIsActive(userId, true))
                .thenReturn(tokens);

        // When
        fcmTokenService.deactivateAllTokens(userId);

        // Then
        verify(fcmTokenRepository, times(3)).save(any(FcmToken.class));

        ArgumentCaptor<FcmToken> captor = ArgumentCaptor.forClass(FcmToken.class);
        verify(fcmTokenRepository, atLeast(1)).save(captor.capture());

        // 비활성화된 토큰 확인
        List<FcmToken> savedTokens = captor.getAllValues();
        assertThat(savedTokens).allMatch(token -> !token.getIsActive());
    }

    @Test
    @DisplayName("토큰이 없을 때 - 에러 없이 처리")
    void testDeactivateAllTokens_NoTokens() {
        // Given
        String userId = "user123";
        when(fcmTokenRepository.findByUserIdAndIsActive(userId, true))
                .thenReturn(List.of());

        // When & Then (에러 없이 완료되어야 함)
        fcmTokenService.deactivateAllTokens(userId);

        verify(fcmTokenRepository, never()).save(any());
    }

    // Helper method
    private FcmToken createToken(String userId, String deviceId, String fcmToken) {
        return FcmToken.builder()
                .id("id_" + fcmToken)
                .userId(userId)
                .deviceId(deviceId)
                .fcmToken(fcmToken)
                .platform(FcmPlatform.ANDROID)
                .countryCode(CountryCode.KR)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
}
