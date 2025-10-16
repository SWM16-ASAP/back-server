package com.linglevel.api.content.custom.service;

import com.linglevel.api.common.dto.PageResponse;
import com.linglevel.api.content.common.DifficultyLevel;
import com.linglevel.api.content.custom.dto.*;
import com.linglevel.api.content.custom.entity.CustomContentChunk;
import com.linglevel.api.content.custom.exception.CustomContentErrorCode;
import com.linglevel.api.content.custom.exception.CustomContentException;
import com.linglevel.api.content.custom.repository.CustomContentChunkRepository;
import com.linglevel.api.content.custom.repository.CustomContentRepository;
import com.linglevel.api.user.entity.User;
import com.linglevel.api.user.repository.UserRepository;
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
public class CustomContentChunkService {

    private final CustomContentChunkRepository customContentChunkRepository;
    private final CustomContentRepository customContentRepository;
    private final UserRepository userRepository;

    public PageResponse<CustomContentChunkResponse> getCustomContentChunks(String userId, String customContentId, GetCustomContentChunksRequest request) {
        log.info("Getting custom content chunks for content {} and user: {}", customContentId, userId);

        validateCustomContentAccess(customContentId, userId);

        customContentRepository.incrementViewCount(customContentId);

        DifficultyLevel difficulty = request.getDifficultyLevel();

        validatePaginationRequest(request);
        Pageable pageable = PageRequest.of(request.getPage() - 1, request.getLimit());

        Page<CustomContentChunk> chunksPage = customContentChunkRepository
                .findByCustomContentIdAndDifficultyLevelAndIsDeletedFalseOrderByChapterNumAscChunkNumAsc(
                        customContentId, difficulty, pageable);
        
        List<CustomContentChunkResponse> chunkResponses = chunksPage.getContent().stream()
                .map(this::convertToCustomContentChunkResponse)
                .toList();
        
        return PageResponse.of(chunksPage, chunkResponses);
    }

    public CustomContentChunkResponse getCustomContentChunk(String userId, String customContentId, String chunkId) {
        log.info("Getting custom content chunk {} from content {} for user: {}", chunkId, customContentId, userId);

        validateCustomContentAccess(customContentId, userId);

        CustomContentChunk chunk = customContentChunkRepository.findByIdAndCustomContentIdAndIsDeletedFalse(chunkId, customContentId)
                .orElseThrow(() -> new CustomContentException(CustomContentErrorCode.CUSTOM_CONTENT_CHUNK_NOT_FOUND));
        
        return convertToCustomContentChunkResponse(chunk);
    }

    private void validateCustomContentAccess(String customContentId, String userId) {
        customContentRepository.findByIdAndUserIdAndIsDeletedFalse(customContentId, userId)
                .orElseThrow(() -> new CustomContentException(CustomContentErrorCode.CUSTOM_CONTENT_NOT_FOUND));
    }


    private void validatePaginationRequest(GetCustomContentChunksRequest request) {
        if (request.getLimit() != null && request.getLimit() > 100) {
            request.setLimit(100);
        }
    }

    public CustomContentChunk findById(String chunkId) {
        return customContentChunkRepository.findById(chunkId)
                .orElseThrow(() -> new CustomContentException(CustomContentErrorCode.CUSTOM_CONTENT_CHUNK_NOT_FOUND));
    }

    public CustomContentChunk findFirstByCustomContentId(String customContentId) {
        return customContentChunkRepository.findFirstByCustomContentIdAndIsDeletedFalseOrderByChapterNumAscChunkNumAsc(customContentId)
                .orElseThrow(() -> new CustomContentException(CustomContentErrorCode.CUSTOM_CONTENT_CHUNK_NOT_FOUND));
    }

    private CustomContentChunkResponse convertToCustomContentChunkResponse(CustomContentChunk chunk) {
        return CustomContentChunkResponse.from(chunk);
    }
}