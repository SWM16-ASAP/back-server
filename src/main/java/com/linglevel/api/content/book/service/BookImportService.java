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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookImportService {

    private final ChapterRepository chapterRepository;
    private final ChunkRepository chunkRepository;
    private final S3UrlService s3UrlService;
    private final BookPathStrategy bookPathStrategy;

    public List<Chapter> createChaptersFromMetadata(BookImportData importData, String bookId) {
        AtomicInteger chapterCounter = new AtomicInteger(1);
        List<Chapter> chapters = importData.getChapterMetadata().stream()
                .map(metadata -> {
                    Chapter chapter = new Chapter();
                    chapter.setBookId(bookId);
                    chapter.setChapterNumber(chapterCounter.getAndIncrement());
                    chapter.setTitle(metadata.getTitle());
                    chapter.setDescription(metadata.getSummary());
                    chapter.setReadingTime(0);
                    return chapter;
                })
                .collect(Collectors.toList());
        
        return chapterRepository.saveAll(chapters);
    }

    public void createChunksFromLeveledResults(BookImportData importData, List<Chapter> savedChapters, String databaseBookId) {
        List<Chunk> allChunks = new ArrayList<>();

        for (BookImportData.TextLevelData levelData : importData.getLeveledResults()) {
            DifficultyLevel difficulty = DifficultyLevel.valueOf(levelData.getTextLevel().toUpperCase());
            List<BookImportData.ChapterData> aiChapters = levelData.getChapters();

            for (int i = 0; i < savedChapters.size(); i++) {
                if (i >= aiChapters.size()) break; // Safety break if lists are not aligned

                Chapter savedChapter = savedChapters.get(i);
                BookImportData.ChapterData aiChapterData = aiChapters.get(i);

                int chunkCounter = 1;
                for (BookImportData.ChunkData chunkData : aiChapterData.getChunks()) {
                    Chunk chunk = createChunk(chunkData, savedChapter, difficulty, databaseBookId, chunkCounter++);
                    allChunks.add(chunk);
                }
            }
        }
        chunkRepository.saveAll(allChunks);
    }
    
    private Chunk createChunk(BookImportData.ChunkData chunkData, Chapter chapter, DifficultyLevel difficulty, String bookId, int chunkNumber) {
        Chunk chunk = new Chunk();
        chunk.setChapterId(chapter.getId());
        chunk.setChunkNumber(chunkNumber);
        chunk.setDifficultyLevel(difficulty);
        
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