package com.linglevel.api.content.news.service;

import com.linglevel.api.content.news.dto.*;
import com.linglevel.api.content.news.entity.News;
import com.linglevel.api.content.news.exception.NewsErrorCode;
import com.linglevel.api.content.news.exception.NewsException;
import com.linglevel.api.content.news.repository.NewsRepository;
import com.linglevel.api.common.dto.PageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NewsService {

    private final NewsRepository newsRepository;

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