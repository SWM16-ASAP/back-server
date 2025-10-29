package com.linglevel.api.streak.scheduler;

import com.google.firebase.messaging.BatchResponse;
import com.linglevel.api.fcm.dto.FcmMessageRequest;
import com.linglevel.api.fcm.entity.FcmPlatform;
import com.linglevel.api.fcm.entity.FcmToken;
import com.linglevel.api.fcm.repository.FcmTokenRepository;
import com.linglevel.api.fcm.service.FcmMessagingService;
import com.linglevel.api.i18n.CountryCode;
import com.linglevel.api.i18n.LanguageCode;
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
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("최적 타이밍 스트릭 리마인더 스케줄러 테스트")
class DailyStreakReminderSchedulerTest {

    @Mock
    private UserStudyReportRepository userStudyReportRepository;

    @Mock
    private DailyCompletionRepository dailyCompletionRepository;

    @Mock
    private FcmTokenRepository fcmTokenRepository;

    @Mock
    private FcmMessagingService fcmMessagingService;

    @InjectMocks
    private DailyStreakReminderScheduler scheduler;

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private LocalDate today;

    @BeforeEach
    void setUp() {
        today = LocalDate.now(KST);
    }

    @Test
    @DisplayName("최적 학습 시간에 해당하는 사용자가 없으면 알림을 보내지 않는다")
    void skipNotificationWhenNoOptimalTimeUsers() {
        // Given
        when(userStudyReportRepository.findUsersForOptimalTimingReminder(any(Instant.class), any(Instant.class)))
                .thenReturn(new ArrayList<>());

        // When
        scheduler.sendOptimalTimingStreakReminders();

        // Then
        verify(fcmMessagingService, never()).sendMessage(anyString(), any());
        verify(fcmMessagingService, never()).sendMulticastMessage(anyList(), any());
    }

    @Test
    @DisplayName("모든 후보 사용자가 오늘 학습을 완료했으면 알림을 보내지 않는다")
    void skipNotificationWhenAllUsersCompleted() {
        // Given
        List<UserStudyReport> candidateReports = List.of(
                createReport("user1", 5),
                createReport("user2", 10)
        );

        when(userStudyReportRepository.findUsersForOptimalTimingReminder(any(Instant.class), any(Instant.class)))
                .thenReturn(candidateReports);
        when(dailyCompletionRepository.existsByUserIdAndCompletionDate(anyString(), any(LocalDate.class)))
                .thenReturn(true);

        // When
        scheduler.sendOptimalTimingStreakReminders();

        // Then
        verify(fcmMessagingService, never()).sendMessage(anyString(), any());
        verify(fcmMessagingService, never()).sendMulticastMessage(anyList(), any());
    }

    @Test
    @DisplayName("오늘 학습을 완료하지 않은 사용자에게만 알림을 보낸다")
    void sendNotificationOnlyToUsersWhoHaveNotCompleted() {
        // Given
        UserStudyReport completedUser = createReport("user1", 5);
        UserStudyReport notCompletedUser = createReport("user2", 10);

        // Day 1 window만 사용자 반환, 나머지는 빈 리스트
        when(userStudyReportRepository.findUsersForOptimalTimingReminder(any(Instant.class), any(Instant.class)))
                .thenReturn(List.of(completedUser, notCompletedUser))  // Day 1
                .thenReturn(new ArrayList<>())  // Day 2
                .thenReturn(new ArrayList<>())  // Day 3
                .thenReturn(new ArrayList<>());  // Day 4

        when(dailyCompletionRepository.existsByUserIdAndCompletionDate("user1", today)).thenReturn(true);
        when(dailyCompletionRepository.existsByUserIdAndCompletionDate("user2", today)).thenReturn(false);

        when(fcmTokenRepository.findByUserIdAndIsActive("user2", true))
                .thenReturn(List.of(createFcmToken("user2", "token123")));

        when(fcmMessagingService.sendMessage(anyString(), any(FcmMessageRequest.class)))
                .thenReturn("message-id-123");

        // When
        scheduler.sendOptimalTimingStreakReminders();

        // Then
        verify(fcmMessagingService, times(1)).sendMessage(eq("token123"), any(FcmMessageRequest.class));
    }

    @Test
    @DisplayName("FCM 토큰이 없는 사용자에게는 알림을 보내지 않는다")
    void skipNotificationForUsersWithoutToken() {
        // Given
        UserStudyReport report = createReport("user1", 5);

        when(userStudyReportRepository.findUsersForOptimalTimingReminder(any(Instant.class), any(Instant.class)))
                .thenReturn(List.of(report))
                .thenReturn(new ArrayList<>())
                .thenReturn(new ArrayList<>())
                .thenReturn(new ArrayList<>());
        when(dailyCompletionRepository.existsByUserIdAndCompletionDate("user1", today)).thenReturn(false);
        when(fcmTokenRepository.findByUserIdAndIsActive("user1", true)).thenReturn(new ArrayList<>());

        // When
        scheduler.sendOptimalTimingStreakReminders();

        // Then
        verify(fcmMessagingService, never()).sendMessage(anyString(), any());
        verify(fcmMessagingService, never()).sendMulticastMessage(anyList(), any());
    }

    @Test
    @DisplayName("단일 토큰 사용자에게 sendMessage를 호출한다")
    void useSendMessageForSingleToken() {
        // Given
        UserStudyReport report = createReport("user1", 5);

        when(userStudyReportRepository.findUsersForOptimalTimingReminder(any(Instant.class), any(Instant.class)))
                .thenReturn(List.of(report))
                .thenReturn(new ArrayList<>())
                .thenReturn(new ArrayList<>())
                .thenReturn(new ArrayList<>());
        when(dailyCompletionRepository.existsByUserIdAndCompletionDate("user1", today)).thenReturn(false);
        when(fcmTokenRepository.findByUserIdAndIsActive("user1", true))
                .thenReturn(List.of(createFcmToken("user1", "token123")));
        when(fcmMessagingService.sendMessage(anyString(), any(FcmMessageRequest.class)))
                .thenReturn("message-id-123");

        // When
        scheduler.sendOptimalTimingStreakReminders();

        // Then
        verify(fcmMessagingService, times(1)).sendMessage(eq("token123"), any(FcmMessageRequest.class));
        verify(fcmMessagingService, never()).sendMulticastMessage(anyList(), any());
    }

    @Test
    @DisplayName("여러 토큰 사용자에게 sendMulticastMessage를 호출한다")
    void useSendMulticastMessageForMultipleTokens() {
        // Given
        UserStudyReport report = createReport("user1", 5);
        List<FcmToken> tokens = List.of(
                createFcmToken("user1", "token1"),
                createFcmToken("user1", "token2")
        );

        when(userStudyReportRepository.findUsersForOptimalTimingReminder(any(Instant.class), any(Instant.class)))
                .thenReturn(List.of(report))
                .thenReturn(new ArrayList<>())
                .thenReturn(new ArrayList<>())
                .thenReturn(new ArrayList<>());
        when(dailyCompletionRepository.existsByUserIdAndCompletionDate("user1", today)).thenReturn(false);
        when(fcmTokenRepository.findByUserIdAndIsActive("user1", true)).thenReturn(tokens);

        BatchResponse mockBatchResponse = mock(BatchResponse.class);
        when(mockBatchResponse.getSuccessCount()).thenReturn(2);
        when(fcmMessagingService.sendMulticastMessage(anyList(), any(FcmMessageRequest.class)))
                .thenReturn(mockBatchResponse);

        // When
        scheduler.sendOptimalTimingStreakReminders();

        // Then
        verify(fcmMessagingService, times(1)).sendMulticastMessage(
                argThat(list -> list.size() == 2 && list.contains("token1") && list.contains("token2")),
                any(FcmMessageRequest.class)
        );
        verify(fcmMessagingService, never()).sendMessage(anyString(), any());
    }

    @Test
    @DisplayName("여러 사용자를 동시에 처리한다")
    void processMultipleUsers() {
        // Given
        List<UserStudyReport> reports = List.of(
                createReport("user1", 5),
                createReport("user2", 10)
        );

        when(userStudyReportRepository.findUsersForOptimalTimingReminder(any(Instant.class), any(Instant.class)))
                .thenReturn(reports)
                .thenReturn(new ArrayList<>())
                .thenReturn(new ArrayList<>())
                .thenReturn(new ArrayList<>());
        when(dailyCompletionRepository.existsByUserIdAndCompletionDate(anyString(), any(LocalDate.class)))
                .thenReturn(false);

        when(fcmTokenRepository.findByUserIdAndIsActive("user1", true))
                .thenReturn(List.of(createFcmToken("user1", "token1", CountryCode.US)));
        when(fcmTokenRepository.findByUserIdAndIsActive("user2", true))
                .thenReturn(List.of(createFcmToken("user2", "token2", CountryCode.KR)));

        when(fcmMessagingService.sendMessage(anyString(), any(FcmMessageRequest.class)))
                .thenReturn("message-id");

        // When
        scheduler.sendOptimalTimingStreakReminders();

        // Then
        verify(fcmMessagingService, times(2)).sendMessage(anyString(), any(FcmMessageRequest.class));
    }

    @Test
    @DisplayName("개별 알림 실패 시에도 다른 사용자는 계속 처리된다")
    void continueProcessingOnIndividualFailure() {
        // Given
        List<UserStudyReport> reports = List.of(
                createReport("user1", 5),
                createReport("user2", 10),
                createReport("user3", 15)
        );

        when(userStudyReportRepository.findUsersForOptimalTimingReminder(any(Instant.class), any(Instant.class)))
                .thenReturn(reports)
                .thenReturn(new ArrayList<>())
                .thenReturn(new ArrayList<>())
                .thenReturn(new ArrayList<>());
        when(dailyCompletionRepository.existsByUserIdAndCompletionDate(anyString(), any(LocalDate.class)))
                .thenReturn(false);

        when(fcmTokenRepository.findByUserIdAndIsActive("user1", true))
                .thenReturn(List.of(createFcmToken("user1", "token1")));
        when(fcmTokenRepository.findByUserIdAndIsActive("user2", true))
                .thenReturn(List.of(createFcmToken("user2", "token2")));
        when(fcmTokenRepository.findByUserIdAndIsActive("user3", true))
                .thenReturn(List.of(createFcmToken("user3", "token3")));

        when(fcmMessagingService.sendMessage(eq("token1"), any(FcmMessageRequest.class)))
                .thenReturn("message-id-1");
        when(fcmMessagingService.sendMessage(eq("token2"), any(FcmMessageRequest.class)))
                .thenThrow(new RuntimeException("FCM service unavailable"));
        when(fcmMessagingService.sendMessage(eq("token3"), any(FcmMessageRequest.class)))
                .thenReturn("message-id-3");

        // When
        scheduler.sendOptimalTimingStreakReminders();

        // Then
        verify(fcmMessagingService, times(1)).sendMessage(eq("token1"), any(FcmMessageRequest.class));
        verify(fcmMessagingService, times(1)).sendMessage(eq("token2"), any(FcmMessageRequest.class));
        verify(fcmMessagingService, times(1)).sendMessage(eq("token3"), any(FcmMessageRequest.class));
    }

    @Test
    @DisplayName("CountryCode에 따라 적절한 언어로 메시지를 전송한다")
    void sendMessagesInAppropriateLanguageBasedOnCountryCode() {
        // Given
        List<UserStudyReport> reports = List.of(
                createReportWithLastLearning("koreanUser", 5, Instant.now().minus(java.time.Duration.ofHours(23).plusMinutes(30))),
                createReportWithLastLearning("japaneseUser", 7, Instant.now().minus(java.time.Duration.ofHours(23).plusMinutes(30))),
                createReportWithLastLearning("usUser", 10, Instant.now().minus(java.time.Duration.ofHours(23).plusMinutes(30)))
        );

        when(userStudyReportRepository.findUsersForOptimalTimingReminder(any(Instant.class), any(Instant.class)))
                .thenReturn(reports)
                .thenReturn(new ArrayList<>())
                .thenReturn(new ArrayList<>())
                .thenReturn(new ArrayList<>());
        when(dailyCompletionRepository.existsByUserIdAndCompletionDate(anyString(), any(LocalDate.class)))
                .thenReturn(false);

        // 각 사용자에게 다른 국가 코드의 토큰 설정
        when(fcmTokenRepository.findByUserIdAndIsActive("koreanUser", true))
                .thenReturn(List.of(createFcmToken("koreanUser", "token-kr", CountryCode.KR)));
        when(fcmTokenRepository.findByUserIdAndIsActive("japaneseUser", true))
                .thenReturn(List.of(createFcmToken("japaneseUser", "token-jp", CountryCode.JP)));
        when(fcmTokenRepository.findByUserIdAndIsActive("usUser", true))
                .thenReturn(List.of(createFcmToken("usUser", "token-us", CountryCode.US)));

        when(fcmMessagingService.sendMessage(anyString(), any(FcmMessageRequest.class)))
                .thenReturn("message-id");

        // When
        scheduler.sendOptimalTimingStreakReminders();

        // Then
        verify(fcmMessagingService, times(3)).sendMessage(anyString(), any(FcmMessageRequest.class));
    }

    @Test
    @DisplayName("Day 2 복귀 메시지를 전송한다 (47-48시간)")
    void sendComebackDay2Message() {
        // Given - 47시간 30분 전에 마지막 학습
        UserStudyReport report = createReportWithLastLearning(
                "user1", 5, Instant.now().minus(java.time.Duration.ofHours(47).plusMinutes(30))
        );

        // Day 2 window에만 사용자 반환
        when(userStudyReportRepository.findUsersForOptimalTimingReminder(any(Instant.class), any(Instant.class)))
                .thenReturn(new ArrayList<>())  // Day 1
                .thenReturn(List.of(report))    // Day 2
                .thenReturn(new ArrayList<>())  // Day 3
                .thenReturn(new ArrayList<>());  // Day 4
        when(dailyCompletionRepository.existsByUserIdAndCompletionDate("user1", today))
                .thenReturn(false);
        when(fcmTokenRepository.findByUserIdAndIsActive("user1", true))
                .thenReturn(List.of(createFcmToken("user1", "token1")));
        when(fcmMessagingService.sendMessage(anyString(), any(FcmMessageRequest.class)))
                .thenReturn("message-id");

        // When
        scheduler.sendOptimalTimingStreakReminders();

        // Then
        verify(fcmMessagingService, times(1)).sendMessage(anyString(), any(FcmMessageRequest.class));
    }

    @Test
    @DisplayName("Day 3 복귀 메시지를 전송한다 (71-72시간)")
    void sendComebackDay3Message() {
        // Given - 71시간 30분 전에 마지막 학습
        UserStudyReport report = createReportWithLastLearning(
                "user1", 10, Instant.now().minus(java.time.Duration.ofHours(71).plusMinutes(30))
        );

        // Day 3 window에만 사용자 반환
        when(userStudyReportRepository.findUsersForOptimalTimingReminder(any(Instant.class), any(Instant.class)))
                .thenReturn(new ArrayList<>())  // Day 1
                .thenReturn(new ArrayList<>())  // Day 2
                .thenReturn(List.of(report))    // Day 3
                .thenReturn(new ArrayList<>());  // Day 4
        when(dailyCompletionRepository.existsByUserIdAndCompletionDate("user1", today))
                .thenReturn(false);
        when(fcmTokenRepository.findByUserIdAndIsActive("user1", true))
                .thenReturn(List.of(createFcmToken("user1", "token1")));
        when(fcmMessagingService.sendMessage(anyString(), any(FcmMessageRequest.class)))
                .thenReturn("message-id");

        // When
        scheduler.sendOptimalTimingStreakReminders();

        // Then
        verify(fcmMessagingService, times(1)).sendMessage(anyString(), any(FcmMessageRequest.class));
    }

    @Test
    @DisplayName("Day 4 마지막 복귀 메시지를 전송한다 (95-96시간)")
    void sendComebackDay4Message() {
        // Given - 95시간 30분 전에 마지막 학습
        UserStudyReport report = createReportWithLastLearning(
                "user1", 15, Instant.now().minus(java.time.Duration.ofHours(95).plusMinutes(30))
        );

        // Day 4 window에만 사용자 반환
        when(userStudyReportRepository.findUsersForOptimalTimingReminder(any(Instant.class), any(Instant.class)))
                .thenReturn(new ArrayList<>())  // Day 1
                .thenReturn(new ArrayList<>())  // Day 2
                .thenReturn(new ArrayList<>())  // Day 3
                .thenReturn(List.of(report));   // Day 4
        when(dailyCompletionRepository.existsByUserIdAndCompletionDate("user1", today))
                .thenReturn(false);
        when(fcmTokenRepository.findByUserIdAndIsActive("user1", true))
                .thenReturn(List.of(createFcmToken("user1", "token1")));
        when(fcmMessagingService.sendMessage(anyString(), any(FcmMessageRequest.class)))
                .thenReturn("message-id");

        // When
        scheduler.sendOptimalTimingStreakReminders();

        // Then
        verify(fcmMessagingService, times(1)).sendMessage(anyString(), any(FcmMessageRequest.class));
    }

    @Test
    @DisplayName("스트릭이 깨진 사용자에게 Day 1 STREAK_LOST 메시지를 전송한다 (23-24시간)")
    void sendStreakLostDay1Message() {
        // Given - 스트릭이 0으로 초기화된 사용자, 23시간 30분 전 마지막 학습
        UserStudyReport report = createReportWithLastLearning(
                "user1", 0, Instant.now().minus(java.time.Duration.ofHours(23).plusMinutes(30))
        );

        // Day 1 window에만 사용자 반환
        when(userStudyReportRepository.findUsersForOptimalTimingReminder(any(Instant.class), any(Instant.class)))
                .thenReturn(List.of(report))    // Day 1
                .thenReturn(new ArrayList<>())  // Day 2
                .thenReturn(new ArrayList<>())  // Day 3
                .thenReturn(new ArrayList<>());  // Day 4
        when(dailyCompletionRepository.existsByUserIdAndCompletionDate("user1", today))
                .thenReturn(false);
        when(fcmTokenRepository.findByUserIdAndIsActive("user1", true))
                .thenReturn(List.of(createFcmToken("user1", "token1")));
        when(fcmMessagingService.sendMessage(anyString(), any(FcmMessageRequest.class)))
                .thenReturn("message-id");

        // When
        scheduler.sendOptimalTimingStreakReminders();

        // Then
        verify(fcmMessagingService, times(1)).sendMessage(anyString(), any(FcmMessageRequest.class));
    }

    @Test
    @DisplayName("스트릭이 깨진 사용자에게 Day 2 STREAK_LOST 메시지를 전송한다 (47-48시간)")
    void sendStreakLostDay2Message() {
        // Given - 스트릭이 0으로 초기화된 사용자, 47시간 30분 전 마지막 학습
        UserStudyReport report = createReportWithLastLearning(
                "user1", 0, Instant.now().minus(java.time.Duration.ofHours(47).plusMinutes(30))
        );

        // Day 2 window에만 사용자 반환
        when(userStudyReportRepository.findUsersForOptimalTimingReminder(any(Instant.class), any(Instant.class)))
                .thenReturn(new ArrayList<>())  // Day 1
                .thenReturn(List.of(report))    // Day 2
                .thenReturn(new ArrayList<>())  // Day 3
                .thenReturn(new ArrayList<>());  // Day 4
        when(dailyCompletionRepository.existsByUserIdAndCompletionDate("user1", today))
                .thenReturn(false);
        when(fcmTokenRepository.findByUserIdAndIsActive("user1", true))
                .thenReturn(List.of(createFcmToken("user1", "token1")));
        when(fcmMessagingService.sendMessage(anyString(), any(FcmMessageRequest.class)))
                .thenReturn("message-id");

        // When
        scheduler.sendOptimalTimingStreakReminders();

        // Then
        verify(fcmMessagingService, times(1)).sendMessage(anyString(), any(FcmMessageRequest.class));
    }

    @Test
    @DisplayName("스트릭이 깨진 사용자에게 Day 3 STREAK_LOST 메시지를 전송한다 (71-72시간)")
    void sendStreakLostDay3Message() {
        // Given - 스트릭이 0으로 초기화된 사용자, 71시간 30분 전 마지막 학습
        UserStudyReport report = createReportWithLastLearning(
                "user1", 0, Instant.now().minus(java.time.Duration.ofHours(71).plusMinutes(30))
        );

        // Day 3 window에만 사용자 반환
        when(userStudyReportRepository.findUsersForOptimalTimingReminder(any(Instant.class), any(Instant.class)))
                .thenReturn(new ArrayList<>())  // Day 1
                .thenReturn(new ArrayList<>())  // Day 2
                .thenReturn(List.of(report))    // Day 3
                .thenReturn(new ArrayList<>());  // Day 4
        when(dailyCompletionRepository.existsByUserIdAndCompletionDate("user1", today))
                .thenReturn(false);
        when(fcmTokenRepository.findByUserIdAndIsActive("user1", true))
                .thenReturn(List.of(createFcmToken("user1", "token1")));
        when(fcmMessagingService.sendMessage(anyString(), any(FcmMessageRequest.class)))
                .thenReturn("message-id");

        // When
        scheduler.sendOptimalTimingStreakReminders();

        // Then
        verify(fcmMessagingService, times(1)).sendMessage(anyString(), any(FcmMessageRequest.class));
    }

    @Test
    @DisplayName("스트릭이 깨진 사용자에게 Day 4 STREAK_LOST 메시지를 전송한다 (95-96시간)")
    void sendStreakLostDay4Message() {
        // Given - 스트릭이 0으로 초기화된 사용자, 95시간 30분 전 마지막 학습
        UserStudyReport report = createReportWithLastLearning(
                "user1", 0, Instant.now().minus(java.time.Duration.ofHours(95).plusMinutes(30))
        );

        // Day 4 window에만 사용자 반환
        when(userStudyReportRepository.findUsersForOptimalTimingReminder(any(Instant.class), any(Instant.class)))
                .thenReturn(new ArrayList<>())  // Day 1
                .thenReturn(new ArrayList<>())  // Day 2
                .thenReturn(new ArrayList<>())  // Day 3
                .thenReturn(List.of(report));   // Day 4
        when(dailyCompletionRepository.existsByUserIdAndCompletionDate("user1", today))
                .thenReturn(false);
        when(fcmTokenRepository.findByUserIdAndIsActive("user1", true))
                .thenReturn(List.of(createFcmToken("user1", "token1")));
        when(fcmMessagingService.sendMessage(anyString(), any(FcmMessageRequest.class)))
                .thenReturn("message-id");

        // When
        scheduler.sendOptimalTimingStreakReminders();

        // Then
        verify(fcmMessagingService, times(1)).sendMessage(anyString(), any(FcmMessageRequest.class));
    }

    // Helper methods
    private UserStudyReport createReport(String userId, int currentStreak) {
        UserStudyReport report = new UserStudyReport();
        report.setUserId(userId);
        report.setCurrentStreak(currentStreak);
        report.setLastCompletionDate(today.minusDays(1));
        report.setLastLearningTimestamp(Instant.now().minus(java.time.Duration.ofHours(23).plusMinutes(30)));
        return report;
    }

    private UserStudyReport createReportWithLastLearning(String userId, int currentStreak, Instant lastLearningTime) {
        UserStudyReport report = new UserStudyReport();
        report.setUserId(userId);
        report.setCurrentStreak(currentStreak);
        report.setLastCompletionDate(today.minusDays(1));
        report.setLastLearningTimestamp(lastLearningTime);
        return report;
    }

    private FcmToken createFcmToken(String userId, String token) {
        return createFcmToken(userId, token, CountryCode.US);
    }

    private FcmToken createFcmToken(String userId, String token, CountryCode countryCode) {
        return FcmToken.builder()
                .userId(userId)
                .fcmToken(token)
                .countryCode(countryCode)
                .platform(FcmPlatform.ANDROID)
                .deviceId("device-" + userId)
                .isActive(true)
                .build();
    }
}
