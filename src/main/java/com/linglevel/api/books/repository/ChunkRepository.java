package com.linglevel.api.books.repository;

import com.linglevel.api.books.entity.Chunk;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface ChunkRepository extends MongoRepository<Chunk, String> {
    Page<Chunk> findByChapterId(String chapterId, Pageable pageable);
    
    Optional<Chunk> findFirstByChapterIdOrderByChunkNumberAsc(String chapterId);

    Optional<Chunk> findById(String chunkId);
} 