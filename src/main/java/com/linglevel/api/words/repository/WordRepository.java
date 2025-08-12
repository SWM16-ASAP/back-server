package com.linglevel.api.words.repository;

import com.linglevel.api.words.entity.Word;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WordRepository extends MongoRepository<Word, String> {
    Optional<Word> findByWord(String word);
    
    @Query("{'word': {$regex: ?0, $options: 'i'}}")
    Page<Word> findByWordContainingIgnoreCase(String word, Pageable pageable);
    
    boolean existsByWord(String word);
}