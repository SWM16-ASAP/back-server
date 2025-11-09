package com.linglevel.api.content.custom.repository;

import com.linglevel.api.content.common.ProgressStatus;
import com.linglevel.api.content.custom.dto.GetCustomContentsRequest;
import com.linglevel.api.content.custom.entity.CustomContent;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.*;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
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

        // UserCustomContent를 통한 유저별 콘텐츠 조회
        List<String> userContentIds = getUserCustomContentIds(userId);

        if (userContentIds.isEmpty()) {
            query.addCriteria(Criteria.where("_id").is(null));
            return query;
        }

        // 기본 필터 (유저가 해금한 콘텐츠 ID 목록, isDeleted)
        query.addCriteria(Criteria.where("id").in(userContentIds));
        query.addCriteria(Criteria.where("isDeleted").is(false));

        // 각 필터를 독립적인 메서드로 분리
        applyKeywordFilter(query, request.getKeyword());
        applyTagsFilter(query, request.getTags());
        applyProgressFilter(query, request.getProgress(), userId);

        return query;
    }

    /**
     * 유저가 해금한 콘텐츠 ID 목록 조회
     */
    private List<String> getUserCustomContentIds(String userId) {
        Query query = new Query();
        query.addCriteria(Criteria.where("userId").is(userId));
        query.fields().include("customContentId");

        return mongoTemplate.find(query, org.bson.Document.class, "userCustomContents")
                .stream()
                .map(doc -> doc.getString("customContentId"))
                .toList();
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
        // 해당 사용자가 해금한 모든 콘텐츠 ID 조회
        List<String> allContentIds = getUserCustomContentIds(userId);

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
        query.addCriteria(Criteria.where("normalizedProgress").gt(0));

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
        return mongoTemplate.find(query, org.bson.Document.class, "customProgress")
                .stream()
                .map(doc -> doc.getString("customId"))
                .toList();
    }

    @Override
    public Page<CustomContent> findCustomContentsByUserWithFilters(String userId, GetCustomContentsRequest request, Pageable pageable) {
        List<AggregationOperation> operations = new ArrayList<>();

        // 1. UserCustomContent에서 userId로 필터링
        operations.add(Aggregation.match(Criteria.where("userId").is(userId)));

        // 2. customContentId를 ObjectId로 변환
        operations.add(Aggregation.addFields()
                .addField("customContentIdObj")
                .withValueOf(org.springframework.data.mongodb.core.aggregation.ConvertOperators.ToObjectId.toObjectId("$customContentId"))
                .build());

        // 3. CustomContent와 조인 (lookup) - ObjectId로 변환된 필드 사용
        operations.add(Aggregation.lookup(
                "customContents",           // from collection
                "customContentIdObj",       // localField (ObjectId로 변환된 필드)
                "_id",                      // foreignField (MongoDB의 _id 필드)
                "customContent"             // as
        ));

        // 4. 배열을 객체로 변환 (lookup 결과는 배열이므로)
        operations.add(Aggregation.unwind("customContent"));

        // 5. customContent를 root로 올림
        operations.add(Aggregation.replaceRoot("customContent"));

        // 6. isDeleted = false 필터링
        operations.add(Aggregation.match(Criteria.where("isDeleted").is(false)));

        // 7. 키워드 필터 적용
        if (StringUtils.hasText(request.getKeyword())) {
            Criteria keywordCriteria = new Criteria().orOperator(
                    Criteria.where("title").regex(request.getKeyword(), "i"),
                    Criteria.where("author").regex(request.getKeyword(), "i")
            );
            operations.add(Aggregation.match(keywordCriteria));
        }

        // 8. 태그 필터 적용
        if (StringUtils.hasText(request.getTags())) {
            String[] tagArray = request.getTags().split(",");
            operations.add(Aggregation.match(Criteria.where("tags").all((Object[]) tagArray)));
        }

        // 9. 진도 필터 적용 (별도 처리 필요)
        if (request.getProgress() != null) {
            List<String> contentIds = getContentIdsByProgress(userId, request.getProgress());
            if (contentIds.isEmpty()) {
                return new PageImpl<>(List.of(), pageable, 0);
            }
            // _id는 ObjectId이므로 String을 ObjectId로 변환해서 비교
            List<org.bson.types.ObjectId> objectIds = contentIds.stream()
                    .map(org.bson.types.ObjectId::new)
                    .toList();
            operations.add(Aggregation.match(Criteria.where("_id").in(objectIds)));
        }

        // 총 개수 조회용 aggregation (정렬 및 페이징 전, $count 사용)
        List<AggregationOperation> countOps = new ArrayList<>(operations);
        countOps.add(Aggregation.count().as("total"));
        Aggregation countAggregation = Aggregation.newAggregation(countOps);

        long total = 0;
        var countResult = mongoTemplate.aggregate(countAggregation, "userCustomContents", org.bson.Document.class)
                .getUniqueMappedResult();
        if (countResult != null && countResult.containsKey("total")) {
            total = ((Number) countResult.get("total")).longValue();
        }

        // 10. 정렬
        operations.add(Aggregation.sort(pageable.getSort()));

        // 11. 페이지네이션
        operations.add(Aggregation.skip(pageable.getOffset()));
        operations.add(Aggregation.limit(pageable.getPageSize()));

        // 최종 aggregation
        Aggregation aggregation = Aggregation.newAggregation(operations);
        List<CustomContent> contents = mongoTemplate.aggregate(aggregation, "userCustomContents", CustomContent.class)
                .getMappedResults();

        return new PageImpl<>(contents, pageable, total);
    }

    @Override
    public void incrementViewCount(String customContentId) {
        Query query = new Query(Criteria.where("id").is(customContentId));
        Update update = new Update().inc("viewCount", 1);
        mongoTemplate.updateFirst(query, update, CustomContent.class);
    }
}
