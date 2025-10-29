package com.linglevel.api.streak.scheduler;

import com.google.firebase.messaging.BatchResponse;
import com.linglevel.api.fcm.dto.FcmMessageRequest;
import com.linglevel.api.fcm.entity.FcmToken;
import com.linglevel.api.fcm.repository.FcmTokenRepository;
import com.linglevel.api.fcm.service.FcmMessagingService;
import com.linglevel.api.i18n.CountryCode;
import com.linglevel.api.i18n.LanguageCode;
import com.linglevel.api.streak.entity.StreakReminderMessage;
import com.linglevel.api.streak.entity.UserStudyReport;
import com.linglevel.api.streak.repository.DailyCompletionRepository;
import com.linglevel.api.streak.repository.UserStudyReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class DailyStreakReminderScheduler {

    private final UserStudyReportRepository userStudyReportRepository;
    private final DailyCompletionRepository dailyCompletionRepository;
    private final FcmTokenRepository fcmTokenRepository;
    private final FcmMessagingService fcmMessagingService;

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final String NOTIFICATION_TYPE = "streak_reminder";
    private static final String CAMPAIGN_ID = "optimal_timing_streak_reminder";

    // 마일스톤 달성 직전 날짜들 (예: 6일은 7일 마일스톤 직전)
    private static final Set<Integer> MILESTONE_APPROACHING_DAYS = Set.of(2, 6, 13, 29, 49, 99, 364);

    /**
     * 매시간 실행되어 최적 타이밍(23.5시간 후)에 도달한 사용자에게 개인화된 알림 전송
     */
    @Scheduled(cron = "0 0 * * * *", zone = "Asia/Seoul")
    public void sendOptimalTimingStreakReminders() {
        Instant startTime = Instant.now();
        LocalDate today = LocalDate.now(KST);

        log.info("[Optimal Timing Reminder] Starting at {}", startTime);

        int candidateUsers = 0;
        int usersWithoutCompletion = 0;
        int usersWithTokens = 0;
        int notificationsSent = 0;
        int notificationsFailed = 0;

        try {
            // 1. 최적 타이밍 사용자 조회 (23~24시간 전에 학습한 사용자)
            Instant now = Instant.now();
            Instant twentyFourHoursAgo = now.minus(Duration.ofHours(24));
            Instant twentyThreeHoursAgo = now.minus(Duration.ofHours(23));

            List<UserStudyReport> candidateReports = userStudyReportRepository
                    .findUsersForOptimalTimingReminder(twentyFourHoursAgo, twentyThreeHoursAgo);
            candidateUsers = candidateReports.size();

            log.info("[Optimal Timing Reminder] Found {} users in optimal timing window (23-24h after last learning)",
                    candidateUsers);

            if (candidateReports.isEmpty()) {
                log.info("[Optimal Timing Reminder] No users found in optimal timing window. Skipping notification.");
                return;
            }

            // 2. 오늘 아직 학습하지 않은 사용자 필터링
            List<UserStudyReport> usersToNotify = new ArrayList<>();
            for (UserStudyReport report : candidateReports) {
                boolean hasCompletedToday = dailyCompletionRepository
                        .existsByUserIdAndCompletionDate(report.getUserId(), today);

                if (!hasCompletedToday) {
                    usersToNotify.add(report);
                    usersWithoutCompletion++;
                }
            }

            log.info("[Optimal Timing Reminder] Found {} users who haven't completed today's lesson",
                    usersWithoutCompletion);

            if (usersToNotify.isEmpty()) {
                log.info("[Optimal Timing Reminder] All candidate users have completed today. Skipping notification.");
                return;
            }

            // 3. 사용자별 개인화된 알림 전송
            for (UserStudyReport report : usersToNotify) {
                String userId = report.getUserId();

                // 3-1. FCM 토큰 조회
                List<FcmToken> tokens = fcmTokenRepository.findByUserIdAndIsActive(userId, true);
                if (tokens.isEmpty()) {
                    log.debug("[Optimal Timing Reminder] No active FCM tokens for user: {}", userId);
                    continue;
                }
                usersWithTokens++;

                List<String> fcmTokens = tokens.stream()
                        .map(FcmToken::getFcmToken)
                        .collect(Collectors.toList());

                // 3-2. 언어 결정 (FcmToken의 countryCode 기반)
                LanguageCode languageCode = determineLanguageFromTokens(tokens);

                // 3-3. 개인화된 메시지 생성
                StreakReminderMessage messageType = determineMessageType(report);
                StreakReminderMessage.Message message = messageType.getRandomMessage(languageCode);

                String title = message.getTitle();
                String body = String.format(message.getBodyFormat(), report.getCurrentStreak());

                FcmMessageRequest messageRequest = FcmMessageRequest.builder()
                        .title(title)
                        .body(body)
                        .type(NOTIFICATION_TYPE)
                        .campaignId(CAMPAIGN_ID)
                        .action("open_app")
                        .build();

                // 3-4. 알림 전송
                try {
                    if (fcmTokens.size() == 1) {
                        fcmMessagingService.sendMessage(fcmTokens.get(0), messageRequest);
                        notificationsSent++;
                        log.debug("[Optimal Timing Reminder] Sent to user: {} (lang: {}, streak: {}, type: {})",
                                userId, languageCode, report.getCurrentStreak(), messageType);
                    } else {
                        BatchResponse response = fcmMessagingService.sendMulticastMessage(fcmTokens, messageRequest);
                        notificationsSent += response.getSuccessCount();
                        notificationsFailed += response.getFailureCount();
                        log.debug("[Optimal Timing Reminder] Multicast to user: {} - Success: {}, Failed: {}",
                                userId, response.getSuccessCount(), response.getFailureCount());
                    }
                } catch (Exception e) {
                    notificationsFailed++;
                    log.error("[Optimal Timing Reminder] Failed to send notification to user: {}", userId, e);
                }
            }

            Instant endTime = Instant.now();
            long durationMillis = Duration.between(startTime, endTime).toMillis();

            log.info("[Optimal Timing Reminder] Completed. Candidates: {}, Without Completion: {}, " +
                            "With Tokens: {}, Sent: {}, Failed: {}, Duration: {}ms",
                    candidateUsers, usersWithoutCompletion, usersWithTokens,
                    notificationsSent, notificationsFailed, durationMillis);

        } catch (Exception e) {
            log.error("[Optimal Timing Reminder] Critical error. " +
                            "Candidates: {}, Without Completion: {}, With Tokens: {}, Sent: {}, Failed: {}",
                    candidateUsers, usersWithoutCompletion, usersWithTokens,
                    notificationsSent, notificationsFailed, e);
        }
    }

    /**
     * 사용자의 스트릭 상태를 분석하여 적절한 메시지 타입을 결정합니다.
     *
     * @param report 사용자 학습 리포트
     * @return 적절한 StreakReminderMessage 타입
     */
    private StreakReminderMessage determineMessageType(UserStudyReport report) {
        int currentStreak = report.getCurrentStreak();

        // 마일스톤 직전 (예: 6일 -> 내일 7일 마일스톤)
        if (MILESTONE_APPROACHING_DAYS.contains(currentStreak)) {
            return StreakReminderMessage.MILESTONE_APPROACHING;
        }

        // 긴 스트릭 (7일 이상)
        if (currentStreak >= 7) {
            return StreakReminderMessage.LONG_STREAK_REMINDER;
        }

        // 일반 리마인더
        return StreakReminderMessage.REGULAR_REMINDER;
    }

    /**
     * FcmToken 리스트에서 사용자의 선호 언어를 결정합니다.
     * 첫 번째 토큰의 countryCode를 기반으로 LanguageCode로 변환합니다.
     *
     * @param tokens 사용자의 FCM 토큰 리스트
     * @return 결정된 LanguageCode (기본값: EN)
     */
    private LanguageCode determineLanguageFromTokens(List<FcmToken> tokens) {
        if (tokens.isEmpty()) {
            return LanguageCode.EN;
        }

        // 첫 번째 토큰의 countryCode 사용
        CountryCode countryCode = tokens.get(0).getCountryCode();
        return convertCountryCodeToLanguageCode(countryCode);
    }

    /**
     * CountryCode를 LanguageCode로 변환합니다.
     *
     * @param countryCode 국가 코드
     * @return 변환된 LanguageCode (기본값: EN)
     */
    private LanguageCode convertCountryCodeToLanguageCode(CountryCode countryCode) {
        if (countryCode == null) {
            return LanguageCode.EN;
        }

        switch (countryCode) {
            case KR:
                return LanguageCode.KO;
            case JP:
                return LanguageCode.JA;
            case US:
            default:
                return LanguageCode.EN;
        }
    }
}
