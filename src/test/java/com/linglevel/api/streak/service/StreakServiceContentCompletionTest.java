package com.linglevel.api.streak.service;

import com.linglevel.api.content.common.ContentType;
import com.linglevel.api.streak.entity.DailyCompletion;
import com.linglevel.api.streak.entity.UserStudyReport;
import com.linglevel.api.streak.repository.DailyCompletionRepository;
import com.linglevel.api.streak.repository.FreezeTransactionRepository;
import com.linglevel.api.streak.repository.UserStudyReportRepository;
import com.linglevel.api.user.ticket.repository.TicketTransactionRepository;
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
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("StreakService - 학습 완료 기록 테스트")
class StreakServiceContentCompletionTest {

    @Mock
    private UserStudyReportRepository userStudyReportRepository;

    @Mock
    private DailyCompletionRepository dailyCompletionRepository;

    @Mock
    private TicketService ticketService;

    @Mock
    private FreezeTransactionRepository freezeTransactionRepository;

    @Mock
    private TicketTransactionRepository ticketTransactionRepository;

    @Mock
    private ReadingSessionService readingSessionService;

    @InjectMocks
    private StreakService streakService;

    private static final String TEST_USER_ID = "test-user-123";
    private static final String CONTENT_ID_1 = "content-chapter-1";
    private static final String CONTENT_ID_2 = "content-chapter-2";
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
        testReport.setCreatedAt(Instant.now());
    }

    @Test
    @DisplayName("첫 완료 시 UserStudyReport.completedContentIds에 추가")
    void addCompletedContent_FirstCompletion_AddsToReport() {
        // given
        when(userStudyReportRepository.findByUserId(TEST_USER_ID))
                .thenReturn(Optional.of(testReport));
        when(dailyCompletionRepository.findByUserIdAndCompletionDate(TEST_USER_ID, today))
                .thenReturn(Optional.empty());

        // when
        streakService.addCompletedContent(TEST_USER_ID, CONTENT_TYPE, CONTENT_ID_1);

        // then
        assertThat(testReport.getCompletedContentIds()).contains(CONTENT_ID_1);
        verify(userStudyReportRepository).save(testReport);
        verify(dailyCompletionRepository).save(any(DailyCompletion.class));
    }

    @Test
    @DisplayName("이미 완료한 콘텐츠 재완료 시 중복 기록 안됨")
    void addCompletedContent_DuplicateCompletion_Skipped() {
        // given
        testReport.getCompletedContentIds().add(CONTENT_ID_1); // 이미 완료됨
        when(userStudyReportRepository.findByUserId(TEST_USER_ID))
                .thenReturn(Optional.of(testReport));

        // when
        streakService.addCompletedContent(TEST_USER_ID, CONTENT_TYPE, CONTENT_ID_1);

        // then
        verify(dailyCompletionRepository, never()).save(any());
        verify(userStudyReportRepository, never()).save(any());
    }

    @Test
    @DisplayName("같은 날 여러 콘텐츠 완료 시 모두 기록됨")
    void addCompletedContent_MultipleContentsOnSameDay_AllRecorded() {
        // given
        when(userStudyReportRepository.findByUserId(TEST_USER_ID))
                .thenReturn(Optional.of(testReport));

        DailyCompletion existingDaily = DailyCompletion.builder()
                .userId(TEST_USER_ID)
                .completionDate(today)
                .firstCompletionCount(1)
                .totalCompletionCount(1)
                .completedContents(new ArrayList<>())
                .streakCount(1)
                .createdAt(Instant.now())
                .build();

        when(dailyCompletionRepository.findByUserIdAndCompletionDate(TEST_USER_ID, today))
                .thenReturn(Optional.empty()) // 첫 완료
                .thenReturn(Optional.of(existingDaily)); // 두 번째 완료

        // when
        streakService.addCompletedContent(TEST_USER_ID, CONTENT_TYPE, CONTENT_ID_1);
        streakService.addCompletedContent(TEST_USER_ID, CONTENT_TYPE, CONTENT_ID_2);

        // then
        assertThat(testReport.getCompletedContentIds())
                .contains(CONTENT_ID_1, CONTENT_ID_2);
        verify(userStudyReportRepository, times(2)).save(testReport);
        verify(dailyCompletionRepository, times(2)).save(any(DailyCompletion.class));
    }

    @Test
    @DisplayName("첫 완료 시 DailyCompletion에 firstCompletionCount 증가")
    void addCompletedContent_FirstCompletion_IncrementsFirstCount() {
        // given
        when(userStudyReportRepository.findByUserId(TEST_USER_ID))
                .thenReturn(Optional.of(testReport));

        DailyCompletion existingDaily = DailyCompletion.builder()
                .userId(TEST_USER_ID)
                .completionDate(today)
                .firstCompletionCount(0)
                .totalCompletionCount(0)
                .completedContents(new ArrayList<>())
                .streakCount(0)
                .createdAt(Instant.now())
                .build();

        when(dailyCompletionRepository.findByUserIdAndCompletionDate(TEST_USER_ID, today))
                .thenReturn(Optional.of(existingDaily));

        // when
        streakService.addCompletedContent(TEST_USER_ID, CONTENT_TYPE, CONTENT_ID_1);

        // then
        assertThat(existingDaily.getFirstCompletionCount()).isEqualTo(1);
        assertThat(existingDaily.getTotalCompletionCount()).isEqualTo(1);
        assertThat(existingDaily.getCompletedContents()).hasSize(1);
        verify(dailyCompletionRepository).save(existingDaily);
    }

    @Test
    @DisplayName("completedContentIds null일 때 초기화 후 추가")
    void addCompletedContent_NullCompletedContentIds_InitializesAndAdds() {
        // given
        testReport.setCompletedContentIds(null);
        when(userStudyReportRepository.findByUserId(TEST_USER_ID))
                .thenReturn(Optional.of(testReport));
        when(dailyCompletionRepository.findByUserIdAndCompletionDate(TEST_USER_ID, today))
                .thenReturn(Optional.empty());

        // when
        streakService.addCompletedContent(TEST_USER_ID, CONTENT_TYPE, CONTENT_ID_1);

        // then
        assertThat(testReport.getCompletedContentIds()).isNotNull();
        assertThat(testReport.getCompletedContentIds()).contains(CONTENT_ID_1);
    }

    @Test
    @DisplayName("updateStreak: 세션 유효하지 않으면 false 반환")
    void updateStreak_InvalidSession_ReturnsFalse() {
        // given
        when(dailyCompletionRepository.findByUserIdAndCompletionDate(TEST_USER_ID, today))
                .thenReturn(Optional.empty());
        when(readingSessionService.isReadingSessionValid(TEST_USER_ID, CONTENT_TYPE, CONTENT_ID_1))
                .thenReturn(false);

        // when
        boolean result = streakService.updateStreak(TEST_USER_ID, CONTENT_TYPE, CONTENT_ID_1);

        // then
        assertThat(result).isFalse();
        verify(userStudyReportRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateStreak: 같은 날 두 번 호출 시 두 번째는 false 반환")
    void updateStreak_CalledTwiceSameDay_SecondReturnsFalse() {
        // given
        DailyCompletion existingDaily = DailyCompletion.builder()
                .userId(TEST_USER_ID)
                .completionDate(today)
                .firstCompletionCount(0)
                .totalCompletionCount(1) // 이미 오늘 완료함
                .completedContents(new ArrayList<>())
                .streakCount(1)
                .createdAt(Instant.now())
                .build();

        when(dailyCompletionRepository.findByUserIdAndCompletionDate(TEST_USER_ID, today))
                .thenReturn(Optional.of(existingDaily));

        // when
        boolean result = streakService.updateStreak(TEST_USER_ID, CONTENT_TYPE, CONTENT_ID_1);

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("스트릭과 학습 완료가 독립적으로 동작")
    void streakAndCompletionAreIndependent() {
        // given - 첫 번째 콘텐츠로 스트릭 완료
        when(userStudyReportRepository.findByUserId(TEST_USER_ID))
                .thenReturn(Optional.of(testReport));
        when(dailyCompletionRepository.findByUserIdAndCompletionDate(TEST_USER_ID, today))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(DailyCompletion.builder()
                        .userId(TEST_USER_ID)
                        .completionDate(today)
                        .firstCompletionCount(1)
                        .totalCompletionCount(1)
                        .completedContents(new ArrayList<>())
                        .streakCount(1)
                        .createdAt(Instant.now())
                        .build()));
        when(readingSessionService.isReadingSessionValid(eq(TEST_USER_ID), eq(CONTENT_TYPE), any()))
                .thenReturn(true);

        // when - 첫 번째 콘텐츠로 스트릭 업데이트
        boolean firstStreakResult = streakService.updateStreak(TEST_USER_ID, CONTENT_TYPE, CONTENT_ID_1);

        // 두 번째 콘텐츠는 스트릭 업데이트 안됨 (같은 날)
        boolean secondStreakResult = streakService.updateStreak(TEST_USER_ID, CONTENT_TYPE, CONTENT_ID_2);

        // 하지만 학습 완료 기록은 둘 다 됨
        streakService.addCompletedContent(TEST_USER_ID, CONTENT_TYPE, CONTENT_ID_1);
        streakService.addCompletedContent(TEST_USER_ID, CONTENT_TYPE, CONTENT_ID_2);

        // then
        assertThat(firstStreakResult).isTrue(); // 스트릭은 한 번만
        assertThat(secondStreakResult).isFalse(); // 같은 날 두 번째는 안됨
        assertThat(testReport.getCompletedContentIds()).contains(CONTENT_ID_1, CONTENT_ID_2); // 완료 기록은 둘 다
    }
}
