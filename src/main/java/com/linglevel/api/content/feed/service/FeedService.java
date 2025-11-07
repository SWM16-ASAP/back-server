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
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class FeedService {

    private final FeedRepository feedRepository;
    private final FeedRecommendationService feedRecommendationService;

    public PageResponse<FeedResponse> getFeeds(GetFeedsRequest request, String userId) {
        List<Feed> allFeeds = feedRepository.findAll();

        if (request.getContentTypes() != null && !request.getContentTypes().isEmpty()) {
            allFeeds = allFeeds.stream()
                    .filter(feed -> request.getContentTypes().contains(feed.getContentType()))
                    .collect(java.util.stream.Collectors.toList());
        }

        if (request.getCategory() != null) {
            allFeeds = allFeeds.stream()
                    .filter(feed -> request.getCategory().equals(feed.getCategory()))
                    .collect(java.util.stream.Collectors.toList());
        }

        List<Feed> sortedFeeds = sortFeeds(allFeeds, request.getSortOrder(), userId);

        int totalCount = sortedFeeds.size();
        int totalPages = (int) Math.ceil((double) totalCount / request.getLimit());
        int offset = (request.getPage() - 1) * request.getLimit();

        List<Feed> pagedFeeds = sortedFeeds.stream()
                .skip(offset)
                .limit(request.getLimit())
                .collect(java.util.stream.Collectors.toList());

        List<FeedResponse> feedResponses = pagedFeeds.stream()
                .map(this::mapToResponse)
                .collect(java.util.stream.Collectors.toList());

        return PageResponse.<FeedResponse>builder()
                .data(feedResponses)
                .totalCount(totalCount)
                .totalPages(totalPages)
                .currentPage(request.getPage())
                .hasNext(request.getPage() < totalPages)
                .hasPrevious(request.getPage() > 1)
                .build();
    }

    /**
     * Feed 정렬
     */
    private List<Feed> sortFeeds(List<Feed> feeds, GetFeedsRequest.SortOrder sortOrder, String userId) {
        switch (sortOrder) {
            case POPULAR:
                return feeds.stream()
                        .sorted((f1, f2) -> {
                            int v1 = f1.getViewCount() != null ? f1.getViewCount() : 0;
                            int v2 = f2.getViewCount() != null ? f2.getViewCount() : 0;
                            return Integer.compare(v2, v1);
                        })
                        .collect(java.util.stream.Collectors.toList());

            case RECOMMENDED:
                return feedRecommendationService.sortByRecommendation(feeds, userId);

            case LATEST:
            default:
                // 최신순: createdAt 내림차순
                return feeds.stream()
                        .sorted((f1, f2) -> {
                            if (f1.getCreatedAt() == null) return 1;
                            if (f2.getCreatedAt() == null) return -1;
                            return f2.getCreatedAt().compareTo(f1.getCreatedAt());
                        })
                        .collect(java.util.stream.Collectors.toList());
        }
    }

    public FeedResponse getFeed(String feedId, String userId) {

        Feed feed = feedRepository.findById(feedId)
                .orElseThrow(() -> new FeedException(FeedErrorCode.FEED_NOT_FOUND));

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
