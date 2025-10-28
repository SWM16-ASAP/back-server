package com.linglevel.api.content.custom.controller;


import com.linglevel.api.auth.jwt.JwtClaims;
import com.linglevel.api.common.dto.ExceptionResponse;
import com.linglevel.api.common.dto.MessageResponse;
import com.linglevel.api.common.dto.PageResponse;
import com.linglevel.api.content.common.ContentType;
import com.linglevel.api.content.custom.dto.*;
import com.linglevel.api.content.custom.exception.CustomContentException;
import com.linglevel.api.content.custom.service.CustomContentService;
import com.linglevel.api.content.custom.service.CustomContentChunkService;
import com.linglevel.api.streak.service.ReadingSessionService;
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
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/custom-contents")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Custom Contents", description = "커스텀 콘텐츠 관련 API")
public class CustomContentController {

    private final CustomContentService customContentService;
    private final CustomContentChunkService customContentChunkService;
    private final ReadingSessionService readingSessionService;

    @Operation(
        summary = "커스텀 콘텐츠 목록 조회",
        description = "완료된 커스텀 콘텐츠 목록을 조회합니다. 기본적으로 최신순으로 정렬되며, 선택적으로 태그나 키워드 필터를 적용할 수 있습니다."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "조회 성공", useReturnTypeSchema = true),
        @ApiResponse(responseCode = "401", description = "인증 실패",
            content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    })
    @GetMapping
    public ResponseEntity<PageResponse<CustomContentResponse>> getCustomContents(
            @AuthenticationPrincipal JwtClaims claims,
            @ParameterObject @ModelAttribute GetCustomContentsRequest request) {

        PageResponse<CustomContentResponse> response = customContentService.getCustomContents(claims.getId(), request);
        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "단일 커스텀 콘텐츠 조회",
        description = "특정 커스텀 콘텐츠의 상세 정보를 조회합니다."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "조회 성공", useReturnTypeSchema = true),
        @ApiResponse(responseCode = "404", description = "커스텀 콘텐츠를 찾을 수 없음",
            content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
        @ApiResponse(responseCode = "403", description = "권한 없음",
            content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
        @ApiResponse(responseCode = "401", description = "인증 실패",
            content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    })
    @GetMapping("/{customContentId}")
    public ResponseEntity<CustomContentResponse> getCustomContent(
            @AuthenticationPrincipal JwtClaims claims,
            @Parameter(description = "커스텀 콘텐츠 ID", example = "60d0fe4f5311236168a109ca")
            @PathVariable String customContentId) {

        CustomContentResponse response = customContentService.getCustomContent(claims.getId(), customContentId);
        return ResponseEntity.ok(response);
    }

    @Operation(
            summary = "커스텀 콘텐츠 수정",
            description = "커스텀 콘텐츠의 제목이나 태그를 수정합니다."
    )
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "수정 성공", useReturnTypeSchema = true),
            @ApiResponse(responseCode = "404", description = "커스텀 콘텐츠를 찾을 수 없음",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
            @ApiResponse(responseCode = "403", description = "권한 없음",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
            @ApiResponse(responseCode = "401", description = "인증 실패",
                    content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    })
    @PatchMapping("/{customContentId}")
    public ResponseEntity<CustomContentResponse> updateCustomContent(
            @AuthenticationPrincipal JwtClaims claims,
            @Parameter(description = "커스텀 콘텐츠 ID", example = "60d0fe4f5311236168a109ca")
            @PathVariable String customContentId,
            @RequestBody UpdateCustomContentRequest request) {

        CustomContentResponse response = customContentService.updateCustomContent(claims.getId(), customContentId, request);
        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "커스텀 콘텐츠 청크 목록 조회",
        description = "특정 커스텀 콘텐츠에 속한 텍스트 청크(Chunk)들을 난이도별로 조회합니다."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "조회 성공", useReturnTypeSchema = true),
        @ApiResponse(responseCode = "404", description = "커스텀 콘텐츠를 찾을 수 없음",
            content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
        @ApiResponse(responseCode = "403", description = "권한 없음",
            content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
        @ApiResponse(responseCode = "401", description = "인증 실패",
            content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
        @ApiResponse(responseCode = "400", description = "잘못된 난이도 레벨",
            content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    })
    @GetMapping("/{customContentId}/chunks")
    public ResponseEntity<PageResponse<CustomContentChunkResponse>> getCustomContentChunks(
            @AuthenticationPrincipal JwtClaims claims,
            @Parameter(description = "커스텀 콘텐츠 ID", example = "60d0fe4f5311236168a109ca")
            @PathVariable String customContentId,
            @ParameterObject @ModelAttribute GetCustomContentChunksRequest request) {

        if (claims != null) {
            readingSessionService.startReadingSession(
                claims.getId(),
                ContentType.CUSTOM,
                customContentId
            );
        }

        PageResponse<CustomContentChunkResponse> response = customContentChunkService.getCustomContentChunks(claims.getId(), customContentId, request);
        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "단일 커스텀 콘텐츠 청크 조회",
        description = "특정 커스텀 콘텐츠 청크의 상세 정보를 조회합니다."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "조회 성공", useReturnTypeSchema = true),
        @ApiResponse(responseCode = "404", description = "커스텀 콘텐츠 또는 청크를 찾을 수 없음",
            content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
        @ApiResponse(responseCode = "403", description = "권한 없음",
            content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
        @ApiResponse(responseCode = "401", description = "인증 실패",
            content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    })
    @GetMapping("/{customContentId}/chunks/{chunkId}")
    public ResponseEntity<CustomContentChunkResponse> getCustomContentChunk(
            @AuthenticationPrincipal JwtClaims claims,
            @Parameter(description = "커스텀 콘텐츠 ID", example = "60d0fe4f5311236168a109ca")
            @PathVariable String customContentId,
            @Parameter(description = "청크 ID", example = "60d0fe4f5311236168a109cd")
            @PathVariable String chunkId) {

        CustomContentChunkResponse response = customContentChunkService.getCustomContentChunk(claims.getId(), customContentId, chunkId);
        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "커스텀 콘텐츠 삭제",
        description = "사용자가 본인이 생성한 커스텀 콘텐츠를 삭제합니다. 콘텐츠와 관련된 모든 청크 데이터도 함께 soft delete 처리됩니다."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "삭제 성공", useReturnTypeSchema = true),
        @ApiResponse(responseCode = "404", description = "커스텀 콘텐츠를 찾을 수 없음",
            content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
        @ApiResponse(responseCode = "403", description = "본인 콘텐츠만 삭제 가능",
            content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
        @ApiResponse(responseCode = "401", description = "인증 실패",
            content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    })
    @DeleteMapping("/{customContentId}")
    public ResponseEntity<MessageResponse> deleteCustomContent(
            @AuthenticationPrincipal JwtClaims claims,
            @Parameter(description = "커스텀 콘텐츠 ID", example = "60d0fe4f5311236168a109ca")
            @PathVariable String customContentId) {

        customContentService.deleteCustomContent(claims.getId(), customContentId);
        return ResponseEntity.ok(new MessageResponse("Custom content deleted successfully."));
    }

    @ExceptionHandler(CustomContentException.class)
    public ResponseEntity<ExceptionResponse> handleCustomContentException(CustomContentException e) {
        log.info("Custom Content Exception: {}", e.getMessage());
        return ResponseEntity.status(e.getStatus())
                .body(new ExceptionResponse(e));
    }
}