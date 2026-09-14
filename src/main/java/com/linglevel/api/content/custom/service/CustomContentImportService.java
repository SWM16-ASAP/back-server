package com.linglevel.api.content.custom.service;

import com.linglevel.api.common.util.UrlNormalizer;
import com.linglevel.api.content.common.ChunkType;
import com.linglevel.api.content.common.DifficultyLevel;
import com.linglevel.api.content.custom.dto.AiResultDto;
import com.linglevel.api.content.custom.entity.ContentRequest;
import com.linglevel.api.content.custom.entity.CustomContent;
import com.linglevel.api.content.custom.entity.CustomContentChunk;
import com.linglevel.api.content.custom.repository.CustomContentChunkRepository;
import com.linglevel.api.content.custom.repository.CustomContentRepository;
import com.linglevel.api.s3.service.ImageResizeService;
import com.linglevel.api.s3.service.S3UrlService;
import com.linglevel.api.s3.strategy.CustomContentPathStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomContentImportService {

	private final CustomContentRepository customContentRepository;

	private final CustomContentChunkRepository customContentChunkRepository;

	private final S3UrlService s3UrlService;

	private final ImageResizeService imageResizeService;

	private final CustomContentPathStrategy pathStrategy;

	public CustomContent createCustomContent(ContentRequest contentRequest, AiResultDto aiResult) {
		if (aiResult == null || aiResult.getLeveledResults() == null || aiResult.getLeveledResults().isEmpty()) {
			throw new IllegalArgumentException("AI result contains no content levels");
		}
		String title = StringUtils.hasText(aiResult.getTitle()) ? aiResult.getTitle() : "Untitled Content";

		// ContentRequest의 coverImageUrl이 있으면 우선 사용, 없으면 AI 결과의 coverImageUrl 사용
		String coverImageUrl = StringUtils.hasText(contentRequest.getCoverImageUrl())
				? contentRequest.getCoverImageUrl() : aiResult.getCoverImageUrl();

		String normalizedUrl = null;
		if (StringUtils.hasText(contentRequest.getOriginUrl())) {
			normalizedUrl = UrlNormalizer.normalize(contentRequest.getOriginUrl());
		}

		CustomContent content = CustomContent.builder()
			.userId(contentRequest.getUserId())
			.contentRequestId(contentRequest.getId())
			.title(title)
			.author(contentRequest.getOriginAuthor())
			.coverImageUrl(coverImageUrl)
			.difficultyLevel(DifficultyLevel.fromCode(aiResult.getOriginalTextLevel()))
			.targetDifficultyLevels(aiResult.getLeveledResults()
				.stream()
				.map(level -> DifficultyLevel.fromCode(level.getTextLevel()))
				.collect(Collectors.toList()))
			.readingTime(0) // Placeholder, can be calculated later
			.originUrl(normalizedUrl != null ? normalizedUrl : contentRequest.getOriginUrl())
			.originDomain(contentRequest.getOriginDomain())
			.build();

		CustomContent savedContent = customContentRepository.save(content);

		if (StringUtils.hasText(aiResult.getCoverImageUrl())) {
			try {
				log.info("Auto-processing cover image for imported custom content: {}",
						savedContent.getId().toString());
				String originalCoverS3Key = pathStrategy.generateCoverImagePath(savedContent.getId().toString());
				String smallImageUrl = imageResizeService.createSmallImage(originalCoverS3Key);
				savedContent.setCoverImageUrl(smallImageUrl);
				customContentRepository.save(savedContent);
				log.info("Successfully auto-processed cover image: {} → {}", savedContent.getId().toString(),
						smallImageUrl);
			}
			catch (Exception e) {
				log.warn("Failed to auto-process cover image for custom content: {}, keeping original URL",
						savedContent.getId().toString(), e);
			}
		}

		return savedContent;
	}

	public void createCustomContentChunks(CustomContent customContent, AiResultDto aiResult) {
		List<CustomContentChunk> allChunks = new ArrayList<>();

		if (aiResult.getLeveledResults() == null) {
			log.warn("No leveled results found for custom content: {}", customContent.getId().toString());
			return;
		}

		boolean hasCoverImage = StringUtils.hasText(customContent.getCoverImageUrl());

		for (AiResultDto.LeveledResult leveledResult : aiResult.getLeveledResults()) {
			DifficultyLevel difficulty = DifficultyLevel.fromCode(leveledResult.getTextLevel());
			int chapterCounter = 1;

			for (AiResultDto.Chapter chapter : leveledResult.getChapters()) {
				int chunkCounter = 1;

				if (hasCoverImage && chapterCounter == 1) {
					CustomContentChunk coverImageChunk = CustomContentChunk.builder()
						.customContentId(customContent.getId().toString())
						.userId(customContent.getUserId().toString())
						.difficultyLevel(difficulty)
						.chapterNum(chapterCounter)
						.chunkNum(chunkCounter++)
						.type(ChunkType.IMAGE)
						.chunkText(customContent.getCoverImageUrl())
						.description("")
						.build();
					allChunks.add(coverImageChunk);
					log.debug("Added cover image chunk for difficulty {} at chapter {}", difficulty, chapterCounter);
				}

				for (AiResultDto.Chunk chunkData : chapter.getChunks()) {
					CustomContentChunk newChunk = createCustomContentChunk(chunkData, customContent.getId().toString(),
							customContent.getUserId().toString(), difficulty, chapterCounter, chunkCounter++);
					allChunks.add(newChunk);
				}
				chapterCounter++;
			}
		}
		if (allChunks.stream()
			.noneMatch(chunk -> chunk.getType() == ChunkType.TEXT && StringUtils.hasText(chunk.getChunkText()))) {
			throw new IllegalArgumentException("AI result contains no readable text");
		}
		customContentChunkRepository.saveAll(allChunks);
		log.info("Saved {} chunks for custom content {}", allChunks.size(), customContent.getId().toString());
	}

	private CustomContentChunk createCustomContentChunk(AiResultDto.Chunk chunkData, String customContentId,
			String userId, DifficultyLevel difficulty, int chapterNum, int chunkNum) {
		CustomContentChunk.CustomContentChunkBuilder builder = CustomContentChunk.builder()
			.customContentId(customContentId)
			.userId(userId)
			.difficultyLevel(difficulty)
			.chapterNum(chapterNum)
			.chunkNum(chunkNum);

		if (Boolean.TRUE.equals(chunkData.getIsImage())) {
			String imageUrl = s3UrlService.buildImageUrl(customContentId, chunkData.getChunkText(), pathStrategy);
			builder.type(ChunkType.IMAGE).chunkText(imageUrl).description(chunkData.getDescription());
		}
		else {
			builder.type(ChunkType.TEXT).chunkText(chunkData.getChunkText());
		}

		return builder.build();
	}

}
