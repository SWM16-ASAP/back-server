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
import java.util.UUID;
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
        String pushId = UUID.randomUUID().toString();

        try {
            Map<String, String> data = buildDataWithUserId(messageRequest, userId);
            data.put("campaignId", pushId);

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
            String fcmMessageId = firebaseMessaging.send(message);
            log.debug("FCM message sent successfully - pushId: {}, fcmMessageId: {}", pushId, fcmMessageId);

            if (userId != null) {
                pushLogService.logSent(pushId, userId, true, campaignGroup, fcmMessageId);
            }

            return pushId;  // 자체 UUID 반환

        } catch (FirebaseMessagingException e) {
            log.error("Failed to send FCM message - pushId: {}", pushId, e);

            if (userId != null) {
                pushLogService.logSent(pushId, userId, false, campaignGroup, null);
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
            Map<Integer, String> indexToPushId = new java.util.HashMap<>();  // 인덱스별 pushId 매핑

            // Google Analytics 추적을 위한 FcmOptions 설정
            String analyticsLabel = null;
            FcmOptions fcmOptions = null;
            if (campaignGroup != null) {
                analyticsLabel = ANALYTICS_LABEL_PREFIX + campaignGroup;
                fcmOptions = FcmOptions.withAnalyticsLabel(analyticsLabel);
            }

            int index = 0;
            for (String fcmToken : fcmTokens) {
                String userId = tokenToUserId.get(fcmToken);
                String pushId = UUID.randomUUID().toString();
                indexToPushId.put(index, pushId);

                Map<String, String> data = buildDataWithUserId(messageRequest, userId);
                data.put("campaignId", pushId);

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
                index++;
            }

            BatchResponse response = firebaseMessaging.sendEach(messages);
            log.info("Batch messages sent to {} tokens - Success: {}, Failed: {} (Analytics: {})",
                     fcmTokens.size(), response.getSuccessCount(), response.getFailureCount(),
                     analyticsLabel != null ? analyticsLabel : "N/A");

            // 배치로 로그 저장 (자체 UUID와 FCM messageId 함께 저장)
            savePushLogsBatch(fcmTokens, campaignGroup, response, indexToPushId);

            return response;

        } catch (FirebaseMessagingException e) {
            log.error("Failed to send multicast FCM message: {}", e.getMessage());

            savePushLogsAllFailed(fcmTokens, campaignGroup);

            throw new FcmException(FcmErrorCode.MESSAGE_SEND_FAILED);
        }
    }

    /**
     * 푸시 로그를 배치로 저장 (성공/실패 혼합)
     */
    private void savePushLogsBatch(List<String> fcmTokens, String campaignGroup, BatchResponse response, Map<Integer, String> indexToPushId) {
        try {
            Map<String, String> tokenToUserId = getTokenToUserIdMap(fcmTokens);
            List<PushLog> logsToSave = new ArrayList<>();
            LocalDateTime now = LocalDateTime.now();

            for (int i = 0; i < response.getResponses().size(); i++) {
                String fcmToken = fcmTokens.get(i);
                String userId = tokenToUserId.get(fcmToken);
                SendResponse sendResponse = response.getResponses().get(i);
                boolean success = sendResponse.isSuccessful();
                String pushId = indexToPushId.get(i);  // 미리 생성된 UUID

                if (userId != null && pushId != null) {
                    String fcmMessageId = success ? sendResponse.getMessageId() : null;
                    logsToSave.add(createPushLog(pushId, userId, success, campaignGroup, fcmMessageId, now));
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

            for (String fcmToken : fcmTokens) {
                String userId = tokenToUserId.get(fcmToken);
                if (userId != null) {
                    String pushId = UUID.randomUUID().toString();
                    logsToSave.add(createPushLog(pushId, userId, false, campaignGroup, null, now));
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
    private PushLog createPushLog(String pushId, String userId, boolean success, String campaignGroup, String fcmMessageId, LocalDateTime now) {
        return PushLog.builder()
                .campaignId(pushId)  // 자체 UUID를 campaignId로 사용
                .fcmMessageId(fcmMessageId)  // FCM messageId (선택적)
                .campaignGroup(campaignGroup)  // 캠페인 그룹
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