package com.linglevel.api.content.custom.service;

import com.linglevel.api.common.dto.PageResponse;
import com.linglevel.api.content.custom.dto.*;
import com.linglevel.api.content.custom.entity.ContentRequest;
import com.linglevel.api.content.custom.entity.ContentRequestStatus;
import com.linglevel.api.content.custom.exception.CustomContentErrorCode;
import com.linglevel.api.content.custom.exception.CustomContentException;
import com.linglevel.api.content.custom.repository.ContentRequestRepository;
import com.linglevel.api.crawling.service.CrawlingService;
import com.linglevel.api.s3.service.S3AiService;
import com.linglevel.api.s3.strategy.CustomContentPathStrategy;
import com.linglevel.api.user.entity.User;

import com.linglevel.api.user.ticket.service.TicketService;
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

    private final S3AiService s3AiService;
    private final CustomContentPathStrategy pathStrategy;
    private final CrawlingService crawlingService;
    private final TicketService ticketService;

    public CreateContentRequestResponse createContentRequest(String userId, CreateContentRequestRequest request) {
        log.info("Creating content request for user: {}", userId);

        String extractedDomain = validateUrlForCrawling(request.getOriginUrl());

        // 🎫 티켓 소비 (1개 티켓 필요)
        try {
            ticketService.spendTicket(userId, 1, "Custom content creation");
            log.info("Ticket spent for user: {} (Custom content: {})", userId, request.getTitle());
        } catch (Exception e) {
            log.info("Failed to spend ticket for user: {}", userId, e);
            throw new CustomContentException(CustomContentErrorCode.INSUFFICIENT_TICKETS);
        }

        ContentRequest contentRequest = ContentRequest.builder()
                .userId(userId)
                .title(request.getTitle())
                .contentType(request.getContentType())
                .originalText(request.getOriginalContent())
                .targetDifficultyLevels(request.getTargetDifficultyLevels())
                .originUrl(request.getOriginUrl())
                .originDomain(extractedDomain)
                .originAuthor(request.getOriginAuthor())
                .coverImageUrl(request.getCoverImageUrl())
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

    private String validateUrlForCrawling(String originUrl) {
        try {
            // URL 형식 및 크롤링 가능 여부 검증
            if (!crawlingService.isValidUrl(originUrl)) {
                throw new CustomContentException(CustomContentErrorCode.INVALID_URL_FORMAT);
            }

            // DSL 존재 여부 확인 (크롤링 가능한 도메인인지 검증)
            var lookupResult = crawlingService.lookupDsl(originUrl, true);
            if (!lookupResult.isValid()) {
                throw new CustomContentException(CustomContentErrorCode.URL_NOT_SUPPORTED);
            }

            // 도메인 반환
            return lookupResult.getDomain();

        } catch (com.linglevel.api.crawling.exception.CrawlingException e) {
            return null;
        }
    }

    private void uploadToAiInput(ContentRequest contentRequest, CreateContentRequestRequest request) {
        try {
            Map<String, Object> aiInputData = new HashMap<>();
            aiInputData.put("type", "custom");
            aiInputData.put("content", request.getOriginalContent());
            if (request.getCoverImageUrl() != null) {
                aiInputData.put("coverImageUrl", request.getCoverImageUrl());
            }

            s3AiService.uploadJsonToInputBucket(contentRequest.getId(), aiInputData, pathStrategy);
            log.info("Successfully uploaded AI input data for request: {}", contentRequest.getId());
            
        } catch (Exception e) {
            log.error("Failed to upload AI input data for request: {}", contentRequest.getId(), e);
            contentRequest.setStatus(ContentRequestStatus.FAILED);
            contentRequest.setErrorMessage("AI 입력 데이터 업로드 실패: " + e.getMessage());
            contentRequestRepository.save(contentRequest);

            // AI 입력 업로드 실패 시 티켓 복원
            try {
                ticketService.grantTicket(contentRequest.getUserId(), 1, "Content creation failed - refund");
                log.info("Ticket refunded for failed request: {}", contentRequest.getId());
            } catch (Exception ticketE) {
                log.error("Failed to refund ticket for request: {}", contentRequest.getId(), ticketE);
            }
            
            throw new CustomContentException(CustomContentErrorCode.AI_INPUT_UPLOAD_FAILED);
        }
    }

    public PageResponse<ContentRequestResponse> getContentRequests(String userId, GetContentRequestsRequest request) {
        log.info("Getting content requests for user: {}", userId);
        
        Pageable pageable = PageRequest.of(
                request.getPage() - 1,
                request.getLimit(),
                Sort.by(Sort.Direction.DESC, "createdAt")
        );

        Page<ContentRequest> contentRequests;
        if (request.getStatus() != null) {
            ContentRequestStatus status = ContentRequestStatus.valueOf(request.getStatus().toUpperCase());
            contentRequests = contentRequestRepository.findByUserIdAndStatus(userId, status, pageable);
        } else {
            contentRequests = contentRequestRepository.findByUserIdAndStatusNot(
                    userId, ContentRequestStatus.DELETED, pageable);
        }

        Page<ContentRequestResponse> responsePage = contentRequests.map(this::mapToResponse);
        return new PageResponse<>(responsePage.getContent(), responsePage);
    }

    public ContentRequestResponse getContentRequest(String userId, String requestId) {
        log.info("Getting content request {} for user: {}", requestId, userId);
        
        ContentRequest contentRequest = contentRequestRepository.findByIdAndUserId(requestId, userId)
                .orElseThrow(() -> new CustomContentException(CustomContentErrorCode.CONTENT_REQUEST_NOT_FOUND));

        return mapToResponse(contentRequest);
    }

    private ContentRequestResponse mapToResponse(ContentRequest contentRequest) {
        ContentRequestResponse response = new ContentRequestResponse();
        response.setId(contentRequest.getId());
        response.setTitle(contentRequest.getTitle());
        response.setOriginalText(contentRequest.getOriginalText());
        response.setContentType(contentRequest.getContentType().getCode());
        response.setTargetDifficultyLevels(contentRequest.getTargetDifficultyLevels());
        response.setOriginUrl(contentRequest.getOriginUrl());
        response.setOriginDomain(contentRequest.getOriginDomain());
        response.setOriginAuthor(contentRequest.getOriginAuthor());
        response.setCoverImageUrl(contentRequest.getCoverImageUrl());
        response.setStatus(contentRequest.getStatus().getCode());
        response.setProgress(contentRequest.getProgress());
        response.setCreatedAt(contentRequest.getCreatedAt());
        response.setCompletedAt(contentRequest.getCompletedAt());
        response.setErrorMessage(contentRequest.getErrorMessage());
        response.setResultCustomContentId(contentRequest.getResultCustomContentId());
        return response;
    }
}