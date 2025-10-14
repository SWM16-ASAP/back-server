package com.linglevel.api.content.book.repository;

import com.linglevel.api.content.book.dto.GetChaptersRequest;
import com.linglevel.api.content.book.entity.Chapter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ChapterRepositoryCustom {
    Page<Chapter> findChaptersWithFilters(String bookId, GetChaptersRequest request, String userId, Pageable pageable);
}
