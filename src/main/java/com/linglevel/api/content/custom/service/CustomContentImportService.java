package com.linglevel.api.content.custom.service;

import com.linglevel.api.content.common.ChunkType;
import com.linglevel.api.content.common.DifficultyLevel;
import com.linglevel.api.content.custom.dto.AiResultDto;
import com.linglevel.api.content.custom.entity.ContentRequest;
import com.linglevel.api.content.custom.entity.CustomContent;
import com.linglevel.api.content.custom.entity.CustomContentChunk;
import com.linglevel.api.content.custom.repository.CustomContentChunkRepository;
import com.linglevel.api.content.custom.repository.CustomContentRepository;
import com.linglevel.api.s3.service.S3UrlService;
import com.linglevel.api.s3.strategy.CustomContentPathStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomContentImportService {

    private final CustomContentRepository customContentRepository;
    private final CustomContentChunkRepository customContentChunkRepository;
    private final S3UrlService s3UrlService;
    private final CustomContentPathStrategy customContentPathStrategy;

    @Transactional
    public CustomContent createCustomContent(ContentRequest contentRequest, AiResultDto aiResult) {
        String title = StringUtils.hasText(aiResult.getTitle()) ? aiResult.getTitle() : "Untitled Content";
        int totalChunkCount = calculateTotalChunkCount(aiResult);

        CustomContent customContent = CustomContent.builder()
                .userId(contentRequest.getUserId())
                .contentRequestId(contentRequest.getId())
                .title(title)
                .author(aiResult.getAuthor())
                .coverImageUrl(aiResult.getCoverImageUrl())
                .difficultyLevel(DifficultyLevel.fromCode(aiResult.getOriginalTextLevel()))
                .targetDifficultyLevels(contentRequest.getTargetDifficultyLevels())
                .chunkCount(totalChunkCount)
                .originUrl(contentRequest.getOriginUrl())
                .originDomain(contentRequest.getOriginDomain())
                .build();

        return customContentRepository.save(customContent);
    }

    @Transactional
    public void createCustomContentChunks(CustomContent customContent, AiResultDto aiResult) {
        log.info("Creating chunks for custom content: {}", customContent.getId());
        
        List<CustomContentChunk> allChunks = new ArrayList<>();
        if (aiResult.getLeveledResults() == null) {
            return;
        }

        for (AiResultDto.LeveledResult leveledResult : aiResult.getLeveledResults()) {
            DifficultyLevel chunkDifficulty = DifficultyLevel.fromCode(leveledResult.getTextLevel());
            
            if (leveledResult.getChapters() != null) {
                for (AiResultDto.Chapter chapter : leveledResult.getChapters()) {
                    if (chapter.getChunks() != null) {
                        for (AiResultDto.Chunk chunkData : chapter.getChunks()) {
                            CustomContentChunk chunk = createCustomContentChunk(
                                    chunkData, 
                                    customContent.getId(), 
                                    customContent.getUserId(),
                                    chunkDifficulty, 
                                    chapter.getChapterNum() != null ? chapter.getChapterNum() : 0
                            );
                            allChunks.add(chunk);
                        }
                    }
                }
            }
        }

        if (!allChunks.isEmpty()) {
            List<CustomContentChunk> savedChunks = customContentChunkRepository.saveAll(allChunks);
            log.info("Successfully created {} chunks for custom content: {}", savedChunks.size(), customContent.getId());
        }
    }

    private CustomContentChunk createCustomContentChunk(AiResultDto.Chunk chunkData, String customContentId, String userId, DifficultyLevel difficulty, int chapterNum) {
        CustomContentChunk.CustomContentChunkBuilder builder = CustomContentChunk.builder()
                .customContentId(customContentId)
                .userId(userId)
                .difficultyLevel(difficulty)
                .chapterNum(chapterNum)
                .chunkNum(chunkData.getChunkNum() != null ? chunkData.getChunkNum() : 0);
        
        if (Boolean.TRUE.equals(chunkData.getIsImage())) {
            builder.type(ChunkType.IMAGE);
            String imageUrl = s3UrlService.buildImageUrl(customContentId, chunkData.getChunkText(), customContentPathStrategy) + "?w=256&h=256";
            builder.chunkText(imageUrl);
            builder.description(chunkData.getDescription());
        } else {
            builder.type(ChunkType.TEXT);
            builder.chunkText(chunkData.getChunkText() != null ? chunkData.getChunkText() : "");
            builder.description(null);
        }
        
        return builder.build();
    }

    public int calculateTotalChunkCount(AiResultDto aiResult) {
        if (aiResult.getLeveledResults() == null || aiResult.getLeveledResults().isEmpty()) {
            return 0;
        }

        AiResultDto.LeveledResult firstLevel = aiResult.getLeveledResults().get(0);
        if (firstLevel.getChapters() != null && !firstLevel.getChapters().isEmpty()) {
            return firstLevel.getChapters().get(0).getChunks() != null ? 
                   firstLevel.getChapters().get(0).getChunks().size() : 0;
        }
        
        return 0;
    }
}
