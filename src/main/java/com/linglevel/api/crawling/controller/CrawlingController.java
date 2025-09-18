package com.linglevel.api.crawling.controller;

import com.linglevel.api.common.dto.ExceptionResponse;
import com.linglevel.api.common.dto.PageResponse;
import com.linglevel.api.crawling.dto.DslLookupResponse;
import com.linglevel.api.crawling.dto.DomainsResponse;
import com.linglevel.api.crawling.exception.CrawlingException;
import com.linglevel.api.crawling.service.CrawlingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Crawling DSL", description = "크롤링 DSL 관리 관련 API")
public class CrawlingController {

    private final CrawlingService crawlingService;

    @Operation(summary = "DSL 조회 및 URL 유효성 검증", 
               description = "클라이언트가 현재 접속 중인 URL을 전달하면, 해당 URL의 도메인이 존재하는 경우 제목/본문 추출 DSL을 반환합니다. 또한 URL이 크롤링 가능한지 유효성 검증도 수행할 수 있습니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (URL 누락 또는 형식 오류)",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    })
    @GetMapping("/crawling-dsl/lookup")
    public ResponseEntity<DslLookupResponse> lookupDsl(
            @Parameter(description = "크롤링할 전체 URL", required = true, example = "https://www.coupang.com/vp/products/123456")
            @RequestParam String url,
            @Parameter(description = "DSL 반환 없이 유효성만 검증", example = "false")
            @RequestParam(defaultValue = "false") boolean validate_only) {
        
        DslLookupResponse response = crawlingService.lookupDsl(url, validate_only);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "등록된 도메인 목록 조회", description = "시스템에 등록된 모든 도메인 목록을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공", useReturnTypeSchema = true)
    })
    @GetMapping("/crawling-dsl/domains")
    public ResponseEntity<PageResponse<DomainsResponse>> getDomains(
            @Parameter(description = "조회할 페이지 번호", example = "1")
            @RequestParam(defaultValue = "1") int page,
            @Parameter(description = "페이지 당 항목 수 (최대 100)", example = "10")
            @RequestParam(defaultValue = "10") int limit) {
        
        if (limit > 100) limit = 100;
        
        Page<DomainsResponse> domains = crawlingService.getDomains(page, limit);
        return ResponseEntity.ok(new PageResponse<>(domains.getContent(), domains));
    }


    @ExceptionHandler(CrawlingException.class)
    public ResponseEntity<ExceptionResponse> handleCrawlingException(CrawlingException e) {
        log.info("Crawling Exception: {}", e.getMessage());
        return ResponseEntity.status(e.getStatus())
                .body(new ExceptionResponse(e.getMessage()));
    }
}