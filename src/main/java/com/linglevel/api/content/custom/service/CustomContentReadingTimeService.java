package com.linglevel.api.content.custom.service;

import com.linglevel.api.content.common.ChunkType;
import com.linglevel.api.content.common.service.ReadingTimeService;
import com.linglevel.api.content.custom.entity.CustomContent;
import com.linglevel.api.content.custom.entity.CustomContentChunk;
import com.linglevel.api.content.custom.exception.CustomContentErrorCode;
import com.linglevel.api.content.custom.exception.CustomContentException;
import com.linglevel.api.content.custom.repository.CustomContentChunkRepository;
import com.linglevel.api.content.custom.repository.CustomContentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomContentReadingTimeService {

    private final CustomContentRepository customContentRepository;
    private final CustomContentChunkRepository customContentChunkRepository;
    private final ReadingTimeService readingTimeService;

    @Transactional
    public void updateReadingTime(String customContentId) {
        CustomContent customContent = customContentRepository.findById(customContentId)
                .orElseThrow(() -> new CustomContentException(CustomContentErrorCode.CUSTOM_CONTENT_NOT_FOUND));

        List<CustomContentChunk> chunks = customContentChunkRepository.findByCustomContentIdAndDifficultyLevelAndIsDeletedFalseOrderByChapterNumAscChunkNumAsc(
                customContentId,
                customContent.getDifficultyLevel()
        );

        int totalCharacters = chunks.stream()
                .filter(chunk -> chunk.getType() == ChunkType.TEXT)
                .mapToInt(chunk -> chunk.getChunkText().length())
                .sum();

        int readingTime = readingTimeService.calculateReadingTimeFromCharacters(totalCharacters);
        customContent.setReadingTime(readingTime);

        customContentRepository.save(customContent);
    }
}
