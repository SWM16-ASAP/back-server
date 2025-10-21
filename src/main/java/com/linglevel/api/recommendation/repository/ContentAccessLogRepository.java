package com.linglevel.api.recommendation.repository;

import com.linglevel.api.recommendation.entity.ContentAccessLog;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface ContentAccessLogRepository extends MongoRepository<ContentAccessLog, String> {

    List<ContentAccessLog> findByAccessedAtAfter(LocalDateTime after);
    void deleteByAccessedAtBefore(LocalDateTime before);
}
