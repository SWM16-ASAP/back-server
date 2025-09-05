package com.linglevel.api.fcm.repository;

import com.linglevel.api.fcm.entity.FcmToken;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface FcmTokenRepository extends MongoRepository<FcmToken, String> {
    
    Optional<FcmToken> findByUserIdAndDeviceId(String userId, String deviceId);
}