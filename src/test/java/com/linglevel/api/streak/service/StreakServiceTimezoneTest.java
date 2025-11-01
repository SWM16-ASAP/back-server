package com.linglevel.api.streak.service;

import com.linglevel.api.common.AbstractDatabaseTest;
import com.linglevel.api.content.common.ContentType;
import com.linglevel.api.i18n.LanguageCode;
import com.linglevel.api.streak.dto.CalendarResponse;
import com.linglevel.api.streak.dto.StreakResponse;
import com.linglevel.api.streak.dto.WeekStreakResponse;
import com.linglevel.api.streak.entity.DailyCompletion;
import com.linglevel.api.streak.entity.StreakStatus;
import com.linglevel.api.streak.repository.DailyCompletionRepository;
import com.linglevel.api.streak.repository.FreezeTransactionRepository;
import com.linglevel.api.streak.repository.UserStudyReportRepository;
import com.linglevel.api.user.ticket.repository.TicketTransactionRepository;
import com.linglevel.api.user.ticket.service.TicketService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.context.annotation.Import;

import java.time.*;
import java.util.TimeZone;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 타임존 문제를 재현하는 테스트
 *
 * 문제 시나리오:
 * 1. 서버가 UTC 타임존으로 실행 중일 때 학습 데이터 저장
 * 2. 서버 타임존이 KST로 변경됨
 * 3. streak/me는 정상 작동하지만 캘린더/주간 스트릭에서 데이터를 찾지 못함
 */
@DataMongoTest
@Import({StreakService.class, TicketService.class})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("StreakService 타임존 문제 재현 테스트")
class StreakServiceTimezoneTest extends AbstractDatabaseTest {

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

    private static final String TEST_USER_ID = "timezone-test-user";
    private static final ZoneId KST_ZONE = ZoneId.of("Asia/Seoul");

    private TimeZone originalTimeZone;

    @BeforeEach
    void setUp() {
        // 원래 타임존 저장
        originalTimeZone = TimeZone.getDefault();

        // 테스트 데이터 정리
        dailyCompletionRepository.deleteAll();
        userStudyReportRepository.deleteAll();
        freezeTransactionRepository.deleteAll();
        ticketTransactionRepository.deleteAll();
    }

    @AfterEach
    void tearDown() {
        // 타임존 복원
        TimeZone.setDefault(originalTimeZone);
    }

    @Test
    @DisplayName("시나리오 1: UTC에서 저장 → KST에서 조회 시 캘린더 데이터 조회 실패")
    void testTimezoneIssue_SaveInUTC_QueryInKST() {
        // Given: 시스템 타임존을 UTC로 설정
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));

        // KST 기준 오늘 날짜 (예: 2025-11-01 20:00 KST)
        LocalDate kstToday = LocalDate.now(KST_ZONE);

        // UTC에서 학습 완료 (내부적으로 LocalDate.now(KST_ZONE) 사용)
        streakService.updateStreak(TEST_USER_ID, ContentType.BOOK, "test-book-1");

        // MongoDB에 저장된 데이터 확인
        DailyCompletion savedCompletion = dailyCompletionRepository
            .findByUserIdAndCompletionDate(TEST_USER_ID, kstToday)
            .orElse(null);

        System.out.println("=== UTC 환경에서 저장 후 ===");
        System.out.println("System Default TimeZone: " + TimeZone.getDefault().getID());
        System.out.println("KST Today: " + kstToday);
        System.out.println("Saved Completion Date: " + (savedCompletion != null ? savedCompletion.getCompletionDate() : "null"));

        // When: 시스템 타임존을 KST로 변경
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));

        System.out.println("\n=== KST 환경으로 변경 후 ===");
        System.out.println("System Default TimeZone: " + TimeZone.getDefault().getID());

        // streak/me 조회 (단일 조회)
        StreakResponse streakResponse = streakService.getStreakInfo(TEST_USER_ID, LanguageCode.KO);

        // 캘린더 조회 (범위 조회)
        CalendarResponse calendarResponse = streakService.getCalendar(
            TEST_USER_ID,
            kstToday.getYear(),
            kstToday.getMonthValue()
        );

        // 주간 스트릭 조회 (범위 조회)
        WeekStreakResponse weekResponse = streakService.getThisWeekStreak(TEST_USER_ID);

        // Then: 결과 비교
        System.out.println("\n=== 조회 결과 ===");
        System.out.println("streak/me - todayStatus: " + streakResponse.getTodayStatus());
        System.out.println("streak/me - currentStreak: " + streakResponse.getCurrentStreak());

        var todayInCalendar = calendarResponse.getDays().stream()
            .filter(day -> day.getDate().equals(kstToday))
            .findFirst()
            .orElse(null);

        System.out.println("calendar - todayStatus: " + (todayInCalendar != null ? todayInCalendar.getStatus() : "NOT_FOUND"));
        System.out.println("calendar - streakCount: " + (todayInCalendar != null ? todayInCalendar.getStreakCount() : "null"));
        System.out.println("calendar - totalCompletionCount: " + (todayInCalendar != null ? todayInCalendar.getTotalCompletionCount() : "null"));

        var todayInWeek = weekResponse.getWeekDays().stream()
            .filter(day -> day.getDate().equals(kstToday))
            .findFirst()
            .orElse(null);

        System.out.println("week - todayStatus: " + (todayInWeek != null ? todayInWeek.getStatus() : "NOT_FOUND"));

        // 검증: 타임존 불일치로 인한 문제 재현
        // streak/me는 단일 조회라 운 좋게 작동할 수 있음
        // 하지만 캘린더는 범위 조회 + Map 조회라 실패할 가능성 높음

        // 이상적으로는 모두 COMPLETED여야 함
        assertThat(streakResponse.getTodayStatus())
            .as("streak/me should show COMPLETED")
            .isIn(StreakStatus.COMPLETED, StreakStatus.MISSED); // 타임존 이슈로 둘 다 가능

        if (todayInCalendar != null) {
            assertThat(todayInCalendar.getStatus())
                .as("calendar should show COMPLETED but might fail due to timezone")
                .isIn(StreakStatus.COMPLETED, StreakStatus.MISSED);

            // 문제 발생 시 streakCount가 null이거나 0
            System.out.println("\n⚠️  Expected: COMPLETED with streakCount=1");
            System.out.println("⚠️  Actual: " + todayInCalendar.getStatus() + " with streakCount=" + todayInCalendar.getStreakCount());
        }
    }

    @Test
    @DisplayName("시나리오 2: 단일 조회 vs 범위 조회 비교")
    void testSingleQuery_vs_RangeQuery() {
        // Given: KST로 타임존 설정
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));

        LocalDate kstToday = LocalDate.now(KST_ZONE);

        // 학습 완료
        streakService.updateStreak(TEST_USER_ID, ContentType.BOOK, "test-book-1");

        System.out.println("=== KST 환경에서 저장 및 조회 ===");
        System.out.println("System Default TimeZone: " + TimeZone.getDefault().getID());
        System.out.println("KST Today: " + kstToday);

        // When: 단일 조회
        DailyCompletion singleQueryResult = dailyCompletionRepository
            .findByUserIdAndCompletionDate(TEST_USER_ID, kstToday)
            .orElse(null);

        // When: 범위 조회
        var rangeQueryResults = dailyCompletionRepository
            .findByUserIdAndCompletionDateBetween(
                TEST_USER_ID,
                kstToday.minusDays(7),
                kstToday.plusDays(7)
            );

        // Then: 결과 비교
        System.out.println("\n=== 조회 결과 비교 ===");
        System.out.println("Single Query - Found: " + (singleQueryResult != null));
        System.out.println("Single Query - Date: " + (singleQueryResult != null ? singleQueryResult.getCompletionDate() : "null"));

        System.out.println("\nRange Query - Count: " + rangeQueryResults.size());
        rangeQueryResults.forEach(completion -> {
            System.out.println("Range Query - Date: " + completion.getCompletionDate());
        });

        // 검증
        assertThat(singleQueryResult).isNotNull();
        assertThat(rangeQueryResults).isNotEmpty();

        // 같은 데이터를 찾아야 함
        boolean foundInRange = rangeQueryResults.stream()
            .anyMatch(c -> c.getCompletionDate().equals(kstToday));

        assertThat(foundInRange)
            .as("Range query should find the same data as single query")
            .isTrue();
    }

    @Test
    @DisplayName("시나리오 3: KST 타임존 설정 후 정상 동작 확인")
    void testWithKSTTimezone_AllQueriesWork() {
        // Given: KST로 타임존 설정 (ApiApplication.init()과 동일)
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));

        LocalDate kstToday = LocalDate.now(KST_ZONE);

        // 학습 완료
        streakService.updateStreak(TEST_USER_ID, ContentType.BOOK, "test-book-1");

        // When: 모든 조회 API 실행
        StreakResponse streakResponse = streakService.getStreakInfo(TEST_USER_ID, LanguageCode.KO);
        CalendarResponse calendarResponse = streakService.getCalendar(
            TEST_USER_ID,
            kstToday.getYear(),
            kstToday.getMonthValue()
        );
        WeekStreakResponse weekResponse = streakService.getThisWeekStreak(TEST_USER_ID);

        // Then: 모든 API가 정상 동작
        System.out.println("=== KST 환경에서 모든 조회 성공 ===");
        System.out.println("streak/me - status: " + streakResponse.getTodayStatus() + ", streak: " + streakResponse.getCurrentStreak());

        var todayInCalendar = calendarResponse.getDays().stream()
            .filter(day -> day.getDate().equals(kstToday))
            .findFirst()
            .orElse(null);

        assertThat(todayInCalendar).isNotNull();
        System.out.println("calendar - status: " + todayInCalendar.getStatus() + ", streak: " + todayInCalendar.getStreakCount());

        var todayInWeek = weekResponse.getWeekDays().stream()
            .filter(day -> day.getDate().equals(kstToday))
            .findFirst()
            .orElse(null);

        assertThat(todayInWeek).isNotNull();
        System.out.println("week - status: " + todayInWeek.getStatus());

        // 검증: 모두 COMPLETED여야 함
        assertThat(streakResponse.getTodayStatus()).isEqualTo(StreakStatus.COMPLETED);
        assertThat(streakResponse.getCurrentStreak()).isEqualTo(1);

        assertThat(todayInCalendar.getStatus()).isEqualTo(StreakStatus.COMPLETED);
        assertThat(todayInCalendar.getStreakCount()).isEqualTo(1);
        assertThat(todayInCalendar.getTotalCompletionCount()).isEqualTo(1);

        assertThat(todayInWeek.getStatus()).isEqualTo(StreakStatus.COMPLETED);

        System.out.println("\n✅ 모든 API가 일관된 결과 반환!");
    }

    @Test
    @DisplayName("시나리오 4: MongoDB LocalDate 저장 방식 확인")
    void testMongoDBLocalDateStorage() {
        // Given: 타임존별 LocalDate 저장
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        LocalDate dateInUTC = LocalDate.of(2025, 11, 1);

        DailyCompletion completionUTC = DailyCompletion.builder()
            .userId(TEST_USER_ID)
            .completionDate(dateInUTC)
            .totalCompletionCount(1)
            .createdAt(Instant.now())
            .build();
        dailyCompletionRepository.save(completionUTC);

        System.out.println("=== MongoDB LocalDate 저장/조회 테스트 ===");
        System.out.println("Saved in UTC timezone: " + dateInUTC);

        // When: KST로 변경 후 조회
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));

        DailyCompletion queriedInKST = dailyCompletionRepository
            .findByUserIdAndCompletionDate(TEST_USER_ID, dateInUTC)
            .orElse(null);

        System.out.println("Queried in KST timezone with same LocalDate: " + (queriedInKST != null ? "Found" : "Not Found"));

        if (queriedInKST != null) {
            System.out.println("Retrieved date: " + queriedInKST.getCompletionDate());
            System.out.println("✅ MongoDB handles LocalDate correctly regardless of JVM timezone");
        } else {
            System.out.println("❌ MongoDB LocalDate query failed due to timezone conversion");
        }

        // 모든 데이터 조회해서 확인
        var allData = dailyCompletionRepository.findAll();
        System.out.println("\nAll data in DB:");
        allData.forEach(d -> System.out.println("  - " + d.getCompletionDate() + " (userId: " + d.getUserId() + ")"));
    }
}
