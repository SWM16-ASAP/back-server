package com.linglevel.api.crawling.service;

import com.linglevel.api.content.feed.entity.FeedContentType;
import com.linglevel.api.crawling.dto.*;
import com.linglevel.api.crawling.entity.CrawlingDsl;
import com.linglevel.api.crawling.exception.CrawlingErrorCode;
import com.linglevel.api.crawling.exception.CrawlingException;
import com.linglevel.api.crawling.repository.CrawlingDslRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class  CrawlingService {

    private final CrawlingDslRepository crawlingDslRepository;

    public DslLookupResponse lookupDsl(String url, boolean validateOnly) {
        if (url == null || url.trim().isEmpty()) {
            throw new CrawlingException(CrawlingErrorCode.URL_PARAMETER_REQUIRED);
        }

        String domain = extractDomain(url);
        Optional<CrawlingDsl> crawlingDsl = crawlingDslRepository.findByDomain(domain);

        if (crawlingDsl.isPresent()) {
            if (validateOnly) {
                return DslLookupResponse.builder()
                        .domain(domain)
                        .valid(true)
                        .message("URL is valid for crawling.")
                        .build();
            } else {
                return DslLookupResponse.builder()
                        .domain(domain)
                        .titleDsl(crawlingDsl.get().getTitleDsl())
                        .contentDsl(crawlingDsl.get().getContentDsl())
                        .coverImageDsl(crawlingDsl.get().getCoverImageDsl())
                        .valid(true)
                        .build();
            }
        } else {
            return DslLookupResponse.builder()
                    .domain(domain)
                    .valid(false)
                    .message("DSL not available for this domain.")
                    .build();
        }
    }

    public Page<DomainsResponse> getDomains(int page, int limit, List<FeedContentType> contentTypes) {
        Pageable pageable = PageRequest.of(page - 1, limit);
        Page<CrawlingDsl> domains;

        if (contentTypes == null || contentTypes.isEmpty()) {
            domains = crawlingDslRepository.findAll(pageable);
        } else {
            domains = crawlingDslRepository.findByContentTypeIn(contentTypes, pageable);
        }

        return domains.map(dsl -> DomainsResponse.builder()
                .id(dsl.getId())
                .domain(dsl.getDomain())
                .name(dsl.getName())
                .contentType(dsl.getContentType())
                .build());
    }

    public CreateDslResponse createDsl(CreateDslRequest request) {
        if (crawlingDslRepository.existsByDomain(request.getDomain())) {
            throw new CrawlingException(CrawlingErrorCode.DOMAIN_ALREADY_EXISTS);
        }

        CrawlingDsl crawlingDsl = CrawlingDsl.builder()
                .domain(request.getDomain())
                .name(request.getName())
                .contentType(request.getContentType())
                .titleDsl(request.getTitleDsl())
                .contentDsl(request.getContentDsl())
                .coverImageDsl(request.getCoverImageDsl())
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        CrawlingDsl saved = crawlingDslRepository.save(crawlingDsl);

        return CreateDslResponse.builder()
                .id(saved.getId())
                .domain(saved.getDomain())
                .message("DSL created successfully.")
                .build();
    }

    public UpdateDslResponse updateDsl(String domain, UpdateDslRequest request) {
        CrawlingDsl crawlingDsl = crawlingDslRepository.findByDomain(domain)
                .orElseThrow(() -> new CrawlingException(CrawlingErrorCode.DOMAIN_NOT_FOUND));

        crawlingDsl.setName(request.getName());
        crawlingDsl.setTitleDsl(request.getTitleDsl());
        crawlingDsl.setContentDsl(request.getContentDsl());
        crawlingDsl.setCoverImageDsl(request.getCoverImageDsl());

        // contentType은 옵셔널 - null이 아닐 경우에만 업데이트
        if (request.getContentType() != null) {
            crawlingDsl.setContentType(request.getContentType());
        }

        crawlingDsl.setUpdatedAt(Instant.now());

        CrawlingDsl updated = crawlingDslRepository.save(crawlingDsl);

        return UpdateDslResponse.builder()
                .id(updated.getId())
                .domain(updated.getDomain())
                .name(updated.getName())
                .titleDsl(updated.getTitleDsl())
                .contentDsl(updated.getContentDsl())
                .coverImageDsl(updated.getCoverImageDsl())
                .message("DSL updated successfully.")
                .build();
    }

    public void deleteDsl(String domain) {
        if (!crawlingDslRepository.existsByDomain(domain)) {
            throw new CrawlingException(CrawlingErrorCode.DOMAIN_NOT_FOUND);
        }
        crawlingDslRepository.deleteByDomain(domain);
    }

    public boolean isValidUrl(String url) {
        return extractDomain(url) != null;
    }

    private String extractDomain(String url) {
        try {
            URL parsedUrl = new URL(url);
            String host = parsedUrl.getHost().toLowerCase();

            Pattern pattern = Pattern.compile("([^.]+\\.[^.]+)$");
            Matcher matcher = pattern.matcher(host);

            if (matcher.find()) {
                return matcher.group(1);
            }

            return host;
        } catch (MalformedURLException e) {
            throw new CrawlingException(CrawlingErrorCode.INVALID_URL_FORMAT);
        }
    }
}