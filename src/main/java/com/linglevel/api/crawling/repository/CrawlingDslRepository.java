package com.linglevel.api.crawling.repository;

import com.linglevel.api.crawling.entity.CrawlingDsl;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CrawlingDslRepository extends MongoRepository<CrawlingDsl, String> {
    Optional<CrawlingDsl> findByDomain(String domain);
    
    Page<CrawlingDsl> findAll(Pageable pageable);
    
    boolean existsByDomain(String domain);
    
    void deleteByDomain(String domain);
}