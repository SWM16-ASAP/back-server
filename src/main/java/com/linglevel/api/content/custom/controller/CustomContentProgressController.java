package com.linglevel.api.content.custom.controller;

import com.linglevel.api.content.custom.dto.CustomContentReadingProgressResponse;
import com.linglevel.api.content.custom.dto.CustomContentReadingProgressUpdateRequest;
import com.linglevel.api.content.custom.exception.CustomContentException;
import com.linglevel.api.content.custom.service.CustomContentReadingProgressService;
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
@RequestMapping("/api/v1/custom-contents")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Custom Contents Progress", description = "커스텀 콘텐츠 진도 관리 API")
public class CustomContentProgressController {

    private final CustomContentReadingProgressService customContentReadingProgressService;

    @Operation(summary = "커스텀 콘텐츠 읽기 진도 업데이트", description = "사용자의 커스텀 콘텐츠 읽기 진도를 업데이트합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "업데이트 성공", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "404", description = "커스텀 콘텐츠 또는 청크를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
            @ApiResponse(responseCode = "400", description = "잘못된 청크 ID",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    })
    @PutMapping("/{customId}/progress")
    public ResponseEntity<CustomContentReadingProgressResponse> updateProgress(
            @Parameter(description = "커스텀 콘텐츠 ID", example = "60d0fe4f5311236168a109ca")
            @PathVariable String customId,
            @Valid @RequestBody CustomContentReadingProgressUpdateRequest request,
            Authentication authentication) {
        String username = authentication.getName();
        CustomContentReadingProgressResponse response = customContentReadingProgressService.updateProgress(customId, request, username);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "커스텀 콘텐츠 읽기 진도 조회", description = "특정 커스텀 콘텐츠에 대한 사용자의 읽기 진도를 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "조회 성공", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "404", description = "커스텀 콘텐츠를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    })
    @GetMapping("/{customId}/progress")
    public ResponseEntity<CustomContentReadingProgressResponse> getProgress(
            @Parameter(description = "커스텀 콘텐츠 ID", example = "60d0fe4f5311236168a109ca")
            @PathVariable String customId,
            Authentication authentication) {
        String username = authentication.getName();
        CustomContentReadingProgressResponse response = customContentReadingProgressService.getProgress(customId, username);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "커스텀 콘텐츠 읽기 진도 삭제", description = "사용자의 읽기 진도 기록을 완전히 삭제합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "204", description = "삭제 성공"),
            @ApiResponse(responseCode = "404", description = "커스텀 콘텐츠 또는 진도 기록을 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    })
    @DeleteMapping("/{customId}/progress")
    public ResponseEntity<Void> deleteProgress(
            @Parameter(description = "커스텀 콘텐츠 ID", example = "60d0fe4f5311236168a109ca")
            @PathVariable String customId,
            Authentication authentication) {
        String username = authentication.getName();
        customContentReadingProgressService.deleteProgress(customId, username);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(CustomContentException.class)
    public ResponseEntity<ExceptionResponse> handleCustomContentException(CustomContentException e) {
        log.info("Custom Content Progress Exception: {}", e.getMessage());
        return ResponseEntity.status(e.getStatus())
                .body(new ExceptionResponse(e));
    }
}