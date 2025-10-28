package com.linglevel.api.streak.scheduler;

import com.linglevel.api.streak.entity.FreezeTransaction;
import com.linglevel.api.streak.entity.UserStudyReport;
import com.linglevel.api.streak.repository.DailyCompletionRepository;
import com.linglevel.api.streak.repository.FreezeTransactionRepository;
import com.linglevel.api.streak.repository.UserStudyReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
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
    private final DailyCompletionRepository dailyCompletionRepository;
    private final FreezeTransactionRepository freezeTransactionRepository;

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

                    boolean completedYesterday = dailyCompletionRepository
                            .existsByUserIdAndCompletionDate(report.getUserId(), yesterday);

                    if (completedYesterday) {
                        maintainedCount++;
                        log.debug("[Streak Validation] User {} completed yesterday. Streak maintained: {}",
                                report.getUserId(), report.getCurrentStreak());
                        continue;
                    }

                    if (report.getAvailableFreezes() > 0) {
                        // 프리즈 사용
                        handleFreezeUsage(report, yesterday);
                        freezeUsedCount++;
                        log.debug("[Streak Validation] User {} used freeze. Remaining freezes: {}, Streak: {}",
                                report.getUserId(), report.getAvailableFreezes(), report.getCurrentStreak());
                    } else {
                        // 프리즈 없음 → 스트릭 리셋
                        handleStreakReset(report, yesterday);
                        streakResetCount++;
                        log.debug("[Streak Validation] User {} streak reset. Previous streak: {}",
                                report.getUserId(), report.getCurrentStreak() - 1);
                    }

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

    /**
     * 프리즈 사용 처리
     */
    private void handleFreezeUsage(UserStudyReport report, LocalDate missedDate) {
        // 프리즈 1개 소모
        report.setAvailableFreezes(report.getAvailableFreezes() - 1);
        report.setUpdatedAt(Instant.now());

        userStudyReportRepository.save(report);

        // FreezeTransaction 기록 (음수: 소모)
        FreezeTransaction transaction = FreezeTransaction.builder()
                .userId(report.getUserId())
                .amount(-1)
                .description("Auto-consumed for missed day: " + missedDate)
                .createdAt(Instant.now())
                .build();
        freezeTransactionRepository.save(transaction);

        // TODO: FCM 알림 전송 (Phase 2)
        // notificationService.sendFreezeConsumedNotification(report.getUserId(), missedDate);
    }

    /**
     * 스트릭 리셋 처리
     */
    private void handleStreakReset(UserStudyReport report, LocalDate missedDate) {
        int previousStreak = report.getCurrentStreak();

        // 스트릭 리셋
        report.setCurrentStreak(0);
        report.setLastCompletionDate(null);
        report.setStreakStartDate(null);
        report.setUpdatedAt(Instant.now());

        userStudyReportRepository.save(report);

        // TODO: FCM 알림 전송 (Phase 2)
        // notificationService.sendStreakResetNotification(report.getUserId(), previousStreak, missedDate);

        log.warn("[Streak Validation] User {} streak reset from {} to 0 due to missed day: {}",
                report.getUserId(), previousStreak, missedDate);
    }
}
