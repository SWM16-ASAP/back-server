package com.linglevel.api.streak.scheduler;

import com.google.firebase.messaging.BatchResponse;
import com.linglevel.api.fcm.dto.FcmMessageRequest;
import com.linglevel.api.fcm.entity.FcmToken;
import com.linglevel.api.fcm.repository.FcmTokenRepository;
import com.linglevel.api.fcm.service.FcmMessagingService;
import com.linglevel.api.i18n.CountryCode;
import com.linglevel.api.i18n.LanguageCode;
import com.linglevel.api.streak.entity.DailyCompletion;
import com.linglevel.api.streak.entity.FreezeTransaction;
import com.linglevel.api.streak.entity.StreakReminderMessage;
import com.linglevel.api.streak.entity.UserStudyReport;
import com.linglevel.api.streak.repository.DailyCompletionRepository;
import com.linglevel.api.streak.repository.FreezeTransactionRepository;
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
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 스트릭 보호 알림 스케줄러
 * 매일 밤 9시에 실행하여 스트릭이 깨지지 않도록 알림을 전송합니다.
 * - 조건: currentStreak > 0 && 오늘 학습 미완료
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class StreakProtectionScheduler {

    private final UserStudyReportRepository userStudyReportRepository;
    private final DailyCompletionRepository dailyCompletionRepository;
    private final FreezeTransactionRepository freezeTransactionRepository;
    private final FcmTokenRepository fcmTokenRepository;
    private final FcmMessagingService fcmMessagingService;
    private final com.linglevel.api.fcm.service.FcmTokenService fcmTokenService;

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final String NOTIFICATION_TYPE = "streak_protection";
    private static final String CAMPAIGN_ID = "streak_protection";
    private static final int BATCH_SIZE = 500;

    /**
     * 매일 밤 9시에 실행: 스트릭 보호 알림 전송
     */
    @Scheduled(cron = "0 0 21 * * *", zone = "Asia/Seoul")
    public void sendStreakProtectionNotifications() {
        Instant startTime = Instant.now();
        LocalDate today = LocalDate.now(KST);

        log.info("[Streak Protection] Starting notification batch at 21:00 KST for date: {}", today);

        int candidateUsers = 0;
        int usersWithoutCompletion = 0;
        int usersWithTokens = 0;
        int notificationsSent = 0;
        int notificationsFailed = 0;

        try {
            // 1. 현재 스트릭이 있는 모든 활성 사용자 조회 (currentStreak > 0)
            List<UserStudyReport> activeUsers = userStudyReportRepository.findByCurrentStreakGreaterThan(0);
            candidateUsers = activeUsers.size();

            log.info("[Streak Protection] Found {} active users with streak > 0", candidateUsers);

            // 2. 오늘 학습 완료하지 않은 사용자 필터링 및 알림 전송
            for (UserStudyReport report : activeUsers) {
                String userId = report.getUserId();

                // 2-1. 오늘 학습 완료 여부 확인
                boolean hasCompletedToday = dailyCompletionRepository
                        .existsByUserIdAndCompletionDate(userId, today);
                if (hasCompletedToday) {
                    continue; // 이미 학습 완료한 사용자는 스킵
                }
                usersWithoutCompletion++;

                // 2-2. FCM 토큰 조회
                List<FcmToken> tokens = fcmTokenRepository.findByUserIdAndIsActive(userId, true);
                if (tokens.isEmpty()) {
                    log.debug("[Streak Protection] No active FCM tokens for user: {}", userId);
                    continue;
                }
                usersWithTokens++;

                List<String> fcmTokens = tokens.stream()
                        .map(FcmToken::getFcmToken)
                        .collect(Collectors.toList());

                // 2-3. 언어 결정
                LanguageCode languageCode = determineLanguageFromTokens(tokens);

                // 2-4. 어제 프리즈 사용 여부 확인
                boolean usedFreezeYesterday = checkIfFreezeUsedYesterday(userId, today);

                // 2-5. 메시지 타입 결정 (프리즈 사용 여부에 따라)
                StreakReminderMessage messageType = usedFreezeYesterday
                        ? StreakReminderMessage.STREAK_SAVED_BY_FREEZE
                        : StreakReminderMessage.STREAK_PROTECTION;

                StreakReminderMessage.Message message = messageType.getRandomMessage(languageCode);
                String body = String.format(message.getBodyFormat(), report.getCurrentStreak());

                FcmMessageRequest messageRequest = FcmMessageRequest.builder()
                        .title(message.getTitle())
                        .body(body)
                        .type(NOTIFICATION_TYPE)
                        .campaignId(CAMPAIGN_ID)
                        .action("open_app")
                        .build();

                // 2-5. 알림 전송
                try {
                    if (fcmTokens.size() == 1) {
                        fcmMessagingService.sendMessage(fcmTokens.get(0), messageRequest);
                        notificationsSent++;
                        log.debug("[Streak Protection] Sent to user: {} (streak: {}, lang: {}, type: {})",
                                userId, report.getCurrentStreak(), languageCode, messageType);
                    } else {
                        BatchResponse response = fcmMessagingService.sendMulticastMessage(fcmTokens, messageRequest);

                        for (int i = 0; i < response.getResponses().size(); i++) {
                            if (response.getResponses().get(i).isSuccessful()) {
                                notificationsSent++;
                            } else {
                                notificationsFailed++;
                                String failedToken = fcmTokens.get(i);
                                log.warn("[Streak Protection] Failed to send to user: {}, token error: {}",
                                        userId, response.getResponses().get(i).getException().getMessage());
                                fcmTokenService.deactivateToken(failedToken);
                            }
                        }

                        log.debug("[Streak Protection] Multicast to user: {} - Success: {}, Failed: {}",
                                userId, response.getSuccessCount(), response.getFailureCount());
                    }
                } catch (Exception e) {
                    notificationsFailed++;
                    log.warn("[Streak Protection] Failed to send notification to user: {}", userId, e);
                }
            }

            long durationMillis = Duration.between(startTime, Instant.now()).toMillis();

            log.info("[Streak Protection] Completed. Candidates: {}, Without completion: {}, With tokens: {}, " +
                            "Sent: {}, Failed: {}, Duration: {}ms",
                    candidateUsers, usersWithoutCompletion, usersWithTokens,
                    notificationsSent, notificationsFailed, durationMillis);

        } catch (Exception e) {
            log.error("[Streak Protection] Critical error. Candidates: {}, Without completion: {}, Sent: {}, Failed: {}",
                    candidateUsers, usersWithoutCompletion, notificationsSent, notificationsFailed, e);
        }
    }

    /**
     * 어제 프리즈가 사용되었는지 확인합니다.
     * 어제 날짜(00:00 ~ 23:59)에 amount가 -1인 트랜잭션이 있으면 프리즈 사용됨
     */
    private boolean checkIfFreezeUsedYesterday(String userId, LocalDate today) {
        LocalDate yesterday = today.minusDays(1);
        Instant yesterdayStart = yesterday.atStartOfDay(KST).toInstant();
        Instant yesterdayEnd = today.atStartOfDay(KST).toInstant();

        List<FreezeTransaction> transactions = freezeTransactionRepository
                .findByUserIdAndAmountAndCreatedAtBetween(userId, -1, yesterdayStart, yesterdayEnd);

        return !transactions.isEmpty();
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
