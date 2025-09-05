package com.linglevel.api.content.custom.service;

import com.linglevel.api.fcm.dto.FcmMessageRequest;
import com.linglevel.api.fcm.entity.FcmToken;
import com.linglevel.api.fcm.repository.FcmTokenRepository;
import com.linglevel.api.fcm.service.FcmMessagingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomContentNotificationService {

    private final FcmTokenRepository fcmTokenRepository;
    private final FcmMessagingService fcmMessagingService;

    public void sendContentCompletedNotification(String userId, String requestId, String contentTitle) {
        try {
            List<FcmToken> userTokens = fcmTokenRepository.findByUserId(userId);
            
            if (userTokens.isEmpty()) {
                log.info("No FCM tokens found for user: {}", userId);
                return;
            }
            
            Map<String, String> data = new HashMap<>();
            data.put("type", "custom_content_completed");
            data.put("requestId", requestId);
            data.put("contentTitle", contentTitle);
            
            FcmMessageRequest messageRequest = FcmMessageRequest.builder()
                    .title("콘텐츠 처리 완료")
                    .body(String.format("'%s' 콘텐츠가 성공적으로 처리되었습니다.", contentTitle))
                    .data(data)
                    .build();
            
            for (FcmToken token : userTokens) {
                try {
                    fcmMessagingService.sendMessage(token.getFcmToken(), messageRequest);
                    log.info("Sent completion notification to token: {} for user: {}", 
                            token.getDeviceId(), userId);
                } catch (Exception e) {
                    log.error("Failed to send completion notification to token: {} for user: {}", 
                            token.getDeviceId(), userId, e);
                }
            }
            
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
            
            Map<String, String> data = new HashMap<>();
            data.put("type", "custom_content_failed");
            data.put("requestId", requestId);
            data.put("contentTitle", contentTitle);
            data.put("errorMessage", errorMessage);
            
            FcmMessageRequest messageRequest = FcmMessageRequest.builder()
                    .title("콘텐츠 처리 실패")
                    .body(String.format("'%s' 콘텐츠 처리 중 오류가 발생했습니다.", contentTitle))
                    .data(data)
                    .build();
            
            for (FcmToken token : userTokens) {
                try {
                    fcmMessagingService.sendMessage(token.getFcmToken(), messageRequest);
                    log.info("Sent failure notification to token: {} for user: {}", 
                            token.getDeviceId(), userId);
                } catch (Exception e) {
                    log.error("Failed to send failure notification to token: {} for user: {}", 
                            token.getDeviceId(), userId, e);
                }
            }
            
        } catch (Exception e) {
            log.error("Failed to send content failure notification for user: {}, requestId: {}", 
                    userId, requestId, e);
        }
    }

}