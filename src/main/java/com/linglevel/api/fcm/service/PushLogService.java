package com.linglevel.api.fcm.service;

import com.linglevel.api.fcm.entity.PushLog;
import com.linglevel.api.fcm.exception.FcmErrorCode;
import com.linglevel.api.fcm.exception.FcmException;
import com.linglevel.api.fcm.repository.PushLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.OptimisticLockingFailureException;
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
     * 푸시 알림 오픈 로그 업데이트 (낙관적 락을 통한 동시성 제어)
     */
    public void logOpened(String userId, String campaignId, LocalDateTime openedAt) {
        try {
            PushLog pushLog = pushLogRepository.findByCampaignIdAndUserId(campaignId, userId)
                    .orElseThrow(() -> new FcmException(FcmErrorCode.PUSH_LOG_NOT_FOUND));

            // 멱등성 보장: 이미 오픈되었으면 무시
            if (pushLog.getOpenedAt() != null) {
                log.debug("Push already opened, ignoring duplicate - campaignId: {}, userId: {}",
                        campaignId, userId);
                return;
            }

            pushLog.setOpenedAt(openedAt);
            pushLogRepository.save(pushLog);
            log.debug("Logged push opened - campaignId: {}, userId: {}", campaignId, userId);

        } catch (OptimisticLockingFailureException e) {
            // 낙관적 락 충돌: 다른 요청이 먼저 업데이트함 (정상 케이스)
            log.debug("Optimistic lock conflict on push opened - campaignId: {}, userId: {} (already updated by another request)",
                    campaignId, userId);
        } catch (Exception e) {
            log.error("Failed to update push opened log - campaignId: {}, userId: {}", campaignId, userId, e);
            throw new FcmException(FcmErrorCode.PUSH_LOG_SAVE_FAILED);
        }
    }
}
