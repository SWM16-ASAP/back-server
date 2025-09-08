package com.linglevel.api.content.custom.service;

import com.linglevel.api.common.dto.PageResponse;
import com.linglevel.api.content.custom.dto.*;
import com.linglevel.api.content.custom.entity.ContentRequest;
import com.linglevel.api.content.custom.entity.ContentRequestStatus;
import com.linglevel.api.content.custom.exception.CustomContentErrorCode;
import com.linglevel.api.content.custom.exception.CustomContentException;
import com.linglevel.api.content.custom.repository.ContentRequestRepository;
import com.linglevel.api.s3.service.S3AiService;
import com.linglevel.api.s3.strategy.CustomContentPathStrategy;
import com.linglevel.api.user.entity.User;
import com.linglevel.api.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomContentRequestService {

    private final ContentRequestRepository contentRequestRepository;
    private final UserRepository userRepository;
    private final S3AiService s3AiService;
    private final CustomContentPathStrategy pathStrategy;

    public CreateContentRequestResponse createContentRequest(String username, CreateContentRequestRequest request) {
        log.info("Creating content request for user: {}", username);
        
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new CustomContentException(CustomContentErrorCode.USER_NOT_FOUND));
        
        ContentRequest contentRequest = ContentRequest.builder()
                .userId(user.getId())
                .title(request.getTitle())
                .contentType(request.getContentType())
                .targetDifficultyLevels(request.getTargetDifficultyLevels())
                .originUrl(request.getOriginUrl())
                .originAuthor(request.getOriginAuthor())
                .status(ContentRequestStatus.PENDING)
                .progress(0)
                .build();

        ContentRequest savedRequest = contentRequestRepository.save(contentRequest);
        log.info("Content request created with ID: {}", savedRequest.getId());

        uploadToAiInput(savedRequest, request);

        return CreateContentRequestResponse.builder()
                .requestId(savedRequest.getId())
                .title(savedRequest.getTitle())
                .status(savedRequest.getStatus().getCode())
                .createdAt(savedRequest.getCreatedAt())
                .build();
    }

    private void uploadToAiInput(ContentRequest contentRequest, CreateContentRequestRequest request) {
        try {
            Map<String, Object> aiInputData = new HashMap<>();
            aiInputData.put("type", "custom");
            aiInputData.put("content", request.getOriginalContent());

            s3AiService.uploadJsonToInputBucket(contentRequest.getId(), aiInputData, pathStrategy);
            log.info("Successfully uploaded AI input data for request: {}", contentRequest.getId());
            
        } catch (Exception e) {
            log.error("Failed to upload AI input data for request: {}", contentRequest.getId(), e);
            contentRequest.setStatus(ContentRequestStatus.FAILED);
            contentRequest.setErrorMessage("AI 입력 데이터 업로드 실패: " + e.getMessage());
            contentRequestRepository.save(contentRequest);
            throw new CustomContentException(CustomContentErrorCode.AI_INPUT_UPLOAD_FAILED);
        }
    }

    public PageResponse<ContentRequestResponse> getContentRequests(String username, GetContentRequestsRequest request) {
        log.info("Getting content requests for user: {}", username);
        
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new CustomContentException(CustomContentErrorCode.USER_NOT_FOUND));
        
        int limit = Math.min(request.getLimit(), 100);
        Pageable pageable = PageRequest.of(
                request.getPage() - 1, 
                limit,
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<ContentRequest> contentRequests;
        if (request.getStatus() != null) {
            ContentRequestStatus status = ContentRequestStatus.valueOf(request.getStatus().toUpperCase());
            contentRequests = contentRequestRepository.findByUserIdAndStatus(user.getId(), status, pageable);
        } else {
            contentRequests = contentRequestRepository.findByUserIdAndStatusNot(
                    user.getId(), ContentRequestStatus.DELETED, pageable);
        }

        Page<ContentRequestResponse> responsePage = contentRequests.map(this::mapToResponse);
        return new PageResponse<>(responsePage.getContent(), responsePage);
    }

    public ContentRequestResponse getContentRequest(String username, String requestId) {
        log.info("Getting content request {} for user: {}", requestId, username);
        
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new CustomContentException(CustomContentErrorCode.USER_NOT_FOUND));
        
        ContentRequest contentRequest = contentRequestRepository.findByIdAndUserId(requestId, user.getId())
                .orElseThrow(() -> new CustomContentException(CustomContentErrorCode.CONTENT_REQUEST_NOT_FOUND));

        return mapToResponse(contentRequest);
    }

    private ContentRequestResponse mapToResponse(ContentRequest contentRequest) {
        ContentRequestResponse response = new ContentRequestResponse();
        response.setId(contentRequest.getId());
        response.setTitle(contentRequest.getTitle());
        response.setContentType(contentRequest.getContentType().getCode());
        response.setTargetDifficultyLevels(contentRequest.getTargetDifficultyLevels());
        response.setOriginUrl(contentRequest.getOriginUrl());
        response.setOriginDomain(contentRequest.getOriginDomain());
        response.setOriginAuthor(contentRequest.getOriginAuthor());
        response.setStatus(contentRequest.getStatus().getCode());
        response.setProgress(contentRequest.getProgress());
        response.setCreatedAt(contentRequest.getCreatedAt());
        response.setCompletedAt(contentRequest.getCompletedAt());
        response.setErrorMessage(contentRequest.getErrorMessage());
        response.setResultCustomContentId(contentRequest.getResultCustomContentId());
        return response;
    }
}