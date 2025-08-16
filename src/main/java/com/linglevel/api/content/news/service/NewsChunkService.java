package com.linglevel.api.content.news.service;

import com.linglevel.api.content.common.DifficultyLevel;
import com.linglevel.api.content.news.dto.*;
import com.linglevel.api.content.news.entity.NewsChunk;
import com.linglevel.api.content.news.exception.NewsErrorCode;
import com.linglevel.api.content.news.exception.NewsException;
import com.linglevel.api.content.news.repository.NewsChunkRepository;
import com.linglevel.api.content.news.repository.NewsRepository;
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
public class NewsChunkService {

    private final NewsChunkRepository newsChunkRepository;
    private final NewsRepository newsRepository;

    public PageResponse<NewsChunkResponse> getNewsChunks(String newsId, GetNewsChunksRequest request) {
        validateNewsExists(newsId);

        DifficultyLevel difficulty = validateAndParseDifficulty(request.getDifficulty());

        validatePaginationRequest(request);
        Pageable pageable = PageRequest.of(request.getPage() - 1, request.getLimit());

        Page<NewsChunk> chunksPage = newsChunkRepository.findByNewsIdAndDifficultyOrderByChunkNumber(
                newsId, difficulty, pageable);
        
        List<NewsChunkResponse> chunkResponses = chunksPage.getContent().stream()
                .map(this::convertToNewsChunkResponse)
                .toList();
        
        return PageResponse.of(chunksPage, chunkResponses);
    }

    public NewsChunkResponse getNewsChunk(String newsId, String chunkId) {
        validateNewsExists(newsId);

        NewsChunk chunk = newsChunkRepository.findByNewsIdAndId(newsId, chunkId)
                .orElseThrow(() -> new NewsException(NewsErrorCode.CHUNK_NOT_FOUND));
        
        return convertToNewsChunkResponse(chunk);
    }

    private void validateNewsExists(String newsId) {
        if (!newsRepository.existsById(newsId)) {
            throw new NewsException(NewsErrorCode.NEWS_NOT_FOUND);
        }
    }

    private DifficultyLevel validateAndParseDifficulty(String difficultyStr) {
        if (difficultyStr == null || difficultyStr.trim().isEmpty()) {
            throw new NewsException(NewsErrorCode.INVALID_DIFFICULTY_LEVEL);
        }
        
        try {
            return DifficultyLevel.valueOf(difficultyStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new NewsException(NewsErrorCode.INVALID_DIFFICULTY_LEVEL);
        }
    }

    private void validatePaginationRequest(GetNewsChunksRequest request) {
        if (request.getLimit() != null && request.getLimit() > 50) {
            request.setLimit(50);
        }
    }

    private NewsChunkResponse convertToNewsChunkResponse(NewsChunk chunk) {
        NewsChunkResponse response = new NewsChunkResponse();
        response.setId(chunk.getId());
        response.setChunkNumber(chunk.getChunkNumber());
        response.setDifficulty(chunk.getDifficulty());
        response.setType(chunk.getType());
        response.setContent(chunk.getContent());
        response.setDescription(chunk.getDescription());
        return response;
    }
}