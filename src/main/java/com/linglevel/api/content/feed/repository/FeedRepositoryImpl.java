package com.linglevel.api.content.feed.repository;

import com.linglevel.api.content.feed.dto.GetFeedsRequest;
import com.linglevel.api.content.feed.entity.Feed;
import com.linglevel.api.content.feed.service.FeedRecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.stream.Collectors;

@Repository
@RequiredArgsConstructor
public class FeedRepositoryImpl implements FeedRepositoryCustom {

    private final MongoTemplate mongoTemplate;
    private final FeedRecommendationService feedRecommendationService;

    private static final int RECOMMENDED_FETCH_LIMIT = 100;

    @Override
    public List<Feed> findByDeletedFalseWithProjection() {
        Query query = new Query(Criteria.where("deleted").is(false));

        query.fields()
                .exclude("displayOrder")
                .exclude("deleted")
                .exclude("deletedAt");

        return mongoTemplate.find(query, Feed.class);
    }

    @Override
    public Page<Feed> findFeedsWithFilters(GetFeedsRequest request, String userId, Pageable pageable) {
        if (request.getSortOrder() == GetFeedsRequest.SortOrder.RECOMMENDED) {
            return findRecommendedFeeds(request, userId, pageable);
        } else {
            return findRegularFeeds(request, pageable);
        }
    }

    private Page<Feed> findRegularFeeds(GetFeedsRequest request, Pageable pageable) {
        Query query = buildBaseQuery(request);

        // 필드 프로젝션
        applyFieldProjection(query);

        // 정렬 (DB 레벨)
        applySorting(query, request.getSortOrder());

        // Count 먼저 실행 (페이징 전)
        long total = mongoTemplate.count(query, Feed.class);

        // 페이징 적용 (DB 레벨)
        query.with(pageable);

        // 필요한 페이지만 조회
        List<Feed> feeds = mongoTemplate.find(query, Feed.class);

        return new PageImpl<>(feeds, pageable, total);
    }

    /**
     * RECOMMENDED: 최근 100개만 로드 후 메모리에서 스코어링
     */
    private Page<Feed> findRecommendedFeeds(GetFeedsRequest request, String userId, Pageable pageable) {
        Query query = buildBaseQuery(request);

        // 필드 프로젝션
        applyFieldProjection(query);

        // 최근 100개만 로드 (publishedAt 내림차순)
        query.with(Sort.by(Sort.Direction.DESC, "publishedAt"))
                .limit(RECOMMENDED_FETCH_LIMIT);

        List<Feed> feeds = mongoTemplate.find(query, Feed.class);

        // 메모리에서 추천 스코어 계산 및 정렬
        List<Feed> scoredFeeds = feedRecommendationService.sortByRecommendation(feeds, userId);

        // 수동 페이징
        int offset = pageable.getPageNumber() * pageable.getPageSize();
        int end = Math.min(offset + pageable.getPageSize(), scoredFeeds.size());

        List<Feed> pagedFeeds = scoredFeeds.subList(
                Math.min(offset, scoredFeeds.size()),
                end
        );

        return new PageImpl<>(pagedFeeds, pageable, scoredFeeds.size());
    }

    /**
     * 기본 쿼리 빌드: deleted=false + 선택적 필터
     */
    private Query buildBaseQuery(GetFeedsRequest request) {
        Query query = new Query();

        // 항상 적용: deleted = false
        query.addCriteria(Criteria.where("deleted").is(false));

        // Optional: contentTypes 필터
        if (request.getContentTypes() != null && !request.getContentTypes().isEmpty()) {
            query.addCriteria(Criteria.where("contentType").in(request.getContentTypes()));
        }

        // Optional: category 필터
        if (request.getCategory() != null) {
            query.addCriteria(Criteria.where("category").is(request.getCategory()));
        }

        return query;
    }

    /**
     * 필드 프로젝션 적용
     */
    private void applyFieldProjection(Query query) {
        query.fields()
                .exclude("displayOrder")
                .exclude("deleted")
                .exclude("deletedAt");
    }

    /**
     * 정렬 적용 (LATEST/POPULAR만)
     */
    private void applySorting(Query query, GetFeedsRequest.SortOrder sortOrder) {
        if (sortOrder == GetFeedsRequest.SortOrder.POPULAR) {
            query.with(Sort.by(Sort.Direction.DESC, "viewCount"));
        } else { // LATEST or default
            query.with(Sort.by(Sort.Direction.DESC, "publishedAt"));
        }
    }
}