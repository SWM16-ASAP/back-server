package com.linglevel.api.crawling.service;

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
import java.time.LocalDateTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class CrawlingService {

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

    public Page<DomainsResponse> getDomains(int page, int limit) {
        Pageable pageable = PageRequest.of(page - 1, limit);
        Page<CrawlingDsl> domains = crawlingDslRepository.findAll(pageable);
        
        return domains.map(dsl -> DomainsResponse.builder()
                .id(dsl.getId())
                .domain(dsl.getDomain())
                .name(dsl.getName())
                .build());
    }

    public CreateDslResponse createDsl(CreateDslRequest request) {
        if (crawlingDslRepository.existsByDomain(request.getDomain())) {
            throw new CrawlingException(CrawlingErrorCode.DOMAIN_ALREADY_EXISTS);
        }

        CrawlingDsl crawlingDsl = CrawlingDsl.builder()
                .domain(request.getDomain())
                .name(request.getName())
                .titleDsl(request.getTitleDsl())
                .contentDsl(request.getContentDsl())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
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
        crawlingDsl.setUpdatedAt(LocalDateTime.now());

        CrawlingDsl updated = crawlingDslRepository.save(crawlingDsl);

        return UpdateDslResponse.builder()
                .id(updated.getId())
                .domain(updated.getDomain())
                .name(updated.getName())
                .titleDsl(updated.getTitleDsl())
                .contentDsl(updated.getContentDsl())
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
            
            if (host.startsWith("www.")) {
                host = host.substring(4);
            }
            
            return host;
        } catch (MalformedURLException e) {
            throw new CrawlingException(CrawlingErrorCode.INVALID_URL_FORMAT);
        }
    }
}