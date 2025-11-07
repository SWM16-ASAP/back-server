package com.linglevel.api.streak.service;

import com.linglevel.api.streak.entity.DailyCompletion;
import com.linglevel.api.streak.entity.FreezeTransaction;
import com.linglevel.api.streak.entity.StreakStatus;
import com.linglevel.api.streak.entity.UserStudyReport;
import com.linglevel.api.streak.repository.DailyCompletionRepository;
import com.linglevel.api.streak.repository.FreezeTransactionRepository;
import com.linglevel.api.streak.repository.UserStudyReportRepository;
import com.linglevel.api.user.ticket.service.TicketService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("StreakService - 프리즈 자동 소비 테스트")
class StreakServiceFreezeAutoConsumeTest {

    @Mock
    private DailyCompletionRepository dailyCompletionRepository;

    @Mock
    private FreezeTransactionRepository freezeTransactionRepository;

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
        testReport.setCurrentStreak(5);
        testReport.setLongestStreak(5);
        testReport.setAvailableFreezes(1);
        testReport.setLastCompletionDate(today.minusDays(2)); // 1일 전에 완료
        testReport.setStreakStartDate(today.minusDays(5));
        testReport.setTotalReadingTimeSeconds(0L);
        testReport.setCreatedAt(Instant.now());
    }

    @Nested
    @DisplayName("1일 놓쳤을 때")
    class OneDayMissed {

        @BeforeEach
        void setUp() {
            testReport.setLastCompletionDate(today.minusDays(2)); // 어제를 놓침
            testReport.setCurrentStreak(5);
            testReport.setAvailableFreezes(1);
        }

        @Test
        @DisplayName("프리즈 1개 있으면 자동 소비하고 스트릭 유지")
        void withOneFreeze_ConsumeFreezeAndMaintainStreak() {
            // given
            LocalDate missedDate = today.minusDays(1);
            when(dailyCompletionRepository.findByUserIdAndCompletionDate(TEST_USER_ID, missedDate))
                    .thenReturn(Optional.empty()); // 아직 처리 안됨

            // when
            boolean wasReset = streakService.processMissedDays(testReport, today);

            // then
            assertThat(wasReset).isFalse(); // 스트릭 유지
            assertThat(testReport.getCurrentStreak()).isEqualTo(5); // 스트릭 유지
            assertThat(testReport.getAvailableFreezes()).isEqualTo(0); // 프리즈 1개 소비

            // FreezeTransaction 저장 확인
            ArgumentCaptor<FreezeTransaction> transactionCaptor = ArgumentCaptor.forClass(FreezeTransaction.class);
            verify(freezeTransactionRepository).save(transactionCaptor.capture());
            FreezeTransaction savedTransaction = transactionCaptor.getValue();
            assertThat(savedTransaction.getUserId()).isEqualTo(TEST_USER_ID);
            assertThat(savedTransaction.getAmount()).isEqualTo(-1); // 소비

            // DailyCompletion 저장 확인
            ArgumentCaptor<DailyCompletion> completionCaptor = ArgumentCaptor.forClass(DailyCompletion.class);
            verify(dailyCompletionRepository).save(completionCaptor.capture());
            DailyCompletion savedCompletion = completionCaptor.getValue();
            assertThat(savedCompletion.getStreakStatus()).isEqualTo(StreakStatus.FREEZE_USED);
            assertThat(savedCompletion.getCompletionDate()).isEqualTo(missedDate);
        }

        @Test
        @DisplayName("프리즈 0개면 스트릭 리셋")
        void withNoFreeze_ResetStreak() {
            // given
            testReport.setAvailableFreezes(0);
            LocalDate missedDate = today.minusDays(1);
            when(dailyCompletionRepository.findByUserIdAndCompletionDate(TEST_USER_ID, missedDate))
                    .thenReturn(Optional.empty());

            // when
            boolean wasReset = streakService.processMissedDays(testReport, today);

            // then
            assertThat(wasReset).isTrue(); // 스트릭 리셋됨
            assertThat(testReport.getCurrentStreak()).isEqualTo(0);
            assertThat(testReport.getLastCompletionDate()).isNull();
            assertThat(testReport.getStreakStartDate()).isNull();
            assertThat(testReport.getAvailableFreezes()).isEqualTo(0);

            // 프리즈 트랜잭션 없음
            verify(freezeTransactionRepository, never()).save(any());
            verify(dailyCompletionRepository, never()).save(any());
        }

        @Test
        @DisplayName("이미 처리된 날짜는 중복 소비하지 않음 (멱등성)")
        void alreadyProcessed_NoDoubleConsumption() {
            // given
            LocalDate missedDate = today.minusDays(1);
            DailyCompletion existingCompletion = new DailyCompletion();
            existingCompletion.setStreakStatus(StreakStatus.FREEZE_USED); // 이미 프리즈로 처리됨
            when(dailyCompletionRepository.findByUserIdAndCompletionDate(TEST_USER_ID, missedDate))
                    .thenReturn(Optional.of(existingCompletion));

            int initialFreezes = testReport.getAvailableFreezes();

            // when
            boolean wasReset = streakService.processMissedDays(testReport, today);

            // then
            assertThat(wasReset).isFalse();
            assertThat(testReport.getAvailableFreezes()).isEqualTo(initialFreezes); // 프리즈 소비 안됨

            // 트랜잭션 저장 안됨
            verify(freezeTransactionRepository, never()).save(any());
            verify(dailyCompletionRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("2일 놓쳤을 때")
    class TwoDaysMissed {

        @BeforeEach
        void setUp() {
            testReport.setLastCompletionDate(today.minusDays(3)); // 2일 전, 어제 모두 놓침
            testReport.setCurrentStreak(5);
        }

        @Test
        @DisplayName("프리즈 2개 있으면 모두 소비하고 스트릭 유지")
        void withTwoFreezes_ConsumeAllAndMaintainStreak() {
            // given
            testReport.setAvailableFreezes(2);
            when(dailyCompletionRepository.findByUserIdAndCompletionDate(eq(TEST_USER_ID), any()))
                    .thenReturn(Optional.empty());

            // when
            boolean wasReset = streakService.processMissedDays(testReport, today);

            // then
            assertThat(wasReset).isFalse();
            assertThat(testReport.getCurrentStreak()).isEqualTo(5); // 스트릭 유지
            assertThat(testReport.getAvailableFreezes()).isEqualTo(0); // 프리즈 2개 모두 소비

            // FreezeTransaction 2번 저장
            verify(freezeTransactionRepository, times(2)).save(any());
            // DailyCompletion 2번 저장
            verify(dailyCompletionRepository, times(2)).save(any());
        }

        @Test
        @DisplayName("프리즈 1개만 있으면 1개 소비하고 스트릭 리셋")
        void withOneFreeze_ConsumeOneAndResetStreak() {
            // given
            testReport.setAvailableFreezes(1);
            when(dailyCompletionRepository.findByUserIdAndCompletionDate(eq(TEST_USER_ID), any()))
                    .thenReturn(Optional.empty());

            // when
            boolean wasReset = streakService.processMissedDays(testReport, today);

            // then
            assertThat(wasReset).isTrue(); // 스트릭 리셋
            assertThat(testReport.getCurrentStreak()).isEqualTo(0);
            assertThat(testReport.getAvailableFreezes()).isEqualTo(0); // 프리즈 1개 소비됨

            // FreezeTransaction 1번만 저장 (1개만 있었으므로)
            verify(freezeTransactionRepository, times(1)).save(any());
            verify(dailyCompletionRepository, times(1)).save(any());
        }

        @Test
        @DisplayName("프리즈 0개면 스트릭 리셋")
        void withNoFreeze_ResetStreak() {
            // given
            testReport.setAvailableFreezes(0);
            when(dailyCompletionRepository.findByUserIdAndCompletionDate(eq(TEST_USER_ID), any()))
                    .thenReturn(Optional.empty());

            // when
            boolean wasReset = streakService.processMissedDays(testReport, today);

            // then
            assertThat(wasReset).isTrue();
            assertThat(testReport.getCurrentStreak()).isEqualTo(0);
            assertThat(testReport.getAvailableFreezes()).isEqualTo(0);

            // 프리즈 트랜잭션 없음
            verify(freezeTransactionRepository, never()).save(any());
            verify(dailyCompletionRepository, never()).save(any());
        }

        @Test
        @DisplayName("1일은 처리됨, 1일은 미처리 시 미처리 1일만 소비")
        void oneProcessedOneMissed_ConsumeOnlyUnprocessed() {
            // given
            testReport.setAvailableFreezes(2);
            LocalDate missedDate1 = today.minusDays(2);
            LocalDate missedDate2 = today.minusDays(1);

            // 첫 번째 날은 이미 처리됨
            DailyCompletion existingCompletion = new DailyCompletion();
            existingCompletion.setStreakStatus(StreakStatus.FREEZE_USED);
            when(dailyCompletionRepository.findByUserIdAndCompletionDate(TEST_USER_ID, missedDate1))
                    .thenReturn(Optional.of(existingCompletion));

            // 두 번째 날은 미처리
            when(dailyCompletionRepository.findByUserIdAndCompletionDate(TEST_USER_ID, missedDate2))
                    .thenReturn(Optional.empty());

            // when
            boolean wasReset = streakService.processMissedDays(testReport, today);

            // then
            assertThat(wasReset).isFalse();
            assertThat(testReport.getCurrentStreak()).isEqualTo(5);
            assertThat(testReport.getAvailableFreezes()).isEqualTo(1); // 1개만 소비

            // 미처리 1일만 트랜잭션 저장
            verify(freezeTransactionRepository, times(1)).save(any());
            verify(dailyCompletionRepository, times(1)).save(any());
        }
    }

    @Nested
    @DisplayName("3일 이상 놓쳤을 때")
    class ThreeDaysMissed {

        @BeforeEach
        void setUp() {
            testReport.setLastCompletionDate(today.minusDays(4)); // 3일 놓침
            testReport.setCurrentStreak(10);
        }

        @Test
        @DisplayName("프리즈 2개(최대)로는 부족 -> 2개 소비하고 스트릭 리셋")
        void withTwoFreezes_ConsumeAllButStillReset() {
            // given
            testReport.setAvailableFreezes(2);
            when(dailyCompletionRepository.findByUserIdAndCompletionDate(eq(TEST_USER_ID), any()))
                    .thenReturn(Optional.empty());

            // when
            boolean wasReset = streakService.processMissedDays(testReport, today);

            // then
            assertThat(wasReset).isTrue(); // 스트릭 리셋
            assertThat(testReport.getCurrentStreak()).isEqualTo(0);
            assertThat(testReport.getAvailableFreezes()).isEqualTo(0); // 2개 모두 소비

            // FreezeTransaction 2번 저장 (2개만 있었으므로)
            verify(freezeTransactionRepository, times(2)).save(any());
            verify(dailyCompletionRepository, times(2)).save(any());
        }
    }

    @Nested
    @DisplayName("누락 없을 때")
    class NoMissedDays {

        @Test
        @DisplayName("어제 완료한 경우 처리 안함")
        void completedYesterday_NoProcessing() {
            // given
            testReport.setLastCompletionDate(today.minusDays(1)); // 어제 완료
            testReport.setAvailableFreezes(1);

            // when
            boolean wasReset = streakService.processMissedDays(testReport, today);

            // then
            assertThat(wasReset).isFalse();
            assertThat(testReport.getAvailableFreezes()).isEqualTo(1); // 프리즈 소비 안됨

            verify(freezeTransactionRepository, never()).save(any());
            verify(dailyCompletionRepository, never()).save(any());
        }

        @Test
        @DisplayName("오늘 이미 완료한 경우 처리 안함")
        void completedToday_NoProcessing() {
            // given
            testReport.setLastCompletionDate(today);
            testReport.setAvailableFreezes(1);

            // when
            boolean wasReset = streakService.processMissedDays(testReport, today);

            // then
            assertThat(wasReset).isFalse();
            assertThat(testReport.getAvailableFreezes()).isEqualTo(1);

            verify(freezeTransactionRepository, never()).save(any());
            verify(dailyCompletionRepository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("엣지 케이스")
    class EdgeCases {

        @Test
        @DisplayName("lastCompletionDate가 null이면 처리 안함")
        void nullLastCompletionDate_NoProcessing() {
            // given
            testReport.setLastCompletionDate(null);
            testReport.setAvailableFreezes(1);

            // when
            boolean wasReset = streakService.processMissedDays(testReport, today);

            // then
            assertThat(wasReset).isFalse();
            assertThat(testReport.getAvailableFreezes()).isEqualTo(1);

            verify(freezeTransactionRepository, never()).save(any());
            verify(dailyCompletionRepository, never()).save(any());
        }

        @Test
        @DisplayName("스트릭 0인데 프리즈 있으면 소비만 하고 리셋 처리")
        void zeroStreakWithFreeze_StillConsumes() {
            // given
            testReport.setCurrentStreak(0);
            testReport.setLastCompletionDate(today.minusDays(2));
            testReport.setAvailableFreezes(1);
            when(dailyCompletionRepository.findByUserIdAndCompletionDate(eq(TEST_USER_ID), any()))
                    .thenReturn(Optional.empty());

            // when
            boolean wasReset = streakService.processMissedDays(testReport, today);

            // then - 이미 0이므로 리셋 처리는 안됨
            assertThat(wasReset).isFalse();
            assertThat(testReport.getCurrentStreak()).isEqualTo(0);
            assertThat(testReport.getAvailableFreezes()).isEqualTo(0); // 프리즈는 소비됨
        }
    }
}