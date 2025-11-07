package com.linglevel.api.content.recommendation.scheduler;

import com.linglevel.api.content.common.ContentCategory;
import com.linglevel.api.content.recommendation.entity.ContentAccessLog;
import com.linglevel.api.content.recommendation.entity.UserCategoryPreference;
import com.linglevel.api.content.recommendation.repository.ContentAccessLogRepository;
import com.linglevel.api.content.recommendation.repository.UserCategoryPreferenceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class UserPreferenceAggregationScheduler {

    private final ContentAccessLogRepository contentAccessLogRepository;
    private final UserCategoryPreferenceRepository userCategoryPreferenceRepository;

    @Scheduled(cron = "0 0 3 * * *")
    public void aggregateUserPreferences() {
        Instant startTime = Instant.now();
        log.info("Starting user preference aggregation batch job at {}", startTime);

        int successCount = 0;
        int failureCount = 0;

        try {
            Instant cutoffDate = Instant.now().minus(90, java.time.temporal.ChronoUnit.DAYS);
            List<ContentAccessLog> recentLogs = contentAccessLogRepository.findByAccessedAtAfter(cutoffDate);

            if (recentLogs.isEmpty()) {
                log.info("No recent access logs found. Skipping aggregation.");
                return;
            }

            log.info("Processing {} access logs from {} users",
                    recentLogs.size(),
                    recentLogs.stream().map(ContentAccessLog::getUserId).distinct().count());

            Map<String, List<ContentAccessLog>> logsByUser = recentLogs.stream()
                    .collect(Collectors.groupingBy(ContentAccessLog::getUserId));

            for (Entry<String, List<ContentAccessLog>> entry : logsByUser.entrySet()) {
                String userId = entry.getKey();
                List<ContentAccessLog> userLogs = entry.getValue();

                try {
                    updateUserPreference(userId, userLogs);
                    successCount++;
                } catch (Exception e) {
                    failureCount++;
                    log.error("Failed to update preference for user: {}, logs count: {}",
                            userId, userLogs.size(), e);
                }
            }

            Instant endTime = Instant.now();
            long durationMillis = java.time.Duration.between(startTime, endTime).toMillis();

            log.info("User preference aggregation completed. Success: {}, Failure: {}, Duration: {}ms",
                    successCount, failureCount, durationMillis);

            // 90일 이전 로그 삭제
            deleteOldLogs(cutoffDate);

        } catch (Exception e) {
            log.error("Critical error during user preference aggregation. Success: {}, Failure: {}",
                    successCount, failureCount, e);
        }
    }

    private void updateUserPreference(String userId, List<ContentAccessLog> logs) {
        // 시간 감쇠 가중치 계산
        Instant now = Instant.now();
        Instant sevenDaysAgo = now.minus(7, java.time.temporal.ChronoUnit.DAYS);
        Instant thirtyDaysAgo = now.minus(30, java.time.temporal.ChronoUnit.DAYS);

        Map<ContentCategory, Double> categoryScores = new HashMap<>();
        Map<ContentCategory, Integer> rawCounts = new HashMap<>();

        for (ContentAccessLog log : logs) {
            ContentCategory category = log.getCategory();
            if (category == null) continue;

            // 원본 카운트 증가
            rawCounts.merge(category, 1, Integer::sum);

            // 시간 감쇠 가중치 적용
            double timeDecayWeight = calculateTimeDecayWeight(log.getAccessedAt(), sevenDaysAgo, thirtyDaysAgo);

            // 읽기 시간 가중치 적용
            double readTimeWeight = calculateReadTimeWeight(log.getReadTimeSeconds());

            // 최종 가중치 = 시간 감쇠 × 읽기 시간
            double finalWeight = timeDecayWeight * readTimeWeight;

            categoryScores.merge(category, finalWeight, Double::sum);
        }

        double totalScore = categoryScores.values().stream().mapToDouble(Double::doubleValue).sum();
        if (totalScore > 0) {
            categoryScores.replaceAll((category, score) -> score / totalScore);
        }

        // 1순위 카테고리 계산 (점수가 가장 높은 카테고리)
        ContentCategory primaryCategory = null;
        if (!categoryScores.isEmpty()) {
            primaryCategory = categoryScores.entrySet().stream()
                    .max(Entry.comparingByValue())
                    .map(Entry::getKey)
                    .orElse(null);
        }

        UserCategoryPreference preference = userCategoryPreferenceRepository.findByUserId(userId)
                .orElse(UserCategoryPreference.builder()
                        .userId(userId)
                        .build());

        preference.setPrimaryCategory(primaryCategory);
        preference.setCategoryScores(categoryScores);
        preference.setRawAccessCounts(rawCounts);
        preference.setTotalAccessCount(logs.size());
        preference.setLastUpdatedAt(now);

        userCategoryPreferenceRepository.save(preference);

        // 로그: category 없는 콘텐츠만 본 경우 명시
        if (primaryCategory != null) {
            log.debug("Updated preference for user {}: primaryCategory={}, totalAccess={}",
                    userId, primaryCategory, logs.size());
        } else {
            log.debug("Updated preference for user {} without category (Book/CustomContent only), totalAccess={}",
                    userId, logs.size());
        }
    }

    private double calculateTimeDecayWeight(Instant accessedAt, Instant sevenDaysAgo, Instant thirtyDaysAgo) {
        if (accessedAt.isAfter(sevenDaysAgo)) {
            return 1.0; // 최근 7일: 최대 가중치
        } else if (accessedAt.isAfter(thirtyDaysAgo)) {
            return 0.5; // 7~30일: 중간 가중치
        } else {
            return 0.2; // 30일 이전: 낮은 가중치
        }
    }

    /**
     * 읽기 시간 기반 가중치 계산
     * 읽기 시간이 길수록 콘텐츠에 대한 진정한 관심도가 높다고 판단
     */
    private double calculateReadTimeWeight(Integer readTimeSeconds) {
        if (readTimeSeconds == null || readTimeSeconds < 30) {
            return 0.1; // 30초 미만: 거의 안 읽음 (매우 낮은 관심도)
        } else if (readTimeSeconds < 180) {  // 3분
            return 0.5; // 30초~3분: 짧게 읽음 (낮은 관심도)
        } else if (readTimeSeconds < 600) {  // 10분
            return 1.0; // 3분~10분: 정상적으로 읽음 (보통 관심도)
        } else {
            return 1.5; // 10분 이상: 깊게 읽음 (높은 관심도)
        }
    }

    private void deleteOldLogs(Instant cutoffDate) {
        try {
            Instant deleteBeforeDate = cutoffDate.minus(30, java.time.temporal.ChronoUnit.DAYS); // 120일 이전 로그 삭제
            contentAccessLogRepository.deleteByAccessedAtBefore(deleteBeforeDate);
            log.info("Deleted old access logs before {}", deleteBeforeDate);
        } catch (Exception e) {
            log.error("Error deleting old logs", e);
        }
    }
}
