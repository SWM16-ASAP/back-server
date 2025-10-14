package com.linglevel.api.content.book.repository;

import com.linglevel.api.content.book.dto.GetChaptersRequest;
import com.linglevel.api.content.book.entity.BookProgress;
import com.linglevel.api.content.book.entity.Chapter;
import com.linglevel.api.content.common.ProgressStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.util.List;

@RequiredArgsConstructor
public class ChapterRepositoryImpl implements ChapterRepositoryCustom {

    private final MongoTemplate mongoTemplate;
    private final BookProgressRepository bookProgressRepository;

    @Override
    public Page<Chapter> findChaptersWithFilters(String bookId, GetChaptersRequest request, String userId, Pageable pageable) {
        Query query = buildQuery(bookId, request, userId);

        // 총 개수 조회 (필터링 적용 후)
        long total = mongoTemplate.count(query, Chapter.class);

        // 페이지네이션 적용
        query.with(pageable);

        // 데이터 조회
        List<Chapter> chapters = mongoTemplate.find(query, Chapter.class);

        return new PageImpl<>(chapters, pageable, total);
    }

    /**
     * 동적 쿼리 빌드
     */
    private Query buildQuery(String bookId, GetChaptersRequest request, String userId) {
        Query query = new Query();

        // bookId 필터는 항상 적용
        query.addCriteria(Criteria.where("bookId").is(bookId));

        // 진도 필터 적용
        applyProgressFilter(query, request.getProgress(), bookId, userId);

        return query;
    }

    /**
     * 진도 필터 적용
     */
    private void applyProgressFilter(Query query, ProgressStatus progress, String bookId, String userId) {
        if (progress == null || userId == null) {
            return;
        }

        BookProgress bookProgress = bookProgressRepository.findByUserIdAndBookId(userId, bookId)
            .orElse(null);

        List<Integer> chapterNumbers = getChapterNumbersByProgress(bookProgress, progress);

        if (chapterNumbers == null) {
            // null이면 필터링하지 않음 (모든 챕터 반환)
            return;
        }

        if (!chapterNumbers.isEmpty()) {
            query.addCriteria(Criteria.where("chapterNumber").in(chapterNumbers));
        } else {
            // 조건에 맞는 챕터가 없으면 빈 결과 반환
            query.addCriteria(Criteria.where("_id").is(null));
        }
    }

    /**
     * 진도 상태별 챕터 번호 목록 조회
     */
    private List<Integer> getChapterNumbersByProgress(BookProgress bookProgress, ProgressStatus progressStatus) {
        if (bookProgress == null) {
            // 진도 정보가 없으면 모든 챕터가 NOT_STARTED
            if (progressStatus == ProgressStatus.NOT_STARTED) {
                return null; // null이면 필터링하지 않음 (모든 챕터 반환)
            } else {
                return List.of(); // 빈 리스트 반환 (결과 없음)
            }
        }

        Integer currentChapterNumber = bookProgress.getCurrentReadChapterNumber() != null
            ? bookProgress.getCurrentReadChapterNumber() : 0;

        // 모든 챕터 번호 조회
        List<Chapter> allChapters = mongoTemplate.find(
            Query.query(Criteria.where("bookId").is(bookProgress.getBookId())),
            Chapter.class
        );

        return allChapters.stream()
            .map(Chapter::getChapterNumber)
            .filter(chapterNumber -> {
                return switch (progressStatus) {
                    case NOT_STARTED -> chapterNumber > currentChapterNumber;
                    case IN_PROGRESS -> chapterNumber.equals(currentChapterNumber) && currentChapterNumber > 0;
                    case COMPLETED -> chapterNumber < currentChapterNumber;
                };
            })
            .toList();
    }
}
