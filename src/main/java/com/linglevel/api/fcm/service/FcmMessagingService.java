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
        String campaignGroup = messageRequest.getCampaignId();  // 원래의 campaignId를 그룹으로 사용

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
            if (campaignGroup != null) {
                String analyticsLabel = ANALYTICS_LABEL_PREFIX + campaignGroup;
                messageBuilder.setFcmOptions(FcmOptions.withAnalyticsLabel(analyticsLabel));
                log.debug("Analytics label set: {}", analyticsLabel);
            }

            Message message = messageBuilder.build();
            String messageId = firebaseMessaging.send(message);  // FCM이 반환하는 고유 messageId
            log.debug("FCM message sent successfully - messageId: {}", messageId);

            // 송신 성공 로그 저장 (messageId를 campaignId로 사용)
            if (messageId != null && userId != null) {
                pushLogService.logSent(messageId, userId, true, campaignGroup);
            }

            return messageId;

        } catch (FirebaseMessagingException e) {
            log.error("Failed to send FCM message: {}", e.getMessage());

            // 송신 실패 시에는 임시 ID 생성
            if (userId != null) {
                String tempMessageId = "failed-" + System.currentTimeMillis() + "-" + userId;
                pushLogService.logSent(tempMessageId, userId, false, campaignGroup);
            }

            throw new FcmException(FcmErrorCode.MESSAGE_SEND_FAILED);
        }
    }

    /**
     * 여러 사용자에게 동시 알림 전송 (각 토큰마다 userId 포함)
     */
    public BatchResponse sendMulticastMessage(List<String> fcmTokens, FcmMessageRequest messageRequest) {
        String campaignGroup = messageRequest.getCampaignId();  // 원래의 campaignId를 그룹으로 사용

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
            if (campaignGroup != null) {
                analyticsLabel = ANALYTICS_LABEL_PREFIX + campaignGroup;
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

            // 배치로 로그 저장 (각 응답의 messageId 사용)
            savePushLogsBatch(fcmTokens, campaignGroup, response);

            return response;

        } catch (FirebaseMessagingException e) {
            log.error("Failed to send multicast FCM message: {}", e.getMessage());

            // 전체 실패 로그 배치 저장
            savePushLogsAllFailed(fcmTokens, campaignGroup);

            throw new FcmException(FcmErrorCode.MESSAGE_SEND_FAILED);
        }
    }

    /**
     * 푸시 로그를 배치로 저장 (성공/실패 혼합)
     */
    private void savePushLogsBatch(List<String> fcmTokens, String campaignGroup, BatchResponse response) {
        try {
            Map<String, String> tokenToUserId = getTokenToUserIdMap(fcmTokens);
            List<PushLog> logsToSave = new ArrayList<>();
            LocalDateTime now = LocalDateTime.now();

            for (int i = 0; i < response.getResponses().size(); i++) {
                String fcmToken = fcmTokens.get(i);
                String userId = tokenToUserId.get(fcmToken);
                SendResponse sendResponse = response.getResponses().get(i);
                boolean success = sendResponse.isSuccessful();

                if (userId != null) {
                    // FCM messageId를 campaignId로 사용
                    String messageId = success ? sendResponse.getMessageId()
                            : "failed-" + System.currentTimeMillis() + "-" + i + "-" + userId;
                    logsToSave.add(createPushLog(messageId, userId, success, campaignGroup, now));
                }
            }

            savePushLogsIfNotEmpty(logsToSave, campaignGroup);
        } catch (Exception e) {
            log.error("Failed to batch save push logs for campaignGroup: {}", campaignGroup, e);
        }
    }

    /**
     * 전체 실패 시 푸시 로그를 배치로 저장
     */
    private void savePushLogsAllFailed(List<String> fcmTokens, String campaignGroup) {
        try {
            Map<String, String> tokenToUserId = getTokenToUserIdMap(fcmTokens);
            List<PushLog> logsToSave = new ArrayList<>();
            LocalDateTime now = LocalDateTime.now();

            int index = 0;
            for (String fcmToken : fcmTokens) {
                String userId = tokenToUserId.get(fcmToken);
                if (userId != null) {
                    String tempMessageId = "failed-" + System.currentTimeMillis() + "-" + index + "-" + userId;
                    logsToSave.add(createPushLog(tempMessageId, userId, false, campaignGroup, now));
                    index++;
                }
            }

            savePushLogsIfNotEmpty(logsToSave, campaignGroup);
        } catch (Exception e) {
            log.error("Failed to batch save failed push logs for campaignGroup: {}", campaignGroup, e);
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
    private PushLog createPushLog(String messageId, String userId, boolean success, String campaignGroup, LocalDateTime now) {
        return PushLog.builder()
                .campaignId(messageId)  // FCM messageId를 campaignId로 사용
                .campaignGroup(campaignGroup)  // 원래의 campaignId를 그룹으로 사용
                .userId(userId)
                .sentAt(now)
                .sentSuccess(success)
                .createdAt(now)
                .build();
    }

    /**
     * PushLog 목록이 비어있지 않으면 배치 저장
     */
    private void savePushLogsIfNotEmpty(List<PushLog> logsToSave, String campaignGroup) {
        if (!logsToSave.isEmpty()) {
            pushLogRepository.saveAll(logsToSave);
            log.debug("Batch saved {} push logs for campaignGroup: {}", logsToSave.size(), campaignGroup);
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