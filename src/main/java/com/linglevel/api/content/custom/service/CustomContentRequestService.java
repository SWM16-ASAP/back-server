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
import com.linglevel.api.user.repository.UserRepository;
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
    private final UserRepository userRepository;
    private final S3AiService s3AiService;
    private final CustomContentPathStrategy pathStrategy;
    private final CrawlingService crawlingService;
    private final TicketService ticketService;

    public CreateContentRequestResponse createContentRequest(String username, CreateContentRequestRequest request) {
        log.info("Creating content request for user: {}", username);
        
        // URL 유효성 검증 (LINK 타입인 경우)
        if (request.getContentType() == com.linglevel.api.content.custom.entity.ContentType.LINK) {
            validateUrlForCrawling(request.getOriginUrl());
        }
        
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new CustomContentException(CustomContentErrorCode.USER_NOT_FOUND));
        
        // 🎫 티켓 예약 (1개 티켓 필요)
        String reservationId;
        try {
            reservationId = ticketService.reserveTicket(user.getId(), 1, "Custom content creation: " + request.getTitle());
            log.info("Ticket reserved for user: {} (Custom content: {})", user.getId(), request.getTitle());
        } catch (Exception e) {
            log.info("Failed to reserve ticket for user: {}", user.getId(), e);
            throw new CustomContentException(CustomContentErrorCode.INSUFFICIENT_TICKETS);
        }
        
        ContentRequest contentRequest = ContentRequest.builder()
                .userId(user.getId())
                .title(request.getTitle())
                .contentType(request.getContentType())
                .originalText(request.getOriginalContent())
                .targetDifficultyLevels(request.getTargetDifficultyLevels())
                .originUrl(request.getOriginUrl())
                .originAuthor(request.getOriginAuthor())
                .status(ContentRequestStatus.PENDING)
                .progress(0)
                .build();

        ContentRequest savedRequest = contentRequestRepository.save(contentRequest);
        log.info("Content request created with ID: {}", savedRequest.getId());

        uploadToAiInput(savedRequest, request, reservationId);

        return CreateContentRequestResponse.builder()
                .requestId(savedRequest.getId())
                .title(savedRequest.getTitle())
                .status(savedRequest.getStatus().getCode())
                .createdAt(savedRequest.getCreatedAt())
                .build();
    }

    private void validateUrlForCrawling(String originUrl) {
        if (originUrl == null || originUrl.trim().isEmpty()) {
            throw new CustomContentException(CustomContentErrorCode.URL_REQUIRED);
        }
        
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
            
        } catch (com.linglevel.api.crawling.exception.CrawlingException e) {
            // CrawlingException을 CustomContentException으로 변환
            throw new CustomContentException(CustomContentErrorCode.INVALID_REQUEST, e.getMessage());
        }
    }

    private void uploadToAiInput(ContentRequest contentRequest, CreateContentRequestRequest request, String reservationId) {
        try {
            Map<String, Object> aiInputData = new HashMap<>();
            aiInputData.put("type", "custom");
            aiInputData.put("content", request.getOriginalContent());

            s3AiService.uploadJsonToInputBucket(contentRequest.getId(), aiInputData, pathStrategy);
            log.info("Successfully uploaded AI input data for request: {}", contentRequest.getId());
            
            // AI 입력 업로드 성공 시 티켓 예약 확정
            ticketService.confirmReservation(reservationId);
            log.info("Ticket reservation confirmed for request: {}", contentRequest.getId());
            
        } catch (Exception e) {
            log.error("Failed to upload AI input data for request: {}", contentRequest.getId(), e);
            contentRequest.setStatus(ContentRequestStatus.FAILED);
            contentRequest.setErrorMessage("AI 입력 데이터 업로드 실패: " + e.getMessage());
            contentRequestRepository.save(contentRequest);
            
            // AI 입력 업로드 실패 시 티켓 예약 취소 (복구)
            try {
                ticketService.cancelReservation(reservationId);
                log.info("Ticket reservation cancelled for failed request: {}", contentRequest.getId());
            } catch (Exception ticketE) {
                log.error("Failed to cancel ticket reservation for request: {}", contentRequest.getId(), ticketE);
            }
            
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
        response.setOriginalText(contentRequest.getOriginalText());
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