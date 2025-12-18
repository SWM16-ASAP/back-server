package com.linglevel.api.content.feed.repository;

import com.linglevel.api.content.feed.entity.Feed;

import java.util.List;

public interface FeedRepositoryCustom {

    List<Feed> findByDeletedFalseWithProjection();
}