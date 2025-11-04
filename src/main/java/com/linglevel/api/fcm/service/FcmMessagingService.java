package com.linglevel.api.fcm.service;

import com.google.firebase.messaging.*;
import com.linglevel.api.fcm.dto.FcmMessageRequest;
import com.linglevel.api.fcm.entity.FcmToken;
import com.linglevel.api.fcm.entity.PushLog;
import com.linglevel.api.fcm.exception.FcmErrorCode;
import com.linglevel.api.fcm.exception.FcmException;
import com.linglevel.api.fcm.repository.FcmTokenRepository;
import com.linglevel.api.fcm.repository.PushLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FcmMessagingService {

    private final FirebaseMessaging firebaseMessaging;
    private final FcmTokenRepository fcmTokenRepository;
    private final PushLogRepository pushLogRepository;
    private final PushLogService pushLogService;

    private static final String ANALYTICS_LABEL_PREFIX = "notification_sent_";

    /**
     * 단일 사용자에게 알림 전송
     */
    public String sendMessage(String fcmToken, FcmMessageRequest messageRequest) {
        String userId = getUserIdFromToken(fcmToken);
        String campaignId = messageRequest.getCampaignId();

        try {
            Map<String, String> data = buildDataWithUserId(messageRequest, userId);

            Message.Builder messageBuilder = Message.builder()
                    .setToken(fcmToken)
                    .setNotification(Notification.builder()
                            .setTitle(messageRequest.getTitle())
                            .setBody(messageRequest.getBody())
                            .build())
                    .putAllData(data);

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
     * 여러 사용자에게 동시 알림 전송 (각 토큰마다 userId 포함)
     */
    public BatchResponse sendMulticastMessage(List<String> fcmTokens, FcmMessageRequest messageRequest) {
        String campaignId = messageRequest.getCampaignId();

        try {
            if (fcmTokens == null || fcmTokens.isEmpty()) {
                throw new FcmException(FcmErrorCode.MESSAGE_SEND_FAILED);
            }

            // 각 토큰마다 userId를 포함한 개별 메시지 생성
            Map<String, String> tokenToUserId = getTokenToUserIdMap(fcmTokens);
            List<Message> messages = new ArrayList<>();

            // Google Analytics 추적을 위한 FcmOptions 설정
            String analyticsLabel = null;
            FcmOptions fcmOptions = null;
            if (campaignId != null) {
                analyticsLabel = ANALYTICS_LABEL_PREFIX + campaignId;
                fcmOptions = FcmOptions.withAnalyticsLabel(analyticsLabel);
            }

            for (String fcmToken : fcmTokens) {
                String userId = tokenToUserId.get(fcmToken);
                Map<String, String> data = buildDataWithUserId(messageRequest, userId);

                Message.Builder messageBuilder = Message.builder()
                        .setToken(fcmToken)
                        .setNotification(Notification.builder()
                                .setTitle(messageRequest.getTitle())
                                .setBody(messageRequest.getBody())
                                .build())
                        .putAllData(data);

                if (fcmOptions != null) {
                    messageBuilder.setFcmOptions(fcmOptions);
                }

                messages.add(messageBuilder.build());
            }

            BatchResponse response = firebaseMessaging.sendAll(messages);
            log.info("Batch messages sent to {} tokens - Success: {}, Failed: {} (Analytics: {})",
                     fcmTokens.size(), response.getSuccessCount(), response.getFailureCount(),
                     analyticsLabel != null ? analyticsLabel : "N/A");

            // 배치로 로그 저장
            if (campaignId != null) {
                savePushLogsBatch(fcmTokens, campaignId, response);
            }

            return response;

        } catch (FirebaseMessagingException e) {
            log.error("Failed to send multicast FCM message: {}", e.getMessage());

            // 전체 실패 로그 배치 저장
            if (campaignId != null) {
                savePushLogsAllFailed(fcmTokens, campaignId);
            }

            throw new FcmException(FcmErrorCode.MESSAGE_SEND_FAILED);
        }
    }

    /**
     * 푸시 로그를 배치로 저장 (성공/실패 혼합)
     */
    private void savePushLogsBatch(List<String> fcmTokens, String campaignId, BatchResponse response) {
        try {
            Map<String, String> tokenToUserId = getTokenToUserIdMap(fcmTokens);
            List<PushLog> logsToSave = new ArrayList<>();
            LocalDateTime now = LocalDateTime.now();

            for (int i = 0; i < response.getResponses().size(); i++) {
                String fcmToken = fcmTokens.get(i);
                String userId = tokenToUserId.get(fcmToken);
                boolean success = response.getResponses().get(i).isSuccessful();

                if (userId != null) {
                    logsToSave.add(createPushLog(campaignId, userId, success, now));
                }
            }

            savePushLogsIfNotEmpty(logsToSave, campaignId);
        } catch (Exception e) {
            log.error("Failed to batch save push logs for campaign: {}", campaignId, e);
        }
    }

    /**
     * 전체 실패 시 푸시 로그를 배치로 저장
     */
    private void savePushLogsAllFailed(List<String> fcmTokens, String campaignId) {
        try {
            Map<String, String> tokenToUserId = getTokenToUserIdMap(fcmTokens);
            List<PushLog> logsToSave = new ArrayList<>();
            LocalDateTime now = LocalDateTime.now();

            for (String fcmToken : fcmTokens) {
                String userId = tokenToUserId.get(fcmToken);
                if (userId != null) {
                    logsToSave.add(createPushLog(campaignId, userId, false, now));
                }
            }

            savePushLogsIfNotEmpty(logsToSave, campaignId);
        } catch (Exception e) {
            log.error("Failed to batch save failed push logs for campaign: {}", campaignId, e);
        }
    }

    /**
     * FCM 토큰 목록으로 토큰-사용자ID 맵 생성
     */
    private Map<String, String> getTokenToUserIdMap(List<String> fcmTokens) {
        List<FcmToken> tokens = fcmTokenRepository.findAllByFcmTokenIn(fcmTokens);
        return tokens.stream()
                .collect(Collectors.toMap(
                        FcmToken::getFcmToken,
                        FcmToken::getUserId,
                        (existing, replacement) -> existing
                ));
    }

    /**
     * PushLog 객체 생성
     */
    private PushLog createPushLog(String campaignId, String userId, boolean success, LocalDateTime now) {
        return PushLog.builder()
                .campaignId(campaignId)
                .userId(userId)
                .sentAt(now)
                .sentSuccess(success)
                .createdAt(now)
                .build();
    }

    /**
     * PushLog 목록이 비어있지 않으면 배치 저장
     */
    private void savePushLogsIfNotEmpty(List<PushLog> logsToSave, String campaignId) {
        if (!logsToSave.isEmpty()) {
            pushLogRepository.saveAll(logsToSave);
            log.debug("Batch saved {} push logs for campaign: {}", logsToSave.size(), campaignId);
        }
    }

    /**
     * FCM 토큰으로 사용자 ID 조회
     */
    private String getUserIdFromToken(String fcmToken) {
        Optional<FcmToken> tokenOpt = fcmTokenRepository.findFirstByFcmToken(fcmToken);
        return tokenOpt.map(FcmToken::getUserId).orElse(null);
    }

    /**
     * 메시지 요청의 data에 userId 추가
     */
    private Map<String, String> buildDataWithUserId(FcmMessageRequest messageRequest, String userId) {
        Map<String, String> data = messageRequest.getData() != null
                ? new java.util.HashMap<>(messageRequest.getData())
                : new java.util.HashMap<>();
        if (userId != null) {
            data.put("userId", userId);
        }
        return data;
    }

}