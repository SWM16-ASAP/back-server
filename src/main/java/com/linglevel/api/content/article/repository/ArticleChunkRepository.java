package com.linglevel.api.content.article.repository;

import com.linglevel.api.content.article.entity.ArticleChunk;
import com.linglevel.api.content.common.DifficultyLevel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ArticleChunkRepository extends JpaRepository<ArticleChunk, Long> {

	Page<ArticleChunk> findByArticleIdAndDifficultyLevelOrderByChunkNumber(Long articleId,
			DifficultyLevel difficultyLevel, Pageable pageable);

	default Page<ArticleChunk> findByArticleIdAndDifficultyLevelOrderByChunkNumber(String articleId,
			DifficultyLevel difficultyLevel, Pageable pageable) {
		return findByArticleIdAndDifficultyLevelOrderByChunkNumber(Long.valueOf(articleId), difficultyLevel, pageable);
	}

	Optional<ArticleChunk> findByArticleIdAndId(Long articleId, Long chunkId);

	default Optional<ArticleChunk> findByArticleIdAndId(String articleId, String chunkId) {
		return findByArticleIdAndId(Long.valueOf(articleId), Long.valueOf(chunkId));
	}

	Optional<ArticleChunk> findFirstByArticleIdOrderByChunkNumber(Long articleId);

	default Optional<ArticleChunk> findFirstByArticleIdOrderByChunkNumber(String articleId) {
		return findFirstByArticleIdOrderByChunkNumber(Long.valueOf(articleId));
	}

	Optional<ArticleChunk> findById(Long chunkId);

	default Optional<ArticleChunk> findById(String chunkId) {
		return findById(Long.valueOf(chunkId));
	}

	long countByArticleIdAndDifficultyLevel(Long articleId, DifficultyLevel difficultyLevel);

	default long countByArticleIdAndDifficultyLevel(String articleId, DifficultyLevel difficultyLevel) {
		return countByArticleIdAndDifficultyLevel(Long.valueOf(articleId), difficultyLevel);
	}

}
