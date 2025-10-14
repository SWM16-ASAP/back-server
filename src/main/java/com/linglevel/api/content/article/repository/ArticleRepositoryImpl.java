package com.linglevel.api.content.article.repository;

import com.linglevel.api.content.article.dto.GetArticlesRequest;
import com.linglevel.api.content.article.entity.Article;
import com.linglevel.api.content.common.ProgressStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
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
        applyTagsFilter(query, request.getTags());
        applyKeywordFilter(query, request.getKeyword());
        applyProgressFilter(query, request.getProgress(), userId);

        return query;
    }

    /**
     * 태그 필터 적용
     */
    private void applyTagsFilter(Query query, String tags) {
        if (!StringUtils.hasText(tags)) {
            return;
        }

        List<String> tagList = Arrays.asList(tags.split(","));
        query.addCriteria(Criteria.where("tags").in(tagList));
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
     * 진도 필터 적용
     */
    private void applyProgressFilter(Query query, ProgressStatus progress, String userId) {
        if (progress == null || userId == null) {
            return;
        }

        List<String> articleIds = getArticleIdsByProgress(userId, progress);
        if (!articleIds.isEmpty()) {
            query.addCriteria(Criteria.where("id").in(articleIds));
        } else {
            // 조건에 맞는 아티클이 없으면 빈 결과 반환
            query.addCriteria(Criteria.where("_id").is(null));
        }
    }

    /**
     * 진도 상태별 아티클 ID 목록 조회
     */
    private List<String> getArticleIdsByProgress(String userId, ProgressStatus progressStatus) {
        return switch (progressStatus) {
            case NOT_STARTED -> getNotStartedArticleIds(userId);
            case IN_PROGRESS -> getInProgressArticleIds(userId);
            case COMPLETED -> getCompletedArticleIds(userId);
        };
    }

    /**
     * 시작하지 않은 아티클 ID 목록 조회
     */
    private List<String> getNotStartedArticleIds(String userId) {
        // 모든 아티클 ID 조회
        List<String> allArticleIds = mongoTemplate.findAll(Article.class).stream()
                .map(Article::getId)
                .toList();

        // 진도가 있는 아티클 ID 조회
        List<String> progressArticleIds = findProgressArticleIds(userId);

        // 진도가 없는 아티클만 반환
        return allArticleIds.stream()
                .filter(articleId -> !progressArticleIds.contains(articleId))
                .toList();
    }

    /**
     * 진행 중인 아티클 ID 목록 조회
     */
    private List<String> getInProgressArticleIds(String userId) {
        Query query = new Query();
        query.addCriteria(Criteria.where("userId").is(userId));
        query.addCriteria(Criteria.where("isCompleted").is(false));
        query.addCriteria(Criteria.where("currentReadChunkNumber").gt(0));

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
        return mongoTemplate.find(query, org.bson.Document.class, "articleProgress")
                .stream()
                .map(doc -> doc.getString("articleId"))
                .toList();
    }
}
