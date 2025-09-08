package com.linglevel.api.fcm.service;

import com.google.firebase.messaging.*;
import com.linglevel.api.fcm.dto.FcmMessageRequest;
import com.linglevel.api.fcm.exception.FcmErrorCode;
import com.linglevel.api.fcm.exception.FcmException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class FcmMessagingService {

    private final FirebaseMessaging firebaseMessaging;

    /**
     * 단일 사용자에게 알림 전송
     */
    public String sendMessage(String fcmToken, FcmMessageRequest messageRequest) {
        try {
            Message message = Message.builder()
                    .setToken(fcmToken)
                    .setNotification(Notification.builder()
                            .setTitle(messageRequest.getTitle())
                            .setBody(messageRequest.getBody())
                            .build())
                    .putAllData(messageRequest.getData() != null ? messageRequest.getData() : Map.of())
                    .build();

            String response = firebaseMessaging.send(message);
            log.debug("FCM message sent successfully: {}", response);
            return response;
            
        } catch (FirebaseMessagingException e) {
            log.error("Failed to send FCM message: {}", e.getMessage());
            throw new FcmException(FcmErrorCode.MESSAGE_SEND_FAILED);
        }
    }

    /**
     * 여러 사용자에게 동시 알림 전송 (기본 우선순위)
     */
    public BatchResponse sendMulticastMessage(List<String> fcmTokens, FcmMessageRequest messageRequest) {
        try {
            if (fcmTokens == null || fcmTokens.isEmpty()) {
                throw new FcmException(FcmErrorCode.MESSAGE_SEND_FAILED);
            }

            MulticastMessage message = MulticastMessage.builder()
                    .setNotification(Notification.builder()
                            .setTitle(messageRequest.getTitle())
                            .setBody(messageRequest.getBody())
                            .build())
                    .putAllData(messageRequest.getData() != null ? messageRequest.getData() : Map.of())
                    .addAllTokens(fcmTokens)
                    .build();

            BatchResponse response = firebaseMessaging.sendMulticast(message);
            log.info("Multicast message sent to {} tokens - Success: {}, Failed: {}", 
                     fcmTokens.size(), response.getSuccessCount(), response.getFailureCount());
            
            return response;
            
        } catch (FirebaseMessagingException e) {
            log.error("Failed to send multicast FCM message: {}", e.getMessage());
            throw new FcmException(FcmErrorCode.MESSAGE_SEND_FAILED);
        }
    }

}