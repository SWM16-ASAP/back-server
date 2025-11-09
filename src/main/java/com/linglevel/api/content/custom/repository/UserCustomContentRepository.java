package com.linglevel.api.content.custom.repository;

import com.linglevel.api.content.custom.entity.UserCustomContent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserCustomContentRepository extends MongoRepository<UserCustomContent, String> {

    Optional<UserCustomContent> findByUserIdAndCustomContentId(String userId, String customContentId);

    Page<UserCustomContent> findByUserId(String userId, Pageable pageable);

    List<UserCustomContent> findByUserId(String userId);

    long countByCustomContentId(String customContentId);

    Optional<UserCustomContent> findByUserIdAndContentRequestId(String userId, String contentRequestId);

    Optional<UserCustomContent> findByContentRequestId(String contentRequestId);

    boolean existsByUserIdAndCustomContentId(String userId, String customContentId);
}