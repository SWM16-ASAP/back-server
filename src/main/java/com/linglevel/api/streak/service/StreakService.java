package com.linglevel.api.streak.service;

import com.linglevel.api.content.common.ContentType;
import com.linglevel.api.streak.dto.StreakResponse;
import com.linglevel.api.streak.entity.DailyCompletion;
import com.linglevel.api.streak.entity.UserStudyReport;
import com.linglevel.api.streak.exception.StreakErrorCode;
import com.linglevel.api.streak.exception.StreakException;
import com.linglevel.api.streak.repository.DailyCompletionRepository;
import com.linglevel.api.streak.repository.UserStudyReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
                report.setCurrentStreak(1);
                report.setStreakStartDate(today);
            }
        }

        if (report.getCurrentStreak() > report.getLongestStreak()) {
            report.setLongestStreak(report.getCurrentStreak());
        }

        report.setLastCompletionDate(today);
        report.setUpdatedAt(Instant.now());
        userStudyReportRepository.save(report);

        saveDailyCompletion(userId, today, contentType, contentId);

        log.info("Streak updated for user: {}. Current streak: {}", userId, report.getCurrentStreak());
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
        // For now, returning a mock value.
        // Real implementation would require aggregating data from all users.
        return 75.5;
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
}