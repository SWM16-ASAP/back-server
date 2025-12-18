package com.linglevel.api.content.feed.repository;

import com.linglevel.api.content.feed.dto.GetFeedsRequest;
import com.linglevel.api.content.feed.entity.Feed;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface FeedRepositoryCustom {

    List<Feed> findByDeletedFalseWithProjection();

    Page<Feed> findFeedsWithFilters(GetFeedsRequest request, String userId, Pageable pageable);
}