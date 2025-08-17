package com.linglevel.api.content.news.service;

import com.linglevel.api.content.common.ChunkType;
import com.linglevel.api.content.common.DifficultyLevel;
import com.linglevel.api.content.news.dto.NewsImportData;
import com.linglevel.api.content.news.entity.NewsChunk;
import com.linglevel.api.content.news.repository.NewsChunkRepository;
import com.linglevel.api.s3.service.S3UrlService;
import com.linglevel.api.s3.strategy.NewsPathStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NewsImportService {

    private final NewsChunkRepository newsChunkRepository;
    private final S3UrlService s3UrlService;
    private final NewsPathStrategy newsPathStrategy;

    public void createChunksFromLeveledResults(NewsImportData importData, String newsId) {
        log.info("Creating chunks for news: {}", newsId);
        
        List<NewsChunk> allChunks = new ArrayList<>();
        
        for (NewsImportData.TextLevelData levelData : importData.getLeveledResults()) {
            DifficultyLevel difficulty = DifficultyLevel.valueOf(levelData.getTextLevel().toUpperCase());
            
            // 챕터는 1개가 보장되므로 첫 번째 챕터만 사용
            if (!levelData.getChapters().isEmpty()) {
                NewsImportData.ChapterData chapterData = levelData.getChapters().get(0);
                
                for (NewsImportData.ChunkData chunkData : chapterData.getChunks()) {
                    NewsChunk chunk = createNewsChunk(chunkData, newsId, difficulty);
                    allChunks.add(chunk);
                }
            }
        }
        
        List<NewsChunk> savedChunks = newsChunkRepository.saveAll(allChunks);
        log.info("Successfully created {} chunks for news: {}", savedChunks.size(), newsId);
    }

    private NewsChunk createNewsChunk(NewsImportData.ChunkData chunkData, String newsId, DifficultyLevel difficulty) {
        NewsChunk chunk = new NewsChunk();
        chunk.setNewsId(newsId);
        chunk.setChunkNumber(chunkData.getChunkNum());
        chunk.setDifficulty(difficulty);
        
        if (Boolean.TRUE.equals(chunkData.getIsImage())) {
            chunk.setType(ChunkType.IMAGE);
            String imageUrl = s3UrlService.buildImageUrl(newsId, chunkData.getChunkText(), newsPathStrategy);
            chunk.setContent(imageUrl);
            chunk.setDescription(chunkData.getDescription());
        } else {
            chunk.setType(ChunkType.TEXT);
            chunk.setContent(chunkData.getChunkText());
            chunk.setDescription(null);
        }
        
        return chunk;
    }


    public int calculateTotalChunkCount(NewsImportData importData) {
        if (importData.getLeveledResults().isEmpty()) {
            return 0;
        }

        NewsImportData.TextLevelData firstLevel = importData.getLeveledResults().get(0);
        if (!firstLevel.getChapters().isEmpty()) {
            return firstLevel.getChapters().get(0).getChunks().size();
        }
        
        return 0;
    }
}