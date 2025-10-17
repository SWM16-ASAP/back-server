package com.linglevel.api.content.custom.repository;

import com.linglevel.api.content.common.ProgressStatus;
import com.linglevel.api.content.custom.dto.GetCustomContentsRequest;
import com.linglevel.api.content.custom.entity.CustomContent;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.util.StringUtils;

import java.util.List;

@RequiredArgsConstructor
public class CustomContentRepositoryImpl implements CustomContentRepositoryCustom {

    private final MongoTemplate mongoTemplate;

    @Override
    public Page<CustomContent> findCustomContentsWithFilters(String userId, GetCustomContentsRequest request, Pageable pageable) {
        Query query = buildQuery(userId, request);

        // 총 개수 조회 (필터링 적용 후)
        long total = mongoTemplate.count(query, CustomContent.class);

        // 페이지네이션 적용
        query.with(pageable);

        // 데이터 조회
        List<CustomContent> contents = mongoTemplate.find(query, CustomContent.class);

        return new PageImpl<>(contents, pageable, total);
    }

    /**
     * 동적 쿼리 빌드
     */
    private Query buildQuery(String userId, GetCustomContentsRequest request) {
        Query query = new Query();

        // 기본 필터 (userId, isDeleted)
        query.addCriteria(Criteria.where("userId").is(userId));
        query.addCriteria(Criteria.where("isDeleted").is(false));

        // 각 필터를 독립적인 메서드로 분리
        applyKeywordFilter(query, request.getKeyword());
        applyTagsFilter(query, request.getTags());
        applyProgressFilter(query, request.getProgress(), userId);

        return query;
    }

    /**
     * 키워드 필터 적용 (제목 또는 작가)
     */
    private void applyKeywordFilter(Query query, String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return;
        }

        Criteria keywordCriteria = new Criteria().orOperator(
            Criteria.where("title").regex(keyword, "i"),
            Criteria.where("author").regex(keyword, "i")
        );
        query.addCriteria(keywordCriteria);
    }

    /**
     * 태그 필터 적용
     */
    private void applyTagsFilter(Query query, String tags) {
        if (!StringUtils.hasText(tags)) {
            return;
        }

        String[] tagArray = tags.split(",");
        query.addCriteria(Criteria.where("tags").all((Object[]) tagArray));
    }

    /**
     * 진도 필터 적용
     */
    private void applyProgressFilter(Query query, ProgressStatus progress, String userId) {
        if (progress == null || userId == null) {
            return;
        }

        List<String> contentIds = getContentIdsByProgress(userId, progress);
        if (!contentIds.isEmpty()) {
            query.addCriteria(Criteria.where("id").in(contentIds));
        } else {
            // 조건에 맞는 콘텐츠가 없으면 빈 결과 반환
            query.addCriteria(Criteria.where("_id").is(null));
        }
    }

    /**
     * 진도 상태별 콘텐츠 ID 목록 조회
     */
    private List<String> getContentIdsByProgress(String userId, ProgressStatus progressStatus) {
        return switch (progressStatus) {
            case NOT_STARTED -> getNotStartedContentIds(userId);
            case IN_PROGRESS -> getInProgressContentIds(userId);
            case COMPLETED -> getCompletedContentIds(userId);
        };
    }

    /**
     * 시작하지 않은 콘텐츠 ID 목록 조회
     */
    private List<String> getNotStartedContentIds(String userId) {
        // 해당 사용자의 모든 콘텐츠 ID 조회
        Query userContentQuery = new Query();
        userContentQuery.addCriteria(Criteria.where("userId").is(userId));
        userContentQuery.addCriteria(Criteria.where("isDeleted").is(false));

        List<String> allContentIds = mongoTemplate.find(userContentQuery, CustomContent.class).stream()
                .map(CustomContent::getId)
                .toList();

        // 진도가 있는 콘텐츠 ID 조회
        List<String> progressContentIds = findProgressContentIds(userId);

        // 진도가 없는 콘텐츠만 반환
        return allContentIds.stream()
                .filter(contentId -> !progressContentIds.contains(contentId))
                .toList();
    }

    /**
     * 진행 중인 콘텐츠 ID 목록 조회
     */
    private List<String> getInProgressContentIds(String userId) {
        Query query = new Query();
        query.addCriteria(Criteria.where("userId").is(userId));
        query.addCriteria(Criteria.where("isCompleted").is(false));
        query.addCriteria(Criteria.where("currentReadChunkNumber").gt(0));

        return findContentIdsFromProgress(query);
    }

    /**
     * 완료한 콘텐츠 ID 목록 조회
     */
    private List<String> getCompletedContentIds(String userId) {
        Query query = new Query();
        query.addCriteria(Criteria.where("userId").is(userId));
        query.addCriteria(Criteria.where("isCompleted").is(true));

        return findContentIdsFromProgress(query);
    }

    /**
     * 특정 사용자의 모든 진도 콘텐츠 ID 조회
     */
    private List<String> findProgressContentIds(String userId) {
        Query query = new Query();
        query.addCriteria(Criteria.where("userId").is(userId));

        return findContentIdsFromProgress(query);
    }

    /**
     * CustomContentProgress 컬렉션에서 customId 추출
     */
    private List<String> findContentIdsFromProgress(Query query) {
        return mongoTemplate.find(query, org.bson.Document.class, "customContentProgress")
                .stream()
                .map(doc -> doc.getString("customId"))
                .toList();
    }

    @Override
    public void incrementViewCount(String customContentId) {
        Query query = new Query(Criteria.where("id").is(customContentId));
        Update update = new Update().inc("viewCount", 1);
        mongoTemplate.updateFirst(query, update, CustomContent.class);
    }
}
