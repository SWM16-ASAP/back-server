package com.linglevel.api.recommendation.scheduler;

import com.linglevel.api.content.common.ContentCategory;
import com.linglevel.api.content.common.ContentType;
import com.linglevel.api.recommendation.entity.ContentAccessLog;
import com.linglevel.api.recommendation.entity.UserCategoryPreference;
import com.linglevel.api.recommendation.repository.ContentAccessLogRepository;
import com.linglevel.api.recommendation.repository.UserCategoryPreferenceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("사용자 선호도 집계 스케줄러 테스트")
class UserPreferenceAggregationSchedulerTest {

    @Mock
    private ContentAccessLogRepository contentAccessLogRepository;

    @Mock
    private UserCategoryPreferenceRepository userCategoryPreferenceRepository;

    @InjectMocks
    private UserPreferenceAggregationScheduler scheduler;

    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        now = LocalDateTime.now();
    }

    @Test
    @DisplayName("로그가 없으면 집계를 건너뛴다")
    void skipAggregationWhenNoLogs() {
        // Given
        when(contentAccessLogRepository.findByAccessedAtAfter(any(LocalDateTime.class)))
                .thenReturn(new ArrayList<>());

        // When
        scheduler.aggregateUserPreferences();

        // Then
        verify(userCategoryPreferenceRepository, never()).save(any());
    }

    @Test
    @DisplayName("Article 접근 로그만 있는 경우 primaryCategory가 설정된다")
    void setPrimaryCategoryForArticleLogsOnly() {
        // Given
        String userId = "user123";
        List<ContentAccessLog> logs = List.of(
                createLog(userId, "article1", ContentType.ARTICLE, ContentCategory.TECH, now.minusDays(1)),
                createLog(userId, "article2", ContentType.ARTICLE, ContentCategory.TECH, now.minusDays(2)),
                createLog(userId, "article3", ContentType.ARTICLE, ContentCategory.BUSINESS, now.minusDays(3))
        );

        when(contentAccessLogRepository.findByAccessedAtAfter(any(LocalDateTime.class)))
                .thenReturn(logs);
        when(userCategoryPreferenceRepository.findByUserId(userId))
                .thenReturn(Optional.empty());
        when(userCategoryPreferenceRepository.save(any(UserCategoryPreference.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        scheduler.aggregateUserPreferences();

        // Then
        verify(userCategoryPreferenceRepository).save(argThat(preference ->
                preference.getUserId().equals(userId) &&
                preference.getPrimaryCategory() == ContentCategory.TECH &&
                preference.getTotalAccessCount() == 3 &&
                preference.getCategoryScores().containsKey(ContentCategory.TECH) &&
                preference.getCategoryScores().containsKey(ContentCategory.BUSINESS)
        ));
    }

    @Test
    @DisplayName("Book만 본 경우 primaryCategory는 null이다")
    void nullPrimaryCategoryForBookOnly() {
        // Given
        String userId = "user456";
        List<ContentAccessLog> logs = List.of(
                createLog(userId, "book1", ContentType.BOOK, null, now.minusDays(1)),
                createLog(userId, "book2", ContentType.BOOK, null, now.minusDays(2))
        );

        when(contentAccessLogRepository.findByAccessedAtAfter(any(LocalDateTime.class)))
                .thenReturn(logs);
        when(userCategoryPreferenceRepository.findByUserId(userId))
                .thenReturn(Optional.empty());
        when(userCategoryPreferenceRepository.save(any(UserCategoryPreference.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        scheduler.aggregateUserPreferences();

        // Then
        verify(userCategoryPreferenceRepository).save(argThat(preference ->
                preference.getUserId().equals(userId) &&
                preference.getPrimaryCategory() == null &&
                preference.getTotalAccessCount() == 2 &&
                preference.getCategoryScores().isEmpty()
        ));
    }

    @Test
    @DisplayName("시간 감쇠가 적용되어 최근 로그가 더 높은 가중치를 받는다")
    void applyTimeDecayWeighting() {
        // Given
        String userId = "user789";
        List<ContentAccessLog> logs = List.of(
                // 최근 7일 - 가중치 1.0
                createLog(userId, "article1", ContentType.ARTICLE, ContentCategory.TECH, now.minusDays(3)),
                // 7~30일 - 가중치 0.5
                createLog(userId, "article2", ContentType.ARTICLE, ContentCategory.BUSINESS, now.minusDays(15)),
                createLog(userId, "article3", ContentType.ARTICLE, ContentCategory.BUSINESS, now.minusDays(20)),
                // 30일 이전 - 가중치 0.2
                createLog(userId, "article4", ContentType.ARTICLE, ContentCategory.SPORTS, now.minusDays(60))
        );

        when(contentAccessLogRepository.findByAccessedAtAfter(any(LocalDateTime.class)))
                .thenReturn(logs);
        when(userCategoryPreferenceRepository.findByUserId(userId))
                .thenReturn(Optional.empty());
        when(userCategoryPreferenceRepository.save(any(UserCategoryPreference.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        scheduler.aggregateUserPreferences();

        // Then
        verify(userCategoryPreferenceRepository).save(argThat(preference -> {
            // TECH: 1.0, BUSINESS: 0.5 + 0.5 = 1.0, SPORTS: 0.2
            // 정규화 후: TECH = 1.0/2.2 = 0.45, BUSINESS = 1.0/2.2 = 0.45, SPORTS = 0.2/2.2 = 0.09
            Double techScore = preference.getCategoryScores().get(ContentCategory.TECH);
            Double businessScore = preference.getCategoryScores().get(ContentCategory.BUSINESS);

            // TECH와 BUSINESS가 거의 동일한 점수여야 함
            return Math.abs(techScore - businessScore) < 0.01;
        }));
    }

    @Test
    @DisplayName("여러 사용자 로그를 동시에 처리한다")
    void aggregateMultipleUsers() {
        // Given
        List<ContentAccessLog> logs = List.of(
                createLog("user1", "article1", ContentType.ARTICLE, ContentCategory.TECH, now.minusDays(1)),
                createLog("user2", "article2", ContentType.ARTICLE, ContentCategory.BUSINESS, now.minusDays(1)),
                createLog("user3", "book1", ContentType.BOOK, null, now.minusDays(1))
        );

        when(contentAccessLogRepository.findByAccessedAtAfter(any(LocalDateTime.class)))
                .thenReturn(logs);
        when(userCategoryPreferenceRepository.findByUserId(anyString()))
                .thenReturn(Optional.empty());
        when(userCategoryPreferenceRepository.save(any(UserCategoryPreference.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        scheduler.aggregateUserPreferences();

        // Then
        verify(userCategoryPreferenceRepository, times(3)).save(any(UserCategoryPreference.class));
    }

    @Test
    @DisplayName("개별 사용자 처리 실패 시에도 다른 사용자는 계속 처리된다")
    void continueProcessingOnIndividualFailure() {
        // Given
        List<ContentAccessLog> logs = List.of(
                createLog("user1", "article1", ContentType.ARTICLE, ContentCategory.TECH, now.minusDays(1)),
                createLog("user2", "article2", ContentType.ARTICLE, ContentCategory.BUSINESS, now.minusDays(1)),
                createLog("user3", "article3", ContentType.ARTICLE, ContentCategory.SPORTS, now.minusDays(1))
        );

        when(contentAccessLogRepository.findByAccessedAtAfter(any(LocalDateTime.class)))
                .thenReturn(logs);

        // user2 처리 시 에러 발생
        when(userCategoryPreferenceRepository.findByUserId("user1"))
                .thenReturn(Optional.empty());
        when(userCategoryPreferenceRepository.findByUserId("user2"))
                .thenThrow(new RuntimeException("DB connection failed"));
        when(userCategoryPreferenceRepository.findByUserId("user3"))
                .thenReturn(Optional.empty());

        when(userCategoryPreferenceRepository.save(any(UserCategoryPreference.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        scheduler.aggregateUserPreferences();

        // Then - user1과 user3는 정상 처리됨
        verify(userCategoryPreferenceRepository, times(2)).save(any(UserCategoryPreference.class));
    }

    @Test
    @DisplayName("기존 선호도 데이터가 있으면 업데이트한다")
    void updateExistingPreference() {
        // Given
        String userId = "existingUser";
        UserCategoryPreference existingPreference = UserCategoryPreference.builder()
                .id("pref123")
                .userId(userId)
                .primaryCategory(ContentCategory.SPORTS)
                .totalAccessCount(5)
                .build();

        List<ContentAccessLog> newLogs = List.of(
                createLog(userId, "article1", ContentType.ARTICLE, ContentCategory.TECH, now.minusDays(1))
        );

        when(contentAccessLogRepository.findByAccessedAtAfter(any(LocalDateTime.class)))
                .thenReturn(newLogs);
        when(userCategoryPreferenceRepository.findByUserId(userId))
                .thenReturn(Optional.of(existingPreference));
        when(userCategoryPreferenceRepository.save(any(UserCategoryPreference.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        // When
        scheduler.aggregateUserPreferences();

        // Then
        verify(userCategoryPreferenceRepository).save(argThat(preference ->
                preference.getId().equals("pref123") && // 기존 ID 유지
                preference.getPrimaryCategory() == ContentCategory.TECH && // 새로운 카테고리로 업데이트
                preference.getTotalAccessCount() == 1 // 새로운 로그 개수
        ));
    }

    // Helper method
    private ContentAccessLog createLog(String userId, String contentId, ContentType contentType,
                                       ContentCategory category, LocalDateTime accessedAt) {
        return ContentAccessLog.builder()
                .userId(userId)
                .contentId(contentId)
                .contentType(contentType)
                .category(category)
                .accessedAt(accessedAt)
                .build();
    }
}
