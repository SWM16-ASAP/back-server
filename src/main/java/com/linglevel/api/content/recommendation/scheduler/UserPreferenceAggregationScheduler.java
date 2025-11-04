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
            double weight = calculateTimeDecayWeight(log.getAccessedAt(), sevenDaysAgo, thirtyDaysAgo);
            categoryScores.merge(category, weight, Double::sum);
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
