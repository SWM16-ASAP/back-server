package com.linglevel.api.streak.service;

import com.linglevel.api.common.AbstractDatabaseTest;
import com.linglevel.api.content.common.ContentType;
import com.linglevel.api.streak.dto.CalendarResponse;
import com.linglevel.api.streak.entity.DailyCompletion;
import com.linglevel.api.streak.entity.FreezeTransaction;
import com.linglevel.api.streak.entity.StreakStatus;
import com.linglevel.api.streak.entity.UserStudyReport;
import com.linglevel.api.streak.repository.DailyCompletionRepository;
import com.linglevel.api.streak.repository.FreezeTransactionRepository;
import com.linglevel.api.streak.repository.UserStudyReportRepository;
import com.linglevel.api.user.ticket.repository.TicketTransactionRepository;
import com.linglevel.api.user.ticket.service.TicketService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;

import java.time.*;
import java.util.ArrayList;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;

@DataMongoTest
@Import({StreakService.class, TicketService.class})
@DisplayName("StreakService Backfill 테스트")
class StreakServiceBackfillTest extends AbstractDatabaseTest {

    @Autowired
    private StreakService streakService;

    @Autowired
    private DailyCompletionRepository dailyCompletionRepository;

    @Autowired
    private UserStudyReportRepository userStudyReportRepository;

    @Autowired
    private FreezeTransactionRepository freezeTransactionRepository;

    @Autowired
    private TicketTransactionRepository ticketTransactionRepository;

    @MockBean
    private ReadingSessionService readingSessionService;

    private static final String TEST_USER_ID = "backfill-test-user";
    private static final ZoneId KST_ZONE = ZoneId.of("Asia/Seoul");

    @BeforeEach
    void setUp() {
        dailyCompletionRepository.deleteAll();
        userStudyReportRepository.deleteAll();
        freezeTransactionRepository.deleteAll();
        ticketTransactionRepository.deleteAll();
    }

    @Test
    @DisplayName("단일 연속 streak, 모든 streakCount가 null인 경우 backfill")
    void testBackfill_SingleStreak_AllNull() {
        // Given: 5일 연속 학습했지만 streakCount가 모두 null
        LocalDate startDate = LocalDate.of(2024, 1, 1);

        for (int i = 0; i < 5; i++) {
            LocalDate date = startDate.plusDays(i);
            DailyCompletion completion = DailyCompletion.builder()
                    .userId(TEST_USER_ID)
                    .completionDate(date)
                    .totalCompletionCount(1)
                    .firstCompletionCount(1)
                    .completedContents(new ArrayList<>())
                    .streakCount(null)  // null!
                    .createdAt(Instant.now())
                    .build();
            dailyCompletionRepository.save(completion);
        }

        // When: 캘린더 조회 (backfill 트리거)
        CalendarResponse calendar = streakService.getCalendar(TEST_USER_ID, 2024, 1);

        // Then: streakCount가 1, 2, 3, 4, 5로 채워짐
        assertThat(calendar.getDays()).hasSize(31);
        assertThat(calendar.getDays().get(0).getStreakCount()).isEqualTo(1);
        assertThat(calendar.getDays().get(1).getStreakCount()).isEqualTo(2);
        assertThat(calendar.getDays().get(2).getStreakCount()).isEqualTo(3);
        assertThat(calendar.getDays().get(3).getStreakCount()).isEqualTo(4);
        assertThat(calendar.getDays().get(4).getStreakCount()).isEqualTo(5);

        // DB에도 저장되었는지 확인
        DailyCompletion saved = dailyCompletionRepository
                .findByUserIdAndCompletionDate(TEST_USER_ID, startDate)
                .orElseThrow();
        assertThat(saved.getStreakCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("Freeze 포함 streak, freeze는 카운트 유지")
    void testBackfill_WithFreeze_MaintainCount() {
        // Given: 1일 학습, 2일 freeze, 3일 학습 (streakCount null)
        LocalDate day1 = LocalDate.of(2024, 1, 1);
        LocalDate day2 = LocalDate.of(2024, 1, 2);
        LocalDate day3 = LocalDate.of(2024, 1, 3);

        // Day 1: 학습
        DailyCompletion completion1 = DailyCompletion.builder()
                .userId(TEST_USER_ID)
                .completionDate(day1)
                .totalCompletionCount(1)
                .firstCompletionCount(1)
                .completedContents(new ArrayList<>())
                .streakCount(null)
                .createdAt(Instant.now())
                .build();
        dailyCompletionRepository.save(completion1);

        // Day 2: Freeze (DailyCompletion with count=0)
        DailyCompletion completion2 = DailyCompletion.builder()
                .userId(TEST_USER_ID)
                .completionDate(day2)
                .totalCompletionCount(0)
                .firstCompletionCount(0)
                .completedContents(new ArrayList<>())
                .streakCount(null)
                .createdAt(Instant.now())
                .build();
        dailyCompletionRepository.save(completion2);

        FreezeTransaction freezeTx = FreezeTransaction.builder()
                .userId(TEST_USER_ID)
                .amount(-1)
                .description("Freeze for day 2")
                .createdAt(day2.atStartOfDay(KST_ZONE).toInstant())
                .build();
        freezeTransactionRepository.save(freezeTx);

        // Day 3: 학습
        DailyCompletion completion3 = DailyCompletion.builder()
                .userId(TEST_USER_ID)
                .completionDate(day3)
                .totalCompletionCount(1)
                .firstCompletionCount(1)
                .completedContents(new ArrayList<>())
                .streakCount(null)
                .createdAt(Instant.now())
                .build();
        dailyCompletionRepository.save(completion3);

        // When: 캘린더 조회
        CalendarResponse calendar = streakService.getCalendar(TEST_USER_ID, 2024, 1);

        // Then: Day1=1, Day2=1 (유지!), Day3=2
        assertThat(calendar.getDays().get(0).getStreakCount()).isEqualTo(1);
        assertThat(calendar.getDays().get(1).getStreakCount()).isEqualTo(1); // Freeze는 유지
        assertThat(calendar.getDays().get(2).getStreakCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("다중 독립 streak 구간, 모두 backfill")
    void testBackfill_MultipleStreaks() {
        // Given:
        // 1-3일: streak 3
        // 4일: MISSED (끊김)
        // 5-7일: 새로운 streak 3
        LocalDate day1 = LocalDate.of(2024, 1, 1);

        // First streak: 1-3일
        for (int i = 0; i < 3; i++) {
            LocalDate date = day1.plusDays(i);
            DailyCompletion completion = DailyCompletion.builder()
                    .userId(TEST_USER_ID)
                    .completionDate(date)
                    .totalCompletionCount(1)
                    .firstCompletionCount(1)
                    .completedContents(new ArrayList<>())
                    .streakCount(null)
                    .createdAt(Instant.now())
                    .build();
            dailyCompletionRepository.save(completion);
        }

        // Day 4: MISSED (데이터 없음)

        // Second streak: 5-7일
        for (int i = 4; i < 7; i++) {
            LocalDate date = day1.plusDays(i);
            DailyCompletion completion = DailyCompletion.builder()
                    .userId(TEST_USER_ID)
                    .completionDate(date)
                    .totalCompletionCount(1)
                    .firstCompletionCount(1)
                    .completedContents(new ArrayList<>())
                    .streakCount(null)
                    .createdAt(Instant.now())
                    .build();
            dailyCompletionRepository.save(completion);
        }

        // When: 캘린더 조회
        CalendarResponse calendar = streakService.getCalendar(TEST_USER_ID, 2024, 1);

        // Then:
        // Day 1-3: 1, 2, 3
        assertThat(calendar.getDays().get(0).getStreakCount()).isEqualTo(1);
        assertThat(calendar.getDays().get(1).getStreakCount()).isEqualTo(2);
        assertThat(calendar.getDays().get(2).getStreakCount()).isEqualTo(3);

        // Day 4: MISSED
        assertThat(calendar.getDays().get(3).getStatus()).isEqualTo(StreakStatus.MISSED);
        assertThat(calendar.getDays().get(3).getStreakCount()).isEqualTo(0);

        // Day 5-7: 1, 2, 3 (새로운 streak!)
        assertThat(calendar.getDays().get(4).getStreakCount()).isEqualTo(1);
        assertThat(calendar.getDays().get(5).getStreakCount()).isEqualTo(2);
        assertThat(calendar.getDays().get(6).getStreakCount()).isEqualTo(3);
    }

    @Test
    @DisplayName("일부는 이미 채워짐, 일부만 null인 경우")
    void testBackfill_PartiallyFilled() {
        // Given:
        // 1-2일: 이미 streakCount 있음
        // 3-5일: streakCount null
        LocalDate day1 = LocalDate.of(2024, 1, 1);

        // Day 1-2: 이미 채워진 데이터
        for (int i = 0; i < 2; i++) {
            LocalDate date = day1.plusDays(i);
            DailyCompletion completion = DailyCompletion.builder()
                    .userId(TEST_USER_ID)
                    .completionDate(date)
                    .totalCompletionCount(1)
                    .firstCompletionCount(1)
                    .completedContents(new ArrayList<>())
                    .streakCount(i + 1)  // 이미 있음!
                    .createdAt(Instant.now())
                    .build();
            dailyCompletionRepository.save(completion);
        }

        // Day 3-5: null
        for (int i = 2; i < 5; i++) {
            LocalDate date = day1.plusDays(i);
            DailyCompletion completion = DailyCompletion.builder()
                    .userId(TEST_USER_ID)
                    .completionDate(date)
                    .totalCompletionCount(1)
                    .firstCompletionCount(1)
                    .completedContents(new ArrayList<>())
                    .streakCount(null)
                    .createdAt(Instant.now())
                    .build();
            dailyCompletionRepository.save(completion);
        }

        // When: 캘린더 조회
        CalendarResponse calendar = streakService.getCalendar(TEST_USER_ID, 2024, 1);

        // Then: Day 3-5만 채워짐 (3, 4, 5)
        assertThat(calendar.getDays().get(0).getStreakCount()).isEqualTo(1);  // 기존 유지
        assertThat(calendar.getDays().get(1).getStreakCount()).isEqualTo(2);  // 기존 유지
        assertThat(calendar.getDays().get(2).getStreakCount()).isEqualTo(3);  // 새로 채워짐
        assertThat(calendar.getDays().get(3).getStreakCount()).isEqualTo(4);  // 새로 채워짐
        assertThat(calendar.getDays().get(4).getStreakCount()).isEqualTo(5);  // 새로 채워짐
    }

    @Test
    @DisplayName("캘린더 범위 밖으로 확장되는 streak")
    void testBackfill_StreakExtendsBeforeCalendar() {
        // Given:
        // 12월 28-31일: streak 시작
        // 1월 1-5일: 계속 (null)
        LocalDate dec28 = LocalDate.of(2023, 12, 28);

        // 12월 28-31일
        for (int i = 0; i < 4; i++) {
            LocalDate date = dec28.plusDays(i);
            DailyCompletion completion = DailyCompletion.builder()
                    .userId(TEST_USER_ID)
                    .completionDate(date)
                    .totalCompletionCount(1)
                    .firstCompletionCount(1)
                    .completedContents(new ArrayList<>())
                    .streakCount(null)
                    .createdAt(Instant.now())
                    .build();
            dailyCompletionRepository.save(completion);
        }

        // 1월 1-5일
        LocalDate jan1 = LocalDate.of(2024, 1, 1);
        for (int i = 0; i < 5; i++) {
            LocalDate date = jan1.plusDays(i);
            DailyCompletion completion = DailyCompletion.builder()
                    .userId(TEST_USER_ID)
                    .completionDate(date)
                    .totalCompletionCount(1)
                    .firstCompletionCount(1)
                    .completedContents(new ArrayList<>())
                    .streakCount(null)
                    .createdAt(Instant.now())
                    .build();
            dailyCompletionRepository.save(completion);
        }

        // When: 1월 캘린더 조회
        CalendarResponse calendar = streakService.getCalendar(TEST_USER_ID, 2024, 1);

        // Then: 1월 1일은 5 (12/28-1/1 = 5일)
        assertThat(calendar.getDays().get(0).getStreakCount()).isEqualTo(5);
        assertThat(calendar.getDays().get(1).getStreakCount()).isEqualTo(6);
        assertThat(calendar.getDays().get(2).getStreakCount()).isEqualTo(7);
        assertThat(calendar.getDays().get(3).getStreakCount()).isEqualTo(8);
        assertThat(calendar.getDays().get(4).getStreakCount()).isEqualTo(9);
    }
}
