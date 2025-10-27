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
        readingSessionService.startReadingSession(userId, ContentType.ARTICLE, articleId);

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

        // 완료 조건: maxNormalizedProgress >= 100%
        boolean isCompleted = progressCalculationService.isCompleted(articleProgress.getMaxNormalizedProgress());
        articleProgress.setIsCompleted(progressCalculationService.updateCompletedFlag(
            articleProgress.getIsCompleted(), isCompleted
        ));

        // 스트릭 검사 로직
        if (isLastChunk(chunk) && readingSessionService.isReadingSessionValid(userId, ContentType.ARTICLE, articleId)) {
            streakService.updateStreak(userId, ContentType.ARTICLE, articleId);
            readingSessionService.deleteReadingSession(userId);
        }

        articleProgressRepository.save(articleProgress);

        return convertToArticleProgressResponse(articleProgress);
    }

    private boolean isLastChunk(ArticleChunk chunk) {
        long totalChunks = articleChunkRepository.countByArticleIdAndDifficultyLevel(
            chunk.getArticleId(), chunk.getDifficultyLevel()
        );
        return chunk.getChunkNumber() >= totalChunks;
    }


    @Transactional(readOnly = true)
    public ArticleProgressResponse getProgress(String articleId, String userId) {
        // 아티클 존재 여부 확인
        if (!articleService.existsById(articleId)) {
            throw new ArticleException(ArticleErrorCode.ARTICLE_NOT_FOUND);
        }

        ArticleProgress articleProgress = articleProgressRepository.findByUserIdAndArticleId(userId, articleId)
                .orElseGet(() -> initializeProgress(userId, articleId));

        return convertToArticleProgressResponse(articleProgress);
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

    private ArticleProgressResponse convertToArticleProgressResponse(ArticleProgress progress) {
        // [DTO_MAPPING] chunk에서 chunkNumber 조회
        ArticleChunk chunk = articleChunkService.findById(progress.getChunkId());

        // [FALLBACK] V2 필드가 없으면 동적 계산 (기존 데이터 대응)
        if (progress.getNormalizedProgress() == null || progress.getCurrentDifficultyLevel() == null) {
            log.info("V2 fields missing for ArticleProgress {}, calculating lazily", progress.getId());

            // 난이도별 전체 청크 수 조회
            long totalChunks = articleChunkRepository.countByArticleIdAndDifficultyLevel(
                chunk.getArticleId(), chunk.getDifficultyLevel()
            );
            double normalizedProgress = progressCalculationService.calculateNormalizedProgress(
                chunk.getChunkNumber(), totalChunks
            );

            // Lazy migration: V2 필드 저장
            progress.setNormalizedProgress(normalizedProgress);
            progress.setMaxNormalizedProgress(normalizedProgress);
            progress.setCurrentDifficultyLevel(chunk.getDifficultyLevel());

            // 완료 조건 재계산
            boolean isCompleted = progressCalculationService.isCompleted(normalizedProgress);
            progress.setIsCompleted(progressCalculationService.updateCompletedFlag(
                progress.getIsCompleted(), isCompleted
            ));

            articleProgressRepository.save(progress);
            log.info("Lazy migration completed for ArticleProgress {}", progress.getId());
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
                .updatedAt(progress.getUpdatedAt())
                .build();
    }
}