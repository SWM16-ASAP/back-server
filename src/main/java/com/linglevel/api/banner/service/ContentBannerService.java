package com.linglevel.api.banner.service;

import com.linglevel.api.banner.dto.ContentBannerResponse;
import com.linglevel.api.banner.dto.CreateContentBannerRequest;
import com.linglevel.api.banner.dto.UpdateContentBannerRequest;
import com.linglevel.api.banner.entity.ContentBanner;
import com.linglevel.api.banner.exception.BannerErrorCode;
import com.linglevel.api.banner.exception.BannerException;
import com.linglevel.api.banner.repository.ContentBannerRepository;
import com.linglevel.api.content.common.service.ContentInfo;
import com.linglevel.api.content.common.service.ContentInfoProviderFactory;
import com.linglevel.api.i18n.CountryCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContentBannerService {

    private final ContentBannerRepository contentBannerRepository;
    private final ContentInfoProviderFactory contentInfoProviderFactory;

    /**
     * 활성화된 배너 목록 조회 (국가별, 표시순서)
     */
    public List<ContentBannerResponse> getActiveBanners(CountryCode countryCode) {
        log.debug("Getting active banners for country: {}", countryCode);

        List<ContentBanner> banners = contentBannerRepository
                .findByCountryCodeAndIsActiveTrueOrderByDisplayOrderAsc(countryCode);

        return banners.stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    /**
     * 배너 목록 조회 (페이지네이션)
     */
    public Page<ContentBannerResponse> getBanners(CountryCode countryCode, int page, int size) {
        log.debug("Getting banners for country: {}, page: {}, size: {}", countryCode, page, size);

        Pageable pageable = PageRequest.of(page, size, Sort.by("displayOrder").ascending());
        Page<ContentBanner> bannerPage = contentBannerRepository.findByCountryCode(countryCode, pageable);

        return bannerPage.map(this::convertToResponse);
    }

    /**
     * 특정 배너 조회
     */
    public ContentBannerResponse getBanner(String bannerId) {
        log.debug("Getting banner by id: {}", bannerId);

        ContentBanner banner = contentBannerRepository.findById(bannerId)
                .orElseThrow(() -> new BannerException(BannerErrorCode.BANNER_NOT_FOUND));

        return convertToResponse(banner);
    }

    /**
     * 배너 생성
     */
    public ContentBannerResponse createBanner(CreateContentBannerRequest request) {
        log.info("Creating new banner for content: {} ({})", request.getContentId(), request.getContentType());

        // displayOrder가 제공되지 않았거나 중복인 경우 처리
        Integer displayOrder = request.getDisplayOrder();
        if (displayOrder == null || contentBannerRepository.existsByCountryCodeAndDisplayOrder(request.getCountryCode(), displayOrder)) {
            displayOrder = getNextDisplayOrder(request.getCountryCode());
            log.debug("Using next available display order: {}", displayOrder);
        }

        ContentBanner banner = new ContentBanner();
        banner.setCountryCode(request.getCountryCode());
        banner.setContentId(request.getContentId());
        banner.setContentType(request.getContentType());
        banner.setSubtitle(request.getSubtitle());
        banner.setTitle(request.getTitle());
        banner.setDescription(request.getDescription());
        banner.setDisplayOrder(displayOrder);
        banner.setIsActive(request.getIsActive());
        banner.setCreatedAt(LocalDateTime.now());

        ContentInfo contentInfo = contentInfoProviderFactory.getContentInfo(
                request.getContentId(), request.getContentType());

        banner.setContentTitle(contentInfo.getTitle());
        banner.setContentAuthor(contentInfo.getAuthor());
        banner.setContentCoverImageUrl(contentInfo.getCoverImageUrl());
        banner.setContentReadingTime(contentInfo.getReadingTime());

        ContentBanner savedBanner = contentBannerRepository.save(banner);
        log.info("Banner created successfully with id: {}", savedBanner.getId());

        return convertToResponse(savedBanner);
    }

    /**
     * 배너 수정
     */
    public ContentBannerResponse updateBanner(String bannerId, UpdateContentBannerRequest request) {
        log.info("Updating banner: {}", bannerId);

        ContentBanner banner = contentBannerRepository.findById(bannerId)
                .orElseThrow(() -> new BannerException(BannerErrorCode.BANNER_NOT_FOUND));

        if (request.getTitle() != null) {
            banner.setTitle(request.getTitle());
        }
        if (request.getSubtitle() != null) {
            banner.setSubtitle(request.getSubtitle());
        }
        if (request.getDescription() != null) {
            banner.setDescription(request.getDescription());
        }
        if (request.getDisplayOrder() != null) {
            banner.setDisplayOrder(request.getDisplayOrder());
        }
        if (request.getIsActive() != null) {
            banner.setIsActive(request.getIsActive());
        }

        ContentBanner updatedBanner = contentBannerRepository.save(banner);
        log.info("Banner updated successfully: {}", bannerId);

        return convertToResponse(updatedBanner);
    }

    /**
     * 배너 삭제
     */
    public void deleteBanner(String bannerId) {
        log.info("Deleting banner: {}", bannerId);

        if (!contentBannerRepository.existsById(bannerId)) {
            throw new BannerException(BannerErrorCode.BANNER_NOT_FOUND);
        }

        contentBannerRepository.deleteById(bannerId);
        log.info("Banner deleted successfully: {}", bannerId);
    }

    /**
     * 다음 사용 가능한 표시순서 조회
     */
    private Integer getNextDisplayOrder(CountryCode countryCode) {
        List<ContentBanner> banners = contentBannerRepository
                .findByCountryCodeOrderByDisplayOrderDesc(countryCode);

        if (banners.isEmpty()) {
            return 1;
        }

        return banners.get(0).getDisplayOrder() + 1;
    }

    /**
     * Entity를 Response DTO로 변환
     */
    private ContentBannerResponse convertToResponse(ContentBanner banner) {
        ContentBannerResponse response = new ContentBannerResponse();
        response.setId(banner.getId());
        response.setCountryCode(banner.getCountryCode());
        response.setContentId(banner.getContentId());
        response.setContentType(banner.getContentType());
        response.setContentTitle(banner.getContentTitle());
        response.setContentAuthor(banner.getContentAuthor());
        response.setContentCoverImageUrl(banner.getContentCoverImageUrl());
        response.setContentReadingTime(banner.getContentReadingTime());
        response.setSubtitle(banner.getSubtitle());
        response.setTitle(banner.getTitle());
        response.setDescription(banner.getDescription());
        response.setDisplayOrder(banner.getDisplayOrder());
        response.setIsActive(banner.getIsActive());
        response.setCreatedAt(banner.getCreatedAt());
        return response;
    }
}