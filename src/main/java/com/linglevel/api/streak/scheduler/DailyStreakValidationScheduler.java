package com.linglevel.api.streak.scheduler;

import com.linglevel.api.streak.entity.UserStudyReport;
import com.linglevel.api.streak.repository.UserStudyReportRepository;
import com.linglevel.api.streak.service.StreakService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * 매일 자정(KST)에 실행되어 스트릭 검증 및 프리즈 자동 소모를 처리하는 배치 작업
 *
 * 주요 기능:
 * 1. 어제 학습하지 않은 사용자 감지
 * 2. 프리즈 자동 소모 (있는 경우)
 * 3. 프리즈 없으면 스트릭 리셋
 * 4. FreezeTransaction 기록
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DailyStreakValidationScheduler {

    private final UserStudyReportRepository userStudyReportRepository;
    private final StreakService streakService;

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");

    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    public void validateDailyStreaks() {
        Instant startTime = Instant.now();
        LocalDate today = LocalDate.now(KST);
        LocalDate yesterday = today.minusDays(1);

        log.info("[Streak Validation] Starting daily streak validation at {} for date: {}",
                startTime, yesterday);

        int processedCount = 0;
        int freezeUsedCount = 0;
        int streakResetCount = 0;
        int maintainedCount = 0;

        try {
            List<UserStudyReport> activeReports = userStudyReportRepository
                    .findByCurrentStreakGreaterThan(0);

            log.info("[Streak Validation] Found {} active users with streak > 0", activeReports.size());

            for (UserStudyReport report : activeReports) {
                try {
                    processedCount++;

                    boolean wasReset = streakService.processMissedDays(report, today);

                    if (wasReset) {
                        streakResetCount++;
                        // TODO: FCM 알림 전송 (Phase 2)
                        // notificationService.sendStreakResetNotification(report.getUserId());
                    } else {
                        // 스트릭 유지됨 (어제 완료 또는 프리즈 소진)
                        long daysSinceLastCompletion = ChronoUnit.DAYS.between(
                                report.getLastCompletionDate(), today);

                        if (daysSinceLastCompletion == 1) {
                            maintainedCount++;
                        } else if (daysSinceLastCompletion > 1) {
                            // 프리즈 소진으로 스트릭 유지
                            freezeUsedCount++;
                            // TODO: FCM 알림 전송 (Phase 2)
                            // notificationService.sendFreezeConsumedNotification(report.getUserId());
                        }
                    }

                    report.setUpdatedAt(Instant.now());
                    userStudyReportRepository.save(report);

                } catch (Exception e) {
                    log.error("[Streak Validation] Failed to process user: {}", report.getUserId(), e);
                }
            }

            Instant endTime = Instant.now();
            long durationMillis = java.time.Duration.between(startTime, endTime).toMillis();

            log.info("[Streak Validation] Completed. Processed: {}, Maintained: {}, Freeze Used: {}, Reset: {}, Duration: {}ms",
                    processedCount, maintainedCount, freezeUsedCount, streakResetCount, durationMillis);

        } catch (Exception e) {
            log.error("[Streak Validation] Critical error during streak validation. Processed: {}, Freeze Used: {}, Reset: {}",
                    processedCount, freezeUsedCount, streakResetCount, e);
        }
    }
}
