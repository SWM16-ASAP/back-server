package com.linglevel.api.content.book.repository;

import com.linglevel.api.content.book.entity.Chapter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface ChapterRepository extends MongoRepository<Chapter, String>, ChapterRepositoryCustom {
    Page<Chapter> findByBookId(String chapterId, Pageable pageable);
    
    List<Chapter> findByBookIdOrderByChapterNumber(String bookId);
    
    Optional<Chapter> findFirstByBookIdOrderByChapterNumberAsc(String chapterId);

    Integer countByBookId(String bookId);

    Optional<Chapter> findById(String chapterId);
} 