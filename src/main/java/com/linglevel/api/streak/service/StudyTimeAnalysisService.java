package com.linglevel.api.streak.service;

import com.linglevel.api.streak.entity.DailyCompletion;
import com.linglevel.api.streak.entity.UserStudyReport;
import com.linglevel.api.streak.repository.DailyCompletionRepository;
import com.linglevel.api.streak.repository.UserStudyReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 사용자의 학습 시간 패턴을 분석하는 서비스
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class StudyTimeAnalysisService {

    private final DailyCompletionRepository dailyCompletionRepository;
    private final UserStudyReportRepository userStudyReportRepository;
    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private static final int ANALYSIS_DAYS = 7; // 최근 7일 분석

    /**
     * DB에 저장된 사용자의 선호 학습 시간을 반환합니다.
     * 값이 없으면 즉시 계산하여 저장합니다.
     */
    public Optional<Integer> getPreferredStudyHour(String userId) {
        Optional<UserStudyReport> reportOpt = userStudyReportRepository.findByUserId(userId);

        if (reportOpt.isEmpty()) {
            return Optional.empty();
        }

        UserStudyReport report = reportOpt.get();

        // DB에 저장된 값이 있으면 반환
        if (report.getPreferredStudyHour() != null) {
            return Optional.of(report.getPreferredStudyHour());
        }

        // 값이 없으면 즉시 계산하여 저장
        return calculateAndSavePreferredStudyHour(userId);
    }

    /**
     * 사용자의 선호 학습 시간을 계산하고 DB에 저장합니다.
     * PreferredStudyHourUpdateScheduler에서 주기적으로 호출됩니다.
     */
    public Optional<Integer> calculateAndSavePreferredStudyHour(String userId) {
        Optional<UserStudyReport> reportOpt = userStudyReportRepository.findByUserId(userId);

        if (reportOpt.isEmpty()) {
            return Optional.empty();
        }

        // 최근 7일 학습 데이터 기반 계산
        Optional<Integer> calculatedHour = calculateMostFrequentStudyHour(userId);

        if (calculatedHour.isPresent()) {
            UserStudyReport report = reportOpt.get();
            report.setPreferredStudyHour(calculatedHour.get());
            report.setPreferredStudyHourUpdatedAt(Instant.now());
            report.setUpdatedAt(Instant.now());
            userStudyReportRepository.save(report);

            log.debug("[StudyTimeAnalysis] Calculated and saved preferred hour for user: {} - {}:00",
                    userId, calculatedHour.get());
        }

        return calculatedHour;
    }

    /**
     * 최근 7일 학습 데이터를 기반으로 가장 빈번한 학습 시간을 계산합니다.
     */
    private Optional<Integer> calculateMostFrequentStudyHour(String userId) {
        LocalDate startDate = LocalDate.now(KST).minusDays(ANALYSIS_DAYS);
        List<DailyCompletion> recentCompletions = dailyCompletionRepository
                .findByUserIdAndCompletionDateAfter(userId, startDate);

        if (recentCompletions.isEmpty()) {
            log.debug("[StudyTimeAnalysis] No recent completions found for user: {}", userId);
            return Optional.empty();
        }

        // 시간대별 학습 빈도 계산
        Map<Integer, Integer> hourFrequency = new HashMap<>();

        for (DailyCompletion completion : recentCompletions) {
            if (completion.getCompletedContents() == null) {
                continue;
            }

            for (DailyCompletion.CompletedContent content : completion.getCompletedContents()) {
                if (content.getCompletedAt() == null) {
                    continue;
                }

                ZonedDateTime completedTime = content.getCompletedAt().atZone(KST);
                int hour = completedTime.getHour();
                hourFrequency.put(hour, hourFrequency.getOrDefault(hour, 0) + 1);
            }
        }

        if (hourFrequency.isEmpty()) {
            log.debug("[StudyTimeAnalysis] No valid completion timestamps for user: {}", userId);
            return Optional.empty();
        }

        // 가장 빈번한 시간대 찾기
        int mostFrequentHour = hourFrequency.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(-1);

        if (mostFrequentHour == -1) {
            return Optional.empty();
        }

        int totalCompletions = hourFrequency.values().stream().mapToInt(Integer::intValue).sum();
        int frequencyCount = hourFrequency.get(mostFrequentHour);

        log.debug("[StudyTimeAnalysis] User: {} | Most frequent hour: {}:00 ({}% of {} completions in last {} days)",
                userId, mostFrequentHour,
                (frequencyCount * 100 / totalCompletions),
                totalCompletions,
                ANALYSIS_DAYS);

        return Optional.of(mostFrequentHour);
    }
}
