package com.linglevel.api.streak.service;

import com.linglevel.api.streak.dto.StreakResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Streak service - Business logic placeholder
 * TODO: Implement core streak logic
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StreakService {

    // TODO: Add repository dependencies
    // private final UserStudyReportRepository reportRepository;
    // private final DailyCompletionRepository dailyCompletionRepository;
    // private final FreezeTransactionRepository freezeTransactionRepository;

    /**
     * Get streak information for a user
     * @param userId User ID
     * @return Streak information
     */
    public StreakResponse getStreakInfo(String userId) {
        // TODO: Implement
        // 1. Get UserStudyReport by userId
        // 2. Check if today is completed (query DailyCompletion)
        // 3. Calculate total study days (count DailyCompletion)
        // 4. Calculate total contents read (sum completionCount)
        // 5. Calculate percentile rank
        // 6. Get encouragement message
        // 7. Return StreakResponse

        throw new UnsupportedOperationException("Not implemented yet");
    }

    /**
     * Check and update streak when content is completed
     * @param userId User ID
     * @param contentInfo Content information (TODO: create ContentInfo DTO)
     * @return true if streak was increased, false otherwise
     */
    public boolean checkAndUpdateStreak(String userId, Object contentInfo) {
        // TODO: Implement according to HELP.md section 11.1
        // 1. Get completion date (KST timezone)
        // 2. Get UserStudyReport
        // 3. Check if already completed today
        // 4. Calculate days difference
        // 5. Handle streak logic (consecutive/freeze/reset)
        // 6. Accumulate reading time
        // 7. Grant rewards if milestone reached
        // 8. Save daily completion log
        // 9. Return streak increased boolean

        throw new UnsupportedOperationException("Not implemented yet");
    }

    /**
     * Calculate percentile rank for a user's streak
     * @param currentStreak User's current streak
     * @return Percentile rank (0-100)
     */
    private Integer calculatePercentileRank(int currentStreak) {
        // TODO: Implement
        // long totalUsers = reportRepository.count();
        // long usersWithHigherStreak = reportRepository.countByCurrentStreakGreaterThanEqual(currentStreak);
        // return (int) Math.round((double) usersWithHigherStreak / totalUsers * 100);

        return 0;
    }
}
