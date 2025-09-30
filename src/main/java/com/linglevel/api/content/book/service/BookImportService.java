package com.linglevel.api.content.book.service;

import com.linglevel.api.content.book.dto.BookImportData;
import com.linglevel.api.content.book.entity.Chapter;
import com.linglevel.api.content.book.entity.Chunk;
import com.linglevel.api.content.common.ChunkType;
import com.linglevel.api.content.common.DifficultyLevel;
import com.linglevel.api.content.book.repository.ChapterRepository;
import com.linglevel.api.content.book.repository.ChunkRepository;
import com.linglevel.api.s3.service.S3UrlService;
import com.linglevel.api.s3.strategy.BookPathStrategy;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookImportService {

    private final ChapterRepository chapterRepository;
    private final ChunkRepository chunkRepository;
    private final S3UrlService s3UrlService;
    private final BookPathStrategy bookPathStrategy;

    public List<Chapter> createChaptersFromMetadata(BookImportData importData, String bookId) {
        List<Chapter> chapters = importData.getChapterMetadata().stream()
                .map(metadata -> {
                    Chapter chapter = new Chapter();
                    chapter.setBookId(bookId);
                    chapter.setChapterNumber(metadata.getChapterNum());
                    chapter.setTitle(metadata.getTitle());
                    chapter.setDescription(metadata.getSummary());
                    chapter.setReadingTime(0);
                    
                    int chunkCount = getChunkCountForChapter(importData, metadata.getChapterNum());
                    chapter.setChunkCount(chunkCount);
                    
                    return chapter;
                })
                .collect(Collectors.toList());
        
        return chapterRepository.saveAll(chapters);
    }

    public void createChunksFromLeveledResults(BookImportData importData, List<Chapter> savedChapters, String databaseBookId) {
        Map<Integer, Chapter> chapterMap = savedChapters.stream()
                .collect(Collectors.toMap(Chapter::getChapterNumber, chapter -> chapter));
        
        List<Chunk> allChunks = importData.getLeveledResults().stream()
                .flatMap(textLevelData ->
                    textLevelData.getChapters().stream()
                        .flatMap(chapterData ->
                            chapterData.getChunks().stream()
                                .map(chunkData -> createChunk(
                                    chunkData,
                                    chapterMap.get(chapterData.getChapterNum()),
                                    textLevelData.getTextLevel(),
                                    databaseBookId
                                ))
                        )
                )
                .collect(Collectors.toList());
        
        chunkRepository.saveAll(allChunks);
    }

    private int getChunkCountForChapter(BookImportData importData, int chapterNum) {
        return importData.getLeveledResults().stream()
                .findFirst()
                .flatMap(firstLevel -> 
                    firstLevel.getChapters().stream()
                        .filter(chapterData -> chapterData.getChapterNum() == chapterNum)
                        .findFirst()
                        .map(chapterData -> chapterData.getChunks().size())
                )
                .orElse(0);
    }
    
    private Chunk createChunk(BookImportData.ChunkData chunkData, Chapter chapter, String difficultyLevel, String bookId) {
        Chunk chunk = new Chunk();
        chunk.setChapterId(chapter.getId());
        chunk.setChunkNumber(chunkData.getChunkNum());
        chunk.setDifficultyLevel(DifficultyLevel.valueOf(difficultyLevel.toUpperCase()));
        
        if (Boolean.TRUE.equals(chunkData.getIsImage())) {
            chunk.setType(ChunkType.IMAGE);
            String imageUrl = s3UrlService.buildImageUrl(bookId, chunkData.getChunkText(), bookPathStrategy);
            chunk.setContent(imageUrl);
            chunk.setDescription(chunkData.getDescription());
        } else {
            chunk.setType(ChunkType.TEXT);
            chunk.setContent(chunkData.getChunkText());
            chunk.setDescription(null);
        }
        
        return chunk;
    }
}