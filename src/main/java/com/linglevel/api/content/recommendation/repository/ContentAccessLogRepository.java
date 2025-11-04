package com.linglevel.api.content.recommendation.repository;

import com.linglevel.api.content.recommendation.entity.ContentAccessLog;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.Instant;
import java.util.List;

public interface ContentAccessLogRepository extends MongoRepository<ContentAccessLog, String> {

    List<ContentAccessLog> findByAccessedAtAfter(Instant after);
    void deleteByAccessedAtBefore(Instant before);
}
