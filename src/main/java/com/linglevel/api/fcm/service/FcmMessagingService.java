package com.linglevel.api.fcm.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.linglevel.api.fcm.dto.FcmMessageRequest;
import com.linglevel.api.fcm.exception.FcmErrorCode;
import com.linglevel.api.fcm.exception.FcmException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class FcmMessagingService {

    private final FirebaseMessaging firebaseMessaging;

    /**
     * 단일 기기로 푸시 알림 전송
     */
    public String sendMessage(String fcmToken, FcmMessageRequest messageRequest) {
        try {
            Message message = Message.builder()
                    .setToken(fcmToken)
                    .setNotification(Notification.builder()
                            .setTitle(messageRequest.getTitle())
                            .setBody(messageRequest.getBody())
                            .setImage(messageRequest.getImageUrl())
                            .build())
                    .putAllData(messageRequest.getData())
                    .build();

            String response = firebaseMessaging.send(message);
            log.info("Successfully sent FCM message: {}", response);
            return response;
        } catch (FirebaseMessagingException e) {
            log.error("Failed to send FCM message to token: {}, error: {}", fcmToken, e.getMessage());
            throw new FcmException(FcmErrorCode.MESSAGE_SEND_FAILED);
        }
    }

    /**
     * 여러 기기로 동일한 메시지 전송
     */
    public void sendMulticastMessage(java.util.List<String> fcmTokens, FcmMessageRequest messageRequest) {
        // TODO: MulticastMessage 구현
        // 대량 발송시 사용
    }
}