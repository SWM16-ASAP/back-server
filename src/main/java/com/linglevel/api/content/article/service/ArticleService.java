package com.linglevel.api.content.article.service;

import com.linglevel.api.common.dto.PageResponse;
import com.linglevel.api.content.common.DifficultyLevel;
import com.linglevel.api.content.common.ProgressStatus;
import com.linglevel.api.content.article.dto.*;
import com.linglevel.api.content.article.entity.Article;
import com.linglevel.api.content.article.entity.ArticleChunk;
import com.linglevel.api.content.article.exception.ArticleErrorCode;
import com.linglevel.api.content.article.exception.ArticleException;
import com.linglevel.api.content.article.repository.ArticleRepository;
import com.linglevel.api.content.article.repository.ArticleProgressRepository;
import com.linglevel.api.content.article.repository.ArticleChunkRepository;
import com.linglevel.api.content.article.entity.ArticleProgress;
import java.util.stream.Collectors;
import com.linglevel.api.s3.service.S3AiService;
import com.linglevel.api.s3.service.S3TransferService;
import com.linglevel.api.s3.service.S3UrlService;
import com.linglevel.api.s3.service.ImageResizeService;
import com.linglevel.api.s3.strategy.ArticlePathStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ArticleService {

    private final ArticleRepository articleRepository;
    private final ArticleProgressRepository articleProgressRepository;
    private final ArticleChunkRepository articleChunkRepository;
    private final ArticleImportService articleImportService;
    private final ArticleReadingTimeService articleReadingTimeService;
    private final ArticleChunkService articleChunkService;
    private final S3AiService s3AiService;
    private final S3TransferService s3TransferService;
    private final S3UrlService s3UrlService;
    private final ImageResizeService imageResizeService;
    private final ArticlePathStrategy articlePathStrategy;

    public PageResponse<ArticleResponse> getArticles(GetArticlesRequest request, String userId) {
        validateGetArticlesRequest(request);

        Pageable pageable = createPageable(request);

        // Custom Repository 사용 - 필터링 + 페이지네이션 통합 처리
        Page<Article> articlePage = articleRepository.findArticlesWithFilters(request, userId, pageable);

        List<ArticleResponse> articleResponses = articlePage.getContent().stream()
                .map(article -> convertToArticleResponse(article, userId))
                .collect(Collectors.toList());

        return PageResponse.of(articlePage, articleResponses);
    }

    public ArticleResponse getArticle(String articleId, String userId) {
        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new ArticleException(ArticleErrorCode.ARTICLE_NOT_FOUND));

        return convertToArticleResponse(article, userId);
    }

    @Transactional
    public ArticleImportResponse importArticle(ArticleImportRequest request) {
        log.info("Starting article import for file: {}", request.getId());
        
        ArticleImportData importData = s3AiService.downloadJsonFile(request.getId(), ArticleImportData.class, articlePathStrategy);
        
        Article article = createArticle(importData, request.getId());
        Article savedArticle = articleRepository.save(article);
        
        s3TransferService.transferImagesFromAiToStatic(request.getId(), savedArticle.getId(), articlePathStrategy);
        
        String coverImageUrl = s3UrlService.getCoverImageUrl(savedArticle.getId(), articlePathStrategy);
        savedArticle.setCoverImageUrl(coverImageUrl);

        if (StringUtils.hasText(coverImageUrl)) {
            try {
                log.info("Auto-processing cover image for imported article: {}", savedArticle.getId());

                String originalCoverS3Key = articlePathStrategy.generateCoverImagePath(savedArticle.getId());
                String smallImageUrl = imageResizeService.createSmallImage(originalCoverS3Key);

                savedArticle.setCoverImageUrl(smallImageUrl);
                log.info("Successfully auto-processed cover image: {} → {}", savedArticle.getId(), smallImageUrl);

            } catch (Exception e) {
                log.warn("Failed to auto-process cover image for article: {}, keeping original URL", savedArticle.getId(), e);
            }
        }

        articleRepository.save(savedArticle);
        
        articleImportService.createChunksFromLeveledResults(importData, savedArticle.getId());
        
        articleReadingTimeService.updateReadingTime(savedArticle.getId(), importData);
        
        log.info("Successfully imported article with id: {}", savedArticle.getId());
        
        ArticleImportResponse response = new ArticleImportResponse();
        response.setId(savedArticle.getId());
        return response;
    }

    private void validateGetArticlesRequest(GetArticlesRequest request) {
        if (request.getSortBy() != null) {
            if (!isValidSortBy(request.getSortBy())) {
                throw new ArticleException(ArticleErrorCode.INVALID_SORT_BY);
            }
        }
        
        if (request.getLimit() != null && request.getLimit() > 100) {
            request.setLimit(100);
        }
    }

    private boolean isValidSortBy(String sortBy) {
        return "view_count".equals(sortBy) || 
               "average_rating".equals(sortBy) || 
               "created_at".equals(sortBy);
    }

    private Pageable createPageable(GetArticlesRequest request) {
        Sort sort = createSort(request.getSortBy());
        return PageRequest.of(request.getPage() - 1, request.getLimit(), sort);
    }

    private Sort createSort(String sortBy) {
        return switch (sortBy) {
            case "view_count" -> Sort.by(Sort.Direction.DESC, "viewCount");
            case "average_rating" -> Sort.by(Sort.Direction.DESC, "averageRating");
            default -> Sort.by(Sort.Direction.DESC, "createdAt");
        };
    }

    private Article createArticle(ArticleImportData importData, String requestId) {
        Article article = new Article();
        article.setTitle(importData.getTitle());
        article.setAuthor(importData.getAuthor());
        
        DifficultyLevel difficultyLevel = DifficultyLevel.valueOf(
                importData.getOriginalTextLevel().toUpperCase());
        article.setDifficultyLevel(difficultyLevel);
        
        String coverImageUrl = s3UrlService.getCoverImageUrl(requestId, articlePathStrategy);
        article.setCoverImageUrl(coverImageUrl);
        
        article.setReadingTime(0);
        article.setAverageRating(0.0);
        article.setReviewCount(0);
        article.setViewCount(0);
        article.setTags(importData.getTags() != null ? importData.getTags() : List.of());
        article.setCreatedAt(LocalDateTime.now());
        
        return article;
    }


    private ArticleResponse convertToArticleResponse(Article article, String userId) {
        // 진도 정보 조회
        int currentReadChunkNumber = 0;
        double progressPercentage = 0.0;
        boolean isCompleted = false;
        DifficultyLevel currentDifficultyLevel = article.getDifficultyLevel(); // Fallback: Article의 난이도

        if (userId != null) {
            ArticleProgress progress = articleProgressRepository
                .findByUserIdAndArticleId(userId, article.getId())
                .orElse(null);

            if (progress != null) {
                // [DTO_MAPPING] chunk에서 chunkNumber 조회 (안전하게 처리)
                try {
                    ArticleChunk chunk = articleChunkService.findById(progress.getChunkId());
                    currentReadChunkNumber = chunk.getChunkNumber() != null ? chunk.getChunkNumber() : 0;
                } catch (Exception e) {
                    log.warn("Failed to find chunk for progress: {}", progress.getChunkId(), e);
                    currentReadChunkNumber = 0;
                }

                // Progress가 있으면 currentDifficultyLevel 사용
                if (progress.getCurrentDifficultyLevel() != null) {
                    currentDifficultyLevel = progress.getCurrentDifficultyLevel();
                }

                // V2: 현재 난이도 기준으로 동적으로 청크 수 계산
                long totalChunksForLevel = articleChunkRepository.countByArticleIdAndDifficultyLevel(article.getId(), currentDifficultyLevel);

                if (totalChunksForLevel > 0) {
                    progressPercentage = (double) currentReadChunkNumber / totalChunksForLevel * 100.0;
                }

                // DB에 저장된 완료 여부 사용
                isCompleted = progress.getIsCompleted() != null ? progress.getIsCompleted() : false;
            }
        }

        ArticleResponse response = new ArticleResponse();
        response.setId(article.getId());
        response.setTitle(article.getTitle());
        response.setAuthor(article.getAuthor());
        response.setCoverImageUrl(article.getCoverImageUrl());
        response.setDifficultyLevel(article.getDifficultyLevel());
        response.setChunkCount((int) articleChunkRepository.countByArticleIdAndDifficultyLevel(article.getId(), currentDifficultyLevel));
        response.setCurrentReadChunkNumber(currentReadChunkNumber);
        response.setProgressPercentage(progressPercentage);
        response.setCurrentDifficultyLevel(currentDifficultyLevel);
        response.setIsCompleted(isCompleted);
        response.setReadingTime(article.getReadingTime());
        response.setAverageRating(article.getAverageRating());
        response.setReviewCount(article.getReviewCount());
        response.setViewCount(article.getViewCount());
        response.setTags(article.getTags());
        response.setCreatedAt(article.getCreatedAt());
        return response;
    }

    public boolean existsById(String articleId) {
        return articleRepository.existsById(articleId);
    }

    public Article findById(String articleId) {
        return articleRepository.findById(articleId)
                .orElseThrow(() -> new ArticleException(ArticleErrorCode.ARTICLE_NOT_FOUND));
    }
}