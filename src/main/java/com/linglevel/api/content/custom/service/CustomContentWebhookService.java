package com.linglevel.api.content.custom.service;

import com.linglevel.api.content.custom.dto.*;
import com.linglevel.api.content.custom.entity.ContentRequest;
import com.linglevel.api.content.custom.entity.ContentRequestStatus;
import com.linglevel.api.content.custom.entity.CustomContent;
import com.linglevel.api.content.custom.exception.CustomContentErrorCode;
import com.linglevel.api.content.custom.exception.CustomContentException;
import com.linglevel.api.content.custom.repository.ContentRequestRepository;
import com.linglevel.api.content.custom.repository.CustomContentRepository;
import com.linglevel.api.s3.service.S3AiService;
import com.linglevel.api.s3.service.S3TransferService;
import com.linglevel.api.s3.service.S3UrlService;
import com.linglevel.api.s3.strategy.CustomContentPathStrategy;
import com.linglevel.api.user.ticket.service.TicketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomContentWebhookService {

    private final ContentRequestRepository contentRequestRepository;
    private final CustomContentRepository customContentRepository;
    private final UserCustomContentService userCustomContentService;
    private final CustomContentImportService customContentImportService;
    private final CustomContentReadingTimeService customContentReadingTimeService;
    private final S3AiService s3AiService;
    private final S3TransferService s3TransferService;
    private final S3UrlService s3UrlService;
    private final CustomContentPathStrategy pathStrategy;
    private final CustomContentNotificationService notificationService;
    private final TicketService ticketService;

    @Transactional
    public CustomContentCompletedResponse handleContentCompleted(CustomContentCompletedRequest request) {
        log.info("Handling content completion for request: {}", request.getRequestId());
        
        try {
            // 1. Get ContentRequest and AiResultDto
            ContentRequest contentRequest = contentRequestRepository.findById(request.getRequestId())
                    .orElseThrow(() -> new CustomContentException(CustomContentErrorCode.CONTENT_REQUEST_NOT_FOUND));
            
            AiResultDto aiResult = s3AiService.downloadJsonFile(
                    request.getRequestId(), 
                    AiResultDto.class, 
                    pathStrategy
            );
            
            // 2. Create the main CustomContent entity
            CustomContent savedContent = customContentImportService.createCustomContent(contentRequest, aiResult);

            // 3. Create UserCustomContent mapping for this user
            userCustomContentService.createMapping(contentRequest, savedContent);

            // 4. Transfer S3 images from AI temp location to static location
            transferS3ImagesAndUpdateCoverUrl(request.getRequestId(), savedContent, aiResult);

            // Save updated content with permanent cover image URL
            savedContent = customContentRepository.save(savedContent);

            // 5. Create all associated chunks with permanent image URLs
            customContentImportService.createCustomContentChunks(savedContent, aiResult);

            // 6. Calculate and update reading time
            customContentReadingTimeService.updateReadingTime(savedContent.getId());

            // 7. Update ContentRequest status
            contentRequest.setResultCustomContentId(savedContent.getId());
            contentRequest.setStatus(ContentRequestStatus.COMPLETED);
            contentRequest.setCompletedAt(Instant.now());
            contentRequestRepository.save(contentRequest);

            // 8. Send notification
            notificationService.sendContentCompletedNotification(
                    contentRequest.getUserId(),
                    request.getRequestId(),
                    aiResult.getTitle(),
                    savedContent.getId()
            );
            
            log.info("Successfully processed AI result for request: {}", request.getRequestId());
            
            return CustomContentCompletedResponse.builder()
                    .requestId(request.getRequestId())
                    .status("completed")
                    .build();
                    
        } catch (Exception e) {
            log.error("Failed to process content completion for request: {}. Error: {}", request.getRequestId(), e.getMessage());
            
            // Handle failure case with proper exception handling
            handleContentProcessingFailure(request.getRequestId(), e);
            
            throw new CustomContentException(CustomContentErrorCode.AI_RESULT_PROCESSING_FAILED, e.getMessage());
        }
    }

    @Transactional
    public void handleContentFailed(CustomContentFailedRequest request) {
        log.info("Handling content failure for request: {}", request.getRequestId());
        
        try {
            ContentRequest contentRequest = contentRequestRepository.findById(request.getRequestId())
                    .orElseThrow(() -> new CustomContentException(CustomContentErrorCode.CONTENT_REQUEST_NOT_FOUND));
            
            contentRequest.setStatus(ContentRequestStatus.FAILED);
            contentRequest.setErrorMessage(request.getErrorMessage());
            contentRequestRepository.save(contentRequest);

            // 티켓 복원 (1개 환불)
            try {
                ticketService.grantTicket(contentRequest.getUserId(), 1, "Content creation failed - refund");
                log.info("Ticket refunded for failed request: {}", request.getRequestId());
            } catch (Exception ticketE) {
                log.error("Failed to refund ticket for request: {}. Error: {}", request.getRequestId(), ticketE.getMessage());
            }

            notificationService.sendContentFailedNotification(
                    contentRequest.getUserId(),
                    request.getRequestId(),
                    contentRequest.getTitle(),
                    request.getErrorMessage()
            );

            log.info("Updated request status to FAILED for request: {}", request.getRequestId());
            
        } catch (Exception e) {
            log.error("Failed to handle content failure for request: {}. Error: {}", request.getRequestId(), e.getMessage());
            throw new CustomContentException(CustomContentErrorCode.WEBHOOK_PROCESSING_FAILED, e.getMessage());
        }
    }

    @Transactional
    public void handleContentProgress(CustomContentProgressRequest request) {
        log.info("Handling content progress for request: {} - {}%", request.getRequestId(), request.getProgress());
        
        try {
            ContentRequest contentRequest = contentRequestRepository.findById(request.getRequestId())
                    .orElseThrow(() -> new CustomContentException(CustomContentErrorCode.CONTENT_REQUEST_NOT_FOUND));
            
            contentRequest.setStatus(ContentRequestStatus.PROCESSING);
            contentRequest.setProgress(request.getProgress());
            contentRequestRepository.save(contentRequest);
            
            log.info("Updated progress to {}% for request: {}", request.getProgress(), request.getRequestId());
            
        } catch (Exception e) {
            log.error("Failed to handle content progress for request: {}. Error: {}", request.getRequestId(), e.getMessage());
            throw new CustomContentException(CustomContentErrorCode.WEBHOOK_PROCESSING_FAILED, e.getMessage());
        }
    }

    private void transferS3ImagesAndUpdateCoverUrl(String requestId, CustomContent customContent, AiResultDto aiResult) {
        try {
            s3TransferService.transferImagesFromAiToStatic(requestId, customContent.getId(), pathStrategy);
            
            if (StringUtils.hasText(aiResult.getCoverImageUrl())) {
                String permanentCoverImageUrl = s3UrlService.getCoverImageUrl(customContent.getId(), pathStrategy);
                customContent.setCoverImageUrl(permanentCoverImageUrl);
            }
        } catch (Exception e) {
            log.error("Failed to transfer S3 images for content: {}. Error: {}", customContent.getId(), e.getMessage());
            // Don't fail the entire process for S3 transfer issues
        }
    }
    
    private void handleContentProcessingFailure(String requestId, Exception originalException) {
        try {
            ContentRequest contentRequest = contentRequestRepository.findById(requestId)
                    .orElse(null);
            if (contentRequest != null) {
                contentRequest.setStatus(ContentRequestStatus.FAILED);
                contentRequest.setErrorMessage("AI 결과 처리 실패: " + originalException.getMessage());
                contentRequestRepository.save(contentRequest);

                // 티켓 복원 (1개 환불)
                try {
                    ticketService.grantTicket(contentRequest.getUserId(), 1, "Content processing failed - refund");
                    log.info("Ticket refunded for processing failure: {}", requestId);
                } catch (Exception ticketE) {
                    log.error("Failed to refund ticket for processing failure: {}. Error: {}", requestId, ticketE.getMessage());
                }
            }
        } catch (Exception e) {
            log.error("Failed to update content request status to FAILED for request: {}. Error: {}", requestId, e.getMessage());
            // Don't throw exception here to preserve original exception
        }
    }
}
