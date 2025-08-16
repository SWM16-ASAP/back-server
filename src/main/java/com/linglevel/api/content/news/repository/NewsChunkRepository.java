package com.linglevel.api.content.news.repository;

import com.linglevel.api.content.common.DifficultyLevel;
import com.linglevel.api.content.news.entity.NewsChunk;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface NewsChunkRepository extends MongoRepository<NewsChunk, String> {
    Page<NewsChunk> findByNewsIdAndDifficultyOrderByChunkNumber(String newsId, DifficultyLevel difficulty, Pageable pageable);
    Optional<NewsChunk> findByNewsIdAndId(String newsId, String chunkId);
}