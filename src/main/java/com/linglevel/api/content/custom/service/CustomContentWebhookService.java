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
			ContentRequest contentRequest = contentRequestRepository.findForUpdateByRequestKey(request.getRequestId())
				.orElseThrow(() -> new CustomContentException(CustomContentErrorCode.CONTENT_REQUEST_NOT_FOUND));

			if (contentRequest.getStatus() == ContentRequestStatus.COMPLETED) {
				return CustomContentCompletedResponse.builder()
					.requestId(request.getRequestId())
					.status("completed")
					.build();
			}
			if (isTerminal(contentRequest)) {
				throw new CustomContentException(CustomContentErrorCode.AI_RESULT_PROCESSING_FAILED,
						"Request already closed");
			}

			AiResultDto aiResult = s3AiService.downloadJsonFile(request.getRequestId(), AiResultDto.class,
					pathStrategy);

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
			customContentReadingTimeService.updateReadingTime(savedContent.getId().toString());

			// 7. Update ContentRequest status
			contentRequest.setResultCustomContentId(savedContent.getId());
			contentRequest.setStatus(ContentRequestStatus.COMPLETED);
			contentRequest.setProgress(100);
			contentRequest.setCompletedAt(Instant.now());
			contentRequestRepository.save(contentRequest);
			ticketService.confirmReservation(contentRequest.getTicketReservationId().toString());

			// Publish notifications only after SQL commit.
			String contentId = savedContent.getId().toString();
			org.springframework.transaction.support.TransactionSynchronizationManager
				.registerSynchronization(new org.springframework.transaction.support.TransactionSynchronization() {
					@Override
					public void afterCommit() {
						try {
							notificationService.sendContentCompletedNotification(contentRequest.getUserId().toString(),
									request.getRequestId(), aiResult.getTitle(), contentId);
						}
						catch (Exception notificationError) {
							log.warn("Completion notification failed for {}", request.getRequestId(),
									notificationError);
						}
					}
				});

			log.info("Successfully processed AI result for request: {}", request.getRequestId());

			return CustomContentCompletedResponse.builder()
				.requestId(request.getRequestId())
				.status("completed")
				.build();

		}
		catch (Exception e) {
			log.error("Failed to process content completion for request: {}. Error: {}", request.getRequestId(),
					e.getMessage());

			// Handle failure case with proper exception handling
			// SQL state rolls back; retain the reservation so processing can be retried.

			throw new CustomContentException(CustomContentErrorCode.AI_RESULT_PROCESSING_FAILED, e.getMessage());
		}
	}

	@Transactional
	public void handleContentFailed(CustomContentFailedRequest request) {
		log.info("Handling content failure for request: {}", request.getRequestId());

		try {
			ContentRequest contentRequest = contentRequestRepository.findForUpdateByRequestKey(request.getRequestId())
				.orElseThrow(() -> new CustomContentException(CustomContentErrorCode.CONTENT_REQUEST_NOT_FOUND));

			if (isTerminal(contentRequest)) {
				return;
			}
			contentRequest.setStatus(ContentRequestStatus.FAILED);
			contentRequest.setErrorMessage(request.getErrorMessage());
			contentRequestRepository.save(contentRequest);

			ticketService.cancelReservation(contentRequest.getTicketReservationId().toString());

			String titleForNotification = StringUtils.hasText(contentRequest.getTitle()) ? contentRequest.getTitle()
					: "Untitled Content";

			notificationService.sendContentFailedNotification(contentRequest.getUserId().toString(),
					request.getRequestId(), titleForNotification, request.getErrorMessage());

			log.info("Updated request status to FAILED for request: {}", request.getRequestId());

		}
		catch (Exception e) {
			log.error("Failed to handle content failure for request: {}. Error: {}", request.getRequestId(),
					e.getMessage());
			throw new CustomContentException(CustomContentErrorCode.WEBHOOK_PROCESSING_FAILED, e.getMessage());
		}
	}

	@Transactional
	public void handleContentProgress(CustomContentProgressRequest request) {
		log.info("Handling content progress for request: {} - {}%", request.getRequestId(), request.getProgress());

		try {
			ContentRequest contentRequest = contentRequestRepository.findForUpdateByRequestKey(request.getRequestId())
				.orElseThrow(() -> new CustomContentException(CustomContentErrorCode.CONTENT_REQUEST_NOT_FOUND));

			if (isTerminal(contentRequest)) {
				return;
			}
			contentRequest.setStatus(ContentRequestStatus.PROCESSING);
			contentRequest
				.setProgress(Math.max(contentRequest.getProgress(), Math.min(100, Math.max(0, request.getProgress()))));
			contentRequestRepository.save(contentRequest);

			log.info("Updated progress to {}% for request: {}", request.getProgress(), request.getRequestId());

		}
		catch (Exception e) {
			log.error("Failed to handle content progress for request: {}. Error: {}", request.getRequestId(),
					e.getMessage());
			throw new CustomContentException(CustomContentErrorCode.WEBHOOK_PROCESSING_FAILED, e.getMessage());
		}
	}

	private void transferS3ImagesAndUpdateCoverUrl(String requestId, CustomContent customContent,
			AiResultDto aiResult) {
		try {
			s3TransferService.transferImagesFromAiToStatic(requestId, customContent.getId().toString(), pathStrategy);

			if (StringUtils.hasText(aiResult.getCoverImageUrl())) {
				String permanentCoverImageUrl = s3UrlService.getCoverImageUrl(customContent.getId().toString(),
						pathStrategy);
				customContent.setCoverImageUrl(permanentCoverImageUrl);
			}
		}
		catch (Exception e) {
			log.error("Failed to transfer S3 images for content: {}. Error: {}", customContent.getId().toString(),
					e.getMessage());
			throw new CustomContentException(CustomContentErrorCode.AI_RESULT_PROCESSING_FAILED,
					"Image transfer failed");
		}
	}

	private boolean isTerminal(ContentRequest request) {
		return request.getStatus() == ContentRequestStatus.COMPLETED
				|| request.getStatus() == ContentRequestStatus.FAILED
				|| request.getStatus() == ContentRequestStatus.DELETED;
	}

}
