package com.linglevel.api.content.article.service;

import com.linglevel.api.content.common.ChunkType;
import com.linglevel.api.content.common.DifficultyLevel;
import com.linglevel.api.content.article.dto.ArticleImportData;
import com.linglevel.api.content.article.entity.ArticleChunk;
import com.linglevel.api.content.article.repository.ArticleChunkRepository;
import com.linglevel.api.s3.service.S3UrlService;
import com.linglevel.api.s3.strategy.ArticlePathStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ArticleImportService {

	private final ArticleChunkRepository articleChunkRepository;

	private final S3UrlService s3UrlService;

	private final ArticlePathStrategy articlePathStrategy;

	public void createChunksFromLeveledResults(ArticleImportData importData, String articleId) {
		log.info("Creating chunks for article: {}", articleId);

		List<ArticleChunk> allChunks = new ArrayList<>();
		if (importData.getLeveledResults() == null || importData.getLeveledResults().isEmpty()) {
			throw new IllegalArgumentException("Article must contain leveled text");
		}

		for (ArticleImportData.TextLevelData levelData : importData.getLeveledResults()) {
			DifficultyLevel difficulty = DifficultyLevel.valueOf(levelData.getTextLevel().toUpperCase());

			if (levelData.getChapters() == null || levelData.getChapters().size() != 1) {
				throw new IllegalArgumentException("Article must have exactly one chapter per level");
			}
			{
				ArticleImportData.ChapterData chapterData = levelData.getChapters().get(0);
				if (chapterData.getChunks() == null || chapterData.getChunks()
					.stream()
					.noneMatch(c -> !Boolean.TRUE.equals(c.getIsImage())
							&& org.springframework.util.StringUtils.hasText(c.getChunkText()))) {
					throw new IllegalArgumentException("Article has no text");
				}

				int chunkCounter = 1;
				for (ArticleImportData.ChunkData chunkData : chapterData.getChunks()) {
					ArticleChunk chunk = createArticleChunk(chunkData, articleId, difficulty, chunkCounter++);
					allChunks.add(chunk);
				}
			}
		}

		List<ArticleChunk> savedChunks = articleChunkRepository.saveAll(allChunks);
		log.info("Successfully created {} chunks for article: {}", savedChunks.size(), articleId);
	}

	private ArticleChunk createArticleChunk(ArticleImportData.ChunkData chunkData, String articleId,
			DifficultyLevel difficulty, int chunkNumber) {
		ArticleChunk chunk = new ArticleChunk();
		chunk.setArticleId(articleId);
		chunk.setChunkNumber(chunkNumber);
		chunk.setDifficultyLevel(difficulty);

		if (Boolean.TRUE.equals(chunkData.getIsImage())) {
			chunk.setType(ChunkType.IMAGE);
			String imageUrl = s3UrlService.buildImageUrl(articleId, chunkData.getChunkText(), articlePathStrategy);
			chunk.setContent(imageUrl);
			chunk.setDescription(chunkData.getDescription());
		}
		else {
			chunk.setType(ChunkType.TEXT);
			chunk.setContent(chunkData.getChunkText());
			chunk.setDescription(null);
		}

		return chunk;
	}

}
