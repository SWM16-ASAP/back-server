package com.linglevel.api.books.repository;

import com.linglevel.api.books.entity.Chapter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface ChapterRepository extends MongoRepository<Chapter, String> {
    Page<Chapter> findByBookId(String chapterId, Pageable pageable);
    
    List<Chapter> findByBookIdOrderByChapterNumber(String bookId);
    
    Optional<Chapter> findFirstByBookIdOrderByChapterNumberAsc(String chapterId);

    Optional<Chapter> findById(String chapterId);
} 