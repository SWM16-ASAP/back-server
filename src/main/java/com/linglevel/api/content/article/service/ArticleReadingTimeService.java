package com.linglevel.api.content.article.service;

import com.linglevel.api.content.article.dto.ArticleImportData;
import com.linglevel.api.content.article.entity.Article;
import com.linglevel.api.content.common.DifficultyLevel;
import com.linglevel.api.content.common.service.ReadingTimeService;
import com.linglevel.api.content.article.exception.ArticleErrorCode;
import com.linglevel.api.content.article.exception.ArticleException;
import com.linglevel.api.content.article.repository.ArticleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ArticleReadingTimeService {

    private final ArticleRepository articleRepository;
    private final ReadingTimeService readingTimeService;

    public void updateReadingTime(String articleId, ArticleImportData importData) {
        Article article = articleRepository.findById(articleId)
            .orElseThrow(() -> new ArticleException(ArticleErrorCode.ARTICLE_NOT_FOUND));
        
        int articleReadingTime = calculateArticleReadingTime(article.getDifficultyLevel(), importData);
        article.setReadingTime(articleReadingTime);
        
        articleRepository.save(article);
    }
    
    private int calculateArticleReadingTime(DifficultyLevel difficultyLevel, ArticleImportData importData) {
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