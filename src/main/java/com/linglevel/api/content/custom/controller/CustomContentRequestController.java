package com.linglevel.api.content.custom.controller;


import com.linglevel.api.common.dto.ExceptionResponse;
import com.linglevel.api.common.dto.PageResponse;
import com.linglevel.api.content.custom.dto.*;
import com.linglevel.api.content.custom.exception.CustomContentException;
import com.linglevel.api.content.custom.service.CustomContentRequestService;
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
import com.linglevel.api.auth.jwt.JwtClaims;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/custom-contents/requests")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Custom Content Requests", description = "커스텀 콘텐츠 처리 요청 관련 API")
public class CustomContentRequestController {

    private final CustomContentRequestService customContentRequestService;

    @Operation(
        summary = "콘텐츠 처리 요청 생성", 
        description = "사용자가 텍스트를 입력하여 AI 콘텐츠 처리 요청을 생성합니다."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "요청 생성 성공", useReturnTypeSchema = true),
        @ApiResponse(responseCode = "400", description = "잘못된 요청 데이터",
            content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
        @ApiResponse(responseCode = "401", description = "인증 실패",
            content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    })
    @PostMapping
    public ResponseEntity<CreateContentRequestResponse> createContentRequest(
            @Parameter(description = "콘텐츠 처리 요청 생성", required = true) 
            @Valid @RequestBody CreateContentRequestRequest request,
            @AuthenticationPrincipal JwtClaims claims) {
        
        CreateContentRequestResponse response = customContentRequestService.createContentRequest(claims.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(
        summary = "콘텐츠 처리 요청 목록 조회", 
        description = "사용자의 콘텐츠 처리 요청 목록을 조회합니다. 진행 중이거나 완료된 요청들을 상태별로 확인할 수 있습니다."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "조회 성공", useReturnTypeSchema = true),
        @ApiResponse(responseCode = "401", description = "인증 실패",
            content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    })
    @GetMapping
    public ResponseEntity<PageResponse<ContentRequestResponse>> getContentRequests(
            @ParameterObject @ModelAttribute GetContentRequestsRequest request,
            @AuthenticationPrincipal JwtClaims claims) {
        
        PageResponse<ContentRequestResponse> response = customContentRequestService.getContentRequests(claims.getId(), request);
        return ResponseEntity.ok(response);
    }

    @Operation(
        summary = "특정 콘텐츠 처리 요청 조회", 
        description = "특정 콘텐츠 처리 요청의 상세 정보를 조회합니다."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "조회 성공", useReturnTypeSchema = true),
        @ApiResponse(responseCode = "404", description = "요청을 찾을 수 없음",
            content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
        @ApiResponse(responseCode = "403", description = "권한 없음",
            content = @Content(schema = @Schema(implementation = ExceptionResponse.class))),
        @ApiResponse(responseCode = "401", description = "인증 실패",
            content = @Content(schema = @Schema(implementation = ExceptionResponse.class)))
    })
    @GetMapping("/{requestId}")
    public ResponseEntity<ContentRequestResponse> getContentRequest(
            @Parameter(description = "요청 ID", example = "60d0fe4f5311236168a109ca")
            @PathVariable String requestId,
            @AuthenticationPrincipal JwtClaims claims) {
        
        ContentRequestResponse response = customContentRequestService.getContentRequest(claims.getId(), requestId);
        return ResponseEntity.ok(response);
    }

    @ExceptionHandler(CustomContentException.class)
    public ResponseEntity<ExceptionResponse> handleCustomContentException(CustomContentException e) {
        log.info("Custom Content Exception: {}", e.getMessage());
        return ResponseEntity.status(e.getStatus())
                .body(new ExceptionResponse(e));
    }
}