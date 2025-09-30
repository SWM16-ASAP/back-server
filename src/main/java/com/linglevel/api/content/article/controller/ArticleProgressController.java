package com.linglevel.api.content.article.controller;

import com.linglevel.api.content.article.dto.ArticleProgressResponse;
import com.linglevel.api.content.article.dto.ArticleProgressUpdateRequest;
import com.linglevel.api.content.article.exception.ArticleException;
import com.linglevel.api.content.article.service.ArticleProgressService;
import com.linglevel.api.common.dto.ExceptionResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/articles")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Articles Progress", description = "아티클 진도 관리 API")
public class ArticleProgressController {

    private final ArticleProgressService articleProgressService;

    @Operation(summary = "아티클 읽기 진도 업데이트", description = "사용자의 아티클 읽기 진도를 업데이트합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "업데이트 성공", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "404", description = "아티클 또는 청크를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 청크 ID",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    })
    @PutMapping("/{articleId}/progress")
    public ResponseEntity<ArticleProgressResponse> updateProgress(
            @Parameter(description = "아티클 ID", example = "60d0fe4f5311236168a109ca")
            @PathVariable String articleId,
            @Valid @RequestBody ArticleProgressUpdateRequest request,
            Authentication authentication) {
        String username = authentication.getName();
        ArticleProgressResponse response = articleProgressService.updateProgress(articleId, request, username);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "아티클 읽기 진도 조회", description = "특정 아티클에 대한 사용자의 읽기 진도를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "404", description = "아티클을 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    })
    @GetMapping("/{articleId}/progress")
    public ResponseEntity<ArticleProgressResponse> getProgress(
            @Parameter(description = "아티클 ID", example = "60d0fe4f5311236168a109ca")
            @PathVariable String articleId,
            Authentication authentication) {
        String username = authentication.getName();
        ArticleProgressResponse response = articleProgressService.getProgress(articleId, username);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "아티클 읽기 진도 삭제", description = "사용자의 읽기 진도 기록을 완전히 삭제합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "삭제 성공"),
            @ApiResponse(responseCode = "404", description = "아티클 또는 진도 기록을 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    })
    @DeleteMapping("/{articleId}/progress")
    public ResponseEntity<Void> deleteProgress(
            @Parameter(description = "아티클 ID", example = "60d0fe4f5311236168a109ca")
            @PathVariable String articleId,
            Authentication authentication) {
        String username = authentication.getName();
        articleProgressService.deleteProgress(articleId, username);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(ArticleException.class)
    public ResponseEntity<ExceptionResponse> handleArticleException(ArticleException e) {
        log.info("Article Progress Exception: {}", e.getMessage());
        return ResponseEntity.status(e.getStatus())
                .body(new ExceptionResponse(e));
    }
}