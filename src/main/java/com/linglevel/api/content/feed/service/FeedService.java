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

import java.util.Collections;

@Service
@RequiredArgsConstructor
@Slf4j
public class FeedService {

    private final FeedRepository feedRepository;

    /**
     * Feed 목록 조회 (필터링 + 정렬)
     *
     * TODO: 실제 구현 필요
     * - contentTypes 필터링
     * - category 필터링
     * - sortOrder에 따른 정렬 (RECOMMENDED: 추천 알고리즘, LATEST: publishedAt desc, POPULAR: viewCount desc)
     * - 페이징 처리
     */
    public PageResponse<FeedResponse> getFeeds(GetFeedsRequest request, String userId) {
        log.info("getFeeds called with request: {}, userId: {}", request, userId);

        // TODO: 실제 구현 필요
        // 임시로 빈 응답 반환
        return PageResponse.<FeedResponse>builder()
                .data(Collections.emptyList())
                .totalCount(0)
                .totalPages(0)
                .currentPage(request.getPage())
                .hasNext(false)
                .hasPrevious(false)
                .build();
    }

    /**
     * 단일 Feed 조회
     *
     * TODO: 실제 구현 필요
     * - Feed 조회
     * - FeedResponse로 변환
     * - 조회수 증가 (비동기)
     */
    public FeedResponse getFeed(String feedId, String userId) {
        log.info("getFeed called with feedId: {}, userId: {}", feedId, userId);

        Feed feed = feedRepository.findById(feedId)
                .orElseThrow(() -> new FeedException(FeedErrorCode.FEED_NOT_FOUND));

        // TODO: 조회수 증가 로직 추가 (비동기)
        // TODO: FeedResponse로 변환 로직 추가

        // 임시로 빈 응답 반환
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
