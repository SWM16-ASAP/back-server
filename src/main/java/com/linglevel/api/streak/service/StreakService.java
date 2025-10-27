package com.linglevel.api.streak.service;

import com.linglevel.api.streak.dto.ContentInfo;
import com.linglevel.api.streak.dto.EncouragementMessage;
import com.linglevel.api.streak.dto.StreakResponse;
import com.linglevel.api.streak.entity.CompletedContent;
import com.linglevel.api.streak.entity.DailyCompletion;
import com.linglevel.api.streak.entity.FreezeTransaction;
import com.linglevel.api.streak.entity.StreakStatus;
import com.linglevel.api.streak.entity.UserStudyReport;
import com.linglevel.api.streak.exception.StreakErrorCode;
import com.linglevel.api.streak.exception.StreakException;
import com.linglevel.api.streak.repository.DailyCompletionRepository;
import com.linglevel.api.streak.repository.FreezeTransactionRepository;
import com.linglevel.api.streak.repository.UserStudyReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class StreakService {

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final int MAX_FREEZE_COUNT = 2;

    private final UserStudyReportRepository reportRepository;
    private final DailyCompletionRepository dailyCompletionRepository;
    private final FreezeTransactionRepository freezeTransactionRepository;

    /**
     * Get streak information for a user
     * @param userId User ID
     * @return Streak information
     */
    public StreakResponse getStreakInfo(String userId) {
        UserStudyReport report = reportRepository.findByUserId(userId)
                .orElseThrow(() -> new StreakException(StreakErrorCode.STREAK_NOT_FOUND));

        LocalDate today = LocalDate.now(KST);
        StreakStatus todayStatus = determineTodayStatus(userId, today);
        long totalStudyDays = dailyCompletionRepository.countByUserId(userId);
        List<DailyCompletion> completions = dailyCompletionRepository.findCompletionCountsByUserId(userId);
        long totalContentsRead = completions.stream()
                .mapToLong(dc -> dc.getCompletionCount() != null ? dc.getCompletionCount() : 0)
                .sum();
        int percentileRank = calculatePercentileRank(report.getCurrentStreak());
        EncouragementMessage encouragementMessage = getEncouragementMessage(
                report.getCurrentStreak(),
                percentileRank
        );

        return StreakResponse.builder()
                .currentStreak(report.getCurrentStreak())
                .todayStatus(todayStatus)
                .longestStreak(report.getLongestStreak())
                .streakStartDate(report.getStreakStartDate())
                .totalStudyDays(totalStudyDays)
                .totalContentsRead(totalContentsRead)
                .freezeCount(report.getFreezeCount())
                .totalReadingTimeSeconds(report.getTotalReadingTimeSeconds())
                .percentileRank(percentileRank)
                .encouragementMessage(encouragementMessage)
                .build();
    }

    /**
     * Check and update streak when content is completed
     * @param userId User ID
     * @param contentInfo Content completion information
     * @return true if streak was increased, false otherwise
     */
    public boolean checkAndUpdateStreak(String userId, ContentInfo contentInfo) {
        validateContentInfo(contentInfo);

        LocalDate completionDate = getCompletionDate();
        validateCompletionDate(completionDate);

        UserStudyReport report = findOrCreateReport(userId);

        if (completionDate.equals(report.getLastCompletionDate())) {
            log.info("User {} already completed today, accumulating reading time only", userId);

            if (contentInfo.getReadingTime() != null) {
                report.setTotalReadingTimeSeconds(
                        report.getTotalReadingTimeSeconds() + contentInfo.getReadingTime()
                );
                report.setUpdatedAt(Instant.now());
                reportRepository.save(report);
            }

            saveDailyCompletion(userId, completionDate, contentInfo);

            return false;
        }

        boolean streakIncreased = false;

        if (report.getLastCompletionDate() == null) {
            log.info("First completion for user: {}", userId);
            report.setCurrentStreak(1);
            report.setLongestStreak(1);
            report.setStreakStartDate(completionDate);
            streakIncreased = true;
        } else {
            long daysDiff = ChronoUnit.DAYS.between(
                    report.getLastCompletionDate(),
                    completionDate
            );

            log.info("Days difference for user {}: {}", userId, daysDiff);

            if (daysDiff == 1) {
                report.setCurrentStreak(report.getCurrentStreak() + 1);
                streakIncreased = true;
            } else if (daysDiff > 1) {
                int daysMissed = (int) daysDiff - 1;

                if (report.getFreezeCount() >= daysMissed) {
                    report.setFreezeCount(report.getFreezeCount() - daysMissed);
                    report.setCurrentStreak(report.getCurrentStreak() + 1);
                    streakIncreased = true;

                    logFreezeUsage(userId, daysMissed);

                    log.info("Used {} freeze(s), streak maintained at: {}", daysMissed, report.getCurrentStreak());
                } else {
                    int oldStreak = report.getCurrentStreak();
                    report.setCurrentStreak(1);
                    report.setStreakStartDate(completionDate);
                    report.setFreezeCount(0);

                    log.warn("Insufficient freezes. Streak reset from {} to 1 for user: {}", oldStreak, userId);
                }
            }
        }

        if (report.getCurrentStreak() > report.getLongestStreak()) {
            report.setLongestStreak(report.getCurrentStreak());
            log.info("New longest streak record for user {}: {}", userId, report.getLongestStreak());
        }

        if (contentInfo.getReadingTime() != null) {
            report.setTotalReadingTimeSeconds(
                    report.getTotalReadingTimeSeconds() + contentInfo.getReadingTime()
            );
        }

        report.setLastCompletionDate(completionDate);
        report.setUpdatedAt(Instant.now());
        reportRepository.save(report);

        checkAndGrantRewards(report);

        saveDailyCompletion(userId, completionDate, contentInfo);

        // 13. TODO: Publish event for notifications
        return streakIncreased;
    }

    /**
     * Calculate percentile rank for a user's streak
     * @param currentStreak User's current streak
     * @return Percentile rank (0-100)
     */
    private Integer calculatePercentileRank(int currentStreak) {
        long totalUsers = reportRepository.count();

        if (totalUsers == 0) {
            return 0;
        }

        long usersWithHigherStreak = reportRepository.countByCurrentStreakGreaterThanEqual(currentStreak);
        return (int) Math.round((double) usersWithHigherStreak / totalUsers * 100);
    }

    /**
     * Determine today's streak status
     * @param userId User ID
     * @param today Today's date
     * @return StreakStatus (COMPLETED, FREEZE_USED, MISSED)
     */
    private StreakStatus determineTodayStatus(String userId, LocalDate today) {
        boolean todayCompleted = dailyCompletionRepository.existsByUserIdAndCompletionDate(userId, today);

        if (todayCompleted) {
            return StreakStatus.COMPLETED;
        }

        LocalDate yesterday = today.minusDays(1);
        boolean yesterdayFreezeUsed = checkYesterdayFreezeUsed(userId, yesterday);

        if (yesterdayFreezeUsed) {
            return StreakStatus.FREEZE_USED;
        }

        return StreakStatus.MISSED;
    }

    /**
     * Check if freeze was used yesterday
     * @param userId User ID
     * @param yesterday Yesterday's date
     * @return true if freeze was auto-used yesterday
     */
    private boolean checkYesterdayFreezeUsed(String userId, LocalDate yesterday) {
        List<FreezeTransaction> transactions = freezeTransactionRepository
                .findByUserIdOrderByCreatedAtDesc(userId);

        return transactions.stream()
                .filter(tx -> tx.getAmount() < 0)  // Consumption only
                .anyMatch(tx -> {
                    LocalDate txDate = tx.getCreatedAt().atZone(KST).toLocalDate();
                    return txDate.equals(yesterday);
                });
    }

    /**
     * Get encouragement message based on streak and percentile rank
     * TODO: Implement DB-based message system
     * - Query EncouragementMessageTemplate by streak range
     * - Filter by priority and select by weight
     * - Support multiple locales (LanguageCode enum)
     * - Replace template variables ({{streak}}, {{percentile}})
     * - Add caching for performance
     *
     * @param currentStreak Current streak count
     * @param percentileRank Percentile rank (0-100)
     * @return EncouragementMessage
     */
    private EncouragementMessage getEncouragementMessage(int currentStreak, int percentileRank) {
        // TODO: Replace with DB query
        // Temporary simple implementation
        String title = currentStreak + " day streak";
        String body = "Keep going!";
        String translation = "계속하세요!";

        return EncouragementMessage.builder()
                .title(title)
                .body(body)
                .translation(translation)
                .build();
    }

    private LocalDate getCompletionDate() {
        return Instant.now().atZone(KST).toLocalDate();
    }

    private void validateCompletionDate(LocalDate completionDate) {
        LocalDate today = LocalDate.now(KST);
        if (completionDate.isAfter(today)) {
            throw new StreakException(StreakErrorCode.INVALID_COMPLETION_DATE);
        }
    }

    private void validateContentInfo(ContentInfo contentInfo) {
        if (contentInfo == null) {
            throw new StreakException(StreakErrorCode.CONTENT_INFO_REQUIRED);
        }
        if (contentInfo.getReadingTime() == null || contentInfo.getReadingTime() < 30) {
            throw new StreakException(StreakErrorCode.INVALID_READING_TIME);
        }
    }

    private UserStudyReport findOrCreateReport(String userId) {
        return reportRepository.findByUserId(userId)
                .orElseGet(() -> {
                    log.info("Creating new UserStudyReport for user: {}", userId);
                    UserStudyReport newReport = UserStudyReport.builder()
                            .userId(userId)
                            .currentStreak(0)
                            .longestStreak(0)
                            .freezeCount(0)
                            .totalReadingTimeSeconds(0L)
                            .createdAt(Instant.now())
                            .updatedAt(Instant.now())
                            .build();
                    return reportRepository.save(newReport);
                });
    }

    private void saveDailyCompletion(String userId, LocalDate completionDate, ContentInfo contentInfo) {
        DailyCompletion dailyCompletion = dailyCompletionRepository
                .findByUserIdAndCompletionDate(userId, completionDate)
                .orElseGet(() -> {
                    log.info("Creating new DailyCompletion for user: {} on date: {}", userId, completionDate);
                    DailyCompletion newCompletion = DailyCompletion.builder()
                            .userId(userId)
                            .completionDate(completionDate)
                            .completionCount(0)
                            .completedContents(new ArrayList<>())
                            .createdAt(Instant.now())
                            .build();
                    return newCompletion;
                });

        dailyCompletion.setCompletionCount(dailyCompletion.getCompletionCount() + 1);

        CompletedContent completedContent = CompletedContent.builder()
                .type(contentInfo.getType())
                .contentId(contentInfo.getContentId())
                .chapterId(contentInfo.getChapterId())
                .completedAt(contentInfo.getCompletedAt())
                .readingTime(contentInfo.getReadingTime())
                .category(contentInfo.getCategory())
                .difficultyLevel(contentInfo.getDifficultyLevel())
                .build();

        dailyCompletion.getCompletedContents().add(completedContent);
        dailyCompletionRepository.save(dailyCompletion);

        log.info("Saved daily completion for user: {} on date: {}, total count: {}",
                userId, completionDate, dailyCompletion.getCompletionCount());
    }

    private void logFreezeUsage(String userId, int daysMissed) {
        log.info("Auto-consuming {} freeze(s) for user: {}", daysMissed, userId);

        FreezeTransaction transaction = FreezeTransaction.builder()
                .userId(userId)
                .amount(-daysMissed)
                .description("Freeze auto-used for " + daysMissed + " missed day(s)")
                .createdAt(Instant.now())
                .build();

        freezeTransactionRepository.save(transaction);
    }

    private void checkAndGrantRewards(UserStudyReport report) {
        int currentDay = report.getCurrentStreak();
        log.info("Checking rewards for user: {} at streak day: {}", report.getUserId(), currentDay);

        // Ticket rewards: 1, 7, 15, 30, 45, 60...
        boolean shouldGrantTicket = false;
        if (currentDay == 1 || currentDay == 7 || currentDay == 15) {
            shouldGrantTicket = true;
        } else if (currentDay > 15 && (currentDay - 15) % 15 == 0) {
            shouldGrantTicket = true;
        }

        if (shouldGrantTicket) {
            // TODO: Integrate with TicketService when available
            log.info("Should grant ticket to user: {} for {} day milestone", report.getUserId(), currentDay);
            // ticketService.addTickets(report.getUserId(), 1, "Streak " + currentDay + " days milestone");
        }

        // Freeze rewards: 5, 10, 15, 20, 25... (every 5 days)
        // Max freeze count: 2
        if (currentDay >= 5 && currentDay % 5 == 0) {
            if (report.getFreezeCount() < MAX_FREEZE_COUNT) {
                report.setFreezeCount(report.getFreezeCount() + 1);

                // Log freeze transaction
                FreezeTransaction freezeReward = FreezeTransaction.builder()
                        .userId(report.getUserId())
                        .amount(1)  // Positive amount = gain
                        .description("Reward for " + currentDay + "-day streak milestone")
                        .createdAt(Instant.now())
                        .build();

                freezeTransactionRepository.save(freezeReward);

                log.info("Granted freeze to user: {} for {} day milestone, total freezes: {}",
                        report.getUserId(), currentDay, report.getFreezeCount());

                // TODO: Integrate with NotificationService when available
                // notificationService.sendFreezeRewardNotification(report.getUserId());
            } else {
                log.info("Freeze limit ({}) reached for user: {}, cannot grant more freezes",
                        MAX_FREEZE_COUNT, report.getUserId());
            }
        }
    }
}
