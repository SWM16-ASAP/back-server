package com.linglevel.api.admin.service;

import com.google.firebase.messaging.*;
import com.linglevel.api.admin.dto.NotificationBroadcastRequest;
import com.linglevel.api.admin.dto.NotificationBroadcastResponse;
import com.linglevel.api.admin.dto.NotificationSendResponse;
import com.linglevel.api.admin.dto.NotificationSendRequest;
import com.linglevel.api.fcm.dto.FcmMessageRequest;
import com.linglevel.api.fcm.entity.FcmToken;
import com.linglevel.api.fcm.repository.FcmTokenRepository;
import com.linglevel.api.fcm.service.FcmMessagingService;
import com.linglevel.api.i18n.CountryCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final FcmMessagingService fcmMessagingService;
    private final FcmTokenRepository fcmTokenRepository;

    public NotificationSendResponse sendNotificationFromRequest(NotificationSendRequest request) {
        return sendLocalizedNotification(request.getTargets(), request.getMessages(), request.getData());
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

    /**
     * 국가별 메시지를 전송합니다.
     */
    private NotificationSendResponse sendLocalizedNotification(
            List<String> targetUserIds,
            Map<String, NotificationSendRequest.LocalizedMessage> messages,
            Map<String, String> data) {

        log.info("Starting localized notification send to {} users", targetUserIds.size());

        // 대상 사용자들의 활성 FCM 토큰 조회
        List<FcmToken> allTokens = new ArrayList<>();
        for (String userId : targetUserIds) {
            List<FcmToken> activeTokens = fcmTokenRepository.findByUserIdAndIsActive(userId, true);
            allTokens.addAll(activeTokens);
        }

        if (allTokens.isEmpty()) {
            log.warn("No FCM tokens found for users: {}", targetUserIds);
            return new NotificationSendResponse(
                    "No FCM tokens found for users.",
                    0, 0,
                    new NotificationSendResponse.NotificationSendDetails(
                            Collections.emptyList(),
                            Collections.emptyList()
                    )
            );
        }

        // 국가별로 토큰 그룹핑
        Map<CountryCode, List<FcmToken>> tokensByCountry = allTokens.stream()
                .collect(Collectors.groupingBy(
                        token -> token.getCountryCode() != null ? token.getCountryCode() : CountryCode.US
                ));

        List<String> sentTokens = new ArrayList<>();
        List<String> failedTokens = new ArrayList<>();

        // 국가별로 메시지 전송
        tokensByCountry.forEach((countryCode, tokens) -> {
            NotificationSendRequest.LocalizedMessage message = messages.get(countryCode.getCode());

            // 해당 국가 메시지가 없으면 US 기본값 사용
            if (message == null) {
                message = messages.get("US");
            }

            // US 메시지도 없으면 스킵
            if (message == null) {
                log.warn("No message found for country: {} and no fallback (US) message", countryCode);
                tokens.forEach(token -> failedTokens.add(token.getFcmToken()));
                return;
            }

            FcmMessageRequest fcmRequest = FcmMessageRequest.builder()
                    .title(message.getTitle())
                    .body(message.getBody())
                    .data(data)
                    .build();

            for (FcmToken token : tokens) {
                try {
                    fcmMessagingService.sendMessage(token.getFcmToken(), fcmRequest);
                    sentTokens.add(token.getFcmToken());
                    log.debug("Sent localized message (country: {}) to token: {}", countryCode, maskToken(token.getFcmToken()));
                } catch (Exception e) {
                    failedTokens.add(token.getFcmToken());
                    log.warn("Failed to send localized message to token: {}, error: {}", maskToken(token.getFcmToken()), e.getMessage());

                    if (e instanceof com.linglevel.api.fcm.exception.FcmException) {
                        deactivateTokenByFcmToken(token.getFcmToken());
                    }
                }
            }
        });

        log.info("Localized notification send completed - Success: {}, Failed: {}",
                sentTokens.size(), failedTokens.size());

        return new NotificationSendResponse(
                "Localized notification sent successfully.",
                sentTokens.size(),
                failedTokens.size(),
                new NotificationSendResponse.NotificationSendDetails(sentTokens, failedTokens)
        );
    }

    public NotificationBroadcastResponse sendBroadcastNotification(NotificationBroadcastRequest request) {
        return sendLocalizedBroadcast(request.getMessages(), request.getData());
    }

    /**
     * 국가별 메시지 브로드캐스트
     */
    private NotificationBroadcastResponse sendLocalizedBroadcast(
            Map<String, NotificationBroadcastRequest.LocalizedMessage> messages,
            Map<String, String> data) {

        log.info("Starting localized broadcast notification");

        // 모든 활성 FCM 토큰 조회
        List<FcmToken> allActiveTokens = fcmTokenRepository.findByIsActive(true);

        if (allActiveTokens.isEmpty()) {
            log.warn("No FCM tokens found for broadcast");
            return new NotificationBroadcastResponse(
                    "No FCM tokens found for broadcast.",
                    0, 0, 0,
                    new NotificationBroadcastResponse.NotificationBroadcastDetails(0, 0, 0)
            );
        }

        // 국가별로 토큰 그룹핑
        Map<CountryCode, List<FcmToken>> tokensByCountry = allActiveTokens.stream()
                .collect(Collectors.groupingBy(
                        token -> token.getCountryCode() != null ? token.getCountryCode() : CountryCode.US
                ));

        int totalTokens = allActiveTokens.size();
        int totalSentCount = 0;
        int totalFailedCount = 0;
        Set<String> successfulUserIds = new HashSet<>();
        Set<String> failedUserIds = new HashSet<>();

        // 국가별로 메시지 전송
        for (Map.Entry<CountryCode, List<FcmToken>> entry : tokensByCountry.entrySet()) {
            CountryCode countryCode = entry.getKey();
            List<FcmToken> tokens = entry.getValue();

            NotificationBroadcastRequest.LocalizedMessage message = messages.get(countryCode.getCode());

            // 해당 국가 메시지가 없으면 US 기본값 사용
            if (message == null) {
                message = messages.get("US");
            }

            // US 메시지도 없으면 스킵
            if (message == null) {
                log.warn("No message found for country: {} and no fallback (US) message", countryCode);
                for (FcmToken token : tokens) {
                    failedUserIds.add(token.getUserId());
                    totalFailedCount++;
                }
                continue;
            }

            FcmMessageRequest fcmRequest = FcmMessageRequest.builder()
                    .title(message.getTitle())
                    .body(message.getBody())
                    .data(data)
                    .build();

            for (FcmToken token : tokens) {
                try {
                    fcmMessagingService.sendMessage(token.getFcmToken(), fcmRequest);
                    successfulUserIds.add(token.getUserId());
                    totalSentCount++;
                    log.debug("Broadcast localized message (country: {}) sent to user: {}, token: {}",
                            countryCode, token.getUserId(), maskToken(token.getFcmToken()));
                } catch (Exception e) {
                    totalFailedCount++;
                    log.warn("Failed to send localized broadcast to user: {}, token: {}, error: {}",
                            token.getUserId(), maskToken(token.getFcmToken()), e.getMessage());

                    if (e instanceof com.linglevel.api.fcm.exception.FcmException) {
                        deactivateTokenByFcmToken(token.getFcmToken());
                    }
                }
            }
        }

        // 실패만 한 사용자 계산
        int failedOnlyUsers = (int) failedUserIds.stream()
                .filter(userId -> !successfulUserIds.contains(userId))
                .count();

        int totalUsers = (int) allActiveTokens.stream()
                .map(FcmToken::getUserId)
                .distinct()
                .count();

        log.info("Localized broadcast completed - Total users: {}, Successful users: {}, Failed users: {}, " +
                "Total sent: {}, Total failed: {}",
                totalUsers, successfulUserIds.size(), failedOnlyUsers, totalSentCount, totalFailedCount);

        return new NotificationBroadcastResponse(
                "Localized broadcast notification sent successfully.",
                totalUsers,
                totalSentCount,
                totalFailedCount,
                new NotificationBroadcastResponse.NotificationBroadcastDetails(
                        successfulUserIds.size(),
                        failedOnlyUsers,
                        totalTokens
                )
        );
    }
}