package com.linglevel.api.content.article.service;

import com.linglevel.api.common.dto.PageResponse;
import com.linglevel.api.content.common.DifficultyLevel;
import com.linglevel.api.content.article.dto.*;
import com.linglevel.api.content.article.entity.Article;
import com.linglevel.api.content.article.exception.ArticleErrorCode;
import com.linglevel.api.content.article.exception.ArticleException;
import com.linglevel.api.content.article.repository.ArticleRepository;
import com.linglevel.api.s3.service.S3AiService;
import com.linglevel.api.s3.service.S3TransferService;
import com.linglevel.api.s3.service.S3UrlService;
import com.linglevel.api.s3.strategy.ArticlePathStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ArticleService {

    private final ArticleRepository articleRepository;
    private final ArticleImportService articleImportService;
    private final ArticleReadingTimeService articleReadingTimeService;
    private final S3AiService s3AiService;
    private final S3TransferService s3TransferService;
    private final S3UrlService s3UrlService;
    private final ArticlePathStrategy articlePathStrategy;

    public PageResponse<ArticleResponse> getArticles(GetArticlesRequest request) {
        validateGetArticlesRequest(request);
        
        Pageable pageable = createPageable(request);
        Page<Article> articlePage = findArticles(request, pageable);
        
        List<ArticleResponse> articleResponses = articlePage.getContent().stream()
                .map(this::convertToArticleResponse)
                .toList();
        
        return PageResponse.of(articlePage, articleResponses);
    }

    public ArticleResponse getArticle(String articleId) {
        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> new ArticleException(ArticleErrorCode.ARTICLE_NOT_FOUND));
        
        return convertToArticleResponse(article);
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

    private Page<Article> findArticles(GetArticlesRequest request, Pageable pageable) {
        boolean hasTags = request.getTags() != null && !request.getTags().trim().isEmpty();
        boolean hasKeyword = request.getKeyword() != null && !request.getKeyword().trim().isEmpty();
        
        if (hasTags && hasKeyword) {
            List<String> tagList = Arrays.asList(request.getTags().split(","));
            return articleRepository.findByTagsInAndTitleOrAuthorContaining(tagList, request.getKeyword(), pageable);
        } else if (hasTags) {
            List<String> tagList = Arrays.asList(request.getTags().split(","));
            return articleRepository.findByTagsIn(tagList, pageable);
        } else if (hasKeyword) {
            return articleRepository.findByTitleOrAuthorContaining(request.getKeyword(), pageable);
        } else {
            return articleRepository.findAll(pageable);
        }
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
        
        int chunkCount = articleImportService.calculateTotalChunkCount(importData);
        article.setChunkCount(chunkCount);
        
        article.setReadingTime(0);
        article.setAverageRating(0.0);
        article.setReviewCount(0);
        article.setViewCount(0);
        article.setTags(importData.getTags() != null ? importData.getTags() : List.of());
        article.setCreatedAt(LocalDateTime.now());
        
        return article;
    }

    private ArticleResponse convertToArticleResponse(Article article) {
        ArticleResponse response = new ArticleResponse();
        response.setId(article.getId());
        response.setTitle(article.getTitle());
        response.setAuthor(article.getAuthor());
        response.setCoverImageUrl(article.getCoverImageUrl());
        response.setDifficultyLevel(article.getDifficultyLevel());
        response.setChunkCount(article.getChunkCount());
        response.setReadingTime(article.getReadingTime());
        response.setAverageRating(article.getAverageRating());
        response.setReviewCount(article.getReviewCount());
        response.setViewCount(article.getViewCount());
        response.setTags(article.getTags());
        response.setCreatedAt(article.getCreatedAt());
        return response;
    }
}