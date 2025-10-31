package com.linglevel.api.fcm.service;

import com.google.firebase.messaging.*;
import com.linglevel.api.fcm.dto.FcmMessageRequest;
import com.linglevel.api.fcm.entity.FcmToken;
import com.linglevel.api.fcm.exception.FcmErrorCode;
import com.linglevel.api.fcm.exception.FcmException;
import com.linglevel.api.fcm.repository.FcmTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class FcmMessagingService {

    private final FirebaseMessaging firebaseMessaging;
    private final FcmTokenRepository fcmTokenRepository;
    private final PushLogService pushLogService;

    private static final String ANALYTICS_LABEL_PREFIX = "notification_sent_";

    /**
     * 단일 사용자에게 알림 전송
     */
    public String sendMessage(String fcmToken, FcmMessageRequest messageRequest) {
        String userId = getUserIdFromToken(fcmToken);
        String campaignId = messageRequest.getCampaignId();

        try {
            Message.Builder messageBuilder = Message.builder()
                    .setToken(fcmToken)
                    .setNotification(Notification.builder()
                            .setTitle(messageRequest.getTitle())
                            .setBody(messageRequest.getBody())
                            .build())
                    .putAllData(messageRequest.getData() != null ? messageRequest.getData() : Map.of());

            // Google Analytics 추적을 위한 FcmOptions 설정
            if (campaignId != null) {
                String analyticsLabel = ANALYTICS_LABEL_PREFIX + campaignId;
                messageBuilder.setFcmOptions(FcmOptions.withAnalyticsLabel(analyticsLabel));
                log.debug("Analytics label set: {}", analyticsLabel);
            }

            Message message = messageBuilder.build();
            String response = firebaseMessaging.send(message);
            log.debug("FCM message sent successfully: {}", response);

            // 송신 성공 로그 저장
            if (campaignId != null && userId != null) {
                pushLogService.logSent(campaignId, userId, true);
            }

            return response;

        } catch (FirebaseMessagingException e) {
            log.error("Failed to send FCM message: {}", e.getMessage());

            // 송신 실패 로그 저장
            if (campaignId != null && userId != null) {
                pushLogService.logSent(campaignId, userId, false);
            }

            throw new FcmException(FcmErrorCode.MESSAGE_SEND_FAILED);
        }
    }

    /**
     * 여러 사용자에게 동시 알림 전송 (기본 우선순위)
     */
    public BatchResponse sendMulticastMessage(List<String> fcmTokens, FcmMessageRequest messageRequest) {
        String campaignId = messageRequest.getCampaignId();

        try {
            if (fcmTokens == null || fcmTokens.isEmpty()) {
                throw new FcmException(FcmErrorCode.MESSAGE_SEND_FAILED);
            }

            MulticastMessage.Builder messageBuilder = MulticastMessage.builder()
                    .setNotification(Notification.builder()
                            .setTitle(messageRequest.getTitle())
                            .setBody(messageRequest.getBody())
                            .build())
                    .putAllData(messageRequest.getData() != null ? messageRequest.getData() : Map.of())
                    .addAllTokens(fcmTokens);

            // Google Analytics 추적을 위한 FcmOptions 설정
            String analyticsLabel = null;
            if (campaignId != null) {
                analyticsLabel = ANALYTICS_LABEL_PREFIX + campaignId;
                messageBuilder.setFcmOptions(FcmOptions.withAnalyticsLabel(analyticsLabel));
            }

            MulticastMessage message = messageBuilder.build();
            BatchResponse response = firebaseMessaging.sendMulticast(message);
            log.info("Multicast message sent to {} tokens - Success: {}, Failed: {} (Analytics: {})",
                     fcmTokens.size(), response.getSuccessCount(), response.getFailureCount(),
                     analyticsLabel != null ? analyticsLabel : "N/A");

            // 개별 결과를 로그에 저장
            if (campaignId != null) {
                for (int i = 0; i < response.getResponses().size(); i++) {
                    String fcmToken = fcmTokens.get(i);
                    String userId = getUserIdFromToken(fcmToken);
                    boolean success = response.getResponses().get(i).isSuccessful();

                    if (userId != null) {
                        pushLogService.logSent(campaignId, userId, success);
                    }
                }
            }

            return response;

        } catch (FirebaseMessagingException e) {
            log.error("Failed to send multicast FCM message: {}", e.getMessage());

            // 전체 실패 로그 저장
            if (campaignId != null) {
                for (String fcmToken : fcmTokens) {
                    String userId = getUserIdFromToken(fcmToken);
                    if (userId != null) {
                        pushLogService.logSent(campaignId, userId, false);
                    }
                }
            }

            throw new FcmException(FcmErrorCode.MESSAGE_SEND_FAILED);
        }
    }

    /**
     * FCM 토큰으로 사용자 ID 조회
     */
    private String getUserIdFromToken(String fcmToken) {
        Optional<FcmToken> tokenOpt = fcmTokenRepository.findByFcmToken(fcmToken);
        return tokenOpt.map(FcmToken::getUserId).orElse(null);
    }

}