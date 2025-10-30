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
    private final com.linglevel.api.fcm.service.FcmTokenService fcmTokenService;

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final String NOTIFICATION_TYPE = "streak_reminder";
    private static final String CAMPAIGN_ID = "optimal_timing_streak_reminder";

    // 마일스톤 달성 직전 날짜들 (예: 6일은 7일 마일스톤 직전)
    private static final Set<Integer> MILESTONE_APPROACHING_DAYS = Set.of(2, 6, 13, 29, 49, 99, 364);

    /**
     * 매시간 실행되어 최적 타이밍에 도달한 사용자에게 개인화된 알림 전송
     *
     * 다단계 리마인더 시스템:
     * - Day 1 (23-24h): 오늘도 학습해요
     * - Day 2 (47-48h): 어제는 빼먹었지만 오늘이라면?
     * - Day 3 (71-72h): 다시 돌아와 주세요
     * - Day 4 (95-96h): 마지막 부탁
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
            Instant now = Instant.now();

            // 1. 여러 시간 윈도우에서 사용자 조회
            List<UserStudyReport> allCandidates = new ArrayList<>();

            // Day 1: 23-24시간 전 (첫 번째 리마인더)
            allCandidates.addAll(getUsersInTimeWindow(now, 23, 24));

            // Day 2: 47-48시간 전 (어제 놓침)
            allCandidates.addAll(getUsersInTimeWindow(now, 47, 48));

            // Day 3: 71-72시간 전 (이틀 연속 놓침)
            allCandidates.addAll(getUsersInTimeWindow(now, 71, 72));

            // Day 4: 95-96시간 전 (사흘 연속 놓침 - 마지막)
            allCandidates.addAll(getUsersInTimeWindow(now, 95, 96));

            candidateUsers = allCandidates.size();

            log.info("[Optimal Timing Reminder] Found {} users across all time windows",
                    candidateUsers);

            if (allCandidates.isEmpty()) {
                log.info("[Optimal Timing Reminder] No users found in time windows. Skipping notification.");
                return;
            }

            // 2. 오늘 아직 학습하지 않은 사용자 필터링
            List<UserStudyReport> usersToNotify = new ArrayList<>();
            for (UserStudyReport report : allCandidates) {
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
                StreakReminderMessage messageType = determineMessageType(report, now);
                StreakReminderMessage.Message message = messageType.getRandomMessage(languageCode);

                String title = message.getTitle();
                // 스트릭이 0이면 "새로운 시작"을 표현하기 위해 1로 표시
                int displayStreak = report.getCurrentStreak() > 0 ? report.getCurrentStreak() : 1;
                String body = String.format(message.getBodyFormat(), displayStreak);

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

                        // 개별 응답 처리 - 실패한 토큰 비활성화
                        for (int i = 0; i < response.getResponses().size(); i++) {
                            if (response.getResponses().get(i).isSuccessful()) {
                                notificationsSent++;
                            } else {
                                notificationsFailed++;
                                String failedToken = fcmTokens.get(i);
                                log.warn("[Optimal Timing Reminder] Failed to send to user: {}, token error: {}",
                                        userId, response.getResponses().get(i).getException().getMessage());
                                fcmTokenService.deactivateToken(failedToken);
                            }
                        }

                        log.debug("[Optimal Timing Reminder] Multicast to user: {} - Success: {}, Failed: {}",
                                userId, response.getSuccessCount(), response.getFailureCount());
                    }
                } catch (Exception e) {
                    notificationsFailed += fcmTokens.size();
                    log.error("[Optimal Timing Reminder] Failed to send notification to user: {}", userId, e);
                    if (e instanceof com.linglevel.api.fcm.exception.FcmException) {
                        fcmTokens.forEach(fcmTokenService::deactivateToken);
                    }
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
     * 시간 윈도우 내의 사용자 조회
     *
     * @param now 현재 시간
     * @param hoursAgoStart 시작 시간 (예: 23시간 전)
     * @param hoursAgoEnd 종료 시간 (예: 24시간 전)
     * @return 해당 윈도우의 사용자 리스트
     */
    private List<UserStudyReport> getUsersInTimeWindow(Instant now, int hoursAgoStart, int hoursAgoEnd) {
        Instant windowEnd = now.minus(Duration.ofHours(hoursAgoEnd));
        Instant windowStart = now.minus(Duration.ofHours(hoursAgoStart));

        return userStudyReportRepository.findUsersForOptimalTimingReminder(windowEnd, windowStart);
    }

    /**
     * 사용자의 스트릭 상태와 마지막 학습 시간을 분석하여 적절한 메시지 타입을 결정합니다.
     *
     * 우선순위:
     * 1. 스트릭이 깨진 상태 (currentStreak = 0)
     *    - Day 1 (23-24h): STREAK_LOST_DAY1
     *    - Day 2 (47-48h): STREAK_LOST_DAY2
     *    - Day 3 (71-72h): STREAK_LOST_DAY3
     *    - Day 4 (95-96h): STREAK_LOST_DAY4
     * 2. 스트릭 유지 중 - 시간별 복귀 유도 (Day 2, 3, 4)
     * 3. 스트릭 유지 중 - 일반 유지 메시지 (Day 1)
     *
     * @param report 사용자 학습 리포트
     * @param now 현재 시간
     * @return 적절한 StreakReminderMessage 타입
     */
    private StreakReminderMessage determineMessageType(UserStudyReport report, Instant now) {
        int currentStreak = report.getCurrentStreak();
        Instant lastLearning = report.getLastLearningTimestamp();

        // lastLearningTimestamp가 없으면 기본 메시지
        if (lastLearning == null) {
            return currentStreak == 0
                ? StreakReminderMessage.STREAK_LOST_DAY1
                : StreakReminderMessage.REGULAR_REMINDER;
        }

        // 마지막 학습 후 경과 시간 계산
        long hoursSinceLastLearning = Duration.between(lastLearning, now).toHours();

        // 스트릭이 깨진 경우: 시간별 STREAK_LOST 메시지
        if (currentStreak == 0) {
            if (hoursSinceLastLearning >= 95 && hoursSinceLastLearning < 96) {
                return StreakReminderMessage.STREAK_LOST_DAY4;
            }
            if (hoursSinceLastLearning >= 71 && hoursSinceLastLearning < 72) {
                return StreakReminderMessage.STREAK_LOST_DAY3;
            }
            if (hoursSinceLastLearning >= 47 && hoursSinceLastLearning < 48) {
                return StreakReminderMessage.STREAK_LOST_DAY2;
            }
            if (hoursSinceLastLearning >= 23 && hoursSinceLastLearning < 24) {
                return StreakReminderMessage.STREAK_LOST_DAY1;
            }
            // 기본값
            return StreakReminderMessage.STREAK_LOST_DAY1;
        }

        // 스트릭 유지 중: 시간별 복귀 유도 또는 일반 메시지
        // Day 4 (95-96h): 마지막 부탁
        if (hoursSinceLastLearning >= 95 && hoursSinceLastLearning < 96) {
            return StreakReminderMessage.COMEBACK_DAY4;
        }

        // Day 3 (71-72h): 다시 돌아와 주세요
        if (hoursSinceLastLearning >= 71 && hoursSinceLastLearning < 72) {
            return StreakReminderMessage.COMEBACK_DAY3;
        }

        // Day 2 (47-48h): 어제는 빼먹었지만 오늘이라면?
        if (hoursSinceLastLearning >= 47 && hoursSinceLastLearning < 48) {
            return StreakReminderMessage.COMEBACK_DAY2;
        }

        // Day 1 (23-24h): 현재 스트릭 상태에 따라 결정
        if (hoursSinceLastLearning >= 23 && hoursSinceLastLearning < 24) {
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

        // 기본값: 일반 리마인더
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
