package com.linglevel.api.streak.service;

import com.linglevel.api.streak.entity.DailyCompletion;
import com.linglevel.api.streak.entity.StreakStatus;
import com.linglevel.api.streak.entity.UserStudyReport;
import com.linglevel.api.streak.repository.DailyCompletionRepository;
import com.linglevel.api.streak.repository.FreezeTransactionRepository;
import com.linglevel.api.streak.repository.UserStudyReportRepository;
import com.linglevel.api.user.ticket.service.TicketService;
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
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("StreakService - recalculateUserStudyReport 테스트")
class StreakServiceRecalculateTest {

    @Mock
    private UserStudyReportRepository userStudyReportRepository;

    @Mock
    private DailyCompletionRepository dailyCompletionRepository;

    @InjectMocks
    private StreakService streakService;

    private static final String TEST_USER_ID = "test-user-123";
    private static final ZoneId KST_ZONE = ZoneId.of("Asia/Seoul");

    private UserStudyReport testReport;
    private LocalDate today;

    @BeforeEach
    void setUp() {
        today = LocalDate.now(KST_ZONE);
        testReport = new UserStudyReport();
        testReport.setUserId(TEST_USER_ID);
        testReport.setCompletedContentIds(new HashSet<>());
        testReport.setCurrentStreak(0);
        testReport.setLongestStreak(0);
        testReport.setAvailableFreezes(0);
        testReport.setTotalReadingTimeSeconds(0L);
        testReport.setCreatedAt(Instant.now());
    }

    @Test
    @DisplayName("완료 기록이 없으면 모든 값이 초기화된다")
    void recalculate_NoCompletions_ResetsAllValues() {
        // given
        when(userStudyReportRepository.findByUserId(TEST_USER_ID))
                .thenReturn(Optional.of(testReport));
        when(dailyCompletionRepository.findByUserIdOrderByCompletionDateAsc(TEST_USER_ID))
                .thenReturn(List.of());
        when(userStudyReportRepository.save(any(UserStudyReport.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        UserStudyReport result = streakService.recalculateUserStudyReport(TEST_USER_ID);

        // then
        assertThat(result.getCurrentStreak()).isEqualTo(0);
        assertThat(result.getLongestStreak()).isEqualTo(0);
        assertThat(result.getLastCompletionDate()).isNull();
        assertThat(result.getStreakStartDate()).isNull();
        verify(userStudyReportRepository).save(testReport);
    }

    @Test
    @DisplayName("연속 3일 완료 시 currentStreak=3, longestStreak=3")
    void recalculate_ThreeConsecutiveDays_CalculatesCorrectly() {
        // given
        LocalDate day1 = today.minusDays(2);
        LocalDate day2 = today.minusDays(1);
        LocalDate day3 = today;

        List<DailyCompletion> completions = List.of(
                createCompletion(day1, StreakStatus.COMPLETED, 1),
                createCompletion(day2, StreakStatus.COMPLETED, 2),
                createCompletion(day3, StreakStatus.COMPLETED, 3)
        );

        when(userStudyReportRepository.findByUserId(TEST_USER_ID))
                .thenReturn(Optional.of(testReport));
        when(dailyCompletionRepository.findByUserIdOrderByCompletionDateAsc(TEST_USER_ID))
                .thenReturn(completions);
        when(userStudyReportRepository.save(any(UserStudyReport.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        UserStudyReport result = streakService.recalculateUserStudyReport(TEST_USER_ID);

        // then
        assertThat(result.getCurrentStreak()).isEqualTo(3);
        assertThat(result.getLongestStreak()).isEqualTo(3);
        assertThat(result.getLastCompletionDate()).isEqualTo(day3);
        assertThat(result.getStreakStartDate()).isEqualTo(day1);
    }

    @Test
    @DisplayName("프리즈 사용으로 스트릭이 유지된 경우")
    void recalculate_WithFreezeUsed_MaintainsStreak() {
        // given
        LocalDate day1 = today.minusDays(3);
        LocalDate day2 = today.minusDays(2);  // FREEZE_USED
        LocalDate day3 = today.minusDays(1);

        List<DailyCompletion> completions = List.of(
                createCompletion(day1, StreakStatus.COMPLETED, 1),
                createCompletion(day2, StreakStatus.FREEZE_USED, 1),
                createCompletion(day3, StreakStatus.COMPLETED, 2)
        );

        when(userStudyReportRepository.findByUserId(TEST_USER_ID))
                .thenReturn(Optional.of(testReport));
        when(dailyCompletionRepository.findByUserIdOrderByCompletionDateAsc(TEST_USER_ID))
                .thenReturn(completions);
        when(userStudyReportRepository.save(any(UserStudyReport.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        UserStudyReport result = streakService.recalculateUserStudyReport(TEST_USER_ID);

        // then
        assertThat(result.getCurrentStreak()).isEqualTo(2);
        assertThat(result.getLongestStreak()).isEqualTo(2);
        assertThat(result.getLastCompletionDate()).isEqualTo(day3);
        assertThat(result.getStreakStartDate()).isEqualTo(day1);
    }

    @Test
    @DisplayName("MISSED 상태로 스트릭이 끊긴 후 다시 시작")
    void recalculate_WithMissed_ResetsStreak() {
        // given
        LocalDate day1 = today.minusDays(5);
        LocalDate day2 = today.minusDays(4);
        LocalDate day3 = today.minusDays(3);  // MISSED
        LocalDate day4 = today.minusDays(2);
        LocalDate day5 = today.minusDays(1);

        List<DailyCompletion> completions = List.of(
                createCompletion(day1, StreakStatus.COMPLETED, 1),
                createCompletion(day2, StreakStatus.COMPLETED, 2),
                createCompletion(day3, StreakStatus.MISSED, null),
                createCompletion(day4, StreakStatus.COMPLETED, 1),
                createCompletion(day5, StreakStatus.COMPLETED, 2)
        );

        when(userStudyReportRepository.findByUserId(TEST_USER_ID))
                .thenReturn(Optional.of(testReport));
        when(dailyCompletionRepository.findByUserIdOrderByCompletionDateAsc(TEST_USER_ID))
                .thenReturn(completions);
        when(userStudyReportRepository.save(any(UserStudyReport.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        UserStudyReport result = streakService.recalculateUserStudyReport(TEST_USER_ID);

        // then
        assertThat(result.getCurrentStreak()).isEqualTo(2);
        assertThat(result.getLongestStreak()).isEqualTo(2);  // 첫 번째 스트릭도 2였으므로 최장은 2
        assertThat(result.getLastCompletionDate()).isEqualTo(day5);
        assertThat(result.getStreakStartDate()).isEqualTo(day4);
    }

    @Test
    @DisplayName("최장 스트릭이 현재 스트릭보다 길었던 경우")
    void recalculate_LongestStreakInPast_KeepsLongestStreak() {
        // given
        LocalDate day1 = today.minusDays(6);
        LocalDate day2 = today.minusDays(5);
        LocalDate day3 = today.minusDays(4);
        LocalDate day4 = today.minusDays(3);
        LocalDate day5 = today.minusDays(2);  // MISSED
        LocalDate day6 = today.minusDays(1);

        List<DailyCompletion> completions = List.of(
                createCompletion(day1, StreakStatus.COMPLETED, 1),
                createCompletion(day2, StreakStatus.COMPLETED, 2),
                createCompletion(day3, StreakStatus.COMPLETED, 3),
                createCompletion(day4, StreakStatus.COMPLETED, 4),
                createCompletion(day5, StreakStatus.MISSED, null),
                createCompletion(day6, StreakStatus.COMPLETED, 1)
        );

        when(userStudyReportRepository.findByUserId(TEST_USER_ID))
                .thenReturn(Optional.of(testReport));
        when(dailyCompletionRepository.findByUserIdOrderByCompletionDateAsc(TEST_USER_ID))
                .thenReturn(completions);
        when(userStudyReportRepository.save(any(UserStudyReport.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        UserStudyReport result = streakService.recalculateUserStudyReport(TEST_USER_ID);

        // then
        assertThat(result.getCurrentStreak()).isEqualTo(1);
        assertThat(result.getLongestStreak()).isEqualTo(4);  // 과거 최장 기록
        assertThat(result.getLastCompletionDate()).isEqualTo(day6);
        assertThat(result.getStreakStartDate()).isEqualTo(day6);
    }

    @Test
    @DisplayName("연속성이 끊긴 경우 (날짜 간격이 2일 이상)")
    void recalculate_GapInDates_ResetsStreak() {
        // given
        LocalDate day1 = today.minusDays(5);
        LocalDate day2 = today.minusDays(4);
        // day3(today.minusDays(3))에 기록 없음 - 연속성 끊김
        LocalDate day4 = today.minusDays(2);
        LocalDate day5 = today.minusDays(1);

        List<DailyCompletion> completions = List.of(
                createCompletion(day1, StreakStatus.COMPLETED, 1),
                createCompletion(day2, StreakStatus.COMPLETED, 2),
                createCompletion(day4, StreakStatus.COMPLETED, 1),
                createCompletion(day5, StreakStatus.COMPLETED, 2)
        );

        when(userStudyReportRepository.findByUserId(TEST_USER_ID))
                .thenReturn(Optional.of(testReport));
        when(dailyCompletionRepository.findByUserIdOrderByCompletionDateAsc(TEST_USER_ID))
                .thenReturn(completions);
        when(userStudyReportRepository.save(any(UserStudyReport.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        UserStudyReport result = streakService.recalculateUserStudyReport(TEST_USER_ID);

        // then
        assertThat(result.getCurrentStreak()).isEqualTo(2);
        assertThat(result.getLongestStreak()).isEqualTo(2);
        assertThat(result.getLastCompletionDate()).isEqualTo(day5);
        assertThat(result.getStreakStartDate()).isEqualTo(day4);
    }

    @Test
    @DisplayName("마지막 완료일이 어제이고 오늘 기록이 없으면 currentStreak 유지")
    void recalculate_LastCompletionYesterday_MaintainsStreak() {
        // given
        LocalDate day1 = today.minusDays(2);
        LocalDate day2 = today.minusDays(1);
        // 오늘은 아직 완료 안 함

        List<DailyCompletion> completions = List.of(
                createCompletion(day1, StreakStatus.COMPLETED, 1),
                createCompletion(day2, StreakStatus.COMPLETED, 2)
        );

        when(userStudyReportRepository.findByUserId(TEST_USER_ID))
                .thenReturn(Optional.of(testReport));
        when(dailyCompletionRepository.findByUserIdOrderByCompletionDateAsc(TEST_USER_ID))
                .thenReturn(completions);
        when(userStudyReportRepository.save(any(UserStudyReport.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        UserStudyReport result = streakService.recalculateUserStudyReport(TEST_USER_ID);

        // then
        assertThat(result.getCurrentStreak()).isEqualTo(2);
        assertThat(result.getLongestStreak()).isEqualTo(2);
        assertThat(result.getLastCompletionDate()).isEqualTo(day2);
        assertThat(result.getStreakStartDate()).isEqualTo(day1);
    }

    @Test
    @DisplayName("마지막 완료일이 2일 전이면 currentStreak=0으로 리셋")
    void recalculate_LastCompletionTwoDaysAgo_ResetsStreak() {
        // given
        LocalDate day1 = today.minusDays(3);
        LocalDate day2 = today.minusDays(2);
        // 어제와 오늘 기록 없음

        List<DailyCompletion> completions = List.of(
                createCompletion(day1, StreakStatus.COMPLETED, 1),
                createCompletion(day2, StreakStatus.COMPLETED, 2)
        );

        when(userStudyReportRepository.findByUserId(TEST_USER_ID))
                .thenReturn(Optional.of(testReport));
        when(dailyCompletionRepository.findByUserIdOrderByCompletionDateAsc(TEST_USER_ID))
                .thenReturn(completions);
        when(userStudyReportRepository.save(any(UserStudyReport.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        UserStudyReport result = streakService.recalculateUserStudyReport(TEST_USER_ID);

        // then
        assertThat(result.getCurrentStreak()).isEqualTo(0);
        assertThat(result.getLongestStreak()).isEqualTo(2);
        assertThat(result.getLastCompletionDate()).isEqualTo(day2);
        assertThat(result.getStreakStartDate()).isNull();
    }

    @Test
    @DisplayName("복잡한 시나리오: 프리즈 + MISSED + 여러 스트릭 구간")
    void recalculate_ComplexScenario_CalculatesCorrectly() {
        // given
        LocalDate day1 = today.minusDays(10);
        LocalDate day2 = today.minusDays(9);
        LocalDate day3 = today.minusDays(8);
        LocalDate day4 = today.minusDays(7);   // FREEZE_USED
        LocalDate day5 = today.minusDays(6);
        LocalDate day6 = today.minusDays(5);   // MISSED - 스트릭 끊김
        LocalDate day7 = today.minusDays(4);
        LocalDate day8 = today.minusDays(3);
        LocalDate day9 = today.minusDays(2);
        LocalDate day10 = today.minusDays(1);
        LocalDate day11 = today;

        List<DailyCompletion> completions = List.of(
                createCompletion(day1, StreakStatus.COMPLETED, 1),
                createCompletion(day2, StreakStatus.COMPLETED, 2),
                createCompletion(day3, StreakStatus.COMPLETED, 3),
                createCompletion(day4, StreakStatus.FREEZE_USED, 3),
                createCompletion(day5, StreakStatus.COMPLETED, 4),
                createCompletion(day6, StreakStatus.MISSED, null),
                createCompletion(day7, StreakStatus.COMPLETED, 1),
                createCompletion(day8, StreakStatus.COMPLETED, 2),
                createCompletion(day9, StreakStatus.COMPLETED, 3),
                createCompletion(day10, StreakStatus.COMPLETED, 4),
                createCompletion(day11, StreakStatus.COMPLETED, 5)
        );

        when(userStudyReportRepository.findByUserId(TEST_USER_ID))
                .thenReturn(Optional.of(testReport));
        when(dailyCompletionRepository.findByUserIdOrderByCompletionDateAsc(TEST_USER_ID))
                .thenReturn(completions);
        when(userStudyReportRepository.save(any(UserStudyReport.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // when
        UserStudyReport result = streakService.recalculateUserStudyReport(TEST_USER_ID);

        // then
        assertThat(result.getCurrentStreak()).isEqualTo(5);  // day7~day11
        assertThat(result.getLongestStreak()).isEqualTo(5);  // 현재 스트릭이 가장 김
        assertThat(result.getLastCompletionDate()).isEqualTo(day11);
        assertThat(result.getStreakStartDate()).isEqualTo(day7);
    }

    private DailyCompletion createCompletion(LocalDate date, StreakStatus status, Integer streakCount) {
        return DailyCompletion.builder()
                .userId(TEST_USER_ID)
                .completionDate(date)
                .streakStatus(status)
                .streakCount(streakCount)
                .firstCompletionCount(0)
                .totalCompletionCount(status == StreakStatus.COMPLETED ? 1 : 0)
                .completedContents(new ArrayList<>())
                .createdAt(Instant.now())
                .build();
    }
}
