package com.linglevel.api.content.custom.repository;

import com.linglevel.api.content.custom.dto.GetCustomContentsRequest;
import com.linglevel.api.content.custom.entity.CustomContent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface CustomContentRepositoryCustom {
    Page<CustomContent> findCustomContentsWithFilters(String userId, GetCustomContentsRequest request, Pageable pageable);

    Page<CustomContent> findCustomContentsByUserWithFilters(String userId, GetCustomContentsRequest request, Pageable pageable);

    void incrementViewCount(String customContentId);
}
