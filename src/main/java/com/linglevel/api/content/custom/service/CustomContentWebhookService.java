package com.linglevel.api.content.custom.service;

import com.linglevel.api.content.custom.dto.*;
import com.linglevel.api.content.custom.exception.CustomContentErrorCode;
import com.linglevel.api.content.custom.exception.CustomContentException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomContentWebhookService {

    public CustomContentCompletedResponse handleContentCompleted(CustomContentCompletedRequest request) {
        log.info("Handling content completion for request: {}", request.getRequestId());
        throw new CustomContentException(CustomContentErrorCode.SERVICE_NOT_IMPLEMENTED);
    }

    public void handleContentFailed(CustomContentFailedRequest request) {
        log.info("Handling content failure for request: {}", request.getRequestId());
        throw new CustomContentException(CustomContentErrorCode.SERVICE_NOT_IMPLEMENTED);
    }

    public void handleContentProgress(CustomContentProgressRequest request) {
        log.info("Handling content progress for request: {} - {}%", request.getRequestId(), request.getProgress());
        throw new CustomContentException(CustomContentErrorCode.SERVICE_NOT_IMPLEMENTED);
    }
}