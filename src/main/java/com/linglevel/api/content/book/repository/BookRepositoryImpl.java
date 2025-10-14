package com.linglevel.api.content.book.repository;

import com.linglevel.api.content.book.dto.GetBooksRequest;
import com.linglevel.api.content.book.entity.Book;
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

/**
 * Book Repository 커스텀 구현체
 * MongoTemplate + Criteria를 BooleanExpression 스타일로 사용
 */
@RequiredArgsConstructor
public class BookRepositoryImpl implements BookRepositoryCustom {

    private final MongoTemplate mongoTemplate;

    @Override
    public Page<Book> findBooksWithFilters(GetBooksRequest request, String userId, Pageable pageable) {
        Query query = buildQuery(request, userId);

        // 총 개수 조회 (필터링 적용 후)
        long total = mongoTemplate.count(query, Book.class);

        // 페이지네이션 적용
        query.with(pageable);

        // 데이터 조회
        List<Book> books = mongoTemplate.find(query, Book.class);

        return new PageImpl<>(books, pageable, total);
    }

    /**
     * 동적 쿼리 빌드 (BooleanExpression 스타일)
     */
    private Query buildQuery(GetBooksRequest request, String userId) {
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

        List<String> bookIds = getBookIdsByProgress(userId, progress);
        if (!bookIds.isEmpty()) {
            query.addCriteria(Criteria.where("id").in(bookIds));
        }
    }

    /**
     * 진도 상태별 책 ID 목록 조회
     */
    private List<String> getBookIdsByProgress(String userId, ProgressStatus progressStatus) {
        return switch (progressStatus) {
            case NOT_STARTED -> getNotStartedBookIds(userId);
            case IN_PROGRESS -> getInProgressBookIds(userId);
            case COMPLETED -> getCompletedBookIds(userId);
        };
    }

    /**
     * 시작하지 않은 책 ID 목록 조회
     */
    private List<String> getNotStartedBookIds(String userId) {
        // 모든 책 ID 조회
        List<String> allBookIds = mongoTemplate.findAll(Book.class).stream()
                .map(Book::getId)
                .toList();

        // 진도가 있는 책 ID 조회
        List<String> progressBookIds = findProgressBookIds(userId);

        // 진도가 없는 책만 반환
        return allBookIds.stream()
                .filter(bookId -> !progressBookIds.contains(bookId))
                .toList();
    }

    /**
     * 진행 중인 책 ID 목록 조회
     */
    private List<String> getInProgressBookIds(String userId) {
        Query query = new Query();
        query.addCriteria(Criteria.where("userId").is(userId));
        query.addCriteria(Criteria.where("isCompleted").is(false));
        query.addCriteria(Criteria.where("maxReadChunkNumber").gt(0));

        return findBookIdsFromProgress(query);
    }

    /**
     * 완료한 책 ID 목록 조회
     */
    private List<String> getCompletedBookIds(String userId) {
        Query query = new Query();
        query.addCriteria(Criteria.where("userId").is(userId));
        query.addCriteria(Criteria.where("isCompleted").is(true));

        return findBookIdsFromProgress(query);
    }

    /**
     * 특정 사용자의 모든 진도 책 ID 조회
     */
    private List<String> findProgressBookIds(String userId) {
        Query query = new Query();
        query.addCriteria(Criteria.where("userId").is(userId));

        return findBookIdsFromProgress(query);
    }

    /**
     * Progress 컬렉션에서 bookId 추출
     */
    private List<String> findBookIdsFromProgress(Query query) {
        return mongoTemplate.find(query, org.bson.Document.class, "bookProgress")
                .stream()
                .map(doc -> doc.getString("bookId"))
                .toList();
    }
}
