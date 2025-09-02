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
public class CustomContentService {

    public PageResponse<CustomContentResponse> getCustomContents(String userId, GetCustomContentsRequest request) {
        log.info("Getting custom contents for user: {}", userId);
        throw new CustomContentException(CustomContentErrorCode.SERVICE_NOT_IMPLEMENTED);
    }

    public CustomContentResponse getCustomContent(String userId, String customContentId) {
        log.info("Getting custom content {} for user: {}", customContentId, userId);
        throw new CustomContentException(CustomContentErrorCode.SERVICE_NOT_IMPLEMENTED);
    }

    public void deleteCustomContent(String userId, String customContentId) {
        log.info("Deleting custom content {} for user: {}", customContentId, userId);
        throw new CustomContentException(CustomContentErrorCode.SERVICE_NOT_IMPLEMENTED);
    }
}