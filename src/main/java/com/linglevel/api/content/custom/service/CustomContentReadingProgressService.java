package com.linglevel.api.content.custom.service;

import com.linglevel.api.content.custom.dto.CustomContentReadingProgressResponse;
import com.linglevel.api.content.custom.dto.CustomContentReadingProgressUpdateRequest;
import com.linglevel.api.content.custom.entity.CustomContent;
import com.linglevel.api.content.custom.entity.CustomContentChunk;
import com.linglevel.api.content.custom.entity.CustomContentProgress;
import com.linglevel.api.content.custom.repository.CustomContentRepository;
import com.linglevel.api.content.custom.exception.CustomContentErrorCode;
import com.linglevel.api.content.custom.exception.CustomContentException;
import com.linglevel.api.content.custom.repository.CustomContentProgressRepository;
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
    private final CustomContentRepository customContentRepository;


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
        customProgress.setCurrentReadChunkNumber(chunk.getChunkNum()); // CustomContentChunk는 chunkNum 필드 사용

        // max 진도 업데이트 (current가 max보다 크면 max도 업데이트)
        if (customProgress.getMaxReadChunkNumber() == null ||
            chunk.getChunkNum() > customProgress.getMaxReadChunkNumber()) {
            customProgress.setMaxReadChunkNumber(chunk.getChunkNum());
        }

        // 완료 조건 자동 체크 (한번 true가 되면 계속 유지)
        CustomContent customContent = customContentRepository.findById(customId)
                .orElseThrow(() -> new CustomContentException(CustomContentErrorCode.CUSTOM_CONTENT_NOT_FOUND));
        boolean isCompleted = chunk.getChunkNum() >= customContent.getChunkCount();
        customProgress.setIsCompleted(customProgress.getIsCompleted() != null && customProgress.getIsCompleted() || isCompleted);

        // updatedAt은 @LastModifiedDate에 의해 자동 설정됨

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
        newProgress.setCurrentReadChunkNumber(firstChunk.getChunkNum());
        newProgress.setMaxReadChunkNumber(firstChunk.getChunkNum());
        // updatedAt은 @LastModifiedDate에 의해 자동 설정됨

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
        return CustomContentReadingProgressResponse.builder()
                .id(progress.getId())
                .userId(progress.getUserId())
                .customId(progress.getCustomId())
                .chunkId(progress.getChunkId())
                .currentReadChunkNumber(progress.getCurrentReadChunkNumber())
                .maxReadChunkNumber(progress.getMaxReadChunkNumber())
                .isCompleted(progress.getIsCompleted())
                .updatedAt(progress.getUpdatedAt())
                .build();
    }
}