package com.linglevel.api.streak.service;

import com.linglevel.api.content.common.ContentType;
import com.linglevel.api.streak.dto.FreezeTransactionResponse;
import com.linglevel.api.streak.dto.StreakResponse;
import com.linglevel.api.streak.entity.DailyCompletion;
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

    @Transactional(readOnly = true)
    public StreakResponse getStreakInfo(String userId) {
        UserStudyReport report = userStudyReportRepository.findByUserId(userId)
                .orElseThrow(() -> new StreakException(StreakErrorCode.STREAK_NOT_FOUND));

        boolean isCompletedToday = hasCompletedStreakToday(userId, getKstToday());

        return StreakResponse.builder()
                .currentStreak(report.getCurrentStreak())
                .longestStreak(report.getLongestStreak())
                .totalReadingTimeSeconds(report.getTotalReadingTimeSeconds())
                .isCompletedToday(isCompletedToday)
                .availableFreezes(report.getAvailableFreezes())
                .percentile(calculatePercentile(report))
                .encouragementMessage(getEncouragementMessage(report.getCurrentStreak()))
                .build();
    }

    @Transactional
    public void updateStreak(String userId, ContentType contentType, String contentId) {
        LocalDate today = getKstToday();
        if (hasCompletedStreakToday(userId, today)) {
            log.info("User {} has already completed a streak today.", userId);
            return;
        }

        UserStudyReport report = userStudyReportRepository.findByUserId(userId)
                .orElseGet(() -> createNewUserStudyReport(userId));

        if (report.getLastCompletionDate() == null) {
            report.setCurrentStreak(1);
            report.setLongestStreak(1);
            report.setStreakStartDate(today);
        } else {
            long daysBetween = ChronoUnit.DAYS.between(report.getLastCompletionDate(), today);
            if (daysBetween == 1) {
                report.setCurrentStreak(report.getCurrentStreak() + 1);
            } else if (daysBetween > 1) {
                handleStreakReset(report, today);
            }
        }

        if (report.getCurrentStreak() > report.getLongestStreak()) {
            report.setLongestStreak(report.getCurrentStreak());
        }

        // 보상 지급 확인 및 적용
        checkAndGrantRewards(report);

        report.setLastCompletionDate(today);
        report.setUpdatedAt(Instant.now());
        userStudyReportRepository.save(report);

        saveDailyCompletion(userId, today, contentType, contentId);

        log.info("Streak updated for user: {}. Current streak: {}", userId, report.getCurrentStreak());
    }

    private void handleStreakReset(UserStudyReport report, LocalDate today) {
        if (report.getAvailableFreezes() > 0) {
            report.setAvailableFreezes(report.getAvailableFreezes() - 1);
            // 스트릭을 유지하기 위해 마지막 완료 날짜를 어제로 "브릿지"합니다.
            // 이렇게 하면 daysBetween이 1이 되어 스트릭이 계속됩니다.
            report.setLastCompletionDate(today.minusDays(1));
            report.setCurrentStreak(report.getCurrentStreak() + 1); // 스트릭 증가

            FreezeTransaction transaction = FreezeTransaction.builder()
                    .userId(report.getUserId())
                    .amount(-1)
                    .description("Used a freeze to maintain streak")
                    .createdAt(Instant.now())
                    .build();
            freezeTransactionRepository.save(transaction);
            log.info("Used 1 freeze for user {}. Remaining freezes: {}", report.getUserId(), report.getAvailableFreezes());
        } else {
            report.setCurrentStreak(1);
            report.setStreakStartDate(today);
            log.info("Resetting streak for user {} due to inactivity and no available freezes.", report.getUserId());
        }
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

    private void saveDailyCompletion(String userId, LocalDate today, ContentType contentType, String contentId) {
        DailyCompletion.CompletedContent completedContent = DailyCompletion.CompletedContent.builder()
                .type(contentType)
                .contentId(contentId)
                .completedAt(Instant.now())
                .build();

        DailyCompletion dailyCompletion = DailyCompletion.builder()
                .userId(userId)
                .completionDate(today)
                .completedContents(Collections.singletonList(completedContent))
                .createdAt(Instant.now())
                .build();

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

        long usersWithLowerStreak = userStudyReportRepository.countByCurrentStreakLessThan(report.getCurrentStreak());

        double percentile = ((double) usersWithLowerStreak / totalUsers) * 100;

        // 소수점 첫째 자리까지 반올림
        return Math.round(percentile * 10.0) / 10.0;
    }

    private String getEncouragementMessage(int currentStreak) {
        if (currentStreak == 0) {
            return "첫 스트릭을 시작해보세요!";
        } else if (currentStreak < 5) {
            return "잘하고 있어요! 계속 꾸준히 학습해보세요.";
        } else if (currentStreak < 10) {
            return "벌써 " + currentStreak + "일 연속 스트릭 달성! 대단해요!";
        } else {
            return "당신의 꾸준함에 박수를 보냅니다! " + currentStreak + "일 연속 스트릭!";
        }
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