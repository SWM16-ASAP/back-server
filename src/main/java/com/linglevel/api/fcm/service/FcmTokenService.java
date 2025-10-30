package com.linglevel.api.fcm.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.linglevel.api.fcm.dto.FcmTokenUpsertRequest;
import com.linglevel.api.fcm.dto.FcmTokenUpsertResult;
import com.linglevel.api.fcm.entity.FcmToken;
import com.linglevel.api.fcm.exception.FcmErrorCode;
import com.linglevel.api.fcm.exception.FcmException;
import com.linglevel.api.fcm.repository.FcmTokenRepository;
import com.linglevel.api.i18n.CountryCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class FcmTokenService {

    private final FcmTokenRepository fcmTokenRepository;
    private final FirebaseMessaging firebaseMessaging;

    /**
     * FCM 토큰 유효성 검증
     */
    private boolean validateFcmToken(String fcmToken) {
        try {
            Message testMessage = Message.builder()
                    .setToken(fcmToken)
                    .setNotification(Notification.builder()
                            .setTitle("Token Validation")
                            .setBody("This is a test message")
                            .build())
                    .build();

            // dry-run으로 토큰 검증 (실제 전송 안함)
            firebaseMessaging.send(testMessage, true);
            return true;
        } catch (FirebaseMessagingException e) {
            log.warn("Invalid FCM token: {}, error: {}", fcmToken, e.getMessage());
            return false;
        }
    }

    /**
     * FCM 토큰을 비활성화합니다.
     * 전송 실패한 토큰을 비활성화하여 더 이상 사용하지 않도록 합니다.
     */
    public void deactivateToken(String fcmToken) {
        try {
            Optional<FcmToken> tokenOpt = fcmTokenRepository.findByFcmToken(fcmToken);
            if (tokenOpt.isPresent()) {
                FcmToken token = tokenOpt.get();
                token.setIsActive(false);
                token.setUpdatedAt(LocalDateTime.now());
                fcmTokenRepository.save(token);
                log.info("Deactivated invalid FCM token for user: {}, device: {}",
                         token.getUserId(), token.getDeviceId());
            } else {
                log.warn("FCM token not found in database for deactivation");
            }
        } catch (Exception e) {
            log.error("Failed to deactivate FCM token", e);
        }
    }

    public FcmTokenUpsertResult upsertFcmToken(String userId, FcmTokenUpsertRequest request) {
        try {
            // FCM 토큰 유효성 검증
            if (!validateFcmToken(request.getFcmToken())) {
                throw new FcmException(FcmErrorCode.INVALID_FCM_TOKEN_FORMAT);
            }

            // countryCode가 제공되지 않으면 기본값 US 사용
            CountryCode countryCode = request.getCountryCode() != null ? request.getCountryCode() : CountryCode.US;

            Optional<FcmToken> existingToken = fcmTokenRepository.findByUserIdAndDeviceId(userId, request.getDeviceId());
            
            if (existingToken.isPresent()) {
                // 기존 토큰 업데이트
                FcmToken token = existingToken.get();
                token.setFcmToken(request.getFcmToken());
                token.setPlatform(request.getPlatform());
                token.setCountryCode(countryCode);
                token.setAppVersion(request.getAppVersion());
                token.setOsVersion(request.getOsVersion());
                token.setUpdatedAt(LocalDateTime.now());
                token.setIsActive(true);
                
                FcmToken savedToken = fcmTokenRepository.save(token);
                log.info("FCM token updated for user: {}, device: {}", userId, request.getDeviceId());
                
                return FcmTokenUpsertResult.builder()
                        .tokenId(savedToken.getId())
                        .created(false)
                        .build();
            } else {
                // 새 토큰 생성
                FcmToken newToken = FcmToken.builder()
                        .userId(userId)
                        .deviceId(request.getDeviceId())
                        .fcmToken(request.getFcmToken())
                        .platform(request.getPlatform())
                        .countryCode(countryCode)
                        .appVersion(request.getAppVersion())
                        .osVersion(request.getOsVersion())
                        .createdAt(LocalDateTime.now())
                        .updatedAt(LocalDateTime.now())
                        .isActive(true)
                        .build();
                
                FcmToken savedToken = fcmTokenRepository.save(newToken);
                log.info("FCM token created for user: {}, device: {}", userId, request.getDeviceId());
                
                return FcmTokenUpsertResult.builder()
                        .tokenId(savedToken.getId())
                        .created(true)
                        .build();
            }
        } catch (Exception e) {
            log.error("Failed to upsert FCM token for user: {}, device: {}", userId, request.getDeviceId(), e);
            throw new FcmException(FcmErrorCode.TOKEN_CREATION_FAILED);
        }
    }
}