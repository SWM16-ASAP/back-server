package com.linglevel.api.streak.service;

import com.linglevel.api.content.common.ContentType;
import com.linglevel.api.streak.entity.DailyCompletion;
import com.linglevel.api.streak.entity.FreezeTransaction;
import com.linglevel.api.streak.entity.StreakStatus;
import com.linglevel.api.streak.entity.UserStudyReport;
import com.linglevel.api.streak.repository.DailyCompletionRepository;
import com.linglevel.api.streak.repository.FreezeTransactionRepository;
import com.linglevel.api.streak.repository.UserStudyReportRepository;
import com.linglevel.api.user.ticket.entity.TicketTransaction;
import com.linglevel.api.user.ticket.repository.TicketTransactionRepository;
import com.linglevel.api.user.ticket.service.TicketService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("StreakService - 비즈니스 로직 검증 테스트")
class StreakServiceBusinessLogicTest {

    @Mock
    private UserStudyReportRepository userStudyReportRepository;

    @Mock
    private DailyCompletionRepository dailyCompletionRepository;

    @Mock
    private TicketService ticketService;

    @Mock
    private FreezeTransactionRepository freezeTransactionRepository;

    @Mock
    private ReadingSessionService readingSessionService;

    @InjectMocks
    private StreakService streakService;

    private static final String TEST_USER_ID = "test-user-123";
    private static final String CHAPTER_ID_1 = "chapter-1";
    private static final String CHAPTER_ID_2 = "chapter-2";
    private static final ContentType CONTENT_TYPE = ContentType.BOOK;
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
    @DisplayName("30초 이상 읽고 마지막 청크 완료 시 스트릭 증가")
    void updateStreak_ValidSession_IncreasesStreak() {
        // given - 30초 이상 읽음
        when(dailyCompletionRepository.findByUserIdAndCompletionDate(TEST_USER_ID, today))
                .thenReturn(Optional.empty());
        when(readingSessionService.isReadingSessionValid(TEST_USER_ID, CONTENT_TYPE, CHAPTER_ID_1))
                .thenReturn(true); // 30초 이상
        when(userStudyReportRepository.findByUserId(TEST_USER_ID))
                .thenReturn(Optional.of(testReport));

        // when
        boolean result = streakService.updateStreak(TEST_USER_ID, CONTENT_TYPE, CHAPTER_ID_1);

        // then
        assertThat(result).isTrue();
        assertThat(testReport.getCurrentStreak()).isEqualTo(1);
        assertThat(testReport.getLongestStreak()).isEqualTo(1);
        verify(userStudyReportRepository).save(testReport);
    }

    @Test
    @DisplayName("30초 미만 읽으면 스트릭 업데이트 안됨")
    void updateStreak_InvalidSession_DoesNotIncreaseStreak() {
        // given - 30초 미만
        when(dailyCompletionRepository.findByUserIdAndCompletionDate(TEST_USER_ID, today))
                .thenReturn(Optional.empty());
        when(readingSessionService.isReadingSessionValid(TEST_USER_ID, CONTENT_TYPE, CHAPTER_ID_1))
                .thenReturn(false); // 30초 미만

        // when
        boolean result = streakService.updateStreak(TEST_USER_ID, CONTENT_TYPE, CHAPTER_ID_1);

        // then
        assertThat(result).isFalse();
        verify(userStudyReportRepository, never()).save(any());
    }

    @Test
    @DisplayName("같은 날 두 번째 콘텐츠 완료 시 스트릭 중복 방지")
    void updateStreak_SameDaySecondContent_ReturnsFalse() {
        // given - 오늘 이미 스트릭 완료
        DailyCompletion existingDaily = DailyCompletion.builder()
                .userId(TEST_USER_ID)
                .completionDate(today)
                .streakStatus(StreakStatus.COMPLETED)
                .streakCount(1)
                .totalCompletionCount(1)
                .firstCompletionCount(1)
                .completedContents(new ArrayList<>())
                .createdAt(Instant.now())
                .build();

        when(dailyCompletionRepository.findByUserIdAndCompletionDate(TEST_USER_ID, today))
                .thenReturn(Optional.of(existingDaily));

        // when
        boolean result = streakService.updateStreak(TEST_USER_ID, CONTENT_TYPE, CHAPTER_ID_2);

        // then - 스트릭은 업데이트 안됨
        assertThat(result).isFalse();
        verify(userStudyReportRepository, never()).save(any());
    }

    @Test
    @DisplayName("5일 연속 달성 시 프리즈 1개 지급")
    void updateStreak_FiveDayStreak_GrantsFreeze() {
        // given
        testReport.setCurrentStreak(4);
        testReport.setLastCompletionDate(today.minusDays(1));

        when(dailyCompletionRepository.findByUserIdAndCompletionDate(TEST_USER_ID, today))
                .thenReturn(Optional.empty());
        when(readingSessionService.isReadingSessionValid(TEST_USER_ID, CONTENT_TYPE, CHAPTER_ID_1))
                .thenReturn(true);
        when(userStudyReportRepository.findByUserId(TEST_USER_ID))
                .thenReturn(Optional.of(testReport));

        // when
        boolean result = streakService.updateStreak(TEST_USER_ID, CONTENT_TYPE, CHAPTER_ID_1);

        // then
        assertThat(result).isTrue();
        assertThat(testReport.getCurrentStreak()).isEqualTo(5);
        assertThat(testReport.getAvailableFreezes()).isEqualTo(1);

        ArgumentCaptor<FreezeTransaction> freezeCaptor = ArgumentCaptor.forClass(FreezeTransaction.class);
        verify(freezeTransactionRepository).save(freezeCaptor.capture());
        assertThat(freezeCaptor.getValue().getAmount()).isEqualTo(1);
        assertThat(freezeCaptor.getValue().getDescription()).contains("5-day streak");
    }

    @Test
    @DisplayName("프리즈 2개 보유 시 추가 지급 안됨 (MAX 2개)")
    void updateStreak_MaxFreezesReached_NoAdditionalFreeze() {
        // given - 이미 프리즈 2개 보유
        testReport.setCurrentStreak(4);
        testReport.setLastCompletionDate(today.minusDays(1));
        testReport.setAvailableFreezes(2);

        when(dailyCompletionRepository.findByUserIdAndCompletionDate(TEST_USER_ID, today))
                .thenReturn(Optional.empty());
        when(readingSessionService.isReadingSessionValid(TEST_USER_ID, CONTENT_TYPE, CHAPTER_ID_1))
                .thenReturn(true);
        when(userStudyReportRepository.findByUserId(TEST_USER_ID))
                .thenReturn(Optional.of(testReport));

        // when
        streakService.updateStreak(TEST_USER_ID, CONTENT_TYPE, CHAPTER_ID_1);

        // then - 프리즈 증가 안함
        assertThat(testReport.getAvailableFreezes()).isEqualTo(2);
        verify(freezeTransactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("7일 연속 달성 시 티켓 1개 지급")
    void updateStreak_SevenDayStreak_GrantsTicket() {
        // given
        testReport.setCurrentStreak(6);
        testReport.setLastCompletionDate(today.minusDays(1));

        when(dailyCompletionRepository.findByUserIdAndCompletionDate(TEST_USER_ID, today))
                .thenReturn(Optional.empty());
        when(readingSessionService.isReadingSessionValid(TEST_USER_ID, CONTENT_TYPE, CHAPTER_ID_1))
                .thenReturn(true);
        when(userStudyReportRepository.findByUserId(TEST_USER_ID))
                .thenReturn(Optional.of(testReport));

        // when
        streakService.updateStreak(TEST_USER_ID, CONTENT_TYPE, CHAPTER_ID_1);

        // then
        assertThat(testReport.getCurrentStreak()).isEqualTo(7);
        verify(ticketService).grantTicket(eq(TEST_USER_ID), eq(1), contains("7-day streak"));
    }

    @Test
    @DisplayName("15일, 30일 연속 달성 시 티켓 추가 지급")
    void updateStreak_FifteenAndThirtyDayStreak_GrantsTickets() {
        // given - 14일 스트릭
        testReport.setCurrentStreak(14);
        testReport.setLastCompletionDate(today.minusDays(1));

        when(dailyCompletionRepository.findByUserIdAndCompletionDate(TEST_USER_ID, today))
                .thenReturn(Optional.empty());
        when(readingSessionService.isReadingSessionValid(TEST_USER_ID, CONTENT_TYPE, CHAPTER_ID_1))
                .thenReturn(true);
        when(userStudyReportRepository.findByUserId(TEST_USER_ID))
                .thenReturn(Optional.of(testReport));

        // when - 15일 달성
        streakService.updateStreak(TEST_USER_ID, CONTENT_TYPE, CHAPTER_ID_1);

        // then
        assertThat(testReport.getCurrentStreak()).isEqualTo(15);
        verify(ticketService).grantTicket(eq(TEST_USER_ID), eq(1), contains("15-day streak"));

        // given - 29일 스트릭
        testReport.setCurrentStreak(29);
        testReport.setLastCompletionDate(today.minusDays(1));
        reset(ticketService);

        // when - 30일 달성
        streakService.updateStreak(TEST_USER_ID, CONTENT_TYPE, CHAPTER_ID_1);

        // then
        assertThat(testReport.getCurrentStreak()).isEqualTo(30);
        verify(ticketService).grantTicket(eq(TEST_USER_ID), eq(1), contains("30-day streak"));
    }

    @Test
    @DisplayName("학습 시간 누적 테스트")
    void addStudyTime_AccumulatesCorrectly() {
        // given
        when(userStudyReportRepository.findByUserId(TEST_USER_ID))
                .thenReturn(Optional.of(testReport));

        // when - 여러 번 학습
        streakService.addStudyTime(TEST_USER_ID, 120L); // 2분
        streakService.addStudyTime(TEST_USER_ID, 180L); // 3분
        streakService.addStudyTime(TEST_USER_ID, 60L);  // 1분

        // then
        assertThat(testReport.getTotalReadingTimeSeconds()).isEqualTo(360L); // 6분
        verify(userStudyReportRepository, times(3)).save(testReport);
    }

    @Test
    @DisplayName("첫 완료 콘텐츠 기록 시 firstCompletionCount 증가")
    void addCompletedContent_FirstTime_IncrementsFirstCount() {
        // given
        when(userStudyReportRepository.findByUserId(TEST_USER_ID))
                .thenReturn(Optional.of(testReport));
        when(dailyCompletionRepository.findByUserIdAndCompletionDate(TEST_USER_ID, today))
                .thenReturn(Optional.empty());

        // when - 스트릭과 무관하게 완료만 기록
        streakService.addCompletedContent(TEST_USER_ID, CONTENT_TYPE, CHAPTER_ID_1, false);

        // then
        assertThat(testReport.getCompletedContentIds()).contains(CHAPTER_ID_1);

        ArgumentCaptor<DailyCompletion> dailyCaptor = ArgumentCaptor.forClass(DailyCompletion.class);
        verify(dailyCompletionRepository).save(dailyCaptor.capture());

        DailyCompletion savedDaily = dailyCaptor.getValue();
        assertThat(savedDaily.getFirstCompletionCount()).isEqualTo(1);
        assertThat(savedDaily.getTotalCompletionCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("이미 완료한 콘텐츠 재완료 시 totalCount는 증가, firstCount는 유지")
    void addCompletedContent_AlreadyCompleted_DoesNotIncrementFirstCount() {
        // given - 이미 완료한 콘텐츠
        testReport.getCompletedContentIds().add(CHAPTER_ID_1);

        DailyCompletion existing = DailyCompletion.builder()
                .userId(TEST_USER_ID)
                .completionDate(today)
                .firstCompletionCount(1)
                .totalCompletionCount(1)
                .completedContents(new ArrayList<>())
                .streakCount(0)
                .createdAt(Instant.now())
                .build();

        when(userStudyReportRepository.findByUserId(TEST_USER_ID))
                .thenReturn(Optional.of(testReport));
        when(dailyCompletionRepository.findByUserIdAndCompletionDate(TEST_USER_ID, today))
                .thenReturn(Optional.of(existing));

        // when - 재완료이므로 streakUpdated=false
        streakService.addCompletedContent(TEST_USER_ID, CONTENT_TYPE, CHAPTER_ID_1, false);

        // then - totalCount는 증가, firstCount는 유지
        ArgumentCaptor<DailyCompletion> captor = ArgumentCaptor.forClass(DailyCompletion.class);
        verify(dailyCompletionRepository).save(captor.capture());

        DailyCompletion saved = captor.getValue();
        assertThat(saved.getFirstCompletionCount()).isEqualTo(1); // 유지
        assertThat(saved.getTotalCompletionCount()).isEqualTo(2); // 증가
    }

    @Test
    @DisplayName("같은 날 여러 콘텐츠 완료 시 모두 기록, firstCount는 첫 완료만 증가")
    void addCompletedContent_MultipleContents_OnlyFirstIncrementsFirstCount() {
        // given
        when(userStudyReportRepository.findByUserId(TEST_USER_ID))
                .thenReturn(Optional.of(testReport));

        DailyCompletion dailyAfterFirst = DailyCompletion.builder()
                .userId(TEST_USER_ID)
                .completionDate(today)
                .firstCompletionCount(1)
                .totalCompletionCount(1)
                .completedContents(new ArrayList<>())
                .streakCount(0)
                .createdAt(Instant.now())
                .build();

        when(dailyCompletionRepository.findByUserIdAndCompletionDate(TEST_USER_ID, today))
                .thenReturn(Optional.empty())      // 첫 번째 완료
                .thenReturn(Optional.of(dailyAfterFirst)); // 두 번째 완료

        // when - 첫 번째는 스트릭 완료, 두 번째는 같은 날이라 스트릭 X
        streakService.addCompletedContent(TEST_USER_ID, CONTENT_TYPE, CHAPTER_ID_1, true);  // 스트릭 완료
        streakService.addCompletedContent(TEST_USER_ID, CONTENT_TYPE, CHAPTER_ID_2, false); // 같은 날 추가 완료

        // then
        assertThat(testReport.getCompletedContentIds()).contains(CHAPTER_ID_1, CHAPTER_ID_2);

        ArgumentCaptor<DailyCompletion> dailyCaptor = ArgumentCaptor.forClass(DailyCompletion.class);
        verify(dailyCompletionRepository, times(2)).save(dailyCaptor.capture());

        // 두 번째 저장에서 firstCount는 1, totalCount는 2
        DailyCompletion secondSave = dailyCaptor.getAllValues().get(1);
        assertThat(secondSave.getFirstCompletionCount()).isEqualTo(2); // 두 번째도 첫 완료
        assertThat(secondSave.getTotalCompletionCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("재완료 콘텐츠: firstCompletionCount는 유지, totalCompletionCount는 증가")
    void addCompletedContent_Recompletion_OnlyIncrementsTotalCount() {
        // given - 첫 완료
        when(userStudyReportRepository.findByUserId(TEST_USER_ID))
                .thenReturn(Optional.of(testReport));

        DailyCompletion dailyAfterFirst = DailyCompletion.builder()
                .userId(TEST_USER_ID)
                .completionDate(today)
                .firstCompletionCount(1)
                .totalCompletionCount(1)
                .completedContents(new ArrayList<>())
                .streakCount(0)
                .createdAt(Instant.now())
                .build();

        when(dailyCompletionRepository.findByUserIdAndCompletionDate(TEST_USER_ID, today))
                .thenReturn(Optional.empty())      // 첫 완료
                .thenReturn(Optional.of(dailyAfterFirst)); // 재완료

        // when - 첫 완료
        streakService.addCompletedContent(TEST_USER_ID, CONTENT_TYPE, CHAPTER_ID_1, false);

        // 재완료 시도
        streakService.addCompletedContent(TEST_USER_ID, CONTENT_TYPE, CHAPTER_ID_1, false);

        // then
        ArgumentCaptor<DailyCompletion> dailyCaptor = ArgumentCaptor.forClass(DailyCompletion.class);
        verify(dailyCompletionRepository, times(2)).save(dailyCaptor.capture());

        // 두 번째 저장 (재완료)에서 firstCount는 1, totalCount는 2가 되어야 함
        DailyCompletion secondSave = dailyCaptor.getAllValues().get(1);
        assertThat(secondSave.getFirstCompletionCount()).isEqualTo(1); // 유지
        assertThat(secondSave.getTotalCompletionCount()).isEqualTo(2); // 증가
    }

    @Test
    @DisplayName("30초 미만 + 새 콘텐츠: 스트릭 X, 학습 완료 O")
    void scenario_ShortSession_NewContent_NoStreakButRecordsCompletion() {
        // given - 30초 미만
        when(dailyCompletionRepository.findByUserIdAndCompletionDate(TEST_USER_ID, today))
                .thenReturn(Optional.empty());
        when(readingSessionService.isReadingSessionValid(TEST_USER_ID, CONTENT_TYPE, CHAPTER_ID_1))
                .thenReturn(false); // 30초 미만
        when(userStudyReportRepository.findByUserId(TEST_USER_ID))
                .thenReturn(Optional.of(testReport));

        // when
        boolean streakResult = streakService.updateStreak(TEST_USER_ID, CONTENT_TYPE, CHAPTER_ID_1);
        streakService.addCompletedContent(TEST_USER_ID, CONTENT_TYPE, CHAPTER_ID_1, streakResult);

        // then
        assertThat(streakResult).isFalse(); // 스트릭은 실패
        assertThat(testReport.getCompletedContentIds()).contains(CHAPTER_ID_1); // 완료는 기록됨

        ArgumentCaptor<DailyCompletion> dailyCaptor = ArgumentCaptor.forClass(DailyCompletion.class);
        verify(dailyCompletionRepository).save(dailyCaptor.capture());

        DailyCompletion saved = dailyCaptor.getValue();
        assertThat(saved.getFirstCompletionCount()).isEqualTo(1);
        assertThat(saved.getTotalCompletionCount()).isEqualTo(1);
        assertThat(saved.getStreakStatus()).isEqualTo(StreakStatus.MISSED); // 스트릭 미완료 상태
    }

    @Test
    @DisplayName("30초 미만 + 재완료 콘텐츠: 스트릭 X, totalCount만 증가")
    void scenario_ShortSession_Recompletion_OnlyIncrementsTotalCount() {
        // given - 이미 완료한 콘텐츠
        testReport.getCompletedContentIds().add(CHAPTER_ID_1);

        DailyCompletion existing = DailyCompletion.builder()
                .userId(TEST_USER_ID)
                .completionDate(today)
                .firstCompletionCount(1)
                .totalCompletionCount(1)
                .completedContents(new ArrayList<>())
                .streakCount(0)
                .streakStatus(StreakStatus.MISSED)
                .createdAt(Instant.now())
                .build();

        when(dailyCompletionRepository.findByUserIdAndCompletionDate(TEST_USER_ID, today))
                .thenReturn(Optional.of(existing));
        when(readingSessionService.isReadingSessionValid(TEST_USER_ID, CONTENT_TYPE, CHAPTER_ID_1))
                .thenReturn(false);
        when(userStudyReportRepository.findByUserId(TEST_USER_ID))
                .thenReturn(Optional.of(testReport));

        // when
        boolean streakResult = streakService.updateStreak(TEST_USER_ID, CONTENT_TYPE, CHAPTER_ID_1);
        streakService.addCompletedContent(TEST_USER_ID, CONTENT_TYPE, CHAPTER_ID_1, streakResult);

        // then
        assertThat(streakResult).isFalse();

        ArgumentCaptor<DailyCompletion> dailyCaptor = ArgumentCaptor.forClass(DailyCompletion.class);
        verify(dailyCompletionRepository).save(dailyCaptor.capture());

        DailyCompletion saved = dailyCaptor.getValue();
        assertThat(saved.getFirstCompletionCount()).isEqualTo(1); // 유지
        assertThat(saved.getTotalCompletionCount()).isEqualTo(2); // 증가
    }

    @Test
    @DisplayName("스트릭과 학습 완료 기록은 독립적")
    void streakAndCompletionAreIndependent() {
        // given
        when(userStudyReportRepository.findByUserId(TEST_USER_ID))
                .thenReturn(Optional.of(testReport));
        when(readingSessionService.isReadingSessionValid(eq(TEST_USER_ID), eq(CONTENT_TYPE), any()))
                .thenReturn(true);

        // 첫 번째 완료 후 상태
        DailyCompletion dailyAfterFirst = DailyCompletion.builder()
                .userId(TEST_USER_ID)
                .completionDate(today)
                .streakStatus(StreakStatus.COMPLETED)
                .streakCount(1)
                .firstCompletionCount(1)
                .totalCompletionCount(1)
                .completedContents(new ArrayList<>())
                .createdAt(Instant.now())
                .build();

        when(dailyCompletionRepository.findByUserIdAndCompletionDate(TEST_USER_ID, today))
                .thenReturn(Optional.empty())           // 첫 스트릭
                .thenReturn(Optional.of(dailyAfterFirst)) // 두 번째 스트릭 시도
                .thenReturn(Optional.empty())           // 첫 완료
                .thenReturn(Optional.of(dailyAfterFirst)); // 두 번째 완료

        // when
        boolean firstStreak = streakService.updateStreak(TEST_USER_ID, CONTENT_TYPE, CHAPTER_ID_1);
        boolean secondStreak = streakService.updateStreak(TEST_USER_ID, CONTENT_TYPE, CHAPTER_ID_2);

        streakService.addCompletedContent(TEST_USER_ID, CONTENT_TYPE, CHAPTER_ID_1, firstStreak);
        streakService.addCompletedContent(TEST_USER_ID, CONTENT_TYPE, CHAPTER_ID_2, secondStreak);

        // then
        assertThat(firstStreak).isTrue();   // 첫 스트릭 성공
        assertThat(secondStreak).isFalse(); // 같은 날 두 번째는 실패
        assertThat(testReport.getCompletedContentIds()).contains(CHAPTER_ID_1, CHAPTER_ID_2); // 완료는 둘 다 기록
    }
}
