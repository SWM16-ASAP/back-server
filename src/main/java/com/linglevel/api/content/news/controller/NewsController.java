package com.linglevel.api.content.news.controller;

import com.linglevel.api.content.news.dto.*;
import com.linglevel.api.content.news.exception.NewsException;
import com.linglevel.api.common.dto.ExceptionResponse;
import com.linglevel.api.common.dto.PageResponse;
import com.linglevel.api.common.exception.CommonErrorCode;
import com.linglevel.api.common.exception.CommonException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/news")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "News", description = "뉴스 관련 API")
public class NewsController {

    // TODO : JWT으로 인증 변경 후 삭제
    @Value("${import.api.key}")
    private String importApiKey;

    @Operation(summary = "뉴스 목록 조회", description = "뉴스 목록을 조건에 따라 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 파라미터",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    })
    @GetMapping
    public ResponseEntity<PageResponse<NewsResponse>> getNews(
            @ParameterObject @ModelAttribute GetNewsRequest request) {
        
        // TODO: Service 구현 후 연결
        throw new RuntimeException("Not implemented yet");
    }

    @Operation(summary = "단일 뉴스 조회", description = "특정 뉴스의 상세 정보를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "404", description = "뉴스를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    })
    @GetMapping("/{newsId}")
    public ResponseEntity<NewsResponse> getNews(
            @Parameter(description = "뉴스 ID", example = "60d0fe4f5311236168a109ca")
            @PathVariable String newsId) {
        
        // TODO: Service 구현 후 연결
        throw new RuntimeException("Not implemented yet");
    }

    @Operation(summary = "뉴스 청크 목록 조회", description = "특정 뉴스의 청크 목록을 난이도별로 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "404", description = "뉴스를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 난이도 레벨",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    })
    @GetMapping("/{newsId}/chunks")
    public ResponseEntity<PageResponse<NewsChunkResponse>> getNewsChunks(
            @Parameter(description = "뉴스 ID", example = "60d0fe4f5311236168a109ca")
            @PathVariable String newsId,
            @ParameterObject @ModelAttribute GetNewsChunksRequest request) {
        
        // TODO: Service 구현 후 연결
        throw new RuntimeException("Not implemented yet");
    }

    @Operation(summary = "단일 뉴스 청크 조회", description = "특정 뉴스 청크의 상세 정보를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "404", description = "뉴스 또는 청크를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    })
    @GetMapping("/{newsId}/chunks/{chunkId}")
    public ResponseEntity<NewsChunkResponse> getNewsChunk(
            @Parameter(description = "뉴스 ID", example = "60d0fe4f5311236168a109ca")
            @PathVariable String newsId,
            @Parameter(description = "청크 ID", example = "60d0fe4f5311236168a109cd")
            @PathVariable String chunkId) {
        
        // TODO: Service 구현 후 연결
        throw new RuntimeException("Not implemented yet");
    }

    @Operation(summary = "뉴스 데이터 import", description = "S3에 저장된 JSON 파일을 읽어서 새로운 뉴스와 관련 청크 데이터를 생성합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "생성 성공", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "500", description = "import 실패",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    })
    @PostMapping("/import")
    public ResponseEntity<NewsImportResponse> importNews(
            @RequestHeader(value = "X-API-Key", required = true) String apiKey,
            @RequestBody NewsImportRequest request) {

        if (!importApiKey.equals(apiKey)) {
            log.warn("Invalid API key provided for news import");
            throw new CommonException(CommonErrorCode.UNAUTHORIZED);
        }

        // TODO: Service 구현 후 연결
        throw new RuntimeException("Not implemented yet");
    }

    @ExceptionHandler(NewsException.class)
    public ResponseEntity<ExceptionResponse> handleNewsException(NewsException e) {
        log.info("News Exception: {}", e.getMessage());
        return ResponseEntity.status(e.getStatus())
                .body(new ExceptionResponse(e));
    }
}