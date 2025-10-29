package com.linglevel.api.streak.scheduler;

import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.SendResponse;
import com.linglevel.api.fcm.dto.FcmMessageRequest;
import com.linglevel.api.fcm.entity.FcmPlatform;
import com.linglevel.api.fcm.entity.FcmToken;
import com.linglevel.api.fcm.repository.FcmTokenRepository;
import com.linglevel.api.fcm.service.FcmMessagingService;
import com.linglevel.api.i18n.CountryCode;
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
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("일일 스트릭 리마인더 스케줄러 테스트")
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
    @DisplayName("활성 스트릭이 있는 사용자가 없으면 알림을 보내지 않는다")
    void skipNotificationWhenNoActiveUsers() {
        // Given
        when(userStudyReportRepository.findByCurrentStreakGreaterThan(0))
                .thenReturn(new ArrayList<>());

        // When
        scheduler.sendDailyStreakReminders();

        // Then
        verify(fcmMessagingService, never()).sendMessage(anyString(), any());
        verify(fcmMessagingService, never()).sendMulticastMessage(anyList(), any());
    }

    @Test
    @DisplayName("모든 활성 사용자가 오늘 완료했으면 알림을 보내지 않는다")
    void skipNotificationWhenAllUsersCompleted() {
        // Given
        List<UserStudyReport> activeReports = List.of(
                createReport("user1", 5),
                createReport("user2", 10)
        );

        when(userStudyReportRepository.findByCurrentStreakGreaterThan(0))
                .thenReturn(activeReports);
        when(dailyCompletionRepository.existsByUserIdAndCompletionDate(anyString(), any(LocalDate.class)))
                .thenReturn(true);

        // When
        scheduler.sendDailyStreakReminders();

        // Then
        verify(fcmMessagingService, never()).sendMessage(anyString(), any());
        verify(fcmMessagingService, never()).sendMulticastMessage(anyList(), any());
    }

    @Test
    @DisplayName("오늘 완료하지 않은 사용자에게만 알림을 보낸다")
    void sendNotificationOnlyToUsersWhoHaveNotCompleted() {
        // Given
        UserStudyReport completedUser = createReport("user1", 5);
        UserStudyReport notCompletedUser = createReport("user2", 10);

        when(userStudyReportRepository.findByCurrentStreakGreaterThan(0))
                .thenReturn(List.of(completedUser, notCompletedUser));

        // user1은 완료, user2는 미완료
        when(dailyCompletionRepository.existsByUserIdAndCompletionDate("user1", today))
                .thenReturn(true);
        when(dailyCompletionRepository.existsByUserIdAndCompletionDate("user2", today))
                .thenReturn(false);

        // user2는 FCM 토큰이 있음
        when(fcmTokenRepository.findByUserIdAndIsActive("user2", true))
                .thenReturn(List.of(createFcmToken("user2", "token123")));

        when(fcmMessagingService.sendMessage(anyString(), any(FcmMessageRequest.class)))
                .thenReturn("message-id-123");

        // When
        scheduler.sendDailyStreakReminders();

        // Then
        verify(fcmMessagingService, times(1)).sendMessage(eq("token123"), any(FcmMessageRequest.class));
        verify(fcmMessagingService, never()).sendMulticastMessage(anyList(), any());
    }

    @Test
    @DisplayName("FCM 토큰이 없는 사용자에게는 알림을 보내지 않는다")
    void skipNotificationForUsersWithoutToken() {
        // Given
        UserStudyReport report = createReport("user1", 5);

        when(userStudyReportRepository.findByCurrentStreakGreaterThan(0))
                .thenReturn(List.of(report));
        when(dailyCompletionRepository.existsByUserIdAndCompletionDate("user1", today))
                .thenReturn(false);
        when(fcmTokenRepository.findByUserIdAndIsActive("user1", true))
                .thenReturn(new ArrayList<>()); // 토큰 없음

        // When
        scheduler.sendDailyStreakReminders();

        // Then
        verify(fcmMessagingService, never()).sendMessage(anyString(), any());
        verify(fcmMessagingService, never()).sendMulticastMessage(anyList(), any());
    }

    @Test
    @DisplayName("단일 토큰이 있는 사용자에게 sendMessage를 사용한다")
    void useSendMessageForSingleToken() {
        // Given
        UserStudyReport report = createReport("user1", 5);

        when(userStudyReportRepository.findByCurrentStreakGreaterThan(0))
                .thenReturn(List.of(report));
        when(dailyCompletionRepository.existsByUserIdAndCompletionDate("user1", today))
                .thenReturn(false);
        when(fcmTokenRepository.findByUserIdAndIsActive("user1", true))
                .thenReturn(List.of(createFcmToken("user1", "token123")));
        when(fcmMessagingService.sendMessage(anyString(), any(FcmMessageRequest.class)))
                .thenReturn("message-id-123");

        // When
        scheduler.sendDailyStreakReminders();

        // Then
        verify(fcmMessagingService, times(1)).sendMessage(eq("token123"), any(FcmMessageRequest.class));
        verify(fcmMessagingService, never()).sendMulticastMessage(anyList(), any());
    }

    @Test
    @DisplayName("여러 토큰이 있는 사용자에게 sendMulticastMessage를 사용한다")
    void useSendMulticastMessageForMultipleTokens() {
        // Given
        UserStudyReport report = createReport("user1", 5);
        List<FcmToken> tokens = List.of(
                createFcmToken("user1", "token1"),
                createFcmToken("user1", "token2"),
                createFcmToken("user1", "token3")
        );

        when(userStudyReportRepository.findByCurrentStreakGreaterThan(0))
                .thenReturn(List.of(report));
        when(dailyCompletionRepository.existsByUserIdAndCompletionDate("user1", today))
                .thenReturn(false);
        when(fcmTokenRepository.findByUserIdAndIsActive("user1", true))
                .thenReturn(tokens);

        BatchResponse mockBatchResponse = mock(BatchResponse.class);
        when(mockBatchResponse.getSuccessCount()).thenReturn(3);
        when(mockBatchResponse.getFailureCount()).thenReturn(0);
        when(fcmMessagingService.sendMulticastMessage(anyList(), any(FcmMessageRequest.class)))
                .thenReturn(mockBatchResponse);

        // When
        scheduler.sendDailyStreakReminders();

        // Then
        verify(fcmMessagingService, never()).sendMessage(anyString(), any());
        verify(fcmMessagingService, times(1)).sendMulticastMessage(
                argThat(tokenList -> tokenList.size() == 3 &&
                        tokenList.contains("token1") &&
                        tokenList.contains("token2") &&
                        tokenList.contains("token3")),
                any(FcmMessageRequest.class)
        );
    }

    @Test
    @DisplayName("여러 사용자를 동시에 처리한다")
    void processMultipleUsers() {
        // Given
        List<UserStudyReport> reports = List.of(
                createReport("user1", 5),
                createReport("user2", 10),
                createReport("user3", 15)
        );

        when(userStudyReportRepository.findByCurrentStreakGreaterThan(0))
                .thenReturn(reports);

        // 모든 사용자가 미완료
        when(dailyCompletionRepository.existsByUserIdAndCompletionDate(anyString(), any(LocalDate.class)))
                .thenReturn(false);

        // 각 사용자는 단일 토큰 보유
        when(fcmTokenRepository.findByUserIdAndIsActive("user1", true))
                .thenReturn(List.of(createFcmToken("user1", "token1")));
        when(fcmTokenRepository.findByUserIdAndIsActive("user2", true))
                .thenReturn(List.of(createFcmToken("user2", "token2")));
        when(fcmTokenRepository.findByUserIdAndIsActive("user3", true))
                .thenReturn(List.of(createFcmToken("user3", "token3")));

        when(fcmMessagingService.sendMessage(anyString(), any(FcmMessageRequest.class)))
                .thenReturn("message-id");

        // When
        scheduler.sendDailyStreakReminders();

        // Then
        verify(fcmMessagingService, times(3)).sendMessage(anyString(), any(FcmMessageRequest.class));
    }

    @Test
    @DisplayName("개별 사용자 알림 실패 시에도 다른 사용자는 계속 처리된다")
    void continueProcessingOnIndividualFailure() {
        // Given
        List<UserStudyReport> reports = List.of(
                createReport("user1", 5),
                createReport("user2", 10),
                createReport("user3", 15)
        );

        when(userStudyReportRepository.findByCurrentStreakGreaterThan(0))
                .thenReturn(reports);
        when(dailyCompletionRepository.existsByUserIdAndCompletionDate(anyString(), any(LocalDate.class)))
                .thenReturn(false);

        when(fcmTokenRepository.findByUserIdAndIsActive("user1", true))
                .thenReturn(List.of(createFcmToken("user1", "token1")));
        when(fcmTokenRepository.findByUserIdAndIsActive("user2", true))
                .thenReturn(List.of(createFcmToken("user2", "token2")));
        when(fcmTokenRepository.findByUserIdAndIsActive("user3", true))
                .thenReturn(List.of(createFcmToken("user3", "token3")));

        // user2 알림 전송 실패
        when(fcmMessagingService.sendMessage(eq("token1"), any(FcmMessageRequest.class)))
                .thenReturn("message-id-1");
        when(fcmMessagingService.sendMessage(eq("token2"), any(FcmMessageRequest.class)))
                .thenThrow(new RuntimeException("FCM service unavailable"));
        when(fcmMessagingService.sendMessage(eq("token3"), any(FcmMessageRequest.class)))
                .thenReturn("message-id-3");

        // When
        scheduler.sendDailyStreakReminders();

        // Then - user1과 user3는 정상 처리됨
        verify(fcmMessagingService, times(3)).sendMessage(anyString(), any(FcmMessageRequest.class));
    }

    @Test
    @DisplayName("알림 메시지가 올바른 형식으로 전송된다")
    void sendNotificationWithCorrectFormat() {
        // Given
        UserStudyReport report = createReport("user1", 5);

        when(userStudyReportRepository.findByCurrentStreakGreaterThan(0))
                .thenReturn(List.of(report));
        when(dailyCompletionRepository.existsByUserIdAndCompletionDate("user1", today))
                .thenReturn(false);
        when(fcmTokenRepository.findByUserIdAndIsActive("user1", true))
                .thenReturn(List.of(createFcmToken("user1", "token123")));
        when(fcmMessagingService.sendMessage(anyString(), any(FcmMessageRequest.class)))
                .thenReturn("message-id");

        // When
        scheduler.sendDailyStreakReminders();

        // Then
        verify(fcmMessagingService).sendMessage(
                eq("token123"),
                argThat(request ->
                        request.getTitle().equals("Keep your streak alive!") &&
                        request.getBody().equals("Don't forget to complete your daily learning to maintain your streak.") &&
                        request.getType().equals("streak_reminder") &&
                        request.getCampaignId().equals("daily_streak_reminder_9am") &&
                        request.getAction().equals("open_app")
                )
        );
    }

    @Test
    @DisplayName("비활성 FCM 토큰은 무시된다")
    void ignoreInactiveTokens() {
        // Given
        UserStudyReport report = createReport("user1", 5);

        when(userStudyReportRepository.findByCurrentStreakGreaterThan(0))
                .thenReturn(List.of(report));
        when(dailyCompletionRepository.existsByUserIdAndCompletionDate("user1", today))
                .thenReturn(false);
        when(fcmTokenRepository.findByUserIdAndIsActive("user1", true))
                .thenReturn(new ArrayList<>()); // 활성 토큰 없음

        // When
        scheduler.sendDailyStreakReminders();

        // Then
        verify(fcmMessagingService, never()).sendMessage(anyString(), any());
        verify(fcmMessagingService, never()).sendMulticastMessage(anyList(), any());
    }

    // Helper methods
    private UserStudyReport createReport(String userId, int currentStreak) {
        UserStudyReport report = new UserStudyReport();
        report.setUserId(userId);
        report.setCurrentStreak(currentStreak);
        report.setLongestStreak(currentStreak);
        report.setLastCompletionDate(today.minusDays(1));
        report.setCreatedAt(Instant.now());
        return report;
    }

    private FcmToken createFcmToken(String userId, String token) {
        return FcmToken.builder()
                .id("token-id-" + token)
                .userId(userId)
                .deviceId("device-" + userId)
                .fcmToken(token)
                .platform(FcmPlatform.IOS)
                .countryCode(CountryCode.KR)
                .appVersion("1.0.0")
                .osVersion("15.0")
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
    }
}
