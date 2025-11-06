package com.linglevel.api.content.feed.dto;

import com.linglevel.api.content.feed.entity.FeedSource;
import com.linglevel.api.content.feed.repository.FeedSourceRepository;
import com.linglevel.api.content.feed.scheduler.FeedCrawlingScheduler;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.MalformedURLException;
import java.net.URL;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/v1/admin/feed-sources")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Admin - Feed Sources", description = "Feed 크롤링 소스 관리 API")
public class AdminFeedSourceController {

    private final FeedSourceRepository feedSourceRepository;
    private final FeedCrawlingScheduler feedCrawlingScheduler;

    @Operation(summary = "FeedSource 생성", description = "크롤링할 Feed 소스를 등록합니다.")
    @PostMapping
    public ResponseEntity<FeedSource> createFeedSource(@Valid @RequestBody CreateFeedSourceRequest request) {
        FeedSource feedSource = FeedSource.builder()
            .url(request.getUrl())
            .domain(extractDomain(request.getUrl()))
            .name(request.getName())
            .contentType(request.getContentType())
            .category(request.getCategory())
            .tags(request.getTags())
            .isActive(true)
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

        FeedSource saved = feedSourceRepository.save(feedSource);
        log.info("FeedSource created: {} ({})", saved.getName(), saved.getUrl());

        return ResponseEntity.ok(saved);
    }

    @Operation(summary = "FeedSource 목록 조회", description = "등록된 모든 Feed 소스를 조회합니다.")
    @GetMapping
    public ResponseEntity<List<FeedSource>> getAllFeedSources() {
        return ResponseEntity.ok(feedSourceRepository.findAll());
    }

    @Operation(summary = "FeedSource 단건 조회", description = "특정 Feed 소스를 조회합니다.")
    @GetMapping("/{id}")
    public ResponseEntity<FeedSource> getFeedSource(@PathVariable String id) {
        return feedSourceRepository.findById(id)
            .map(ResponseEntity::ok)
            .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "FeedSource 수정", description = "Feed 소스 정보를 수정합니다.")
    @PutMapping("/{id}")
    public ResponseEntity<FeedSource> updateFeedSource(
            @PathVariable String id,
            @Valid @RequestBody CreateFeedSourceRequest request) {

        return feedSourceRepository.findById(id)
            .map(feedSource -> {
                feedSource.setUrl(request.getUrl());
                feedSource.setDomain(extractDomain(request.getUrl()));
                feedSource.setName(request.getName());
                feedSource.setContentType(request.getContentType());
                feedSource.setCategory(request.getCategory());
                feedSource.setTags(request.getTags());
                feedSource.setUpdatedAt(Instant.now());

                FeedSource updated = feedSourceRepository.save(feedSource);
                log.info("FeedSource updated: {} ({})", updated.getName(), updated.getId());

                return ResponseEntity.ok(updated);
            })
            .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "FeedSource 활성화/비활성화", description = "Feed 소스의 크롤링 활성화 상태를 변경합니다.")
    @PatchMapping("/{id}/active")
    public ResponseEntity<FeedSource> toggleActive(
            @PathVariable String id,
            @RequestParam boolean isActive) {

        return feedSourceRepository.findById(id)
            .map(feedSource -> {
                feedSource.setIsActive(isActive);
                feedSource.setUpdatedAt(Instant.now());

                FeedSource updated = feedSourceRepository.save(feedSource);
                log.info("FeedSource {} {}: {}",
                    updated.getName(),
                    isActive ? "activated" : "deactivated",
                    updated.getId());

                return ResponseEntity.ok(updated);
            })
            .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "FeedSource 삭제", description = "Feed 소스를 삭제합니다.")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteFeedSource(@PathVariable String id) {
        if (feedSourceRepository.existsById(id)) {
            feedSourceRepository.deleteById(id);
            log.info("FeedSource deleted: {}", id);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }

    @Operation(summary = "전체 수동 크롤링", description = "모든 활성화된 Feed 소스를 즉시 크롤링합니다.")
    @PostMapping("/crawl-all")
    public ResponseEntity<Map<String, Integer>> triggerCrawlAll() {
        log.info("Manual crawl-all triggered by admin");
        int count = feedCrawlingScheduler.crawlAllSources();
        return ResponseEntity.ok(Map.of("crawledCount", count));
    }

    @Operation(summary = "특정 소스 수동 크롤링", description = "특정 Feed 소스만 즉시 크롤링합니다.")
    @PostMapping("/{id}/crawl")
    public ResponseEntity<Map<String, Integer>> triggerCrawlSingle(@PathVariable String id) {
        log.info("Manual crawl triggered for FeedSource: {}", id);
        int count = feedCrawlingScheduler.crawlSingleSource(id);
        return ResponseEntity.ok(Map.of("crawledCount", count));
    }

    private String extractDomain(String url) {
        try {
            URL parsedUrl = new URL(url);
            String host = parsedUrl.getHost().toLowerCase();
            Pattern pattern = Pattern.compile("([^.]+\\.[^.]+)$");
            Matcher matcher = pattern.matcher(host);
            return matcher.find() ? matcher.group(1) : host;
        } catch (MalformedURLException e) {
            log.error("Invalid URL format: {}", url, e);
            return null;
        }
    }
}
