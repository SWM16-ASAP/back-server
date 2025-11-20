package com.linglevel.api.streak.service;

import com.linglevel.api.streak.entity.DailyCompletion;
import com.linglevel.api.streak.entity.FreezeTransaction;
import com.linglevel.api.streak.entity.StreakStatus;
import com.linglevel.api.streak.entity.UserStudyReport;
import com.linglevel.api.streak.repository.DailyCompletionRepository;
import com.linglevel.api.streak.repository.FreezeTransactionRepository;
import com.linglevel.api.streak.repository.UserStudyReportRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("StreakService - recoverStreak TDD 테스트")
class StreakServiceRecoveryTest {

    @Mock
    private UserStudyReportRepository userStudyReportRepository;

    @Mock
    private DailyCompletionRepository dailyCompletionRepository;

    @Mock
    private FreezeTransactionRepository freezeTransactionRepository;

    @InjectMocks
    private StreakService streakService;

    @Captor
    private ArgumentCaptor<List<DailyCompletion>> dailyCompletionListCaptor;

    @Captor
    private ArgumentCaptor<List<FreezeTransaction>> freezeTransactionListCaptor;

    @Captor
    private ArgumentCaptor<UserStudyReport> userStudyReportCaptor;

    private static final String TEST_USER_ID = "test-user-123";
    private static final ZoneId KST_ZONE = ZoneId.of("Asia/Seoul");

    private UserStudyReport testReport;
    private LocalDate today;
    private Map<LocalDate, DailyCompletion> completionMap;

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

        completionMap = new HashMap<>();
    }

    @Test
    @DisplayName("시나리오 1: 단순 MISSED 1일 복구")
    void recoverStreak_SimpleMissedDay_CreatesCompletion() {
        // given
        LocalDate day1 = today.minusDays(2);
        LocalDate day2 = today.minusDays(1);  // MISSED → 복구 대상

        DailyCompletion day1Completion = createCompletion(day1, StreakStatus.COMPLETED, 1);
        completionMap.put(day1, day1Completion);

        setupMocks();

        // when
        streakService.recoverStreak(TEST_USER_ID, day2, day2);

        // then
        verify(dailyCompletionRepository, atLeastOnce()).saveAll(dailyCompletionListCaptor.capture());

        // 저장된 모든 DailyCompletion 수집
        List<DailyCompletion> allSaved = new ArrayList<>();
        dailyCompletionListCaptor.getAllValues().forEach(allSaved::addAll);

        // day2가 COMPLETED로 생성되었는지 확인
        DailyCompletion day2Completion = allSaved.stream()
                .filter(dc -> dc.getCompletionDate().equals(day2))
                .findFirst()
                .orElseThrow(() -> new AssertionError("day2 completion not found"));

        assertThat(day2Completion.getStreakStatus()).isEqualTo(StreakStatus.COMPLETED);
        assertThat(day2Completion.getStreakCount()).isEqualTo(2);
        assertThat(day2Completion.getUserId()).isEqualTo(TEST_USER_ID);

        // 프리즈 트랜잭션이 없어야 함 (복구만 했고, FREEZE_USED 아님)
        verify(freezeTransactionRepository, never()).saveAll(any());

        // UserStudyReport가 재계산되어 저장되었는지 확인
        verify(userStudyReportRepository, atLeastOnce()).save(userStudyReportCaptor.capture());
        UserStudyReport savedReport = userStudyReportCaptor.getValue();
        assertThat(savedReport.getCurrentStreak()).isEqualTo(2);
        assertThat(savedReport.getLongestStreak()).isEqualTo(2);
        assertThat(savedReport.getLastCompletionDate()).isEqualTo(day2);
        assertThat(savedReport.getAvailableFreezes()).isEqualTo(0);  // 프리즈 보상 없음
    }

    @Test
    @DisplayName("시나리오 2: FREEZE_USED를 COMPLETED로 복구 + 프리즈 보상")
    void recoverStreak_FreezeUsedDay_ConvertsToCompletedAndRewardsFreeze() {
        // given
        LocalDate day1 = today.minusDays(2);
        LocalDate day2 = today.minusDays(1);  // FREEZE_USED → 복구

        DailyCompletion day1Completion = createCompletion(day1, StreakStatus.COMPLETED, 1);
        DailyCompletion day2Completion = createCompletion(day2, StreakStatus.FREEZE_USED, 1);

        completionMap.put(day1, day1Completion);
        completionMap.put(day2, day2Completion);

        setupMocks();

        // when
        streakService.recoverStreak(TEST_USER_ID, day2, day2);

        // then
        verify(dailyCompletionRepository, atLeastOnce()).saveAll(dailyCompletionListCaptor.capture());

        // 저장된 모든 DailyCompletion 수집
        List<DailyCompletion> allSaved = new ArrayList<>();
        dailyCompletionListCaptor.getAllValues().forEach(allSaved::addAll);

        // day2가 COMPLETED로 변경되었는지 확인
        DailyCompletion day2Updated = allSaved.stream()
                .filter(dc -> dc.getCompletionDate().equals(day2))
                .findFirst()
                .orElseThrow(() -> new AssertionError("day2 completion not found"));

        assertThat(day2Updated.getStreakStatus()).isEqualTo(StreakStatus.COMPLETED);
        assertThat(day2Updated.getStreakCount()).isEqualTo(2);

        // 프리즈 보상 확인 - 1개의 +1 트랜잭션
        verify(freezeTransactionRepository, atLeastOnce()).saveAll(freezeTransactionListCaptor.capture());

        List<FreezeTransaction> allFreezes = new ArrayList<>();
        freezeTransactionListCaptor.getAllValues().forEach(allFreezes::addAll);

        // +1 보상 트랜잭션이 정확히 1개 있어야 함
        List<FreezeTransaction> rewards = allFreezes.stream()
                .filter(tx -> tx.getAmount() == 1)
                .toList();
        assertThat(rewards).hasSize(1);

        FreezeTransaction rewardTx = rewards.get(0);
        assertThat(rewardTx.getUserId()).isEqualTo(TEST_USER_ID);
        assertThat(rewardTx.getDescription()).contains(day2.toString());

        // UserStudyReport 검증
        verify(userStudyReportRepository, atLeastOnce()).save(userStudyReportCaptor.capture());
        UserStudyReport savedReport = userStudyReportCaptor.getValue();
        assertThat(savedReport.getCurrentStreak()).isEqualTo(2);
        assertThat(savedReport.getLongestStreak()).isEqualTo(2);
        // 프리즈: day2 복구 보상 +1 (오늘은 배치에서 처리하므로 제외)
        assertThat(savedReport.getAvailableFreezes()).isEqualTo(1);
    }

    @Test
    @DisplayName("시나리오 3: 연속 MISSED 3일 복구")
    void recoverStreak_MultipleMissedDays_CreatesAllCompletions() {
        // given
        LocalDate day1 = today.minusDays(4);
        LocalDate day2 = today.minusDays(3);  // MISSED → 복구
        LocalDate day3 = today.minusDays(2);  // MISSED → 복구
        LocalDate day4 = today.minusDays(1);  // MISSED → 복구

        DailyCompletion day1Completion = createCompletion(day1, StreakStatus.COMPLETED, 1);
        completionMap.put(day1, day1Completion);

        setupMocks();

        // when
        streakService.recoverStreak(TEST_USER_ID, day2, day4);

        // then
        verify(dailyCompletionRepository, atLeastOnce()).saveAll(dailyCompletionListCaptor.capture());

        List<DailyCompletion> allSaved = new ArrayList<>();
        dailyCompletionListCaptor.getAllValues().forEach(allSaved::addAll);

        // day2, day3, day4가 모두 COMPLETED로 생성되었는지 확인
        DailyCompletion day2Saved = allSaved.stream()
                .filter(dc -> dc.getCompletionDate().equals(day2))
                .findFirst()
                .orElseThrow(() -> new AssertionError("day2 not found"));

        DailyCompletion day3Saved = allSaved.stream()
                .filter(dc -> dc.getCompletionDate().equals(day3))
                .findFirst()
                .orElseThrow(() -> new AssertionError("day3 not found"));

        DailyCompletion day4Saved = allSaved.stream()
                .filter(dc -> dc.getCompletionDate().equals(day4))
                .findFirst()
                .orElseThrow(() -> new AssertionError("day4 not found"));

        // streakCount 검증
        assertThat(day2Saved.getStreakStatus()).isEqualTo(StreakStatus.COMPLETED);
        assertThat(day2Saved.getStreakCount()).isEqualTo(2);

        assertThat(day3Saved.getStreakStatus()).isEqualTo(StreakStatus.COMPLETED);
        assertThat(day3Saved.getStreakCount()).isEqualTo(3);

        assertThat(day4Saved.getStreakStatus()).isEqualTo(StreakStatus.COMPLETED);
        assertThat(day4Saved.getStreakCount()).isEqualTo(4);

        // UserStudyReport 검증
        verify(userStudyReportRepository, atLeastOnce()).save(userStudyReportCaptor.capture());
        UserStudyReport savedReport = userStudyReportCaptor.getValue();
        assertThat(savedReport.getCurrentStreak()).isEqualTo(4);
        assertThat(savedReport.getLongestStreak()).isEqualTo(4);
    }

    @Test
    @DisplayName("시나리오 4: 복구 후 이후 날짜 재계산 - 프리즈 자동 사용")
    void recoverStreak_AfterRecovery_AutoUsesFreeze() {
        // given
        LocalDate day1 = today.minusDays(5);
        LocalDate day2 = today.minusDays(4);  // FREEZE_USED → COMPLETED (복구, 프리즈 +1)
        LocalDate day3 = today.minusDays(3);  // MISSED (복구 범위 밖, 프리즈 자동 사용)
        LocalDate day4 = today.minusDays(2);  // COMPLETED (연결됨)
        LocalDate day5 = today.minusDays(1);  // COMPLETED (연결됨)

        DailyCompletion day1Completion = createCompletion(day1, StreakStatus.COMPLETED, 1);
        DailyCompletion day2Completion = createCompletion(day2, StreakStatus.FREEZE_USED, 1);
        DailyCompletion day4Completion = createCompletion(day4, StreakStatus.COMPLETED, 1);
        DailyCompletion day5Completion = createCompletion(day5, StreakStatus.COMPLETED, 2);

        completionMap.put(day1, day1Completion);
        completionMap.put(day2, day2Completion);
        completionMap.put(day4, day4Completion);
        completionMap.put(day5, day5Completion);

        setupMocks();

        // when
        streakService.recoverStreak(TEST_USER_ID, day2, day2);

        // then
        verify(dailyCompletionRepository, atLeastOnce()).saveAll(dailyCompletionListCaptor.capture());

        List<DailyCompletion> allSaved = new ArrayList<>();
        dailyCompletionListCaptor.getAllValues().forEach(allSaved::addAll);

        // day2: FREEZE_USED → COMPLETED (streakCount=2)
        DailyCompletion day2Updated = allSaved.stream()
                .filter(dc -> dc.getCompletionDate().equals(day2))
                .findFirst()
                .orElseThrow(() -> new AssertionError("day2 not found"));

        assertThat(day2Updated.getStreakStatus()).isEqualTo(StreakStatus.COMPLETED);
        assertThat(day2Updated.getStreakCount()).isEqualTo(2);

        // day3: 프리즈 자동 사용으로 FREEZE_USED 생성 (streakCount=2 유지)
        DailyCompletion day3Created = allSaved.stream()
                .filter(dc -> dc.getCompletionDate().equals(day3))
                .findFirst()
                .orElseThrow(() -> new AssertionError("day3 not found"));

        assertThat(day3Created.getStreakStatus()).isEqualTo(StreakStatus.FREEZE_USED);
        assertThat(day3Created.getStreakCount()).isEqualTo(2);

        // day4: streakCount 재계산 (3으로 증가)
        DailyCompletion day4Updated = allSaved.stream()
                .filter(dc -> dc.getCompletionDate().equals(day4))
                .findFirst()
                .orElseThrow(() -> new AssertionError("day4 not found"));

        assertThat(day4Updated.getStreakStatus()).isEqualTo(StreakStatus.COMPLETED);
        assertThat(day4Updated.getStreakCount()).isEqualTo(3);

        // day5: streakCount 재계산 (4로 증가)
        DailyCompletion day5Updated = allSaved.stream()
                .filter(dc -> dc.getCompletionDate().equals(day5))
                .findFirst()
                .orElseThrow(() -> new AssertionError("day5 not found"));

        assertThat(day5Updated.getStreakStatus()).isEqualTo(StreakStatus.COMPLETED);
        assertThat(day5Updated.getStreakCount()).isEqualTo(4);

        // 프리즈 트랜잭션 확인: +1 (day2 복구) -1 (day3 자동 사용)
        verify(freezeTransactionRepository, atLeastOnce()).saveAll(freezeTransactionListCaptor.capture());

        List<FreezeTransaction> allTxs = new ArrayList<>();
        freezeTransactionListCaptor.getAllValues().forEach(allTxs::addAll);

        // +1 보상이 정확히 1개, -1 사용이 정확히 1개
        long rewardCount = allTxs.stream().filter(tx -> tx.getAmount() == 1).count();
        long usageCount = allTxs.stream().filter(tx -> tx.getAmount() == -1).count();

        assertThat(rewardCount).isEqualTo(1);
        assertThat(usageCount).isEqualTo(1);

        FreezeTransaction rewardTx = allTxs.stream()
                .filter(tx -> tx.getAmount() == 1)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Reward transaction not found"));
        assertThat(rewardTx.getDescription()).contains(day2.toString());

        FreezeTransaction usageTx = allTxs.stream()
                .filter(tx -> tx.getAmount() == -1)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Usage transaction not found"));
        assertThat(usageTx.getDescription()).contains(day3.toString());

        // UserStudyReport 검증
        verify(userStudyReportRepository, atLeastOnce()).save(userStudyReportCaptor.capture());
        UserStudyReport savedReport = userStudyReportCaptor.getValue();
        assertThat(savedReport.getCurrentStreak()).isEqualTo(4);
        assertThat(savedReport.getLongestStreak()).isEqualTo(4);
        assertThat(savedReport.getLastCompletionDate()).isEqualTo(day5);
        assertThat(savedReport.getAvailableFreezes()).isEqualTo(0);  // +1 보상 -1 사용 = 0
    }

    @Test
    @DisplayName("시나리오 5: 복구 후 프리즈 부족으로 스트릭 연결 중단")
    void recoverStreak_AfterRecovery_StopsWhenNoFreeze() {
        // given
        LocalDate day1 = today.minusDays(5);
        LocalDate day2 = today.minusDays(4);  // MISSED → 복구 (프리즈 보상 없음)
        LocalDate day3 = today.minusDays(3);  // MISSED (프리즈 없어서 연결 안 됨)
        LocalDate day4 = today.minusDays(2);  // COMPLETED (연결 안 됨, 새 스트릭)

        DailyCompletion day1Completion = createCompletion(day1, StreakStatus.COMPLETED, 1);
        DailyCompletion day4Completion = createCompletion(day4, StreakStatus.COMPLETED, 1);

        completionMap.put(day1, day1Completion);
        completionMap.put(day4, day4Completion);

        setupMocks();

        // when
        streakService.recoverStreak(TEST_USER_ID, day2, day2);

        // then
        verify(dailyCompletionRepository, atLeastOnce()).saveAll(dailyCompletionListCaptor.capture());

        List<DailyCompletion> allSaved = new ArrayList<>();
        dailyCompletionListCaptor.getAllValues().forEach(allSaved::addAll);

        // day2는 복구됨
        DailyCompletion day2Saved = allSaved.stream()
                .filter(dc -> dc.getCompletionDate().equals(day2))
                .findFirst()
                .orElseThrow(() -> new AssertionError("day2 not found"));

        assertThat(day2Saved.getStreakStatus()).isEqualTo(StreakStatus.COMPLETED);
        assertThat(day2Saved.getStreakCount()).isEqualTo(2);

        // day3은 프리즈가 없어서 FREEZE_USED로 생성되지 않음
        boolean hasDay3 = allSaved.stream()
                .anyMatch(dc -> dc.getCompletionDate().equals(day3));

        assertThat(hasDay3).isFalse();

        // day4는 기존 값 유지 (연결 안 됨)
        DailyCompletion day4Saved = allSaved.stream()
                .filter(dc -> dc.getCompletionDate().equals(day4))
                .findFirst()
                .orElse(null);

        // day4가 저장되지 않았거나, 저장되었어도 streakCount가 1로 유지
        if (day4Saved != null) {
            assertThat(day4Saved.getStreakCount()).isEqualTo(1);
        }

        // UserStudyReport 검증 - day3에서 끊겼으므로 currentStreak=0
        verify(userStudyReportRepository, atLeastOnce()).save(userStudyReportCaptor.capture());
        UserStudyReport savedReport = userStudyReportCaptor.getValue();
        // day2까지만 연결, day3에서 끊김 -> 오늘까지 2일 이상 차이 -> streak=0
        assertThat(savedReport.getCurrentStreak()).isEqualTo(0);
        assertThat(savedReport.getLongestStreak()).isEqualTo(2);
    }

    @Test
    @DisplayName("시나리오 6: 여러 FREEZE_USED 복구 + 복잡한 프리즈 사용")
    void recoverStreak_MultipleFreeze_ComplexScenario() {
        // given
        LocalDate day1 = today.minusDays(7);   // COMPLETED (streak=1)
        LocalDate day2 = today.minusDays(6);   // FREEZE_USED → COMPLETED (복구, 프리즈 +1)
        LocalDate day3 = today.minusDays(5);   // MISSED → COMPLETED (복구)
        LocalDate day4 = today.minusDays(4);   // MISSED (프리즈 1개 사용)
        LocalDate day5 = today.minusDays(3);   // COMPLETED (연결됨)
        LocalDate day6 = today.minusDays(2);   // COMPLETED (연결됨)
        LocalDate day7 = today.minusDays(1);   // COMPLETED (연결됨)

        DailyCompletion day1Completion = createCompletion(day1, StreakStatus.COMPLETED, 1);
        DailyCompletion day2Completion = createCompletion(day2, StreakStatus.FREEZE_USED, 1);
        DailyCompletion day5Completion = createCompletion(day5, StreakStatus.COMPLETED, 1);
        DailyCompletion day6Completion = createCompletion(day6, StreakStatus.COMPLETED, 2);
        DailyCompletion day7Completion = createCompletion(day7, StreakStatus.COMPLETED, 3);

        completionMap.put(day1, day1Completion);
        completionMap.put(day2, day2Completion);
        completionMap.put(day5, day5Completion);
        completionMap.put(day6, day6Completion);
        completionMap.put(day7, day7Completion);

        setupMocks();

        // when - day2, day3 복구
        streakService.recoverStreak(TEST_USER_ID, day2, day3);

        // then
        List<DailyCompletion> allSaved = new ArrayList<>();
        verify(dailyCompletionRepository, atLeastOnce()).saveAll(dailyCompletionListCaptor.capture());
        dailyCompletionListCaptor.getAllValues().forEach(allSaved::addAll);

        // day2: FREEZE_USED → COMPLETED (streak=2)
        assertThat(allSaved.stream()
                .anyMatch(dc -> dc.getCompletionDate().equals(day2)
                        && dc.getStreakStatus() == StreakStatus.COMPLETED
                        && dc.getStreakCount() == 2))
                .isTrue();

        // day3: MISSED → COMPLETED (streak=3)
        assertThat(allSaved.stream()
                .anyMatch(dc -> dc.getCompletionDate().equals(day3)
                        && dc.getStreakStatus() == StreakStatus.COMPLETED
                        && dc.getStreakCount() == 3))
                .isTrue();

        // day4: 프리즈 사용으로 FREEZE_USED (streak=3 유지)
        assertThat(allSaved.stream()
                .anyMatch(dc -> dc.getCompletionDate().equals(day4)
                        && dc.getStreakStatus() == StreakStatus.FREEZE_USED
                        && dc.getStreakCount() == 3))
                .isTrue();

        // day5, day6, day7: streakCount 재계산
        assertThat(allSaved.stream()
                .anyMatch(dc -> dc.getCompletionDate().equals(day5)
                        && dc.getStreakCount() == 4))
                .isTrue();

        assertThat(allSaved.stream()
                .anyMatch(dc -> dc.getCompletionDate().equals(day6)
                        && dc.getStreakCount() == 5))
                .isTrue();

        assertThat(allSaved.stream()
                .anyMatch(dc -> dc.getCompletionDate().equals(day7)
                        && dc.getStreakCount() == 6))
                .isTrue();

        // 프리즈 트랜잭션: +1 (day2 보상), -1 (day4 사용)
        verify(freezeTransactionRepository, atLeastOnce()).saveAll(freezeTransactionListCaptor.capture());
        List<FreezeTransaction> allTxs = new ArrayList<>();
        freezeTransactionListCaptor.getAllValues().forEach(allTxs::addAll);

        long rewards = allTxs.stream().filter(tx -> tx.getAmount() == 1).count();
        long usages = allTxs.stream().filter(tx -> tx.getAmount() == -1).count();

        assertThat(rewards).isEqualTo(1);
        assertThat(usages).isEqualTo(1);

        // UserStudyReport 검증
        verify(userStudyReportRepository, atLeastOnce()).save(userStudyReportCaptor.capture());
        UserStudyReport savedReport = userStudyReportCaptor.getValue();
        assertThat(savedReport.getCurrentStreak()).isEqualTo(6);
        assertThat(savedReport.getLongestStreak()).isEqualTo(6);
        assertThat(savedReport.getAvailableFreezes()).isEqualTo(0);  // +1 보상 -1 사용 = 0
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

    private void setupMocks() {
        // UserStudyReport mock
        lenient().when(userStudyReportRepository.findByUserId(TEST_USER_ID))
                .thenReturn(Optional.of(testReport));
        lenient().when(userStudyReportRepository.save(any(UserStudyReport.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // DailyCompletion 조회 mock
        lenient().when(dailyCompletionRepository.findByUserIdAndCompletionDate(eq(TEST_USER_ID), any(LocalDate.class)))
                .thenAnswer(invocation -> {
                    LocalDate date = invocation.getArgument(1);
                    return Optional.ofNullable(completionMap.get(date));
                });

        // DailyCompletion 전체 조회 mock (recalculateUserStudyReport용) - 날짜 순으로 정렬
        lenient().when(dailyCompletionRepository.findByUserIdOrderByCompletionDateAsc(TEST_USER_ID))
                .thenAnswer(invocation -> {
                    List<DailyCompletion> sorted = new ArrayList<>(completionMap.values());
                    sorted.sort((a, b) -> a.getCompletionDate().compareTo(b.getCompletionDate()));
                    return sorted;
                });

        // saveAll mock
        lenient().when(dailyCompletionRepository.saveAll(anyList()))
                .thenAnswer(invocation -> {
                    List<DailyCompletion> saved = invocation.getArgument(0);
                    // completionMap 업데이트
                    saved.forEach(dc -> completionMap.put(dc.getCompletionDate(), dc));
                    return saved;
                });

        lenient().when(freezeTransactionRepository.saveAll(anyList()))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }
}
