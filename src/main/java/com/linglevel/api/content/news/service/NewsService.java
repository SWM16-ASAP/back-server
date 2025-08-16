package com.linglevel.api.content.news.service;

import com.linglevel.api.common.dto.PageResponse;
import com.linglevel.api.content.common.DifficultyLevel;
import com.linglevel.api.content.news.dto.*;
import com.linglevel.api.content.news.entity.News;
import com.linglevel.api.content.news.exception.NewsErrorCode;
import com.linglevel.api.content.news.exception.NewsException;
import com.linglevel.api.content.news.repository.NewsRepository;
import com.linglevel.api.s3.service.S3AiService;
import com.linglevel.api.s3.service.S3TransferService;
import com.linglevel.api.s3.service.S3UrlService;
import com.linglevel.api.s3.strategy.NewsPathStrategy;
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
public class NewsService {

    private final NewsRepository newsRepository;
    private final NewsImportService newsImportService;
    private final S3AiService s3AiService;
    private final S3TransferService s3TransferService;
    private final S3UrlService s3UrlService;
    private final NewsPathStrategy newsPathStrategy;

    public PageResponse<NewsResponse> getNews(GetNewsRequest request) {
        validateGetNewsRequest(request);
        
        Pageable pageable = createPageable(request);
        Page<News> newsPage = findNews(request, pageable);
        
        List<NewsResponse> newsResponses = newsPage.getContent().stream()
                .map(this::convertToNewsResponse)
                .toList();
        
        return PageResponse.of(newsPage, newsResponses);
    }

    public NewsResponse getNews(String newsId) {
        News news = newsRepository.findById(newsId)
                .orElseThrow(() -> new NewsException(NewsErrorCode.NEWS_NOT_FOUND));
        
        return convertToNewsResponse(news);
    }

    @Transactional
    public NewsImportResponse importNews(NewsImportRequest request) {
        log.info("Starting news import for file: {}", request.getId());
        
        NewsImportData importData = s3AiService.downloadJsonFile(request.getId(), NewsImportData.class, newsPathStrategy);
        
        News news = createNews(importData, request.getId());
        News savedNews = newsRepository.save(news);
        
        s3TransferService.transferImagesFromAiToStatic(request.getId(), savedNews.getId(), newsPathStrategy);
        
        String coverImageUrl = s3UrlService.getCoverImageUrl(savedNews.getId(), newsPathStrategy);
        savedNews.setCoverImageUrl(coverImageUrl);
        newsRepository.save(savedNews);
        
        newsImportService.createChunksFromLeveledResults(importData, savedNews.getId());
        
        log.info("Successfully imported news with id: {}", savedNews.getId());
        
        NewsImportResponse response = new NewsImportResponse();
        response.setId(savedNews.getId());
        return response;
    }

    private void validateGetNewsRequest(GetNewsRequest request) {
        if (request.getSortBy() != null) {
            if (!isValidSortBy(request.getSortBy())) {
                throw new NewsException(NewsErrorCode.INVALID_SORT_BY);
            }
        }
        
        if (request.getLimit() != null && request.getLimit() > 50) {
            request.setLimit(50);
        }
    }

    private boolean isValidSortBy(String sortBy) {
        return "view_count".equals(sortBy) || 
               "average_rating".equals(sortBy) || 
               "created_at".equals(sortBy);
    }

    private Pageable createPageable(GetNewsRequest request) {
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

    private Page<News> findNews(GetNewsRequest request, Pageable pageable) {
        boolean hasTags = request.getTags() != null && !request.getTags().trim().isEmpty();
        boolean hasKeyword = request.getKeyword() != null && !request.getKeyword().trim().isEmpty();
        
        if (hasTags && hasKeyword) {
            List<String> tagList = Arrays.asList(request.getTags().split(","));
            return newsRepository.findByTagsInAndTitleOrAuthorContaining(tagList, request.getKeyword(), pageable);
        } else if (hasTags) {
            List<String> tagList = Arrays.asList(request.getTags().split(","));
            return newsRepository.findByTagsIn(tagList, pageable);
        } else if (hasKeyword) {
            return newsRepository.findByTitleOrAuthorContaining(request.getKeyword(), pageable);
        } else {
            return newsRepository.findAll(pageable);
        }
    }

    private News createNews(NewsImportData importData, String requestId) {
        News news = new News();
        news.setTitle(importData.getTitle());
        news.setAuthor(importData.getAuthor());
        
        DifficultyLevel difficultyLevel = DifficultyLevel.valueOf(
                importData.getOriginalTextLevel().toUpperCase());
        news.setDifficultyLevel(difficultyLevel);
        
        String coverImageUrl = s3UrlService.getCoverImageUrl(requestId, newsPathStrategy);
        news.setCoverImageUrl(coverImageUrl);
        
        int chunkCount = newsImportService.calculateTotalChunkCount(importData);
        news.setChunkCount(chunkCount);
        
        news.setReadingTime(0);
        news.setAverageRating(0.0);
        news.setReviewCount(0);
        news.setViewCount(0);
        news.setTags(List.of());
        news.setCreatedAt(LocalDateTime.now());
        
        return news;
    }

    private NewsResponse convertToNewsResponse(News news) {
        NewsResponse response = new NewsResponse();
        response.setId(news.getId());
        response.setTitle(news.getTitle());
        response.setAuthor(news.getAuthor());
        response.setCoverImageUrl(news.getCoverImageUrl());
        response.setDifficultyLevel(news.getDifficultyLevel());
        response.setChunkCount(news.getChunkCount());
        response.setReadingTime(news.getReadingTime());
        response.setAverageRating(news.getAverageRating());
        response.setReviewCount(news.getReviewCount());
        response.setViewCount(news.getViewCount());
        response.setTags(news.getTags());
        response.setCreatedAt(news.getCreatedAt());
        return response;
    }
}