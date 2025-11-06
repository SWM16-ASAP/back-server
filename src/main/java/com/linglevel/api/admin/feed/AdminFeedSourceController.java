package com.linglevel.api.admin.feed;

import com.linglevel.api.common.dto.ExceptionResponse;
import com.linglevel.api.common.dto.MessageResponse;
import com.linglevel.api.content.feed.dto.CreateFeedSourceRequest;
import com.linglevel.api.content.feed.dto.FeedSourceResponse;
import com.linglevel.api.content.feed.entity.FeedSource;
import com.linglevel.api.content.feed.repository.FeedSourceRepository;
import com.linglevel.api.content.feed.scheduler.FeedCrawlingScheduler;
import com.linglevel.api.crawling.service.CrawlingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/admin/feed-sources")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Admin - FeedSource", description = "어드민 FeedSource 관리 API")
@SecurityRequirement(name = "adminApiKey")
public class AdminFeedSourceController {

    private final FeedSourceRepository feedSourceRepository;
    private final FeedCrawlingScheduler feedCrawlingScheduler;
    private final CrawlingService crawlingService;

    @Operation(summary = "FeedSource 생성", description = "새로운 FeedSource를 등록합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "생성 성공", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "400", description = "잘못된 요청",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    })
    @PostMapping
    public ResponseEntity<FeedSourceResponse> createFeedSource(@Valid @RequestBody CreateFeedSourceRequest request) {
        log.info("Creating FeedSource: {}", request.getName());

        String domain = crawlingService.isValidUrl(request.getUrl())
                ? extractDomain(request.getUrl())
                : null;

        if (domain == null) {
            throw new IllegalArgumentException("Invalid URL format");
        }

        FeedSource feedSource = FeedSource.builder()
                .url(request.getUrl())
                .domain(domain)
                .name(request.getName())
                .titleDsl(request.getTitleDsl())
                .coverImageDsl(request.getCoverImageDsl())
                .contentType(request.getContentType())
                .category(request.getCategory())
                .tags(request.getTags())
                .isActive(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        FeedSource saved = feedSourceRepository.save(feedSource);
        log.info("FeedSource created: {} ({})", saved.getName(), saved.getId());

        return ResponseEntity.status(HttpStatus.CREATED).body(mapToResponse(saved));
    }

    @Operation(summary = "FeedSource 목록 조회", description = "등록된 모든 FeedSource를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    })
    @GetMapping
    public ResponseEntity<List<FeedSourceResponse>> getAllFeedSources() {
        List<FeedSource> feedSources = feedSourceRepository.findAll();
        List<FeedSourceResponse> responses = feedSources.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @Operation(summary = "FeedSource 단건 조회", description = "특정 FeedSource를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
            @ApiResponse(responseCode = "404", description = "FeedSource를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<FeedSourceResponse> getFeedSource(@PathVariable String id) {
        FeedSource feedSource = feedSourceRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("FeedSource not found: " + id));
        return ResponseEntity.ok(mapToResponse(feedSource));
    }

    @Operation(summary = "FeedSource 삭제", description = "특정 FeedSource를 삭제합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "삭제 성공",
                    content = @Content(schema = @Schema(implementation = MessageResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
            @ApiResponse(responseCode = "404", description = "FeedSource를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    })
    @DeleteMapping("/{id}")
    public ResponseEntity<MessageResponse> deleteFeedSource(@PathVariable String id) {
        if (!feedSourceRepository.existsById(id)) {
            throw new IllegalArgumentException("FeedSource not found: " + id);
        }
        feedSourceRepository.deleteById(id);
        log.info("FeedSource deleted: {}", id);
        return ResponseEntity.ok(new MessageResponse("FeedSource deleted successfully"));
    }

    @Operation(summary = "전체 크롤링 실행", description = "모든 활성화된 FeedSource를 크롤링합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "크롤링 성공", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    })
    @PostMapping("/crawl-all")
    public ResponseEntity<Map<String, Integer>> triggerCrawlAll() {
        log.info("Manual crawl-all triggered");
        int count = feedCrawlingScheduler.crawlAllSources();
        return ResponseEntity.ok(Map.of("crawledCount", count));
    }

    @Operation(summary = "개별 크롤링 실행", description = "특정 FeedSource를 크롤링합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "크롤링 성공", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
            @ApiResponse(responseCode = "404", description = "FeedSource를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    })
    @PostMapping("/{id}/crawl")
    public ResponseEntity<Map<String, Integer>> triggerCrawlSingle(
            @Parameter(description = "크롤링할 FeedSource ID") @PathVariable String id) {
        log.info("Manual crawl triggered for FeedSource: {}", id);
        int count = feedCrawlingScheduler.crawlSingleSource(id);
        return ResponseEntity.ok(Map.of("crawledCount", count));
    }

    private FeedSourceResponse mapToResponse(FeedSource feedSource) {
        return FeedSourceResponse.builder()
                .id(feedSource.getId())
                .url(feedSource.getUrl())
                .domain(feedSource.getDomain())
                .name(feedSource.getName())
                .titleDsl(feedSource.getTitleDsl())
                .coverImageDsl(feedSource.getCoverImageDsl())
                .contentType(feedSource.getContentType())
                .category(feedSource.getCategory())
                .tags(feedSource.getTags())
                .isActive(feedSource.getIsActive())
                .createdAt(feedSource.getCreatedAt())
                .updatedAt(feedSource.getUpdatedAt())
                .build();
    }

    private String extractDomain(String url) {
        try {
            java.net.URL parsedUrl = new java.net.URL(url);
            String host = parsedUrl.getHost().toLowerCase();
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("([^.]+\\.[^.]+)$");
            java.util.regex.Matcher matcher = pattern.matcher(host);
            return matcher.find() ? matcher.group(1) : host;
        } catch (Exception e) {
            return null;
        }
    }
}
