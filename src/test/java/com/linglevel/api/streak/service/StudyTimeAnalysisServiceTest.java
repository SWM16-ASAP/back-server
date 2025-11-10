package com.linglevel.api.streak.service;

import com.linglevel.api.streak.entity.DailyCompletion;
import com.linglevel.api.streak.entity.UserStudyReport;
import com.linglevel.api.streak.repository.DailyCompletionRepository;
import com.linglevel.api.streak.repository.UserStudyReportRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("학습 시간 분석 서비스 테스트")
class StudyTimeAnalysisServiceTest {

    @Mock
    private DailyCompletionRepository dailyCompletionRepository;

    @Mock
    private UserStudyReportRepository userStudyReportRepository;

    @InjectMocks
    private StudyTimeAnalysisService service;

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private UserStudyReport testReport;

    @BeforeEach
    void setUp() {
        testReport = new UserStudyReport();
        testReport.setUserId("test-user");
        testReport.setCurrentStreak(5);
    }

    @Test
    @DisplayName("DB에 저장된 선호 시간이 있으면 바로 반환")
    void getPreferredStudyHour_WithExistingValue_ReturnsStoredValue() {
        // given
        testReport.setPreferredStudyHour(14);
        when(userStudyReportRepository.findByUserId("test-user"))
                .thenReturn(Optional.of(testReport));

        // when
        Optional<Integer> result = service.getPreferredStudyHour("test-user");

        // then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(14);
        verify(dailyCompletionRepository, never()).findByUserIdAndCompletionDateAfter(any(), any());
    }

    @Test
    @DisplayName("DB에 저장된 값이 없으면 즉시 계산하여 저장")
    void getPreferredStudyHour_WithoutExistingValue_CalculatesAndSaves() {
        // given
        testReport.setPreferredStudyHour(null);
        when(userStudyReportRepository.findByUserId("test-user"))
                .thenReturn(Optional.of(testReport));

        List<DailyCompletion> completions = createCompletionsAtHour(14, 5);
        when(dailyCompletionRepository.findByUserIdAndCompletionDateAfter(eq("test-user"), any(LocalDate.class)))
                .thenReturn(completions);

        // when
        Optional<Integer> result = service.getPreferredStudyHour("test-user");

        // then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(14);
        verify(userStudyReportRepository).save(argThat(report ->
                report.getPreferredStudyHour() == 14 &&
                report.getPreferredStudyHourUpdatedAt() != null
        ));
    }

    @Test
    @DisplayName("가장 빈번한 학습 시간대 계산 - 단일 시간대")
    void calculateAndSavePreferredStudyHour_SingleFrequentHour() {
        // given
        when(userStudyReportRepository.findByUserId("test-user"))
                .thenReturn(Optional.of(testReport));

        // 14시에 5번, 15시에 2번 학습
        List<DailyCompletion> completions = new ArrayList<>();
        completions.addAll(createCompletionsAtHour(14, 5));
        completions.addAll(createCompletionsAtHour(15, 2));

        when(dailyCompletionRepository.findByUserIdAndCompletionDateAfter(eq("test-user"), any(LocalDate.class)))
                .thenReturn(completions);

        // when
        Optional<Integer> result = service.calculateAndSavePreferredStudyHour("test-user");

        // then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(14);
    }

    @Test
    @DisplayName("가장 빈번한 학습 시간대 계산 - 여러 시간대")
    void calculateAndSavePreferredStudyHour_MultipleHours() {
        // given
        when(userStudyReportRepository.findByUserId("test-user"))
                .thenReturn(Optional.of(testReport));

        // 20시에 3번, 14시에 2번, 15시에 2번
        List<DailyCompletion> completions = new ArrayList<>();
        completions.addAll(createCompletionsAtHour(20, 3));
        completions.addAll(createCompletionsAtHour(14, 2));
        completions.addAll(createCompletionsAtHour(15, 2));

        when(dailyCompletionRepository.findByUserIdAndCompletionDateAfter(eq("test-user"), any(LocalDate.class)))
                .thenReturn(completions);

        // when
        Optional<Integer> result = service.calculateAndSavePreferredStudyHour("test-user");

        // then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(20);
    }

    @Test
    @DisplayName("학습 데이터가 없으면 Empty 반환")
    void calculateAndSavePreferredStudyHour_NoData_ReturnsEmpty() {
        // given
        when(userStudyReportRepository.findByUserId("test-user"))
                .thenReturn(Optional.of(testReport));
        when(dailyCompletionRepository.findByUserIdAndCompletionDateAfter(eq("test-user"), any(LocalDate.class)))
                .thenReturn(List.of());

        // when
        Optional<Integer> result = service.calculateAndSavePreferredStudyHour("test-user");

        // then
        assertThat(result).isEmpty();
        verify(userStudyReportRepository, never()).save(any());
    }

    @Test
    @DisplayName("사용자가 없으면 Empty 반환")
    void getPreferredStudyHour_UserNotFound_ReturnsEmpty() {
        // given
        when(userStudyReportRepository.findByUserId("non-existent-user"))
                .thenReturn(Optional.empty());

        // when
        Optional<Integer> result = service.getPreferredStudyHour("non-existent-user");

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("UTC 시각을 KST로 정확히 변환하여 계산")
    void calculateAndSavePreferredStudyHour_UtcToKstConversion() {
        // given
        when(userStudyReportRepository.findByUserId("test-user"))
                .thenReturn(Optional.of(testReport));

        // UTC 05:00 = KST 14:00
        Instant utcTime = ZonedDateTime.of(2025, 1, 10, 5, 0, 0, 0, ZoneId.of("UTC")).toInstant();
        List<DailyCompletion> completions = createCompletionsAtInstant(utcTime, 3);

        when(dailyCompletionRepository.findByUserIdAndCompletionDateAfter(eq("test-user"), any(LocalDate.class)))
                .thenReturn(completions);

        // when
        Optional<Integer> result = service.calculateAndSavePreferredStudyHour("test-user");

        // then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(14); // KST 14시여야 함
    }

    /**
     * 특정 KST 시간에 n번 학습한 DailyCompletion 생성
     */
    private List<DailyCompletion> createCompletionsAtHour(int kstHour, int count) {
        List<DailyCompletion> completions = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            DailyCompletion completion = new DailyCompletion();
            completion.setUserId("test-user");
            completion.setCompletionDate(LocalDate.now(KST).minusDays(i));

            // KST 시간 -> UTC로 변환
            ZonedDateTime kstTime = ZonedDateTime.of(2025, 1, 10, kstHour, 0, 0, 0, KST);
            Instant utcInstant = kstTime.toInstant();

            DailyCompletion.CompletedContent content = DailyCompletion.CompletedContent.builder()
                    .completedAt(utcInstant)
                    .build();

            completion.setCompletedContents(List.of(content));
            completions.add(completion);
        }

        return completions;
    }

    /**
     * 특정 Instant에 n번 학습한 DailyCompletion 생성
     */
    private List<DailyCompletion> createCompletionsAtInstant(Instant instant, int count) {
        List<DailyCompletion> completions = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            DailyCompletion completion = new DailyCompletion();
            completion.setUserId("test-user");
            completion.setCompletionDate(LocalDate.now(KST).minusDays(i));

            DailyCompletion.CompletedContent content = DailyCompletion.CompletedContent.builder()
                    .completedAt(instant)
                    .build();

            completion.setCompletedContents(List.of(content));
            completions.add(completion);
        }

        return completions;
    }
}
