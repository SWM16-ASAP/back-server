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
public class CustomContentRequestService {

    public CreateContentRequestResponse createContentRequest(String userId, CreateContentRequestRequest request) {
        log.info("Creating content request for user: {}", userId);
        throw new CustomContentException(CustomContentErrorCode.SERVICE_NOT_IMPLEMENTED);
    }

    public PageResponse<ContentRequestResponse> getContentRequests(String userId, GetContentRequestsRequest request) {
        log.info("Getting content requests for user: {}", userId);
        throw new CustomContentException(CustomContentErrorCode.SERVICE_NOT_IMPLEMENTED);
    }

    public ContentRequestResponse getContentRequest(String userId, String requestId) {
        log.info("Getting content request {} for user: {}", requestId, userId);
        throw new CustomContentException(CustomContentErrorCode.SERVICE_NOT_IMPLEMENTED);
    }
}