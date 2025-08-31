package com.linglevel.api.admin.controller;

import com.linglevel.api.admin.dto.UpdateChunkRequest;
import com.linglevel.api.admin.service.AdminService;
import com.linglevel.api.common.dto.MessageResponse;
import com.linglevel.api.common.exception.CommonErrorCode;
import com.linglevel.api.common.exception.CommonException;
import com.linglevel.api.content.book.dto.ChunkResponse;
import com.linglevel.api.content.article.dto.ArticleChunkResponse;
import com.linglevel.api.version.dto.VersionUpdateRequest;
import com.linglevel.api.version.dto.VersionUpdateResponse;
import com.linglevel.api.version.service.VersionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
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
@Tag(name = "Admin", description = "어드민 관리 API")
public class AdminController {

    private final AdminService adminService;
    private final VersionService versionService;
    
    @Value("${import.api.key}")
    private String importApiKey;

    @Operation(summary = "책 청크 수정", description = "어드민 권한으로 특정 책의 청크 내용을 수정합니다.")
    @PutMapping("/books/{bookId}/chapters/{chapterId}/chunks/{chunkId}")
    public ResponseEntity<ChunkResponse> updateBookChunk(
            @Parameter(description = "API 키", required = true) @RequestHeader(value = "X-API-Key") String apiKey,
            @Parameter(description = "책 ID", required = true) @PathVariable String bookId,
            @Parameter(description = "챕터 ID", required = true) @PathVariable String chapterId,
            @Parameter(description = "청크 ID", required = true) @PathVariable String chunkId,
            @Parameter(description = "청크 수정 요청", required = true) @Valid @RequestBody UpdateChunkRequest request) {
        
        validateApiKey(apiKey);
        
        log.info("Admin updating book chunk - bookId: {}, chapterId: {}, chunkId: {}", bookId, chapterId, chunkId);
        
        ChunkResponse response = adminService.updateBookChunk(bookId, chapterId, chunkId, request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "기사 청크 수정", description = "어드민 권한으로 특정 기사의 청크 내용을 수정합니다.")
    @PutMapping("/articles/{articleId}/chunks/{chunkId}")
    public ResponseEntity<ArticleChunkResponse> updateArticleChunk(
            @Parameter(description = "API 키", required = true) @RequestHeader(value = "X-API-Key") String apiKey,
            @Parameter(description = "기사 ID", required = true) @PathVariable String articleId,
            @Parameter(description = "청크 ID", required = true) @PathVariable String chunkId,
            @Parameter(description = "청크 수정 요청", required = true) @Valid @RequestBody UpdateChunkRequest request) {
        
        validateApiKey(apiKey);
        
        log.info("Admin updating article chunk - articleId: {}, chunkId: {}", articleId, chunkId);
        
        ArticleChunkResponse response = adminService.updateArticleChunk(articleId, chunkId, request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "책 삭제", description = "어드민 권한으로 특정 책과 관련된 모든 데이터(챕터, 청크, 진도, S3 파일 등)를 삭제합니다.")
    @DeleteMapping("/books/{bookId}")
    public ResponseEntity<MessageResponse> deleteBook(
            @Parameter(description = "API 키", required = true) @RequestHeader(value = "X-API-Key") String apiKey,
            @Parameter(description = "책 ID", required = true) @PathVariable String bookId) {
        
        validateApiKey(apiKey);
        
        log.info("Admin deleting book - bookId: {}", bookId);
        
        adminService.deleteBook(bookId);
        return ResponseEntity.ok(new MessageResponse("Book and all related data deleted successfully."));
    }

    @Operation(summary = "기사 삭제", description = "어드민 권한으로 특정 기사와 관련된 모든 데이터(청크, S3 파일 등)를 삭제합니다.")
    @DeleteMapping("/articles/{articleId}")
    public ResponseEntity<MessageResponse> deleteArticle(
            @Parameter(description = "API 키", required = true) @RequestHeader(value = "X-API-Key") String apiKey,
            @Parameter(description = "기사 ID", required = true) @PathVariable String articleId) {
        
        validateApiKey(apiKey);
        
        log.info("Admin deleting article - articleId: {}", articleId);
        
        adminService.deleteArticle(articleId);
        return ResponseEntity.ok(new MessageResponse("Article and all related data deleted successfully."));
    }

    @Operation(summary = "앱 버전 업데이트", description = "어드민 권한으로 앱의 최신 버전 및 최소 요구 버전을 부분 업데이트합니다.")
    @PatchMapping("/version")
    public ResponseEntity<VersionUpdateResponse> updateVersion(
            @Parameter(description = "API 키", required = true) @RequestHeader(value = "X-API-Key") String apiKey,
            @Parameter(description = "버전 업데이트 요청", required = true) @Valid @RequestBody VersionUpdateRequest request) {
        
        validateApiKey(apiKey);
        
        log.info("Admin updating app version - latestVersion: {}, minimumVersion: {}", 
                request.getLatestVersion(), request.getMinimumVersion());
        
        VersionUpdateResponse response = versionService.updateVersion(request);
        return ResponseEntity.ok(response);
    }

    private void validateApiKey(String apiKey) {
        if (!importApiKey.equals(apiKey)) {
            log.warn("Invalid API key attempt");
            throw new CommonException(CommonErrorCode.UNAUTHORIZED);
        }
    }

    @ExceptionHandler(CommonException.class)
    public ResponseEntity<MessageResponse> handleCommonException(CommonException e) {
        log.error("Admin API error: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new MessageResponse("Invalid API key."));
    }
}