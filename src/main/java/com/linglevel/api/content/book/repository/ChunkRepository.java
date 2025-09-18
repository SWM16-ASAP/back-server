package com.linglevel.api.content.book.repository;

import com.linglevel.api.content.book.entity.Chunk;
import com.linglevel.api.content.common.DifficultyLevel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface ChunkRepository extends MongoRepository<Chunk, String> {
    Page<Chunk> findByChapterId(String chapterId, Pageable pageable);
    
    Page<Chunk> findByChapterIdAndDifficultyLevel(String chapterId, DifficultyLevel difficultyLevel, Pageable pageable);
    
    Optional<Chunk> findFirstByChapterIdOrderByChunkNumberAsc(String chapterId);

    Optional<Chunk> findById(String chunkId);
    
    List<Chunk> findByChapterIdOrderByChunkNumber(String chapterId);
} 