package com.linglevel.api.content.article.repository;

import com.linglevel.api.content.article.entity.ArticleChunk;
import com.linglevel.api.content.common.DifficultyLevel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface ArticleChunkRepository extends MongoRepository<ArticleChunk, String> {
    Page<ArticleChunk> findByArticleIdAndDifficultyLevelOrderByChunkNumber(String articleId, DifficultyLevel difficultyLevel, Pageable pageable);
    Optional<ArticleChunk> findByArticleIdAndId(String articleId, String chunkId);
}