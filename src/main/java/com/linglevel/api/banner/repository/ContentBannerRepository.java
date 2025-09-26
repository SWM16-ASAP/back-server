package com.linglevel.api.banner.repository;

import com.linglevel.api.banner.entity.ContentBanner;
import com.linglevel.api.content.common.ContentType;
import com.linglevel.api.i18n.CountryCode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ContentBannerRepository extends MongoRepository<ContentBanner, String> {

    /**
     * 활성화된 배너를 국가별, 표시순서로 조회
     */
    List<ContentBanner> findByCountryCodeAndIsActiveTrueOrderByDisplayOrderAsc(CountryCode countryCode);

    /**
     * 국가별 배너 페이지네이션 조회
     */
    Page<ContentBanner> findByCountryCode(CountryCode countryCode, Pageable pageable);

    /**
     * 특정 표시순서가 이미 사용되었는지 확인
     */
    boolean existsByCountryCodeAndDisplayOrder(CountryCode countryCode, Integer displayOrder);

    /**
     * 국가별 최대 표시순서 조회
     */
    @Query("{ 'countryCode': ?0 }")
    List<ContentBanner> findByCountryCodeOrderByDisplayOrderDesc(CountryCode countryCode);
}