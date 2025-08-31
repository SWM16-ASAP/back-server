package com.linglevel.api.version.repository;

import com.linglevel.api.version.entity.AppVersion;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface AppVersionRepository extends MongoRepository<AppVersion, String> {
    Optional<AppVersion> findTopByOrderByUpdatedAtDesc();
}