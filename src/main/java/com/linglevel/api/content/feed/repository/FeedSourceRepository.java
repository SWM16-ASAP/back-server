package com.linglevel.api.content.feed.repository;

import com.linglevel.api.content.feed.entity.FeedSource;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface FeedSourceRepository extends MongoRepository<FeedSource, String> {

    boolean existsByUrl(String url);

    Optional<FeedSource> findByUrl(String url);

    List<FeedSource> findByIsActiveTrue();
}
