package com.linglevel.api.content.custom.repository;

import com.linglevel.api.content.custom.entity.ContentRequest;
import com.linglevel.api.content.custom.entity.ContentRequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ContentRequestRepository extends MongoRepository<ContentRequest, String> {
    
    Page<ContentRequest> findByUserIdAndStatusNot(String userId, ContentRequestStatus status, Pageable pageable);
    
    Page<ContentRequest> findByUserIdAndStatus(String userId, ContentRequestStatus status, Pageable pageable);
    
    Optional<ContentRequest> findByIdAndUserId(String id, String userId);
}