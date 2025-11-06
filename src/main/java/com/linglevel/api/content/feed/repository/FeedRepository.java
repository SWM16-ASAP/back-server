package com.linglevel.api.content.feed.repository;

import com.linglevel.api.content.feed.entity.Feed;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.Instant;
import java.util.List;

public interface FeedRepository extends MongoRepository<Feed, String> {

    boolean existsByUrl(String url);

    List<Feed> findByPublishedAtAfter(Instant after);
}
