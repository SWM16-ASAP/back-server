package com.linglevel.api.admin.controller;

import com.linglevel.api.admin.dto.NotificationSendResponse;
import com.linglevel.api.admin.dto.NotificationSendRequest;
import com.linglevel.api.fcm.dto.FcmMessageRequest;
import com.linglevel.api.admin.dto.UpdateChunkRequest;
import com.linglevel.api.admin.service.AdminService;
import com.linglevel.api.admin.service.NotificationService;
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
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Admin", description = "어드민 관리 API")
@SecurityRequirement(name = "adminApiKey")
public class AdminController {

    private final AdminService adminService;
    private final VersionService versionService;
    private final NotificationService notificationService;

    @Operation(summary = "책 청크 수정", description = "어드민 권한으로 특정 책의 청크 내용을 수정합니다.")
    @PutMapping("/books/{bookId}/chapters/{chapterId}/chunks/{chunkId}")
    public ResponseEntity<ChunkResponse> updateBookChunk(
            @Parameter(description = "책 ID", required = true) @PathVariable String bookId,
            @Parameter(description = "챕터 ID", required = true) @PathVariable String chapterId,
            @Parameter(description = "청크 ID", required = true) @PathVariable String chunkId,
            @Parameter(description = "청크 수정 요청", required = true) @Valid @RequestBody UpdateChunkRequest request) {
        
        log.info("Admin updating book chunk - bookId: {}, chapterId: {}, chunkId: {}", bookId, chapterId, chunkId);
        
        ChunkResponse response = adminService.updateBookChunk(bookId, chapterId, chunkId, request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "기사 청크 수정", description = "어드민 권한으로 특정 기사의 청크 내용을 수정합니다.")
    @PutMapping("/articles/{articleId}/chunks/{chunkId}")
    public ResponseEntity<ArticleChunkResponse> updateArticleChunk(
            @Parameter(description = "기사 ID", required = true) @PathVariable String articleId,
            @Parameter(description = "청크 ID", required = true) @PathVariable String chunkId,
            @Parameter(description = "청크 수정 요청", required = true) @Valid @RequestBody UpdateChunkRequest request) {
        
        log.info("Admin updating article chunk - articleId: {}, chunkId: {}", articleId, chunkId);
        
        ArticleChunkResponse response = adminService.updateArticleChunk(articleId, chunkId, request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "책 삭제", description = "어드민 권한으로 특정 책과 관련된 모든 데이터(챕터, 청크, 진도, S3 파일 등)를 삭제합니다.")
    @DeleteMapping("/books/{bookId}")
    public ResponseEntity<MessageResponse> deleteBook(
            @Parameter(description = "책 ID", required = true) @PathVariable String bookId) {
        
        log.info("Admin deleting book - bookId: {}", bookId);
        
        adminService.deleteBook(bookId);
        return ResponseEntity.ok(new MessageResponse("Book and all related data deleted successfully."));
    }

    @Operation(summary = "기사 삭제", description = "어드민 권한으로 특정 기사와 관련된 모든 데이터(청크, S3 파일 등)를 삭제합니다.")
    @DeleteMapping("/articles/{articleId}")
    public ResponseEntity<MessageResponse> deleteArticle(
            @Parameter(description = "기사 ID", required = true) @PathVariable String articleId) {
        
        log.info("Admin deleting article - articleId: {}", articleId);
        
        adminService.deleteArticle(articleId);
        return ResponseEntity.ok(new MessageResponse("Article and all related data deleted successfully."));
    }

    @Operation(summary = "앱 버전 업데이트", description = "어드민 권한으로 앱의 최신 버전 및 최소 요구 버전을 부분 업데이트합니다.")
    @PatchMapping("/version")
    public ResponseEntity<VersionUpdateResponse> updateVersion(
            @Parameter(description = "버전 업데이트 요청", required = true) @Valid @RequestBody VersionUpdateRequest request) {
        
        log.info("Admin updating app version - latestVersion: {}, minimumVersion: {}", 
                request.getLatestVersion(), request.getMinimumVersion());
        
        VersionUpdateResponse response = versionService.updateVersion(request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "푸시 알림 전송", description = "어드민 권한으로 사용자에게 FCM 푸시 알림을 전송합니다.")
    @PostMapping("/notifications/send")
    public ResponseEntity<NotificationSendResponse> sendNotification(
            @Parameter(description = "알림 전송 요청", required = true) @Valid @RequestBody NotificationSendRequest request) {
        
        log.info("Admin sending notification - targets: {}, title: {}", 
                request.getTargets() != null ? request.getTargets().size() : 0, request.getTitle());
        
        NotificationSendResponse response = notificationService.sendNotificationFromRequest(request);
        return ResponseEntity.ok(response);
    }

}