package com.linglevel.api.content.book.repository;

import com.linglevel.api.content.book.dto.GetBooksRequest;
import com.linglevel.api.content.book.entity.Book;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface BookRepositoryCustom {
    /**
     * 동적 필터링이 적용된 책 목록 조회
     *
     * @param request 필터링 조건 (tags, keyword, progress 등)
     * @param userId 사용자 ID (진도 필터링에 사용)
     * @param pageable 페이지네이션 정보
     * @return 필터링된 책 페이지
     */
    Page<Book> findBooksWithFilters(GetBooksRequest request, String userId, Pageable pageable);
    
    void incrementViewCount(String bookId);
}
