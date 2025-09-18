package com.linglevel.api.admin.service;

import com.google.firebase.messaging.*;
import com.linglevel.api.admin.dto.NotificationSendResponse;
import com.linglevel.api.admin.dto.NotificationSendRequest;
import com.linglevel.api.fcm.dto.FcmMessageRequest;
import com.linglevel.api.fcm.entity.FcmToken;
import com.linglevel.api.fcm.repository.FcmTokenRepository;
import com.linglevel.api.fcm.service.FcmMessagingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final FcmMessagingService fcmMessagingService;
    private final FcmTokenRepository fcmTokenRepository;

    public NotificationSendResponse sendNotificationFromRequest(NotificationSendRequest request) {
        List<String> targets = request.getTargets();
        
        // FcmMessageRequest로 변환
        FcmMessageRequest fcmRequest = FcmMessageRequest.builder()
                .title(request.getTitle())
                .body(request.getBody())
                .data(request.getData())
                .build();
        
        return sendNotificationFromFcmRequest(targets, fcmRequest);
    }
    
    private NotificationSendResponse sendNotificationFromFcmRequest(List<String> targets, FcmMessageRequest request) {
        log.info("Starting notification send to {} users", 
                 targets != null ? targets.size() : 0);
        
        // targets가 null이면 빈 응답 반환
        if (targets == null || targets.isEmpty()) {
            log.warn("No target users provided");
            return new NotificationSendResponse(
                    "No target users provided.",
                    0, 0,
                    new NotificationSendResponse.NotificationSendDetails(
                            Collections.emptyList(),
                            Collections.emptyList()
                    )
            );
        }
        
        // 사용자들의 활성 FCM 토큰 조회
        List<String> allTokens = new ArrayList<>();
        for (String userId : targets) {
            List<FcmToken> activeTokens = fcmTokenRepository.findByUserIdAndIsActive(userId, true);
            List<String> tokenStrings = activeTokens.stream()
                    .map(FcmToken::getFcmToken)
                    .toList();
            allTokens.addAll(tokenStrings);
        }

        if (allTokens.isEmpty()) {
            log.warn("No FCM tokens found for users: {}", targets);
            return new NotificationSendResponse(
                    "No FCM tokens found for user.",
                    0, 0,
                    new NotificationSendResponse.NotificationSendDetails(
                            Collections.emptyList(),
                            Collections.emptyList()
                    )
            );
        }

        // 전송
        return sendNotification(allTokens, request);
    }

    private NotificationSendResponse sendNotification(List<String> tokens, FcmMessageRequest request) {
        try {
            // 멀티캐스트 대신 개별 전송으로 변경 (FCM /batch 엔드포인트 문제 회피)
            List<String> sentTokens = new ArrayList<>();
            List<String> failedTokens = new ArrayList<>();

            for (String token : tokens) {
                try {
                    String response = fcmMessagingService.sendMessage(token, request);
                    sentTokens.add(token);
                    log.debug("FCM message sent successfully to token: {}", maskToken(token));
                } catch (Exception e) {
                    failedTokens.add(token);
                    log.warn("Failed to send FCM message to token: {}, error: {}", maskToken(token), e.getMessage());

                    // 유효하지 않은 토큰인 경우 비활성화
                    if (e instanceof com.linglevel.api.fcm.exception.FcmException) {
                        deactivateTokenByFcmToken(token);
                    }
                }
            }

            log.info("Notification send completed - Success: {}, Failed: {}",
                     sentTokens.size(), failedTokens.size());

            return new NotificationSendResponse(
                    "Notification sent successfully.",
                    sentTokens.size(),
                    failedTokens.size(),
                    new NotificationSendResponse.NotificationSendDetails(sentTokens, failedTokens)
            );

        } catch (Exception e) {
            log.error("Failed to send notification", e);
            throw new RuntimeException("Failed to send notification", e);
        }
    }


    private void deactivateTokenByFcmToken(String fcmToken) {
        try {
            Optional<FcmToken> tokenEntity = fcmTokenRepository.findByFcmToken(fcmToken);
            if (tokenEntity.isPresent()) {
                FcmToken token = tokenEntity.get();
                token.setIsActive(false);
                token.setUpdatedAt(LocalDateTime.now());
                fcmTokenRepository.save(token);
                log.info("Deactivated invalid FCM token for user: {}, device: {}", 
                         token.getUserId(), token.getDeviceId());
            } else {
                log.warn("FCM token not found in database: {}", maskToken(fcmToken));
            }
        } catch (Exception e) {
            log.error("Failed to deactivate token: {}", maskToken(fcmToken), e);
        }
    }

    private String maskToken(String token) {
        if (token == null || token.length() < 8) {
            return "***";
        }
        return token.substring(0, 4) + "***" + token.substring(token.length() - 4);
    }
}