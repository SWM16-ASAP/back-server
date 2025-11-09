package com.linglevel.api.content.feed.repository;

import com.linglevel.api.content.feed.entity.Feed;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface FeedRepository extends MongoRepository<Feed, String> {

    boolean existsByUrl(String url);

    Optional<Feed> findByUrl(String url);

    List<Feed> findByDeletedFalse();

    Optional<Feed> findByIdAndDeletedFalse(String id);
}
