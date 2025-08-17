package com.linglevel.api.content.news.service;

import com.linglevel.api.content.news.dto.NewsImportData;
import com.linglevel.api.content.news.entity.News;
import com.linglevel.api.content.common.DifficultyLevel;
import com.linglevel.api.content.common.service.ReadingTimeService;
import com.linglevel.api.content.news.exception.NewsErrorCode;
import com.linglevel.api.content.news.exception.NewsException;
import com.linglevel.api.content.news.repository.NewsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NewsReadingTimeService {

    private final NewsRepository newsRepository;
    private final ReadingTimeService readingTimeService;

    public void updateReadingTime(String newsId, NewsImportData importData) {
        News news = newsRepository.findById(newsId)
            .orElseThrow(() -> new NewsException(NewsErrorCode.NEWS_NOT_FOUND));
        
        int newsReadingTime = calculateNewsReadingTime(news.getDifficultyLevel(), importData);
        news.setReadingTime(newsReadingTime);
        
        newsRepository.save(news);
    }
    
    private int calculateNewsReadingTime(DifficultyLevel difficultyLevel, NewsImportData importData) {
        int totalCharacters = importData.getLeveledResults().stream()
            .filter(levelData -> DifficultyLevel.valueOf(levelData.getTextLevel().toUpperCase()) == difficultyLevel)
            .flatMap(levelData -> levelData.getChapters().stream())
            .flatMap(chapterData -> chapterData.getChunks().stream())
            .filter(chunkData -> !Boolean.TRUE.equals(chunkData.getIsImage()))
            .mapToInt(chunkData -> chunkData.getChunkText().length())
            .sum();
        
        return readingTimeService.calculateReadingTimeFromCharacters(totalCharacters);
    }
}