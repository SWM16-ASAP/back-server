package com.linglevel.api.content.article.controller;

import com.linglevel.api.auth.jwt.JwtClaims;
import com.linglevel.api.content.article.dto.*;
import com.linglevel.api.content.article.exception.ArticleException;
import com.linglevel.api.content.article.service.ArticleService;
import com.linglevel.api.content.article.service.ArticleChunkService;
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
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/articles")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Articles", description = "기사 관련 API")
public class ArticleController {

    private final ArticleService articleService;
    private final ArticleChunkService articleChunkService;


    @Operation(summary = "기사 목록 조회", description = "기사 목록을 조건에 따라 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 파라미터",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    })
    @GetMapping
    public ResponseEntity<PageResponse<ArticleResponse>> getArticles(
            @ParameterObject @ModelAttribute GetArticlesRequest request,
            @AuthenticationPrincipal JwtClaims claims) {
        String userId = claims != null ? claims.getId() : null;
        PageResponse<ArticleResponse> response = articleService.getArticles(request, userId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "단일 기사 조회", description = "특정 기사의 상세 정보를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "404", description = "기사를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    })
    @GetMapping("/{articleId}")
    public ResponseEntity<ArticleResponse> getArticle(
            @Parameter(description = "기사 ID", example = "60d0fe4f5311236168a109ca")
            @PathVariable String articleId,
            @AuthenticationPrincipal JwtClaims claims) {
        String userId = claims != null ? claims.getId() : null;
        ArticleResponse response = articleService.getArticle(articleId, userId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "기사 청크 목록 조회", description = "특정 기사의 청크 목록을 난이도별로 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "404", description = "기사를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 난이도 레벨",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    })
    @GetMapping("/{articleId}/chunks")
    public ResponseEntity<PageResponse<ArticleChunkResponse>> getArticleChunks(
            @Parameter(description = "기사 ID", example = "60d0fe4f5311236168a109ca")
            @PathVariable String articleId,
            @ParameterObject @Valid @ModelAttribute GetArticleChunksRequest request) {
        
        PageResponse<ArticleChunkResponse> response = articleChunkService.getArticleChunks(articleId, request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "단일 기사 청크 조회", description = "특정 기사 청크의 상세 정보를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "404", description = "기사 또는 청크를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    })
    @GetMapping("/{articleId}/chunks/{chunkId}")
    public ResponseEntity<ArticleChunkResponse> getArticleChunk(
            @Parameter(description = "기사 ID", example = "60d0fe4f5311236168a109ca")
            @PathVariable String articleId,
            @Parameter(description = "청크 ID", example = "60d0fe4f5311236168a109cd")
            @PathVariable String chunkId) {
        
        ArticleChunkResponse response = articleChunkService.getArticleChunk(articleId, chunkId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "기사 데이터 import", description = "S3에 저장된 JSON 파일을 읽어서 새로운 기사와 관련 청크 데이터를 생성합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "생성 성공", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "500", description = "import 실패",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    })
    @SecurityRequirement(name = "adminApiKey")
    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/import")
    public ResponseEntity<ArticleImportResponse> importArticle(
            @RequestBody ArticleImportRequest request) {

        ArticleImportResponse response = articleService.importArticle(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @ExceptionHandler(ArticleException.class)
    public ResponseEntity<ExceptionResponse> handleArticleException(ArticleException e) {
        log.info("Article Exception: {}", e.getMessage());
        return ResponseEntity.status(e.getStatus())
                .body(new ExceptionResponse(e));
    }
}