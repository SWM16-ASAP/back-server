package com.linglevel.api.streak.scheduler;

import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.SendResponse;
import com.linglevel.api.fcm.dto.FcmMessageRequest;
import com.linglevel.api.fcm.entity.FcmToken;
import com.linglevel.api.fcm.repository.FcmTokenRepository;
import com.linglevel.api.fcm.service.FcmMessagingService;
import com.linglevel.api.fcm.service.FcmTokenService;
import com.linglevel.api.i18n.CountryCode;
import com.linglevel.api.i18n.LanguageCode;
import com.linglevel.api.streak.entity.UserStudyReport;
import com.linglevel.api.streak.repository.DailyCompletionRepository;
import com.linglevel.api.streak.repository.UserStudyReportRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("스트릭 보호 스케줄러 테스트")
class StreakProtectionSchedulerTest {

    @Mock
    private UserStudyReportRepository userStudyReportRepository;

    @Mock
    private DailyCompletionRepository dailyCompletionRepository;

    @Mock
    private com.linglevel.api.streak.repository.FreezeTransactionRepository freezeTransactionRepository;

    @Mock
    private FcmTokenRepository fcmTokenRepository;

    @Mock
    private FcmMessagingService fcmMessagingService;

    @Mock
    private FcmTokenService fcmTokenService;

    @InjectMocks
    private StreakProtectionScheduler scheduler;

    private static final ZoneId KST = ZoneId.of("Asia/Seoul");
    private LocalDate today;

    @BeforeEach
    void setUp() {
        today = LocalDate.now(KST);
    }

    @Test
    @DisplayName("스트릭이 있고 오늘 학습 미완료한 사용자에게 알림 전송")
    void sendNotification_ToActiveUserWithoutCompletion() throws Exception {
        // given
        UserStudyReport user = createUserReport("user1", 5);
        when(userStudyReportRepository.findByCurrentStreakGreaterThan(0))
                .thenReturn(List.of(user));

        // 오늘 학습 미완료
        when(dailyCompletionRepository.existsByUserIdAndCompletionDate("user1", today))
                .thenReturn(false);

        // 프리즈 사용 안함
        when(freezeTransactionRepository.findByUserIdAndAmountAndCreatedAtBetween(
                eq("user1"), eq(-1), any(), any()))
                .thenReturn(List.of());

        // FCM 토큰 있음
        FcmToken token = createFcmToken("user1", "token1", CountryCode.KR);
        when(fcmTokenRepository.findByUserIdAndIsActive("user1", true))
                .thenReturn(List.of(token));

        // when
        scheduler.sendStreakProtectionNotifications();

        // then
        verify(fcmMessagingService).sendMessage(eq("token1"), any(FcmMessageRequest.class));
    }

    @Test
    @DisplayName("오늘 이미 학습 완료한 사용자는 알림 전송 안함")
    void noNotification_WhenAlreadyCompleted() throws Exception {
        // given
        UserStudyReport user = createUserReport("user1", 5);
        when(userStudyReportRepository.findByCurrentStreakGreaterThan(0))
                .thenReturn(List.of(user));

        // 오늘 학습 완료
        when(dailyCompletionRepository.existsByUserIdAndCompletionDate("user1", today))
                .thenReturn(true);

        // when
        scheduler.sendStreakProtectionNotifications();

        // then
        verify(fcmMessagingService, never()).sendMessage(anyString(), any(FcmMessageRequest.class));
    }

    @Test
    @DisplayName("FCM 토큰이 없는 사용자는 알림 전송 안함")
    void noNotification_WhenNoFcmToken() throws Exception {
        // given
        UserStudyReport user = createUserReport("user1", 5);
        when(userStudyReportRepository.findByCurrentStreakGreaterThan(0))
                .thenReturn(List.of(user));

        when(dailyCompletionRepository.existsByUserIdAndCompletionDate("user1", today))
                .thenReturn(false);

        // FCM 토큰 없음
        when(fcmTokenRepository.findByUserIdAndIsActive("user1", true))
                .thenReturn(List.of());

        // when
        scheduler.sendStreakProtectionNotifications();

        // then
        verify(fcmMessagingService, never()).sendMessage(anyString(), any(FcmMessageRequest.class));
    }

    @Test
    @DisplayName("여러 토큰이 있는 사용자에게 멀티캐스트 전송")
    void sendMulticast_WhenMultipleTokens() throws Exception {
        // given
        UserStudyReport user = createUserReport("user1", 5);
        when(userStudyReportRepository.findByCurrentStreakGreaterThan(0))
                .thenReturn(List.of(user));

        when(dailyCompletionRepository.existsByUserIdAndCompletionDate("user1", today))
                .thenReturn(false);

        // 프리즈 사용 안함
        when(freezeTransactionRepository.findByUserIdAndAmountAndCreatedAtBetween(
                eq("user1"), eq(-1), any(), any()))
                .thenReturn(List.of());

        // 여러 FCM 토큰
        List<FcmToken> tokens = List.of(
                createFcmToken("user1", "token1", CountryCode.KR),
                createFcmToken("user1", "token2", CountryCode.KR)
        );
        when(fcmTokenRepository.findByUserIdAndIsActive("user1", true))
                .thenReturn(tokens);

        BatchResponse batchResponse = mock(BatchResponse.class);
        when(batchResponse.getSuccessCount()).thenReturn(2);
        when(batchResponse.getFailureCount()).thenReturn(0);

        List<SendResponse> responses = new ArrayList<>();
        SendResponse successResponse1 = mock(SendResponse.class);
        when(successResponse1.isSuccessful()).thenReturn(true);
        SendResponse successResponse2 = mock(SendResponse.class);
        when(successResponse2.isSuccessful()).thenReturn(true);
        responses.add(successResponse1);
        responses.add(successResponse2);

        when(batchResponse.getResponses()).thenReturn(responses);
        when(fcmMessagingService.sendMulticastMessage(anyList(), any(FcmMessageRequest.class)))
                .thenReturn(batchResponse);

        // when
        scheduler.sendStreakProtectionNotifications();

        // then
        verify(fcmMessagingService).sendMulticastMessage(anyList(), any(FcmMessageRequest.class));
        verify(fcmMessagingService, never()).sendMessage(anyString(), any(FcmMessageRequest.class));
    }

    @Test
    @DisplayName("언어 코드 변환 - 한국어")
    void languageConversion_Korean() throws Exception {
        // given
        UserStudyReport user = createUserReport("user1", 3);
        when(userStudyReportRepository.findByCurrentStreakGreaterThan(0))
                .thenReturn(List.of(user));

        when(dailyCompletionRepository.existsByUserIdAndCompletionDate("user1", today))
                .thenReturn(false);

        // 프리즈 사용 안함
        when(freezeTransactionRepository.findByUserIdAndAmountAndCreatedAtBetween(
                eq("user1"), eq(-1), any(), any()))
                .thenReturn(List.of());

        FcmToken token = createFcmToken("user1", "token1", CountryCode.KR);
        when(fcmTokenRepository.findByUserIdAndIsActive("user1", true))
                .thenReturn(List.of(token));

        // when
        scheduler.sendStreakProtectionNotifications();

        // then
        verify(fcmMessagingService).sendMessage(eq("token1"), argThat(request ->
                request.getTitle().contains("불꽃") ||
                request.getTitle().contains("스트릭") ||
                request.getTitle().contains("마지막") ||
                request.getTitle().contains("늦지") ||
                request.getTitle().contains("자기") ||
                request.getTitle().contains("남았") ||
                request.getTitle().contains("기다려") ||
                request.getTitle().contains("기회") ||
                request.getTitle().contains("거의") ||
                request.getTitle().contains("오늘")
        ));
    }

    @Test
    @DisplayName("언어 코드 변환 - 영어")
    void languageConversion_English() throws Exception {
        // given
        UserStudyReport user = createUserReport("user1", 3);
        when(userStudyReportRepository.findByCurrentStreakGreaterThan(0))
                .thenReturn(List.of(user));

        when(dailyCompletionRepository.existsByUserIdAndCompletionDate("user1", today))
                .thenReturn(false);

        // 프리즈 사용 안함
        when(freezeTransactionRepository.findByUserIdAndAmountAndCreatedAtBetween(
                eq("user1"), eq(-1), any(), any()))
                .thenReturn(List.of());

        FcmToken token = createFcmToken("user1", "token1", CountryCode.US);
        when(fcmTokenRepository.findByUserIdAndIsActive("user1", true))
                .thenReturn(List.of(token));

        // when
        scheduler.sendStreakProtectionNotifications();

        // then
        verify(fcmMessagingService).sendMessage(eq("token1"), argThat(request ->
                request.getTitle().contains("flame") ||
                request.getTitle().contains("streak") ||
                request.getTitle().contains("chance") ||
                request.getTitle().contains("late") ||
                request.getTitle().contains("minutes") ||
                request.getTitle().contains("left") ||
                request.getTitle().contains("waiting") ||
                request.getTitle().contains("Almost") ||
                request.getTitle().contains("Only")
        ));
    }

    @Test
    @DisplayName("언어 코드 변환 - 일본어")
    void languageConversion_Japanese() throws Exception {
        // given
        UserStudyReport user = createUserReport("user1", 3);
        when(userStudyReportRepository.findByCurrentStreakGreaterThan(0))
                .thenReturn(List.of(user));

        when(dailyCompletionRepository.existsByUserIdAndCompletionDate("user1", today))
                .thenReturn(false);

        // 프리즈 사용 안함
        when(freezeTransactionRepository.findByUserIdAndAmountAndCreatedAtBetween(
                eq("user1"), eq(-1), any(), any()))
                .thenReturn(List.of());

        FcmToken token = createFcmToken("user1", "token1", CountryCode.JP);
        when(fcmTokenRepository.findByUserIdAndIsActive("user1", true))
                .thenReturn(List.of(token));

        // when
        scheduler.sendStreakProtectionNotifications();

        // then
        verify(fcmMessagingService).sendMessage(eq("token1"), argThat(request ->
                request.getTitle().contains("炎") ||
                request.getTitle().contains("ストリーク") ||
                request.getTitle().contains("チャンス") ||
                request.getTitle().contains("遅く") ||
                request.getTitle().contains("寝る前") ||
                request.getTitle().contains("残って") ||
                request.getTitle().contains("待って") ||
                request.getTitle().contains("もうすぐ") ||
                request.getTitle().contains("今日")
        ));
    }

    @Test
    @DisplayName("메시지에 현재 스트릭 수가 포함됨")
    void messageContainsStreakCount() throws Exception {
        // given
        UserStudyReport user = createUserReport("user1", 7);
        when(userStudyReportRepository.findByCurrentStreakGreaterThan(0))
                .thenReturn(List.of(user));

        when(dailyCompletionRepository.existsByUserIdAndCompletionDate("user1", today))
                .thenReturn(false);

        // 프리즈 사용 안함
        when(freezeTransactionRepository.findByUserIdAndAmountAndCreatedAtBetween(
                eq("user1"), eq(-1), any(), any()))
                .thenReturn(List.of());

        FcmToken token = createFcmToken("user1", "token1", CountryCode.KR);
        when(fcmTokenRepository.findByUserIdAndIsActive("user1", true))
                .thenReturn(List.of(token));

        // when
        scheduler.sendStreakProtectionNotifications();

        // then
        verify(fcmMessagingService).sendMessage(eq("token1"), argThat(request ->
                request.getBody().contains("7")
        ));
    }

    @Test
    @DisplayName("여러 사용자에게 개별 알림 전송")
    void sendToMultipleUsers() throws Exception {
        // given
        List<UserStudyReport> users = List.of(
                createUserReport("user1", 3),
                createUserReport("user2", 5),
                createUserReport("user3", 7)
        );
        when(userStudyReportRepository.findByCurrentStreakGreaterThan(0))
                .thenReturn(users);

        // 모두 학습 미완료
        when(dailyCompletionRepository.existsByUserIdAndCompletionDate(anyString(), eq(today)))
                .thenReturn(false);

        // 프리즈 사용 안함
        when(freezeTransactionRepository.findByUserIdAndAmountAndCreatedAtBetween(
                anyString(), eq(-1), any(), any()))
                .thenReturn(List.of());

        // 각각 FCM 토큰 있음
        when(fcmTokenRepository.findByUserIdAndIsActive("user1", true))
                .thenReturn(List.of(createFcmToken("user1", "token1", CountryCode.KR)));
        when(fcmTokenRepository.findByUserIdAndIsActive("user2", true))
                .thenReturn(List.of(createFcmToken("user2", "token2", CountryCode.US)));
        when(fcmTokenRepository.findByUserIdAndIsActive("user3", true))
                .thenReturn(List.of(createFcmToken("user3", "token3", CountryCode.JP)));

        // when
        scheduler.sendStreakProtectionNotifications();

        // then
        verify(fcmMessagingService, times(3)).sendMessage(anyString(), any(FcmMessageRequest.class));
    }

    @Test
    @DisplayName("어제 프리즈를 사용한 경우 STREAK_SAVED_BY_FREEZE 메시지 전송")
    void sendFreezeMessage_WhenFreezeUsedYesterday() throws Exception {
        // given
        UserStudyReport user = createUserReport("user1", 5);
        when(userStudyReportRepository.findByCurrentStreakGreaterThan(0))
                .thenReturn(List.of(user));

        when(dailyCompletionRepository.existsByUserIdAndCompletionDate("user1", today))
                .thenReturn(false);

        FcmToken token = createFcmToken("user1", "token1", CountryCode.KR);
        when(fcmTokenRepository.findByUserIdAndIsActive("user1", true))
                .thenReturn(List.of(token));

        // 어제 프리즈 사용 (amount = -1인 트랜잭션 존재)
        com.linglevel.api.streak.entity.FreezeTransaction freezeTransaction =
                com.linglevel.api.streak.entity.FreezeTransaction.builder()
                        .userId("user1")
                        .amount(-1)
                        .description("Auto-consumed for missed day")
                        .createdAt(today.minusDays(1).atStartOfDay(KST).toInstant())
                        .build();
        when(freezeTransactionRepository.findByUserIdAndAmountAndCreatedAtBetween(
                eq("user1"), eq(-1), any(), any()))
                .thenReturn(List.of(freezeTransaction));

        // when
        scheduler.sendStreakProtectionNotifications();

        // then
        verify(fcmMessagingService).sendMessage(eq("token1"), argThat(request -> {
            // STREAK_SAVED_BY_FREEZE 메시지는 프리즈 관련 키워드를 포함
            String title = request.getTitle();
            String body = request.getBody();
            return body.contains("프리즈") && (body.contains("꼭") || body.contains("반드시") || body.contains("학습"));
        }));
    }

    @Test
    @DisplayName("어제 프리즈를 사용하지 않은 경우 STREAK_PROTECTION 메시지 전송")
    void sendProtectionMessage_WhenNoFreezeUsed() throws Exception {
        // given
        UserStudyReport user = createUserReport("user1", 5);
        when(userStudyReportRepository.findByCurrentStreakGreaterThan(0))
                .thenReturn(List.of(user));

        when(dailyCompletionRepository.existsByUserIdAndCompletionDate("user1", today))
                .thenReturn(false);

        FcmToken token = createFcmToken("user1", "token1", CountryCode.KR);
        when(fcmTokenRepository.findByUserIdAndIsActive("user1", true))
                .thenReturn(List.of(token));

        // 어제 프리즈 사용 안함 (트랜잭션 없음)
        when(freezeTransactionRepository.findByUserIdAndAmountAndCreatedAtBetween(
                eq("user1"), eq(-1), any(), any()))
                .thenReturn(List.of());

        // when
        scheduler.sendStreakProtectionNotifications();

        // then
        verify(fcmMessagingService).sendMessage(eq("token1"), argThat(request -> {
            // STREAK_PROTECTION 메시지는 "자기 전", "5분", "늦지", "기회" 등의 키워드를 포함하거나
            // 스트릭 보호 관련 메시지 (단, 프리즈 언급은 없음)
            String title = request.getTitle();
            String body = request.getBody();
            return (title.contains("자기") || title.contains("남았") || title.contains("기다려") ||
                    title.contains("늦지") || title.contains("기회") || title.contains("불꽃") ||
                    title.contains("거의") || title.contains("마무리") || title.contains("스트릭")) &&
                   (!body.contains("프리즈"));  // 프리즈 언급 없음
        }));
    }

    @Test
    @DisplayName("프리즈 사용 여부 확인 - 어제 날짜 범위 정확성")
    void checkFreezeUsage_YesterdayDateRange() throws Exception {
        // given
        UserStudyReport user = createUserReport("user1", 5);
        when(userStudyReportRepository.findByCurrentStreakGreaterThan(0))
                .thenReturn(List.of(user));

        when(dailyCompletionRepository.existsByUserIdAndCompletionDate("user1", today))
                .thenReturn(false);

        FcmToken token = createFcmToken("user1", "token1", CountryCode.KR);
        when(fcmTokenRepository.findByUserIdAndIsActive("user1", true))
                .thenReturn(List.of(token));

        when(freezeTransactionRepository.findByUserIdAndAmountAndCreatedAtBetween(
                eq("user1"), eq(-1), any(), any()))
                .thenReturn(List.of());

        // when
        scheduler.sendStreakProtectionNotifications();

        // then - 정확한 시간 범위로 조회했는지 검증
        ArgumentCaptor<java.time.Instant> startCaptor = ArgumentCaptor.forClass(java.time.Instant.class);
        ArgumentCaptor<java.time.Instant> endCaptor = ArgumentCaptor.forClass(java.time.Instant.class);

        verify(freezeTransactionRepository).findByUserIdAndAmountAndCreatedAtBetween(
                eq("user1"),
                eq(-1),
                startCaptor.capture(),
                endCaptor.capture()
        );

        // 어제 00:00 ~ 오늘 00:00 범위 확인
        java.time.LocalDate yesterday = today.minusDays(1);
        java.time.Instant expectedStart = yesterday.atStartOfDay(KST).toInstant();
        java.time.Instant expectedEnd = today.atStartOfDay(KST).toInstant();

        assertThat(startCaptor.getValue()).isEqualTo(expectedStart);
        assertThat(endCaptor.getValue()).isEqualTo(expectedEnd);
    }

    @Test
    @DisplayName("전송 실패한 토큰은 비활성화됨")
    void deactivateFailedTokens() throws Exception {
        // given
        UserStudyReport user = createUserReport("user1", 5);
        when(userStudyReportRepository.findByCurrentStreakGreaterThan(0))
                .thenReturn(List.of(user));

        when(dailyCompletionRepository.existsByUserIdAndCompletionDate("user1", today))
                .thenReturn(false);

        // 프리즈 사용 안함
        when(freezeTransactionRepository.findByUserIdAndAmountAndCreatedAtBetween(
                eq("user1"), eq(-1), any(), any()))
                .thenReturn(List.of());

        List<FcmToken> tokens = List.of(
                createFcmToken("user1", "token1", CountryCode.KR),
                createFcmToken("user1", "token2", CountryCode.KR)
        );
        when(fcmTokenRepository.findByUserIdAndIsActive("user1", true))
                .thenReturn(tokens);

        // 하나는 성공, 하나는 실패
        BatchResponse batchResponse = mock(BatchResponse.class);
        when(batchResponse.getSuccessCount()).thenReturn(1);
        when(batchResponse.getFailureCount()).thenReturn(1);

        List<SendResponse> responses = new ArrayList<>();
        SendResponse successResponse = mock(SendResponse.class);
        when(successResponse.isSuccessful()).thenReturn(true);

        SendResponse failResponse = mock(SendResponse.class);
        when(failResponse.isSuccessful()).thenReturn(false);
        FirebaseMessagingException exception = mock(FirebaseMessagingException.class);
        when(exception.getMessage()).thenReturn("Invalid token");
        when(failResponse.getException()).thenReturn(exception);

        responses.add(successResponse);
        responses.add(failResponse);

        when(batchResponse.getResponses()).thenReturn(responses);
        when(fcmMessagingService.sendMulticastMessage(anyList(), any(FcmMessageRequest.class)))
                .thenReturn(batchResponse);

        // when
        scheduler.sendStreakProtectionNotifications();

        // then
        verify(fcmTokenService).deactivateToken("token2");
    }

    // Helper methods
    private UserStudyReport createUserReport(String userId, int currentStreak) {
        UserStudyReport report = new UserStudyReport();
        report.setUserId(userId);
        report.setCurrentStreak(currentStreak);
        return report;
    }

    private FcmToken createFcmToken(String userId, String token, CountryCode countryCode) {
        FcmToken fcmToken = new FcmToken();
        fcmToken.setUserId(userId);
        fcmToken.setFcmToken(token);
        fcmToken.setCountryCode(countryCode);
        fcmToken.setIsActive(true);
        return fcmToken;
    }
}
