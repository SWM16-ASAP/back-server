package com.linglevel.api.content.article.service;

import com.linglevel.api.content.common.DifficultyLevel;
import com.linglevel.api.content.article.dto.*;
import com.linglevel.api.content.article.entity.ArticleChunk;
import com.linglevel.api.content.article.exception.ArticleErrorCode;
import com.linglevel.api.content.article.exception.ArticleException;
import com.linglevel.api.content.article.repository.ArticleChunkRepository;
import com.linglevel.api.content.article.repository.ArticleRepository;
import com.linglevel.api.common.dto.PageResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ArticleChunkService {

    private final ArticleChunkRepository articleChunkRepository;
    private final ArticleRepository articleRepository;

    public PageResponse<ArticleChunkResponse> getArticleChunks(String articleId, GetArticleChunksRequest request) {
        validateArticleExists(articleId);

        DifficultyLevel difficulty = request.getDifficultyLevel();

        validatePaginationRequest(request);
        Pageable pageable = PageRequest.of(request.getPage() - 1, request.getLimit());

        Page<ArticleChunk> chunksPage = articleChunkRepository.findByArticleIdAndDifficultyLevelOrderByChunkNumber(
                articleId, difficulty, pageable);
        
        List<ArticleChunkResponse> chunkResponses = chunksPage.getContent().stream()
                .map(this::convertToArticleChunkResponse)
                .toList();
        
        return PageResponse.of(chunksPage, chunkResponses);
    }

    public ArticleChunkResponse getArticleChunk(String articleId, String chunkId) {
        validateArticleExists(articleId);

        ArticleChunk chunk = articleChunkRepository.findByArticleIdAndId(articleId, chunkId)
                .orElseThrow(() -> new ArticleException(ArticleErrorCode.CHUNK_NOT_FOUND));
        
        return convertToArticleChunkResponse(chunk);
    }

    private void validateArticleExists(String articleId) {
        if (!articleRepository.existsById(articleId)) {
            throw new ArticleException(ArticleErrorCode.ARTICLE_NOT_FOUND);
        }
    }

    private DifficultyLevel validateAndParseDifficulty(String difficultyStr) {
        if (difficultyStr == null || difficultyStr.trim().isEmpty()) {
            throw new ArticleException(ArticleErrorCode.INVALID_DIFFICULTY_LEVEL);
        }
        
        try {
            return DifficultyLevel.valueOf(difficultyStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ArticleException(ArticleErrorCode.INVALID_DIFFICULTY_LEVEL);
        }
    }

    private void validatePaginationRequest(GetArticleChunksRequest request) {
        if (request.getLimit() != null && request.getLimit() > 100) {
            request.setLimit(100);
        }
    }

    public ArticleChunk findById(String chunkId) {
        return articleChunkRepository.findById(chunkId)
                .orElseThrow(() -> new ArticleException(ArticleErrorCode.CHUNK_NOT_FOUND));
    }

    public ArticleChunk findFirstByArticleId(String articleId) {
        return articleChunkRepository.findFirstByArticleIdOrderByChunkNumber(articleId)
                .orElseThrow(() -> new ArticleException(ArticleErrorCode.CHUNK_NOT_FOUND));
    }

    private ArticleChunkResponse convertToArticleChunkResponse(ArticleChunk chunk) {
        return ArticleChunkResponse.from(chunk);
    }
}