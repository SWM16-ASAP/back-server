package com.linglevel.api.content.custom.repository;

import com.linglevel.api.content.custom.entity.CustomContent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CustomContentRepository extends MongoRepository<CustomContent, String>, CustomContentRepositoryCustom {
    
    Page<CustomContent> findByUserIdAndIsDeletedFalse(String userId, Pageable pageable);
    
    Optional<CustomContent> findByIdAndUserIdAndIsDeletedFalse(String id, String userId);
    
    Optional<CustomContent> findByIdAndIsDeletedFalse(String id);

    Optional<CustomContent> findByContentRequestIdAndIsDeletedFalse(String contentRequestId);

    Optional<CustomContent> findByOriginUrlAndIsDeletedFalse(String originUrl);
}