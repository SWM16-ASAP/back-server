package com.linglevel.api.admin.crawling;

import com.linglevel.api.common.dto.ExceptionResponse;
import com.linglevel.api.common.dto.MessageResponse;
import com.linglevel.api.crawling.dto.CreateDslRequest;
import com.linglevel.api.crawling.dto.CreateDslResponse;
import com.linglevel.api.crawling.dto.UpdateDslRequest;
import com.linglevel.api.crawling.dto.UpdateDslResponse;
import com.linglevel.api.common.exception.CommonErrorCode;
import com.linglevel.api.common.exception.CommonException;
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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Admin - Crawling DSL", description = "어드민 크롤링 DSL 관리 API")
public class AdminCrawlingController {

    private final CrawlingService crawlingService;
    
    @Value("${import.api.key}")
    private String importApiKey;

    @Operation(summary = "어드민 - DSL 생성", description = "어드민 권한으로 새로운 도메인의 제목/본문 추출 DSL을 추가합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "생성 성공", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (필수 필드 누락)",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패 (잘못된 API 키)",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
            @ApiResponse(responseCode = "409", description = "도메인이 이미 존재함",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    })
    @PostMapping("/crawling-dsl")
    public ResponseEntity<CreateDslResponse> createDsl(
            @RequestHeader("X-API-Key") String apiKey,
            @Valid @RequestBody CreateDslRequest request) {
        
        validateApiKey(apiKey);
        CreateDslResponse response = crawlingService.createDsl(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "어드민 - DSL 업데이트", description = "어드민 권한으로 특정 도메인의 제목/본문 추출 DSL을 업데이트합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "업데이트 성공", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (제목/본문 DSL 필드 누락)",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패 (잘못된 API 키)",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
            @ApiResponse(responseCode = "404", description = "도메인을 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    })
    @PutMapping("/crawling-dsl/{domain}")
    public ResponseEntity<UpdateDslResponse> updateDsl(
            @RequestHeader("X-API-Key") String apiKey,
            @Parameter(description = "업데이트할 도메인명", example = "coupang.com")
            @PathVariable String domain,
            @Valid @RequestBody UpdateDslRequest request) {
        
        validateApiKey(apiKey);
        UpdateDslResponse response = crawlingService.updateDsl(domain, request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "어드민 - DSL 삭제", description = "어드민 권한으로 특정 도메인과 관련된 제목/본문 추출 DSL을 삭제합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "삭제 성공",
                    content = @Content(schema = @Schema(implementation = MessageResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패 (잘못된 API 키)",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
            @ApiResponse(responseCode = "404", description = "도메인을 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    })
    @DeleteMapping("/crawling-dsl/{domain}")
    public ResponseEntity<MessageResponse> deleteDsl(
            @RequestHeader("X-API-Key") String apiKey,
            @Parameter(description = "삭제할 도메인명", example = "coupang.com")
            @PathVariable String domain) {
        
        validateApiKey(apiKey);
        crawlingService.deleteDsl(domain);
        return ResponseEntity.ok(new MessageResponse("DSL deleted successfully."));
    }

    private void validateApiKey(String apiKey) {
        if (!importApiKey.equals(apiKey)) {
            log.warn("Invalid API key attempt");
            throw new CommonException(CommonErrorCode.UNAUTHORIZED);
        }
    }

    @ExceptionHandler(CommonException.class)
    public ResponseEntity<MessageResponse> handleCommonException(CommonException e) {
        log.error("Admin Crawling API error: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new MessageResponse("Invalid API key."));
    }
}