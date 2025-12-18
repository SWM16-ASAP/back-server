package com.linglevel.api.content.feed.service;

import com.linglevel.api.common.dto.PageResponse;
import com.linglevel.api.content.common.ContentCategory;
import com.linglevel.api.content.feed.dto.FeedResponse;
import com.linglevel.api.content.feed.dto.GetFeedsRequest;
import com.linglevel.api.content.feed.entity.Feed;
import com.linglevel.api.content.feed.entity.FeedContentType;
import com.linglevel.api.content.feed.repository.FeedRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("FeedService 단위 테스트 - 검색 옵션 검증")
class FeedServiceTest {

    @Mock
    private FeedRepository feedRepository;

    @Mock
    private FeedRecommendationService feedRecommendationService;

    @InjectMocks
    private FeedService feedService;

    private List<Feed> mockFeeds;

    @BeforeEach
    void setUp() {
        mockFeeds = Arrays.asList(
                createFeed("1", "Feed 1", FeedContentType.BLOG, ContentCategory.TECH, 100, Instant.now()),
                createFeed("2", "Feed 2", FeedContentType.YOUTUBE, ContentCategory.SCIENCE, 200, Instant.now().minusSeconds(3600)),
                createFeed("3", "Feed 3", FeedContentType.NEWS, ContentCategory.TECH, 150, Instant.now().minusSeconds(7200))
        );
    }

    @Test
    @DisplayName("[검색옵션] contentType 필터 - BLOG만 조회 (YOUTUBE, NEWS 제외 확인)")
    void testContentTypeFilter_Blog() {
        // Given
        GetFeedsRequest request = new GetFeedsRequest();
        request.setContentTypes(List.of(FeedContentType.BLOG));
        request.setSortOrder(GetFeedsRequest.SortOrder.LATEST);
        request.setPage(1);
        request.setLimit(20);

        Page<Feed> mockPage = new PageImpl<>(List.of(mockFeeds.get(0)));
        when(feedRepository.findFeedsWithFilters(eq(request), isNull(), any(Pageable.class)))
                .thenReturn(mockPage);

        // When
        PageResponse<FeedResponse> response = feedService.getFeeds(request, null);

        // Then
        assertThat(response.getData()).hasSize(1);

        // 필터링된 데이터 확인
        assertThat(response.getData())
                .extracting(FeedResponse::getContentType)
                .containsOnly(FeedContentType.BLOG);

        // 제외되어야 할 타입이 포함되지 않았는지 확인
        assertThat(response.getData())
                .extracting(FeedResponse::getContentType)
                .doesNotContain(FeedContentType.YOUTUBE, FeedContentType.NEWS);

        verify(feedRepository).findFeedsWithFilters(eq(request), isNull(), any(Pageable.class));
    }

    @Test
    @DisplayName("[검색옵션] contentTypes 필터 - 여러 타입 조회 (YOUTUBE 제외 확인)")
    void testContentTypeFilter_Multiple() {
        // Given
        GetFeedsRequest request = new GetFeedsRequest();
        request.setContentTypes(List.of(FeedContentType.BLOG, FeedContentType.NEWS));
        request.setSortOrder(GetFeedsRequest.SortOrder.LATEST);
        request.setPage(1);
        request.setLimit(20);

        List<Feed> filteredFeeds = Arrays.asList(mockFeeds.get(0), mockFeeds.get(2));
        Page<Feed> mockPage = new PageImpl<>(filteredFeeds);
        when(feedRepository.findFeedsWithFilters(eq(request), isNull(), any(Pageable.class)))
                .thenReturn(mockPage);

        // When
        PageResponse<FeedResponse> response = feedService.getFeeds(request, null);

        // Then
        assertThat(response.getData()).hasSize(2);

        // 필터에 포함된 타입만 있는지 확인
        assertThat(response.getData())
                .extracting(FeedResponse::getContentType)
                .containsOnly(FeedContentType.BLOG, FeedContentType.NEWS);

        // 제외되어야 할 YOUTUBE가 포함되지 않았는지 확인
        assertThat(response.getData())
                .extracting(FeedResponse::getContentType)
                .doesNotContain(FeedContentType.YOUTUBE);

        verify(feedRepository).findFeedsWithFilters(eq(request), isNull(), any(Pageable.class));
    }

    @Test
    @DisplayName("[검색옵션] category 필터 - TECH 카테고리만 조회 (SCIENCE 제외 확인)")
    void testCategoryFilter_Tech() {
        // Given
        GetFeedsRequest request = new GetFeedsRequest();
        request.setCategory(ContentCategory.TECH);
        request.setSortOrder(GetFeedsRequest.SortOrder.LATEST);
        request.setPage(1);
        request.setLimit(20);

        List<Feed> techFeeds = Arrays.asList(mockFeeds.get(0), mockFeeds.get(2));
        Page<Feed> mockPage = new PageImpl<>(techFeeds);
        when(feedRepository.findFeedsWithFilters(eq(request), isNull(), any(Pageable.class)))
                .thenReturn(mockPage);

        // When
        PageResponse<FeedResponse> response = feedService.getFeeds(request, null);

        // Then
        assertThat(response.getData()).hasSize(2);

        // TECH 카테고리만 있는지 확인
        assertThat(response.getData())
                .extracting(FeedResponse::getCategory)
                .containsOnly(ContentCategory.TECH);

        // 제외되어야 할 SCIENCE 카테고리가 포함되지 않았는지 확인
        assertThat(response.getData())
                .extracting(FeedResponse::getCategory)
                .doesNotContain(ContentCategory.SCIENCE, ContentCategory.SPORTS,
                               ContentCategory.BUSINESS, ContentCategory.EDU, ContentCategory.CULTURE);

        verify(feedRepository).findFeedsWithFilters(eq(request), isNull(), any(Pageable.class));
    }

    @Test
    @DisplayName("[검색옵션] contentType + category 복합 필터 (다른 타입/카테고리 제외 확인)")
    void testCombinedFilters() {
        // Given
        GetFeedsRequest request = new GetFeedsRequest();
        request.setContentTypes(List.of(FeedContentType.BLOG));
        request.setCategory(ContentCategory.TECH);
        request.setSortOrder(GetFeedsRequest.SortOrder.LATEST);
        request.setPage(1);
        request.setLimit(20);

        Page<Feed> mockPage = new PageImpl<>(List.of(mockFeeds.get(0)));
        when(feedRepository.findFeedsWithFilters(eq(request), isNull(), any(Pageable.class)))
                .thenReturn(mockPage);

        // When
        PageResponse<FeedResponse> response = feedService.getFeeds(request, null);

        // Then
        assertThat(response.getData()).hasSize(1);

        // 필터 조건에 맞는 데이터만 있는지 확인
        FeedResponse feed = response.getData().get(0);
        assertThat(feed.getContentType()).isEqualTo(FeedContentType.BLOG);
        assertThat(feed.getCategory()).isEqualTo(ContentCategory.TECH);

        // 제외되어야 할 타입이 포함되지 않았는지 확인
        assertThat(response.getData())
                .extracting(FeedResponse::getContentType)
                .doesNotContain(FeedContentType.YOUTUBE, FeedContentType.NEWS);

        // 제외되어야 할 카테고리가 포함되지 않았는지 확인
        assertThat(response.getData())
                .extracting(FeedResponse::getCategory)
                .doesNotContain(ContentCategory.SCIENCE, ContentCategory.SPORTS);

        verify(feedRepository).findFeedsWithFilters(eq(request), isNull(), any(Pageable.class));
    }

    @Test
    @DisplayName("[검색옵션] LATEST 정렬")
    void testSortOrder_Latest() {
        // Given
        GetFeedsRequest request = new GetFeedsRequest();
        request.setSortOrder(GetFeedsRequest.SortOrder.LATEST);
        request.setPage(1);
        request.setLimit(20);

        Page<Feed> mockPage = new PageImpl<>(mockFeeds);
        when(feedRepository.findFeedsWithFilters(eq(request), isNull(), any(Pageable.class)))
                .thenReturn(mockPage);

        // When
        PageResponse<FeedResponse> response = feedService.getFeeds(request, null);

        // Then
        assertThat(response.getData()).hasSize(3);
        verify(feedRepository).findFeedsWithFilters(
                argThat(req -> req.getSortOrder() == GetFeedsRequest.SortOrder.LATEST),
                isNull(),
                any(Pageable.class)
        );
    }

    @Test
    @DisplayName("[검색옵션] POPULAR 정렬")
    void testSortOrder_Popular() {
        // Given
        GetFeedsRequest request = new GetFeedsRequest();
        request.setSortOrder(GetFeedsRequest.SortOrder.POPULAR);
        request.setPage(1);
        request.setLimit(20);

        List<Feed> sortedFeeds = Arrays.asList(mockFeeds.get(1), mockFeeds.get(2), mockFeeds.get(0));
        Page<Feed> mockPage = new PageImpl<>(sortedFeeds);
        when(feedRepository.findFeedsWithFilters(eq(request), isNull(), any(Pageable.class)))
                .thenReturn(mockPage);

        // When
        PageResponse<FeedResponse> response = feedService.getFeeds(request, null);

        // Then
        assertThat(response.getData()).hasSize(3);
        assertThat(response.getData())
                .extracting(FeedResponse::getViewCount)
                .containsExactly(200, 150, 100);
        verify(feedRepository).findFeedsWithFilters(
                argThat(req -> req.getSortOrder() == GetFeedsRequest.SortOrder.POPULAR),
                isNull(),
                any(Pageable.class)
        );
    }

    @Test
    @DisplayName("[검색옵션] RECOMMENDED 정렬 - 사용자 ID 전달 확인")
    void testSortOrder_Recommended() {
        // Given
        String userId = "user123";
        GetFeedsRequest request = new GetFeedsRequest();
        request.setSortOrder(GetFeedsRequest.SortOrder.RECOMMENDED);
        request.setPage(1);
        request.setLimit(20);

        Page<Feed> mockPage = new PageImpl<>(mockFeeds);
        when(feedRepository.findFeedsWithFilters(eq(request), eq(userId), any(Pageable.class)))
                .thenReturn(mockPage);

        // When
        PageResponse<FeedResponse> response = feedService.getFeeds(request, userId);

        // Then
        assertThat(response.getData()).hasSize(3);
        verify(feedRepository).findFeedsWithFilters(
                argThat(req -> req.getSortOrder() == GetFeedsRequest.SortOrder.RECOMMENDED),
                eq(userId),
                any(Pageable.class)
        );
    }

    @Test
    @DisplayName("[검색옵션] 페이징 - Pageable 변환 확인")
    void testPagination_PageableConversion() {
        // Given
        GetFeedsRequest request = new GetFeedsRequest();
        request.setSortOrder(GetFeedsRequest.SortOrder.LATEST);
        request.setPage(2);  // 2페이지
        request.setLimit(10);

        Page<Feed> mockPage = new PageImpl<>(List.of(), PageRequest.of(1, 10), 15);
        when(feedRepository.findFeedsWithFilters(eq(request), isNull(), any(Pageable.class)))
                .thenReturn(mockPage);

        // When
        feedService.getFeeds(request, null);

        // Then - page=2가 0-based index 1로 변환되었는지 검증
        verify(feedRepository).findFeedsWithFilters(
                eq(request),
                isNull(),
                argThat(pageable ->
                    pageable.getPageNumber() == 1 &&  // 2페이지 → index 1
                    pageable.getPageSize() == 10
                )
        );
    }

    @Test
    @DisplayName("[검색옵션] 필터 없이 전체 조회")
    void testNoFilters_AllFeeds() {
        // Given
        GetFeedsRequest request = new GetFeedsRequest();
        request.setSortOrder(GetFeedsRequest.SortOrder.LATEST);
        request.setPage(1);
        request.setLimit(20);

        Page<Feed> mockPage = new PageImpl<>(mockFeeds);
        when(feedRepository.findFeedsWithFilters(eq(request), isNull(), any(Pageable.class)))
                .thenReturn(mockPage);

        // When
        PageResponse<FeedResponse> response = feedService.getFeeds(request, null);

        // Then
        assertThat(response.getData()).hasSize(3);
        verify(feedRepository).findFeedsWithFilters(eq(request), isNull(), any(Pageable.class));
    }

    @Test
    @DisplayName("[검색옵션] 빈 결과 처리")
    void testEmptyResult() {
        // Given
        GetFeedsRequest request = new GetFeedsRequest();
        request.setContentTypes(List.of(FeedContentType.BLOG));
        request.setSortOrder(GetFeedsRequest.SortOrder.LATEST);
        request.setPage(1);
        request.setLimit(20);

        Page<Feed> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 20), 0);
        when(feedRepository.findFeedsWithFilters(eq(request), isNull(), any(Pageable.class)))
                .thenReturn(emptyPage);

        // When
        PageResponse<FeedResponse> response = feedService.getFeeds(request, null);

        // Then
        assertThat(response.getData()).isEmpty();
        assertThat(response.getTotalCount()).isZero();
    }

    private Feed createFeed(String id, String title, FeedContentType contentType,
                           ContentCategory category, Integer viewCount, Instant publishedAt) {
        return Feed.builder()
                .id(id)
                .title(title)
                .contentType(contentType)
                .category(category)
                .viewCount(viewCount)
                .publishedAt(publishedAt)
                .url("https://example.com/" + id)
                .author("Author " + id)
                .description("Description " + id)
                .deleted(false)
                .build();
    }
}
