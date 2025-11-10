package com.linglevel.api.streak.scheduler;

import com.linglevel.api.streak.entity.UserStudyReport;
import com.linglevel.api.streak.repository.UserStudyReportRepository;
import com.linglevel.api.streak.service.StudyTimeAnalysisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

/**
 * 사용자의 선호 학습 시간(preferredStudyHour)을 주기적으로 재계산하는 스케줄러
 * 매일 새벽 3시에 실행하여 모든 활성 사용자의 학습 패턴을 업데이트합니다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class PreferredStudyHourUpdateScheduler {

    private final UserStudyReportRepository userStudyReportRepository;
    private final StudyTimeAnalysisService studyTimeAnalysisService;

    private static final int BATCH_SIZE = 100; // 한 번에 처리할 사용자 수

    /**
     * 매일 새벽 3시에 실행: 모든 활성 사용자의 선호 학습 시간 재계산
     */
    @Scheduled(cron = "0 0 3 * * *", zone = "Asia/Seoul")
    public void updatePreferredStudyHours() {
        Instant startTime = Instant.now();
        log.info("[Preferred Study Hour Update] Starting batch update");

        int totalUsers = 0;
        int updatedUsers = 0;
        int failedUsers = 0;

        try {
            // 활성 사용자 조회 (currentStreak > 0)
            List<UserStudyReport> activeUsers = userStudyReportRepository.findByCurrentStreakGreaterThan(0);
            totalUsers = activeUsers.size();

            log.info("[Preferred Study Hour Update] Found {} active users to update", totalUsers);

            // 배치로 처리
            for (int i = 0; i < activeUsers.size(); i += BATCH_SIZE) {
                int end = Math.min(i + BATCH_SIZE, activeUsers.size());
                List<UserStudyReport> batch = activeUsers.subList(i, end);

                for (UserStudyReport report : batch) {
                    try {
                        String userId = report.getUserId();

                        // 선호 학습 시간 재계산 및 저장
                        studyTimeAnalysisService.calculateAndSavePreferredStudyHour(userId);
                        updatedUsers++;

                        if (updatedUsers % 100 == 0) {
                            log.info("[Preferred Study Hour Update] Progress: {}/{} users updated",
                                    updatedUsers, totalUsers);
                        }
                    } catch (Exception e) {
                        failedUsers++;
                        log.warn("[Preferred Study Hour Update] Failed to update user: {}",
                                report.getUserId(), e);
                    }
                }
            }

            Instant endTime = Instant.now();

            log.info("[Preferred Study Hour Update] Completed. Total: {}, Updated: {}, Failed: {}, Duration: {}ms",
                    totalUsers, updatedUsers, failedUsers, Duration.between(startTime, endTime).toMillis());

        } catch (Exception e) {
            log.error("[Preferred Study Hour Update] Critical error. Total: {}, Updated: {}, Failed: {}",
                    totalUsers, updatedUsers, failedUsers, e);
        }
    }
}
