package com.linglevel.api.content.article.service;

import com.linglevel.api.content.article.dto.*;
import com.linglevel.api.content.article.entity.Article;
import com.linglevel.api.content.article.entity.ArticleChunk;
import com.linglevel.api.content.article.entity.ArticleProgress;
import com.linglevel.api.content.article.exception.ArticleErrorCode;
import com.linglevel.api.content.article.exception.ArticleException;
import com.linglevel.api.content.article.repository.ArticleRepository;
import com.linglevel.api.content.article.repository.ArticleProgressRepository;
import com.linglevel.api.content.article.repository.ArticleChunkRepository;
import com.linglevel.api.content.article.repository.dto.ArticleChunkCount;
import com.linglevel.api.common.dto.PageResponse;
import com.linglevel.api.content.common.ContentCategory;
import com.linglevel.api.content.common.DifficultyLevel;
import com.linglevel.api.i18n.LanguageCode;

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

import java.time.Instant;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
@RequiredArgsConstructor
@Slf4j
public class ArticleService {

    private static final int MAX_PAGE_SIZE = 40;

    private final ArticleRepository articleRepository;
    private final ArticleProgressRepository articleProgressRepository;
    private final ArticleChunkRepository articleChunkRepository;
    private final ArticleImportService articleImportService;
    private final ArticleReadingTimeService articleReadingTimeService;
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

        List<ArticleResponse> articleResponses = mapArticlesWithAggregations(articlePage.getContent(), userId);

        return PageResponse.of(articlePage, articleResponses);
    }

    public ArticleResponse getArticle(String articleId, String userId) {
        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new ArticleException(ArticleErrorCode.ARTICLE_NOT_FOUND));

        return mapArticlesWithAggregations(List.of(article), userId).stream()
                .findFirst()
                .orElseThrow(() -> new ArticleException(ArticleErrorCode.ARTICLE_NOT_FOUND));
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
        if (request.getSortBy() != null && !isValidSortBy(request.getSortBy())) {
            throw new ArticleException(ArticleErrorCode.INVALID_SORT_BY);
        }

        int requestedLimit = request.getLimit() == null ? MAX_PAGE_SIZE : request.getLimit();
        request.setLimit(Math.min(requestedLimit, MAX_PAGE_SIZE));

        if (request.getPage() == null || request.getPage() < 1) {
            request.setPage(1);
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

        // 카테고리와 태그 파싱
        parseCategoryAndTags(article, importData.getTags());

        // targetLanguageCode 매핑
        if (importData.getTargetLanguageCode() != null && !importData.getTargetLanguageCode().isEmpty()) {
            List<LanguageCode> targetLanguageCodes = importData.getTargetLanguageCode().stream()
                    .map(code -> LanguageCode.valueOf(code.toUpperCase()))
                    .collect(Collectors.toList());
            article.setTargetLanguageCode(targetLanguageCodes);
        } else {
            // null이거나 빈 리스트면 모든 언어 코드로 설정
            article.setTargetLanguageCode(LanguageCode.getAllCodes());
        }

        article.setOriginUrl(importData.getOriginUrl());
        article.setCreatedAt(Instant.now());

        return article;
    }

    /**
     * AI가 제공한 태그 리스트에서 카테고리 추출
     * - 5개 특별 태그(Sports, Science, Tech, Business, Culture) 중 하나가 있으면 category로 설정
     * - 카테고리는 tags 리스트에도 그대로 유지 (중복 허용)
     * - 유저 선호도 분석 등에 활용
     */
    private void parseCategoryAndTags(Article article, List<String> importedTags) {
        if (importedTags == null || importedTags.isEmpty()) {
            article.setCategory(null);
            article.setTags(List.of());
            return;
        }

        ContentCategory foundCategory = null;

        for (String tag : importedTags) {
            ContentCategory category = ContentCategory.fromString(tag);
            if (category != null && foundCategory == null) {
                // 첫 번째로 발견된 카테고리 태그를 사용
                foundCategory = category;
            }
        }

        article.setCategory(foundCategory);
        // 모든 태그를 그대로 유지
        article.setTags(importedTags);
    }

    private List<ArticleResponse> mapArticlesWithAggregations(List<Article> articles, String userId) {
        if (articles.isEmpty()) {
            return List.of();
        }

        List<String> articleIds = articles.stream()
                .map(Article::getId)
                .toList();

        Map<String, ArticleProgress> progressByArticleId = fetchProgressMap(userId, articleIds);
        Map<String, ArticleChunk> chunkById = fetchChunksByProgress(progressByArticleId);
        Map<String, Map<DifficultyLevel, Long>> chunkCountsByArticle = buildChunkCountMap(articleIds);

        return articles.stream()
                .map(article -> convertToArticleResponse(
                        article,
                        progressByArticleId.get(article.getId()),
                        chunkCountsByArticle,
                        chunkById
                ))
                .toList();
    }

    private Map<String, ArticleProgress> fetchProgressMap(String userId, List<String> articleIds) {
        if (userId == null || articleIds.isEmpty()) {
            return Map.of();
        }

        List<ArticleProgress> progresses = articleProgressRepository.findByUserIdAndArticleIdIn(userId, articleIds);

        return progresses.stream()
                .collect(Collectors.toMap(
                        ArticleProgress::getArticleId,
                        Function.identity(),
                        (existing, replacement) -> existing
                ));
    }

    private Map<String, ArticleChunk> fetchChunksByProgress(Map<String, ArticleProgress> progressByArticleId) {
        if (progressByArticleId.isEmpty()) {
            return Map.of();
        }

        List<String> chunkIds = progressByArticleId.values().stream()
                .map(ArticleProgress::getChunkId)
                .filter(Objects::nonNull)
                .toList();

        if (chunkIds.isEmpty()) {
            return Map.of();
        }

        return StreamSupport.stream(articleChunkRepository.findAllById(chunkIds).spliterator(), false)
                .collect(Collectors.toMap(
                        ArticleChunk::getId,
                        Function.identity(),
                        (existing, replacement) -> existing
                ));
    }

    private Map<String, Map<DifficultyLevel, Long>> buildChunkCountMap(List<String> articleIds) {
        if (articleIds.isEmpty()) {
            return Map.of();
        }

        List<ArticleChunkCount> counts = articleChunkRepository.countChunksByArticleIds(articleIds);
        Map<String, Map<DifficultyLevel, Long>> chunkCountsByArticle = new HashMap<>();

        for (ArticleChunkCount count : counts) {
            Map<DifficultyLevel, Long> perDifficulty = chunkCountsByArticle.computeIfAbsent(
                    count.getArticleId(),
                    id -> new EnumMap<>(DifficultyLevel.class)
            );
            perDifficulty.put(count.getDifficultyLevel(), count.getCount());
        }

        return chunkCountsByArticle;
    }

    private ArticleResponse convertToArticleResponse(
            Article article,
            ArticleProgress progress,
            Map<String, Map<DifficultyLevel, Long>> chunkCountsByArticle,
            Map<String, ArticleChunk> chunkById
    ) {
        int currentReadChunkNumber = 0;
        boolean isCompleted = false;
        DifficultyLevel currentDifficultyLevel = article.getDifficultyLevel();

        if (progress != null) {
            if (progress.getCurrentDifficultyLevel() != null) {
                currentDifficultyLevel = progress.getCurrentDifficultyLevel();
            }

            if (progress.getChunkId() != null) {
                ArticleChunk chunk = chunkById.get(progress.getChunkId());
                if (chunk != null && chunk.getChunkNumber() != null) {
                    currentReadChunkNumber = chunk.getChunkNumber();
                }
            }

            isCompleted = progress.getIsCompleted() != null && progress.getIsCompleted();
        }

        long chunkCount = chunkCountsByArticle
                .getOrDefault(article.getId(), Map.of())
                .getOrDefault(currentDifficultyLevel, 0L);

        double progressPercentage = chunkCount > 0
                ? (double) currentReadChunkNumber / chunkCount * 100.0
                : 0.0;

        ArticleResponse response = new ArticleResponse();
        response.setId(article.getId());
        response.setTitle(article.getTitle());
        response.setAuthor(article.getAuthor());
        response.setCoverImageUrl(article.getCoverImageUrl());
        response.setDifficultyLevel(article.getDifficultyLevel());
        response.setChunkCount((int) chunkCount);
        response.setCurrentReadChunkNumber(currentReadChunkNumber);
        response.setProgressPercentage(progressPercentage);
        response.setCurrentDifficultyLevel(currentDifficultyLevel);
        response.setIsCompleted(isCompleted);
        response.setReadingTime(article.getReadingTime());
        response.setAverageRating(article.getAverageRating());
        response.setReviewCount(article.getReviewCount());
        response.setViewCount(article.getViewCount());
        response.setCategory(article.getCategory());
        response.setTags(article.getTags());

        List<LanguageCode> targetLanguageCodes = article.getTargetLanguageCode();
        response.setTargetLanguageCode(
            (targetLanguageCodes != null && !targetLanguageCodes.isEmpty())
                ? targetLanguageCodes
                : LanguageCode.getAllCodes()
        );

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

    public PageResponse<ArticleOriginResponse> getArticleOrigins(GetArticleOriginsRequest request) {
        log.info("Fetching article origins with filters - tags: {}, targetLanguageCode: {}",
                request.getTags(), request.getTargetLanguageCode());

        Pageable pageable = PageRequest.of(request.getPage() - 1, request.getLimit(),
                Sort.by(Sort.Direction.DESC, "createdAt"));

        Page<Article> articlePage = articleRepository.findArticleOriginsWithFilters(request, pageable);

        List<ArticleOriginResponse> responses = articlePage.getContent().stream()
                .map(this::convertToArticleOriginResponse)
                .collect(Collectors.toList());

        return PageResponse.of(articlePage, responses);
    }

    private ArticleOriginResponse convertToArticleOriginResponse(Article article) {
        ArticleOriginResponse response = new ArticleOriginResponse();
        response.setId(article.getId());
        response.setTitle(article.getTitle());
        response.setOriginUrl(article.getOriginUrl());

        List<LanguageCode> targetLanguageCodes = article.getTargetLanguageCode();
        response.setTargetLanguageCode(
            (targetLanguageCodes != null && !targetLanguageCodes.isEmpty())
                ? targetLanguageCodes
                : LanguageCode.getAllCodes()
        );

        response.setCategory(article.getCategory());
        response.setTags(article.getTags());
        return response;
    }

    @Transactional
    public long migrateTargetLanguageCode() {
        log.info("Starting migration: setting default targetLanguageCode for articles");

        List<Article> articles = articleRepository.findAll();
        long updatedCount = 0;

        for (Article article : articles) {
            if (article.getTargetLanguageCode() == null || article.getTargetLanguageCode().isEmpty()) {
                article.setTargetLanguageCode(LanguageCode.getAllCodes());
                articleRepository.save(article);
                updatedCount++;
            }
        }

        log.info("Migration completed: updated {} articles", updatedCount);
        return updatedCount;
    }
}
