package com.linglevel.api.content.custom.repository;

import com.linglevel.api.content.custom.entity.CustomContentChunk;
import com.linglevel.api.content.common.DifficultyLevel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.mongodb.repository.Query;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomContentChunkRepository extends MongoRepository<CustomContentChunk, String> {

    List<CustomContentChunk> findByCustomContentIdAndIsDeletedFalseOrderByChapterNumAscChunkNumAsc(String customContentId);
    
    List<CustomContentChunk> findByCustomContentIdAndDifficultyLevelAndIsDeletedFalseOrderByChapterNumAscChunkNumAsc(String customContentId, DifficultyLevel difficultyLevel);
    
    @Query("{ 'customContentId': ?0, 'difficultyLevel': ?1, 'isDeleted': false }")
    Page<CustomContentChunk> findByCustomContentIdAndDifficultyLevelAndIsDeletedFalseOrderByChapterNumAscChunkNumAsc(String customContentId, DifficultyLevel difficultyLevel, Pageable pageable);
    
    List<CustomContentChunk> findByUserIdAndIsDeletedFalse(String userId);
    
    Page<CustomContentChunk> findByCustomContentIdAndIsDeletedFalseOrderByChapterNumAscChunkNumAsc(
            String customContentId, Pageable pageable);
    
    Optional<CustomContentChunk> findByIdAndCustomContentIdAndIsDeletedFalse(String id, String customContentId);
    
    long countByCustomContentIdAndIsDeletedFalse(String customContentId);

    Optional<CustomContentChunk> findFirstByCustomContentIdAndIsDeletedFalseOrderByChapterNumAscChunkNumAsc(String customContentId);

    // V2 Progress: Count chunks by difficulty level
    long countByCustomContentIdAndDifficultyLevelAndIsDeletedFalse(String customContentId, DifficultyLevel difficultyLevel);
}