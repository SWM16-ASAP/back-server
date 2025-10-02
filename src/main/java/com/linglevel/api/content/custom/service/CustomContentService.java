package com.linglevel.api.content.custom.service;

import com.linglevel.api.common.dto.PageResponse;
import com.linglevel.api.content.custom.dto.CustomContentResponse;
import com.linglevel.api.content.custom.dto.GetCustomContentsRequest;
import com.linglevel.api.content.custom.dto.UpdateCustomContentRequest;
import com.linglevel.api.content.custom.entity.CustomContent;
import com.linglevel.api.content.custom.entity.CustomContentChunk;
import com.linglevel.api.content.custom.exception.CustomContentErrorCode;
import com.linglevel.api.content.custom.exception.CustomContentException;
import com.linglevel.api.content.custom.repository.CustomContentChunkRepository;
import com.linglevel.api.content.custom.repository.CustomContentRepository;
import com.linglevel.api.content.custom.repository.CustomContentProgressRepository;
import com.linglevel.api.content.custom.entity.CustomContentProgress;
import com.linglevel.api.content.common.ProgressStatus;
import com.linglevel.api.user.entity.User;
import com.linglevel.api.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomContentService {

    private final CustomContentRepository customContentRepository;
    private final CustomContentChunkRepository customContentChunkRepository;
    private final CustomContentProgressRepository customContentProgressRepository;
    private final UserRepository userRepository;
    private final MongoTemplate mongoTemplate;

    public PageResponse<CustomContentResponse> getCustomContents(String username, GetCustomContentsRequest request) {
        log.info("Getting custom contents for user: {} with request: {}", username, request);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new CustomContentException(CustomContentErrorCode.USER_NOT_FOUND));

        Sort sort = Sort.by(Sort.Direction.DESC, "createdAt"); // 기본값: 최신순
        if (StringUtils.hasText(request.getSortBy())) {
            switch (request.getSortBy()) {
                case "view_count":
                    sort = Sort.by(Sort.Direction.DESC, "viewCount");
                    break;
                case "average_rating":
                    sort = Sort.by(Sort.Direction.DESC, "averageRating");
                    break;
            }
        }

        Pageable pageable = PageRequest.of(request.getPage() - 1, request.getLimit(), sort);

        // Custom Repository 사용 - 필터링 + 페이지네이션 통합 처리
        Page<CustomContent> page = customContentRepository.findCustomContentsWithFilters(user.getId(), request, pageable);

        List<CustomContentResponse> responses = page.getContent().stream()
                .map(content -> mapToResponse(content, user.getId()))
                .collect(Collectors.toList());

        return new PageResponse<>(responses, page);
    }

    public CustomContentResponse getCustomContent(String username, String customContentId) {
        log.info("Getting custom content {} for user: {}", customContentId, username);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new CustomContentException(CustomContentErrorCode.USER_NOT_FOUND));

        CustomContent content = customContentRepository.findByIdAndUserIdAndIsDeletedFalse(customContentId, user.getId())
                .orElseThrow(() -> new CustomContentException(CustomContentErrorCode.CUSTOM_CONTENT_NOT_FOUND));

        return mapToResponse(content, user.getId());
    }

    @Transactional
    public CustomContentResponse updateCustomContent(String username, String customContentId, UpdateCustomContentRequest request) {
        log.info("Updating custom content {} for user: {}", customContentId, username);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new CustomContentException(CustomContentErrorCode.USER_NOT_FOUND));

        CustomContent content = customContentRepository.findByIdAndUserIdAndIsDeletedFalse(customContentId, user.getId())
                .orElseThrow(() -> new CustomContentException(CustomContentErrorCode.CUSTOM_CONTENT_NOT_FOUND));

        if (request.getTitle() != null) {
            content.setTitle(request.getTitle());
        }
        if (request.getTags() != null) {
            content.setTags(request.getTags());
        }

        CustomContent updatedContent = customContentRepository.save(content);
        return mapToResponse(updatedContent, user.getId());
    }

    @Transactional
    public void deleteCustomContent(String username, String customContentId) {
        log.info("Deleting custom content {} for user: {}", customContentId, username);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new CustomContentException(CustomContentErrorCode.USER_NOT_FOUND));

        CustomContent content = customContentRepository.findByIdAndUserIdAndIsDeletedFalse(customContentId, user.getId())
                .orElseThrow(() -> new CustomContentException(CustomContentErrorCode.CUSTOM_CONTENT_NOT_FOUND));

        // Soft delete the main content
        content.setIsDeleted(true);
        content.setDeletedAt(LocalDateTime.now());
        customContentRepository.save(content);

        // Cascade soft delete to all related chunks
        List<CustomContentChunk> chunks = customContentChunkRepository.findByCustomContentIdAndIsDeletedFalseOrderByChapterNumAscChunkNumAsc(customContentId);
        if (!chunks.isEmpty()) {
            chunks.forEach(chunk -> {
                chunk.setIsDeleted(true);
                chunk.setDeletedAt(LocalDateTime.now());
            });
            customContentChunkRepository.saveAll(chunks);
            log.info("Soft deleted {} related chunks for custom content: {}", chunks.size(), customContentId);
        }
    }

    private String getUserId(String username) {
        if (username == null) return null;
        return userRepository.findByUsername(username)
            .map(User::getId)
            .orElse(null);
    }

    private CustomContentResponse mapToResponse(CustomContent content) {
        return mapToResponse(content, null);
    }

    private CustomContentResponse mapToResponse(CustomContent content, String userId) {
        // 진도 정보 조회
        int currentReadChunkNumber = 0;
        double progressPercentage = 0.0;
        boolean isCompleted = false;

        if (userId != null) {
            CustomContentProgress progress = customContentProgressRepository
                .findByUserIdAndCustomId(userId, content.getId())
                .orElse(null);

            if (progress != null) {
                currentReadChunkNumber = progress.getCurrentReadChunkNumber() != null
                    ? progress.getCurrentReadChunkNumber() : 0;

                if (content.getChunkCount() != null && content.getChunkCount() > 0) {
                    progressPercentage = (double) currentReadChunkNumber / content.getChunkCount() * 100.0;
                }

                isCompleted = progress.getIsCompleted() != null ? progress.getIsCompleted() : false;
            }
        }
        CustomContentResponse response = new CustomContentResponse();
        response.setId(content.getId());
        response.setTitle(content.getTitle());
        response.setAuthor(content.getAuthor());
        response.setCoverImageUrl(content.getCoverImageUrl());
        response.setDifficultyLevel(content.getDifficultyLevel());
        response.setTargetDifficultyLevels(content.getTargetDifficultyLevels());
        response.setChunkCount(content.getChunkCount());
        response.setCurrentReadChunkNumber(currentReadChunkNumber);
        response.setProgressPercentage(progressPercentage);
        response.setIsCompleted(isCompleted);
        response.setReadingTime(content.getReadingTime());
        response.setAverageRating(content.getAverageRating() != null ? content.getAverageRating().floatValue() : 0.0d);
        response.setReviewCount(content.getReviewCount());
        response.setViewCount(content.getViewCount());
        response.setTags(content.getTags());
        response.setOriginUrl(content.getOriginUrl());
        response.setOriginDomain(content.getOriginDomain());
        response.setCreatedAt(content.getCreatedAt());
        response.setUpdatedAt(content.getUpdatedAt());
        return response;
    }

    public boolean existsById(String customContentId) {
        return customContentRepository.existsById(customContentId);
    }
}