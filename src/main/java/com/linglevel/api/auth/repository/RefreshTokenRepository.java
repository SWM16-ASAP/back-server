package com.linglevel.api.auth.repository;

import com.linglevel.api.auth.jwt.RefreshToken;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends MongoRepository<RefreshToken, String> {
    
    Optional<RefreshToken> findByTokenId(String tokenId);
    
    Optional<RefreshToken> findByUserId(String userId);
    
    void deleteByUserId(String userId);
    
    void deleteByTokenId(String tokenId);
}