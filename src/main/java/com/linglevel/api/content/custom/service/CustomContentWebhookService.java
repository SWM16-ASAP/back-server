package com.linglevel.api.content.custom.service;

import com.linglevel.api.content.common.DifficultyLevel;
import com.linglevel.api.content.custom.dto.*;
import com.linglevel.api.content.custom.entity.*;
import com.linglevel.api.content.custom.exception.CustomContentErrorCode;
import com.linglevel.api.content.custom.exception.CustomContentException;
import com.linglevel.api.content.custom.repository.*;
import com.linglevel.api.s3.service.S3AiService;
import com.linglevel.api.s3.strategy.CustomContentPathStrategy;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomContentWebhookService {

    private final ContentRequestRepository contentRequestRepository;
    private final CustomContentRepository customContentRepository;
    private final CustomContentChunkRepository customContentChunkRepository;
    private final S3AiService s3AiService;
    private final CustomContentPathStrategy pathStrategy;
    private final CustomContentNotificationService notificationService;

    @Transactional
    public CustomContentCompletedResponse handleContentCompleted(CustomContentCompletedRequest request) {
        log.info("Handling content completion for request: {}", request.getRequestId());
        
        try {
            // 1. ContentRequest 조회
            ContentRequest contentRequest = contentRequestRepository.findById(request.getRequestId())
                    .orElseThrow(() -> new CustomContentException(CustomContentErrorCode.CONTENT_REQUEST_NOT_FOUND));
            
            // 2. S3에서 AI 결과 JSON 다운로드
            AiResultDto aiResult = s3AiService.downloadJsonFile(
                    request.getRequestId(), 
                    AiResultDto.class, 
                    pathStrategy
            );
            
            // 3. CustomContent 및 CustomContentChunk 엔티티 생성
            List<CustomContent> customContents = createCustomContentsFromAiResult(contentRequest, aiResult);
            
            // 4. 데이터베이스 저장
            customContentRepository.saveAll(customContents);
            
            // 5. ContentRequest 상태 업데이트
            contentRequest.setStatus(ContentRequestStatus.COMPLETED);
            contentRequest.setCompletedAt(LocalDateTime.now());
            if (!customContents.isEmpty()) {
                contentRequest.setResultCustomContentId(customContents.get(0).getId());
            }
            contentRequestRepository.save(contentRequest);
            
            // 6. FCM 알림 전송
            notificationService.sendContentCompletedNotification(
                    contentRequest.getUserId(),
                    request.getRequestId(),
                    aiResult.getTitle()
            );
            
            log.info("Successfully processed AI result for request: {}", request.getRequestId());
            
            return CustomContentCompletedResponse.builder()
                    .requestId(request.getRequestId())
                    .status("completed")
                    .build();
                    
        } catch (Exception e) {
            log.error("Failed to process content completion for request: {}", request.getRequestId(), e);
            
            // ContentRequest 상태를 FAILED로 업데이트
            ContentRequest contentRequest = contentRequestRepository.findById(request.getRequestId())
                    .orElse(null);
            if (contentRequest != null) {
                contentRequest.setStatus(ContentRequestStatus.FAILED);
                contentRequest.setErrorMessage("AI 결과 처리 실패: " + e.getMessage());
                contentRequestRepository.save(contentRequest);
            }
            
            throw new CustomContentException(CustomContentErrorCode.AI_RESULT_PROCESSING_FAILED);
        }
    }
    
    private List<CustomContent> createCustomContentsFromAiResult(ContentRequest contentRequest, AiResultDto aiResult) {
        List<CustomContent> customContents = new ArrayList<>();
        
        for (AiResultDto.LeveledResult leveledResult : aiResult.getLeveledResults()) {
            DifficultyLevel difficultyLevel = DifficultyLevel.fromCode(leveledResult.getTextLevel());
            
            // 각 난이도별로 CustomContent 생성
            CustomContent customContent = CustomContent.builder()
                    .userId(contentRequest.getUserId())
                    .contentRequestId(contentRequest.getId())
                    .title(aiResult.getTitle())
                    .author(aiResult.getAuthor())
                    .coverImageUrl(aiResult.getCoverImageUrl())
                    .difficultyLevel(difficultyLevel)
                    .targetDifficultyLevels(contentRequest.getTargetDifficultyLevels())
                    .chunkCount(calculateTotalChunkCount(leveledResult))
                    .originUrl(contentRequest.getOriginUrl())
                    .originDomain(contentRequest.getOriginDomain())
                    .build();
            
            customContent = customContentRepository.save(customContent);
            customContents.add(customContent);
            
            // CustomContentChunk 생성
            List<CustomContentChunk> chunks = createChunksFromLeveledResult(
                    customContent, 
                    leveledResult, 
                    contentRequest.getUserId()
            );
            customContentChunkRepository.saveAll(chunks);
        }
        
        return customContents;
    }
    
    private Integer calculateTotalChunkCount(AiResultDto.LeveledResult leveledResult) {
        return leveledResult.getChapters().stream()
                .mapToInt(chapter -> chapter.getChunks().size())
                .sum();
    }
    
    private List<CustomContentChunk> createChunksFromLeveledResult(
            CustomContent customContent, 
            AiResultDto.LeveledResult leveledResult,
            String userId) {
        
        List<CustomContentChunk> chunks = new ArrayList<>();
        
        for (AiResultDto.Chapter chapter : leveledResult.getChapters()) {
            for (AiResultDto.Chunk chunkData : chapter.getChunks()) {
                CustomContentChunk chunk = CustomContentChunk.builder()
                        .customContentId(customContent.getId())
                        .userId(userId)
                        .difficultyLevel(customContent.getDifficultyLevel())
                        .chapterNum(chapter.getChapterNum())
                        .chunkNum(chunkData.getChunkNum())
                        .isImage(chunkData.getIsImage())
                        .chunkText(chunkData.getChunkText())
                        .description(chunkData.getDescription())
                        .build();
                
                chunks.add(chunk);
            }
        }
        
        return chunks;
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
            
            // FCM 실패 알림 전송
            notificationService.sendContentFailedNotification(
                    contentRequest.getUserId(),
                    request.getRequestId(),
                    contentRequest.getTitle(),
                    request.getErrorMessage()
            );
            
            log.info("Updated request status to FAILED for request: {}", request.getRequestId());
            
        } catch (Exception e) {
            log.error("Failed to handle content failure for request: {}", request.getRequestId(), e);
            throw new CustomContentException(CustomContentErrorCode.WEBHOOK_PROCESSING_FAILED);
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
            log.error("Failed to handle content progress for request: {}", request.getRequestId(), e);
            throw new CustomContentException(CustomContentErrorCode.WEBHOOK_PROCESSING_FAILED);
        }
    }
}