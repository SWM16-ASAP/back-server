package com.linglevel.api.content.feed.service;

import com.linglevel.api.common.dto.PageResponse;
import com.linglevel.api.content.feed.dto.FeedResponse;
import com.linglevel.api.content.feed.dto.GetFeedsRequest;
import com.linglevel.api.content.feed.entity.Feed;
import com.linglevel.api.content.feed.exception.FeedErrorCode;
import com.linglevel.api.content.feed.exception.FeedException;
import com.linglevel.api.content.feed.repository.FeedRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FeedService {

    private final FeedRepository feedRepository;
    private final FeedRecommendationService feedRecommendationService;

    private static final int MAX_PAGE_SIZE = 40;

    public PageResponse<FeedResponse> getFeeds(GetFeedsRequest request, String userId) {
        // 내부적으로 최대 페이지 크기를 50개로 제한
        int effectiveLimit = Math.min(request.getLimit(), MAX_PAGE_SIZE);

        // Pageable 생성 (0-based indexing)
        Pageable pageable = PageRequest.of(request.getPage() - 1, effectiveLimit);

        // DB 레벨 필터링/정렬/페이징 처리
        Page<Feed> feedPage = feedRepository.findFeedsWithFilters(request, userId, pageable);

        // 응답 DTO로 변환
        List<FeedResponse> feedResponses = feedPage.getContent().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return PageResponse.<FeedResponse>builder()
                .data(feedResponses)
                .totalCount((int) feedPage.getTotalElements())
                .totalPages(feedPage.getTotalPages())
                .currentPage(request.getPage())
                .hasNext(feedPage.hasNext())
                .hasPrevious(feedPage.hasPrevious())
                .build();
    }

    public FeedResponse getFeed(String feedId, String userId) {
        Feed feed = feedRepository.findByIdAndDeletedFalse(feedId)
                .orElseThrow(() -> new FeedException(FeedErrorCode.FEED_NOT_FOUND));
        feed.setViewCount((feed.getViewCount() != null ? feed.getViewCount() : 0) + 1);
        feedRepository.save(feed);
        return mapToResponse(feed);
    }

    private FeedResponse mapToResponse(Feed feed) {
        FeedResponse response = new FeedResponse();
        response.setId(feed.getId());
        response.setContentType(feed.getContentType());
        response.setTitle(feed.getTitle());
        response.setUrl(feed.getUrl());
        response.setThumbnailUrl(feed.getThumbnailUrl());
        response.setAuthor(feed.getAuthor());
        response.setDescription(feed.getDescription());
        response.setCategory(feed.getCategory());
        response.setTags(feed.getTags());
        response.setSourceProvider(feed.getSourceProvider());
        response.setPublishedAt(feed.getPublishedAt());
        response.setViewCount(feed.getViewCount());
        response.setAvgReadTimeSeconds(feed.getAvgReadTimeSeconds());
        response.setCreatedAt(feed.getCreatedAt());
        return response;
    }
}
