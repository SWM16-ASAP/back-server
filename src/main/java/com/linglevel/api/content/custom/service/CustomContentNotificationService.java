package com.linglevel.api.content.custom.service;

import com.linglevel.api.fcm.dto.FcmMessageRequest;
import com.linglevel.api.fcm.entity.FcmToken;
import com.linglevel.api.fcm.dto.NotificationMessage;
import com.linglevel.api.fcm.repository.FcmTokenRepository;
import com.linglevel.api.fcm.service.FcmMessagingService;
import com.linglevel.api.i18n.CountryCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomContentNotificationService {

    private final FcmTokenRepository fcmTokenRepository;
    private final FcmMessagingService fcmMessagingService;
    private final com.linglevel.api.fcm.service.FcmTokenService fcmTokenService;

    public void sendContentCompletedNotification(String userId, String requestId, String contentTitle, String contentId) {
        try {
            List<FcmToken> userTokens = fcmTokenRepository.findByUserId(userId);

            if (userTokens.isEmpty()) {
                log.info("No FCM tokens found for user: {}", userId);
                return;
            }

            Map<String, String> additionalData = new HashMap<>();
            additionalData.put("requestId", requestId);
            additionalData.put("contentTitle", contentTitle);
            if (contentId != null) {
                additionalData.put("contentId", contentId);
            }

            // 토큰을 국가별로 그룹핑
            Map<CountryCode, List<FcmToken>> tokensByCountry = userTokens.stream()
                    .collect(Collectors.groupingBy(
                            token -> token.getCountryCode() != null ? token.getCountryCode() : CountryCode.US
                    ));

            // 국가별로 다른 메시지 전송
            tokensByCountry.forEach((countryCode, tokens) -> {
                String title = NotificationMessage.CONTENT_COMPLETED.getTitle(countryCode);
                String body = NotificationMessage.CONTENT_COMPLETED.getBody(countryCode, contentTitle);

                FcmMessageRequest messageRequest = FcmMessageRequest.builder()
                        .title(title)
                        .body(body)
                        .type("custom_content_completed")
                        .userId(userId)
                        .action("view_content")
                        .deepLink(contentId != null ? "linglevel:///customContent/" + contentId : "linglevel:///customContent")
                        .campaignId("customContent-completed")
                        .additionalData(additionalData)
                        .build();

                List<String> fcmTokens = tokens.stream()
                        .map(FcmToken::getFcmToken)
                        .collect(Collectors.toList());

                try {
                    if (fcmTokens.size() == 1) {
                        fcmMessagingService.sendMessage(fcmTokens.get(0), messageRequest);
                        log.info("Sent completion notification (country: {}) to user: {}", countryCode, userId);
                    } else if (!fcmTokens.isEmpty()) {
                        com.google.firebase.messaging.BatchResponse response =
                                fcmMessagingService.sendMulticastMessage(fcmTokens, messageRequest);

                        // 개별 응답 처리 - 실패한 토큰 비활성화
                        for (int i = 0; i < response.getResponses().size(); i++) {
                            if (!response.getResponses().get(i).isSuccessful()) {
                                String failedToken = fcmTokens.get(i);
                                log.warn("Failed to send completion notification to token for user: {}, error: {}",
                                        userId, response.getResponses().get(i).getException().getMessage());
                                fcmTokenService.deactivateToken(failedToken);
                            }
                        }

                        log.info("Sent multicast completion notification (country: {}) to user: {} - Success: {}, Failed: {}",
                                countryCode, userId, response.getSuccessCount(), response.getFailureCount());
                    }
                } catch (Exception e) {
                    log.error("Failed to send completion notification (country: {}) to user: {}, error: {}",
                            countryCode, userId, e.getMessage(), e);
                    if (e instanceof com.linglevel.api.fcm.exception.FcmException) {
                        fcmTokens.forEach(fcmTokenService::deactivateToken);
                    }
                }
            });

        } catch (Exception e) {
            log.error("Failed to send content completion notification for user: {}, requestId: {}",
                    userId, requestId, e);
        }
    }

    public void sendContentFailedNotification(String userId, String requestId, String contentTitle, String errorMessage) {
        try {
            List<FcmToken> userTokens = fcmTokenRepository.findByUserId(userId);

            if (userTokens.isEmpty()) {
                log.info("No FCM tokens found for user: {}", userId);
                return;
            }

            Map<String, String> additionalData = new HashMap<>();
            additionalData.put("requestId", requestId);
            additionalData.put("contentTitle", contentTitle);
            additionalData.put("errorMessage", errorMessage);

            // 토큰을 국가별로 그룹핑
            Map<CountryCode, List<FcmToken>> tokensByCountry = userTokens.stream()
                    .collect(Collectors.groupingBy(
                            token -> token.getCountryCode() != null ? token.getCountryCode() : CountryCode.US
                    ));

            // 국가별로 다른 메시지 전송
            tokensByCountry.forEach((countryCode, tokens) -> {
                String title = NotificationMessage.CONTENT_FAILED.getTitle(countryCode);
                String body = NotificationMessage.CONTENT_FAILED.getBody(countryCode);

                FcmMessageRequest messageRequest = FcmMessageRequest.builder()
                        .title(title)
                        .body(body)
                        .type("custom_content_failed")
                        .userId(userId)
                        .action("view_chat")
                        .deepLink("linglevel:///import?state=chat")
                        .campaignId("customContent-failed")
                        .additionalData(additionalData)
                        .build();

                List<String> fcmTokens = tokens.stream()
                        .map(FcmToken::getFcmToken)
                        .collect(Collectors.toList());

                try {
                    if (fcmTokens.size() == 1) {
                        fcmMessagingService.sendMessage(fcmTokens.get(0), messageRequest);
                        log.info("Sent failure notification (country: {}) to user: {}", countryCode, userId);
                    } else if (!fcmTokens.isEmpty()) {
                        com.google.firebase.messaging.BatchResponse response =
                                fcmMessagingService.sendMulticastMessage(fcmTokens, messageRequest);

                        // 개별 응답 처리 - 실패한 토큰 비활성화
                        for (int i = 0; i < response.getResponses().size(); i++) {
                            if (!response.getResponses().get(i).isSuccessful()) {
                                String failedToken = fcmTokens.get(i);
                                log.warn("Failed to send failure notification to token for user: {}, error: {}",
                                        userId, response.getResponses().get(i).getException().getMessage());
                                fcmTokenService.deactivateToken(failedToken);
                            }
                        }

                        log.info("Sent multicast failure notification (country: {}) to user: {} - Success: {}, Failed: {}",
                                countryCode, userId, response.getSuccessCount(), response.getFailureCount());
                    }
                } catch (Exception e) {
                    log.error("Failed to send failure notification (country: {}) to user: {}, error: {}",
                            countryCode, userId, e.getMessage(), e);
                    if (e instanceof com.linglevel.api.fcm.exception.FcmException) {
                        fcmTokens.forEach(fcmTokenService::deactivateToken);
                    }
                }
            });

        } catch (Exception e) {
            log.error("Failed to send content failure notification for user: {}, requestId: {}",
                    userId, requestId, e);
        }
    }

}