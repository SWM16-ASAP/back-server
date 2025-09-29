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

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomContentService {

    private final CustomContentRepository customContentRepository;
    private final CustomContentChunkRepository customContentChunkRepository;
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

        Query query = new Query().with(pageable);
        query.addCriteria(Criteria.where("userId").is(user.getId()));
        query.addCriteria(Criteria.where("isDeleted").is(false));

        if (StringUtils.hasText(request.getKeyword())) {
            Criteria keywordCriteria = new Criteria().orOperator(
                    Criteria.where("title").regex(request.getKeyword(), "i"),
                    Criteria.where("author").regex(request.getKeyword(), "i")
            );
            query.addCriteria(keywordCriteria);
        }

        if (StringUtils.hasText(request.getTags())) {
            String[] tags = request.getTags().split(",");
            query.addCriteria(Criteria.where("tags").all((Object[]) tags));
        }

        long total = mongoTemplate.count(query.limit(-1).skip(-1), CustomContent.class);
        List<CustomContent> contents = mongoTemplate.find(query, CustomContent.class);

        Page<CustomContent> page = new PageImpl<>(contents, pageable, total);
        Page<CustomContentResponse> responsePage = page.map(this::mapToResponse);

        return new PageResponse<>(responsePage.getContent(), responsePage);
    }

    public CustomContentResponse getCustomContent(String username, String customContentId) {
        log.info("Getting custom content {} for user: {}", customContentId, username);
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new CustomContentException(CustomContentErrorCode.USER_NOT_FOUND));

        CustomContent content = customContentRepository.findByIdAndUserIdAndIsDeletedFalse(customContentId, user.getId())
                .orElseThrow(() -> new CustomContentException(CustomContentErrorCode.CUSTOM_CONTENT_NOT_FOUND));

        return mapToResponse(content);
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
        return mapToResponse(updatedContent);
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

    private CustomContentResponse mapToResponse(CustomContent content) {
        CustomContentResponse response = new CustomContentResponse();
        response.setId(content.getId());
        response.setTitle(content.getTitle());
        response.setAuthor(content.getAuthor());
        response.setCoverImageUrl(content.getCoverImageUrl());
        response.setDifficultyLevel(content.getDifficultyLevel());
        response.setTargetDifficultyLevels(content.getTargetDifficultyLevels());
        response.setChunkCount(content.getChunkCount());
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