package com.linglevel.api.content.custom.service;

import com.linglevel.api.common.dto.PageResponse;
import com.linglevel.api.content.custom.dto.*;
import com.linglevel.api.content.custom.exception.CustomContentErrorCode;
import com.linglevel.api.content.custom.exception.CustomContentException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomContentChunkService {

    public PageResponse<CustomContentChunkResponse> getCustomContentChunks(String userId, String customContentId, GetCustomContentChunksRequest request) {
        log.info("Getting custom content chunks for content {} and user: {}", customContentId, userId);
        throw new CustomContentException(CustomContentErrorCode.SERVICE_NOT_IMPLEMENTED);
    }

    public CustomContentChunkResponse getCustomContentChunk(String userId, String customContentId, String chunkId) {
        log.info("Getting custom content chunk {} from content {} for user: {}", chunkId, customContentId, userId);
        throw new CustomContentException(CustomContentErrorCode.SERVICE_NOT_IMPLEMENTED);
    }
}