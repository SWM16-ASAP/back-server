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

            FcmMessageRequest messageRequest = FcmMessageRequest.builder()
                    .title("Content Ready")
                    .body(String.format("'%s' has been successfully processed.", contentTitle))
                    .type("custom_content_completed")
                    .userId(userId)
                    .action("view_content")
                    .deepLink(contentId != null ? "linglevel:///customContent/" + contentId : "linglevel:///customContent")
                    .additionalData(additionalData)
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

            Map<String, String> additionalData = new HashMap<>();
            additionalData.put("requestId", requestId);
            additionalData.put("contentTitle", contentTitle);
            additionalData.put("errorMessage", errorMessage);

            FcmMessageRequest messageRequest = FcmMessageRequest.builder()
                    .title("Content Processing Failed")
                    .body("An error occurred while processing.")
                    .type("custom_content_failed")
                    .userId(userId)
                    .action("view_chat")
                    .deepLink("linglevel:///import?state=chat")
                    .additionalData(additionalData)
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