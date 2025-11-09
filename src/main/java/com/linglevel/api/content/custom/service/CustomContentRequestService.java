package com.linglevel.api.content.custom.service;

import com.linglevel.api.common.dto.PageResponse;
import com.linglevel.api.common.util.UrlNormalizer;
import com.linglevel.api.content.custom.dto.ContentRequestResponse;
import com.linglevel.api.content.custom.dto.CreateContentRequestRequest;
import com.linglevel.api.content.custom.dto.CreateContentRequestResponse;
import com.linglevel.api.content.custom.dto.GetContentRequestsRequest;
import com.linglevel.api.content.custom.entity.ContentRequest;
import com.linglevel.api.content.custom.entity.ContentRequestStatus;
import com.linglevel.api.content.custom.entity.ContentType;
import com.linglevel.api.content.custom.entity.CustomContent;
import com.linglevel.api.content.custom.exception.CustomContentErrorCode;
import com.linglevel.api.content.custom.exception.CustomContentException;
import com.linglevel.api.content.custom.repository.ContentRequestRepository;
import com.linglevel.api.content.custom.repository.CustomContentRepository;
import com.linglevel.api.crawling.service.CrawlingService;
import com.linglevel.api.s3.service.S3AiService;
import com.linglevel.api.s3.strategy.CustomContentPathStrategy;
import com.linglevel.api.user.ticket.service.TicketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomContentRequestService {

    private final ContentRequestRepository contentRequestRepository;
    private final CustomContentRepository customContentRepository;
    private final UserCustomContentService userCustomContentService;
    private final S3AiService s3AiService;
    private final CustomContentPathStrategy pathStrategy;
    private final CrawlingService crawlingService;
    private final TicketService ticketService;

    @Transactional
    public CreateContentRequestResponse createContentRequest(String userId, CreateContentRequestRequest request) {
        log.info("Creating content request for user: {}", userId);

        String extractedDomain = validateUrlForCrawling(request.getOriginUrl());

        // URL 기반 캐시 검사 (LINK, YOUTUBE만 해당)
        Optional<CustomContent> cachedContent = Optional.empty();
        if (isUrlBasedContentType(request.getContentType())) {
            cachedContent = checkCachedContent(request.getOriginUrl());
            if (cachedContent.isPresent()) {
                if (!userCustomContentService.validateNotOwned(userId, cachedContent.get().getId())) {
                   throw new CustomContentException(CustomContentErrorCode.CONTENT_ALREADY_OWNED);
                }
            }
        }

        // 티켓 소비 (캐시 히트/미스 무관하게 소비, 단 이미 소유한 경우는 제외)
        try {
            ticketService.spendTicket(userId, 1, "Custom content creation");
            log.info("Ticket spent for user: {} (Custom content: {})", userId, request.getTitle());
        } catch (Exception e) {
            log.error("Failed to spend ticket for user: {}", userId, e);
            throw new CustomContentException(CustomContentErrorCode.INSUFFICIENT_TICKETS);
        }

        // ContentRequest 생성
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

        // 캐시 히트 시: 즉시 완료 처리
        if (cachedContent.isPresent()) {
            return handleCacheHit(savedRequest, cachedContent.get());
        }

        // 캐시 미스 시: AI 처리 진행
        uploadToAiInput(savedRequest, request);

        return CreateContentRequestResponse.builder()
                .requestId(savedRequest.getId())
                .title(savedRequest.getTitle())
                .status(savedRequest.getStatus().getCode())
                .cached(false)
                .createdAt(savedRequest.getCreatedAt())
                .build();
    }

    private Optional<CustomContent> checkCachedContent(String originUrl) {
        if (!StringUtils.hasText(originUrl)) {
            return Optional.empty();
        }

        String normalizedUrl = UrlNormalizer.normalize(originUrl);
        Optional<CustomContent> existingContent = customContentRepository.findByOriginUrlAndIsDeletedFalse(normalizedUrl);

        if (existingContent.isPresent()) {
            log.info("Cache HIT: Found existing content for URL: {} -> Content ID: {}",
                    normalizedUrl, existingContent.get().getId());
        } else {
            log.debug("Cache MISS: No existing content for URL: {}", normalizedUrl);
        }

        return existingContent;
    }

    private CreateContentRequestResponse handleCacheHit(ContentRequest contentRequest, CustomContent cachedContent) {
        // 1. UserCustomContent 매핑 생성
        userCustomContentService.createMapping(contentRequest, cachedContent);

        // 2. ContentRequest 즉시 완료 처리
        contentRequest.setResultCustomContentId(cachedContent.getId());
        contentRequest.setStatus(ContentRequestStatus.COMPLETED);
        contentRequest.setProgress(100);
        contentRequest.setCompletedAt(Instant.now());
        contentRequestRepository.save(contentRequest);

        return CreateContentRequestResponse.builder()
                .requestId(contentRequest.getId())
                .title(contentRequest.getTitle())
                .status(ContentRequestStatus.COMPLETED.getCode())
                .cached(true)
                .customContentId(cachedContent.getId())
                .customContentTitle(cachedContent.getTitle())
                .createdAt(contentRequest.getCreatedAt())
                .build();
    }

    /**
     * URL 기반 캐싱이 가능한 ContentType인지 확인
     * TEXT, PDF는 사용자가 직접 입력한 고유 콘텐츠이므로 캐싱 불가
     */
    private boolean isUrlBasedContentType(ContentType contentType) {
        return contentType == ContentType.LINK || contentType == ContentType.YOUTUBE;
    }

    private String validateUrlForCrawling(String originUrl) {
        return crawlingService.extractDomain(originUrl);
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