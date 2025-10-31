package com.linglevel.api.fcm.service;

import com.linglevel.api.fcm.entity.PushLog;
import com.linglevel.api.fcm.exception.FcmErrorCode;
import com.linglevel.api.fcm.exception.FcmException;
import com.linglevel.api.fcm.repository.PushLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class PushLogService {

    private final PushLogRepository pushLogRepository;

    /**
     * 푸시 알림 송신 로그 저장
     */
    public void logSent(String campaignId, String userId, boolean success) {
        try {
            PushLog pushLog = PushLog.builder()
                    .campaignId(campaignId)
                    .userId(userId)
                    .sentAt(LocalDateTime.now())
                    .sentSuccess(success)
                    .createdAt(LocalDateTime.now())
                    .build();

            pushLogRepository.save(pushLog);
        } catch (Exception e) {
            log.error("Failed to save push log - campaignId: {}, userId: {}", campaignId, userId, e);
            throw new FcmException(FcmErrorCode.PUSH_LOG_SAVE_FAILED);
        }
    }

    /**
     * 푸시 알림 오픈 로그 업데이트
     */
    public void logOpened(String userId, String campaignId, LocalDateTime openedAt) {
        PushLog pushLog = pushLogRepository.findByCampaignIdAndUserId(campaignId, userId)
                .orElseThrow(() -> new FcmException(FcmErrorCode.PUSH_LOG_NOT_FOUND));

        pushLog.setOpenedAt(openedAt);

        try {
            pushLogRepository.save(pushLog);
        } catch (Exception e) {
            log.error("Failed to update push opened log - campaignId: {}, userId: {}", campaignId, userId, e);
            throw new FcmException(FcmErrorCode.PUSH_LOG_SAVE_FAILED);
        }
    }
}
