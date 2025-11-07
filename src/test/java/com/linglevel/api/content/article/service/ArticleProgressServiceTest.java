package com.linglevel.api.content.article.service;

import com.linglevel.api.content.article.dto.ArticleProgressUpdateRequest;
import com.linglevel.api.content.article.entity.Article;
import com.linglevel.api.content.article.entity.ArticleChunk;
import com.linglevel.api.content.article.entity.ArticleProgress;
import com.linglevel.api.content.article.repository.ArticleChunkRepository;
import com.linglevel.api.content.article.repository.ArticleProgressRepository;
import com.linglevel.api.content.common.DifficultyLevel;
import com.linglevel.api.content.common.service.ProgressCalculationService;
import com.linglevel.api.content.common.service.ReadingCompletionService;
import com.linglevel.api.streak.service.StreakService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ArticleProgressServiceTest {

    @Mock
    private ArticleService articleService;

    @Mock
    private ArticleProgressRepository articleProgressRepository;

    @Mock
    private ArticleChunkRepository articleChunkRepository;

    @Mock
    private ArticleChunkService articleChunkService;

    @Mock
    private ProgressCalculationService progressCalculationService;

    @Mock
    private ReadingCompletionService readingCompletionService;

    @Mock
    private StreakService streakService;

    @InjectMocks
    private ArticleProgressService articleProgressService;

    @Captor
    private ArgumentCaptor<ArticleProgress> articleProgressCaptor;

    @Test
    @DisplayName("오래된 Article 진행률 업데이트 시 V2 필드가 정상적으로 마이그레이션된다")
    void updateProgress_shouldLazyMigrate_forOldData() {
        // Given: 마이그레이션되지 않은(V2 필드가 null인) ArticleProgress 설정
        String userId = "test-user";
        String articleId = "test-article";
        String chunkId = "test-chunk";

        // V2 필드가 null인 레거시 데이터
        ArticleProgress legacyProgress = new ArticleProgress();
        legacyProgress.setId("legacy-progress-id");
        legacyProgress.setUserId(userId);
        legacyProgress.setArticleId(articleId);
        // legacyProgress.normalizedProgress is null
        // legacyProgress.currentDifficultyLevel is null

        ArticleChunk currentChunk = new ArticleChunk();
        currentChunk.setId(chunkId);
        currentChunk.setArticleId(articleId);
        currentChunk.setChunkNumber(10);
        currentChunk.setDifficultyLevel(DifficultyLevel.B1);

        Article article = new Article();
        article.setId(articleId);

        ArticleProgressUpdateRequest request = new ArticleProgressUpdateRequest();
        request.setChunkId(chunkId);

        // Mocking
        when(articleService.existsById(articleId)).thenReturn(true);
        when(articleService.findById(articleId)).thenReturn(article);
        when(articleProgressRepository.findByUserIdAndArticleId(userId, articleId)).thenReturn(Optional.of(legacyProgress));
        when(articleChunkService.findById(chunkId)).thenReturn(currentChunk);
        when(articleChunkRepository.countByArticleIdAndDifficultyLevel(articleId, DifficultyLevel.B1)).thenReturn(100L);
        when(progressCalculationService.calculateNormalizedProgress(10, 100L)).thenReturn(10.0);

        // When: 진행률 업데이트 호출
        articleProgressService.updateProgress(articleId, request, userId);

        // Then: V2 필드가 채워진 상태로 저장되는지 검증
        verify(articleProgressRepository).save(articleProgressCaptor.capture());
        ArticleProgress savedProgress = articleProgressCaptor.getValue();

        assertThat(savedProgress.getId()).isEqualTo("legacy-progress-id");
        assertThat(savedProgress.getNormalizedProgress()).isNotNull();
        assertThat(savedProgress.getNormalizedProgress()).isEqualTo(10.0);
        assertThat(savedProgress.getMaxNormalizedProgress()).isEqualTo(10.0);
        assertThat(savedProgress.getCurrentDifficultyLevel()).isNotNull();
        assertThat(savedProgress.getCurrentDifficultyLevel()).isEqualTo(DifficultyLevel.B1);
    }
}
