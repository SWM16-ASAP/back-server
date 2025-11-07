package com.linglevel.api.content.feed.repository;

import com.linglevel.api.content.feed.entity.FeedSource;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface FeedSourceRepository extends MongoRepository<FeedSource, String> {

    boolean existsByUrl(String url);

    List<FeedSource> findByIsActiveTrue();
}
