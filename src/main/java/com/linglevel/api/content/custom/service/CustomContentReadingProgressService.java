package com.linglevel.api.content.custom.service;

import com.linglevel.api.content.custom.dto.CustomContentReadingProgressResponse;
import com.linglevel.api.content.custom.dto.CustomContentReadingProgressUpdateRequest;
import com.linglevel.api.content.custom.entity.CustomContentChunk;
import com.linglevel.api.content.custom.entity.CustomContentProgress;
import com.linglevel.api.content.custom.exception.CustomContentErrorCode;
import com.linglevel.api.content.custom.exception.CustomContentException;
import com.linglevel.api.content.custom.repository.CustomContentProgressRepository;
import com.linglevel.api.user.entity.User;
import com.linglevel.api.user.exception.UsersErrorCode;
import com.linglevel.api.user.exception.UsersException;
import com.linglevel.api.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomContentReadingProgressService {

    private final CustomContentService customContentService;
    private final CustomContentChunkService customContentChunkService;
    private final CustomContentProgressRepository customContentProgressRepository;
    private final UserRepository userRepository;

    @Transactional
    public CustomContentReadingProgressResponse updateProgress(String customId, CustomContentReadingProgressUpdateRequest request, String username) {
        // 커스텀 콘텐츠 존재 여부 확인
        if (!customContentService.existsById(customId)) {
            throw new CustomContentException(CustomContentErrorCode.CUSTOM_CONTENT_NOT_FOUND);
        }

        // 사용자 조회
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsersException(UsersErrorCode.USER_NOT_FOUND));

        // chunkId로부터 chunk 정보 조회
        CustomContentChunk chunk = customContentChunkService.findById(request.getChunkId());

        // chunk가 해당 custom content에 속하는지 검증
        if (!chunk.getCustomContentId().equals(customId)) {
            throw new CustomContentException(CustomContentErrorCode.CHUNK_NOT_FOUND_IN_CUSTOM_CONTENT);
        }

        String userId = user.getId();

        CustomContentProgress customProgress = customContentProgressRepository.findByUserIdAndCustomId(userId, customId)
                .orElse(new CustomContentProgress());

        customProgress.setUserId(userId);
        customProgress.setCustomId(customId);
        customProgress.setChunkId(request.getChunkId());
        customProgress.setCurrentReadChunkNumber(chunk.getChunkNum()); // CustomContentChunk는 chunkNum 필드 사용
        customProgress.setUpdatedAt(LocalDateTime.now());

        customContentProgressRepository.save(customProgress);

        return convertToCustomContentReadingProgressResponse(customProgress);
    }

    @Transactional(readOnly = true)
    public CustomContentReadingProgressResponse getProgress(String customId, String username) {
        // 커스텀 콘텐츠 존재 여부 확인
        if (!customContentService.existsById(customId)) {
            throw new CustomContentException(CustomContentErrorCode.CUSTOM_CONTENT_NOT_FOUND);
        }

        // 사용자 조회
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsersException(UsersErrorCode.USER_NOT_FOUND));

        String userId = user.getId();

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
        newProgress.setUpdatedAt(LocalDateTime.now());

        return customContentProgressRepository.save(newProgress);
    }

    private CustomContentReadingProgressResponse convertToCustomContentReadingProgressResponse(CustomContentProgress progress) {
        return CustomContentReadingProgressResponse.builder()
                .id(progress.getId())
                .userId(progress.getUserId())
                .customId(progress.getCustomId())
                .chunkId(progress.getChunkId())
                .currentReadChunkNumber(progress.getCurrentReadChunkNumber())
                .updatedAt(progress.getUpdatedAt())
                .build();
    }
}