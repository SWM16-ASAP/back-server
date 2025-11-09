package com.linglevel.api.content.custom.service;

import com.linglevel.api.content.custom.entity.ContentRequest;
import com.linglevel.api.content.custom.entity.CustomContent;
import com.linglevel.api.content.custom.entity.UserCustomContent;
import com.linglevel.api.content.custom.exception.CustomContentErrorCode;
import com.linglevel.api.content.custom.exception.CustomContentException;
import com.linglevel.api.content.custom.repository.UserCustomContentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserCustomContentService {

    private final UserCustomContentRepository userCustomContentRepository;

    public boolean validateNotOwned(String userId, String customContentId) {
        return !userCustomContentRepository.existsByUserIdAndCustomContentId(userId, customContentId);
    }

    @Transactional
    public void createMapping(ContentRequest contentRequest, CustomContent customContent) {
        createMapping(
                contentRequest.getUserId(),
                customContent.getId(),
                contentRequest.getId()
        );
    }

    @Transactional
    public void createMapping(String userId, String customContentId, String contentRequestId) {
        UserCustomContent userCustomContent = UserCustomContent.builder()
                .userId(userId)
                .customContentId(customContentId)
                .contentRequestId(contentRequestId)
                .build();

        userCustomContentRepository.save(userCustomContent);
        log.info("Created UserCustomContent mapping for user: {} and content: {}",
                userId, customContentId);
    }
}
