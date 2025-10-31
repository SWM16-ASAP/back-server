package com.linglevel.api.content.article.service;

import com.linglevel.api.content.article.dto.ArticleProgressResponse;
import com.linglevel.api.content.article.dto.ArticleProgressUpdateRequest;
import com.linglevel.api.content.article.entity.ArticleChunk;
import com.linglevel.api.content.article.entity.ArticleProgress;
import com.linglevel.api.content.article.exception.ArticleErrorCode;
import com.linglevel.api.content.article.exception.ArticleException;
import com.linglevel.api.content.article.repository.ArticleChunkRepository;
import com.linglevel.api.content.article.repository.ArticleProgressRepository;
import com.linglevel.api.content.common.ContentType;
import com.linglevel.api.content.common.service.ProgressCalculationService;
import com.linglevel.api.streak.service.ReadingSessionService;
import com.linglevel.api.streak.service.StreakService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ArticleProgressService {

    private final ArticleService articleService;
    private final ArticleChunkService articleChunkService;
    private final ArticleProgressRepository articleProgressRepository;
    private final ArticleChunkRepository articleChunkRepository;
    private final ProgressCalculationService progressCalculationService;
    private final ReadingSessionService readingSessionService;
    private final StreakService streakService;

    @Transactional
    public ArticleProgressResponse updateProgress(String articleId, ArticleProgressUpdateRequest request, String userId) {
        // 아티클 존재 여부 확인
        if (!articleService.existsById(articleId)) {
            throw new ArticleException(ArticleErrorCode.ARTICLE_NOT_FOUND);
        }

        // chunkId로부터 chunk 정보 조회
        ArticleChunk chunk = articleChunkService.findById(request.getChunkId());

        // chunk가 해당 article에 속하는지 검증
        if (chunk.getArticleId() == null || !chunk.getArticleId().equals(articleId)) {
            throw new ArticleException(ArticleErrorCode.CHUNK_NOT_FOUND_IN_ARTICLE);
        }

        ArticleProgress articleProgress = articleProgressRepository.findByUserIdAndArticleId(userId, articleId)
                .orElse(new ArticleProgress());

        // [MIGRATION] V2 진행률 필드 마이그레이션
        ensureMigrated(articleProgress, chunk);

        // Null 체크
        if (chunk.getChunkNumber() == null) {
            throw new ArticleException(ArticleErrorCode.CHUNK_NOT_FOUND);
        }

        articleProgress.setUserId(userId);
        articleProgress.setArticleId(articleId);
        articleProgress.setChunkId(request.getChunkId());

        // [V2_CORE] V2 필드: 정규화된 진행률 계산
        long totalChunks = articleChunkRepository.countByArticleIdAndDifficultyLevel(
            articleId, chunk.getDifficultyLevel()
        );
        double normalizedProgress = progressCalculationService.calculateNormalizedProgress(
            chunk.getChunkNumber(), totalChunks
        );

        articleProgress.setNormalizedProgress(normalizedProgress);
        articleProgress.setCurrentDifficultyLevel(chunk.getDifficultyLevel());

        // maxNormalizedProgress 업데이트 (누적 최대값)
        if (progressCalculationService.shouldUpdateMaxProgress(
                articleProgress.getMaxNormalizedProgress(), normalizedProgress)) {
            articleProgress.setMaxNormalizedProgress(normalizedProgress);
        }

        // 스트릭 검사 및 완료 처리 로직
        boolean streakUpdated = false;
        if (isLastChunk(chunk)) {
            // 첫 완료 시에만 isCompleted와 completedAt 설정
            if (articleProgress.getCompletedAt() == null) {
                articleProgress.setIsCompleted(true);
                articleProgress.setCompletedAt(java.time.Instant.now());
            }

            streakService.addStudyTime(userId, readingSessionService.getReadingSessionSeconds(userId, ContentType.ARTICLE, articleId));

            // 스트릭 업데이트 (세션이 유효할 때만)
            if (readingSessionService.isReadingSessionValid(userId, ContentType.ARTICLE, articleId)) {
                streakUpdated = streakService.updateStreak(userId, ContentType.ARTICLE, articleId);
            }

            readingSessionService.deleteReadingSession(userId);
        }

        articleProgressRepository.save(articleProgress);

        return convertToArticleProgressResponse(articleProgress, streakUpdated);
    }

    private boolean isLastChunk(ArticleChunk chunk) {
        long totalChunks = articleChunkRepository.countByArticleIdAndDifficultyLevel(
            chunk.getArticleId(), chunk.getDifficultyLevel()
        );
        return chunk.getChunkNumber() >= totalChunks;
    }

    /**
     * V2 마이그레이션 보장
     * updateProgress 시점에 한 번만 실행
     */
    private void ensureMigrated(ArticleProgress progress, ArticleChunk chunk) {
        boolean needsMigration = false;

        // V2 필드 초기화
        if (progress.getNormalizedProgress() == null) {
            long totalChunks = articleChunkRepository.countByArticleIdAndDifficultyLevel(
                chunk.getArticleId(), chunk.getDifficultyLevel()
            );
            double normalizedProgress = progressCalculationService.calculateNormalizedProgress(
                chunk.getChunkNumber(), totalChunks
            );
            progress.setNormalizedProgress(normalizedProgress);
            progress.setMaxNormalizedProgress(normalizedProgress);
            needsMigration = true;
        }

        if (progress.getCurrentDifficultyLevel() == null) {
            progress.setCurrentDifficultyLevel(chunk.getDifficultyLevel());
            needsMigration = true;
        }

        if (needsMigration) {
            log.info("V2 migration completed for ArticleProgress id={}, userId={}",
                progress.getId(), progress.getUserId());
        }
    }


    @Transactional(readOnly = true)
    public ArticleProgressResponse getProgress(String articleId, String userId) {
        // 아티클 존재 여부 확인
        if (!articleService.existsById(articleId)) {
            throw new ArticleException(ArticleErrorCode.ARTICLE_NOT_FOUND);
        }

        ArticleProgress articleProgress = articleProgressRepository.findByUserIdAndArticleId(userId, articleId)
                .orElseGet(() -> initializeProgress(userId, articleId));

        return convertToArticleProgressResponse(articleProgress, false);
    }

    private ArticleProgress initializeProgress(String userId, String articleId) {
        // 첫 번째 청크로 초기화
        ArticleChunk firstChunk = articleChunkService.findFirstByArticleId(articleId);

        ArticleProgress newProgress = new ArticleProgress();
        newProgress.setUserId(userId);
        newProgress.setArticleId(articleId);
        newProgress.setChunkId(firstChunk.getId());

        // [V2_CORE] V2 필드: 초기 진행률 계산
        long totalChunks = articleChunkRepository.countByArticleIdAndDifficultyLevel(
            articleId, firstChunk.getDifficultyLevel()
        );
        double initialProgress = progressCalculationService.calculateNormalizedProgress(
            firstChunk.getChunkNumber(), totalChunks
        );

        newProgress.setNormalizedProgress(initialProgress);
        newProgress.setMaxNormalizedProgress(initialProgress);
        newProgress.setCurrentDifficultyLevel(firstChunk.getDifficultyLevel());

        return articleProgressRepository.save(newProgress);
    }

    @Transactional
    public void deleteProgress(String articleId, String userId) {
        if (!articleService.existsById(articleId)) {
            throw new ArticleException(ArticleErrorCode.ARTICLE_NOT_FOUND);
        }

        ArticleProgress articleProgress = articleProgressRepository.findByUserIdAndArticleId(userId, articleId)
                .orElseThrow(() -> new ArticleException(ArticleErrorCode.PROGRESS_NOT_FOUND));

        articleProgressRepository.delete(articleProgress);
    }

    private ArticleProgressResponse convertToArticleProgressResponse(ArticleProgress progress, boolean streakUpdated) {
        // [DTO_MAPPING] chunk에서 chunkNumber 조회
        ArticleChunk chunk = articleChunkService.findById(progress.getChunkId());

        // [SAFETY] 마이그레이션이 안 되어 있는 경우 경고 로그
        if (progress.getNormalizedProgress() == null || progress.getCurrentDifficultyLevel() == null) {
            log.warn("ArticleProgress {} not migrated yet - this should only happen on read-only access",
                progress.getId());
        }

        return ArticleProgressResponse.builder()
                .id(progress.getId())
                .userId(progress.getUserId())
                .articleId(progress.getArticleId())
                .chunkId(progress.getChunkId())
                .currentReadChunkNumber(chunk.getChunkNumber())
                .isCompleted(progress.getIsCompleted())
                .currentDifficultyLevel(progress.getCurrentDifficultyLevel())
                .normalizedProgress(progress.getNormalizedProgress())
                .maxNormalizedProgress(progress.getMaxNormalizedProgress())
                .streakUpdated(streakUpdated)
                .updatedAt(progress.getUpdatedAt())
                .build();
    }
}