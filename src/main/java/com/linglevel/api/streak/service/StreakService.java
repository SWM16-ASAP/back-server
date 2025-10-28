package com.linglevel.api.streak.service;

import com.linglevel.api.content.common.ContentType;
import com.linglevel.api.streak.dto.EncouragementMessage;
import com.linglevel.api.streak.dto.FreezeTransactionResponse;
import com.linglevel.api.streak.dto.StreakResponse;
import com.linglevel.api.streak.entity.DailyCompletion;
import com.linglevel.api.streak.entity.StreakStatus;
import com.linglevel.api.streak.entity.UserStudyReport;
import com.linglevel.api.streak.exception.StreakErrorCode;
import com.linglevel.api.streak.exception.StreakException;
import com.linglevel.api.streak.repository.DailyCompletionRepository;
import com.linglevel.api.streak.repository.UserStudyReportRepository;
import com.linglevel.api.user.ticket.service.TicketService;
import com.linglevel.api.streak.repository.FreezeTransactionRepository;
import com.linglevel.api.streak.entity.FreezeTransaction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Collections;

@Service
@RequiredArgsConstructor
@Slf4j
public class StreakService {

    private final UserStudyReportRepository userStudyReportRepository;
    private final DailyCompletionRepository dailyCompletionRepository;
    private final TicketService ticketService;
    private final FreezeTransactionRepository freezeTransactionRepository;

    @Transactional
    public StreakResponse getStreakInfo(String userId) {
        UserStudyReport report = userStudyReportRepository.findByUserId(userId)
                .orElseGet(() -> {
                    UserStudyReport newReport = createNewUserStudyReport(userId);
                    userStudyReportRepository.save(newReport);
                    log.info("Created new UserStudyReport for user: {}", userId);
                    return newReport;
                });

        LocalDate today = getKstToday();
        StreakStatus todayStatus = calculateTodayStatus(userId, today);
        StreakStatus yesterdayStatus = calculateTodayStatus(userId, today.minusDays(1));
        long totalStudyDays = dailyCompletionRepository.countByUserId(userId);
        long totalContentsRead = report.getCompletedContentIds() != null ? report.getCompletedContentIds().size() : 0;

        return StreakResponse.builder()
                .currentStreak(report.getCurrentStreak())
                .todayStatus(todayStatus)
                .yesterdayStatus(yesterdayStatus)
                .longestStreak(report.getLongestStreak())
                .streakStartDate(report.getStreakStartDate())
                .totalStudyDays(totalStudyDays)
                .totalContentsRead(totalContentsRead)
                .availableFreezes(report.getAvailableFreezes())
                .totalReadingTimeSeconds(report.getTotalReadingTimeSeconds())
                .percentile(calculatePercentile(report))
                .encouragementMessage(getEncouragementMessage(report.getCurrentStreak()))
                .build();
    }

    @Transactional
    public boolean updateStreak(String userId, ContentType contentType, String contentId) {
        LocalDate today = getKstToday();

        // 오늘 이미 완료했는지 체크 (중복 방지)
        if (hasCompletedStreakToday(userId, today)) {
            log.info("User {} has already completed a streak today.", userId);
            return false;
        }

        UserStudyReport report = userStudyReportRepository.findByUserId(userId)
                .orElseGet(() -> createNewUserStudyReport(userId));

        if (report.getLastCompletionDate() == null) {
            // 첫 완료
            report.setCurrentStreak(1);
            report.setLongestStreak(1);
            report.setStreakStartDate(today);
        } else {
            long daysBetween = ChronoUnit.DAYS.between(report.getLastCompletionDate(), today);

            if (daysBetween == 1) {
                // 연속 완료 → 스트릭 증가
                report.setCurrentStreak(report.getCurrentStreak() + 1);
            } else if (daysBetween == 0) {
                // 오늘 이미 완료 (hasCompletedStreakToday에서 걸러져야 함)
                log.warn("User {} completed multiple times today. Should have been prevented.", userId);
                return false;
            } else {
                // daysBetween > 1: 배치가 이미 처리했어야 함
                // 서버 다운 등으로 배치 누락된 경우 보완
                log.warn("User {} has {} days gap. Batch should have processed this. Starting new streak.",
                        userId, daysBetween);
                report.setCurrentStreak(1);
                report.setStreakStartDate(today);
            }
        }

        // 최장 기록 갱신
        if (report.getCurrentStreak() > report.getLongestStreak()) {
            report.setLongestStreak(report.getCurrentStreak());
        }

        // 보상 지급 확인 및 적용
        checkAndGrantRewards(report);

        report.setLastCompletionDate(today);
        report.setUpdatedAt(Instant.now());

        saveDailyCompletion(report, today, contentType, contentId);

        userStudyReportRepository.save(report);

        log.info("Streak updated for user: {}. Current streak: {}", userId, report.getCurrentStreak());
        return true;
    }

    private void checkAndGrantRewards(UserStudyReport report) {
        int currentStreak = report.getCurrentStreak();
        String userId = report.getUserId();

        // 프리즈 지급 (5일 주기, 최대 2개 보유)
        if (currentStreak > 0 && currentStreak % 5 == 0 && report.getAvailableFreezes() < 2) {
            report.setAvailableFreezes(report.getAvailableFreezes() + 1);

            FreezeTransaction freezeTransaction = FreezeTransaction.builder()
                    .userId(userId)
                    .amount(1)
                    .description("Reward for " + currentStreak + "-day streak")
                    .createdAt(Instant.now())
                    .build();
            freezeTransactionRepository.save(freezeTransaction);
            log.info("Granted 1 freeze to user {} for {} day streak. User now has {} freezes.", userId, currentStreak, report.getAvailableFreezes());
        }

        // 티켓 지급 (1, 7, 15, 30, 45...)
        boolean shouldGrantTicket = false;
        if (currentStreak == 1 || currentStreak == 7) {
            shouldGrantTicket = true;
        } else if (currentStreak >= 15 && (currentStreak - 15) % 15 == 0) {
            shouldGrantTicket = true;
        }

        if (shouldGrantTicket) {
            String description = "Reward for " + currentStreak + "-day streak";
            ticketService.grantTicket(userId, 1, description);
            log.info("Granted 1 ticket to user {} for {} day streak.", userId, currentStreak);
        }
    }

    public boolean hasCompletedStreakToday(String userId, LocalDate today) {
        return dailyCompletionRepository.existsByUserIdAndCompletionDate(userId, today);
    }

    private UserStudyReport createNewUserStudyReport(String userId) {
        UserStudyReport report = new UserStudyReport();
        report.setUserId(userId);
        report.setCreatedAt(Instant.now());
        return report;
    }

    private void saveDailyCompletion(UserStudyReport report, LocalDate today, ContentType contentType, String contentId) {
        DailyCompletion.CompletedContent completedContent = DailyCompletion.CompletedContent.builder()
                .type(contentType)
                .contentId(contentId)
                .completedAt(Instant.now())
                .build();

        // 전체 기간에서 첫 완료인지 확인
        boolean isFirstCompletion = !report.getCompletedContentIds().contains(contentId);

        // 해당 날짜에 이미 DailyCompletion이 있는지 확인
        DailyCompletion dailyCompletion = dailyCompletionRepository
                .findByUserIdAndCompletionDate(report.getUserId(), today)
                .orElse(null);

        if (dailyCompletion != null) {
            // 기존 레코드가 있는 경우
            if (dailyCompletion.getCompletedContents() == null) {
                dailyCompletion.setCompletedContents(new java.util.ArrayList<>());
            }

            // 총 완료 개수 증가 (복습 포함)
            dailyCompletion.setTotalCompletionCount(dailyCompletion.getTotalCompletionCount() + 1);

            // 첫 완료인 경우만 firstCompletionCount 증가
            if (isFirstCompletion) {
                dailyCompletion.setFirstCompletionCount(dailyCompletion.getFirstCompletionCount() + 1);
                report.getCompletedContentIds().add(contentId);
            }

            // 모든 완료 기록을 completedContents에 추가
            dailyCompletion.getCompletedContents().add(completedContent);
        } else {
            // 새로운 레코드 생성
            dailyCompletion = DailyCompletion.builder()
                    .userId(report.getUserId())
                    .completionDate(today)
                    .firstCompletionCount(isFirstCompletion ? 1 : 0)
                    .totalCompletionCount(1)
                    .completedContents(new java.util.ArrayList<>(Collections.singletonList(completedContent)))
                    .createdAt(Instant.now())
                    .build();

            if (isFirstCompletion) {
                report.getCompletedContentIds().add(contentId);
            }
        }

        dailyCompletionRepository.save(dailyCompletion);
    }

    private LocalDate getKstToday() {
        return LocalDate.now(ZoneId.of("Asia/Seoul"));
    }

    private double calculatePercentile(UserStudyReport report) {
        if (report.getCurrentStreak() == 0) {
            return 0.0;
        }

        long totalUsers = userStudyReportRepository.count();
        if (totalUsers <= 1) {
            return 100.0;
        }

        // Calculate "top X%" - users with higher or equal streak
        long usersWithHigherOrEqualStreak = userStudyReportRepository.countByCurrentStreakGreaterThanEqual(report.getCurrentStreak());

        double percentile = ((double) usersWithHigherOrEqualStreak / totalUsers) * 100;

        // 소수점 첫째 자리까지 반올림
        return Math.round(percentile * 10.0) / 10.0;
    }

    private EncouragementMessage getEncouragementMessage(int currentStreak) {
        String title;
        String body;
        String translation;

        if (currentStreak == 0) {
            title = "시작하세요!";
            body = "Start your first streak!";
            translation = "첫 스트릭을 시작해보세요!";
        } else if (currentStreak < 5) {
            title = currentStreak + "일 연속!";
            body = "Great job! Keep it up!";
            translation = "잘하고 있어요! 계속 꾸준히 학습해보세요.";
        } else if (currentStreak < 10) {
            title = currentStreak + "일 연속!";
            body = "Amazing! " + currentStreak + " days in a row!";
            translation = "벌써 " + currentStreak + "일 연속 스트릭 달성! 대단해요!";
        } else {
            title = currentStreak + "일 연속!";
            body = "Impressive! " + currentStreak + " days in a row!";
            translation = "당신의 꾸준함에 박수를 보냅니다! " + currentStreak + "일 연속 스트릭!";
        }

        return EncouragementMessage.builder()
                .title(title)
                .body(body)
                .translation(translation)
                .build();
    }

    private StreakStatus calculateTodayStatus(String userId, LocalDate today) {
        boolean todayCompleted = dailyCompletionRepository.existsByUserIdAndCompletionDate(userId, today);

        if (todayCompleted) {
            return StreakStatus.COMPLETED;
        }

        // Check if yesterday freeze was used
        boolean yesterdayFreezeUsed = checkYesterdayFreezeUsed(userId, today);
        if (yesterdayFreezeUsed) {
            return StreakStatus.FREEZE_USED;
        }

        return StreakStatus.MISSED;
    }

    private boolean checkYesterdayFreezeUsed(String userId, LocalDate today) {
        LocalDate yesterday = today.minusDays(1);

        // Convert yesterday date to Instant range (00:00:00 ~ 23:59:59 KST)
        ZoneId kst = ZoneId.of("Asia/Seoul");
        Instant yesterdayStart = yesterday.atStartOfDay(kst).toInstant();
        Instant yesterdayEnd = yesterday.plusDays(1).atStartOfDay(kst).toInstant();

        // Check if freeze was consumed (amount = -1) yesterday
        return freezeTransactionRepository.existsByUserIdAndAmountAndCreatedAtBetween(
            userId, -1, yesterdayStart, yesterdayEnd
        );
    }

    @Transactional(readOnly = true)
    public Page<FreezeTransactionResponse> getFreezeTransactions(String userId, int page, int limit) {
        PageRequest pageRequest = PageRequest.of(page - 1, limit);
        Page<FreezeTransaction> transactions = freezeTransactionRepository.findByUserIdOrderByCreatedAtDesc(userId, pageRequest);
        return transactions.map(this::toFreezeTransactionResponse);
    }

    private FreezeTransactionResponse toFreezeTransactionResponse(FreezeTransaction transaction) {
        return FreezeTransactionResponse.builder()
                .id(transaction.getId())
                .amount(transaction.getAmount())
                .description(transaction.getDescription())
                .createdAt(transaction.getCreatedAt())
                .build();
    }
}