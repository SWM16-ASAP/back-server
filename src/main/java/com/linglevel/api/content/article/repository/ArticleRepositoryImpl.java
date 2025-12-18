package com.linglevel.api.content.article.repository;

import com.linglevel.api.content.article.dto.GetArticleOriginsRequest;
import com.linglevel.api.content.article.dto.GetArticlesRequest;
import com.linglevel.api.content.article.entity.Article;
import com.linglevel.api.content.common.ContentCategory;
import com.linglevel.api.content.common.ProgressStatus;
import com.linglevel.api.i18n.LanguageCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.mongodb.core.query.TextCriteria;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;

@RequiredArgsConstructor
public class ArticleRepositoryImpl implements ArticleRepositoryCustom {

    private final MongoTemplate mongoTemplate;

    @Override
    public Page<Article> findArticlesWithFilters(GetArticlesRequest request, String userId, Pageable pageable) {
        Query query = buildQuery(request, userId);

        // 총 개수 조회 (필터링 적용 후)
        long total = mongoTemplate.count(query, Article.class);

        applyListProjection(query);

        // 페이지네이션 적용
        query.with(pageable);

        // 데이터 조회
        List<Article> articles = mongoTemplate.find(query, Article.class);

        return new PageImpl<>(articles, pageable, total);
    }

    /**
     * 동적 쿼리 빌드
     */
    private Query buildQuery(GetArticlesRequest request, String userId) {
        Query query = new Query();

        // 각 필터를 독립적인 메서드로 분리
        applyCategoryFilter(query, request.getCategory());
        applyTagsFilter(query, request.getTags());
        applyKeywordFilter(query, request.getKeyword());
        applyProgressFilter(query, request.getProgress(), userId);
        applyTargetLanguageCodeFilter(query, request.getTargetLanguageCode());
        applyCreatedAfterFilter(query, request.getCreatedAfter());

        return query;
    }

    /**
     * 카테고리 필터 적용
     */
    private void applyCategoryFilter(Query query, ContentCategory category) {
        if (category == null) {
            return;
        }

        query.addCriteria(Criteria.where("category").is(category));
    }

    /**
     * 태그 필터 적용
     */
    private void applyTagsFilter(Query query, String tags) {
        if (!StringUtils.hasText(tags)) {
            return;
        }

        List<String> tagList = Arrays.stream(tags.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toList();
        if (tagList.isEmpty()) {
            return;
        }
        query.addCriteria(Criteria.where("tags").in(tagList));
    }

    /**
     * 키워드 필터 적용 (제목 또는 작가)
     */
    private void applyKeywordFilter(Query query, String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return;
        }

        TextCriteria textCriteria = TextCriteria.forDefaultLanguage()
                .matchingAny(keyword.split("\\s+"));
        query.addCriteria(textCriteria);
    }

    /**
     * 진도 필터 적용
     */
    private void applyProgressFilter(Query query, ProgressStatus progress, String userId) {
        if (progress == null || userId == null) {
            return;
        }

        switch (progress) {
            case NOT_STARTED -> applyNotStartedFilter(query, userId);
            case IN_PROGRESS -> applyArticleIdFilter(query, getInProgressArticleIds(userId));
            case COMPLETED -> applyArticleIdFilter(query, getCompletedArticleIds(userId));
        }
    }

    /**
     * NOT_STARTED - 사용자가 진도 등록한 아티클을 제외
     */
    private void applyNotStartedFilter(Query query, String userId) {
        List<String> progressArticleIds = findProgressArticleIds(userId);
        if (progressArticleIds.isEmpty()) {
            return; // 진도가 없으면 전체가 NOT_STARTED이므로 추가 필터 불필요
        }

        query.addCriteria(Criteria.where("id").nin(progressArticleIds));
    }

    /**
     * 특정 articleId 목록만 허용
     */
    private void applyArticleIdFilter(Query query, List<String> articleIds) {
        if (articleIds.isEmpty()) {
            query.addCriteria(Criteria.where("_id").is(null));
            return;
        }

        query.addCriteria(Criteria.where("id").in(articleIds));
    }

    /**
     * 타깃 언어 코드 필터 적용
     */
    private void applyTargetLanguageCodeFilter(Query query, LanguageCode targetLanguageCode) {
        if (targetLanguageCode == null) {
            return;
        }

        query.addCriteria(Criteria.where("targetLanguageCode").is(targetLanguageCode));
    }

    /**
     * 생성 시간 필터 적용 (해당 시간 이후)
     */
    private void applyCreatedAfterFilter(Query query, java.time.LocalDateTime createdAfter) {
        if (createdAfter == null) {
            return;
        }

        query.addCriteria(Criteria.where("createdAt").gte(createdAfter.toInstant(java.time.ZoneOffset.UTC)));
    }

    /**
     * 진행 중인 아티클 ID 목록 조회
     */
    private List<String> getInProgressArticleIds(String userId) {
        Query query = new Query();
        query.addCriteria(Criteria.where("userId").is(userId));
        query.addCriteria(Criteria.where("isCompleted").is(false));
        query.addCriteria(Criteria.where("normalizedProgress").gt(0));

        return findArticleIdsFromProgress(query);
    }

    /**
     * 완료한 아티클 ID 목록 조회
     */
    private List<String> getCompletedArticleIds(String userId) {
        Query query = new Query();
        query.addCriteria(Criteria.where("userId").is(userId));
        query.addCriteria(Criteria.where("isCompleted").is(true));

        return findArticleIdsFromProgress(query);
    }

    /**
     * 특정 사용자의 모든 진도 아티클 ID 조회
     */
    private List<String> findProgressArticleIds(String userId) {
        Query query = new Query();
        query.addCriteria(Criteria.where("userId").is(userId));

        return findArticleIdsFromProgress(query);
    }

    /**
     * ArticleProgress 컬렉션에서 articleId 추출
     */
    private List<String> findArticleIdsFromProgress(Query query) {
        query.fields().include("articleId");
        return mongoTemplate.find(query, org.bson.Document.class, "articleProgress")
                .stream()
                .map(doc -> doc.getString("articleId"))
                .toList();
    }

    /**
     * 리스트 응답에 필요한 필드만 선택
     */
    private void applyListProjection(Query query) {
        query.fields()
                .include("title")
                .include("author")
                .include("coverImageUrl")
                .include("difficultyLevel")
                .include("readingTime")
                .include("averageRating")
                .include("reviewCount")
                .include("viewCount")
                .include("category")
                .include("tags")
                .include("targetLanguageCode")
                .include("createdAt");
    }

    @Override
    public Page<Article> findArticleOriginsWithFilters(GetArticleOriginsRequest request, Pageable pageable) {
        Query query = buildOriginQuery(request);

        // 총 개수 조회
        long total = mongoTemplate.count(query, Article.class);

        // 페이지네이션 적용
        query.with(pageable);

        // 데이터 조회
        List<Article> articles = mongoTemplate.find(query, Article.class);

        return new PageImpl<>(articles, pageable, total);
    }

    /**
     * originUrl 조회용 동적 쿼리 빌드
     */
    private Query buildOriginQuery(GetArticleOriginsRequest request) {
        Query query = new Query();

        // originUrl이 null이 아닌 것만 조회
        query.addCriteria(Criteria.where("originUrl").ne(null));

        applyCategoryFilter(query, request.getCategoryEnum());
        applyTagsFilter(query, request.getTags());
        applyTargetLanguageCodeFilter(query, request.getTargetLanguageCode());

        return query;
    }

    @Override
    public void incrementViewCount(String articleId) {
        Query query = new Query(Criteria.where("id").is(articleId));
        Update update = new Update().inc("viewCount", 1);
        mongoTemplate.updateFirst(query, update, Article.class);
    }
}
