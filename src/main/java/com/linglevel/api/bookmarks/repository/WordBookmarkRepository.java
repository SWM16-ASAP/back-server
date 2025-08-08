package com.linglevel.api.bookmarks.repository;

import com.linglevel.api.bookmarks.entity.WordBookmark;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WordBookmarkRepository extends MongoRepository<WordBookmark, String> {
    
    boolean existsByUserIdAndWord(String userId, String word);
    
    Page<WordBookmark> findByUserId(String userId, Pageable pageable);

    Page<WordBookmark> findByUserIdAndWordIn(String userId, java.util.List<String> words, Pageable pageable);
    
    void deleteByUserIdAndWord(String userId, String word);
}