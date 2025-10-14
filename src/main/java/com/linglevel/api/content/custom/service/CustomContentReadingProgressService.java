package com.linglevel.api.content.custom.service;

import com.linglevel.api.content.custom.dto.CustomContentReadingProgressResponse;
import com.linglevel.api.content.custom.dto.CustomContentReadingProgressUpdateRequest;
import com.linglevel.api.content.custom.entity.CustomContent;
import com.linglevel.api.content.custom.entity.CustomContentChunk;
import com.linglevel.api.content.custom.entity.CustomContentProgress;
import com.linglevel.api.content.custom.repository.CustomContentChunkRepository;
import com.linglevel.api.content.custom.repository.CustomContentRepository;
import com.linglevel.api.content.custom.exception.CustomContentErrorCode;
import com.linglevel.api.content.custom.exception.CustomContentException;
import com.linglevel.api.content.custom.repository.CustomContentProgressRepository;
import com.linglevel.api.content.common.service.ProgressCalculationService;
import com.linglevel.api.user.entity.User;
import com.linglevel.api.user.exception.UsersErrorCode;
import com.linglevel.api.user.exception.UsersException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomContentReadingProgressService {

    private final CustomContentService customContentService;
    private final CustomContentChunkService customContentChunkService;
    private final CustomContentProgressRepository customContentProgressRepository;
    private final CustomContentChunkRepository customContentChunkRepository;
    private final ProgressCalculationService progressCalculationService;


    @Transactional
    public CustomContentReadingProgressResponse updateProgress(String customId, CustomContentReadingProgressUpdateRequest request, String userId) {
        // 커스텀 콘텐츠 존재 여부 확인
        if (!customContentService.existsById(customId)) {
            throw new CustomContentException(CustomContentErrorCode.CUSTOM_CONTENT_NOT_FOUND);
        }

        // chunkId로부터 chunk 정보 조회
        CustomContentChunk chunk = customContentChunkService.findById(request.getChunkId());

        // chunk가 해당 custom content에 속하는지 검증
        if (chunk.getCustomContentId() == null || !chunk.getCustomContentId().equals(customId)) {
            throw new CustomContentException(CustomContentErrorCode.CHUNK_NOT_FOUND_IN_CUSTOM_CONTENT);
        }

        CustomContentProgress customProgress = customContentProgressRepository.findByUserIdAndCustomId(userId, customId)
                .orElse(new CustomContentProgress());

        // Null 체크
        if (chunk.getChunkNum() == null) {
            throw new CustomContentException(CustomContentErrorCode.CUSTOM_CONTENT_CHUNK_NOT_FOUND);
        }

        customProgress.setUserId(userId);
        customProgress.setCustomId(customId);
        customProgress.setChunkId(request.getChunkId());

        // [V2_CORE] V2 필드: 정규화된 진행률 계산
        long totalChunks = customContentChunkRepository.countByCustomContentIdAndDifficultyLevelAndIsDeletedFalse(
            customId, chunk.getDifficultyLevel()
        );
        double normalizedProgress = progressCalculationService.calculateNormalizedProgress(
            chunk.getChunkNum(), totalChunks
        );

        customProgress.setNormalizedProgress(normalizedProgress);
        customProgress.setCurrentDifficultyLevel(chunk.getDifficultyLevel());

        // maxNormalizedProgress 업데이트 (누적 최대값)
        if (progressCalculationService.shouldUpdateMaxProgress(
                customProgress.getMaxNormalizedProgress(), normalizedProgress)) {
            customProgress.setMaxNormalizedProgress(normalizedProgress);
        }

        // 완료 조건: maxNormalizedProgress >= 100%
        boolean isCompleted = progressCalculationService.isCompleted(customProgress.getMaxNormalizedProgress());
        customProgress.setIsCompleted(progressCalculationService.updateCompletedFlag(
            customProgress.getIsCompleted(), isCompleted
        ));

        customContentProgressRepository.save(customProgress);

        return convertToCustomContentReadingProgressResponse(customProgress);
    }

    @Transactional(readOnly = true)
    public CustomContentReadingProgressResponse getProgress(String customId, String userId) {
        // 커스텀 콘텐츠 존재 여부 확인
        if (!customContentService.existsById(customId)) {
            throw new CustomContentException(CustomContentErrorCode.CUSTOM_CONTENT_NOT_FOUND);
        }

        CustomContentProgress customProgress = customContentProgressRepository.findByUserIdAndCustomId(userId, customId)
                .orElseGet(() -> initializeProgress(userId, customId));

        return convertToCustomContentReadingProgressResponse(customProgress);
    }

    private CustomContentProgress initializeProgress(String userId, String customId) {
        // 첫 번째 청크로 초기화
        CustomContentChunk firstChunk = customContentChunkService.findFirstByCustomContentId(customId);

        CustomContentProgress newProgress = new CustomContentProgress();
        newProgress.setUserId(userId);
        newProgress.setCustomId(customId);
        newProgress.setChunkId(firstChunk.getId());

        // [V2_CORE] V2 필드: 초기 진행률 계산
        long totalChunks = customContentChunkRepository.countByCustomContentIdAndDifficultyLevelAndIsDeletedFalse(
            customId, firstChunk.getDifficultyLevel()
        );
        double initialProgress = progressCalculationService.calculateNormalizedProgress(
            firstChunk.getChunkNum(), totalChunks
        );

        newProgress.setNormalizedProgress(initialProgress);
        newProgress.setMaxNormalizedProgress(initialProgress);
        newProgress.setCurrentDifficultyLevel(firstChunk.getDifficultyLevel());

        return customContentProgressRepository.save(newProgress);
    }

    @Transactional
    public void deleteProgress(String customId, String userId) {
        if (!customContentService.existsById(customId)) {
            throw new CustomContentException(CustomContentErrorCode.CUSTOM_CONTENT_NOT_FOUND);
        }

        CustomContentProgress customProgress = customContentProgressRepository.findByUserIdAndCustomId(userId, customId)
                .orElseThrow(() -> new CustomContentException(CustomContentErrorCode.PROGRESS_NOT_FOUND));

        customContentProgressRepository.delete(customProgress);
    }

    private CustomContentReadingProgressResponse convertToCustomContentReadingProgressResponse(CustomContentProgress progress) {
        // [DTO_MAPPING] chunk에서 chunkNum 조회
        CustomContentChunk chunk = customContentChunkService.findById(progress.getChunkId());

        // [FALLBACK] V2 필드가 없으면 동적 계산 (기존 데이터 대응)
        if (progress.getNormalizedProgress() == null || progress.getCurrentDifficultyLevel() == null) {
            log.info("V2 fields missing for CustomContentProgress {}, calculating lazily", progress.getId());

            // 난이도별 전체 청크 수 조회
            long totalChunks = customContentChunkRepository.countByCustomContentIdAndDifficultyLevelAndIsDeletedFalse(
                chunk.getCustomContentId(), chunk.getDifficultyLevel()
            );
            double normalizedProgress = progressCalculationService.calculateNormalizedProgress(
                chunk.getChunkNum(), totalChunks
            );

            // Lazy migration: V2 필드 저장
            progress.setNormalizedProgress(normalizedProgress);
            progress.setMaxNormalizedProgress(normalizedProgress);
            progress.setCurrentDifficultyLevel(chunk.getDifficultyLevel());

            // 완료 조건 재계산
            boolean isCompleted = progressCalculationService.isCompleted(normalizedProgress);
            progress.setIsCompleted(progressCalculationService.updateCompletedFlag(
                progress.getIsCompleted(), isCompleted
            ));

            customContentProgressRepository.save(progress);
            log.info("Lazy migration completed for CustomContentProgress {}", progress.getId());
        }

        return CustomContentReadingProgressResponse.builder()
                .id(progress.getId())
                .userId(progress.getUserId())
                .customId(progress.getCustomId())
                .chunkId(progress.getChunkId())
                .currentReadChunkNumber(chunk.getChunkNum())
                .isCompleted(progress.getIsCompleted())
                .currentDifficultyLevel(progress.getCurrentDifficultyLevel())
                .normalizedProgress(progress.getNormalizedProgress())
                .maxNormalizedProgress(progress.getMaxNormalizedProgress())
                .updatedAt(progress.getUpdatedAt())
                .build();
    }
}