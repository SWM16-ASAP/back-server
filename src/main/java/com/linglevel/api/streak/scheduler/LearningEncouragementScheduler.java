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
import com.linglevel.api.streak.service.StudyTimeAnalysisService;
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
import java.util.Optional;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class LearningEncouragementScheduler {

    private final UserStudyReportRepository userStudyReportRepository;
    private final DailyCompletionRepository dailyCompletionRepository;
    private final FcmTokenRepository fcmTokenRepository;
    private final FcmMessagingService fcmMessagingService;
    private final StudyTimeAnalysisService studyTimeAnalysisService;
    private final com.linglevel.api.fcm.service.FcmTokenService fcmTokenService;

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final String NOTIFICATION_TYPE = "learning_encouragement";
    private static final String CAMPAIGN_ID = "learning_encouragement";

    // 새벽 시간대 제외 (조용한 시간)
    private static final int QUIET_HOURS_START = 0;  // 00:00
    private static final int QUIET_HOURS_END = 6;    // 06:00

    /**
     * 매시간 실행: 활성 유저의 평소 학습 시간에 맞춰 개인화된 학습 권장 알림 전송
     * + 이탈 유저 복귀 유도 알림 (Day 1-4)
     * 새벽 시간대(00:00-06:00)는 알림 미발송
     */
    @Scheduled(cron = "0 0 * * * *", zone = "Asia/Seoul")
    public void sendLearningEncouragementNotifications() {
        Instant startTime = Instant.now();
        int currentHour = startTime.atZone(KST).getHour();

        // 새벽 시간대는 알림 미발송
        if (currentHour >= QUIET_HOURS_START && currentHour < QUIET_HOURS_END) {
            log.info("[Learning Encouragement] Skipping quiet hours ({}:00 KST)", currentHour);
            return;
        }

        LocalDate today = LocalDate.now(KST);
        log.info("[Learning Encouragement] Starting at {} (KST {}:00)", startTime, currentHour);

        int activeUserCount = 0;
        int churnedUserCount = 0;
        int usersMatchingTime = 0;
        int usersWithoutCompletion = 0;
        int usersWithTokens = 0;
        int notificationsSent = 0;
        int notificationsFailed = 0;

        try {
            // 1. 활성 유저 알림 처리
            int[] activeResults = processActiveUsers(today, currentHour);
            activeUserCount = activeResults[0];
            usersMatchingTime += activeResults[1];
            usersWithoutCompletion += activeResults[2];
            usersWithTokens += activeResults[3];
            notificationsSent += activeResults[4];
            notificationsFailed += activeResults[5];

            // 2. 이탈 유저 복귀 알림 처리 (Day 1-4)
            int[] churnedResults = processChurnedUsers(today, currentHour, startTime);
            churnedUserCount = churnedResults[0];
            usersMatchingTime += churnedResults[1];
            usersWithoutCompletion += churnedResults[2];
            usersWithTokens += churnedResults[3];
            notificationsSent += churnedResults[4];
            notificationsFailed += churnedResults[5];

            Instant endTime = Instant.now();
            long durationMillis = Duration.between(startTime, endTime).toMillis();

            log.info("[Learning Encouragement] Completed. Active: {}, Churned: {}, Matching Time: {}, " +
                            "Without Completion: {}, With Tokens: {}, Sent: {}, Failed: {}, Duration: {}ms",
                    activeUserCount, churnedUserCount, usersMatchingTime, usersWithoutCompletion, usersWithTokens,
                    notificationsSent, notificationsFailed, durationMillis);

        } catch (Exception e) {
            log.error("[Learning Encouragement] Critical error. " +
                            "Active: {}, Churned: {}, Matching Time: {}, Without Completion: {}, With Tokens: {}, " +
                            "Sent: {}, Failed: {}",
                    activeUserCount, churnedUserCount, usersMatchingTime, usersWithoutCompletion, usersWithTokens,
                    notificationsSent, notificationsFailed, e);
        }
    }

    /**
     * 활성 유저 알림 처리
     * @return [candidateUsers, usersMatchingTime, usersWithoutCompletion, usersWithTokens, notificationsSent, notificationsFailed]
     */
    private int[] processActiveUsers(LocalDate today, int currentHour) {
        int candidateUsers = 0;
        int usersMatchingTime = 0;
        int usersWithoutCompletion = 0;
        int usersWithTokens = 0;
        int notificationsSent = 0;
        int notificationsFailed = 0;

        try {
            // 1. 활성 유저 조회 (currentStreak > 0)
            List<UserStudyReport> activeUsers = userStudyReportRepository.findByCurrentStreakGreaterThan(0);
            candidateUsers = activeUsers.size();

            log.debug("[Learning Encouragement - Active] Found {} active users", candidateUsers);

            // 2. 평소 학습 시간이 현재 시각과 일치하는 사용자 필터링
            for (UserStudyReport report : activeUsers) {
                String userId = report.getUserId();

                // 2-1. 평소 학습 시간 확인 (DB에서 조회)
                Optional<Integer> usualStudyHour = studyTimeAnalysisService.getPreferredStudyHour(userId);
                if (usualStudyHour.isEmpty() || usualStudyHour.get() != currentHour) {
                    continue;
                }
                usersMatchingTime++;

                // 2-2. 오늘 학습 완료 여부 확인
                boolean hasCompletedToday = dailyCompletionRepository
                        .existsByUserIdAndCompletionDate(userId, today);
                if (hasCompletedToday) {
                    continue;
                }
                usersWithoutCompletion++;

                // 2-3. FCM 토큰 조회
                List<FcmToken> tokens = fcmTokenRepository.findByUserIdAndIsActive(userId, true);
                if (tokens.isEmpty()) {
                    log.debug("[Learning Encouragement] No active FCM tokens for user: {}", userId);
                    continue;
                }
                usersWithTokens++;

                List<String> fcmTokens = tokens.stream()
                        .map(FcmToken::getFcmToken)
                        .collect(Collectors.toList());

                // 2-4. 언어 결정
                LanguageCode languageCode = determineLanguageFromTokens(tokens);

                // 2-5. 학습 권장 메시지 생성
                StreakReminderMessage.Message message = StreakReminderMessage.LEARNING_ENCOURAGEMENT
                        .getRandomMessage(languageCode);

                FcmMessageRequest messageRequest = FcmMessageRequest.builder()
                        .title(message.getTitle())
                        .body(message.getBodyFormat())  // 스트릭 수 포맷팅 불필요
                        .type(NOTIFICATION_TYPE)
                        .campaignId(CAMPAIGN_ID)
                        .action("open_app")
                        .build();

                // 2-6. 알림 전송
                try {
                    if (fcmTokens.size() == 1) {
                        fcmMessagingService.sendMessage(fcmTokens.get(0), messageRequest);
                        notificationsSent++;
                        log.debug("[Learning Encouragement - Active] Sent to user: {} (lang: {}, hour: {})",
                                userId, languageCode, currentHour);
                    } else {
                        BatchResponse response = fcmMessagingService.sendMulticastMessage(fcmTokens, messageRequest);

                        for (int i = 0; i < response.getResponses().size(); i++) {
                            if (response.getResponses().get(i).isSuccessful()) {
                                notificationsSent++;
                            } else {
                                notificationsFailed++;
                                String failedToken = fcmTokens.get(i);
                                log.warn("[Learning Encouragement] Failed to send to user: {}, token error: {}",
                                        userId, response.getResponses().get(i).getException().getMessage());
                                fcmTokenService.deactivateToken(failedToken);
                            }
                        }

                        log.debug("[Learning Encouragement] Multicast to user: {} - Success: {}, Failed: {}",
                                userId, response.getSuccessCount(), response.getFailureCount());
                    }
                } catch (Exception e) {
                    notificationsFailed += fcmTokens.size();
                    log.error("[Learning Encouragement - Active] Failed to send notification to user: {}", userId, e);
                    if (e instanceof com.linglevel.api.fcm.exception.FcmException) {
                        fcmTokens.forEach(fcmTokenService::deactivateToken);
                    }
                }
            }
        } catch (Exception e) {
            log.error("[Learning Encouragement - Active] Error processing active users", e);
        }

        return new int[]{candidateUsers, usersMatchingTime, usersWithoutCompletion, usersWithTokens, notificationsSent, notificationsFailed};
    }

    /**
     * 이탈 유저 복귀 알림 처리 (Day 1-4)
     * @return [candidateUsers, usersMatchingTime, usersWithoutCompletion, usersWithTokens, notificationsSent, notificationsFailed]
     */
    private int[] processChurnedUsers(LocalDate today, int currentHour, Instant now) {
        int candidateUsers = 0;
        int usersMatchingTime = 0;
        int usersWithoutCompletion = 0;
        int usersWithTokens = 0;
        int notificationsSent = 0;
        int notificationsFailed = 0;

        try {
            // Day 1-4 시간 윈도우에서 이탈 유저 조회
            List<UserStudyReport> allChurnedCandidates = new ArrayList<>();

            // Day 1: 23-24시간 전
            allChurnedCandidates.addAll(getChurnedUsersInTimeWindow(now, 23, 24));

            // Day 2: 47-48시간 전
            allChurnedCandidates.addAll(getChurnedUsersInTimeWindow(now, 47, 48));

            // Day 3: 71-72시간 전
            allChurnedCandidates.addAll(getChurnedUsersInTimeWindow(now, 71, 72));

            // Day 4: 95-96시간 전
            allChurnedCandidates.addAll(getChurnedUsersInTimeWindow(now, 95, 96));

            candidateUsers = allChurnedCandidates.size();

            log.debug("[Learning Encouragement - Churned] Found {} churned users across all time windows", candidateUsers);

            if (allChurnedCandidates.isEmpty()) {
                return new int[]{0, 0, 0, 0, 0, 0};
            }

            // 각 이탈 유저에 대해 처리
            for (UserStudyReport report : allChurnedCandidates) {
                String userId = report.getUserId();

                // 1. 평소 학습 시간 확인 (DB에서 조회)
                Optional<Integer> usualStudyHour = studyTimeAnalysisService.getPreferredStudyHour(userId);
                if (usualStudyHour.isEmpty() || usualStudyHour.get() != currentHour) {
                    continue;
                }
                usersMatchingTime++;

                // 2. 오늘 학습 완료 여부 확인 (복귀했으면 스킵)
                boolean hasCompletedToday = dailyCompletionRepository
                        .existsByUserIdAndCompletionDate(userId, today);
                if (hasCompletedToday) {
                    continue;
                }
                usersWithoutCompletion++;

                // 3. FCM 토큰 조회
                List<FcmToken> tokens = fcmTokenRepository.findByUserIdAndIsActive(userId, true);
                if (tokens.isEmpty()) {
                    log.debug("[Learning Encouragement - Churned] No active FCM tokens for user: {}", userId);
                    continue;
                }
                usersWithTokens++;

                List<String> fcmTokens = tokens.stream()
                        .map(FcmToken::getFcmToken)
                        .collect(Collectors.toList());

                // 4. 언어 결정
                LanguageCode languageCode = determineLanguageFromTokens(tokens);

                // 5. 메시지 타입 결정 (Day 1-4에 따라)
                StreakReminderMessage messageType = determineChurnedUserMessageType(report, now);
                StreakReminderMessage.Message message = messageType.getRandomMessage(languageCode);

                // 스트릭이 0이므로 표시할 때는 이전 스트릭 사용 (없으면 1)
                int displayStreak = report.getLongestStreak() > 0 ? report.getLongestStreak() : 1;
                String body = message.getBodyFormat().contains("%d")
                        ? String.format(message.getBodyFormat(), displayStreak)
                        : message.getBodyFormat();

                FcmMessageRequest messageRequest = FcmMessageRequest.builder()
                        .title(message.getTitle())
                        .body(body)
                        .type(NOTIFICATION_TYPE)
                        .campaignId(CAMPAIGN_ID + "_churned")
                        .action("open_app")
                        .build();

                // 6. 알림 전송
                try {
                    if (fcmTokens.size() == 1) {
                        fcmMessagingService.sendMessage(fcmTokens.get(0), messageRequest);
                        notificationsSent++;
                        log.debug("[Learning Encouragement - Churned] Sent to user: {} (lang: {}, type: {})",
                                userId, languageCode, messageType);
                    } else {
                        BatchResponse response = fcmMessagingService.sendMulticastMessage(fcmTokens, messageRequest);

                        for (int i = 0; i < response.getResponses().size(); i++) {
                            if (response.getResponses().get(i).isSuccessful()) {
                                notificationsSent++;
                            } else {
                                notificationsFailed++;
                                String failedToken = fcmTokens.get(i);
                                log.warn("[Learning Encouragement - Churned] Failed to send to user: {}, token error: {}",
                                        userId, response.getResponses().get(i).getException().getMessage());
                                fcmTokenService.deactivateToken(failedToken);
                            }
                        }

                        log.debug("[Learning Encouragement - Churned] Multicast to user: {} - Success: {}, Failed: {}",
                                userId, response.getSuccessCount(), response.getFailureCount());
                    }
                } catch (Exception e) {
                    notificationsFailed += fcmTokens.size();
                    log.error("[Learning Encouragement - Churned] Failed to send notification to user: {}", userId, e);
                    if (e instanceof com.linglevel.api.fcm.exception.FcmException) {
                        fcmTokens.forEach(fcmTokenService::deactivateToken);
                    }
                }
            }
        } catch (Exception e) {
            log.error("[Learning Encouragement - Churned] Error processing churned users", e);
        }

        return new int[]{candidateUsers, usersMatchingTime, usersWithoutCompletion, usersWithTokens, notificationsSent, notificationsFailed};
    }

    /**
     * 시간 윈도우 내의 이탈 유저 조회
     */
    private List<UserStudyReport> getChurnedUsersInTimeWindow(Instant now, int hoursAgoStart, int hoursAgoEnd) {
        Instant windowEnd = now.minus(Duration.ofHours(hoursAgoEnd));
        Instant windowStart = now.minus(Duration.ofHours(hoursAgoStart));

        return userStudyReportRepository.findChurnedUsersInTimeWindow(windowStart, windowEnd);
    }

    /**
     * 이탈 유저의 마지막 학습 시간을 기반으로 메시지 타입 결정
     */
    private StreakReminderMessage determineChurnedUserMessageType(UserStudyReport report, Instant now) {
        Instant lastLearning = report.getLastLearningTimestamp();
        if (lastLearning == null) {
            return StreakReminderMessage.STREAK_LOST_DAY1;
        }

        long hoursSinceLastLearning = Duration.between(lastLearning, now).toHours();

        // Day 4 (95-96h)
        if (hoursSinceLastLearning >= 95 && hoursSinceLastLearning < 96) {
            return StreakReminderMessage.STREAK_LOST_DAY4;
        }

        // Day 3 (71-72h)
        if (hoursSinceLastLearning >= 71 && hoursSinceLastLearning < 72) {
            return StreakReminderMessage.STREAK_LOST_DAY3;
        }

        // Day 2 (47-48h)
        if (hoursSinceLastLearning >= 47 && hoursSinceLastLearning < 48) {
            return StreakReminderMessage.STREAK_LOST_DAY2;
        }

        // Day 1 (23-24h)
        return StreakReminderMessage.STREAK_LOST_DAY1;
    }

    /**
     * FcmToken 리스트에서 사용자의 선호 언어를 결정합니다.
     */
    private LanguageCode determineLanguageFromTokens(List<FcmToken> tokens) {
        if (tokens.isEmpty()) {
            return LanguageCode.EN;
        }

        CountryCode countryCode = tokens.get(0).getCountryCode();
        return convertCountryCodeToLanguageCode(countryCode);
    }

    /**
     * CountryCode를 LanguageCode로 변환합니다.
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