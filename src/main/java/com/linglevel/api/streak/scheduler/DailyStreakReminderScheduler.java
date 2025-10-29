package com.linglevel.api.streak.scheduler;

import com.google.firebase.messaging.BatchResponse;
import com.linglevel.api.fcm.dto.FcmMessageRequest;
import com.linglevel.api.fcm.entity.FcmToken;
import com.linglevel.api.fcm.repository.FcmTokenRepository;
import com.linglevel.api.fcm.service.FcmMessagingService;
import com.linglevel.api.streak.entity.UserStudyReport;
import com.linglevel.api.streak.repository.DailyCompletionRepository;
import com.linglevel.api.streak.repository.UserStudyReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 매일 오전 9시(KST)에 실행되어 학습하지 않은 사용자에게 리마인더 알림을 전송하는 배치 작업
 *
 * 주요 기능:
 * 1. 활성 스트릭이 있는 사용자 조회
 * 2. 오늘 학습을 완료하지 않은 사용자 필터링
 * 3. 해당 사용자들에게 FCM 푸시 알림 전송
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DailyStreakReminderScheduler {

    private final UserStudyReportRepository userStudyReportRepository;
    private final DailyCompletionRepository dailyCompletionRepository;
    private final FcmTokenRepository fcmTokenRepository;
    private final FcmMessagingService fcmMessagingService;

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final String NOTIFICATION_TITLE = "Keep your streak alive!";
    private static final String NOTIFICATION_BODY = "Don't forget to complete your daily learning to maintain your streak.";
    private static final String NOTIFICATION_TYPE = "streak_reminder";
    private static final String CAMPAIGN_ID = "daily_streak_reminder_9am";

    @Scheduled(cron = "0 0 9 * * *", zone = "Asia/Seoul")
    public void sendDailyStreakReminders() {
        Instant startTime = Instant.now();
        LocalDate today = LocalDate.now(KST);

        log.info("[Streak Reminder] Starting daily streak reminder at {} for date: {}", startTime, today);

        int totalActiveUsers = 0;
        int usersWithoutCompletion = 0;
        int usersWithTokens = 0;
        int notificationsSent = 0;
        int notificationsFailed = 0;

        try {
            // 1. 활성 스트릭이 있는 사용자 조회 (currentStreak > 0)
            List<UserStudyReport> activeReports = userStudyReportRepository
                    .findByCurrentStreakGreaterThan(0);
            totalActiveUsers = activeReports.size();

            log.info("[Streak Reminder] Found {} active users with streak > 0", totalActiveUsers);

            if (activeReports.isEmpty()) {
                log.info("[Streak Reminder] No active users found. Skipping notification.");
                return;
            }

            // 2. 오늘 아직 학습을 완료하지 않은 사용자 필터링
            List<String> usersToNotify = new ArrayList<>();
            for (UserStudyReport report : activeReports) {
                boolean hasCompletedToday = dailyCompletionRepository
                        .existsByUserIdAndCompletionDate(report.getUserId(), today);

                if (!hasCompletedToday) {
                    usersToNotify.add(report.getUserId());
                    usersWithoutCompletion++;
                }
            }

            log.info("[Streak Reminder] Found {} users who haven't completed today's streak", usersWithoutCompletion);

            if (usersToNotify.isEmpty()) {
                log.info("[Streak Reminder] All active users have completed today. Skipping notification.");
                return;
            }

            // 3. 사용자별 FCM 토큰 조회 및 알림 전송
            Map<String, List<String>> userTokensMap = new HashMap<>();
            for (String userId : usersToNotify) {
                List<FcmToken> tokens = fcmTokenRepository.findByUserIdAndIsActive(userId, true);
                if (!tokens.isEmpty()) {
                    List<String> fcmTokens = tokens.stream()
                            .map(FcmToken::getFcmToken)
                            .collect(Collectors.toList());
                    userTokensMap.put(userId, fcmTokens);
                    usersWithTokens++;
                }
            }

            log.info("[Streak Reminder] Found {} users with active FCM tokens", usersWithTokens);

            // 4. 알림 메시지 생성 및 전송
            FcmMessageRequest messageRequest = FcmMessageRequest.builder()
                    .title(NOTIFICATION_TITLE)
                    .body(NOTIFICATION_BODY)
                    .type(NOTIFICATION_TYPE)
                    .campaignId(CAMPAIGN_ID)
                    .action("open_app")
                    .build();

            for (Map.Entry<String, List<String>> entry : userTokensMap.entrySet()) {
                String userId = entry.getKey();
                List<String> tokens = entry.getValue();

                try {
                    if (tokens.size() == 1) {
                        // 단일 토큰: sendMessage 사용
                        fcmMessagingService.sendMessage(tokens.get(0), messageRequest);
                        notificationsSent++;
                        log.debug("[Streak Reminder] Sent notification to user: {}", userId);
                    } else {
                        // 여러 토큰: sendMulticastMessage 사용
                        BatchResponse response = fcmMessagingService.sendMulticastMessage(tokens, messageRequest);
                        notificationsSent += response.getSuccessCount();
                        notificationsFailed += response.getFailureCount();
                        log.debug("[Streak Reminder] Sent multicast notification to user: {} - Success: {}, Failed: {}",
                                userId, response.getSuccessCount(), response.getFailureCount());
                    }
                } catch (Exception e) {
                    notificationsFailed++;
                    log.error("[Streak Reminder] Failed to send notification to user: {}", userId, e);
                }
            }

            Instant endTime = Instant.now();
            long durationMillis = java.time.Duration.between(startTime, endTime).toMillis();

            log.info("[Streak Reminder] Completed. Total Active Users: {}, Users Without Completion: {}, " +
                    "Users With Tokens: {}, Notifications Sent: {}, Failed: {}, Duration: {}ms",
                    totalActiveUsers, usersWithoutCompletion, usersWithTokens,
                    notificationsSent, notificationsFailed, durationMillis);

        } catch (Exception e) {
            log.error("[Streak Reminder] Critical error during streak reminder. " +
                    "Active Users: {}, Without Completion: {}, With Tokens: {}, Sent: {}, Failed: {}",
                    totalActiveUsers, usersWithoutCompletion, usersWithTokens,
                    notificationsSent, notificationsFailed, e);
        }
    }
}
